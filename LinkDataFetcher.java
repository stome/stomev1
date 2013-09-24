import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.io.*;
import java.util.regex.*;

public class LinkDataFetcher extends Thread
{
    public final static int MAX_FETCHER_COUNT = 30;
    private int type = -1;
    private ArrayList<String> linkKeys = null;
    private ConcurrentHashMap<String,String> allUrls = null;
    private boolean[] busyKeys = null;
    private boolean[] completedKeys = null;
    private LinkProcessor parentProcessor = null;
    private ArrayList<DataFetcher> fetchers = new ArrayList<DataFetcher>();
    private int fetcherCount = 0;
    private boolean updateExisting;

    public LinkDataFetcher( int type, ArrayList<String> linkKeys,
        ConcurrentHashMap<String,String> allUrls, LinkProcessor parentProcessor )
    {
        this.type = type;
        this.linkKeys = linkKeys;
        this.allUrls = allUrls;
        this.parentProcessor = parentProcessor;

        busyKeys      = new boolean[ linkKeys.size() ];
        completedKeys = new boolean[ linkKeys.size() ];
    }

    public boolean completed()
    {
        return ( completedCount() == linkKeys.size() );
    }

    public int completedCount()
    {
        int completedCount = 0;
        for( int i = 0; i < linkKeys.size(); i++ )
        {
            if( completedKeys[ i ] )
                completedCount++;
        }
        return completedCount;
    }

    public void run()
    {
        int i = 0;
        while( i <= linkKeys.size() )
        {
            // If at the end of the list
            if( i == linkKeys.size() )
            {
                // If all urls have been processed then exit loop
                if( completed() )
                {
                    break;
                }
                // Otherwise start at the beginning of the list again
                else
                {
                    i = 0;
                }
            }

            // Ignore urls that are in the process or have already been fetched
            if( completedKeys[ i ] || busyKeys[ i ] )
            {
                if( busyKeys[ i ] )
                    sleep( 1000 );
                i++;
                continue;
            }

            // Don't create too many fetchers at once
            if( fetcherCount > MAX_FETCHER_COUNT )
            {
                sleep( 1000 );
                continue;
            }

            String linkKey = linkKeys.get( i );
            String url = allUrls.get( linkKey );
            DataFetcher fetcher = null;
            if( type == Stome.SHARE_COUNT )
            {
//                System.out.println( "share count fetcher: " + linkKey + " " + url );
                fetcher = 
                    new DataFetcher( Stome.SHARE_COUNT, i, linkKey, url, this );
            }
            else if( type == Stome.TITLE )
            {
//                System.out.println( "title fetcher: " + linkKey + " " + url );
                fetcher = new DataFetcher( Stome.TITLE, i, linkKey, url, this );
            }

            if( fetcher != null )
            {
                fetchers.add( fetcher );
                busyKeys[ i ] = true;
                fetcherCount++;
                fetcher.start();

                i++;
            }

            sleep( 200 );
        }
    }

    public synchronized void processLinkData(
        int keyIndex, String linkKey, String value )
    {
        parentProcessor.processLinkData( type, linkKey, value );
        completedKeys[ keyIndex ] = true;
        busyKeys[ keyIndex ] = false;

        if( type == Stome.SHARE_COUNT )
            parentProcessor.setFetchStatus( completedCount(), linkKeys.size() );

        fetcherCount--;
    }

    public void sleep( int milliseconds )
    {
        try { Thread.sleep( milliseconds ); }
        catch( InterruptedException e )
        {
            // Mark all keys completed
            for( int i = 0; i < linkKeys.size(); i++ )
                completedKeys[ i ] = true;

            // Stop current fetchers
            for( int i = 0; i < fetchers.size(); i++ )
            {
                DataFetcher fetcher = fetchers.get( i );
                fetcher.interrupt();
                while( fetcher.isAlive() && ! fetcher.isInterrupted() )
                {
                    try { Thread.sleep( 200 ); }
                    catch( InterruptedException ex ) {}
                }
            }
        }
    }
}
