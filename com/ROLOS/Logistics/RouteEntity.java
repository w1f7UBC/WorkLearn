package com.ROLOS.Logistics;

import java.util.ArrayList;

import com.jaamsim.input.InputAgent;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.HasScreenPoints;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.SpeedUnit;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityListListInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Vec3dListInput;

public class RouteEntity extends DiscreteHandlingLinkedEntity implements HasScreenPoints {

	private static final ArrayList<RouteEntity> allInstances;

   @Keyword(description = "A list of points in { x, y, z } coordinates defining the line segments that" +
           "make up the arrow.  When two coordinates are given it is assumed that z = 0." ,
            example = "Conveyor1  Points { { 6.7 2.2 m } { 4.9 2.2 m } { 4.9 3.4 m } }")
	private final Vec3dListInput pointsInput;

	@Keyword(description = "The width of the Arrow line segments in pixels.",
	         example = "Road1 Width { 1 }")
	private final ValueInput widthInput;

	@Keyword(description = "The colour of the arrow, defined using a colour keyword or RGB values.",
	         example = "Road1 Color { black }")
	private final ColourInput colorInput;
	
	@Keyword(description = "Maximum speed for this route segment.", 
			example = "Road1 SpeedLimit { 100 km/h } ")
	private final ValueInput speedLimit;
	
	@Keyword(description = "the group of entities that have a common speed. this should be set if speed by entity type is set, then each group of entities "
			+ "for which a speed is set in the capacityByEntityType should be put in braces. for example if Road1 SpeedGroups { { AllTrucks } { AllLoaders } } "
			+ "and Road1 SpeedByEntityType { 10 15 km/h }, then trucks travel with 10 km/h and loaderTrucks travel by 15 km/h", 
			example = "Road1 SpeedGroups { AllShip } or Road1 SpeedGroups { { AllTrucks } { AllLoaders } }")
	private final EntityListListInput<LogisticsEntity> speedGroups;
	
	@Keyword(description = "Maximum speed for each entity type that travels on this route segment. the entered values correspont" +
			"to the order of handledEntityTypes list. if in the Handle keyword groups are used (to use in capacityByEntityTypeList)"
			+ "then the speeds are assumed for all the group", 
			example = "Road1 SpeedLimitByEntityType { 100 70 80 km/h }")
	private final ValueListInput speedLimitByEntityType;
	
	@Keyword(description = "The length of the route entity.", 
			example = "Road1 Length { 100 km }")
	private final ValueInput length;					// Actual length
		
	// Graphics Fields
	private double totalLength;  						// Graphical length 
	private final ArrayList<Double> lengthList;  		// Graphical length of each segment 
	private final ArrayList<Double> cumLengthListStoF;  	// Total graphical length to the end of each segment from start to finish
	private final ArrayList<Double> cumLengthListFtoS;  	// Total graphical length to the end of each segment from finish to start
	
	private HasScreenPoints.PointsInfo[] cachedPointInfo;

	static {
		allInstances = new ArrayList<RouteEntity>(50);
	}
	
	{
		ArrayList<Vec3d> defPoints =  new ArrayList<Vec3d>();
		defPoints.add(new Vec3d(0.0d, 0.0d, 0.0d));
		defPoints.add(new Vec3d(1.0d, 0.0d, 0.0d));
		pointsInput = new Vec3dListInput("Points", "Basic Graphics", defPoints);
		pointsInput.setValidCountRange( 2, Integer.MAX_VALUE );
		pointsInput.setUnitType(DistanceUnit.class);
		this.addInput(pointsInput);

		widthInput = new ValueInput("Width", "Basic Graphics", 1.0d);
		widthInput.setValidRange(1.0d, Double.POSITIVE_INFINITY);
		this.addInput(widthInput);

		colorInput = new ColourInput("Colour", "Basic Graphics", ColourInput.BLACK);
		this.addInput(colorInput);
		
		speedLimit = new ValueInput("SpeedLimit", "Key Inputs", Double.POSITIVE_INFINITY);
		speedLimit.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(speedLimit);
		speedLimit.setUnitType(SpeedUnit.class);
		
		speedGroups = new EntityListListInput<>(LogisticsEntity.class, "SpeedGroups", "Key Inputs", null);
		this.addInput(speedGroups);
		
		speedLimitByEntityType = new ValueListInput("SpeedLimitByEntityType", "Key Inputs", null);
		speedLimitByEntityType.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		speedLimitByEntityType.setUnitType(SpeedUnit.class);
		this.addInput(speedLimitByEntityType);
		
		length = new ValueInput("Length", "Key Inputs", 0.0d);
		length.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(length);
		length.setUnitType(DistanceUnit.class);		

	}
	public RouteEntity() {
		// TODO Auto-generated constructor stub
		super();
		
		synchronized (allInstances) {
			allInstances.add(this);
		}
		
		lengthList = new ArrayList<Double>();
		cumLengthListStoF = new ArrayList<Double>();
		cumLengthListFtoS = new ArrayList<Double>();
	}
	
	@Override
	public void validate() {
		super.validate();
				
		// check if any of the speedByEntityTypes entered is higher than the speed limit 
		if (speedLimitByEntityType.getValue() !=null){
			if (speedGroups.getValue() == null || speedGroups.getValue().size() != speedLimitByEntityType.getValue().size())
				throw new InputErrorException("SpeedGroups and SpeedLimitByEntityType must both be set together and of the same size!");
			if (speedLimit.getValue() != null){
				for (int i=0; i< speedGroups.getValue().size();i++){
					if (speedLimitByEntityType.getValue().get(i)>speedLimit.getValue())
						throw new InputErrorException("Speed Limit for the group starting with %s exceeds maximum speed allowable on %s", speedGroups.getValue().get(i).get(0).getName(), this.getName());
				}
			}	
		}
	}
	
	@Override
	public void earlyInit() {
		// TODO Auto-generated method stub
		super.earlyInit();
		
		 // Initialize the segment length data
		lengthList.clear();
		cumLengthListStoF.clear();
		cumLengthListFtoS.clear();
		totalLength = 0.0;
		for( int i = 1; i < pointsInput.getValue().size(); i++ ) {
			// Get length between points
			Vec3d vec = new Vec3d();
			vec.sub3( pointsInput.getValue().get(i), pointsInput.getValue().get(i-1));
			double length = vec.mag3();

			lengthList.add(length);
			totalLength += length;
			cumLengthListStoF.add(totalLength);
		}
		totalLength = 0;
		for(int i = lengthList.size()-1; i>=0; i--){
			totalLength += lengthList.get(i);
			cumLengthListFtoS.add(totalLength);
		}
	}
		
	public static ArrayList<? extends RouteEntity> getAll() {
		synchronized (allInstances) {
			return allInstances;
		}
	}

	@Override
	public void kill() {
		// TODO Auto-generated method stub
		super.kill();
		synchronized (allInstances) {
			allInstances.remove(this);
		}
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////
	// GETTERS AND SETTERS
	// ////////////////////////////////////////////////////////////////////////////////////

	public <T extends LogisticsEntity> double getSpeedLimit(T entity) {
		LogisticsEntity ent;
		if (entity.testFlag(Entity.FLAG_GENERATED)) {
			ent = entity.getProtoTypeEntity();
		} else {
			ent = entity;
		}
		if (speedGroups.getValue() != null && this.getSpeedGroupIndex(ent)>=0 ) {
			// else get and return respective speed
			return speedLimitByEntityType.getValue().get(this.getSpeedGroupIndex(ent));
		}
			// return speed limit if speedByEntityType is not set
			return speedLimit.getValue();
	}
	
	public double getLength() {
		return length.getValue();
	}
	
	/**
	 * @return the index of list of entities in the braces of Handles keyword that contains entity. -1 if list does not contain ent;
	 * this is privately called by getspeedlimit and should be passed the parent entity
	 */
	private <T extends LogisticsEntity> int getSpeedGroupIndex(T entity){
		
		for (ArrayList<LogisticsEntity> each: speedGroups.getValue()){
			if (each.contains(entity)){
				return speedGroups.getValue().indexOf(each);
			}
		}
		return -1;	
	}
	
	// /////////////////////////////////////////////////////////////////////////////
	// CALCULATION
	// /////////////////////////////////////////////////////////////////////////////
		
	public int calcSegment(double graphicalDist,boolean travellingStoF){
		if(travellingStoF){
			for( int i = 0; i < cumLengthListStoF.size(); i++) {
				if( graphicalDist <= cumLengthListStoF.get(i)) {
					return i;
				}
			}
		}else{
			for( int i = 0; i < cumLengthListFtoS.size(); i++) {
				if( graphicalDist <= cumLengthListFtoS.get(i)) {
					return i;
				}
			}
		}
		return 0;
	}
	
	// TODO refactor to work in 3d graphics
	public Vec3d caclOrientation(double actualDist,boolean travellingStoF){
		double graphicalDist = (actualDist / length.getValue()) * totalLength;
		Vec3d orient = new Vec3d();
		Vec3d vec = new Vec3d();
		int seg = calcSegment(graphicalDist, travellingStoF);
		
		if(travellingStoF){
			vec.sub3(this.pointsInput.getValue().get(seg),this.pointsInput.getValue().get(seg+1)); 
		}else {
			int size = pointsInput.getValue().size() -1;
			vec.sub3(this.pointsInput.getValue().get(size-seg), this.pointsInput.getValue().get(size-seg-1));
		}
		
		orient.z = Math.atan2(vec.y, vec.x);
		
		return orient;
		
	}
	
	/**
	 * Return the position coordinates for a given distance along the Route.
	 * @param actualDist = distance along the route.
	 * @return position coordinates
	*/
	public Vec3d calcPositionForDistance(double actualDist,boolean travellingStoF) {

		// Find the present segment
		double graphicalDist = (actualDist / length.getValue()) * totalLength;
		int seg = calcSegment(graphicalDist,travellingStoF);

		// Interpolate between the start and end of the segment
		double frac = 0.0;
		if (travellingStoF) {
			if (seg == 0) {
				frac = graphicalDist / lengthList.get(0);
			} else {
				frac = (graphicalDist - cumLengthListStoF.get(seg - 1))
						/ lengthList.get(seg);
			}
			if( frac < 0.0 )  frac = 0.0;
			else if( frac > 1.0 )  frac = 1.0;

			Vec3d vec = new Vec3d();
			vec.interpolate3(pointsInput.getValue().get(seg),pointsInput.getValue().get(seg+1), frac);
/*
			vec.sub3( pointsInput.getValue().get(seg+1), pointsInput.getValue().get(seg));

			vec.scale3(frac);
			vec.add3( pointsInput.getValue().get(seg));
			*/
			return vec;
		}else{
			int lastSeg = lengthList.size()-1;
			if (seg == 0) {
				frac = graphicalDist / lengthList.get(lastSeg);
			} else {
				frac = (graphicalDist - cumLengthListFtoS.get(seg - 1))
						/ lengthList.get(lastSeg - seg);
			}
		
			if( frac < 0.0 )  frac = 0.0;
			else if( frac > 1.0 )  frac = 1.0;

			Vec3d vec = new Vec3d();
			vec.interpolate3(pointsInput.getValue().get(lastSeg-seg+1), pointsInput.getValue().get(lastSeg-seg),frac);
/*
			vec.scale3(frac);
			vec.add3(pointsInput.getValue().get(seg));
			*/
			return vec;
		}
	}
	
	// /////////////////////////////////////////////////////////////////////////////
	// Discrete Entities METHODS
	// /////////////////////////////////////////////////////////////////////////////
	
	// TODO refactor when states and traveling process are figured out

	@Override
	public void startProcessingQueuedEntities() {
		// TODO Auto-generated method stub
		super.startProcessingQueuedEntities();
		int i = 0;
		for (; i<this.getQueuedEntitiesList().size();) {
			MovingEntity entityUnderProcess =  (MovingEntity) this.getQueuedEntitiesList().get(i);
						
			if (this.isReadyToHandle(entityUnderProcess)) {
				this.removeFromQueuedEntityList(entityUnderProcess);
				DiscreteHandlingLinkedEntity tempHandledByEntity = (DiscreteHandlingLinkedEntity) entityUnderProcess.getCurrentlHandlersList().get(0);
				// TODO refactor when entities that have length (e.g. trains) is figured out
		//		tempHandledByEntity.removeFromCurrentlyHandlingEntityList(entityUnderProcess, 1);
		//		if(tempHandledByEntity instanceof LoadingBay){
					
		//		}
				entityUnderProcess.setTravellingDirectionOnCurrentRouteSegment(this);
				this.setPresentState("Working");

				this.addToCurrentlyHandlingEntityList(entityUnderProcess, 1);

				entityUnderProcess.setDistanceTraveledOnCurrentRouteSegment(0);
				if(this.getEnterDelay() != null){
					for (Delay eachDelay: this.getEnterDelay()) {
						entityUnderProcess.setPresentState("Enter Delay - "
								+ eachDelay.getName());
						this.simWait(eachDelay.getNextDelayDuration(
								entityUnderProcess));
					}
				}
				double speed = Math.min(entityUnderProcess.getInternalSpeed(),
						this.getSpeedLimit(entityUnderProcess));
				this.startProcess("travel", speed);
			} else 
				i++;
		}
		this.setTriggered(false);
	}
	
	/**
	 * This super method should be called when destination is scheduled for entity.
	 */
	@Override
	public <T extends MovingEntity> void finishProcessingEntity(T entity) {
		super.finishProcessingEntity(entity);

	}
	
	// ////////////////////////////////////////////////////////////////////////////////////
	// GRAPHICS 
	// ////////////////////////////////////////////////////////////////////////////////////

	@Override
	public HasScreenPoints.PointsInfo[] getScreenPoints() {
		if (cachedPointInfo == null) {
			cachedPointInfo = new HasScreenPoints.PointsInfo[1];
			HasScreenPoints.PointsInfo pi = new HasScreenPoints.PointsInfo();
			cachedPointInfo[0] = pi;

			pi.points = pointsInput.getValue();
			pi.color = colorInput.getValue();
			pi.width = widthInput.getValue().intValue();
			if (pi.width < 1) pi.width = 1;
		}
		return cachedPointInfo;
	}
	
	public Vec3d getFirstPointInput(){
		return new Vec3d(pointsInput.getValue().get(0));
	}
	
	public Vec3d getLastPointInput(){
		return new Vec3d(pointsInput.getValue().get(pointsInput.getValue().size()-1));
	}

	@Override
	public boolean selectable() {
		// TODO Auto-generated method stub
		return this.isMovable();
	}
	
	/**
	 *  Inform simulation and editBox of new positions.
	 */
	@Override
	public void dragged(Vec3d dist) {
		ArrayList<Vec3d> vec = new ArrayList<Vec3d>(pointsInput.getValue().size());
		for (Vec3d v : pointsInput.getValue()) {
			vec.add(new Vec3d(v.x + dist.x, v.y + dist.y, v.z + dist.z));
		}

		StringBuilder tmp = new StringBuilder();
		for (Vec3d v : vec) {
			tmp.append(String.format(" { %.3f %.3f %.3f m }", v.x, v.y, v.z));
		}
		InputAgent.processEntity_Keyword_Value(this, pointsInput, tmp.toString());

		super.dragged(dist);
	}

	@Override
	public void updateGraphics( double simTime ) {
		super.updateGraphics(simTime);
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////
	// REPORTING
	// ////////////////////////////////////////////////////////////////////////////////////
	@Override
	public boolean setPresentState(String state) {
		boolean reportPrinted = false;
		if (super.setPresentState(state)) {
			
			stateReportFile.newLine();
			stateReportFile.flush();
			reportPrinted = true;
		}
			
		return reportPrinted;
	}
	
	@Override
	public void printStatesHeader() {
		super.printStatesHeader();
		
		// print units
		this.printStatesUnitsHeader();	
		stateReportFile.newLine(2);
		stateReportFile.flush();	
	
	}
}
