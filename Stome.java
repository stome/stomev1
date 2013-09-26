/*
STOME - Taking the Wold Wide Web by Stome

TODO 0: Make it so that hovering over empty tags doesn't bring anything up

     VERSION 2.0
TODO 1: 1.0 double-click views tag links
TODO 3: 2.0 implement Open Links: open muliple links at once (popup menu)
TODO 4: 2.0 implement Refresh Shares (popup menu)
TODO 5: 4.0 PAUSE/RESUME FETCHING
TODO 6: 8.0 IMPORT LINKS FROM CHROME
TODO 7: 8.0 IMPORT LINKS FROM FIREFOX
TODO 8: 8.0 export selection to database
TODO 9: 8.0 export selection to spreadsheet
TODO 10: 8.0 make tag links that open new stome windows
TODO 11: 8.0 make domain links that open new stome windows
TODO 12: 8.0 add search box and Find button for searching within selected Results
TODO 13: 8.0 add Sites tab (similar to Tags tab)
TODO 14: 16.0 add bing as backup search in case google dies
TODO 15: 40.0 implement Feed tab (rss)
TODO 16: 40.0 implement Feed tab (twitter updates)
TODO 17: 4.0 set up bountysource account

COMPLETE
TODO 2: 2.0 implement Add Tag: add tag to multiple links at once (popup menu)
*/

import net.miginfocom.swing.MigLayout;

import java.util.ArrayList;

import org.apache.commons.validator.routines.UrlValidator;

import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import java.net.URL;
import java.net.URLDecoder;
import java.net.URLConnection;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JFrame;
import javax.swing.DefaultCellEditor;
import javax.swing.ListSelectionModel;
import javax.swing.UIDefaults;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;



public class Stome
{
    public static final String APP_NAME    = "STOME";
    public static final String APP_VERSION = "1.0";
    public static final String APP_TITLE   = APP_NAME + " " + APP_VERSION;

    public static final int SHARE_COUNT = 1;
    public static final int TITLE       = 2;

    private static JFrame frame = new JFrame( "Search - " + Stome.APP_TITLE );

    private static ResultsModel resultsModel = null;

    private static JTextArea  linksInput    = new JTextArea( 20, 40 );
    private static JTextField keywordsInput = new JTextField();
//    static JTextField siteInput     = new JTextField();

    private static JButton linksImportButton    = new JButton( "Import" );
    private static JButton keywordsSearchButton = new JButton( "Search" );
//    static JButton siteScanButton       = new JButton( "Scan" );
    private static JButton tagsViewButton       = new JButton( "View" );

    private static JButton clearLinksButton = new JButton( "Clear Links" );

    private static JTable resultsTable    = null;
    private static JTabbedPane tabbedPane = null;

    private static TagsPanel tagsPanel = null;
 
    private static String ip = null;

    private static String dbFile  = null;
    private static String openTag = null;

    private static Color evenColor = new Color( 250, 250, 250 );
    private static Color selectedColor = null;

    public static void main( String[] args )
    {
        String workingDir = Stome.class.getProtectionDomain().
                            getCodeSource().getLocation().getPath();
        try
        {
            workingDir = URLDecoder.decode( workingDir, "utf-8" );
        }
        catch( UnsupportedEncodingException ex ) {}
        File baseDir = new File( workingDir );
        File dbf = null;
        if( baseDir.getAbsolutePath().matches( ".*\\.jar$" ) )
            dbf = new File( baseDir.getParent(), "Stome.db" );
        else
            dbf = new File( baseDir, "Stome.db" );
        if( dbf != null )
            dbFile = dbf.getAbsolutePath();

        Options options = new Options();
        options.addOption( "d", "database", true, 
            "Use database file (default: " + dbFile + ")" );
        options.addOption( "t", "tag", true, "Load tag on opening" );
        options.addOption( "h", "help", false, "Print this help menu" );

        boolean showHelp = false;

        CommandLineParser parser = new BasicParser();
        try
        {
            CommandLine line = parser.parse( options, args );
            if( line.hasOption( "d" ) )
                dbFile = line.getOptionValue( "d" );

            if( line.hasOption( "t" ) )
                openTag = line.getOptionValue( "t" );

            if( line.hasOption( "h" ) )
                showHelp = true;
        }
        catch( org.apache.commons.cli.ParseException ex ) { ex.printStackTrace(); }

        if( showHelp )
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( Stome.APP_NAME, options );
            System.exit( 0 );
        }

        UIDefaults defaults = javax.swing.UIManager.getDefaults();
        selectedColor = defaults.getColor( "List.selectionBackground" );

        ip = getClientIpAddr();
        guiInit();
        wireButtons();

        if( openTag != null )
        {
            tabbedPane.setSelectedIndex( 2 );

            String[] openTags = openTag.split( "," );
            tagsPanel.select( openTags );
            tagsViewButton.doClick();
        }
    }

    public static void scrollToVisible( JTable table, int rowIndex, int colIndex )
    {
        if( ! ( table.getParent() instanceof JViewport ) )
            return;

        JViewport viewport = (JViewport) table.getParent();

        // This rectangle is relative to the table where the
        // northwest corner of cell (0,0) is always (0,0).
        Rectangle rect = table.getCellRect( rowIndex, colIndex, true );

        // The location of the viewport relative to the table
        Point pt = viewport.getViewPosition();

        // Translate the cell location so that it is relative
        // to the view, assuming the northwest corner of the
        // view is (0,0)
        rect.setLocation( rect.x-pt.x, rect.y-pt.y );

        table.scrollRectToVisible( rect );
    }

    // Called by LinkProcessor to disable query buttons during fetching and
    // reenable them when it's done

    public static void buttonsSetEnabled( boolean enabled )
    {
        linksImportButton.setEnabled( enabled );
        keywordsSearchButton.setEnabled( enabled );
//        siteScanButton.setEnabled( enabled );
        tagsViewButton.setEnabled( enabled );
    }

    private static void wireButtons()
    {
        linksImportButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e )
            {
                String[] links = linksInput.getText().split( "\n" );
                addLinks( links );
            }
        } );

        keywordsSearchButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e )
            {
                String query = keywordsInput.getText().replace( " ", "+" );

                // Only 64 results can be fetched using this method 
                // (8 pages of 8 results per page)

                ArrayList <String> urlArrayList = doSearchJava( query );
//                ArrayList <String> urlArrayList = doSearchLocalPython( query );
//                ArrayList <String> urlArrayList = doSearchRemotePython( query );

                String[] links = urlArrayList.toArray(
                    new String[ urlArrayList.size() ] );
                addLinks( links );
            }
        } );

/*
        siteScanButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e )
            {
                String site = siteInput.getText();
            }
        } );
*/

        tagsViewButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e )
            {
                Tags selectedTags = tagsPanel.getSelectedTags();

                ArrayList<String> urlArrayList = 
                    resultsModel.getTagLinks( selectedTags );
                String[] links = urlArrayList.toArray(
                    new String[ urlArrayList.size() ] );
                addLinks( links );
            }
        } );
    }

/*
    private static ArrayList<String> doSearchLocalPython( String query )
    {
        ArrayList<String> urlArrayList = new ArrayList<String>();

        String cmd = "/home/john/stome/google_search2.py";
        ProcessBuilder builder = new ProcessBuilder( cmd, query, "1", ip );

        try
        {
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader( process.getInputStream () ) );
            String line = null;
            while( ( line = reader.readLine () ) != null )
            {
                urlArrayList.add( line );
            }
        }
        catch( java.io.IOException ex ) {}

        return urlArrayList;
    }

    private static ArrayList<String> doSearchRemotePython( String query )
    {
        ArrayList<String> urlArrayList = new ArrayList<String>();

        String searchUrl = 
            "http://ec2-54-234-73-91.compute-1.amazonaws.com/" + 
            "cgi-bin/do_search.py?user_id=1&query=" + 
            query + "&page=1&ip=" + ip;
        String content = getHTMLContent( searchUrl, 5000, ip );
        String[] urls = content.split( "\n" );
        for( int i = 0; i < urls.length; i++ )
            urlArrayList.add( urls[ i ] );

        return urlArrayList;
    }
*/

    private static ArrayList<String> doSearchJava( String query )
    {
        ArrayList<String> urlArrayList = new ArrayList<String>();

        for( int page = 1; page <= 9; page++ )
        {
            int start = ( page - 1 ) * 8;
            String searchUrl =
                "https://ajax.googleapis.com/ajax/services/search/web" +
                "?v=1.0&safe=active&rsz=large&start=" + start + "&q=" +
                query + "&userip=" + ip;

            String content = getHTMLContent( searchUrl, 5000, ip );

            try
            {
                JSONParser parser = new JSONParser();
                if( parser != null && content != null )
                {
                    JSONObject json = (JSONObject) parser.parse( content );
                    JSONObject jdata = (JSONObject) json.get( "responseData" );

                    if( jdata != null )
                    {
                        JSONArray jresults = (JSONArray) jdata.get( "results" );
                        for( int i = 0; i < jresults.size(); i++ )
                        {
                            JSONObject jresult = (JSONObject) jresults.get( i );
                            String url = (String) jresult.get( "unescapedUrl" );
                            urlArrayList.add( url );
                        }
                    }
                    else
                    {
//System.err.println( json );
//System.err.println( searchUrl );
                        break;
                    }
                }
            }
            catch( org.json.simple.parser.ParseException ex ) {}
//            try { Thread.sleep( 200 ); } catch( InterruptedException ex ) {}
        }

        return urlArrayList;
    }

    public static String getClientIpAddr()
    {
        String ip = null;
        try
        {
            ip = new BufferedReader( new InputStreamReader(
                new URL( "http://agentgatech.appspot.com" ).
                    openStream() ) ).readLine();
        }
        catch( java.net.MalformedURLException ex ) {}
        catch( java.io.IOException ex ) {}

        return ip;
    }

    private static void addLinks( String[] links )
    {
        if( links != null && links.length > 0 )
        {
            buttonsSetEnabled( false );
            resultsModel.startUrls();
            int added = 0;
            for( int i = 0; i < links.length; i++ )
            {
                if( urlIsValid( links[ i ] ) )
                {
                    if( resultsModel.addUrl( links[ i ] ) )
                        added++;
                }
            }
            resultsModel.stopUrls();
            if( added == 0 )
                buttonsSetEnabled( true );
        }
    }

    private static void guiInit()
    {
        MigLayout buttonLayout = new MigLayout(
            "", "[100%,align left][100%,align right]", ""
        );

        JLabel fetchStatusLabel = new JLabel();

        resultsModel = new ResultsModel( fetchStatusLabel, dbFile );

        Tags tags = resultsModel.getAllTags();
        tags.add( 0, "" );

        tagsPanel = new TagsPanel( resultsModel, tagsViewButton );
        resultsModel.setTagsPanel( tagsPanel );

        Java2sAutoComboBox newTagInput = new Java2sAutoComboBox( tags );
        newTagInput.setStrict( false );
        resultsModel.addNewTagInput( newTagInput );

        // Links

        JScrollPane linksInputScrollPane = new JScrollPane( linksInput );
        JButton linksClearButton = new JButton( "Clear" );
        linksClearButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e )
            {
                linksInput.setText( "" );
                linksInput.requestFocus();
            }
        } );

        JPanel linksPanel = new JPanel(
            new MigLayout( "", "[grow]", "[][grow][][]" ) );
        linksPanel.add( new JLabel( "Links" ), "span,wrap" );
        linksPanel.add( linksInputScrollPane, "span,grow,wrap" );
        linksPanel.add( linksClearButton, "align left" );
        linksPanel.add( linksImportButton, "align right" );

        // Keywords

        keywordsInput.setPreferredSize(
            new Dimension( keywordsInput.getMaximumSize().width, 
                           keywordsInput.getPreferredSize().height ) );
        keywordsInput.addKeyListener( new KeyListener() {
            public void keyPressed( KeyEvent e )
            {
                if( e.getKeyCode() == KeyEvent.VK_ENTER )
                    keywordsSearchButton.doClick();
            }

            public void keyReleased( KeyEvent e ) {}
            public void keyTyped( KeyEvent e ) {}
        } );

        JButton keywordsClearButton = new JButton( "Clear" );
        keywordsClearButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e )
            {
                keywordsInput.setText( "" );
                keywordsInput.requestFocus();
            }
        } );

        JPanel keywordsPanel = new JPanel( new MigLayout() );
        keywordsPanel.add( new JLabel( "Keywords" ), "span,wrap" );
        keywordsPanel.add( keywordsInput, "span,wrap" );
        keywordsPanel.add( keywordsClearButton, "align left" );
        keywordsPanel.add( keywordsSearchButton, "align right" );

        // Tags: managed by TagsPanel

        // Site

/*
        siteInput.setPreferredSize(
            new Dimension( siteInput.getMaximumSize().width, 
                           siteInput.getPreferredSize().height ) );

        JButton siteClearButton = new JButton( "Clear" );
        siteClearButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e )
            {
                siteInput.setText( "" );
                siteInput.requestFocus();
            }
        } );

        JPanel sitePanel = new JPanel( new MigLayout() );
        sitePanel.add( new JLabel( "Site" ), "span,wrap" );
        sitePanel.add( siteInput, "span,wrap" );
        sitePanel.add( siteClearButton, "align left" );
        sitePanel.add( siteScanButton, "align right" );
*/

        tabbedPane = new JTabbedPane();
        tabbedPane.add( "Search", keywordsPanel );
//        tabbedPane.add( "Site", sitePanel );
        tabbedPane.add( "Import", linksPanel );
        tabbedPane.add( "Tags", tagsPanel );

        tabbedPane.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent e )
            {
                int selectedIndex = tabbedPane.getSelectedIndex();

                if( selectedIndex == 0 )
                {
                    keywordsInput.requestFocus();
                    frame.setTitle( "Search - " + Stome.APP_TITLE );
                }
/*
                else if( selectedIndex == 1 )
                    siteInput.requestFocus();
*/
                else if( selectedIndex == 1 )
                {
                    linksInput.requestFocus();
                    frame.setTitle( "Import - " + Stome.APP_TITLE );
                }
                else if( selectedIndex == 2 )
                {
                    frame.setTitle( "Tags - " + Stome.APP_TITLE );
                }
            }
        } );

        // Results

        JLabel hoverUrlLabel = new JLabel();
        Font font = hoverUrlLabel.getFont().deriveFont( Font.PLAIN );
        hoverUrlLabel.setFont( font );
        setLabelSize( fetchStatusLabel );
        setLabelSize( hoverUrlLabel );

        resultsTable = new JTable( resultsModel )
        {
            private static final long serialVersionUID = 105;

            @Override public Component prepareRenderer(
                    TableCellRenderer tcr, int row, int column )
            {
                if( tcr == null )
                    return null;
                try
                {
                    Component c = super.prepareRenderer( tcr, row, column );
                    c.setForeground( getForeground() );

                    if( resultsTable.isRowSelected( row ) ) 
                        c.setBackground( selectedColor ); 
                    else
                        c.setBackground( ( row % 2 == 0 ) ?
                            evenColor : getBackground() );

                    return c;
                }
                catch( java.lang.NullPointerException ex ) {}
                return null;
            }
        };

        resultsTable.addMouseListener( new MouseAdapter()
        {
            public void mousePressed( MouseEvent e )
            {
                if( e.isPopupTrigger() )
                    openPopup( e );
            }

            public void mouseReleased( MouseEvent e )
            {
                if( e.isPopupTrigger() )
                    openPopup( e );
            }

            private void openPopup( MouseEvent e )
            {
                PopupMenu menu = new PopupMenu( frame, resultsTable );
                menu.show( e.getComponent(), e.getX(), e.getY() );
            }
        } );

        resultsTable.setIntercellSpacing( new Dimension() );
        resultsTable.setShowGrid( false );
        resultsTable.putClientProperty( "terminateEditOnFocusLost", Boolean.TRUE );

        HyperlinkRenderer renderer = new HyperlinkRenderer( hoverUrlLabel );
        resultsTable.setDefaultRenderer( Hyperlink.class, renderer );
        resultsTable.addMouseListener( renderer );
        resultsTable.addMouseMotionListener( renderer );

        TagRenderer tagRenderer = new TagRenderer();
        resultsTable.setDefaultRenderer( Tags.class, tagRenderer );

        TableColumnModel resultsColModel = resultsTable.getColumnModel();

        DefaultCellEditor cellEditor = new DefaultCellEditor( newTagInput );
        cellEditor.setClickCountToStart( 1 );
        resultsColModel.getColumn( 
            ResultsModel.NEW_TAG_COL ).setCellEditor( cellEditor );

        setColumnWidth( resultsColModel, ResultsModel.SHARES_COL,   70,  70, 120 );
        setColumnWidth( resultsColModel, ResultsModel.DOMAIN_COL,   60, 150, 250 );
        setColumnWidth( resultsColModel, ResultsModel.NEW_TAG_COL,  60, 150, 250 );
        setColumnWidth( resultsColModel, ResultsModel.TAGS_COL,     60, 200,  -1 );
        setColumnWidth( resultsColModel, ResultsModel.LINK_COL,    200, 400,  -1 );

        resultsTable.setAutoCreateRowSorter( true );
        resultsTable.setSelectionMode( 
            ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
        resultsTable.setRowSelectionAllowed( true );

        resultsTable.setFillsViewportHeight( true );
        JScrollPane resultsScrollPane = new JScrollPane( resultsTable );

        clearLinksButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e )
            {
                ResultsModel model = ( ResultsModel ) resultsTable.getModel();
                model.clearLinks();
            }
        } );

        JPanel resultsPane = 
            new JPanel( new MigLayout( "", "[grow]", "[][grow][]" ) );
        resultsPane.add( clearLinksButton, "align left" );
        resultsPane.add( fetchStatusLabel, "align right,wrap" );
        resultsPane.add( resultsScrollPane, "span,grow,wrap" );
        resultsPane.add( hoverUrlLabel, "span,grow" );

        JSplitPane sp = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
        sp.add( tabbedPane );
        sp.add( resultsPane );
        sp.setResizeWeight( 0.33d );
        
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.getContentPane().add( sp );

        frame.pack();
        frame.setSize( new Dimension( 640, 480 ) );

        frame.setVisible( true );
        keywordsInput.requestFocus();
    }

    private static boolean urlIsValid( String url )
    {
        String[] schemes = { "http", "https" };
        UrlValidator urlValidator = new UrlValidator( schemes );
        if( urlValidator.isValid( url  ) )
        {
            if( url.matches( "https?://webcache.googleusercontent.com/.*" ) ||
                url.matches( "https?://www.google.com/search\\?.*" ) )
                return false;
            return true;
        }
        return false;
    }

    private static void setLabelSize( JLabel label )
    {
        FontMetrics fm = label.getFontMetrics( label.getFont() );
        int fontHeight = fm.getAscent() + fm.getDescent() + fm.getLeading();
        label.setMinimumSize(
            new Dimension( label.getPreferredSize().width, fontHeight ) );
    }

    public static void setColumnWidth(
        TableColumnModel colModel, int column,
        int minWidth, int preferredWidth, int maxWidth )
    {
        if( minWidth >= 0 )
            colModel.getColumn( column ).setMinWidth( minWidth );
        if( preferredWidth >= 0 )
            colModel.getColumn( column ).setPreferredWidth( preferredWidth );
        if( maxWidth >= 0 )
            colModel.getColumn( column ).setMaxWidth( maxWidth );
    }

    public static String getHTMLContent(
        String url, int timeoutMilliseconds, String referer )
    {
        String content = "";
        try
        {
            URL urlObj = new URL( url );
            URLConnection conn = urlObj.openConnection();
            conn.setRequestProperty( "Referer", referer );
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
