package seospy.max_jd.seo.tableHelpers;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

@Deprecated
public class IntegerDefaultTableCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        int indexViewRow = table.getRowSorter().convertRowIndexToModel(row);
        Component cellRendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, indexViewRow, column);

        if (isSelected) {
            return cellRendererComponent;
        }

        if (table.getModel().getValueAt(indexViewRow, 13).equals("true")) {
            cellRendererComponent.setBackground(Color.RED);
        } else {
            cellRendererComponent.setBackground(Color.WHITE);
        }
        return cellRendererComponent;
    }

}