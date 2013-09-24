import javax.swing.table.*;

public class TagsModel extends DefaultTableModel
{
    public static final int NAME_COL  = 0;
    public static final int COUNT_COL = 1;

    public static final int SORT_ASC  = 0;
    public static final int SORT_DESC = 1;

    private static final long serialVersionUID = 106;

    private static final ColumnContext[] columnArray =
    {
        new ColumnContext( "Name",  String.class,  false ),
        new ColumnContext( "Count", Integer.class, false )
    };

    @Override public boolean isCellEditable( int row, int col )
    {
        return columnArray[ col ].isEditable;
    }

    @Override public Class<?> getColumnClass( int modelIndex )
    {
        return columnArray[ modelIndex ].columnClass;
    }

    @Override public int getColumnCount()
    {
        return columnArray.length;
    }

    @Override public String getColumnName( int modelIndex )
    {
        return columnArray[ modelIndex ].columnName;
    }

    private static class ColumnContext
    {
        public final String   columnName;
        public final Class<?> columnClass;
        public final boolean  isEditable;
        public ColumnContext( String columnName, Class<?> columnClass,
                              boolean isEditable )
        {
            this.columnName  = columnName;
            this.columnClass = columnClass;
            this.isEditable  = isEditable;
        }
    }

    // Sorts table by sortCol and sortOrder

    public void addRow( String tag, Integer tagCount, int sortCol, int sortOrder )
    {
        if( getRowCount() == 0 )
        {
            addRow( new Object[] { tag, tagCount } );
        }
        else
        {
            boolean added = false;
            for( int i = 0; i < getRowCount(); i++ )
            {
                String t = (String) getValueAt( i, TagsModel.NAME_COL );
                Integer c = (Integer) getValueAt( i, TagsModel.COUNT_COL );

                if( ( sortCol == TagsModel.NAME_COL &&
                      sortOrder == TagsModel.SORT_ASC &&
                      tag.compareTo( t ) <= 0 ) ||
                    ( sortCol == TagsModel.NAME_COL &&
                      sortOrder == TagsModel.SORT_DESC &&
                      tag.compareTo( t ) > 0 ) ||
                    ( sortCol == TagsModel.COUNT_COL &&
                      sortOrder == TagsModel.SORT_ASC &&
                      tagCount.compareTo( c ) <= 0 ) ||
                    ( sortCol == TagsModel.COUNT_COL &&
                      sortOrder == TagsModel.SORT_DESC &&
                      tagCount.compareTo( c ) > 0 ) )
                {
                    insertRow( i, new Object[] { tag, tagCount } );
                    added = true;
                    break;
                }
            }

            if( ! added )
                insertRow( getRowCount(), new Object[] { tag, tagCount } );
        }
    }
}
