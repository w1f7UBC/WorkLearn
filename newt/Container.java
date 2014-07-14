package newt;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;

class Container{

	public static Position p;


	  private static Container instance = null;
	  private void Container(){

	  }
	  
	  void setPosition(Position pos){
		  p=pos;
	  }
	  
	  public Position getPosition(){
		  return p;
		  
	  }
	  
	  
	  public static Container getInstance(){
	    if(instance==null){
	       instance = new Container();
	       p = new Position(new LatLon(Angle.fromDegrees(0.0), Angle.fromDegrees(0.0)), 0);
	      }
	      return instance;
	  }
	}