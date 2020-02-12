package webspy.max_jd;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.validator.routines.UrlValidator;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;




public class WebSpy extends JFrame{
    private List<URL> pages;
    private Spider spider;
    private JTable mainTable;
    private Deque<SeoUrl> deqSeoUrls;
    private Set<SeoUrl> imagesSeoUrls;
    private TunnerSeoURL tunner;
    private SeoUrlValidator validator;

    public static final org.apache.log4j.Logger logToFile =
            org.apache.log4j.Logger.getLogger("webspy.max_jd");

    private JProgressBar progressBar;
    private JMenu menuMenu;
    private JPanel playBar;

//need for transfer top menu among tabs
    private int tabPreviousIndex = 0;
    private int tabCurrentIndex = 0;

    private volatile StateWebsiteSeoChecker state = StateWebsiteSeoChecker.NOT_RUN_YET;
    private final Object lock = new Object();

    public static void main(String arg[]){
        logToFile.info("The program was started.");
        new WebSpy();
        System.out.print("The program is Ended!");
    }

    private void handleImg(HtmlPage parsingPage){
        DomNodeList<HtmlElement> listImages = parsingPage.getBody().getElementsByTagName("img");
        Set<String> setOfImage = new HashSet();

        for(HtmlElement el :listImages){
            String src = el.getAttribute("src");
            try{
                String qualifiedUrl = parsingPage.getFullyQualifiedUrl(src).toString();
                setOfImage.add(qualifiedUrl);

                if(SeoUrl.cacheContentTypePages.get(qualifiedUrl) == null)
                    SeoUrl.cacheContentTypePages.put(qualifiedUrl, validator.getContentType(qualifiedUrl));

            }catch(MalformedURLException ex){
                System.out.println(ex);
            }
        }

        TunnerSeoURL tuner = TunnerSeoURL.getTunner();

        for(String ordinaryUrlOfImage : setOfImage){
            SeoUrl seoUrl = new SeoUrl(ordinaryUrlOfImage, true);
            tuner.tunne(seoUrl, parsingPage);
            seoUrl.analyzeURL();

            seoUrl.statisticLinksOut.putIfAbsent(parsingPage.getUrl().toString(), new HashSet<>());
            seoUrl.statisticLinksOut.get(parsingPage.getUrl().toString()).add(seoUrl.toString());
            SeoUrl.statisticLinksOn.putIfAbsent(seoUrl.toString(), new HashSet<String>());
            SeoUrl.statisticLinksOn.get(seoUrl.toString()).add(parsingPage.getUrl().toString());
            imagesSeoUrls.add(seoUrl);
        }

    }

    private void updateTable(){
        logToFile.info("Updating table...");
        Object[] data = new Object[14];
        DefaultTableModel m = (DefaultTableModel) mainTable.getModel();
        m.setRowCount(0);
        int pageNumber = 1;

        for(SeoUrl seoUrl : deqSeoUrls){

            data[0]  = pageNumber++;
            data[1]  = seoUrl.getURL();
            data[2]  = seoUrl.getCanonical();
            data[3]  = seoUrl.getResponse();
            data[4]  = seoUrl.getTitle();
            data[5]  = seoUrl.getDescription();
            data[6]  = seoUrl.getKeywords();
            data[7]  = seoUrl.getCountH1();
            data[8]  = seoUrl.getContentType();
            data[9]  = seoUrl.getMetarobots();

            if(SeoUrl.externalLinks.get((seoUrl.getURL())) == null)
                data[10] = 0;
            else
                data[10] = SeoUrl.externalLinks.get((seoUrl.getURL())).size();

            System.out.println(seoUrl.getURL() + " is -11--- " + (SeoUrl.statisticLinksOn.get(seoUrl.getURL()) == null));
            data[11] = SeoUrl.statisticLinksOn.get(seoUrl.getURL()).size();
          //  System.out.println("Updating table..." + seoUrl);

            /*try{
            Thread.sleep(2000);
            }catch(Exception ex){
                System.out.println(ex);
            }
*/
            System.out.println(seoUrl.getURL() + " is -12--- " + (SeoUrl.statisticLinksOut.get(seoUrl.getURL()) == null));
            //data[12] = webspy.max_jd.SeoUrl.statisticLinksOut.get(seoUrl.getURL()).size();
            data[13] = seoUrl.getFlagSeoProblem().toString();

            m.addRow(data);
        }

        data = new Object[14];

        for(SeoUrl seoImage : imagesSeoUrls){
            data[0]  = pageNumber++;
            data[1]  = seoImage.getURL();
            data[3]  = seoImage.getResponse();
            data[8]  = seoImage.getContentType();
            data[11] = SeoUrl.statisticLinksOn.get(seoImage.getURL()).size();
            data[13] = seoImage.getFlagSeoProblem().toString();

            m.addRow(data);
        }

        m.fireTableDataChanged();
        mainTable.changeSelection(0,0,false,false);
        logToFile.info("Table was updated.");
    }

    private void loadProject(){
        logToFile.info("Loading project");
        javax.swing.JFileChooser chooser = new JFileChooser("E:\\");
        chooser.setFileFilter(
                new FileNameExtensionFilter(".ser", "ser"));
        int state = chooser.showOpenDialog(this);

        if(state == JFileChooser.APPROVE_OPTION){
            try(FileInputStream f2 = new FileInputStream(chooser.getSelectedFile());
                ObjectInputStream inStreamOb = new ObjectInputStream(f2)) {

                    deqSeoUrls = (Deque)inStreamOb.readObject();
                    imagesSeoUrls = (Set<SeoUrl>) inStreamOb.readObject();
                    SeoUrl.statisticLinksOn = (Map<String, HashSet<String>>) inStreamOb.readObject();
                    SeoUrl.statisticLinksOut = (Map<String, HashSet<String>>)inStreamOb.readObject();
                    SeoUrl.externalLinks = (Map<String, HashSet<String>>) inStreamOb.readObject();
                    SeoUrl.cacheContentTypePages = (Map<String,String>) inStreamOb.readObject();

            }catch(java.io.FileNotFoundException ex){
                logToFile.error(ex.toString());
                System.out.println(ex);
            }catch(java.io.IOException ex){
                logToFile.error(ex.toString());
                System.out.println(ex);
            }catch(ClassNotFoundException ex){
                logToFile.error(ex.toString());
                System.out.println(ex);
            }finally{
                updateTable();
            }
        }
        logToFile.info("Project was loaded.");
    }

    private void saveProject(){
        logToFile.info("Saving project.");
        String pathFile = "E:\\" + spider.startingURL.getHost() + ".ser";
        System.out.println(pathFile);

        try(FileOutputStream fOut = new FileOutputStream(pathFile);
            ObjectOutputStream obOut = new ObjectOutputStream(fOut)) {

            obOut.writeObject(deqSeoUrls);
            obOut.writeObject(imagesSeoUrls);
            obOut.writeObject(SeoUrl.statisticLinksOn);
            obOut.writeObject(SeoUrl.statisticLinksOut);
            obOut.writeObject(SeoUrl.externalLinks);
            obOut.writeObject(SeoUrl.cacheContentTypePages);

        }catch(FileNotFoundException ex ){
            logToFile.error(ex.toString());
            System.out.println(ex);
        }catch(IOException ex) {
            logToFile.error(ex.toString());
            System.out.println(ex);
        }

        logToFile.info("Project was saved.");
    }

    public void initGUI(){
        logToFile.info("Initialization GUI...");
        setName("WebsiteSeoChecker");

        JMenuBar menuBar = new JMenuBar();
        menuMenu = new JMenu("Menu");

        JMenu saveLoadProjSubMenu = new JMenu("Project...");
        JMenuItem saveProjectItem = new JMenuItem("Save");
        saveProjectItem.setEnabled(false);
        JMenuItem loadProjectItem = new JMenuItem("Load");
        saveLoadProjSubMenu.add(saveProjectItem);
        saveLoadProjSubMenu.add(loadProjectItem);

        saveProjectItem.addActionListener((e) -> saveProject());
        loadProjectItem.addActionListener((e) -> loadProject());

        JMenuItem exportMenuItem = new JMenuItem("Export to excel");
        exportMenuItem.setEnabled(false);
        exportMenuItem.addActionListener( (actionEvent) -> {
            System.out.println("Export to excel was pressed");

            //do 2 array to combine 2 collection
            SeoUrl[] arraySeoUrls = new SeoUrl[deqSeoUrls.size()];
            deqSeoUrls.toArray(arraySeoUrls);
            SeoUrl[] arraySeoImages = new SeoUrl[imagesSeoUrls.size()];
            imagesSeoUrls.toArray(arraySeoImages);


            SeoUrl[] arraySeoUrlsAndImages = ArrayUtils.addAll(arraySeoUrls,arraySeoImages);
            ExcelWriter.writeToFile(Paths.get("E:\\report.xlsx"),arraySeoUrlsAndImages);
        });

        menuMenu.add(exportMenuItem);
        menuMenu.add(saveLoadProjSubMenu);

        menuBar.add(menuMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem supportMenuItem = new JMenuItem("Support");
        supportMenuItem.addActionListener((actionEvent) -> {
            JOptionPane.showMessageDialog(WebSpy.this, "To contact " +
                            "with me\t\n write to here juncpp@gmail.com", "Support",
                    JOptionPane.PLAIN_MESSAGE);
        });

        helpMenu.add(supportMenuItem);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        ImageIcon image = new ImageIcon("C:\\Users\\Maxim\\Downloads\\spider-icon.png");
        setIconImage(image.getImage());

System.out.println(System.getProperty("user.dir"));
        System.out.println(System.getProperty(System.getProperty("user.dir")) + "\\JAVA\\IdeaProjects\\WebSpy\\src\\main\\resources\\play.png");

                ImageIcon startIcon = new ImageIcon(
                new ImageIcon(System.getProperty("user.dir") + "\\src\\main\\resources\\play.png").getImage().getScaledInstance(20, 20, Image.SCALE_DEFAULT));
        ImageIcon pauseIcon = new ImageIcon(
                new ImageIcon(System.getProperty("user.dir") + "\\src\\main\\resources\\pause.png").getImage().getScaledInstance(20, 20, Image.SCALE_DEFAULT));
        ImageIcon stopIcon = new ImageIcon(
                new ImageIcon(System.getProperty("user.dir") + "\\src\\main\\resources\\stop.png").getImage().getScaledInstance(20, 20, Image.SCALE_DEFAULT));

        JButton playBut = new  JButton(startIcon);
        JButton pauseBut = new  JButton(pauseIcon);
        pauseBut.setName("Pause Button");
        JButton stopBut  = new  JButton(stopIcon);
        stopBut.setName("Stop Button");

        pauseBut.setEnabled(false);
        stopBut.setEnabled(false);

        pauseBut.addActionListener((actionEve) -> {
            state = StateWebsiteSeoChecker.PAUSED;
            pauseBut.setEnabled(false);
            playBut.setEnabled(true);
            progressBar.setVisible(false);
            saveProjectItem.setEnabled(true);
            exportMenuItem.setEnabled(true);
        });

        stopBut.addActionListener((action) -> {
            state = StateWebsiteSeoChecker.STOPPED;
            stopBut.setEnabled(false);
            pauseBut.setEnabled(false);
            playBut.setEnabled(true);
            progressBar.setVisible(false);
            saveProjectItem.setEnabled(true);
            exportMenuItem.setEnabled(true);
        });


        Object[][] rows = {};
        Object[] nameColumns = {"#", "URL", "Canonical", "Response", "Title", "Description", "Keywords",
                "H1", "Content-Type", "Meta-Robots", "Ex. links", "In links", "Out links", "Problem"};

        JTable table = new JTable(new CustomizedDefaultTableModel(rows, nameColumns)){

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

        table.setAutoCreateRowSorter(true);

        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(false);


        table.addMouseListener(new MouseAdapter(){
           @Override
           public void mousePressed(MouseEvent e){
               if(table.getRowCount() != 0){
                   if(e.getClickCount() == 2){
                       Point point =  e.getPoint();

                       int indexView = table.rowAtPoint(point);
                       String url = (String) table.getModel().getValueAt(
                               table.convertRowIndexToModel(indexView),1);
                       if(Desktop.isDesktopSupported()){
                           try {
                               Desktop.getDesktop().browse(new URI(url));
                           }catch(URISyntaxException ex){
                               System.out.format("%s%n%s", "Oops! Something went wrong!", ex);
                               JOptionPane.showMessageDialog(WebSpy.this, "Oops! Can't open this link!",
                                       "Error: URISyntaxException" , JOptionPane.ERROR_MESSAGE);
                           }catch(IOException ex){
                               JOptionPane.showMessageDialog(WebSpy.this, "Oops! Can't open this link!",
                                       "Error: IOException", JOptionPane.ERROR_MESSAGE);
                               System.out.format("%s%n%s", "Oops! Something went wrong!", ex);
                           }
                       }

                   }
               }
           }
        });

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {

                int indexRowModel = table.getRowSorter().convertRowIndexToModel(row);
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, indexRowModel, column);

                if (isSelected)
                    return c;
                else if ((table.getModel().getValueAt(indexRowModel, 13)).equals("true"))
                    c.setBackground(Color.RED);
                else
                    c.setBackground(Color.CYAN.WHITE);

                return c;
            }
        });

        table.setDefaultRenderer(Integer.class, new DefaultTableCellRenderer(){
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column){
                int indexRowModel = table.getRowSorter().convertRowIndexToModel(row);
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, indexRowModel, column);

                if (isSelected)
                    return c;
                else if((table.getModel().getValueAt(indexRowModel, 13)).equals("true"))
                    c.setBackground(Color.RED);
                else
                    c.setBackground(Color.WHITE);

                return c;
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(50);

        //need replace in the future
        mainTable = table;

        JTable tableTabError = new JTable(table.getModel());
        tableTabError.setAutoCreateRowSorter(true);
        tableTabError.getTableHeader().setReorderingAllowed(false);
        JTabbedPane tabs = new JTabbedPane();

        tableTabError.setDefaultRenderer(Object.class, new ObjectDefaultTableCellRenderer());
        tableTabError.setDefaultRenderer(Integer.class, new IntegerDefaultTableCellRenderer());


        JScrollPane scrollForTableError = new JScrollPane(tableTabError);
        JPanel errorPanel = new JPanel(new BorderLayout());
        errorPanel.add(scrollForTableError, BorderLayout.CENTER);

        JPanel jpFilterTabFirst = new JPanel();
        JTable tableFilterTab = new JTable();
        jpFilterTabFirst.setLayout(new FlowLayout(FlowLayout.LEFT));

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


        JTable tableFilterTabResult = new JTable(table.getModel());
        tableFilterTabResult.getTableHeader().setReorderingAllowed(false);

        TableRowSorter<DefaultTableModel> sorter =
                new TableRowSorter<>(((DefaultTableModel)tableFilterTabResult.getModel()));
        tableFilterTabResult.setRowSorter(sorter);

        searchButt.addActionListener((actionEvent)-> {
            System.out.println("Search was pressed!");
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

        JPanel filterPanel = new JPanel(new BorderLayout());
        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.add(jpFilterTabFirst);
        innerPanel.add(new JScrollPane(tableFilterTabResult));

        filterPanel.add(innerPanel, BorderLayout.CENTER);

        JTextField mainPage = new JTextField(); //?? why we need this?
        mainPage.setPreferredSize(new Dimension(200, 24));

        playBut.addActionListener((actionEvent)-> {
            progressBar.setVisible(true);
            pauseBut.setEnabled(true);
            stopBut.setEnabled(true);
            playBut.setEnabled(false);
            String webSiteToParse =
                    //"https://conditioner-service.com.ua/";
                    //"https://tie.com.ua/image/cache/data/2017/12/";
                    "https://conditionservice.com.ua/";
                   // "https://conditioner-service.com.ua/";  //in the future mainPage.getText();
            //"https://climatbud.com.ua/
            validator = new SeoUrlValidator(webSiteToParse,
                    new String[]{"http", "https"}, SeoUrlValidator.ALLOW_2_SLASHES); //in the future mainPage.getText();
            runSpider(webSiteToParse);
        });


        JPanel scanPanel = new JPanel();
        scanPanel.setName("scanPanel");
        scanPanel.setLayout(new BorderLayout());

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        JPanel topPanel = new JPanel(new GridBagLayout());
        JPanel innerPan1 = new JPanel();
        JPanel innerPan2 = new JPanel();
        innerPan1.add(mainPage);
        innerPan1.add(playBut);
        innerPan1.add(pauseBut);
        innerPan1.add(stopBut);
        playBar = innerPan1;

        innerPan2.add(this.progressBar);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.5f;
        topPanel.add(innerPan1, gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.FIRST_LINE_END;
        topPanel.add(innerPan2, gbc);

        scanPanel.add(scrollPane, BorderLayout.CENTER);
        scanPanel.add(topPanel, BorderLayout.NORTH);

        tabs.addTab("Scan", scanPanel);
        tabs.addTab("Error", errorPanel);
        tabs.addTab("Filter", filterPanel);

        tabs.addChangeListener((changeEvent)->{
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
                System.out.println(tableTabError.getSelectedRow());
                System.out.println(tableTabError.getSelectedColumn());
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
            System.out.println(2);
            JPanel temp1 = ((JPanel)(tabs.getComponentAt(tabPreviousIndex)));
            temp1.remove(topPanel);

            JPanel temp2 = ((JPanel)(tabs.getComponentAt(tabCurrentIndex)));
            temp2.add(topPanel, BorderLayout.NORTH);
        });

        tabs.addChangeListener((changeEvent)->{
            System.out.println(3);
            tabPreviousIndex = tabCurrentIndex;
            tabCurrentIndex = tabs.getSelectedIndex();
            System.out.println("Previous index " + tabPreviousIndex);
            System.out.println("Current index " + tabCurrentIndex);
        });


        add(tabs);

        setUpPopupMenu(table);
        setUpPopupMenu(tableTabError);
        setUpPopupMenu(tableFilterTabResult);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(800, 800));
        setLocationRelativeTo(null);
        pack();
        setVisible(true);

        logToFile.info("GUI was initialized.");
       // runSpider();
    }

    WebSpy(){
        logToFile.info("Initialization webspy.max_jd.WebSpy...");
        pages = new java.util.ArrayList<URL>();
        deqSeoUrls = new LinkedList<>();
        tunner = TunnerSeoURL.getTunner();
        imagesSeoUrls = Collections.synchronizedSet(new HashSet<>());
        initGUI();
        logToFile.info("webspy.max_jd.WebSpy was initialized.");
    }

    //create and set up popups menu on a table
    private void setUpPopupMenu(JTable table){
        logToFile.info("Setting up pop-up menu");
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem exLinksMI = new JMenuItem("URL properties");

        exLinksMI.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){

                JDialog dialog = new JDialog(WebSpy.this, "Our project", true);
                dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.PAGE_AXIS));


                JLabel pageUrlLab, linksOnPageLab, linksFromPageLab, externalLinksLab;
                pageUrlLab = new JLabel("Page URL:");
                linksOnPageLab = new JLabel("Links to this page:");
                linksFromPageLab = new JLabel("Links from this page:");
                externalLinksLab = new JLabel("External links:");

                JTextField pageUrlField = new JTextField();
                JTextArea linksOnPageTA = new JTextArea(),
                        linksFromPageTA = new JTextArea(),
                        externalLinksTA = new JTextArea();

                pageUrlField.setEditable(false);
                linksOnPageTA.setEditable(false);
                linksFromPageTA.setEditable(false);
                externalLinksTA.setEditable(false);

                JScrollPane scrollLinksOn = new JScrollPane(linksOnPageTA, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),

                            scrollLinksFrom = new JScrollPane(linksFromPageTA, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),

                            scrollExternalLinks = new JScrollPane(externalLinksTA, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),

                            scrollPageUrlField = new JScrollPane(pageUrlField, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

                scrollLinksOn.setPreferredSize(new Dimension(50, 70));
                scrollLinksFrom.setPreferredSize(new Dimension(50, 70));
                scrollExternalLinks.setPreferredSize(new Dimension(50, 70));
                pageUrlField.setPreferredSize(new Dimension(70, 20));

                JButton closeButton = new JButton("Close");
                closeButton.addActionListener((ae)-> dialog.dispose());
                closeButton.setAlignmentX(JComponent.LEFT_ALIGNMENT);

                int viewSelectedRow = table.getSelectedRow();
                int modelSelectedRow = table.convertRowIndexToModel(viewSelectedRow);
                String selectedUrl = (String) table.getModel().getValueAt(modelSelectedRow, 1);

                pageUrlField.setText(selectedUrl);

                Set<String> linksOn = SeoUrl.statisticLinksOn.get(selectedUrl);
                for(String s : linksOn){
                    linksOnPageTA.append(s + "\n");
                }

                Set<String> linksOut = SeoUrl.statisticLinksOut.get(selectedUrl);
                if(linksOut != null)
                for(String s : linksOut){
                    linksFromPageTA.append(s + "\n");
                }

                for(SeoUrl s : deqSeoUrls){
                    if (s.getURL().equals(selectedUrl)){
                        String columnExternalLinks = "";
                        if(SeoUrl.externalLinks.get(s.getURL()) != null) {
                            for (String externalUrl : SeoUrl.externalLinks.get(s.getURL()))
                                columnExternalLinks += externalUrl + System.lineSeparator();
                        }
                        externalLinksTA.setText(columnExternalLinks);
                        break;
                    }
                }

                dialog.add(pageUrlLab);
                dialog.add(new JPanel().add(scrollPageUrlField));
                dialog.add(linksOnPageLab);
                dialog.add(scrollLinksOn);
                dialog.add(linksFromPageLab);
                dialog.add(scrollLinksFrom);
                dialog.add(externalLinksLab);
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

        JMenuItem openUrlInBrowserMI = new JMenuItem("Open in browser");

        openUrlInBrowserMI.addActionListener((ae) -> {
            if(Desktop.isDesktopSupported()){
                int viewIndex = table.getSelectedRow();
                int modelIndex = table.convertRowIndexToModel(viewIndex);
                String url = (String) table.getModel().getValueAt(modelIndex, 1);
                try {
                    Desktop.getDesktop().browse(new URI(url));
                }catch(java.net.URISyntaxException ex){
                    logToFile.error(ex.toString());
                    System.out.println(ex);
                }catch(java.io.IOException ex){
                    logToFile.error(ex.toString());
                    System.out.println(ex);
                }
            }
        });

        JMenuItem googleCacheMI = new JMenuItem("Open in Google's cache");
        googleCacheMI.addActionListener((event)->{
            if(Desktop.isDesktopSupported()){
                int viewIndexRow = table.getSelectedRow();
                int modelIndexRow = table.convertRowIndexToModel(viewIndexRow);
                String url = "http://webcache.googleusercontent.com/search?q=cache:" + (String) table.getModel().getValueAt(modelIndexRow, 1);
                try {
                    Desktop.getDesktop().browse(new URI(url));
                }catch(java.net.URISyntaxException ex){
                    logToFile.error(ex.toString());
                    System.out.println(ex);
                }catch(java.io.IOException ex){
                    logToFile.error(ex.toString());
                    System.out.println(ex);
                }
            }
        });

        popupMenu.add(exLinksMI);
        popupMenu.add(openUrlInBrowserMI);
        popupMenu.add(googleCacheMI);

        table.setComponentPopupMenu(popupMenu);
        logToFile.info("Pop-up menu was set up");
    }

    private void createConcurrentUpdaterForTable(){

        Thread updater = new Thread() {
            @Override
            public void run() {
                while(state == StateWebsiteSeoChecker.RUNNING){
                    SwingUtilities.invokeLater(() -> updateTable());
                    try{
                        Thread.sleep(5000);
                    }catch(InterruptedException ex){
                        System.out.println(ex);
                        logToFile.info(ex.toString());
                    }
                }
            }

        };

        updater.start();
    }

    void runSpider(String websiteMainPage){
        logToFile.info("Running spider...");
        if(spider == null){
            try {
                spider = new Spider(new URL(websiteMainPage));
            }catch(java.net.MalformedURLException ex){
                logToFile.error(ex.toString());
                System.out.println(ex);
            }
        }
        logToFile.info("Start scanning.");
        spider.scanMain();
    }

    private void  supplyDataForTable(){
        Set<String> setOfKeys = SeoUrl.statisticLinksOut.keySet();
        Iterator<String> iterSetKeys = setOfKeys.iterator();

        while(iterSetKeys.hasNext()) {
            String key = iterSetKeys.next();
            SeoUrl.statisticLinksOn.put(key, new HashSet<String>());
        }

        Iterator<Map.Entry<String, HashSet<String>>> iterEntryLinksOn = SeoUrl.statisticLinksOn.entrySet().iterator();

        while(iterEntryLinksOn.hasNext()) {
            Map.Entry<String, HashSet<String>> entryLinksOn = iterEntryLinksOn.next();

            for (Map.Entry<String, HashSet<String>> entryTracking : SeoUrl.statisticLinksOut.entrySet()) {
                // if one page refers to this page, then we make a mark in webspy.max_jd.SeoUrl.statisticLinksOn, that
                // it refers to this one
                if (entryTracking.getValue().contains(entryLinksOn.getKey()))
                    entryLinksOn.getValue().add(entryTracking.getKey());
            }
        }
    }

    private class Spider {
        URL startingURL;
        java.util.Queue<URL> queue;

        public Spider(URL startURL){
            logToFile.info("Initialization spider...");

            this.startingURL = startURL;
            queue = new java.util.LinkedList<URL>();

            logToFile.info("Spider was initialized.");
        }



        void saveToFile(java.nio.file.Path path, java.util.Deque<String> deque){
            logToFile.info("Saving to file" + path);
            try(FileWriter fw = new FileWriter(path.toFile());
                BufferedWriter bw = new BufferedWriter(fw)) {
                for(String s : deque){
                    bw.write(s + "\r\n");
                }
            }catch(IOException ex){
                logToFile.error(ex.toString());
                System.out.println(ex);
            }
            logToFile.info("To file was saved.");
        }

        void scan4() {
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
            System.out.println("Domain: " + domain);

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
                        System.out.println("MalformedURLException " + ex);
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



        void scan2(){
            logToFile.info("Scanning website...");
            if(state == StateWebsiteSeoChecker.NOT_RUN_YET ||
                    state == StateWebsiteSeoChecker.SCANING_ENDED ||
                    state == StateWebsiteSeoChecker.STOPPED){
                SwingWorker sw = new SwingWorker() {
                    @Override
                    protected Void doInBackground(){
                        if(state == StateWebsiteSeoChecker.NOT_RUN_YET || state == StateWebsiteSeoChecker.SCANING_ENDED ||
                                state == StateWebsiteSeoChecker.STOPPED){
                            state = StateWebsiteSeoChecker.RUNNING;

                            System.out.println("Updating table...");
                            createConcurrentUpdaterForTable();

                            System.out.println("Start the program");
                            Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);

                            WebClient wc = new WebClient(BrowserVersion.CHROME);
                            wc.getOptions().setRedirectEnabled(false);
                            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
                            wc.getOptions().setJavaScriptEnabled(false);
                            // wc.getOptions().setThrowExceptionOnScriptError(false);


                            try {
                                HtmlPage parsingPage = wc.getPage(startingURL);
                                System.out.println("Start URL is " + startingURL.toString());
                                logToFile.info("Start URL is " + parsingPage.getUrl().toString());
                                do{
                                    ifProgramWasPausedThenWait();
                                    if(wasProgramStopped())
                                        return null;

                                    spider.handleImgs(parsingPage);

                                    SeoUrl seoUrlParsingPage = new SeoUrl(parsingPage.getUrl().toString());
                                    tunner.tunne(seoUrlParsingPage, parsingPage);
                                    seoUrlParsingPage.analyzeURL();


                                    SeoUrl.statisticLinksOut.putIfAbsent(parsingPage.getUrl().toString(), new HashSet<String>());

                                    deqSeoUrls.addFirst(seoUrlParsingPage);

                                    List<HtmlAnchor> anchorsParsingPage = parsingPage.getAnchors();
                                    for (HtmlAnchor an : anchorsParsingPage) {
                                        String tempUrl = parsingPage.getFullyQualifiedUrl(an.getHrefAttribute().toString()).toString();

                                        if(deqSeoUrls.contains(tempUrl))
                                            continue;

                                        //check if tempUrl not equal startingURL without backslash
                                        if (tempUrl.equals((startingURL.toString()).substring(0, startingURL.toString().length() - 1))){
                                            System.out.println("Url with out back slash " + tempUrl);
                                            SeoUrl.statisticLinksOut.putIfAbsent(parsingPage.getUrl().toString(),
                                                    new HashSet<String>());
                                            SeoUrl.statisticLinksOut.get(parsingPage.getUrl().toString()).add(tempUrl);
                                            SeoUrl so = new SeoUrl(tempUrl);

                                            //for determine that on this link someone links
                                            SeoUrl.statisticLinksOut.putIfAbsent(tempUrl, new HashSet<String>());

                                            HtmlPage parsingPageForSettingSeoUrl = wc.getPage(tempUrl);
                                            tunner.tunne(so, parsingPageForSettingSeoUrl);
                                            so.analyzeURL();
                                            deqSeoUrls.addFirst(so);
                                            continue;
                                        }

                                        if (validator.isSchemeHttpOrHttps(tempUrl)) {
                                            //check if the link lead to external site
                                            if( ! validator.isSameHost(new URL(tempUrl) )) {
                                                SeoUrl.externalLinks.putIfAbsent(seoUrlParsingPage.getURL(), new HashSet<String>());
                                                SeoUrl.externalLinks.get(seoUrlParsingPage.getURL()).add(tempUrl);
                                                continue;
                                            }

                                            //check if it is an anchor link, then cutting
                                            if (validator.havePoundSign(new URL(tempUrl)))
                                                tempUrl = tempUrl.substring(0, tempUrl.indexOf('#'));
                                            SeoUrl.statisticLinksOut.get(parsingPage.getUrl().toString()).add(tempUrl);

                                            if( SeoUrl.cacheContentTypePages.get(tempUrl)== null ){
                                                SeoUrl.cacheContentTypePages.put(tempUrl, validator.getContentType(tempUrl));
                                            }

                                            //if it is image (or something else) - put a link in the statistics Map and continue. Image (or something else)  can't follow to another page.
                                            if( ! (SeoUrl.cacheContentTypePages.get(tempUrl).startsWith("text/html") )){
                                                    if(SeoUrl.cacheContentTypePages.get(tempUrl).startsWith("image/"))
                                                        spider.handleImgs(tempUrl, parsingPage);
                                                    else {
                                                        System.out.println(SeoUrl.cacheContentTypePages.get(tempUrl));
                                                        SeoUrl.statisticLinksOut.get(parsingPage.getUrl().toString()).add(tempUrl);
                                                    }
                                                continue;
                                            }


                                            SeoUrl tempSeoUrl = new SeoUrl(tempUrl);
                                            if (!(deqSeoUrls.contains(tempSeoUrl))) {
                                                System.out.print('.');
                                                deqSeoUrls.addLast(tempSeoUrl);
                                            }
                                        }
                                    }
                                    String nextUrl = (deqSeoUrls.pollLast()).getURL();
                                    System.out.println("Parse URL is :" + nextUrl);
                                    parsingPage = wc.getPage(nextUrl);
                                    logToFile.info("Parsing page is " + parsingPage.getUrl().toString());

                                    //if(delay != 0){
                                        // try{
                                        //    Thread.sleep(delay);
                                        //}catch(InterruptedException ex){
                                        //    System.out.println(ex);
                                        // }
                                    // }

                                } while (!((startingURL.toString()).equals(parsingPage.getUrl().toString())));

                                //repeat for the start URL and add to the deque
                                SeoUrl firstPage = new SeoUrl(startingURL.toString());
                                tunner.tunne(firstPage, parsingPage);
                                firstPage.analyzeURL();
                                deqSeoUrls.addFirst(firstPage);
                            }catch(
                                    IOException ex) {
                                logToFile.error(ex.toString());
                                System.out.println(ex);
                            }

                            supplyDataForTable();

                            state = StateWebsiteSeoChecker.SCANING_ENDED;
                            wc.close();

                        }else if(state == StateWebsiteSeoChecker.PAUSED){
                            state = StateWebsiteSeoChecker.RUNNING;
                            this.notify();
                            System.out.println("Updating table...");
                            createConcurrentUpdaterForTable();
                        }
                        return null;
                    }
                    @Override
                    protected void done(){
                        if(state == StateWebsiteSeoChecker.SCANING_ENDED){
                            menuMenu.getItem(0).setEnabled(true);// it does exportMenuItem.setEnabled(true);
                            ((JMenu)(menuMenu.getMenuComponent(1))).getItem(0).setEnabled(true);//it does saveProjectItem.setEnabled(true)
                            ((JButton)playBar.getComponent(1)).setEnabled(true); // it does playBut.setEnabled(true);
                            ((JButton)playBar.getComponent(2)).setEnabled(false); // it does pauseBut.setEnabled(false);
                            ((JButton)playBar.getComponent(3)).setEnabled(false); // it does pauseBut.setEnabled(false);

                            progressBar.setVisible(false);
                            updateTable();
                            logToFile.info("The website was scanned.");
                        }
                    }

                };
                sw.execute();
            }else if(state == StateWebsiteSeoChecker.PAUSED){
                Runnable runToNotify = new Runnable(){
                    @Override
                    public void run(){
                        System.out.println(Thread.currentThread().getName());
                        synchronized(lock){
                            lock.notify();
                        }
                    }
                };
                new Thread(runToNotify).start();
            }

        }


        void scan(){
            if(state == StateWebsiteSeoChecker.NOT_RUN_YET ||
                    state == StateWebsiteSeoChecker.SCANING_ENDED ||
                    state == StateWebsiteSeoChecker.STOPPED){
            SwingWorker sw = new SwingWorker() {
                @Override
                protected Void doInBackground(){
                    if(state == StateWebsiteSeoChecker.NOT_RUN_YET || state == StateWebsiteSeoChecker.SCANING_ENDED ||
                            state == StateWebsiteSeoChecker.STOPPED){
                        state = StateWebsiteSeoChecker.RUNNING;

                        System.out.println("Start the program");
                        Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);

                        WebClient wc = new WebClient(BrowserVersion.CHROME);
                        wc.getOptions().setRedirectEnabled(false);
                        wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
                        wc.getOptions().setJavaScriptEnabled(false);
                        // wc.getOptions().setThrowExceptionOnScriptError(false);


                        Deque<SeoUrl> d = new LinkedList<>();
                        deqSeoUrls = d;

                        try {
                            HtmlPage parsingPage = wc.getPage(startingURL);
                            System.out.println("Start URL is " + startingURL.toString());

                            do{
                                    ifProgramWasPausedThenWait();
                                    if(wasProgramStopped())
                                        return null;

                                    SeoUrl seoUrlParsingPage = new SeoUrl(parsingPage.getUrl().toString());
                                    tunner.tunne(seoUrlParsingPage, parsingPage);
                                    seoUrlParsingPage.analyzeURL();
                                    SeoUrl.statisticLinksOut.putIfAbsent(parsingPage.getUrl().toString(), new HashSet<String>());

                                    d.addFirst(seoUrlParsingPage);

                                    List<HtmlAnchor> anchorsParsingPage = parsingPage.getAnchors();
                                    for (HtmlAnchor an : anchorsParsingPage) {
                                        String tempUrl = parsingPage.getFullyQualifiedUrl(an.getHrefAttribute().toString()).toString();

                                        //check if tempUrl not equal startingURL without backslash
                                        if (tempUrl.equals((startingURL.toString()).substring(0, startingURL.toString().length() - 1)))
                                            continue;

                                        if (tempUrl.startsWith("http")) {
                                            //check if the link lead to external site
                                            if (!(new URL(tempUrl).getHost().equals(startingURL.getHost()))) {
                                               /// seoUrlParsingPage.addExternalLinks(tempUrl.toString()); was deleted, now it is a static field
                                                continue;
                                            }

                                            //check if it is an anchor link, then cutting
                                            if ((tempUrl.contains("#")))
                                                tempUrl = tempUrl.substring(0, tempUrl.indexOf('#'));

                                            // webspy.max_jd.SeoUrl.statisticLinksOut.putIfAbsent(parsingPage.getUrl().toString(), new HashSet<String>());
                                            SeoUrl.statisticLinksOut.get(parsingPage.getUrl().toString()).add(tempUrl);

                                            SeoUrl tempSeoUrl = new SeoUrl(tempUrl);
                                            if (!(d.contains(tempSeoUrl))) {
                                                System.out.print('.');
                                                d.addLast(tempSeoUrl);
                                            }
                                        }
                                    }
                                        String nextUrl = (d.pollLast()).getURL();
                                        System.out.println("Parse URL is :" + nextUrl);
                                        parsingPage = wc.getPage(nextUrl);

                                        //try{
                                        //    Thread.sleep(1000);
                                        //}catch(InterruptedException ex){
                                        //    System.out.println(ex);
                                        // }

                                    } while (!((startingURL.toString()).equals(parsingPage.getUrl().toString())));

                                    //repeat for the start URL and add to the deque
                                    SeoUrl firstPage = new SeoUrl(startingURL.toString());
                                    tunner.tunne(firstPage, parsingPage);
                                    firstPage.analyzeURL();
                                    d.addFirst(firstPage);
                                }catch(
                                IOException ex) {
                                    System.out.println(ex);
                                }

                                Set<String> setOfKey = SeoUrl.statisticLinksOut.keySet();
                                Iterator<String> iterSetKey = setOfKey.iterator();

                            while(iterSetKey.hasNext()) {
                                SeoUrl.statisticLinksOn.put(iterSetKey.next(), new HashSet<String>());
                            }

                            Iterator<Map.Entry<String, HashSet<String>>> iterEntryLinksOn = SeoUrl.statisticLinksOn.entrySet().iterator();

                        while(iterEntryLinksOn.hasNext()) {
                                Map.Entry<String, HashSet<String>> entryLinksOn = iterEntryLinksOn.next();

                                for (Map.Entry<String, HashSet<String>> entryTracking : SeoUrl.statisticLinksOut.entrySet()) {
                                    // if one page refers to this page, then we make a mark in webspy.max_jd.SeoUrl.statisticLinksOn, that
                                    // it refers to this one
                                    if (entryTracking.getValue().contains(entryLinksOn.getKey()))
                                        entryLinksOn.getValue().add(entryTracking.getKey());
                                }
                        }

                        state = StateWebsiteSeoChecker.SCANING_ENDED;
                        wc.close();

                    }else if(state == StateWebsiteSeoChecker.PAUSED){
                        state = StateWebsiteSeoChecker.RUNNING;
                        this.notify();
                    }
                        return null;
                }
                @Override
                protected void done(){
                    if(state == StateWebsiteSeoChecker.SCANING_ENDED){
                        menuMenu.getItem(0).setEnabled(true);// it does exportMenuItem.setEnabled(true);
                        ((JMenu)(menuMenu.getMenuComponent(1))).getItem(0).setEnabled(true);//it does saveProjectItem.setEnabled(true)
                        ((JButton)playBar.getComponent(1)).setEnabled(true); // it does playBut.setEnabled(true);
                        ((JButton)playBar.getComponent(2)).setEnabled(false); // it does pauseBut.setEnabled(false);
                        ((JButton)playBar.getComponent(3)).setEnabled(false); // it does pauseBut.setEnabled(false);

                        progressBar.setVisible(false);
                        updateTable();
                    }
                }

            };
                sw.execute();
            }else if(state == StateWebsiteSeoChecker.PAUSED){
                   Runnable runToNotify = new Runnable(){
                    @Override
                            public void run(){
                                System.out.println(Thread.currentThread().getName());
                                synchronized(lock){
                                    lock.notify();
                                }
                    }
                   };
                   new Thread(runToNotify).start();
                }

        }

        void scanMain(){
            logToFile.info("Scanning website...");
            if(state == StateWebsiteSeoChecker.NOT_RUN_YET ||
                    state == StateWebsiteSeoChecker.SCANING_ENDED ||
                    state == StateWebsiteSeoChecker.STOPPED){
                SwingWorker sw = new SwingWorker() {
                    @Override
                    protected Void doInBackground(){
                        if(state == StateWebsiteSeoChecker.NOT_RUN_YET || state == StateWebsiteSeoChecker.SCANING_ENDED ||
                                state == StateWebsiteSeoChecker.STOPPED){
                            state = StateWebsiteSeoChecker.RUNNING;

                            System.out.println("Create a thread for updating table...");
                            //createConcurrentUpdaterForTable();

                            System.out.println("Start the program");
                            Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);

                            WebClient wc = new WebClient(BrowserVersion.CHROME);
                            wc.getOptions().setRedirectEnabled(false);
                            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
                            wc.getOptions().setJavaScriptEnabled(false);
                            // wc.getOptions().setThrowExceptionOnScriptError(false);


                            try {
                                HtmlPage parsingPage = wc.getPage(startingURL);
                                System.out.println("Start URL is " + startingURL.toString());
                                logToFile.info("Start URL is " + parsingPage.getUrl().toString());
                                do{
                                    ifProgramWasPausedThenWait();
                                    if(wasProgramStopped())
                                        return null;

                                    spider.handleImgs(parsingPage);

                                    SeoUrl seoUrlParsingPage = new SeoUrl(parsingPage.getUrl().toString());
                                    tunner.tunne(seoUrlParsingPage, parsingPage);
                                    seoUrlParsingPage.analyzeURL();
                                    SeoUrl.statisticLinksOut.putIfAbsent(parsingPage.getUrl().toString(), new HashSet<String>());

                                    deqSeoUrls.addFirst(seoUrlParsingPage);

                                    List<HtmlAnchor> anchorsParsingPage = parsingPage.getAnchors();
                                    for (HtmlAnchor an : anchorsParsingPage) {
                                        String tempUrl = parsingPage.getFullyQualifiedUrl(an.getHrefAttribute().toString()).toString();

                                        if(deqSeoUrls.contains(tempUrl))
                                            continue;

                                        //check to see if tempUrl not equal startingURL without backslash
                                        if (tempUrl.equals((startingURL.toString()).substring(0, startingURL.toString().length() - 1))){

                                            SeoUrl.statisticLinksOut.putIfAbsent(parsingPage.getUrl().toString(), new HashSet<String>());
                                            SeoUrl.statisticLinksOut.get(parsingPage.getUrl().toString()).add(tempUrl.toString());
                                            SeoUrl.statisticLinksOn.putIfAbsent(tempUrl.toString(), new HashSet<String>());
                                            SeoUrl.statisticLinksOn.get(tempUrl.toString()).add(parsingPage.getUrl().toString());

                                            SeoUrl so = new SeoUrl(tempUrl);
                                            HtmlPage parsingPageForSettingSeoUrl = wc.getPage(tempUrl);
                                            tunner.tunne(so, parsingPageForSettingSeoUrl);
                                            so.analyzeURL();
                                            deqSeoUrls.addFirst(so);
                                            continue;
                                        }

                                        if (validator.isSchemeHttpOrHttps(tempUrl)) {
                                            //check if the link lead to external site
                                            if( ! validator.isSameHost(new URL(tempUrl) )) {
                                                SeoUrl.externalLinks.putIfAbsent(seoUrlParsingPage.getURL(), new HashSet<String>());
                                                SeoUrl.externalLinks.get(seoUrlParsingPage.getURL()).add(tempUrl);
                                                continue;
                                            }

                                            //check if it is an anchor link, then cutting
                                            if (validator.havePoundSign(new URL(tempUrl)))
                                                tempUrl = tempUrl.substring(0, tempUrl.indexOf('#'));


                                            //need to delete
                                           // webspy.max_jd.SeoUrl.statisticLinksOut.get(parsingPage.getUrl().toString()).add(tempUrl);
                                           // webspy.max_jd.SeoUrl.statisticLinksOn.putIfAbsent(tempUrl.toString(), new HashSet<String>());
                                          //  webspy.max_jd.SeoUrl.statisticLinksOn.get(tempUrl.toString()).add(parsingPage.toString());


                                            SeoUrl.cacheContentTypePages.putIfAbsent(tempUrl, validator.getContentType(tempUrl));


                                            //if it is an image (or something else) - put a link in the statistics Map and continue.
                                            // An image (or something else)  can't refer to another page.
                                            if( ! (SeoUrl.cacheContentTypePages.get(tempUrl).startsWith("text/html") )){
                                                if(SeoUrl.cacheContentTypePages.get(tempUrl).startsWith("image/"))
                                                    spider.handleImgs(tempUrl, parsingPage);
                                                continue;
                                            }


                                            SeoUrl.statisticLinksOut.get(parsingPage.getUrl().toString()).add(tempUrl);
                                            SeoUrl.statisticLinksOn.putIfAbsent(tempUrl.toString(), new HashSet<String>());
                                            SeoUrl.statisticLinksOn.get(tempUrl.toString()).add(parsingPage.toString());


                                            SeoUrl tempSeoUrl = new SeoUrl(tempUrl);
                                            if (!(deqSeoUrls.contains(tempSeoUrl))) {
                                                System.out.print('.');
                                                deqSeoUrls.addLast(tempSeoUrl);
                                            }
                                        }
                                    }
                                    String nextUrl = (deqSeoUrls.pollLast()).getURL();
                                    System.out.println("Parse URL is :" + nextUrl);
                                    parsingPage = wc.getPage(nextUrl);
                                    logToFile.info("Parsing page is " + parsingPage.getUrl().toString());

                                    //if(delay != 0){
                                    // try{
                                    //    Thread.sleep(delay);
                                    //}catch(InterruptedException ex){
                                    //    System.out.println(ex);
                                    // }
                                    // }
                                    updateTable();
                                } while (!((startingURL.toString()).equals(parsingPage.getUrl().toString())));

                                //repeat for the start URL and add to the deque
                                SeoUrl firstPage = new SeoUrl(startingURL.toString());
                                tunner.tunne(firstPage, parsingPage);
                                firstPage.analyzeURL();
                                deqSeoUrls.addFirst(firstPage);
                            }catch(
                                    IOException ex) {
                                logToFile.error(ex.toString());
                                System.out.println(ex);
                            }

                            state = StateWebsiteSeoChecker.SCANING_ENDED;
                            wc.close();

                        }else if(state == StateWebsiteSeoChecker.PAUSED){
                            state = StateWebsiteSeoChecker.RUNNING;
                            this.notify();
                            System.out.println("Updating table...");
                            createConcurrentUpdaterForTable();
                        }
                        return null;
                    }
                    @Override
                    protected void done(){
                        if(state == StateWebsiteSeoChecker.SCANING_ENDED){
                            menuMenu.getItem(0).setEnabled(true);// it does exportMenuItem.setEnabled(true);
                            ((JMenu)(menuMenu.getMenuComponent(1))).getItem(0).setEnabled(true);//it does saveProjectItem.setEnabled(true)
                            ((JButton)playBar.getComponent(1)).setEnabled(true); // it does playBut.setEnabled(true);
                            ((JButton)playBar.getComponent(2)).setEnabled(false); // it does pauseBut.setEnabled(false);
                            ((JButton)playBar.getComponent(3)).setEnabled(false); // it does pauseBut.setEnabled(false);

                            progressBar.setVisible(false);
                            updateTable();
                            logToFile.info("The website was scanned.");
                        }
                    }

                };
                sw.execute();
            }else if(state == StateWebsiteSeoChecker.PAUSED){
                Runnable runToNotify = new Runnable(){
                    @Override
                    public void run(){
                        System.out.println(Thread.currentThread().getName());
                        synchronized(lock){
                            lock.notify();
                        }
                    }
                };
                new Thread(runToNotify).start();
            }

        }


        private void handleImgs(String imageFromTagA, HtmlPage page){
            SeoUrl urlToImage = new SeoUrl(imageFromTagA, true);
            SeoUrl.cacheContentTypePages.putIfAbsent(imageFromTagA, validator.getContentType(imageFromTagA));
            TunnerSeoURL tunner = TunnerSeoURL.getTunner();
            tunner.tunne(urlToImage, page);
            urlToImage.analyzeURL();

            imagesSeoUrls.add(urlToImage);
            SeoUrl.statisticLinksOut.putIfAbsent(page.getUrl().toString(), new HashSet<String>());
            SeoUrl.statisticLinksOut.get(page.getUrl().toString()).add(imageFromTagA);
            SeoUrl.statisticLinksOn.putIfAbsent(urlToImage.toString(), new HashSet<String>());
            SeoUrl.statisticLinksOn.get(page.getUrl().toString()).add(page.getUrl().toString());
        }

        private void handleImgs(HtmlPage parsingPage){
            DomNodeList<HtmlElement> listImages = parsingPage.getBody().getElementsByTagName("img");
            Set<String> setOfImage = new HashSet();

            for(HtmlElement el :listImages){
                String src = el.getAttribute("src");
                try{
                    String qualifiedUrl = parsingPage.getFullyQualifiedUrl(src).toString();
                    setOfImage.add(qualifiedUrl);

                    SeoUrl.cacheContentTypePages.putIfAbsent(qualifiedUrl, validator.getContentType(qualifiedUrl));

                }catch(MalformedURLException ex){
                    System.out.println(ex);
                }
            }

            TunnerSeoURL tuner = TunnerSeoURL.getTunner();

            for(String ordinaryUrlOfImage : setOfImage){
                SeoUrl seoUrl = new SeoUrl(ordinaryUrlOfImage, true);
                tuner.tunne(seoUrl, parsingPage);
                seoUrl.analyzeURL();

                seoUrl.statisticLinksOut.putIfAbsent(parsingPage.getUrl().toString(), new HashSet<>());
                seoUrl.statisticLinksOut.get(parsingPage.getUrl().toString()).add(seoUrl.toString());
                SeoUrl.statisticLinksOn.putIfAbsent(seoUrl.toString(), new HashSet<String>());
                SeoUrl.statisticLinksOn.get(seoUrl.toString()).add(parsingPage.getUrl().toString());
                imagesSeoUrls.add(seoUrl);
            }

        }
    }

    private void ifProgramWasPausedThenWait(){
        synchronized(lock){
            while(state == StateWebsiteSeoChecker.PAUSED){
                try{
                    lock.wait();
                    System.out.println("Woke up");
                    state = StateWebsiteSeoChecker.RUNNING;
                }catch(InterruptedException ex){
                    logToFile.error(ex.toString());
                    System.out.println(ex);
                }
            }
        }
    }

    private boolean wasProgramStopped(){
        return state == StateWebsiteSeoChecker.STOPPED;
    }
}


class CustomizedDefaultTableModel extends DefaultTableModel{
    CustomizedDefaultTableModel(Object[][] rows, Object[] columns){
        super(rows, columns);
    }

    @Override
    public boolean isCellEditable(int row, int column){
        return false;
    }
    @Override
    public Class getColumnClass(int column){
        switch(column){
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
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, indexView, column);

        if (isSelected)
            return c;

        if(table.getModel().getValueAt(indexView, 13).equals("true")){
            c.setBackground(Color.RED);}
        else{
            c.setBackground(Color.WHITE);}
        return c;
    }
}

class IntegerDefaultTableCellRenderer extends DefaultTableCellRenderer{
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
        int indexViewRow = table.getRowSorter().convertRowIndexToModel(row);
        Component c = super.getTableCellRendererComponent(table, value, isSelected,hasFocus, indexViewRow, column);

        if (isSelected)
            return c;

        if(table.getModel().getValueAt(indexViewRow, 13).equals("true")){
            c.setBackground(Color.RED);}
        else{
            c.setBackground(Color.WHITE);}

        return c;
    }
}
