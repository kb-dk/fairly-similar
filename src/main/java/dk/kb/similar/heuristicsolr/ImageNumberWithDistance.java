package dk.kb.similar.heuristicsolr;

public class ImageNumberWithDistance implements Comparable{
private String imageName;
 private Double distance;
private int lineNumber;

public Double getDistance() {
  return distance;
}
public void setDistance(Double distance) {
  this.distance = distance;
}
public int getLineNumber() {
  return lineNumber;
}
public void setLineNumber(int lineNumber) {
  this.lineNumber = lineNumber;
}


public String getImageName() {
  return imageName;
}
public void setImageName(String imageName) {
  this.imageName = imageName;
}
@Override
public int hashCode() {
  final int prime = 31;
  int result = 1;
  result = prime * result + lineNumber;
  return result;
}
@Override
public boolean equals(Object obj) {
  if (this == obj)
    return true;
  if (obj == null)
    return false;
  if (getClass() != obj.getClass())
    return false;
  ImageNumberWithDistance other = (ImageNumberWithDistance) obj;
  if (lineNumber != other.lineNumber)
    return false;
  return true;
}


@Override
public int compareTo(Object other) {
  ImageNumberWithDistance otherObj = (ImageNumberWithDistance) other;
  return this.distance.compareTo(otherObj.getDistance());
}

}
