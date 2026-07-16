package com.local.monitor;

import java.awt.event.MouseWheelEvent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;

final class ReadableTableScrollPane extends JScrollPane {
    ReadableTableScrollPane(JTable table) {
        super(table);
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_AS_NEEDED);
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
    }

    @Override
    protected void processMouseWheelEvent(MouseWheelEvent event) {
        JScrollBar horizontal = getHorizontalScrollBar();
        if (event.isShiftDown()
                && horizontal.isVisible()
                && horizontal.getMaximum() > horizontal.getVisibleAmount()) {
            int direction = event.getWheelRotation() < 0 ? -1 : 1;
            int distance = Math.max(1, Math.abs(event.getUnitsToScroll()))
                    * horizontal.getUnitIncrement(direction);
            horizontal.setValue(horizontal.getValue() + direction * distance);
            event.consume();
            return;
        }
        super.processMouseWheelEvent(event);
    }
}
