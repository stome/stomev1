import java.awt.*;
import javax.swing.*;

class ScrollablePanel extends JPanel implements Scrollable
{
    public ScrollablePanel( LayoutManager layout )
    {
        super.setLayout( layout );
    }

    @Override public boolean getScrollableTracksViewportHeight()
    {
        return true;
    }

    @Override public boolean getScrollableTracksViewportWidth()
    {
        return false;
    }

    @Override public Dimension getPreferredScrollableViewportSize()
    {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }

    @Override public int getScrollableBlockIncrement(
        Rectangle visibleRect, int orientation, int direction )
    {
        return 25;
    }

    @Override public int getScrollableUnitIncrement(
        Rectangle visibleRect, int orientation, int direction )
    {
        return 5;
    }
}
