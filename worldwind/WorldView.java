package worldwind;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import gov.nasa.worldwind.geom.Position;

import com.jaamsim.events.ReflectionTarget;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.WorldWindCameraInput;
import com.jaamsim.math.Vec3d;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.Entity;

public class WorldView extends Entity {

	@Keyword(description = "Sets location of camera for WorldWindFrame")
	private WorldWindCameraInput camera;
	@Keyword(description = "A Boolean indicating whether the WorldView should show a window")
	private BooleanInput showWindow;
	{
		camera = new WorldWindCameraInput("CameraLocation", "WorldWind");
		this.addInput(camera);
		showWindow = new BooleanInput("ShowWindow", "WorldWind", false);
		this.addInput(showWindow);
	}
	private Map<Double, Vec3d> cameraInputMap;

	public WorldView(){
		WorldWindFrame.initialize();
	}

    public void goTo(double lat, double lon, double zoom){
    	WorldWindFrame.AppFrame.getWwd().getView().goTo(Position.fromDegrees(lat, lon), zoom);
    }

    @Override
    public void startUp() {
    	Set<Double> set = cameraInputMap.keySet();
    	Iterator<Double> iterator = set.iterator();
    	while (iterator.hasNext()){
    		double time = iterator.next();
    		Vec3d location = cameraInputMap.get(time);
    		//System.out.println(time + " " + location);
    		this.scheduleProcess(time, 3, new ReflectionTarget(this, "goTo", location.x, location.y, location.z));
    	}
    }

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);
		if(in==camera){
			if (cameraInputMap==null){
				cameraInputMap=new HashMap<Double, Vec3d>();
			}
			cameraInputMap.put(camera.getTime(), camera.getValue());
		}
		if(in==showWindow){
			WorldWindFrame.setViewVisible(showWindow.getValue());
		}
	}

}
