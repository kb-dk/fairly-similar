package dk.kb.similar.heuristicsolr;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.solr.common.SolrDocument;

 

public class HeuristicSolrUtil {

  static String dataFile = "/home/teg/workspace/fairly-similar/src/main/resources/pixplot_vectors_270707.txt";

  public static void main(String[] args) throws Exception {
    int testId = 116530;
   
    // indexFile(file);
   //findAndListBestBruteForce(testId, 20);
    SortedSet<ImageNumberWithDistance> findAndListBestHeuristic = findAndListBestHeuristic(testId, 20);
   
   
    for (ImageNumberWithDistance c : findAndListBestHeuristic) {
      System.out.println(c.getLineNumber() + ":" + c.getDistance());
    }
   
  }
  

  public static SortedSet<ImageNumberWithDistance> findAndListBestHeuristic(int testId, int numberOfBest) throws Exception {
    double[] orgCoords = getCoordsFromLine(dataFile, testId);
    return findAndListBestHeuristic(testId, orgCoords, numberOfBest);   
  }

  public static    SortedSet<ImageNumberWithDistance> findAndListBestHeuristic(int testId,double[] orgCoords, int numberOfBest) throws Exception {
    //System.out.println("id:"+testId +" : coords:"+Arrays.toString(orgCoords));
    
    // Very interesting.. When searching, seems to better to use 40 markers
    boolean[] bitMapMarkers = getBitmapForMaxMarkers(orgCoords, 40);


    String query = buildOrQueriesFromBitmap(bitMapMarkers, testId);

    
      long time = System.currentTimeMillis();
    ArrayList<SolrDocument> docs = FairlySimilarSolrClient.getInstance().query(query, 2000);
         System.out.println(System.currentTimeMillis() - time);
         
    // Calculate distance for Solr hits, see if it finds low distance candidates
    SortedSet<ImageNumberWithDistance> findBestHeuristic = new TreeSet<ImageNumberWithDistance>();
    for (SolrDocument doc : docs) {
      String id = (String) doc.getFieldValue("id");
      String coordStr = (String) doc.getFieldValue("coordinates");

      double[] coords = convertLineToVector(coordStr);
      // int overlap = countBitMapOverlapFromSolr(""+testId, id);
      double dist = getDistanceSquared(orgCoords, coords);

      ImageNumberWithDistance img = new ImageNumberWithDistance();
      img.setDistance(dist);
      img.setLineNumber(Integer.parseInt(id));

      if (findBestHeuristic.size() < numberOfBest) {
        findBestHeuristic.add(img);
      } else if (img.getDistance() < findBestHeuristic.last().getDistance()) {
        findBestHeuristic.remove(findBestHeuristic.last());
        findBestHeuristic.add(img);
      }

    }
        
    return findBestHeuristic;
  }
  
  public static  ArrayList<String> getIdsForBestHeuristic(int testId,double[] orgCoords, int solrResults) throws Exception {
    
    //System.out.println("id:"+testId +" : coords:"+Arrays.toString(orgCoords));
    // Very interesting.. When searching, seems to better to use 40 markers
    boolean[] bitMapMarkers = getBitmapForMaxMarkers(orgCoords, 40);

    String query = buildOrQueriesFromBitmap(bitMapMarkers, testId);
         
     ArrayList<SolrDocument> docs = FairlySimilarSolrClient.getInstance().queryIdOnly(query, solrResults);            
    // Calculate distance for Solr hits, see if it finds low distance candidates
      ArrayList<String> ids = new ArrayList<String>();
      for (SolrDocument doc : docs) {
      String id = (String) doc.getFieldValue("id");
       ids.add(id);
      }
      return ids;
  }
  
  public static void findAndListBestBruteForce(int testId, int numberOfBest) throws Exception {
    double[] orgCoords = getCoordsFromLine(dataFile, testId);
    SortedSet<ImageNumberWithDistance> findBestBruteForce = findBestBruteForce(orgCoords, numberOfBest, dataFile);
    for (ImageNumberWithDistance c : findBestBruteForce) {
      System.out.println(c.getLineNumber() + ":" + c.getDistance());
    }
  }

  public static void indexFile(String file) throws Exception {
    FairlySimilarSolrClient solrClient = FairlySimilarSolrClient.getInstance();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String line;

      // Something to tweak.
      // value of 1.2 gives about ~55 coordinates
      // value of 1.5 gives ~20
      final double THRESHOLD = 1.5d; //

      int linesRead = 1;
      while ((line = br.readLine()) != null) {
        double lengthSquared = lengthSquared(line);
        double[] coords = convertLineToVector(line);

        // below are two different methods to build bitmap. First takes all over
        // a threshold. Second extract a given number with maximum.
        // boolean[] bitmap = buildThresholdBitmapFromLine(line, THRESHOLD);
        boolean[] bitmap = getBitmapForMaxMarkers(coords, 20);

        int trues = countTrues(bitmap);
        solrClient.indexVector(linesRead++, line, bitmap, trues, lengthSquared);
      }
      solrClient.commit();
    }
  }

  public static boolean[] getBitMapForLine(String file, int lineNumber) throws Exception {
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String line;
      final double THRESHOLD = 1.5d; // Something to tweak. value of 1.2 gives
      // ~50 coordinate over threshold
      int linesRead = 1;
      while ((line = br.readLine()) != null) {
        if (linesRead == lineNumber) {
          break;
        }
        linesRead++;
      }

      boolean[] bitmap = buildThresholdBitmapFromLine(line, THRESHOLD);
      return bitmap;
    }

  }

  public static double[] getCoordsFromLine(String file, int lineNumber) throws Exception {

    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String line;

      // ~50 coordinate over threshold
      int linesRead = 1;
      while ((line = br.readLine()) != null) {
        if (linesRead == lineNumber) {
          break;
        }
        linesRead++;
      }
      return convertLineToVector(line);
    }
  }

  public ArrayList<String> getBestMatchIDs(boolean[] bitmap, int excludeId) {
    return null;
  }

  private static double lengthSquared(String line) {
    String[] coords = line.split(" ");
    double sum = 0d;
    for (String coord : coords) {
      sum += Double.valueOf(coord) * Double.valueOf(coord);
    }
    return sum;
  }

  private static String buildOrQueriesFromBitmap(boolean[] bitmap, int excludeId) {
    StringBuilder builder = new StringBuilder();
    builder.append("(");
    for (int i = 0; i < bitmap.length; i++) {
      if (bitmap[i]) {
        builder.append(i + "_threshold:true OR ");
      }
    }
    builder.append("ID:NONE"); // Lazy, just ending the ORS
    builder.append(")");
    builder.append(" -id:"+excludeId);

    return builder.toString();
  }
  
  private static int countTrues(boolean[] bitMap) {
    int trues = 0;
    for (int i = 0; i < bitMap.length; i++) {
      if (bitMap[i]) {
        trues++;
      }
    }
    return trues;
  }
  
  //Not used. Constant numbers of markers was a better strategy
  private static boolean[] buildThresholdBitmapFromLine(String line, double threshold) {
    String[] coords = line.split(" ");
    boolean[] thresholdBitmap = new boolean[2048];
    int coordNumber = 0;
    for (String coord : coords) {
      double parseDouble = Double.parseDouble(coord);
      if (parseDouble > threshold) {
        thresholdBitmap[coordNumber++] = true;
      } else {
        thresholdBitmap[coordNumber++] = false;
      }
    }
    return thresholdBitmap;
  }

  private static int countBitMapOverlap(boolean[] b1, boolean[] b2) {

    int matches = 0;

    for (int i = 0; i < 2048; i++) {
      if (b1[i] && b2[i]) {
        matches++;
      }

    }

    return matches;

  }

  /*
   * Could how many true on same bits.
   */
  private static int countBitMapOverlapFromSolr(String id1, String id2) throws Exception {
    FairlySimilarSolrClient client = FairlySimilarSolrClient.getInstance();
    String query1 = "id:" + id1;
    String query2 = "id:" + id2;
    SolrDocument doc1 = client.query(query1, 1).get(0);
    SolrDocument doc2 = client.query(query2, 1).get(0);

    int matches = 0;
    for (int i = 0; i < 2048; i++) {
      boolean b1 = (Boolean) doc1.getFieldValue(i + "_threshold");
      boolean b2 = (Boolean) doc2.getFieldValue(i + "_threshold");
      if (b1 && b2) {
        matches++;
      }
    }
    // System.out.println(id1+":"+id2 +":" +doc1.getFieldValue("thresholds")
    // +":" +doc2.getFieldValue("thresholds"));
    return matches;
  }

  public static double getDistanceSquared(double[] v1, double[] v2) {
    double sumSquared = 0d;

    for (int i = 0; i < v1.length; i++) {
      double dif = v1[i] - v2[i];
      sumSquared += dif * dif;
    }

    return sumSquared;

  }

  public static double[] convertLineToVector(String line) {
    double[] vec = new double[2048];
    String[] coords = line.split(" ");
    int index = 0;
    for (String coord : coords) {
      vec[index++] = Double.valueOf(coord);
    }
    return vec;
  }

 

  /*
   * Return the numberOfMarkers coordinate entries with highest value
   * 
   */
  public static boolean[] getBitmapForMaxMarkers(double[] coords, int numberOfMarkers) {
    SortedSet<ImageNumberWithDistance> set = new TreeSet<>();
    for (int i = 0; i < coords.length; i++) {
      ImageNumberWithDistance img = new ImageNumberWithDistance();
      img.setLineNumber(i);
      img.setDistance(coords[i]);
      if (set.size() < numberOfMarkers) {
        set.add(img);
      } else if (img.getDistance() > set.first().getDistance()) {
        set.remove(set.first());
        // System.out.println("coord:"+i +" has score:"+img.getScore());
        set.add(img);
      }

    }
    boolean[] bitmap = new boolean[2048];

    for (ImageNumberWithDistance t : set) {
      bitmap[t.getLineNumber()] = true;
      // System.out.println(t.getLineNumber());
    }

    return bitmap;

  }

  /*
   * Returns numberOfHits with shortest distance. (linenumbers in the file) Uses
   * treeSet to keep track of numberOfHits best. CompareTo method compares score
   */
  public static SortedSet<ImageNumberWithDistance> findBestBruteForce(double[] coords, int numberOfHits, String file) throws Exception {

    SortedSet<ImageNumberWithDistance> set = new TreeSet<>();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String line;
      int linesRead = 1;

      while ((line = br.readLine()) != null) {
        double[] other = convertLineToVector(line);
        double dist = getDistanceSquared(coords, other);
        ImageNumberWithDistance img = new ImageNumberWithDistance();
        img.setLineNumber(linesRead);
        img.setDistance(dist);

        if (set.size() < numberOfHits) {
          set.add(img);
        } else if (img.getDistance() < set.last().getDistance()) {
          set.remove(set.last());
          set.add(img);
        }

        linesRead++;

      }
    }
    return set;
  }

  /*
  /*
   * Little complex method to best k hits in linear time. Not used yet.
   * 
   */
  
  /*
  static <T extends Comparable<T>> List<T> top(Iterable<? extends T> items, int k) {
    if (k < 0)
      throw new IllegalArgumentException();
    if (k == 0)
      return Collections.emptyList();
    PriorityQueue<T> top = new PriorityQueue<>(k);
    for (T item : items) {
      if (top.size() < k)
        top.add(item);
      else if (item.compareTo(top.peek()) > 0) {
        top.remove();
        top.add(item);
      }
    }
    List<T> hits = new ArrayList<>(top.size());
    while (!top.isEmpty())
      hits.add(top.remove());
    Collections.reverse(hits);
    return hits;
  }
*/
}