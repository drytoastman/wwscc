/*
 * Created on 25.02.2011
 *
 */
package org.wwscc.util;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * Collection of utility methods for J/X/Table.
 * 
 * @author Jeanette Winzenburg, Berlin
 */
public class TableUtilities {

    /**
     * Returns the preferred height for the given row. It loops
     * across all visible columns and returns the maximal pref height of
     * the rendering component. Falls back to the table's base rowheight, i
     * f there are no columns or the renderers
     * max is zeor.<p>
     * 
     * @param table the table which provides the renderers, must not be null
     * @param row the index of the row in view coordinates
     * @return the preferred row height of
     * @throws NullPointerException if table is null.
     * @throws IndexOutOfBoundsException if the row is not a valid row index
     */
    public static int getPreferredRowHeight(JTable table, int row) {
        int pref = 0;
        for (int column = 0; column < table.getColumnCount(); column++) {
            TableCellRenderer renderer = table.getCellRenderer(row, column);
            Component comp = table.prepareRenderer(renderer, row, column);
            pref = Math.max(pref, comp.getPreferredSize().height);
        }
        return pref > 0 ? pref : table.getRowHeight();
    }
    
    /**
     * 
     * @param table the table which provides the renderers, must not be null
     * @param row the index of the row in view coordinates
     * @throws NullPointerException if table is null.
     * @throws IndexOutOfBoundsException if the row is not a valid row index
     */
    public static void setPreferredRowHeight(JTable table, int row) {
        int prefHeight = getPreferredRowHeight(table, row);
        table.setRowHeight(row, prefHeight);
    }

    /**
     * Sets preferred row heights for all visible rows. 
     * 
     * @param table the table to set row heights to
     * @throws NullPointerException if no table installed.
     */
    public static void setPreferredRowHeights(JTable table) {
        // care about visible rows only
        for (int row = 0; row < table.getRowCount(); row++) {
            setPreferredRowHeight(table, row);
        }
    }
    
    private TableUtilities() {}
}
