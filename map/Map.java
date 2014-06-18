package map;

import java.awt.Color;
import java.awt.List;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Polyline;

import javax.swing.JFrame;

public class Map {

	public static void main(String[] args) {

		//create a WorldWind main object
		WorldWindowGLCanvas worldWindCanvas = new WorldWindowGLCanvas();
		BasicModel a=new BasicModel();
		System.out.println(a.getLayers());

		Set<String> abc=new HashSet<String>();
		abc.add("Stars");
		abc.add("Atmosphere");
		//abc.add("NASA Blue Marble Image");
		//abc.add("Blue Marble May 2004");
		//abc.add("i-cubed Landsat");
		abc.add("USDA NAIP");
		abc.add("USDA NAIP USGS");
		abc.add("MS Virtual Earth Aerial");
		abc.add("Bing Imagery");
		abc.add("USGS Topographic Maps 1:250K");
		abc.add("USGS Topographic Maps 1:100K");
		abc.add("USGS Topographic Maps 1:24K");
		abc.add("USGS Urban Area Ortho");
		abc.add("Political Boundaries");
		abc.add("Open Street Map");
		abc.add("Earth at Night");
		//abc.add("Place Names");
		abc.add("World Map");
		//abc.add("Scale bar");
		abc.add("Compass");

		LayerList layerList=a.getLayers();

		for (Layer x: layerList){
			if (abc.contains(x.getName())){
				layerList.remove(x);
			}
		}
		a.setLayers(layerList);
		System.out.println(a.getLayers());

		worldWindCanvas.setModel(a);

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