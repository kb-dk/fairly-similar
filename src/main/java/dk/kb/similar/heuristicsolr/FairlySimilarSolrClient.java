package dk.kb.similar.heuristicsolr;

import java.util.ArrayList;


import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*/
 * Optimize index with:  curl 'http://localhost:8983/solr/fairlysimilar/update?optimize=true&maxSegments=1&waitFlush=false'
 */
public class FairlySimilarSolrClient {
 
    private Logger log = LoggerFactory.getLogger(FairlySimilarSolrClient.class);
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
  
  public void indexVectorJson(int id,String path, String imageName, String coordinates, boolean[] thresholds, int trues, double lengthSquared,  ArrayList<String> designations ,ArrayList<String> designationsAndProbability) throws Exception{
    SolrInputDocument doc = createDocJson(id, path, imageName,coordinates,thresholds, trues, lengthSquared, designations,designationsAndProbability);
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
  
  
  
  public  SolrInputDocument createDocJson(int id, String path, String imageName,String coordinates, boolean[] thresholds,int trues,double lengthSquared, ArrayList<String> designations, ArrayList<String> designationsAndProbability){
    SolrInputDocument doc = new SolrInputDocument();

      doc.setField("id",id);
      doc.setField("coordinates",coordinates);
      doc.setField("lengthsquared",lengthSquared);
      doc.setField("path", path);
      doc.setField("imagename", imageName);
      doc.setField("thresholds", trues);
            
      for (String designation : designations) {
        doc.addField("designation", designation);
      }
      
      for (String designAndProp: designationsAndProbability) {
        doc.addField("designation_probability",designAndProp);      
      }
      
      for (int i =0;i<thresholds.length ; i++) {
       doc.setField(i+"_threshold",thresholds[i]);//Dynamic field                 
     }
      
      
    return doc;
  }

  /*
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
  */
  
public ArrayList<SolrDocument> query(String query, int numberOfResults, boolean includeCoordinates) throws Exception{    
   System.out.println(query);   
    SolrQuery solrQuery = new  SolrQuery();        
    solrQuery.setQuery(query);

     //We never extract the dynamic field *_threshold
    if (!includeCoordinates) {
      solrQuery.setFields(new String[] {"id","designation", "path","imagename"}); 
    }
    else {
      solrQuery.setFields(new String[] {"id","designation", "path","imagename","coordinates"}); 
    }
    
    solrQuery.setRows(numberOfResults);    

    log.info("Query started");
    QueryResponse rsp =  solrServer.query(solrQuery);
    log.info("Query complete");
    log.info("Result parsing started");
    
    SolrDocumentList docs = rsp.getResults();
    log.info("Result parsing complete");
    
    //System.out.println("Solr client query time:"+(System.currentTimeMillis() - start));
    return docs;     
  }
  

public ArrayList<SolrDocument> queryIdOnly(String query, int numberOfResults) throws Exception{    
  SolrQuery solrQuery = new  SolrQuery();        
  solrQuery.setQuery(query);
  solrQuery.setRows(numberOfResults);    
  solrQuery.setFields(new String[] {"id"}); 
  QueryResponse rsp =  solrServer.query(solrQuery);      
  SolrDocumentList docs = rsp.getResults();                        
  //System.out.println("Solr client query time:"+(System.currentTimeMillis() - start) +" Intern solr elapsedtime:"+rsp.getElapsedTime());
  return docs;     
}

public SolrDocument getById(int id) throws Exception{    
  SolrQuery solrQuery = new  SolrQuery();        
  solrQuery.setQuery("id:"+id);     
  solrQuery.setFields(new String[] {"id","coordinates","designation_probability","imagename"}); 
  QueryResponse rsp =  solrServer.query(solrQuery);      
  SolrDocumentList docs = rsp.getResults();                        
  if (docs.size() !=1) {
    throw new RuntimeException("Not found ID:"+id);
  }
  //
  return docs.get(0);     
}



  
  public void commit() throws Exception {
    solrServer.commit();
  }
  
}
