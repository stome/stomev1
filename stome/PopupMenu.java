package stome;

import net.miginfocom.swing.MigLayout;

import java.util.HashMap;
import java.util.TreeSet;

import java.awt.Toolkit;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.event.WindowEvent;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;

import javax.swing.JRootPane;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JComboBox;
import javax.swing.KeyStroke;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.SwingConstants;
import javax.swing.JFileChooser;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

class PopupMenu extends JPopupMenu
{
    private static final long serialVersionUID = 102;

    public PopupMenu( JFrame frame, final JTable table )
    {
        int rowCount = table.getRowCount();

//        JMenuItem openLinks      = new JMenuItem( "Open Links" );
        JMenuItem copyLinks          = new JMenuItem( "Copy Links" );
        JMenuItem copyTitles         = new JMenuItem( "Copy Titles" );
        JMenuItem exportSelectionXLS = new JMenuItem( "Export Selection to XLS" );
        JMenuItem clearSelection     = new JMenuItem( "Clear Selection" );
        JMenuItem editTitles         = new JMenuItem( "Edit Titles" );
        JMenuItem addTag             = new JMenuItem( "Add Tag" );
        JMenuItem deleteTag          = new JMenuItem( "Delete Tag" );

        if( rowCount == 0 )
        {
            copyLinks.setEnabled( false );
            copyTitles.setEnabled( false );
            exportSelectionXLS.setEnabled( false );
            clearSelection.setEnabled( false );
            editTitles.setEnabled( false );
            addTag.setEnabled( false );
            deleteTag.setEnabled( false );
        }

        CopyListener copyListener = new CopyListener( frame, table );
        copyLinks.addActionListener( copyListener );
        copyLinks.setAccelerator( ( KeyStroke.getKeyStroke( 
            KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK ) ) );
        copyTitles.addActionListener( copyListener );

        ExportListener exportListener = new ExportListener( frame, table );
        exportSelectionXLS.addActionListener( exportListener );

        clearSelection.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e )
            {
                table.clearSelection();
            }
        } );

        editTitles.addActionListener( new EditTitlesListener( frame, table ) );
        addTag.addActionListener( new ModifyTagListener( frame, table ) );
        deleteTag.addActionListener( new ModifyTagListener( frame, table ) );

        add( copyLinks );
        add( copyTitles );
        add( exportSelectionXLS );
        addSeparator();
        add( clearSelection );
        addSeparator();
        add( editTitles );
        add( addTag );
        add( deleteTag );
    }

    public static void installEscapeCloseOperation( final JDialog dialog )
    { 
        final KeyStroke escapeStroke = 
            KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ); 
        final String dispatchWindowClosingActionMapKey = 
            "com.spodding.tackline.dispatch:WINDOW_CLOSING"; 

        Action dispatchClosing = new AbstractAction() { 
            private static final long serialVersionUID = 103;

            public void actionPerformed(ActionEvent event) { 
                dialog.dispatchEvent(new WindowEvent( 
                    dialog, WindowEvent.WINDOW_CLOSING 
                )); 
            } 
        }; 
        JRootPane root = dialog.getRootPane(); 
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put( 
            escapeStroke, dispatchWindowClosingActionMapKey 
        ); 
        root.getActionMap().put(
            dispatchWindowClosingActionMapKey, dispatchClosing ); 
    }
}

class ExportListener implements ActionListener
{
    private JFrame frame = null;
    private JTable table = null;

    public ExportListener( JFrame frame, JTable table )
    {
        this.frame = frame;
        this.table = table;
    }

    public void actionPerformed( ActionEvent e )
    {
        File ssFile = new File( "Stome Selection.xls" );

        JFileChooser jfs = new JFileChooser();
        jfs.setSelectedFile( ssFile );
        if( jfs.showSaveDialog( frame ) == JFileChooser.APPROVE_OPTION )
            ssFile = jfs.getSelectedFile();
        else
            return;

        Workbook wb = new HSSFWorkbook();
        CreationHelper createHelper = wb.getCreationHelper();
        Sheet sheet = wb.createSheet( "Sheet 1" );

        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBoldweight( Font.BOLDWEIGHT_BOLD );
        style.setFont( font );

        CellStyle hlink_style = wb.createCellStyle();
        Font hlink_font = wb.createFont();
        hlink_font.setUnderline( Font.U_SINGLE );
        hlink_font.setColor( IndexedColors.BLUE.getIndex() );
        hlink_style.setFont( hlink_font );

        int selectedRowCount = table.getSelectedRowCount();
        int j = 0;

        String command = e.getActionCommand();
        for( int i = 0; i < table.getRowCount(); i++ )
        {
            if( selectedRowCount == 0 || table.isRowSelected( i ) )
            {
                Row row = sheet.createRow( ( short ) j );

                String shares = ( (Rating) table.getValueAt( i, ResultsModel.SHARES_COL ) ).toString();
                String tags   = ( (Tags) table.getValueAt( i, ResultsModel.TAGS_COL ) ).toString();
                stome.Hyperlink slink = 
                    (stome.Hyperlink) table.getValueAt( i, ResultsModel.LINK_COL );

                String url = slink.getURL().toString();
                String title = slink.getTitle();
                if( title == null || title.matches( "^\\s*$" ) )
                    title = url;
                org.apache.poi.ss.usermodel.Hyperlink link = 
                    createHelper.createHyperlink( org.apache.poi.ss.usermodel.Hyperlink.LINK_URL );
                link.setAddress( url );

                Cell c0 = row.createCell( ( short ) 0 );
                c0.setCellValue( shares );

                Cell c1 = row.createCell( ( short ) 1 );
                c1.setCellValue( tags );

                Cell c2 = row.createCell( ( short ) 2 );
                c2.setCellValue( title );
                c2.setHyperlink( link );
                c2.setCellStyle( hlink_style );

                j++;
            }
        }

        // Write spreadsheet to file
        try
        {
            FileOutputStream fileOut = new FileOutputStream( ssFile );
            wb.write( fileOut );
            fileOut.close();
        }
        catch( FileNotFoundException ex )
        {
            // Shouldn't happen
            ex.printStackTrace();
        }
        catch( IOException ex )
        {
            ex.printStackTrace();
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }
}

class CopyListener implements ActionListener
{
    private JFrame frame = null;
    private JTable table = null;

    public CopyListener( JFrame frame, JTable table )
    {
        this.frame = frame;
        this.table = table;
    }

    public void actionPerformed( ActionEvent e )
    {
        Stome.copyToClipboard( table, e.getActionCommand() );
    }
}

class EditTitlesListener implements ActionListener
{
    private JFrame frame = null;
    private JTable table = null;
    private ResultsModel resultsModel = null;

    public EditTitlesListener( JFrame frame, JTable table )
    {
        this.frame = frame;
        this.table = table;
        resultsModel = (ResultsModel) table.getModel();
    }

    public void actionPerformed( ActionEvent e )
    {
        JPanel panel = new JPanel( new MigLayout( "", "", "" ) );
        JSeparator separator = new JSeparator( SwingConstants.HORIZONTAL );

        int selectedRowCount = table.getSelectedRowCount();

        for( int i = 0; i < table.getRowCount(); i++ )
        {
            if( selectedRowCount == 0 || table.isRowSelected( i ) )
            {
                stome.Hyperlink link = (stome.Hyperlink) table.getValueAt(
                    i, ResultsModel.LINK_COL );

                final String url = link.getURL().toString();

                JLabel linkLabel = new JLabel( url );
                final JTextField titleInput = new JTextField( link.getTitle(), 50 );

                JButton fetchButton = new JButton( "Fetch" );
                fetchButton.addActionListener( new ActionListener() {
                    public void actionPerformed( ActionEvent e )
                    {
                        String title = resultsModel.fetchTitle( url );
                        titleInput.setText( title );
                    }
                } );

                JButton updateButton = new JButton( "Update" );
                updateButton.addActionListener( new ActionListener() {
                    public void actionPerformed( ActionEvent e )
                    {
                        String newTitle = titleInput.getText();
                        resultsModel.updateTitle( url, newTitle );
                    }
                } );

                JPanel buttonsPanel = new JPanel( new FlowLayout() );
                buttonsPanel.add( fetchButton );
                buttonsPanel.add( updateButton );

                panel.add( linkLabel,    "wrap" );
                panel.add( titleInput,   "grow,wrap" );
                panel.add( buttonsPanel, "wrap" );
                panel.add( separator,    "wrap" );
            }
        }

        JScrollPane scrollpane = new JScrollPane( panel );
        JDialog dialog = new JDialog( frame, "Edit Titles", true );

        PopupMenu.installEscapeCloseOperation( dialog );
        
        dialog.getContentPane().add( scrollpane );
        dialog.pack();
        dialog.setVisible( true );
    }

}

class ModifyTagListener implements ActionListener
{
    private JFrame frame = null;
    private JTable table = null;
    private ResultsModel resultsModel = null;
    private JComboBox<Object> modifyTagSelector = null;

    public ModifyTagListener( JFrame frame, JTable table )
    {
        this.frame = frame;
        this.table = table;
        resultsModel = (ResultsModel) table.getModel();
    }

    public void actionPerformed( ActionEvent e )
    {
        final String command = e.getActionCommand();

        final JDialog dialog = new JDialog( frame, command, true );
        if( command.equals( "Add Tag" ) )
        {
            modifyTagSelector = new Java2sAutoComboBox( resultsModel.getAllTags() );
            ( (Java2sAutoComboBox) modifyTagSelector ).setStrict( false );
        }
        else if( command.equals( "Delete Tag" ) )
        {
            modifyTagSelector = new JComboBox<Object>( getSelectedTags() );
        }
        modifyTagSelector.setPrototypeDisplayValue( "XXXXXXXXXXXXXXXX" );

        JButton modifyButton = null;
        if( command.equals( "Add Tag" ) )
            modifyButton = new JButton( "Add" );
        else if( command.equals( "Delete Tag" ) )
            modifyButton = new JButton( "Delete" );

        modifyButton.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                String selectedTag = (String) modifyTagSelector.getSelectedItem();

                // Populate array with list of selected indices

                int j = 0;
                int[] selectedIndices = new int[ table.getRowCount() ];

                int selectedRowCount = table.getSelectedRowCount();
                for( int i = 0; i < table.getRowCount(); i++ )
                {
                    if( selectedRowCount == 0 || table.isRowSelected( i ) )
                    {
                        int mIndex = table.convertRowIndexToModel( i );

                        selectedIndices[ j++ ] = mIndex;
                    }
                }

                if( j < selectedIndices.length )
                    selectedIndices[ j ] = -1;
                if( command.equals( "Add Tag" ) )
                    resultsModel.addLinkTags( selectedTag, selectedIndices );
                else if( command.equals( "Delete Tag" ) )
                    resultsModel.deleteLinkTags( selectedTag, selectedIndices );

                if( command.equals( "Delete Tag" ) )
                {
                    modifyTagSelector.removeItem( selectedTag );
                    if( modifyTagSelector.getItemCount() == 0 )
                        dialog.dispose();
                }
            }
        } );

        JPanel panel = new JPanel( new MigLayout( "", "", "" ) );
        panel.add( modifyTagSelector );
        panel.add( modifyButton );

        JScrollPane scrollpane = new JScrollPane( panel );

        PopupMenu.installEscapeCloseOperation( dialog );
        
        dialog.getContentPane().add( scrollpane );
        dialog.pack();
        dialog.setVisible( true );
    }

    private String[] allTags()
    {
        Tags allTags = resultsModel.getAllTags();

        String[] allTagsArray = allTags.toArray( new String[ allTags.size() ] );
        return allTagsArray;
    }

    private String[] getSelectedTags()
    {
        HashMap<String,Boolean> selectedTagsHash = new HashMap<String,Boolean>();

        int selectedRowCount = table.getSelectedRowCount();
        for( int i = 0; i < table.getRowCount(); i++ )
        {
            if( selectedRowCount == 0 || table.isRowSelected( i ) )
            {
                Tags tags = (Tags) table.getValueAt( i, ResultsModel.TAGS_COL );

                for( int j = 0; j < tags.size(); j++ )
                    selectedTagsHash.put( tags.get( j ), new Boolean( true ) );
            }
        }

        TreeSet<String> selectedTagsSet = new TreeSet<String>();
        selectedTagsSet.addAll( selectedTagsHash.keySet() );

        String[] selectedTags = 
            selectedTagsSet.toArray( new String[ selectedTagsSet.size() ] );
        return selectedTags;
    }
}
