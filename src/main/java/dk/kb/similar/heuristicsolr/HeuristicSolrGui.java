package dk.kb.similar.heuristicsolr;


import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;


import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;

public class HeuristicSolrGui extends JFrame {

  private static final long serialVersionUID = 1L;
  //  static String file = "/home/teg/workspace/fairly-similar/pixplot_vectors_270707.txt";
  static String imageFileNames = "/home/teg/workspace/fairly-similar/files_fixed.txt";
  static JTextField textField;
  static SortedSet<ImageNumberWithDistance> bestImages = new TreeSet<ImageNumberWithDistance>();
  // Menu
  JMenuBar menuBar;
  JMenu jMenu_about;
  JMenuItem menuItem_info;
  static HashMap<Integer, String> fileNames;
  private static HeuristicSolrGui gui;

  public static void main(String s[]) throws Exception {
    gui = new HeuristicSolrGui();
    gui.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });

    gui.setTitle("FairySimilar Swing Gui by TEG");
    gui.init();
    gui.pack();
    gui.setVisible(true);
    gui.setResizable(true);// todo change

    // Load image file list
    fileNames = loadFileNames(imageFileNames);
    System.out.println(fileNames.size());

  }

  public void createGui() {

    JButton startButton;

    // create a new frame to store text field and button

    JLabel text = new JLabel("Enter linenumber:");
    textField = new JTextField(5);
    textField.setEditable(true);

    startButton = new JButton("Start");
    startButton.addActionListener(new FindImagesAction());
    // add panel to frame

    JPanel p = new JPanel( new FlowLayout());
    p.add(text);
    p.add(startButton);
    p.add(textField);

    getContentPane().add(p);

  }

  public void init() {
    createMenu();
    createGui();

  }

  public void createMenu() {

    menuBar = new JMenuBar();
    menuBar.setBorder(new BevelBorder(BevelBorder.RAISED));

    jMenu_about = new JMenu("About");
    menuItem_info = new JMenuItem("Info");

    jMenu_about.add(menuItem_info);
    // menuItem_info.addActionListener(new JoBSimulatorApplet.HelpEvent());

    menuBar.add(jMenu_about);
    setJMenuBar(menuBar);

  }

  class FindImagesAction implements ActionListener {
    public void actionPerformed(ActionEvent e) {

      try {
        String lineNumber = textField.getText();

        SortedSet<ImageNumberWithDistance> best = HeuristicSolrUtil.findAndListBestHeuristic(Integer.parseInt(lineNumber),200);
        
        for (ImageNumberWithDistance current : best) {        
          current.setFilePath( fileNames.get(current.getLineNumber()));
        }

        bestImages = best; // Now static variable that can be used by rendering frame
        createGallery();
        
      } catch (Exception ex) {
        ex.printStackTrace();
      }

    }
  }

public static void createGallery() {

      JFrame galleryFrame = new JFrame();
      galleryFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);//Garbage collect
      galleryFrame.setTitle("Shortest distance");
      
      
      galleryFrame.setPreferredSize(new Dimension(1200,800));         
      JPanel galleryPanel = new JPanel (new GridBagLayout());
      
      JScrollPane scrollPane = new JScrollPane(galleryPanel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      scrollPane.setPreferredSize(new Dimension(600,800));            
      galleryFrame.getContentPane().add(scrollPane);
      galleryFrame.setVisible(true);
      galleryFrame.setResizable(false);
      
       
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.NONE;
      gbc.insets = new Insets(5, 2, 5, 2);
      
      
      
      int i=0;
      for (ImageNumberWithDistance  current : bestImages) {
        
        
        ImageIcon pic = new ImageIcon(current.getFilePath());
        ImageIcon picScaled = scaleImage(pic, 200, 200);
        String [] tokens = current.getFilePath().split("/");

        JLabel label  =  new JLabel (tokens[tokens.length-1] +" ("+current.getDistance()+")", picScaled, JLabel.CENTER);
        label.setVerticalTextPosition(JLabel.BOTTOM);
        label.setHorizontalTextPosition(JLabel.CENTER);
        gbc.gridy = i/3;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;          
        galleryPanel.add( label,gbc);
                        
       i++;
      }
       
       
      
      
      
      galleryFrame.add(scrollPane);
      galleryFrame.pack();
      galleryFrame.setVisible(true);
     
      

  }

  public static ImageIcon scaleImage(ImageIcon icon, int w, int h) {
    int nw = icon.getIconWidth();
    int nh = icon.getIconHeight();

    if (icon.getIconWidth() > w) {
      nw = w;
      nh = (nw * icon.getIconHeight()) / icon.getIconWidth();
    }

    if (nh > h) {
      nh = h;
      nw = (icon.getIconWidth() * nh) / icon.getIconHeight();
    }

    return new ImageIcon(icon.getImage().getScaledInstance(nw, nh, Image.SCALE_DEFAULT));
  }

  public static HashMap<Integer, String> loadFileNames(String file) throws Exception {
    System.out.println("reading fileNames file:" + file);
    HashMap<Integer, String> imageMap = new HashMap<Integer, String>();
    try (BufferedReader br = new BufferedReader(new FileReader(file,Charset.forName("UTF-8")))) {
      String line;
      int linesRead = 1;

      while ((line = br.readLine()) != null) {
        String lineFixed=line.substring(0, line.length() -4);
        lineFixed=lineFixed.replace("image_vectors","display");
        imageMap.put(linesRead,lineFixed);
        linesRead++;
      }

    }
    return imageMap;
  }

}
