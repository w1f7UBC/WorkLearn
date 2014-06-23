package map;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.List;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.EventListener;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.PositionListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.view.orbit.FlatOrbitView;

import javax.swing.JFrame;

import com.jogamp.newt.event.MouseAdapter;

public class Map {

	public static void main(String[] args) {

		Configuration.setValue(AVKey.GLOBE_CLASS_NAME, EarthFlat.class.getName());
        Configuration.setValue(AVKey.VIEW_CLASS_NAME, FlatOrbitView.class.getName());

		final StatusBar statusBar= new StatusBar();


		//create a WorldWind main object
		WorldWindowGLCanvas worldWindCanvas = new WorldWindowGLCanvas();
		BasicModel a=new BasicModel();
		System.out.println(a.getLayers());

		Set<String> exclusionList=new HashSet<String>();
		exclusionList.add("Stars");
		exclusionList.add("Atmosphere");
		//exclusionList.add("NASA Blue Marble Image");
		//exclusionList.add("Blue Marble May 2004");
		//exclusionList.add("i-cubed Landsat");
		exclusionList.add("USDA NAIP");
		exclusionList.add("USDA NAIP USGS");
		exclusionList.add("MS Virtual Earth Aerial");
		exclusionList.add("Bing Imagery");
		exclusionList.add("USGS Topographic Maps 1:250K");
		exclusionList.add("USGS Topographic Maps 1:100K");
		exclusionList.add("USGS Topographic Maps 1:24K");
		exclusionList.add("USGS Urban Area Ortho");
		exclusionList.add("Political Boundaries");
		exclusionList.add("Open Street Map");
		exclusionList.add("Earth at Night");
		//exclusionList.add("Place Names");
		exclusionList.add("World Map");
		//exclusionList.add("Scale bar");
		exclusionList.add("Compass");

		LayerList layerList=a.getLayers();
/*
		for (Layer x: layerList){
			if (exclusionList.contains(x.getName())){
				layerList.remove(x);
			}
		}

		*/
		a.setLayers(layerList);
		System.out.println(a.getLayers());

		worldWindCanvas.setModel(a);

		//build Java swing interface
		JFrame frame = new JFrame("World Wind");
        frame.add(statusBar, BorderLayout.PAGE_END);
        statusBar.setEventSource(worldWindCanvas);

        MapListener xyz=new MapListener();
        worldWindCanvas.addPositionListener(xyz);


		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(worldWindCanvas);
		frame.setSize(800,600);
		frame.setVisible(true);
	}




}