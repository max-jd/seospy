package webspy.max_jd.seo;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import org.apache.commons.validator.routines.UrlValidator;
import webspy.max_jd.utils.ReadWrite;
import webspy.max_jd.utils.RoutineWorker;
import webspy.max_jd.utils.interfaces.Routine;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebSpy extends JFrame {
    private List<URL> pages;
    private Spider spider;
    private Deque<SeoUrl> dequeSeoUrls;
    private Set<SeoUrl> imagesSeoUrls;

    private TunnerSeoURL tunner;
    private SeoUrlValidator validator;

    public static final org.apache.log4j.Logger logToFile =
            org.apache.log4j.Logger.getLogger(WebSpy.class.getName());

    private JProgressBar progressBarUI;
    private JDialog progressDialog;
    private JMenu mainMenu;
    private JPanel kitButtonsPanel;
    private JTable mainTable;
    private JTabbedPane tabs = new JTabbedPane();
    private JButton playButton;
    private JButton pauseButton;
    private JButton stopButton;
    private JMenuItem saveProjectItem;
    private JMenuItem exportMenuItem;
    private JTextField inputMainPageToStart;
    private ReadWrite rw = new ReadWrite();

    //need for transfer top menu among tabs. equals 0 by default
    private int tabPreviousIndex;
    private int tabCurrentIndex;

    private volatile StateSEOSpy state = StateSEOSpy.NOT_RUN_YET;
    //The lock for waking up thread for updating the table concurrently with scanning
    private final Object lock = new Object();

    private void createProgressDialog() {
        progressDialog = new JDialog(this,true);
        progressDialog.setLocationRelativeTo(this);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setBorderPainted(true);

        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
        progressPanel.add(new JLabel("Please, wait. Your request is in processing..."));
        progressPanel.add(progressBar);

        progressDialog.setContentPane(progressPanel);
        progressDialog.pack();
        progressDialog.setResizable(false);
    }

    private void showModalDialogProgress(RoutineWorker worker) {
        JDialog progressDialog = new JDialog(this, "", Dialog.ModalityType.APPLICATION_MODAL);

        //expose if the work was done
        worker.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                if(propertyChangeEvent.getPropertyName().equals("state")) {
                    if(propertyChangeEvent.getNewValue() == SwingWorker.StateValue.DONE) {
                        progressDialog.dispose();
                        updateTable();
                        mainMenu.getItem(0).setEnabled(true);// it does exportMenuItem.setEnabled(true);
                    }
                }
            }
        });

        worker.execute();

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setBorderPainted(true);

        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
        progressPanel.add(new JLabel("Please, wait. Your request is in processing..."));
        progressPanel.add(progressBar);
        progressDialog.setLocationRelativeTo(this);
        progressDialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        progressDialog.setContentPane(progressPanel);
        progressDialog.pack();
        progressDialog.setResizable(false);
        progressDialog.setVisible(true);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        mainMenu = new JMenu("Menu");
        JMenu subMenuLoadAndSaveProject = new JMenu("Project...");
        saveProjectItem = new JMenuItem("Save");
        saveProjectItem.setEnabled(false);
        JMenuItem loadProjectItem = new JMenuItem("Load");
        subMenuLoadAndSaveProject.add(saveProjectItem);
        subMenuLoadAndSaveProject.add(loadProjectItem);
        saveProjectItem.addActionListener((e) -> saveProject());
        loadProjectItem.addActionListener((e) -> loadProject());

        exportMenuItem = new JMenuItem("Export to excel");
        exportMenuItem.setEnabled(false);

        //export the table to a excel file
        exportMenuItem.addActionListener( (ActionEvent actionEvent) -> {

            JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
            chooser.setFileFilter(
                    new FileNameExtensionFilter(rw.getExtensionExport(), rw.getExtensionExport().replaceFirst(".", "")));

            int state = chooser.showSaveDialog(this);
            if(state == JFileChooser.APPROVE_OPTION) {
                final File chosenFile;
                //we need a effective final variable for the lambda
                if(! chooser.getSelectedFile().toString().toLowerCase().endsWith(rw.getExtensionExport())) {
                    chosenFile = new File(chooser.getSelectedFile().toString() + rw.getExtensionExport());
                } else {
                    chosenFile = chooser.getSelectedFile();
                }

                Routine exportToExcelRoutine = () -> {
                    rw.export(dequeSeoUrls, imagesSeoUrls, chosenFile);
                };
                RoutineWorker newRoutineWorker = new RoutineWorker(exportToExcelRoutine);
                showModalDialogProgress(newRoutineWorker);
            }
        });

        mainMenu.add(exportMenuItem);
        mainMenu.add(subMenuLoadAndSaveProject);
        menuBar.add(mainMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem supportMenuItem = new JMenuItem("Support");
        supportMenuItem.addActionListener((actionEvent) -> {
            JOptionPane.showMessageDialog(WebSpy.this, "To contact with me" + System.lineSeparator()
                            + "write to here pavlinich.maxim@gmail.com", "Support",
                    JOptionPane.PLAIN_MESSAGE);
        });

        helpMenu.add(supportMenuItem);
        menuBar.add(helpMenu);

        this.setJMenuBar(menuBar);
    }

    private void createButtons() {
        //getting images
        ImageIcon startIcon = new ImageIcon(
                new ImageIcon(System.getProperty("user.dir") +  File.separator + "src" + File.separator + "main" +
                        File.separator + "resources" + File.separator + "play.png")
                        .getImage().getScaledInstance(20, 20, Image.SCALE_DEFAULT));
        ImageIcon pauseIcon = new ImageIcon(
                new ImageIcon(System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" +
                        File.separator + "resources" + File.separator+ "pause.png")
                        .getImage().getScaledInstance(20, 20, Image.SCALE_DEFAULT));
        ImageIcon stopIcon = new ImageIcon(
                new ImageIcon(System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" +
                        File.separator + "resources" + File.separator + "stop.png").getImage().getScaledInstance(20, 20, Image.SCALE_DEFAULT));
        ImageIcon image = new ImageIcon(System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" +
                File.separator
                + "resources" + File.separator + "spider.png");
        setIconImage(image.getImage());

        //creating buttons. The play button is not recognised by name in the program
        playButton = new JButton(startIcon);
        pauseButton = new JButton(pauseIcon);
        pauseButton.setName("Pause Button");
        stopButton = new JButton(stopIcon);
        stopButton.setName("Stop Button");

        //setting buttons
        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);

        pauseButton.addActionListener((actionEve) -> {
            state = StateSEOSpy.PAUSED;
            pauseButton.setEnabled(false);
            playButton.setEnabled(true);
            progressBarUI.setVisible(false);
            saveProjectItem.setEnabled(true);
            exportMenuItem.setEnabled(true);
        });

        stopButton.addActionListener((action) -> {
            state = StateSEOSpy.STOPPED;
            stopButton.setEnabled(false);
            pauseButton.setEnabled(false);
            playButton.setEnabled(true);
            progressBarUI.setVisible(false);
            saveProjectItem.setEnabled(true);
            exportMenuItem.setEnabled(true);
        });

        playButton.addActionListener((actionEvent) -> {
            //if the program was stopped or scanning was ended and need start form beginning - reset the all previous result
            if(state.equals(StateSEOSpy.STOPPED) || state.equals(StateSEOSpy.SCANNING_ENDED)) {
                ((CustomizedDefaultTableModel)mainTable.getModel()).setRowCount(0);
                SeoUrl.setNewStatistics();
                dequeSeoUrls = new LinkedList<SeoUrl>();
                imagesSeoUrls = new HashSet<SeoUrl>();
            }
            progressBarUI.setVisible(true);
            pauseButton.setEnabled(true);
            stopButton.setEnabled(true);
            playButton.setEnabled(false);
            String webSiteToParse = inputMainPageToStart.getText();
            validator = new SeoUrlValidator(webSiteToParse,
                    new String[]{"http", "https"}, SeoUrlValidator.ALLOW_2_SLASHES);
            runSpider(webSiteToParse);
        });
    }

    private Object[] createColumnsByNames() {
        Object[] nameColumns = {"#", "URL", "Canonical", "Response", "Title", "Description", "Keywords",
                                "H1", "Content-Type", "Meta-Robots", "Ex. links", "In links", "Out links", "Problem"};
        return nameColumns;
    }

    private void createMainTable( Object[][] rows, Object[] nameColumns){
       mainTable = new JTable(new CustomizedDefaultTableModel(rows, nameColumns)){

            protected String[] columnToolTips = {null, null, null,null, null, null, null,
                    "Amount of H1 on the page", null, null, "Links to another websites", null, null, null};

            @Override
            protected JTableHeader createDefaultTableHeader(){
                return new JTableHeader(columnModel){
                    public String getToolTipText(MouseEvent e){
                        Point point = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(point.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return columnToolTips[realIndex];
                    }
                };
            }};
    }

    private void settingMainTable(){
        mainTable.setAutoCreateRowSorter(true);
        mainTable.getTableHeader().setReorderingAllowed(false);

        //object scrollPaneForMainTable will be got to the initGUI method
        JScrollPane scrollPaneForMainTable = new JScrollPane(mainTable);
        mainTable.setFillsViewportHeight(false);

        mainTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e){
                if(mainTable.getRowCount() != 0) {
                    //if 2 clicks was clicked then try open the clicked URL
                    if(e.getClickCount() == 2){
                        Point point =  e.getPoint();
                        int indexView = mainTable.rowAtPoint(point);
                        String url = (String) mainTable.getModel().getValueAt(
                                mainTable.convertRowIndexToModel(indexView),1);

                        if(Desktop.isDesktopSupported()) {
                            try {
                                Desktop.getDesktop().browse(new URI(url));
                            } catch(URISyntaxException ex) {
                                System.out.format("%s%n%s", "Oops! Something went wrong!", ex);
                                JOptionPane.showMessageDialog(WebSpy.this, "Oops! Cannot open this link!",
                                        "Error: URISyntaxException" , JOptionPane.ERROR_MESSAGE);
                                WebSpy.logToFile.error(ex.toString());
                            } catch(IOException ex) {
                                JOptionPane.showMessageDialog(WebSpy.this, "Oops! Cannot open this link!",
                                        "Error: IOException", JOptionPane.ERROR_MESSAGE);
                                System.out.format("%s%n%s", "Oops! Something went wrong!", ex);
                                WebSpy.logToFile.error(ex.toString());
                            }
                        }
                    }
                }
            }
        });

        mainTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                int indexRowModel = table.getRowSorter().convertRowIndexToModel(row);
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, indexRowModel, column);

                if(isSelected){
                    return c;
                } else if((table.getModel().getValueAt(indexRowModel, 13)).equals("true")) {
                    //so seoUrl have seo problem - paint red color of the cell
                    c.setBackground(Color.RED);
                } else {
                    //so problem seoUrl doesn't have - paint white color of the cell
                    c.setBackground(Color.CYAN.WHITE);
                }
                return c;
            }
        });

        mainTable.setDefaultRenderer(Integer.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column){
                int indexRowModel = table.getRowSorter().convertRowIndexToModel(row);
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, indexRowModel, column);

                if (isSelected) {
                    return c;
                } else if((table.getModel().getValueAt(indexRowModel, 13)).equals("true")) {
                    //so seoUrl have seo problem - paint red color of the cell
                    c.setBackground(Color.RED);
                } else {
                    //so problem seoUrl doesn't have - paint white color of the cell
                    c.setBackground(Color.WHITE);
                }
                return c;
            }
        });
        mainTable.getColumnModel().getColumn(0).setPreferredWidth(50);
    }

    private JTable createTabErrorTable() {
        JTable tableTabError = new JTable(mainTable.getModel());
        tableTabError.setAutoCreateRowSorter(true);
        tableTabError.getTableHeader().setReorderingAllowed(false);

        tableTabError.setDefaultRenderer(Object.class, new ObjectDefaultTableCellRenderer());
        tableTabError.setDefaultRenderer(Integer.class, new IntegerDefaultTableCellRenderer());

        //object scrollForTableError will be got to the initGUI method
        JScrollPane scrollForTableError = new JScrollPane(tableTabError);
        return tableTabError;
    }

    private void settingFirstFilterTab(JTable tableFilterTabResult, JPanel jpFilterTabFirst){
        JTable tableFilterTab = new JTable();

        DefaultTableModel modelForFilter = new DefaultTableModel(new Object[]{
                "URL", "Canonical", "Response", "Title", "Description", "Keywords","H1", "MetaRobots"}, 1);

        for(int i = 0; i < 8; i++)
            modelForFilter.setValueAt("", 0, i);

        tableFilterTab.setModel(modelForFilter);
        tableFilterTab.getTableHeader().setReorderingAllowed(false);
        tableFilterTab.setPreferredScrollableViewportSize(new Dimension(tableFilterTab.getColumnModel().
                getTotalColumnWidth(), tableFilterTab.getRowHeight()));
        JScrollPane skrollTableFilterTab = new JScrollPane(tableFilterTab);
        skrollTableFilterTab.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        skrollTableFilterTab.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        jpFilterTabFirst.add(skrollTableFilterTab);

        JButton searchButt = new JButton("Search");

        jpFilterTabFirst.add(searchButt);

        tableFilterTabResult.getTableHeader().setReorderingAllowed(false);

        TableRowSorter<DefaultTableModel> sorter =
                new TableRowSorter<>(((DefaultTableModel)tableFilterTabResult.getModel()));
        tableFilterTabResult.setRowSorter(sorter);

        searchButt.addActionListener((actionEvent) -> {
            if(tableFilterTab.isEditing())
                tableFilterTab.getCellEditor().stopCellEditing();

            ((TableRowSorter<DefaultTableModel>)tableFilterTabResult.getRowSorter()).setRowFilter(null);

            List<RowFilter<DefaultTableModel, Object>> filters = new ArrayList<>();

            for(int i = 0; i < 8; i++){
                String pattern = (String) modelForFilter.getValueAt(0, i);
                if(pattern.equals(""))
                    continue;
                String nameColumn = modelForFilter.getColumnName(i);
                int indexModel = tableFilterTabResult.getColumn(nameColumn).getModelIndex();
                filters.add(RowFilter.regexFilter(pattern, indexModel));
            }

            if(filters.size() != 0) {
                TableRowSorter<DefaultTableModel> trs = (TableRowSorter<DefaultTableModel>) tableFilterTabResult.getRowSorter();
                trs.setRowFilter(RowFilter.andFilter(filters));
            }
            tableFilterTabResult.changeSelection(0,0, false, false);
        });
    }

    private void namingTabs(String[] names, JPanel[] panels) {
        if(names.length != panels.length)
            throw new RuntimeException("Parameters of two arrays not equals.");

        for(int i = 0; i < names.length; i++) {
            tabs.addTab(names[i], panels[i]);
        }
    }

    private void settingUpTabs(JTable tableTabError, JTable tableFilterTabResult, JPanel topPanel){


        tabs.addChangeListener((changeEvent) -> {
            int index = tabs.getSelectedIndex();
            if (index == 1) {
                ((DefaultRowSorter)tableTabError.getRowSorter()).setRowFilter(new RowFilter<DefaultTableModel, Integer>(){
                    @Override
                    public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry){
                        if(entry.getValue(13).toString().equals("true"))
                            return true;
                        return false;
                    }
                });
                //if no one row was selected before - change state to row 0, column 0
                if( (tableTabError.getSelectedRow() + tableTabError.getSelectedColumn()) == -2)
                    tableTabError.changeSelection(0,0, false, false);
            }
            else if(index == 2){
                if(tableFilterTabResult.getSelectedRow() + tableFilterTabResult.getSelectedColumn() == -2)
                    tableFilterTabResult.changeSelection(0,0, false, false);
            }
        });

        tabs.addChangeListener((changeEvent) -> {
            JPanel temp1 = ((JPanel)(tabs.getComponentAt(tabPreviousIndex)));
            temp1.remove(topPanel);

            JPanel temp2 = ((JPanel)(tabs.getComponentAt(tabCurrentIndex)));
            temp2.add(topPanel, BorderLayout.NORTH);
        });

        tabs.addChangeListener((changeEvent)->{
            tabPreviousIndex = tabCurrentIndex;
            tabCurrentIndex = tabs.getSelectedIndex();
        });
    }

    //this - inherited methods
    public void initGUI() {
        logToFile.info("Initialization GUI...");
        this.setName("SEOSpy");

        createMenuBar();
        createButtons();
        createProgressDialog();

        Object[][] rows = {};
        Object[] nameColumns = createColumnsByNames();
        createMainTable(rows, nameColumns);
        settingMainTable();

        JTable tableTabError = createTabErrorTable();

        JPanel errorPanel = new JPanel(new BorderLayout());
        //gets JScrollPane from tableTabError
        errorPanel.add( ((JScrollPane)((JViewport)tableTabError.getParent()).getParent()), BorderLayout.CENTER);

        JPanel jpFilterTabFirst = new JPanel();
        jpFilterTabFirst.setLayout(new FlowLayout(FlowLayout.LEFT));

        //create JTable for searching
        JTable tableFilterTabOfResult = new JTable(mainTable.getModel());
        settingFirstFilterTab(tableFilterTabOfResult, jpFilterTabFirst);

        JPanel filterPanel = new JPanel(new BorderLayout());
        JPanel innerPanelOfFilterPanel = new JPanel();
        innerPanelOfFilterPanel.setLayout(new BoxLayout(innerPanelOfFilterPanel, BoxLayout.Y_AXIS));
        innerPanelOfFilterPanel.add(jpFilterTabFirst);
        innerPanelOfFilterPanel.add(new JScrollPane(tableFilterTabOfResult));
        filterPanel.add(innerPanelOfFilterPanel, BorderLayout.CENTER);

        inputMainPageToStart = new JTextField();
        inputMainPageToStart.setPreferredSize(new Dimension(200, 24));
        String keyStrokeAndKey = "alt V";
        Action actionMainPageJT = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    inputMainPageToStart.getDocument().insertString(
                            0,
                            (String) Toolkit.getDefaultToolkit()
                                    .getSystemClipboard().getData(DataFlavor.stringFlavor),
                            null);
                } catch(IOException | UnsupportedFlavorException | BadLocationException ex){
                    logToFile.error(ex);
                }
            }
        };
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyStrokeAndKey);
        inputMainPageToStart.getInputMap().put(keyStroke, keyStrokeAndKey);
        inputMainPageToStart.getActionMap().put(keyStroke,actionMainPageJT);

        JPanel scanningPanel = new JPanel();
        scanningPanel.setName("scanPanel");
        scanningPanel.setLayout(new BorderLayout());

        progressBarUI = new JProgressBar();
        progressBarUI.setIndeterminate(true);
        progressBarUI.setVisible(false);

        kitButtonsPanel = new JPanel();
        kitButtonsPanel.add(inputMainPageToStart);
        kitButtonsPanel.add(playButton);
        kitButtonsPanel.add(pauseButton);
        kitButtonsPanel.add(stopButton);

        JPanel topPanel = new JPanel(new GridBagLayout());
        JPanel innerPanelOfTopPanel = new JPanel();
        innerPanelOfTopPanel.add(this.progressBarUI);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        //setting up GridBagConstraints and add to the top panel
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.5f;
        topPanel.add(kitButtonsPanel, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_END;
        topPanel.add(innerPanelOfTopPanel, gridBagConstraints);

        //get JScrollPane from mainTable and add to the panel of scanning
        scanningPanel.add(((JScrollPane)((JViewport)mainTable.getParent()).getParent()), BorderLayout.CENTER);
        scanningPanel.add(topPanel, BorderLayout.NORTH);

        namingTabs(new String[] {"Scan", "Error", "Filter"},
                new JPanel[]{scanningPanel, errorPanel, filterPanel});
        settingUpTabs(tableTabError, tableFilterTabOfResult, topPanel);
        this.add(tabs);

        //setting up pop-up menus on the tables
        setUpPopupMenu(mainTable);
        setUpPopupMenu(tableTabError);
        setUpPopupMenu(tableFilterTabOfResult);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //add logging when was pushed  the exit button
        this.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event){
                logToFile.info("The program was closed.");
            }
        });

        this.setPreferredSize(new Dimension(800, 800));
        this.setLocationRelativeTo(null);
        this.pack();
        this.setVisible(true);

        logToFile.info("GUI was initialized.");
    }

    private void updateTable() {
        logToFile.info("Updating table...");
        Object[] data = new Object[14];
        DefaultTableModel tableModel = (DefaultTableModel) mainTable.getModel();
        tableModel.setRowCount(0);
        int numberOfPage = 1;

        for(SeoUrl seoUrl : dequeSeoUrls){
            data[0] = numberOfPage++;
            data[1] = seoUrl.getURL();
            data[2] = seoUrl.getCanonical();
            data[3] = seoUrl.getResponse();
            data[4] = seoUrl.getTitle();
            data[5] = seoUrl.getDescription();
            data[6] = seoUrl.getKeywords();
            data[7] = seoUrl.getCountH1();
            data[8] = seoUrl.getContentType();
            data[9] = seoUrl.getMetarobots();

            if(SeoUrl.externalLinks.get((seoUrl.getURL())) != null) {
                data[10] = SeoUrl.externalLinks.get((seoUrl.getURL())).size();
            } else {
                data[10] = 0;
            }

            if(SeoUrl.statisticLinksOn.get(seoUrl.getURL()) != null) {
                data[11] = SeoUrl.statisticLinksOn.get(seoUrl.getURL()).size();
            } else {
                data[11] = -1;
            }

            /*try{
            Thread.sleep(2000);
            }catch(Exception ex){
                System.out.println(ex);
            }
            */

            if(SeoUrl.statisticLinksOut.get(seoUrl.getURL()) != null) {
                data[12] = SeoUrl.statisticLinksOut.get(seoUrl.getURL()).size();
            } else {
                data[12] = -1;
            }
            data[13] = seoUrl.getFlagSeoProblem().toString();

            tableModel.addRow(data);
        }

        data = new Object[14];

        for(SeoUrl seoImage : imagesSeoUrls) {
            data[0]  = numberOfPage++;
            data[1]  = seoImage.getURL();
            data[3]  = seoImage.getResponse();
            data[8]  = seoImage.getContentType();
            data[11] = SeoUrl.statisticLinksOn.get(seoImage.getURL()).size();
            data[13] = seoImage.getFlagSeoProblem().toString();

            tableModel.addRow(data);
        }

        tableModel.fireTableDataChanged();
        //after adding - update the table
        mainTable.changeSelection(0,0,false,false);
        logToFile.info("Table was updated.");
    }

    private void loadProject() {
        logToFile.info("Loading project");
        JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
        chooser.setFileFilter(
                new FileNameExtensionFilter(rw.getExtensionForLoad(), rw.getExtensionForLoad().replaceFirst(".", "")));
        chooser.setDialogTitle("Load from");
        int state = chooser.showOpenDialog(this);

        if(state == JFileChooser.APPROVE_OPTION) {
            try{
                Routine loadProjectRoutine = () -> {
                    rw.loadFrom(dequeSeoUrls, imagesSeoUrls, chooser.getSelectedFile());
                };
                RoutineWorker newRoutineWorker = new RoutineWorker(loadProjectRoutine);
                showModalDialogProgress(newRoutineWorker);
            } catch(Exception ex) {
                logToFile.error(ex.toString());
                System.out.println(ex.getClass().getName() + ex.getStackTrace());
                ex.printStackTrace();
            }
            saveProjectItem.setEnabled(true);
        }
        logToFile.info("Project was loaded.");
    }

    private void saveProject() {
        JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
        chooser.setDialogTitle("Specify a file to save");
        chooser.setFileFilter(
                new FileNameExtensionFilter(
                        rw.getExtensionToSave(), rw.getExtensionToSave().replaceFirst(".", "")));

        int state = chooser.showSaveDialog(this);
        if(state == JFileChooser.APPROVE_OPTION) {
            final File chosenFile;
            //for the lambda expression we need an effective final variable
            if(! chooser.getSelectedFile().toString().toLowerCase().endsWith(rw.getExtensionToSave())) {
                chosenFile = new File(chooser.getSelectedFile().toString() + rw.getExtensionToSave());
            } else {
                chosenFile = chooser.getSelectedFile();
            }

            try {
            logToFile.info("Saving project.");
                Routine saveProjectRoutine = () -> {
                    rw.saveTo(dequeSeoUrls,imagesSeoUrls, chosenFile);
                };
                RoutineWorker newRoutineWorker = new RoutineWorker(saveProjectRoutine);
                showModalDialogProgress(newRoutineWorker);
            } catch(Exception ex) {
                logToFile.error(ex.toString());
                System.out.println(ex);
            }
            logToFile.info("Project was saved.");
        }
    }

    public WebSpy() {
        logToFile.info("The program was started.");
        logToFile.info("Initialization WebSpy...");
        pages = new java.util.ArrayList<URL>();
        dequeSeoUrls = new LinkedList<>();
        tunner = TunnerSeoURL.getTunner();
        imagesSeoUrls = new HashSet<>();
        initGUI();
        logToFile.info("WebSpy was initialized.");
    }

    //create and set up popups menu on the table
    private void setUpPopupMenu(JTable table) {
        logToFile.info("Setting up pop-up menu");
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem exLinksMenuItem = new JMenuItem("URL properties");

        exLinksMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                JDialog dialog = new JDialog(WebSpy.this, "Our project", true);
                dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.PAGE_AXIS));

                JLabel pageUrlLab, linksPageToLabel, linksFromPageLabel, externalLinksLabel;
                pageUrlLab = new JLabel("Page URL:");
                linksPageToLabel = new JLabel("Links to this page:");
                linksFromPageLabel = new JLabel("Links from this page:");
                externalLinksLabel = new JLabel("External links:");

                JTextField selectedUrlTextField = new JTextField();
                JTextArea linksToPageTextArea = new JTextArea(),
                        linksFromPageTextArea = new JTextArea(),
                        externalLinksTextArea = new JTextArea();

                selectedUrlTextField.setEditable(false);
                linksToPageTextArea.setEditable(false);
                linksFromPageTextArea.setEditable(false);
                externalLinksTextArea.setEditable(false);

                JScrollPane scrollLinksTo = new JScrollPane(linksToPageTextArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                            scrollLinksFrom = new JScrollPane(linksFromPageTextArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                            scrollExternalLinks = new JScrollPane(externalLinksTextArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                            scrollPageUrlField = new JScrollPane(selectedUrlTextField, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

                scrollLinksTo.setPreferredSize(new Dimension(50, 70));
                scrollLinksFrom.setPreferredSize(new Dimension(50, 70));
                scrollExternalLinks.setPreferredSize(new Dimension(50, 70));
                selectedUrlTextField.setPreferredSize(new Dimension(70, 20));

                JButton closeButton = new JButton("Close");
                closeButton.addActionListener((ae) -> dialog.dispose());
                closeButton.setAlignmentX(JComponent.LEFT_ALIGNMENT);

                int viewSelectedRow = table.getSelectedRow();
                int modelSelectedRow = table.convertRowIndexToModel(viewSelectedRow);
                String selectedUrl = (String) table.getModel().getValueAt(modelSelectedRow, 1);
                selectedUrlTextField.setText(selectedUrl);

                Set<String> linksOn = SeoUrl.statisticLinksOn.get(selectedUrl);
                for(String s : linksOn) {
                    linksToPageTextArea.append(s + System.lineSeparator());
                }

                Set<String> linksOut = SeoUrl.statisticLinksOut.get(selectedUrl);
                if(linksOut != null) {
                    for(String s : linksOut){
                        linksFromPageTextArea.append(s + System.lineSeparator());
                    }
                }

                for(SeoUrl s : dequeSeoUrls){
                    if (s.getURL().equals(selectedUrl)) {
                        String columnExternalLinks = "";
                        if(SeoUrl.externalLinks.get(s.getURL()) != null) {
                            for (String externalUrl : SeoUrl.externalLinks.get(s.getURL()))
                                columnExternalLinks += externalUrl + System.lineSeparator();
                        }
                        externalLinksTextArea.setText(columnExternalLinks);
                        break;
                    }
                }

                dialog.add(pageUrlLab);
                dialog.add(new JPanel().add(scrollPageUrlField));
                dialog.add(linksPageToLabel);
                dialog.add(scrollLinksTo);
                dialog.add(linksFromPageLabel);
                dialog.add(scrollLinksFrom);
                dialog.add(externalLinksLabel);
                dialog.add(scrollExternalLinks);
                dialog.add(Box.createRigidArea(new Dimension(10,10)));
                dialog.add(closeButton);
                dialog.add(Box.createRigidArea(new Dimension(10,10)));

                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setPreferredSize(new Dimension(400, 500));
                dialog.pack();
                dialog.setVisible(true);
            }
        });

        JMenuItem openUrlInBrowserMenuItem = new JMenuItem("Open in browser");

        openUrlInBrowserMenuItem.addActionListener((ae) -> {
            if(Desktop.isDesktopSupported()) {
                int viewIndex = table.getSelectedRow();
                int modelIndex = table.convertRowIndexToModel(viewIndex);
                String url = (String) table.getModel().getValueAt(modelIndex, 1);
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch(java.net.URISyntaxException ex) {
                    logToFile.error(ex.toString());
                    System.out.println(ex.getClass().getName() + ex.getStackTrace());
                    ex.printStackTrace();
                } catch(java.io.IOException ex) {
                    logToFile.error(ex.toString());
                    System.out.println(ex.getClass().getName() + ex.getStackTrace());
                    ex.printStackTrace();
                }
            }
        });

        JMenuItem googleCacheMenuItem = new JMenuItem("Open in Google's cache");
        googleCacheMenuItem.addActionListener((event) -> {
            if(Desktop.isDesktopSupported()) {
                int viewIndexRow = table.getSelectedRow();
                int modelIndexRow = table.convertRowIndexToModel(viewIndexRow);
                String url = "http://webcache.googleusercontent.com/search?q=cache:" + (String) table.getModel().getValueAt(modelIndexRow, 1);
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch(java.net.URISyntaxException ex) {
                    logToFile.error(ex.toString());
                    System.out.println(ex.getClass().getName() + ex.getStackTrace());
                    ex.printStackTrace();
                } catch(java.io.IOException ex) {
                    logToFile.error(ex.toString());
                    System.out.println(ex.getClass().getName() + ex.getStackTrace());
                    ex.printStackTrace();
                }
            }
        });

        popupMenu.add(exLinksMenuItem);
        popupMenu.add(openUrlInBrowserMenuItem);
        popupMenu.add(googleCacheMenuItem);

        table.setComponentPopupMenu(popupMenu);
        logToFile.info("Pop-up menu was set up");
    }

    private void createConcurrentUpdaterForTable() {
        Thread updater = new Thread() {
            @Override
            public void run() {
                while(state == StateSEOSpy.RUNNING) {
                    SwingUtilities.invokeLater(() -> updateTable());
                    try {
                        Thread.sleep(2000);
                    } catch(InterruptedException ex) {
                        System.out.println(ex.getClass().getName() + ex.getStackTrace());
                        ex.printStackTrace();
                        logToFile.info(ex.toString());
                    }
                }
            }
        };
        updater.start();
    }

    void runSpider(String websiteMainPage) {
        logToFile.info("Running spider...");
        if(spider == null) {
            try {
                spider = new Spider(new URL(websiteMainPage));
            } catch(java.net.MalformedURLException ex) {
                logToFile.error(ex.toString());
                System.out.println(ex.getClass().getName() + ex.getStackTrace());
                ex.printStackTrace();
            }
        }
        logToFile.info("Start scanning.");
        spider.scanWithDeque();
    }

    private class Spider {
        URL startingURL;
        Queue<URL> queue;

        public Spider(URL startURL){
            logToFile.info("Initialization spider...");

            this.startingURL = startURL;
            queue = new java.util.LinkedList<URL>();

            logToFile.info("Spider was initialized.");
        }

        void scanWithSets() {
            logToFile.info("Scanning website...");
            System.out.println("Start the program");
            Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
            WebClient wc = new WebClient(BrowserVersion.CHROME);
            HtmlPage parsingPage = null;


            wc.getOptions().setRedirectEnabled(false);
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.getOptions().setJavaScriptEnabled(false);
            //wc.getOptions().setThrowExceptionOnScriptError(false);

            List<String> leftPages = new ArrayList<>();
            Set<String> checkedPages = new HashSet<>();
            Set<SeoUrl> seoUrlsSet = new HashSet<>();

            String startPage = "https://conditionservice.com.ua/";
            try {
                parsingPage = wc.getPage("https://conditionservice.com.ua/");
            } catch (IOException ex) {
                logToFile.error(ex.toString());
                System.out.format("Exception %s%n", ex);
            }

            String domain = parsingPage.getUrl().getHost();

            leftPages.add(parsingPage.getUrl().toString());
            TunnerSeoURL tunner = TunnerSeoURL.getTunner();
            UrlValidator validatorUrl = new UrlValidator();

          while(true){
              System.out.println("Scanning URL " + parsingPage.getUrl().toString());
              DomNodeList<HtmlElement> innerLinks = parsingPage.getBody().getElementsByTagName("a");
              DomNodeList<HtmlElement> innerImages = parsingPage.getBody().getElementsByTagName("img");

              for (HtmlElement el : innerLinks) {
                  URL potentialUrl = null;
                  try {
                        potentialUrl = parsingPage.getFullyQualifiedUrl(el.getAttribute("href"));
                        System.out.println("Potential (but not URL): " + parsingPage.getFullyQualifiedUrl(el.getAttribute("href")));
                    } catch (MalformedURLException ex) {
                      System.out.println(ex.getClass().getName() + ex.getStackTrace());
                      ex.printStackTrace();
                    }

                    //if a link to a main page without trailing slash
                    if((potentialUrl.toString() +"/").equals(startPage) && !checkedPages.contains(potentialUrl.toString())){
                      checkedPages.add(potentialUrl.toString());
                      leftPages.remove(potentialUrl.toString());
                      continue;
                    }



                  if(!(validatorUrl.isValid(potentialUrl.toString()))){
                      System.out.println("Not valid: " + potentialUrl.toString());
                      continue;
                  }

                  if(!potentialUrl.getHost().equals(domain)) {
                    System.out.println("Not equal domain: " + potentialUrl.toString());
                    continue;
                    }

                if(potentialUrl.toString().indexOf('#') != -1){
                    System.out.println("Index of " + potentialUrl.toString()
                            .indexOf('#') + " URL is " + potentialUrl.toString());
                    continue;
                }

                if(! (checkedPages.contains(potentialUrl.toString())) &&
                        !(leftPages.contains(potentialUrl.toString()))){
                    System.out.println("Add value: " + potentialUrl.toString());
                    leftPages.add(potentialUrl.toString());
                }
            }

            SeoUrl newSeoUrl = new SeoUrl(parsingPage.getUrl().toString());
            tunner.tunne(newSeoUrl, parsingPage);
            seoUrlsSet.add(newSeoUrl);

            boolean wasAdded = checkedPages.add(parsingPage.getUrl().toString());
            System.out.println("parsing url was added to checkedPages: " + parsingPage.getUrl().toString() + " " + wasAdded);
            System.out.println("\ncheckedPage.toString(): " + checkedPages + "\n");

            System.out.println("\nleftPages.toString(): " + leftPages + "\n");
            boolean isUrlRemoved = leftPages.remove(parsingPage.getUrl().toString());
            System.out.println("parsing url was removed to leftPage: " + parsingPage.getUrl().toString() + " " + isUrlRemoved);
            System.out.println("\nleftPages.toString() after deleted: " + leftPages + "\n");

            String newParseUrl = null;
            if(leftPages.size() != 0)
                newParseUrl = leftPages.get(0);
            else break;

              System.out.println("New parse URL is " + newParseUrl);
            try {
                parsingPage = wc.getPage(newParseUrl);
            } catch (IOException ex) {
                logToFile.error(ex.toString());
                System.out.println(ex);
            }

              System.out.println("Size leftPages: " + leftPages.size());
        }

        //    checkedPages.stream().forEach(System.out::println);
            logToFile.info("The Website was scanned.");
        }


        void scanWithDeque() {
            logToFile.info("Scanning website...");
            if(state == StateSEOSpy.NOT_RUN_YET || state == StateSEOSpy.SCANNING_ENDED || state == StateSEOSpy.STOPPED) {
                SwingWorker sw = new SwingWorker() {
                    @Override
                    protected Void doInBackground() {
                        // if program have been paused and was pressed the play button - waking up thread for continue scanning
                        if(state == StateSEOSpy.PAUSED) {
                            state = StateSEOSpy.RUNNING;
                            synchronized(lock) {
                                lock.notify();
                            }
                            createConcurrentUpdaterForTable();
                        }  else { //else start scanning
                            state = StateSEOSpy.RUNNING;

                            //Create a thread for updating table
                            createConcurrentUpdaterForTable();

                            System.out.println("Start the program");
                            Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);

                            WebClient wc = new WebClient(BrowserVersion.CHROME);
                            wc.getOptions().setRedirectEnabled(false);
                            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
                            wc.getOptions().setJavaScriptEnabled(false);

                            try {
                                HtmlPage parsingHtmlPage = wc.getPage(startingURL);
                                System.out.println("Starting URL is " + startingURL.toString());
                                logToFile.info("Starting URL is " + parsingHtmlPage.getUrl().toString());
                                SeoUrl.statisticLinksOut.put(parsingHtmlPage.getUrl().toString(), new HashSet<String>());
                                SeoUrl.cacheContentTypePages.put(parsingHtmlPage.getUrl().toString(), validator.getContentType(parsingHtmlPage.getUrl().toString()));
                                System.out.println(parsingHtmlPage.getUrl().toString());
                                do {
                                    spider.handleImages(parsingHtmlPage);

                                    SeoUrl seoUrlForParsingPage = new SeoUrl(parsingHtmlPage.getUrl().toString());
                                    tunner.tunne(seoUrlForParsingPage, parsingHtmlPage);
                                    seoUrlForParsingPage.analyzeURL();

                                    dequeSeoUrls.addFirst(seoUrlForParsingPage);

                                    //There are we get all "a" and "link" html-elements with attributes href and this attributes mustn't be empty
                                    List<HtmlElement> anchorsParsingPage = parsingHtmlPage.getHead().getByXPath(
                                            "//a[@href and string-length(@href)!=0] | //link[@href and string-length(@href)!=0]");

                                    for(HtmlElement singleAnchor: anchorsParsingPage) {
                                        //if a user pressed some button - reacting on it
                                        ifPausedThenWait();
                                        if(wasStopped()) {
                                            return null;
                                        }

                                        //set a new potential url
                                        String potentialNewUrl = null;
                                        if(singleAnchor instanceof HtmlAnchor)
                                            potentialNewUrl = parsingHtmlPage.getFullyQualifiedUrl(((HtmlAnchor)singleAnchor).getHrefAttribute().toString()).toString();
                                        else if(singleAnchor instanceof HtmlLink)
                                            potentialNewUrl = parsingHtmlPage.getFullyQualifiedUrl(((HtmlLink)singleAnchor).getHrefAttribute().toString()).toString();

                                        if(dequeSeoUrls.contains(new SeoUrl(potentialNewUrl))) {
                                            continue;
                                        }

                                        //check to see if potentialNewUrl not equal startingURL without backslash
                                        if (potentialNewUrl.equals((startingURL.toString()).substring(0, startingURL.toString().length() - 1))) {
                                            //add statistics
                                            SeoUrl.statisticLinksOut.putIfAbsent(parsingHtmlPage.getUrl().toString(), new HashSet<String>());
                                            SeoUrl.statisticLinksOut.get(parsingHtmlPage.getUrl().toString()).add(potentialNewUrl.toString());
                                            SeoUrl.statisticLinksOn.putIfAbsent(potentialNewUrl.toString(), new HashSet<String>());
                                            SeoUrl.statisticLinksOn.get(potentialNewUrl.toString()).add(parsingHtmlPage.getUrl().toString());
                                            SeoUrl.cacheContentTypePages.put(potentialNewUrl, validator.getContentType(potentialNewUrl));

                                            SeoUrl potentialNewSeoUrl = new SeoUrl(potentialNewUrl);
                                            HtmlPage realUrlForSettingSeoUrl = wc.getPage(potentialNewUrl);
                                            tunner.tunne(potentialNewSeoUrl, realUrlForSettingSeoUrl);
                                            potentialNewSeoUrl.analyzeURL();
                                            dequeSeoUrls.addFirst(potentialNewSeoUrl);
                                            continue;
                                        }

                                        if (validator.isSchemeHttpOrHttps(potentialNewUrl)) {
                                            //check if the link lead to external site
                                            if(! validator.isSameHost(new URL(potentialNewUrl) )) {
                                                //if it is, then add it for statistics and continue for new parsing page
                                                SeoUrl.externalLinks.putIfAbsent(seoUrlForParsingPage.getURL(), new HashSet<String>());
                                                SeoUrl.externalLinks.get(seoUrlForParsingPage.getURL()).add(potentialNewUrl);
                                                continue;
                                            }

                                            //check if it is an anchor link, then cutting
                                            if (validator.havePoundSign(new URL(potentialNewUrl)))
                                                potentialNewUrl = potentialNewUrl.substring(0, potentialNewUrl.indexOf('#'));

                                            //caching if it is a new page
                                            SeoUrl.cacheContentTypePages.putIfAbsent(potentialNewUrl, validator.getContentType(potentialNewUrl));

                                            //if it is an image (or something else) - put a link in the statistics Map and continue.
                                            // An image (or something else)  can't refer to another page.
                                            if(! (SeoUrl.cacheContentTypePages.get(potentialNewUrl).startsWith("text/html") )) {
                                                if(SeoUrl.cacheContentTypePages.get(potentialNewUrl).startsWith("image/")) {
                                                    spider.handleImageFromNotHtmlTagImg(potentialNewUrl, parsingHtmlPage);
                                                }
                                                continue;
                                            }

                                            SeoUrl.statisticLinksOut.putIfAbsent(parsingHtmlPage.getUrl().toString(), new HashSet<String>());
                                            SeoUrl.statisticLinksOut.get(parsingHtmlPage.getUrl().toString()).add(potentialNewUrl);
                                            SeoUrl.statisticLinksOn.putIfAbsent(potentialNewUrl.toString(), new HashSet<String>());
                                            SeoUrl.statisticLinksOn.get(potentialNewUrl.toString()).add(parsingHtmlPage.getUrl().toString());

                                            SeoUrl tempSeoUrl = new SeoUrl(potentialNewUrl);
                                            if (! (dequeSeoUrls.contains(tempSeoUrl))) {
                                                System.out.print('.');
                                                logToFile.info("Adding new URL for scanning: " + tempSeoUrl.toString());
                                                dequeSeoUrls.addLast(tempSeoUrl);
                                            }
                                        }
                                    }

                                    //if next url equals to starting url, then the site was parsed
                                    if(dequeSeoUrls.peekLast().toString().equals(startingURL.toString())) {
                                        break;
                                    } else {
                                        // else get the next URL
                                        String nextUrl = (dequeSeoUrls.pollLast()).getURL();
                                        System.out.println("Parse URL is :" + nextUrl);
                                        parsingHtmlPage = wc.getPage(nextUrl);
                                        logToFile.info("New parsing page is " + parsingHtmlPage.getUrl().toString());
                                    }
                                } while (true);
                            } catch(IOException ex) {
                                logToFile.error(ex.toString());
                                System.out.println(ex.getClass().getName() + ex.getStackTrace());
                                ex.printStackTrace();
                            }
                            state = StateSEOSpy.SCANNING_ENDED;
                            wc.close();
                        }
                        return null;
                    }
                    @Override
                    protected void done(){
                        if(state == StateSEOSpy.SCANNING_ENDED){
                            mainMenu.getItem(0).setEnabled(true);// it does exportMenuItem.setEnabled(true);
                            ((JMenu)(mainMenu.getMenuComponent(1))).getItem(0).setEnabled(true);//it does saveProjectItem.setEnabled(true)
                            ((JButton) kitButtonsPanel.getComponent(1)).setEnabled(true); // it does playButton.setEnabled(true);
                            ((JButton) kitButtonsPanel.getComponent(2)).setEnabled(false); // it does pauseButton.setEnabled(false);
                            ((JButton) kitButtonsPanel.getComponent(3)).setEnabled(false); // it does pauseButton.setEnabled(false);

                            progressBarUI.setVisible(false);
                            updateTable();
                            logToFile.info("The website was scanned.");
                        }
                    }

                };
                sw.execute();
            } else if(state == StateSEOSpy.PAUSED) {
                Runnable runToNotify = new Runnable() {
                    @Override
                    public void run(){
                        synchronized(lock) {
                            state = StateSEOSpy.RUNNING;
                            lock.notify();
                        }
                    }
                };
                new Thread(runToNotify).start();
            }
        }

        private void handleImageFromNotHtmlTagImg(String imageFromTagA, HtmlPage page) {
            SeoUrl seoUrlToImage = new SeoUrl(imageFromTagA, true);
            SeoUrl.cacheContentTypePages.putIfAbsent(imageFromTagA, validator.getContentType(imageFromTagA));
            tunner.tunne(seoUrlToImage, page);
            seoUrlToImage.analyzeURL();

            SeoUrl.statisticLinksOut.putIfAbsent(page.getUrl().toString(), new HashSet<String>());
            SeoUrl.statisticLinksOut.get(page.getUrl().toString()).add(imageFromTagA);
            SeoUrl.statisticLinksOn.putIfAbsent(seoUrlToImage.toString(), new HashSet<String>());
            SeoUrl.statisticLinksOn.get(seoUrlToImage.toString()).add(page.getUrl().toString());
            imagesSeoUrls.add(seoUrlToImage);
        }

        private void handleImages(HtmlPage parsingPage){
            DomNodeList<HtmlElement> listImages = parsingPage.getBody().getElementsByTagName("img");
            Set<String> setOfImage = new HashSet();

            for(HtmlElement htmlElement : listImages) {
                HtmlElement iSThereNoscript = htmlElement.getEnclosingElement("noscript");
                //we do not need html elements from noscript THML-elements
                if(iSThereNoscript != null) {
                    continue;
                }

                String src;
                if(htmlElement.hasAttribute("src")) {
                     src = htmlElement.getAttribute("src");
                } else if(htmlElement.hasAttribute("data-src")) {
                        src = htmlElement.getAttribute("data-src");
                } else {
                    continue;
                }

                try {
                    String qualifiedUrl = parsingPage.getFullyQualifiedUrl(src).toString();
                    setOfImage.add(qualifiedUrl);
                    SeoUrl.cacheContentTypePages.putIfAbsent(qualifiedUrl, validator.getContentType(qualifiedUrl));
                } catch(MalformedURLException ex){
                    System.out.println(ex.getClass().getName() + ex.getStackTrace());
                    ex.printStackTrace();
                }
            }

            for(String ordinaryUrlOfImage : setOfImage){
                SeoUrl seoUrlImage = new SeoUrl(ordinaryUrlOfImage, true);
                tunner.tunne(seoUrlImage, parsingPage);
                seoUrlImage.analyzeURL();

                seoUrlImage.statisticLinksOut.putIfAbsent(parsingPage.getUrl().toString(), new HashSet<String>());
                seoUrlImage.statisticLinksOut.get(parsingPage.getUrl().toString()).add(seoUrlImage.toString());
                SeoUrl.statisticLinksOn.putIfAbsent(seoUrlImage.toString(), new HashSet<String>());
                SeoUrl.statisticLinksOn.get(seoUrlImage.toString()).add(parsingPage.getUrl().toString());
                imagesSeoUrls.add(seoUrlImage);
            }
        }
    }

    private void ifPausedThenWait() {
        synchronized(lock) {
            while(state == StateSEOSpy.PAUSED) {
                try {
                    WebSpy.logToFile.info("Thread for scanning was stopped.");
                    lock.wait();
                    WebSpy.logToFile.info("Thread for scanning was woke up.");
                   // state = StateSEOSpy.RUNNING;
                } catch(InterruptedException ex) {
                    logToFile.error(ex.toString());
                    System.out.println(ex.getClass().getName() + ex.getStackTrace());
                    ex.printStackTrace();
                }
            }

        }
    }

    private boolean wasStopped() {
        return state == StateSEOSpy.STOPPED;
    }
}


class CustomizedDefaultTableModel extends DefaultTableModel{
    CustomizedDefaultTableModel(Object[][] rows, Object[] columns) {
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

class ObjectDefaultTableCellRenderer extends DefaultTableCellRenderer{
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
        int indexView = table.getRowSorter().convertRowIndexToModel(row);
        Component cellRendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, indexView, column);

        if(isSelected) {
            return cellRendererComponent;
        }

        if(table.getModel().getValueAt(indexView, 13).equals("true")) {
            cellRendererComponent.setBackground(Color.RED);
        }
        else {
            cellRendererComponent.setBackground(Color.WHITE);
        }
        return cellRendererComponent;
    }
}

class IntegerDefaultTableCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
        int indexViewRow = table.getRowSorter().convertRowIndexToModel(row);
        Component cellRendererComponent = super.getTableCellRendererComponent(table, value, isSelected,hasFocus, indexViewRow, column);

        if (isSelected) {
            return cellRendererComponent;
        }

        if(table.getModel().getValueAt(indexViewRow, 13).equals("true")) {
            cellRendererComponent.setBackground(Color.RED);
        }
        else {
            cellRendererComponent.setBackground(Color.WHITE);
        }
        return cellRendererComponent;
    }
}

