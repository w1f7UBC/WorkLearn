package map;

import java.util.HashSet;
import java.util.Set;
import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.view.orbit.FlatOrbitView;

public class Map {
	public static WorldWindowGLCanvas map(){
		//create a WorldWind main object
		Configuration.setValue(AVKey.GLOBE_CLASS_NAME, EarthFlat.class.getName());
        Configuration.setValue(AVKey.VIEW_CLASS_NAME, FlatOrbitView.class.getName());
		WorldWindowGLCanvas worldWindCanvas = new WorldWindowGLCanvas();
		BasicModel a=new BasicModel();		
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
		worldWindCanvas.setModel(a);
		return worldWindCanvas;
	}
}