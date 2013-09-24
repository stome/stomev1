import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import net.miginfocom.swing.MigLayout;

class TagsList extends JPanel
{
    private JLabel    tagsLabel = null;
    private JTable    tagsTable = null;
    private TagsModel tagsModel = null;
    private TagsPanel tagsPanel = null;

    private Tags selectedTags = null;
    private Tags listTags     = null;

    private String tagName = null;

    private JScrollPane tableScrollPane = null;
    private Java2sAutoTextField tagSelector = null;

    public TagsList(
        String tagName, Tags selectedTags, Tags listTags, TagsModel tagsModel, 
        TagsPanel tagsPanel )
    {
        tagsLabel = new JLabel( tagName + ":" );
        this.tagName      = tagName;
        this.selectedTags = selectedTags;
        this.listTags     = listTags;
        this.tagsModel    = tagsModel;
        this.tagsPanel    = tagsPanel;

        tagsTable = new JTable( tagsModel );
        tagsTable.setFillsViewportHeight( true );

        tagsTable.setAutoCreateRowSorter( true );
        tagsTable.setShowGrid( false );
        tagsTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        tagsTable.setRowSelectionAllowed( true );

        ListSelectionModel selectionModel = tagsTable.getSelectionModel();
        selectionModel.addListSelectionListener( new ListSelectionListener()
        {
            public void valueChanged( ListSelectionEvent e )
            {
                if( ! e.getValueIsAdjusting() )
                {
                    TagsList.this.tagsPanel.deleteTagsLists(
                        TagsList.this.selectedTags.size() + 1 );

                    int[] selection = tagsTable.getSelectedRows();
                    if( selection.length > 0 )
                    {
                        String tagName = (String) tagsTable.getValueAt(
                            selection[ 0 ], TagsModel.NAME_COL );
                        Tags st = (Tags) TagsList.this.selectedTags.clone();
                        st.add( tagName );
                        TagsList.this.tagsPanel.createTagsList(
                            tagName, st, TagsList.this.listTags );
                    }
                }
            }
        } );

        TableColumnModel tagColModel = tagsTable.getColumnModel();
        Stome.setColumnWidth( tagColModel, TagsModel.NAME_COL,  70, 150, 500 );
        Stome.setColumnWidth( tagColModel, TagsModel.COUNT_COL, 70,  70, 120 );

        tagSelector = new Java2sAutoTextField( listTags );
        tagSelector.setPreferredSize(
            new Dimension( tagSelector.getMaximumSize().width,
                           tagSelector.getPreferredSize().height ) );
        tagSelector.addKeyListener( new KeyListener()
        {
            public void keyPressed( KeyEvent e )
            {
                if( e.getKeyCode() == KeyEvent.VK_ENTER )
                    select( null );
            }

            public void keyReleased( KeyEvent e ) {}
            public void keyTyped( KeyEvent e ) {}
        } );

        tableScrollPane = new JScrollPane( tagsTable );

        setLayout( new MigLayout( "", "[]", "[][grow]" ) );
        add( tagsLabel, "" );
        add( tagSelector, "wrap" );
        add( tableScrollPane, "span,grow" );
    }

    @Override public Dimension getPreferredSize()
    {
        return new Dimension( 300, 
                Toolkit.getDefaultToolkit().getScreenSize().height );
    }

    @Override public String toString()
    {
        return tagName;
    }

    public Tags getSelectedTags()
    {
        return selectedTags;
    }

    public void focusSelector()
    {
        tagSelector.requestFocus();
    }

    public void select( String tag )
    {
        if( tag != null )
            tagSelector.setText( tag );

        tagsTable.clearSelection();
        Stome.scrollToVisible( tagsTable, 0, TagsModel.NAME_COL );
        if( ! tagSelector.getText().equals( "" ) )
        {
            for( int i = 0; i < tagsTable.getRowCount(); i++ )
            {
                String tagName = (String) tagsTable.getValueAt(
                    i, TagsModel.NAME_COL );
                if( tagName.matches( "^" + 
                        tagSelector.getText() + ".*" ) &&
                    ! tagsTable.isRowSelected( i ) )
                {
                    tagsTable.addRowSelectionInterval( i, i );
                    Stome.scrollToVisible(
                        tagsTable, i, TagsModel.NAME_COL );
                    break;
                }
            }
        }
    }
}
