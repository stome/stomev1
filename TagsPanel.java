import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import net.miginfocom.swing.MigLayout;

public class TagsPanel extends JPanel
{
    private ResultsModel resultsModel = null;
    private ScrollablePanel tagsListsPanel = null;

    private JScrollPane tagsScrollPane = null;

    private Tags selectedTags = null;

    public TagsPanel( ResultsModel resultsModel, JButton tagsViewButton )
    {
        this.resultsModel = resultsModel;

        tagsListsPanel = new ScrollablePanel( new MigLayout( "", "", "" ) );
        createRootTagsList();

        tagsScrollPane = new JScrollPane( tagsListsPanel );
        tagsScrollPane.setPreferredSize(
            Toolkit.getDefaultToolkit().getScreenSize() );

        JButton tagsClearButton = new JButton( "Clear" );
        tagsClearButton.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                clear();
            }
        } );

        setLayout( new MigLayout( "", "[grow]", "[grow][]" ) );
        add( tagsScrollPane, "span,grow,wrap" );
        add( tagsClearButton, "align left" );
        add( tagsViewButton, "align right" );
    }

    public void createRootTagsList()
    {
        createTagsList( "ALL", new Tags( false ), resultsModel.getAllTags() );
    }

    public void createTagsList(
        String tagListName, Tags selectedTags, Tags parentTags )
    {
        // Create and populate tagsModel and listTags
        TagsModel tagsModel = new TagsModel();
        Tags listTags = new Tags();
        listTags.add( 0, "" );

        for( int i = 0; i < parentTags.size(); i++ )
        {
            String tagName = parentTags.get( i );
            if( ! tagName.equals( "" ) )
            {
                Integer tagCount = resultsModel.getTagCount( tagName, selectedTags );
                if( tagCount.intValue() > 0 )
                {
                    tagsModel.addRow(
                        tagName, tagCount, TagsModel.COUNT_COL, TagsModel.SORT_DESC );
                    listTags.add( tagName );
                }
            }
        }

        // Create TagsList
        if( listTags.size() > 1 )
        {
            TagsList tl = 
                new TagsList( tagListName, selectedTags, listTags, tagsModel, this );
            tagsListsPanel.add( tl, "" );
            validate();
            tl.focusSelector();
        }

        this.selectedTags = selectedTags;
    }

    public void clear()
    {
        Tags selectedTags = this.selectedTags;
        deleteTagsLists( 0 );
        createRootTagsList();
        this.selectedTags = selectedTags;
    }

    // Removes TagLists with selectedTags.size() greater than or equal to 
    // selectedTagCount

    public void deleteTagsLists( int selectedTagCount )
    {
        Component[] components = tagsListsPanel.getComponents();
        for( int i = 0; i < components.length; i++ )
        {
            TagsList tl = (TagsList) components[ i ];
            int size = tl.getSelectedTags().size();
            if( size >= selectedTagCount )
            {
                tagsListsPanel.remove( tl );
            }
            else if( size == ( selectedTagCount - 1 ) )
                selectedTags = tl.getSelectedTags();
        }
        validate();
    }

    @Override public String toString()
    {
        return selectedTags.toString();
    }

    public Tags getSelectedTags()
    {
        return selectedTags;
    }

    public void select( String[] tags )
    {
        for( int i = 0; i < tags.length; i++ )
        {
            Component[] components = tagsListsPanel.getComponents();
            TagsList tl = (TagsList) components[ i ];
            tl.select( tags[ i ] );
        }
    }
}
