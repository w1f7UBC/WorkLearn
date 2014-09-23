package worldwind;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import gov.nasa.worldwind.geom.Position;
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
	private Map<Double, Vec3d> cameraInputMap;
	private Map<Double, Query> queryInputMap;

	public WorldView(){
		WorldWindFrame.initialize();
	}

    public void goTo(double lat, double lon, double zoom){
    	WorldWindFrame.AppFrame.getWwd().getView().goTo(Position.fromDegrees(lat, lon), zoom);
    }

    @Override
    public void startUp() {
    	Set<Double> cameraSet = cameraInputMap.keySet();
    	Set<Double> querySet = queryInputMap.keySet();
    	Iterator<Double> cameraIterator = cameraSet.iterator();
    	Iterator<Double> queryIterator = querySet.iterator();
    	while (cameraIterator.hasNext()){
    		double time = cameraIterator.next();
    		Vec3d location = cameraInputMap.get(time);
    		//System.out.println(time + " " + location);
    		this.scheduleProcess(time, 3, new ReflectionTarget(this, "goTo", location.x, location.y, location.z));
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
				cameraInputMap=new HashMap<Double, Vec3d>();
			}
			cameraInputMap.put(camera.getTime(), camera.getValue());
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
	
	public void scheduleQuery(InventoryQuery q){
		q.executeArea(true, new DefinedShapeAttributes());
	}

}
