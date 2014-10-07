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


import com.ROLOS.Input.WorldWindCameraInput;
import com.ROLOS.Input.WorldWindQueryInput;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.IntegerListInput;
import com.jaamsim.input.Keyword;

import com.jaamsim.math.Vec3d;

import com.sandwell.JavaSimulation.Entity;

import com.sandwell.JavaSimulation3D.GUIFrame;

public class WorldView extends Entity {

	@Keyword(description = "Sets location of camera for WorldWindFrame")
	private WorldWindCameraInput camera;
	@Keyword(description = "A Boolean indicating whether the WorldView should show a window")
	private BooleanInput showWindow;
	@Keyword(description = "Shows query in the WorldWindFrame")
	private WorldWindQueryInput query;
	@Keyword(description = "The size of the window in pixels (width, height).")
	private final IntegerListInput windowSize;
	@Keyword(description = "The position of the upper left corner of the window in pixels measured" +
            "from the top left corner of the screen.")
	private final IntegerListInput windowPosition;
	
	{
		camera = new WorldWindCameraInput("CameraLocation", "WorldWind");
		this.addInput(camera);
		showWindow = new BooleanInput("ShowWindow", "WorldWind", false);
		this.addInput(showWindow);
		query = new WorldWindQueryInput("QueryLocation", "WorldWind");
		this.addInput(query);
		
		IntegerVector defSize = new IntegerVector(2);
		defSize.add(GUIFrame.COL4_WIDTH);
		defSize.add(GUIFrame.VIEW_HEIGHT);
		windowSize = new IntegerListInput("WindowSize", "WorldWind", defSize);
		windowSize.setValidCount(2);
		windowSize.setValidRange(1, 8192);
		this.addInput(windowSize);
		
		IntegerVector defPos = new IntegerVector(2);
		defPos.add(GUIFrame.COL4_START);
		defPos.add(GUIFrame.TOP_START);
		windowPosition = new IntegerListInput("WindowPosition", "WorldWind", defPos);
		windowPosition.setValidCount(2);
		windowPosition.setValidRange(-8192, 8192);
		this.addInput(windowPosition);
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
		if(in==windowSize){
			WorldWindFrame.setWorldWindSize(windowSize.getValue().get(0), windowSize.getValue().get(1));
		
		}
		if(in==windowPosition){
			WorldWindFrame.setWorldWindLocation(windowPosition.getValue().get(0), windowPosition.getValue().get(1));
		}
	}
	
	public void scheduleQuery(InventoryQuery q){
		q.executeArea(true, new DefinedShapeAttributes());
	}

}
