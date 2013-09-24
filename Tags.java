import java.util.*;
import java.util.regex.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

class Tags extends ArrayList<String>
{
    private boolean sorted = true;

    public Tags()
    {
    }

    public Tags( boolean sorted )
    {
        this.sorted = sorted;
    }

    // Sorts list alphabetically

    @Override public boolean add( String tagName )
    {
        if( ! sorted )
            return super.add( tagName );

        if( size() == 0 )
            return super.add( tagName );

        for( int i = 0; i < size(); i++ )
        {
            if( tagName.compareTo( (String) get( i ) ) <= 0 )
            {
                add( i, tagName );
                return true;
            }
        }
        add( size(), tagName );
        return true;
    }

    @Override public String toString()
    {
        String tagsList = "";
        for( int i = 0; i < size(); i++ )
        {
            if( i < size() - 1 )
                tagsList += get( i ) + ", ";
            else
                tagsList += get( i );
        }
        return tagsList;
    }
}

class TagRenderer extends DefaultTableCellRenderer
{
    private static final long serialVersionUID = 107;

    @Override public Component getTableCellRendererComponent(
        JTable table, Object value, 
        boolean isSelected, boolean hasFocus,
        int row, int column )
    {
        JLabel c = (JLabel) super.getTableCellRendererComponent(
            table, value, isSelected, false, row, column);

        String tags = ( (Tags) value ).toString();

        ArrayList<String> matchList = new ArrayList<String>();
        Pattern regex = Pattern.compile( ".{1,100}(?:\\s|$)", Pattern.DOTALL );
        Matcher regexMatcher = regex.matcher( tags );
        while( regexMatcher.find() )
            matchList.add( regexMatcher.group() );

        String htmlTags = "<html>";
        for( String s : matchList )
            htmlTags += s + "<br/>";
        htmlTags += "</html>";

        c.setToolTipText( htmlTags );

        return c;
    }
}
