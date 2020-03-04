package dk.kb.similar.heuristicsolr;


import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.SortedSet;
import java.util.TreeSet;


import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
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

  private static DecimalFormat df2 = new DecimalFormat("#.##");

  private Logger log = LoggerFactory.getLogger(HeuristicSolrGui.class);

  private static final String imageFolder ="/media/teg/1200GB_SSD/display/";
  private static final long serialVersionUID = 1L;
  //  static String file = "/home/teg/workspace/fairly-similar/pixplot_vectors_270707.txt";
  static JTextField textField;
  static SortedSet<ImageNumberWithDistance> bestImages = new TreeSet<ImageNumberWithDistance>();
  // Menu
  JMenuBar menuBar;
  JMenu jMenu_about;
  JMenuItem menuItem_info;
  private static HeuristicSolrGui gui;
  private static JPanel galleryPanel = new JPanel (new GridBagLayout()); 
  String[] matchTypes = { "Nearest", "Predictions" , "Mixed", "Random"};  
  JComboBox<String> matchTypeBox = new JComboBox<String>(matchTypes);
  
  public static void main(String s[]) throws Exception {
    gui = new HeuristicSolrGui();
    gui.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });

    gui.setTitle("FairlySimilar Swing Gui by TEG");
    gui.init();
    gui.pack();
    gui.setVisible(true);
    gui.setResizable(true);// todo change

  }

  public void createGui() {
    JFXPanel t = new JFXPanel();
    JButton startButton;

    // create a new frame to store text field and button

    JLabel text = new JLabel("Image number:");
    textField = new JTextField(5);
    textField.setEditable(true);

    startButton = new JButton("Start");
    startButton.addActionListener(new FindImagesAction());
    // add panel to frame

 
    
    JPanel p = new JPanel(new BorderLayout());
    p.add(matchTypeBox,BorderLayout.NORTH);
    p.add(text,BorderLayout.WEST);
    p.add(textField,BorderLayout.CENTER);
    p.add(startButton,BorderLayout.EAST);
      
      
    JScrollPane scrollPane = new JScrollPane(galleryPanel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setPreferredSize(new Dimension(1400,800));            
    p.add(scrollPane,BorderLayout.SOUTH);    
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

        String match = (String) matchTypeBox.getSelectedItem();
        
        log.info(match);
        
        if (match.equals(matchTypes[0])) {
          System.out.println("heuristicmarkers selected");
          SortedSet<ImageNumberWithDistance> best = HeuristicSolrUtil.findAndListBestHeuristicMarkers(Integer.parseInt(lineNumber),200);
           bestImages = best; // Now static variable that can be used by rendering frame
        }
        else if (match.equals(matchTypes[1])) {
          System.out.println("predictions selected");
          SortedSet<ImageNumberWithDistance> best =  HeuristicSolrUtil.findAndListBestHeuristicPredictions(Integer.parseInt(lineNumber),200);
          bestImages = best; // Now static variable that can be used by rendering frame
        }
        else if (match.equals(matchTypes[2])) {
          System.out.println("mixed selected");
          SortedSet<ImageNumberWithDistance> best =  HeuristicSolrUtil.findAndListBestHeuristicMixed(Integer.parseInt(lineNumber),200);
          bestImages = best; // Now static variable that can be used by rendering frame
        }
        else if (match.equals(matchTypes[3])) {
          System.out.println("Random");
          SortedSet<ImageNumberWithDistance> best =  HeuristicSolrUtil.findRandomImages(200);
          bestImages = best; // Now static variable that can be used by rendering frame
        }
        
        bestImages.stream().limit(10).forEach(image -> log.info("{}  {}", image.getLineNumber()+1, image.getDistance()));
        createGallery();
   
        
      } catch (Exception ex) {
        ex.printStackTrace();
      }

    }
  }

public  void createGallery() {
      galleryPanel.removeAll();
       
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.NONE;
      gbc.insets = new Insets(5, 2, 5, 2);
      
      int i=0;

      for (ImageNumberWithDistance  current : bestImages) {       
        String score2Digits=df2.format(current.getDistance());
        JLabel label  = new JLabel (current.getImageName() +" ("+ score2Digits+")", new ImageIcon(new BufferedImage(200,200,BufferedImage.TYPE_INT_ARGB)), JLabel.CENTER);                

        label.setVerticalTextPosition(JLabel.BOTTOM);
        label.setHorizontalTextPosition(JLabel.CENTER);
        gbc.gridy = i/4;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;          
        galleryPanel.add( label,gbc);
        label.addMouseListener(new ImageClickedMouseListener(current.getImageName()));
        RenderGalleryImageThread thread = new RenderGalleryImageThread(imageFolder +current.getImageName(),label);
        thread.start();
        
       i++;
      }
      
      galleryPanel.revalidate();

  }

class CloseFramedMouseListener implements MouseListener{
    JFrame frame ;
  
  public CloseFramedMouseListener( JFrame frame ) {
    this.frame=frame;
  }
  
  @Override
  public void mouseClicked(MouseEvent m) {
   frame.dispose();
  }

  @Override
  public void mouseEntered(MouseEvent arg0) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void mouseExited(MouseEvent arg0) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void mousePressed(MouseEvent arg0) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void mouseReleased(MouseEvent arg0) {
    // TODO Auto-generated method stub
    
  }
  
}

class ImageClickedMouseListener implements MouseListener{

  String image = null;
  
  public ImageClickedMouseListener(String image) {
    this.image=image;
  }
    
  @Override
  public void mouseClicked(MouseEvent m) {

    System.out.println("Clicked image:"+image);
    JFrame singleImageFrame = new JFrame();
    singleImageFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);//Garbage collect
    ImageIcon imageFull = new ImageIcon(imageFolder +image);
    JLabel label  =  new JLabel (imageFull);
    label.addMouseListener(new CloseFramedMouseListener(singleImageFrame));
    singleImageFrame.getContentPane().add(label);
    singleImageFrame.pack();
    singleImageFrame.setVisible(true);
 
  }

  @Override
  public void mouseEntered(MouseEvent arg0) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void mouseExited(MouseEvent arg0) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void mousePressed(MouseEvent arg0) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void mouseReleased(MouseEvent arg0) {
    // TODO Auto-generated method stub
    
  }
  
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
  
  
  class RenderGalleryImageThread extends Thread {
      JLabel label=null;
      String imageFile = null;
    public RenderGalleryImageThread(String imageFile, JLabel label){
        this.imageFile=imageFile;
        this.label=label;
        this.setPriority(Thread.MIN_PRIORITY);
    }


     boolean stop=false;
     public void run() {
       
       try {
         new JFXPanel();
         
        String imageURL = Path.of(imageFile).toUri().toURL().toString();

       // Using JavaFX with requestedWidth & requestedHeight _should_ make intelligent load-time scaling of JPEGs
       javafx.scene.image.Image image = new javafx.scene.image.Image(imageURL, 200, 200, true, true);

       BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
       ImageIcon pic = new ImageIcon(bImage);
       //ImageIcon pic = new ImageIcon(imageFolder +current.getImageName());
       
       label.setIcon(pic);
       label.repaint();
       /*
         ImageIcon pic = new ImageIcon(imageFile);
         ImageIcon picScaled = scaleImage(pic, 200, 200);
         label.setIcon(picScaled);
       */
       
       }
       catch(Exception e) {
         e.printStackTrace();
       }
    }

    public void interrupt(){
        stop=true;
    }
}



}
