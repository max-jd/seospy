package seospy.max_jd.seo.tableHelpers;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class CustomizedDefaultTableCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        int indexView = table.getRowSorter().convertRowIndexToModel(row);
        Component cellRendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, indexView, column);

        if (isSelected) {
            return cellRendererComponent;
        }

        if (table.getModel().getValueAt(indexView, 13).equals("true")) {
            cellRendererComponent.setBackground(Color.RED);
        } else {
            cellRendererComponent.setBackground(Color.WHITE);
        }
        return cellRendererComponent;
    }

}