package worldwind;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import DataBase.InventoryQuery;
import DataBase.Query;


import com.jaamsim.events.ReflectionTarget;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.WorldWindCameraInput;
import com.jaamsim.input.WorldWindQueryInput;
import com.jaamsim.math.Vec3d;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.Entity;

public class WorldView extends Entity {

	@Keyword(description = "Sets location of camera for WorldWindFrame")
	private WorldWindCameraInput camera;
	@Keyword(description = "A Boolean indicating whether the WorldView should show a window")
	private BooleanInput showWindow;
	@Keyword(description = "Shows query in the WorldWindFrame")
	private WorldWindQueryInput query;
	{
		camera = new WorldWindCameraInput("CameraLocation", "WorldWind");
		this.addInput(camera);
		showWindow = new BooleanInput("ShowWindow", "WorldWind", false);
		this.addInput(showWindow);
		query = new WorldWindQueryInput("QueryLocation", "WorldWind");
		this.addInput(query);
	}

	private Map<Double, double[]> cameraInputMap;
	private Map<Double, Query> queryInputMap;


	public WorldView(){
		WorldWindFrame.initialize();
	}

    public void goTo(double lat, double lon, double zoom, double heading, double pitch){
    	//WorldWindFrame.AppFrame.getWwd().getView().goTo(Position.fromDegrees(lat, lon), zoom);

    	
    	BasicOrbitView orbitView;
    	orbitView = (BasicOrbitView) WorldWindFrame.AppFrame.getWwd().getView();
    	
    	Angle newHeading = Angle.fromDegrees(heading);
    	Angle newPitch = Angle.fromDegrees(pitch);
    	
		if(heading == -1.0){
			newHeading = orbitView.getHeading();
		}
		if(pitch == -1.0){
			newPitch = orbitView.getPitch();
		}
    	orbitView.addPanToAnimator(Position.fromDegrees(lat, lon), newHeading, newPitch, zoom);
    }

    @Override
    public void startUp() {
//<<<<<<< HEAD
//    	Set<Double> set = cameraInputMap.keySet();
//    	Iterator<Double> iterator = set.iterator();
//    	while (iterator.hasNext()){
//    		double time = iterator.next();
//    		double[] location = cameraInputMap.get(time);
//    		
//    		BasicOrbitView orbitView;
//        	orbitView = (BasicOrbitView) WorldWindFrame.AppFrame.getWwd().getView();
//       
//        	
//    		//System.out.println("lat " + location[0] +"long "+ location[1] + "zoom " + location[2] + "header " + newHeading + "pitch " + newPitch);
//    		this.scheduleProcess(time, 3, new ReflectionTarget(this, "goTo", location[0], location[1], location[2], location[3], location[4]));
//    		
//=======
//    	Set<Double> cameraSet = cameraInputMap.keySet();
//    	Set<Double> querySet = queryInputMap.keySet();
//    	Iterator<Double> cameraIterator = cameraSet.iterator();
//    	Iterator<Double> queryIterator = querySet.iterator();
//    	while (cameraIterator.hasNext()){
//    		double time = cameraIterator.next();
//    		Vec3d location = cameraInputMap.get(time);
//    		//System.out.println(time + " " + location);
//    		this.scheduleProcess(time, 3, new ReflectionTarget(this, "goTo", location.x, location.y, location.z));
//    	}
//    	while (queryIterator.hasNext()){
//    		double time = queryIterator.next();
//    		Query q = queryInputMap.get(time);
//    		this.scheduleProcess(time, 3, new ReflectionTarget(this, "scheduleQuery", q));
//>>>>>>> refs/remotes/origin/master

		Set<Double> cameraSet = cameraInputMap.keySet();
		Set<Double> querySet = queryInputMap.keySet();
		Iterator<Double> cameraIterator = cameraSet.iterator();
		Iterator<Double> queryIterator = querySet.iterator();
		while (cameraIterator.hasNext()){
			double time = cameraIterator.next();
			double[] location = cameraInputMap.get(time);
			//System.out.println(time + " " + location);
			this.scheduleProcess(time, 3, new ReflectionTarget(this, "goTo", location[0], location[1], location[2], location[3], location[4]));
		}
		while (queryIterator.hasNext()){
			double time = queryIterator.next();
			Query q = queryInputMap.get(time);
			this.scheduleProcess(time, 3, new ReflectionTarget(this, "scheduleQuery", q));
		}
    }

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);
		if(in==camera){
			if (cameraInputMap==null){
				cameraInputMap=new HashMap<Double, double[]>();
			}
			cameraInputMap.put(camera.getTime(), camera.getDoubles());
		}
		if(in==showWindow){
			WorldWindFrame.setViewVisible(showWindow.getValue());
		}
		if(in==query){
			if (queryInputMap==null){
				queryInputMap=new HashMap<Double, Query>();
			}
			queryInputMap.put(query.getTime(), query.getQuery());
		}
	}
	
	public void scheduleQuery(InventoryQuery query){
		query.execute();
	}

}
