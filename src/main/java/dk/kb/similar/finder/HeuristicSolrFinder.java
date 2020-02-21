/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.similar.finder;

import java.util.ArrayList;

import dk.kb.similar.MultiDimPoints;
import dk.kb.similar.Nearest;
import dk.kb.similar.NearestNeighbour;
import dk.kb.similar.heuristicsolr.HeuristicSolrUtil;

/**
 * Require solr server running.
 * Uses Solr index and matches markers for highest values of coordinates. (DNA/Spectral lines)
 * In Solr each document has 20 markers for which bit are amoung the 20 highest.
 */
public class HeuristicSolrFinder
extends NearestFinder {

    public HeuristicSolrFinder(MultiDimPoints multiDimPoints) {
        super(multiDimPoints);     
    }
    
    @Override
    public Nearest findNearest(int basePoint) {
      
      if (NearestNeighbour.POINTS_DEFAULT != 270707) {
        throw new RuntimeException("NearestNeighbour.POINTS_DEFAULT must be set to whole corpus (270707)");
      }
     
         //Solr ids starts from 1...
         int solrId=basePoint+1;
               
         //Need coordinates for the solr method 
         double[] coords = getCoordinatesForId(basePoint);
                 
         ArrayList<String> ids = new ArrayList<String>();
         
         try {
            ids = HeuristicSolrUtil.getIdsForBestHeuristic(solrId, coords, 500);
         }
         catch(Exception e) {
           e.printStackTrace();
           throw new RuntimeException("Solr error"+e.getMessage());
         }
         
         // Calculate distances for the 100 candidates
         double bestDist = 10000000;
         int bestId=-1;
         
         for (String id_solr : ids) {
           int id = Integer.parseInt(id_solr)-1; //Solr id's start from 1.
           double dist = exactDistanceSquared(id, basePoint);
           //double dist = atMostDistanceSquared(bestDist,id, basePoint);
           if (dist < bestDist) {
             bestDist = dist;
             bestId=id;
           }                      
         }              
         
        //System.out.println("ID:"+basePoint +":" +bestId  +" distance:"+ bestDist);         
        return new Nearest(basePoint, bestId, bestDist);
    }
    
    public double[] getCoordinatesForId(int id) {
      double[] coords = new double[2048]; 
      for (int i=0;i<2048;i++) {
        coords[i]=multiDimPoints.get(i, id); 
     }
     return coords;
      
    }
    @Override
    public Nearest findNearest(int basePoint, int startPoint, int endPoint) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    protected double getDistance(double shortest, int basePoint, int point) {
        return atMostDistanceSquared(shortest, basePoint, point);
    }

}
