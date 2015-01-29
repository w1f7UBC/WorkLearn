package com.AROMA.Logistics;

import java.util.ArrayList;
import java.util.LinkedList;

import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.SpeedUnit;
import com.jaamsim.events.Process;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.EnumInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Vec3dInput;
import com.sandwell.JavaSimulation.Tester;
import com.AROMA.AROMAEntity;
import com.AROMA.DMAgents.RouteManager.Transport_Mode;
import com.AROMA.Utils.HandyUtils;
import com.AROMA.Utils.HashMapList;
import com.AROMA.Utils.MathUtilities;
import com.AROMA.Utils.TwoLinkedLists;


/** Super class for entities that move in the model such as transporters, moving loader/unloaders , etc.
 * Moving Entities move on a RouteEntity
 * TODO refactor loader/processor only moving entities. Right now only when loading area gets exhausted (LoadingArea.isExhausted) sends the 
 * moving processors and loaders away
 */
public class MovingEntity extends LogisticsEntity {
	private static final ArrayList<MovingEntity> allInstances;
	
	@Keyword(description = "The transportation mode that this moving entity is allowed to travel on.", 
			example = "Truck TransportMode { ROAD }")
	private final EnumInput<Transport_Mode> transportMode;
	
	@Keyword(description = "Upper limit speed that this entity can travel freely on the route (i.e. actual speed = min(CrusingSpeed, LoadedSpeed, UnLoadedSpeed, MaxSpeed(route's),...).", 
			example = "Truck1 CruisingSpeed { 80 km/h } ")
	private final ValueInput cruisingSpeed;
	
	@Keyword(description = "Upper limit speed that this entity can travel when it tows loaded bulkcargo .", 
			example = "Truck1 LoadedSpeed { 80 km/h } ")
	private final ValueInput loadedSpeed;
	
	@Keyword(description = "Upper limit speed that this entity can travel when it tows empty bulkcargo.", 
			example = "Truck1 UnloadedSpeed { 80 km/h } ")
	private final ValueInput unloadedSpeed;

	@Keyword(description = "Whether this is a moving equipment such as a moving loader or grinder. Default is false.", 
			example = "LoaderTruck MovingEquipment { True }")
	private final BooleanInput movingEquipment;
	
	@Keyword(description = "The list of entities that this moving entity tows or is allowed to tow. For example a truck tows trailer(bulk cargo) a" +
			"tug tows a barge and etc.", 
			example = "Truck Tows { WoodChipTrailer } or Train1 Tows { HopperCars }")
	private final EntityListInput<? extends BulkHandlingLinkedEntity> towableEntitiesList;
	
	@Keyword(description = "The list of facilities that this moving entity travels to. "
			+ "This is used for standalone equipment. For example a moving loader that services a"
			+ "sequence of cutblocks as defined in this list.", 
			example = "Loader1 FacilitiesList { Cutblock1 Cutblock2 Cutblock3 }")
	private final EntityListInput<Facility> facilitiesList;
	
	@Keyword(description = "Position of this moving entity in a loading area.",
			example = "Loader LoadingAreaPosition { 10 2 2 m } ")
	private final Vec3dInput loadingAreaPosition;

	private Fleet fleet;
	private double currentSpeed;
	private double distanceTraveledOnCurrentRouteSegment;
	private boolean travellingStoFOnCurrentRouteSegment;		//true if travelling from start to finish, false otherwise
	private boolean traveling;
	private boolean calledBack;									// when moving entities are in the middle of travel and their fleet is rescheduled, a call back is placed on them
	private double startTravelingTimeOnCurrentRoute;
	private double startCycleTime;
	private Route currentRoute;
	DiscreteHandlingLinkedEntity headRoute;
	DiscreteHandlingLinkedEntity tailRoute;
	private Facility originFacility;
	private Facility destinationFacility;
	private DiscreteHandlingLinkedEntity currentDestination;
	private LoadingBay lastLoadingBay;
	private LinkedList<? extends DiscreteHandlingLinkedEntity> plannedNextRouteSegments;
	private double startTravelingTimeOnCurrentRouteSegment;
	private HashMapList<String,LogisticsEntity> currentlyTowingList;
	
	private TwoLinkedLists<BulkMaterial> acceptingBulkMaterialList; // for moving equipment, cargo capacity and current cargo will be zero

	
	private Vec3d lastPosition;								// stores the last graphical position of this entity. for route entities, returns the last point that this entity was on for route direction calculation purposes. getPosition() won't work
	private Process travelingProcess;
	
	static {
		allInstances = new ArrayList<MovingEntity>();
	}
	
	{
		transportMode = new EnumInput<>(Transport_Mode.class, "TransportMode", "Key Inputs", Transport_Mode.ROAD);
		this.addInput(transportMode);
		
		movingEquipment = new BooleanInput("MovingEquipment", "Key Inputs", true);
		this.addInput(movingEquipment);
		
		cruisingSpeed = new ValueInput("CruisingSpeed", "Key Inputs",
				Double.POSITIVE_INFINITY);
		cruisingSpeed.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(cruisingSpeed);
		cruisingSpeed.setUnitType(SpeedUnit.class);
		
		loadedSpeed = new ValueInput("LoadedSpeed", "Key Inputs",
				Double.POSITIVE_INFINITY);
		loadedSpeed.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(loadedSpeed);
		loadedSpeed.setUnitType(SpeedUnit.class);
		
		unloadedSpeed = new ValueInput("UnloadedSpeed", "Key Inputs",
				Double.POSITIVE_INFINITY);
		unloadedSpeed.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(unloadedSpeed);
		unloadedSpeed.setUnitType(SpeedUnit.class);
		
		towableEntitiesList = new EntityListInput<>(BulkHandlingLinkedEntity.class, "Tows", "Key Inputs", null);
		this.addInput(towableEntitiesList);
		
		loadingAreaPosition = new Vec3dInput("LoadingAreaPosition", "Basic Graphics", new Vec3d(0, 0, 0));
		loadingAreaPosition.setUnitType(DistanceUnit.class);
		this.addInput(loadingAreaPosition);
		
		facilitiesList = new EntityListInput<>(Facility.class, "FacilitiesList", "Key Inputs", null);
		this.addInput(facilitiesList);
		
	}
	
	public MovingEntity() {
		super();
		
		synchronized (allInstances) {
			allInstances.add(this);
		}
		
		plannedNextRouteSegments = new LinkedList<>();
		currentlyTowingList = new HashMapList<>(1);
		acceptingBulkMaterialList = new TwoLinkedLists<>(4, new DescendingPriorityComparator<BulkMaterial>(AROMAEntity.class, "getInternalPriority"),0);
	}
	
	@Override
	public void validate() {
		super.validate();
		
		if(towableEntitiesList.getValue() != null)
			for(BulkHandlingLinkedEntity each: towableEntitiesList.getValue()){
				each.attachToTow(this);
		}
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();
		
	}
	
	public static ArrayList<? extends MovingEntity> getAll() {
		synchronized (allInstances) {
			return allInstances;
		}
	}

	@Override
	public void kill() {
		super.kill();
		synchronized (allInstances) {
			allInstances.remove(this);
		}
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////
	// CALCULATIIONS 
	// ////////////////////////////////////////////////////////////////////////////////////
		
	public double calcDistTraveled (double speed, double time) {
		return speed * time;
	}
	
	/**
	 * 
	 * @param route Route that this MovingEntity is traveling on with the specified speed and position
	 * @param speed
	 * @param distAlreadyTraveled is the distance traveled from the beginning of the route so far
	 * @return travel time to reach the end of the specified route
	 */

	public double calcRemainingTravelTime (RouteSegment route, double speed, double distAlreadyTraveled) {
		double remainingTravelTime = (route.getLength() - distAlreadyTraveled) / speed;
		if (remainingTravelTime== 0) {
			throw new ErrorException("%s's length is %f but %s has traveled %f on it!",route.getName(),route.getLength(),this.getName(),distAlreadyTraveled);
		}
		/*
		if (Tester.lessOrEqualCheckTolerance(remainingTravelTime, 0) ) {
			throw new ErrorException("%s's length is %f but %s has traveled %f on it!",route.getName(),route.getLength(),this.getName(),distAlreadyTraveled);
		}
		*/
		return remainingTravelTime;
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////
	// ADDER AND REMOVER METHODS 
	// ////////////////////////////////////////////////////////////////////////////////////

	
				
	// ////////////////////////////////////////////////////////////////////////////////////
	// TRAVELING 
	// ////////////////////////////////////////////////////////////////////////////////////
		
	@Override
	public void startUp() {
		super.startUp();
		
		LoadingBay tempLoadingBay;
		// If loading bay is not defined for the owning facility, means that transportation is
		// not explicitly model, hence return
		if(this.getFacility() == null || this.getFacility().getInsideFacilityLimits().get(LoadingBay.class).isEmpty())
			return;
		
		// puts moving entities at a loading bay or if not found at an unloading bay
		tempLoadingBay = this.getFacility().getOperationsManager().getBulkHandlingRoute(this,null,true).getLoadingBay();
		if(tempLoadingBay == null)
			tempLoadingBay = this.getFacility().getOperationsManager().getBulkHandlingRoute(this,null,false).getLoadingBay();
		tempLoadingBay.addToQueuedEntityList(this);
	}
	
	public void travel(double speed) {
		
		if (this.isTraveling()) {
			throw new ErrorException("%s is already traveling at time: %f! Attempt was made to restart it!", this.getName(),this.getCurrentTime());
		}
		
		travelingProcess = Process.current();
		traveling = true;
		this.setPresentState("Traveling");
		
		// plan next moves while travelling on this route
	/*	if (plannedNextRouteSegments.size() == 1 && currentDestination != null && !plannedNextRouteSegments.contains(scheduledLoadingBay))
			Process.start(new ReflectionTarget(TrafficController.trafficControlManager,"planNextMove",lastLoadingBay, scheduledLoadingBay, this));
		*/
		if (!plannedNextRouteSegments.isEmpty())
			plannedNextRouteSegments.remove(0);
		startTravelingTimeOnCurrentRouteSegment = this.getSimTime();
		this.setCurrentSpeed(speed);
		double travelTimeToEnd = this.calcRemainingTravelTime((RouteSegment) headRoute,speed, distanceTraveledOnCurrentRouteSegment);
		this.simWait(travelTimeToEnd);
		//TODO when interrupt is figured out set distanceTraveled on current route and change traveling
		// checking traveled distance for when interrupted travel is allowed
		distanceTraveledOnCurrentRouteSegment += this.calcDistTraveled(speed, this.getSimTime() - startTravelingTimeOnCurrentRouteSegment);
		if (!Tester.equalCheckTolerance(distanceTraveledOnCurrentRouteSegment, ((RouteSegment) headRoute).getLength())) {	
			traveling = false;
			while(!this.isReadyToResumeTraveling()){
				waitUntil(null, null);
			} //waitUntilEnded();
			this.resumeTravel();
		}
		
		traveling = false;
		headRoute.finishProcessingEntity(this);
		DiscreteHandlingLinkedEntity retriggeredRoute = headRoute;	
		if (!traveling && plannedNextRouteSegments != null && !plannedNextRouteSegments.isEmpty()){
			((DiscreteHandlingLinkedEntity) plannedNextRouteSegments.get(0)).addToQueuedEntityList(this);
		}
		
		if(!retriggeredRoute.isTriggered() && !retriggeredRoute.getQueuedEntitiesList().isEmpty())
			this.startProcess("startProcessingQueuedEntities");
			
		travelingProcess = null;
	}

	// TODO add content to resume travel if stopped in the middle of the route
	public void resumeTravel(){
		
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////
	// GETTERS AND SETTERS
	// ////////////////////////////////////////////////////////////////////////////////////
		
	public Process getTravellingProcess(){
		return travelingProcess;
	}
	
	/**
	 * @return cruising if not towing bulk cargo, minimum of cruising and unloaded if towing empty cargo, minimum of cruising and loaded if towing loaded cargo
	 */
	public double getInternalSpeed() {
		if(currentlyTowingList.get(BulkCargo.class.getSimpleName()).isEmpty())
			return cruisingSpeed.getValue();
		else{
			if (getCurrentlyHandlingBulkMaterialAmount() != 0.0d)
				return Tester.min(cruisingSpeed.getValue(),loadedSpeed.getValue());
			else 
				return Tester.min(cruisingSpeed.getValue(),unloadedSpeed.getValue());
		}
	}

	public Facility getOriginFacility() {
		return originFacility;
	}

	public Facility getDestinationFacility(){
		return destinationFacility;
	}
	
	public Transport_Mode getTransportMode(){
		return transportMode.getValue();
	}
	
	public DiscreteHandlingLinkedEntity getCurrentDestination(){
		return currentDestination;
	}
	
	public void callBack(boolean calledBack){
		this.calledBack = calledBack;
	}
	
	public void setFleet(Fleet currentFleet){
		this.fleet = currentFleet;
	}
	
	public <T extends DiscreteHandlingLinkedEntity> void setCurrentDestination(T currentDestination){
		this.currentDestination = currentDestination;
	}
	public void setOriginFacility(Facility currentOrigin) {
		this.originFacility = currentOrigin;
	}

	public void setDestinationFacility(Facility currentDestination) {
		this.destinationFacility = currentDestination;
	}
	
	public void setCurrentSpeed(double currentSpeed) {
		this.currentSpeed = currentSpeed;
	}

	public double getDistanceTraveledOnCurrentRouteSegment() {
		return distanceTraveledOnCurrentRouteSegment;
	}

	public Vec3d getLoadingAreaPosition() {
		return loadingAreaPosition.getValue();
	}

	public void setDistanceTraveledOnCurrentRouteSegment(
			double distanceTraveledOnCurrentRouteSegment) {
		this.distanceTraveledOnCurrentRouteSegment = distanceTraveledOnCurrentRouteSegment;
	}

	//TODO add content to check if the blocking situation for resuming travel is lifted
	public boolean isReadyToResumeTraveling(){
		return true;
	}
	
	public boolean isCalledBack(){
		return calledBack;
	}
	
	public boolean isMovingEquipment(){
		return movingEquipment.getValue();
	}
	
	public boolean isTraveling() {
		return traveling;
	}

	public double getStartTravelingTimeOnCurrentRoute() {
		return startTravelingTimeOnCurrentRoute;
	}

	public void setStartTravelingTimeOnCurrentRoute(
			double startTravelingTimeOnCurrentRoute) {
		this.startTravelingTimeOnCurrentRoute = startTravelingTimeOnCurrentRoute;
	}
	
	public void setStartTravelingTimeOnCurrentRouteSegment(
			double startTravelingTimeOnCurrentRouteSegment) {
		this.startTravelingTimeOnCurrentRouteSegment = startTravelingTimeOnCurrentRouteSegment;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends DiscreteHandlingLinkedEntity> T getHeadRoute() {
		return this.getCurrentlHandlersList().isEmpty() ? null : (T) this.getCurrentlHandlersList().get(0);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends DiscreteHandlingLinkedEntity> T getTailRoute() {
		int index = this.getCurrentlHandlersList().size() - 1;
		return (T)this.getCurrentlHandlersList().get(index);
	}
	

	public void setCurrentRoute(Route currentRoute) {
		this.currentRoute = currentRoute;
	}

	public void setTraveling(boolean traveling) {
		this.traveling = traveling;
	}
	
	public <T extends DiscreteHandlingLinkedEntity> void setHeadAndTailRoutes(){
		ArrayList<? extends LogisticsEntity> tempRouteList = new ArrayList<>(this.getCurrentlHandlersList());
		this.headRoute = (T) tempRouteList.get(0);
		this.tailRoute = (T) tempRouteList.get(tempRouteList.size()-1);
	}
	
	@Override
	public <T extends LinkedEntity> void addToCurrentHandlersList(T currentlyHandledBy,double amountToAdd) {
		super.addToCurrentHandlersList(currentlyHandledBy,amountToAdd);

		if(currentlyHandledBy instanceof LoadingBay && movingEquipment.getValue())
			
		this.setHeadAndTailRoutes();
	}
	
	@Override
	public <T extends LinkedEntity> void removeFromCurrentHandlersList(T currentlyHandledBy,double amountToRemove) {
		super.removeFromCurrentHandlersList(currentlyHandledBy,amountToRemove);
	}
	
	
	public LoadingBay getLastLoadingBay() {
		return lastLoadingBay;
	}

	/**
	 * After currentorigin is set, currentdestination should be set, otherwise the old desitnation will be associated with the current one!
	 * @param lastLoaingBay
	 */
	public void setLastLoadingBay(LoadingBay loadingBay) {
		this.lastLoadingBay = loadingBay;
	}

	public Fleet getFleet(){
		return fleet;
	}
	
	public ArrayList<Facility> getFacilitiesList(){
		return facilitiesList.getValue();
	}
	
	/**
	 * <br> <b>0-</b> Cargo capacity 
	 * <br> <b>1-</b> Current cargo
	 * <br> <b>2-</b> infeedable rate (meaning there is a loader/processor attached to at least one of the bulk cargos)
	 * <br> <b>3-</b> outfeedable rate (meaning there is an unloader/processor attached to at least one of the bulk cargos)
	 * @return
	 */
	public TwoLinkedLists<BulkMaterial> getAcceptingBulkMaterialList(){
		return acceptingBulkMaterialList;
	}
	
	/** 
	 * @return cost of transporting material per unit of material per time (e.g. 5 $/t/h )
	 */
	public double getTransportationCost(BulkMaterial bulkMaterial){
		return this.getOperatingCost()/this.getAcceptingBulkMaterialList().getValueFor(bulkMaterial, 0);
	}
	
	/** 
	 * all towed entities should be added to the currentlyTowingList through their class'es simple name
	 * @return the currently towed entities list.
	 */
	public HashMapList<String, LogisticsEntity> getCurrentlyTowingEntityList(){
		return currentlyTowingList;
	}

	public LinkedList<? extends DiscreteHandlingLinkedEntity> getPlannedNextRouteSegments() {
		return plannedNextRouteSegments;
	}

	public void setPlannedNextRouteSegments(LinkedList<? extends DiscreteHandlingLinkedEntity> plannedNextRouteSegments) {
		this.plannedNextRouteSegments = plannedNextRouteSegments;
	}

	public double getStartTravellingTimeOnCurrentRouteSegment() {
		return startTravelingTimeOnCurrentRouteSegment;
	}
	
	public boolean isTravellingStoFOnCurrentRouteSegment(){
		return travellingStoFOnCurrentRouteSegment;
	}
	
	/**
	 * true if travelling start to finish, false otherwise
	 */
	public void setTravellingDirectionOnCurrentRouteSegment(RouteSegment currentRouteEntity){
		Vec3d firstPoint = new Vec3d(currentRouteEntity.getFirstPointInput());
		Vec3d lastPoint = new Vec3d(currentRouteEntity.getLastPointInput());
		// set travelling direction for the moving entity
		if(MathUtilities.distance(lastPosition, firstPoint)<
				MathUtilities.distance(lastPosition, lastPoint)){
			travellingStoFOnCurrentRouteSegment = true;
			lastPosition = new Vec3d(lastPoint);
		}
		else{
			travellingStoFOnCurrentRouteSegment = false;
			lastPosition = new Vec3d(firstPoint);
		}
	}
		
	public ArrayList<BulkMaterial> getCurrentlyHandlingBulkMaterialList(){
		ArrayList<BulkMaterial> tempBulkMaterialList = new ArrayList<>();
		for (BulkMaterial each: acceptingBulkMaterialList.getEntityList())
			if(acceptingBulkMaterialList.getValueFor(each, 1) > 0.0d)
				tempBulkMaterialList.add(each);
		return tempBulkMaterialList;	
	}
	
	public double getCurrentlyHandlingBulkMaterialAmount(BulkMaterial bulkMaterial) {
		
		return acceptingBulkMaterialList.getValueFor(bulkMaterial, 1);
	}
	
	public double getCurrentlyHandlingBulkMaterialAmount() {
		double tempAmount = 0;
		for(double each: acceptingBulkMaterialList.getValueLists().get(1))
			tempAmount += each;
		return tempAmount;
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);
		if(in == towableEntitiesList){
			for(BulkHandlingLinkedEntity each : towableEntitiesList.getValue())
				each.setStationary(false);
		}
	}
	// ////////////////////////////////////////////////////////////////////////////////////
	// GRAPHICS
	// ////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);
		if (traveling) {
			Vec3d orientInput = new Vec3d(this.getOrientationInput());
			double actualDistTraveled = this.calcDistTraveled(currentSpeed,
							simTime- this.getStartTravellingTimeOnCurrentRouteSegment());
			Vec3d graphicaldist = ((RouteSegment) this.getHeadRoute()).calcPositionForDistance(actualDistTraveled,
							travellingStoFOnCurrentRouteSegment);
			orientInput.add3(((RouteSegment) this.getHeadRoute()).caclOrientation(actualDistTraveled,
					travellingStoFOnCurrentRouteSegment));
			this.setOrientation(orientInput);
			this.setPosition(graphicaldist);
		}		
		Vec3d orient = new Vec3d();
		for (String eachKey: currentlyTowingList.getKeys()) {
			for (LogisticsEntity each : currentlyTowingList.get(eachKey)) {
				//	if (each.getPresentState() == "Idle") {
				orient.add3(each.getOrientationInput(), this.getOrientation());
				orient.sub3(this.getOrientationInput());
				each.setOrientation(orient);
				((BulkHandlingLinkedEntity) each).updateContentGraphics();
				//	}
			}
		}
	}
	
    public Vec3d getLastPosition(){
		return lastPosition;
	}
	
	public void setLastPosition(Vec3d lastPosition){
		this.lastPosition = lastPosition;
	}
    
	// ////////////////////////////////////////////////////////////////////////////////////
	// REPORTING
	// ////////////////////////////////////////////////////////////////////////////////////

	@Override
	public boolean setPresentState(String state) {
		boolean reportPrinted = false;
		if (super.setPresentState(state)) {
			// add bottom line for states report
			stateReportFile.putStringTabs(HandyUtils.arraylistToString(this.getCurrentlHandlersList()), 1);
			stateReportFile.putStringTabs(HandyUtils.arraylistToString(currentlyTowingList.get(BulkCargo.class.getSimpleName())), 1);
			if(originFacility != null){
				if(destinationFacility != null)
					stateReportFile.putStringTabs(originFacility.getName()+destinationFacility.getName(),1);
				else
					stateReportFile.putStringTabs(originFacility.getName(),1);
			}
			else if(this.getHeadRoute() != null)
				stateReportFile.putStringTabs(this.getHeadRoute().getName(), 1);
			else
				stateReportFile.putTab();
			stateReportFile.putDoubleWithDecimalsTabs(this.getDistanceTraveledOnCurrentRouteSegment(),ReportAgent.getReportPrecision(), 1);
			
			stateReportFile.newLine();
			stateReportFile.flush();
			reportPrinted = true;
		}
			
		return reportPrinted;
	}
	
	@Override
	public void printStatesHeader() {
		super.printStatesHeader();
		stateReportFile.putStringTabs("Head Route", 1);
		stateReportFile.putStringTabs("Towing Bulk Cargo List", 1);
		stateReportFile.putStringTabs("Route", 1);
		stateReportFile.putStringTabs("DistanceTravelled", 1);
		stateReportFile.newLine();

		this.printStatesUnitsHeader();
		stateReportFile.newLine(2);
		stateReportFile.flush();	
	}
	
	@Override
	public void printStatesUnitsHeader() {
		super.printStatesUnitsHeader();
		stateReportFile.putTabs(6);
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////
	// OUTPUTS
	// ////////////////////////////////////////////////////////////////////////////////////

		
}
