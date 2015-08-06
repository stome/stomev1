package stome;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.net.URL;
import java.net.URLEncoder;
import java.net.URLConnection;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.unbescape.html.HtmlEscape;

/* Fetches facebook share count or title for given url, whichever type is specified */

class DataFetcher extends Thread
{
    private int type = -1;
    private int keyIndex = -1;
    private String linkKey = null;
    private String url = null;
    private LinkDataFetcher parentFetcher = null;
    private URLConnection conn = null;

    public DataFetcher( 
        int type, int keyIndex, String linkKey, String url, 
        LinkDataFetcher parent )
    {
        this.type     = type;
        this.keyIndex = keyIndex;
        this.linkKey  = linkKey;
        this.url      = url;
        parentFetcher = parent;
    }

    public String toString()
    {
        String typeString = "";
        if( type == Stome.SHARE_COUNT )
            typeString = "SHARE_COUNT";
        else if( type == Stome.TITLE )
            typeString = "TITLE";
        return typeString + ": " + url;
    }

    public void run()
    {
        String value = "";

        long startTime = System.currentTimeMillis();

        try
        {
            String dataUrl = null;
            if( type == Stome.SHARE_COUNT )
            {
                // Give Youtube video urls with the same id the same share count
                String shareUrl = url;
                if( url.matches( ".*youtube.com/watch.*[?&]v=.*" ) )
                {
                    String youtubeId = url.replaceAll( 
                        ".*youtube.com/watch\\?.*v=([-_0-9a-zA-Z]{11}).*", "$1" );
                    shareUrl = "https://www.youtube.com/watch?v=" + youtubeId;
                }

                dataUrl =
                    "http://api.facebook.com/method/fql.query?query=" +
                    "SELECT%20share_count%20FROM%20link_stat%20WHERE%20url=%27" +
                    URLEncoder.encode( shareUrl.replace( "'", "\\'" ), "UTF-8" ) + "%27";
            }
            else if( type == Stome.TITLE )
            {
                dataUrl =
                    "http://query.yahooapis.com/v1/public/yql?q=" +
                    "select%20*%20from%20html%20where%20url%3D%22" + 
                    URLEncoder.encode( url.replace( "'", "\\'" ), "UTF-8" ) + 
                    "%22%20and%20xpath%3D'%2F%2Ftitle'&format=xml";
            }

            String content = getHTMLContent( dataUrl, 3000 );
            if( content != null )
            {
                String regex = "";
                if( type == Stome.SHARE_COUNT )
                    regex = "<share_count>(\\d+)</share_count>";
                else if( type == Stome.TITLE )
                    regex = "<title>([^<]+)</title>";

                Pattern pattern = Pattern.compile( regex, Pattern.CASE_INSENSITIVE );
                Matcher matcher = pattern.matcher( content );
                if( matcher.find() )
                    value = matcher.group( 1 );
                    if( type == Stome.TITLE )
                    {
                        value = HtmlEscape.unescapeHtml( value.trim() );
                    }
            }
        }
        catch( java.io.UnsupportedEncodingException ex ) {}
        catch( java.lang.NullPointerException ex ) {}

        parentFetcher.processLinkData( keyIndex, linkKey, value );

        // Time fetch to determine how long it takes
        // If it takes less than 1000 milliseconds, then
        // adds a sleep to make up the difference
        // This is needed to prevent Facebook throttling

        if( type == Stome.SHARE_COUNT )
        {
            long endTime = System.currentTimeMillis();

            long elapsed = endTime - startTime;
            long fillTime = 1000 - elapsed;
            if( fillTime > 0 )
            {
                sleep( new Long( fillTime ).intValue() );
            }
        }
    }

    public void sleep( int milliseconds )
    {
        try { Thread.sleep( milliseconds ); }
        catch( InterruptedException e )
        {
            // Forces connection to abort
            conn.setConnectTimeout( 0 );
        }
    }

    public String getHTMLContent( String url, int timeoutMilliseconds )
    {
        String content = "";
        try
        {
            URL urlObj = new URL( url );
            conn = urlObj.openConnection();
            conn.setConnectTimeout( timeoutMilliseconds );

            BufferedReader in = 
                new BufferedReader( new InputStreamReader( conn.getInputStream() ) );
            String inputLine;
            while( ( inputLine = in.readLine() ) != null )
                content += inputLine;
            in.close();
        }
        catch( IOException ex ) {}

        return content;
    }
}
