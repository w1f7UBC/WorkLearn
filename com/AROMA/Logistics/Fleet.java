package com.AROMA.Logistics;

import java.util.ArrayList;

import com.AROMA.Economic.Contract;
import com.AROMA.Utils.PriorityQueue;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.input.Keyword;

public class Fleet extends LogisticsEntity {
	private static final ArrayList<Fleet> allInstances;

	@Keyword(description = "The list of movingentities in this fleet.", 
			example = "SilverdaleTruckFleet FleetList { Truck20-1 Truck20-2 } or LoaderFleet FleetList { LoaderTruck-1 }")
	private final EntityListInput<? extends MovingEntity> fleetList;

	@Keyword(description = "The prototype of the movingentities in this fleet.", 
			example = "SilverdaleTruckFleet FleetProtoType { Truck20-1 }")
	private final EntityInput<? extends MovingEntity> fleetProtoType;
	
	@Keyword(description = "Whether this is a dynamic fleet. i.e. upon introducing new contracts, transportation manager"
			+ "tries to optimize and change fleet size in dynamic fleets. other wise fleet size remains unchanged.", 
			example = "SilverdaleTruckFleet Dynamic { FALSE }")
	private final BooleanInput dynamic;
	
	@Keyword(description = "Dispatching interval between trucks on the exit block of the facility that owns this fleet.",
			example = "SilverdaleParkBlock DispatchInterval { 1 h }")
	private final ValueInput dispatchIntervalInput;
	
	//TODO generalize for when fleet can serve multiple destinations in a route. Right now only one route is served with each fleet (i.e. buyer/supplier back and forth)
	private ArrayList<Facility> destinationsList; // List holding sequence of destinations for the fleet.  
	
	//tentative list of loading and unloading bays when contracts are scheduled
	private ArrayList<LoadingBay> loadingBayList;
	private ArrayList<LoadingBay> unloadingBayList;

	private PriorityQueue<Contract> scheduledContracts;
	private int optimumSize;							// optimum fleet size required as planed by transportation manager's scheduleFleetSize method
	private ArrayList<MovingEntity> currentFleetList;	// list of current entities in the fleet. in dynamic fleets, this list would be different than that of input fleetList
	
	private double dispatchInterval, lastDispatchTime, earliestNextDispatchTime;
	// These are estimated times for moving entities
	private double estimatedCycleAtBuyer, estimatedCycleAtSupplier, estimatedBuyerToSupplier, estimatedSupplierToBuyer, estimatedCycleTime;				
	// These are empirical average cycle time observed from moving entities over time 
	private double avgCycleTime;		
	
	static {
		allInstances = new ArrayList<Fleet>();
	}
	
	{
		fleetList = new EntityListInput<>(MovingEntity.class, "FleetList", "Key Inputs", null);
		this.addInput(fleetList);
		
		fleetProtoType = new EntityInput<MovingEntity>(MovingEntity.class, "FleetProtoType", "Key Inputs", null);
		this.addInput(fleetProtoType);
		
		dynamic = new BooleanInput("Dynamic", "Key Inputs", false);
		this.addInput(dynamic);
	
		dispatchIntervalInput = new ValueInput("DispatchInterval", "Key Inputs", 0.0d);
		this.addInput(dispatchIntervalInput);
		dispatchIntervalInput.setUnitType(TimeUnit.class);

	}
	
	public Fleet() {
		synchronized (allInstances) {
			allInstances.add(this);
		}
		currentFleetList = new ArrayList<>(5);
		scheduledContracts = new PriorityQueue<>(new DescendingPriotityComparator<Contract>(Contract.class,"getContractPriority",this.getFacility()));
		loadingBayList = new ArrayList<>(1);
		unloadingBayList = new ArrayList<>(1);
	}

	@Override
	public void validate() {
		super.validate();
		
		if(fleetList == null)
			throw new InputErrorException("FleetList should be set for %s", this.getName());
		else
			currentFleetList.addAll(fleetList.getValue());
		
		if(fleetProtoType.getValue() == null || fleetProtoType.getValue().equals(currentFleetList.get(0).getProtoTypeEntity()))
			throw new InputErrorException("Fleet's proto type was %s while %s's proto type was %s. The two proto types should be equal!", fleetProtoType.getValue().getName(),currentFleetList.get(0).getName(),currentFleetList.get(0).getProtoTypeEntity().getName());
		
		for (MovingEntity each: fleetList.getValue())
			each.setFleet(this);
		
		dispatchInterval = dispatchIntervalInput.getValue();
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();
		
	}
	
	public static ArrayList<? extends Fleet> getAll() {
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
	public void setEstimatedCycleTime(double estimatedCycleTime){
		this.estimatedCycleTime = estimatedCycleTime;
	}

	/**
	 * TODO since this assumes only one active contract at a time, when contract voids. 
	 */
	public void removeContract(Contract contract){
		/*scheduledContracts.remove(contract);
		loadingBayList.clear();
		unloadingBayList.clear();
		destinationsList.clear();
		destinationsList.add(this.getFacility());
		avgCycleTime = 0;
		estimatedBuyerToSupplier = 0.0d;
		estimatedCycleAtBuyer = 0.0d;
		estimatedCycleAtSupplier = 0.0d;
		estimatedCycleTime = 0.0d;
		estimatedSupplierToBuyer = 0.0d;				
	
		for(MovingEntity each: currentFleetList)
			each.callBack(true);
		
		if(scheduledContracts.isEmpty())
			this.getFacility().getTransportationManager().getUnassignedFleetList().add((MovingEntity)this.getProtoTypeEntity(),this);
	*/
		}
	
	public int getFleetSizeAdjustment(){
		if (this.isDynamic())
			return optimumSize-currentFleetList.size();
		else
			return 0;
	}
	
	public boolean isDynamic(){
		return dynamic.getValue();
	}
	
	/**
	 * @return fleet prototype or if not defined, return the first entity in fleet's list
	 */
	public MovingEntity getFleetProtoType(){
		return fleetProtoType.getValue()!= null ? fleetProtoType.getValue() : fleetList.getValue().get(0);
	}
	
	/**
	 * 
	 * @param origin current origin of the moving entity
	 * @return the next destination in fleet's destination's list. will cycle through
	 * fleet's destination list if reached end of the list.
	 */
	public Facility getNextDestination(Facility origin){
		if(destinationsList.isEmpty())
			return null;
		int index = destinationsList.indexOf(origin);
		if(index == destinationsList.size()-1)
			return destinationsList.get(0);
		else
			return destinationsList.get(++index);
	}
	
	public ArrayList<? extends MovingEntity> getFleetList() {
		return fleetList.getValue();
	}

	public double getEstimatedCycleTime(){
		return estimatedCycleTime;
	}
	
	public double getAvgCycleTime(){
		return avgCycleTime;
	}
	
	public int getFleetSize(){
		return currentFleetList.size();
	}
	
	public ArrayList<MovingEntity> getCurrentFleetList(){
		return currentFleetList;
	}

	public ArrayList<Facility> getDestinationsList() {
		return destinationsList;
	}
	
	public ArrayList<LoadingBay> getTentativeLoadingBaysList(){
		return loadingBayList;
	}
	
	public ArrayList<LoadingBay> getTentativeUnLoadingBaysList(){
		return unloadingBayList;
	}	
	
	public double getDispatchInterval(){
		return dispatchInterval;
	}
	
	public double getLastDispatchTime(){
		return lastDispatchTime;
	}

	public double getEarliestNextDispatchTime() {
		return earliestNextDispatchTime;
	}

	public double getEstimatedBuyerToSupplier() {
		return estimatedBuyerToSupplier;
	}

	public double getEstimatedCycleAtBuyer() {
		return estimatedCycleAtBuyer;
	}

	public double getEstimatedCycleAtSupplier() {
		return estimatedCycleAtSupplier;
	}

	public double getEstimatedSupplierToBuyer() {
		return estimatedSupplierToBuyer;
	}

	/**
	 * @return List of bulk materials that this moving entity is scheduled to load (according to priority)
	 */
	public PriorityQueue<Contract> getScheduledContracts(){
		return scheduledContracts;
	}	
	
	public void setOptimumSize(int size){
		this.optimumSize = size;
	}
	
	public void setAvgCycleTime(double cycleTime){
		this.avgCycleTime = cycleTime;
	}
	
	public void setDispatchInterval(double dispatchInterval){
		this.dispatchInterval = dispatchInterval;
	}
	
	public void setLastDispatchTime(double lastDispatchTime){
		this.lastDispatchTime = lastDispatchTime;
	}

	public void setEstimatedCycleAtBuyer(double estimatedCycleAtBuyer) {
		this.estimatedCycleAtBuyer = estimatedCycleAtBuyer;
	}

	public void setEstimatedCycleAtSupplier(double estimatedCycleAtSupplier) {
		this.estimatedCycleAtSupplier = estimatedCycleAtSupplier;
	}

	public void setEstimatedBuyerToSupplier(double estimatedBuyerToSupplier) {
		this.estimatedBuyerToSupplier = estimatedBuyerToSupplier;
	}

	public void setEstimatedSupplierToBuyer(double estimatedSupplierToBuyer) {
		this.estimatedSupplierToBuyer = estimatedSupplierToBuyer;
	}

	public void setEarliestNextDispatchTime(double lastScheduledDispatchTime) {
		this.earliestNextDispatchTime = lastScheduledDispatchTime;
	}
	
}
