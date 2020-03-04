package dk.kb.similar.heuristicsolr;

import java.util.ArrayList;
import java.util.Arrays;

public class JsonLineParsed {

 private String path;
 private String imageName;
 private ArrayList<Prediction> predictions;
 private double[] vector;
 
 
  public String getPath() {
  return path;
}


public void setPath(String path) {
  this.path = path;
}

public String getImageName() {
  return imageName;
}


public void setImageName(String imageName) {
  this.imageName = imageName;
}


public ArrayList<Prediction> getPredictions() {
  return predictions;
}


public void setPredictions(ArrayList<Prediction> predictions) {
  this.predictions = predictions;
}


public double[] getVector() {
  return vector;
}


public void setVector(double[] vector) {
  this.vector = vector;
}

  public class Prediction{
    String designation;
    double probability;
    public String getDesignation() {
      return designation;
    }
    public void setDesignation(String designation) {
      this.designation = designation;
    }
    public double getProbability() {
      return probability;
    }
    public void setProbability(double probability) {
      this.probability = probability;
    }
    @Override
    public String toString() {
      return "Prediction [designation=" + designation + ", probability=" + probability + "]";
    }    
    
  }

  @Override
  public String toString() {
    return "JsonLineParsed [path=" + path + ", imageName=" + imageName + ", predictions=" + predictions + ", vector=" + Arrays.toString(vector) + "]";
  }

}
