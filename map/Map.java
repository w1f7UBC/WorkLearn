package map;

import java.awt.Color;
import java.util.LinkedList;
import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Polyline;
import javax.swing.JFrame;

public class Map {
	
	public static void main(String[] args) {

		//create a WorldWind main object
		WorldWindowGLCanvas worldWindCanvas = new WorldWindowGLCanvas();
		worldWindCanvas.setModel(new BasicModel());

		//build Java swing interface
		JFrame frame = new JFrame("World Wind");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(worldWindCanvas);
		frame.setSize(800,600);
		frame.setVisible(true);
		
		//create some "Position" to build a polyline
		LinkedList<Position> list = new LinkedList<Position>();
		for(int i = 0 ; i < 90 ; i++) {
			//in this case, points are in geographic coordinates.
			//If you are using cartesian coordinates, you have to convert them to geographic coordinates.
			//Maybe, there are some functions doing that in WWJ API...
			list.add(Position.fromDegrees(i,0.0,i*20000));
		}
		
		//create "Polyline" with list of "Position" and set color / thickness
		Polyline polyline = new Polyline(list);
		polyline.setColor(Color.RED);
		polyline.setLineWidth(3.0);
		
		//create a layer and add Polyline
		RenderableLayer layer = new RenderableLayer();
		layer.addRenderable(polyline);
		
		//add layer to WorldWind
		worldWindCanvas.getModel().getLayers().add(layer);
	}
	
}