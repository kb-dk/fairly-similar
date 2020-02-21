package dk.kb.similar.heuristicsolr;

import java.util.ArrayList;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;


/*/
 * Optimize index with:  curl 'http://localhost:8983/solr/fairlysimilar/update?optimize=true&maxSegments=1&waitFlush=false'
 */
public class FairlySimilarSolrClient {
 
  private final static String SOLR_URL ="http://teg-desktop.sb:8983/solr/fairlysimilar";
  
  protected static HttpSolrClient solrServer;
  

  private static FairlySimilarSolrClient instance;
  private static boolean initialized=false;

  //Singleton  
  public static FairlySimilarSolrClient getInstance() {
    if (!initialized) {            
      solrServer = new HttpSolrClient.Builder(SOLR_URL).withConnectionTimeout(10000).withSocketTimeout(60000).build();
      initialized=true;  
      instance= new FairlySimilarSolrClient();
      return instance;
    }else {
      return instance;
    }    
  }
  
  public void indexVector(int id, String coordinates, boolean[] thresholds, int trues, double lengthSquared) throws Exception{
    SolrInputDocument doc = createDoc(id, coordinates,thresholds, trues, lengthSquared);
    solrServer.add(doc);  
  }
  
  public  SolrInputDocument createDoc(int id, String coordinates, boolean[] thresholds,int trues,double lengthSquared){
    SolrInputDocument doc = new SolrInputDocument();

      doc.setField("id",id);
      doc.setField("coordinates",coordinates);
      doc.setField("lengthsquared",lengthSquared);
      doc.setField("thresholds", trues);
      for (int i =0;i<thresholds.length ; i++) {
       doc.setField(i+"_threshold",thresholds[i]);//Dynamic field                 
     }
          
    return doc;
  }
  
  public ArrayList<String> getBestMatchIds(String query, int numberOfResults) throws Exception{
    
    SolrQuery solrQuery = new  SolrQuery();        
    solrQuery.setQuery(query);
    solrQuery.setRows(numberOfResults);    
    solrQuery.setSort("score ",ORDER.desc);
    QueryResponse rsp =  solrServer.query(solrQuery);    
    SolrDocumentList docs = rsp.getResults();                        

    ArrayList<String> ids =  new ArrayList<String>();    
    for (SolrDocument doc : docs) {
        String id = (String) doc.getFieldValue("id");
    //    System.out.println(id +":"+doc.getFieldValue("score"));
        ids.add(id);
    }
            
    return ids;     
  }
  
public ArrayList<SolrDocument> query(String query, int numberOfResults) throws Exception{    
    long start = System.currentTimeMillis();
    SolrQuery solrQuery = new  SolrQuery();        
    solrQuery.setQuery(query);
    solrQuery.setRows(numberOfResults);    

    QueryResponse rsp =  solrServer.query(solrQuery);    
    SolrDocumentList docs = rsp.getResults();                            
    //System.out.println("Solr client query time:"+(System.currentTimeMillis() - start));
    return docs;     
  }
  

public ArrayList<SolrDocument> queryIdOnly(String query, int numberOfResults) throws Exception{    
  //System.out.println("q2:"+query +" results:"+numberOfResults);
  long start = System.currentTimeMillis();
  SolrQuery solrQuery = new  SolrQuery();        
  solrQuery.setQuery(query);
  solrQuery.setRows(numberOfResults);    
  solrQuery.setFields(new String[] {"id"}); 
  QueryResponse rsp =  solrServer.query(solrQuery);      
  SolrDocumentList docs = rsp.getResults();                        
  //System.out.println("Solr client query time:"+(System.currentTimeMillis() - start) +" Intern solr elapsedtime:"+rsp.getElapsedTime());
  return docs;     
}


  
  public void commit() throws Exception {
    solrServer.commit();
  }
  
}