import net.miginfocom.swing.MigLayout;

import java.util.HashMap;
import java.util.TreeSet;

import java.awt.Toolkit;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
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

class PopupMenu extends JPopupMenu
{
    private static final long serialVersionUID = 102;

    public PopupMenu( JFrame frame, final JTable table )
    {
        int rowCount = table.getRowCount();

//        JMenuItem openLinks      = new JMenuItem( "Open Links" );
        JMenuItem copyLinks      = new JMenuItem( "Copy Links" );
        JMenuItem copyTitles     = new JMenuItem( "Copy Titles" );
        JMenuItem clearSelection = new JMenuItem( "Clear Selection" );
        JMenuItem editTitles     = new JMenuItem( "Edit Titles" );
        JMenuItem addTag         = new JMenuItem( "Add Tag" );
        JMenuItem deleteTag      = new JMenuItem( "Delete Tag" );

        if( rowCount == 0 )
        {
            copyLinks.setEnabled( false );
            copyTitles.setEnabled( false );
            clearSelection.setEnabled( false );
            editTitles.setEnabled( false );
            deleteTag.setEnabled( false );
            addTag.setEnabled( false );

        }

        CopyListener copyListener = new CopyListener( frame, table );
        copyLinks.addActionListener( copyListener );
        copyTitles.addActionListener( copyListener );

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
        addSeparator();
        add( clearSelection );
        addSeparator();
        add( editTitles );
        add( deleteTag );
        add( addTag );
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
        String newLine = String.format( "%n" );

        String value = "";
        int selectedRowCount = table.getSelectedRowCount();

        String command = e.getActionCommand();
        for( int i = 0; i < table.getRowCount(); i++ )
        {
            if( selectedRowCount == 0 || table.isRowSelected( i ) )
            {
                Hyperlink link = 
                    (Hyperlink) table.getValueAt( i, ResultsModel.LINK_COL );
                if( command.equals( "Copy Links" ) )
                    value += link.getURL().toString() + newLine;
                else if( command.equals( "Copy Titles" ) && 
                         link.getTitle() != null )
                    value += link.getTitle() + newLine;
            }
        }

        StringSelection stringSelection = new StringSelection( value );
        Clipboard clipboard = 
            Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents( stringSelection, null );
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
                Hyperlink link = (Hyperlink) table.getValueAt(
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

    public ModifyTagListener( JFrame frame, JTable table )
    {
        this.frame = frame;
        this.table = table;
        resultsModel = (ResultsModel) table.getModel();
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

    public void actionPerformed( ActionEvent e )
    {
        final String command = e.getActionCommand();

        final JDialog dialog = new JDialog( frame, command, true );
        final JComboBox<String> modifyTagSelector = 
            new JComboBox<String>( getSelectedTags() );
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

                int selectedRowCount = table.getSelectedRowCount();
                for( int i = 0; i < table.getRowCount(); i++ )
                {
                    if( selectedRowCount == 0 || table.isRowSelected( i ) )
                    {
                        int mIndex = table.convertRowIndexToModel( i );
                        if( command.equals( "Add Tag" ) )
                            resultsModel.addLinkTag( selectedTag, mIndex );
                        else if( command.equals( "Delete Tag" ) )
                            resultsModel.deleteLinkTag( selectedTag, mIndex );
                    }
                }

                modifyTagSelector.removeItem( selectedTag );
                if( modifyTagSelector.getItemCount() == 0 )
                    dialog.dispose();
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
}
