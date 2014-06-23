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
		for (Layer x: layerList){
			if (exclusionList.contains(x.getName())){
				layerList.remove(x);
			}
		}
		a.setLayers(layerList);
		worldWindCanvas.setModel(a);
		return worldWindCanvas;
	}
}