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
import com.jaamsim.input.ValueListInput;
import com.jaamsim.input.WorldWindCameraInput;
import com.jaamsim.input.WorldWindInitialCamera;
import com.jaamsim.input.WorldWindQueryInput;
import com.jaamsim.math.Vec3d;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.IntegerListInput;
import com.sandwell.JavaSimulation.IntegerVector;
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
	@Keyword(description = "Sets initial location of camera for WorldWindFrame")
	private ValueListInput initialCamera;
	
	{
		camera = new WorldWindCameraInput("CameraLocation", "WorldWind");
		this.addInput(camera);
		DoubleVector vector = new DoubleVector();
		vector.add(0.0);
		initialCamera = new ValueListInput("InitialCamera", "WorldWind", new DoubleVector());
		this.addInput(initialCamera);
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

	private Map<Double, DoubleVector> cameraInputMap;
	private Map<Double, Query> queryInputMap;


	public WorldView(){
		WorldWindFrame.initialize();
	}

    public void goTo(double lat, double lon, double zoom, double heading, double pitch){
    	BasicOrbitView orbitView;
    	orbitView = (BasicOrbitView) WorldWindFrame.AppFrame.getWwd().getView();
    	Angle newHeading = Angle.fromDegrees(heading);
    	Angle newPitch = Angle.fromDegrees(pitch);    	
		if(heading == Double.NEGATIVE_INFINITY){
			newHeading = orbitView.getHeading();
		}
		if(pitch == Double.NEGATIVE_INFINITY){
			newPitch = orbitView.getPitch();
		}
    	orbitView.addPanToAnimator(Position.fromDegrees(lat, lon), newHeading, newPitch, zoom);
    }

    public void initGoTo(double lat, double lon, double zoom, double heading, double pitch){
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
    	orbitView.setCenterPosition(Position.fromDegrees(lat, lon));
    	orbitView.setZoom(zoom);
    	orbitView.setHeading(newHeading);
    	orbitView.setPitch(newPitch);
    }

    @Override
    public void startUp() {
    	
    	
		Set<Double> cameraSet = cameraInputMap.keySet();
		Set<Double> querySet = queryInputMap.keySet();
		Iterator<Double> cameraIterator = cameraSet.iterator();
		Iterator<Double> queryIterator = querySet.iterator();
		while (cameraIterator.hasNext()){
			double time = cameraIterator.next();
			DoubleVector location = cameraInputMap.get(time);
			//System.out.println(time + " " + location);
			this.scheduleProcess(time, 3, new ReflectionTarget(this, "goTo", location.get(0), location.get(1), location.get(2), location.get(3), location.get(4)));
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
				cameraInputMap=new HashMap<Double, DoubleVector>();
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
		if(in==initialCamera){
			DoubleVector initLocation = initialCamera.getValue();
	    	initGoTo(initLocation.get(0), initLocation.get(1), initLocation.get(2), initLocation.get(3), initLocation.get(4));
		}
	}
	
	public void scheduleQuery(InventoryQuery q){
		q.executeArea(true, new DefinedShapeAttributes());
	}

}
