package seospy.max_jd.seo.tableHelpers;

import javax.swing.table.DefaultTableModel;

public class CustomizedDefaultTableModel extends DefaultTableModel {

   public CustomizedDefaultTableModel(Object[][] rows, Object[] columns) {
        super(rows, columns);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public Class getColumnClass(int column) {
        //for int return Integer, otherwise String
        switch(column) {
            case 0:
            case 3:
            case 7:
            case 10:
            case 11:
            case 12:
                return Integer.class;
            default:
                return String.class;
        }
    }

}