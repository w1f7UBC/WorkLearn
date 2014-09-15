package com.ROLOS.DMAgents;

import java.util.ArrayList;

import com.ROLOS.ROLOSEntity;
import com.ROLOS.DMAgents.RouteManager.Route_Type;
import com.ROLOS.DMAgents.RouteManager.Transport_Mode;
import com.ROLOS.Economic.Contract;
import com.ROLOS.Logistics.BulkHandlingRoute;
import com.ROLOS.Logistics.BulkMaterial;
import com.ROLOS.Logistics.Delay;
import com.ROLOS.Logistics.DiscreteHandlingLinkedEntity;
import com.ROLOS.Logistics.EntranceBlock;
import com.ROLOS.Logistics.ExitBlock;
import com.ROLOS.Logistics.Facility;
import com.ROLOS.Logistics.Fleet;
import com.ROLOS.Logistics.LoadingBay;
import com.ROLOS.Logistics.MovingEntity;
import com.ROLOS.Logistics.Route;
import com.ROLOS.Logistics.RouteSegment;
import com.ROLOS.Logistics.Stockpile;
import com.ROLOS.Utils.HashMapList;
import com.ROLOS.Utils.TwoLinkedLists;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Tester;


public class FacilityTransportationManager extends FacilityManager {
	
	@Keyword(description = "Moving Entity list that have cost and capacity defined for them and apear in the TransportableMaterialGroups list."
			+ "These transporters should correspond to fleets' prototypes. I.e. these cost structures apply to the fleet", 
			example = "Temiscaming/TransportationManager Transporters { Truck1 Truck2 Train1 Train2 }")
	private final EntityListInput<MovingEntity> transportersList;

	@Keyword(description = "Whether transshipment is allowed in calaculating routes. Default is false.", 
			example = "Temiscaming/TransportationManager TransshipmentAllowed { TRUE }")
	private final BooleanInput transshipmentAllowed;
	
	private TwoLinkedLists<MovingEntity> transportationCapacityList;

	private HashMapList<Transport_Mode,MovingEntity> transportersMap; 
	private HashMapList<MovingEntity,MovingEntity> unassignedMovingEntityList;
	private HashMapList<Contract,MovingEntity> inactiveContractsList;
		
	{
		
		transportersList = new EntityListInput<>(MovingEntity.class, "Transporters", "Key Inputs", new ArrayList<MovingEntity>(0));
		this.addInput(transportersList);
		
		transshipmentAllowed = new BooleanInput("TransshipmentAllowed", "Key Inputs", false);
		this.addInput(transshipmentAllowed);
	}
	
	public FacilityTransportationManager() {
		transportationCapacityList = new TwoLinkedLists<>(2, new DescendingPriotityComparator<MovingEntity>(ROLOSEntity.class, "getInternalPriority"));

		transportersMap = new HashMapList<>(1);
		unassignedMovingEntityList = new HashMapList<MovingEntity,MovingEntity>(1);
		inactiveContractsList = new HashMapList<Contract,MovingEntity>(1);
	}

	@Override
	public void validate() {
		super.validate();
			
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();

		// TODO assign transportation capacity when figured out (from an input or based on logistics availability)!
		if(transportersList.getValue() != null){
			for(int i = 0; i<transportersList.getValue().size(); i++){
				transportationCapacityList.set(transportersList.getValue().get(i), 0, Double.POSITIVE_INFINITY);
			}
		}
	}
	
	/**
	 * Schedules transportation in the list for the earliest transportation
	 * This method should be called back after contracts are removed for fleet. it looks at the unassignedFleetList to 
	 * schedule fleet.
	 * TODO this method assigns to the first unassigned fleet in the list. refactor for assiging
	 * multiple contract's (possibly with fixed or variable amounts for each contract) to a fleet or choosing a fleet. 
	 */
	public void scheduleTransportation(Contract contract,double amount){
		
		if(contract.isTransportationModeled()){
			
		} 
		// if transportation is not modeled for this contract, tele port material from one stock pile to the other
		else{
			// if transportation is not modeled, and if contract is active, remove material from supplier's 
			// stockpile(s) and add to buyer's stock pile(s)
			double tempAmount = amount;
			double assignedAmount;
			Stockpile supplierPile, buyerPile;
			while (Tester.greaterCheckTolerance(tempAmount, 0.0d) && contract.isActive()){
				supplierPile = contract.getSupplier().getOperationsManager().getStockpileForLoading(contract.getProduct());
				buyerPile = contract.getBuyer().getOperationsManager().getStockpileForUnLoading(contract.getProduct());
				
				if(supplierPile == null || buyerPile == null)
					return;
				
				assignedAmount = Tester.min(tempAmount, supplierPile.getCurrentlyHandlingAmount(),
									buyerPile.getRemainingCapacity(contract.getProduct()));
				// do transaction between piles and contract
				supplierPile.removeFromCurrentlyHandlingEntityList(contract.getProduct(), assignedAmount);
				buyerPile.addToCurrentlyHandlingEntityList(contract.getProduct(), assignedAmount);
				contract.addToFulfilledAmount(assignedAmount);	
				tempAmount -= assignedAmount;
			}
		}
	}
	
	/**
	 * MovingEntities should be saved in the list associated with the name of their prototype
	 */
	public HashMapList<MovingEntity, MovingEntity> getUnassignedMovingEntityList(){
		return unassignedMovingEntityList;
	}
	
	/**
	 * Fleets should be saved in the list associated with the name of their prototype moving entity
	 */
	public HashMapList<Contract, MovingEntity> getInactiveContractsList(){
		return inactiveContractsList;
	}
	
	/**
	 * If scheduled contracts list is not empty, assigns the next contract in the priority list as the next destination
	 * <br> Always assigns the owning facility as the first in destinations
	 */
	
	public void scheduleFleetDestinations(Fleet fleet){
		fleet.getDestinationsList().clear();
		Contract tempContract = new Contract();
		
		// Simply assigns the owning facility as the first destination and the first contract as the next
		fleet.getDestinationsList().add(0,fleet.getFacility());
		tempContract = fleet.getScheduledContracts().peek();
		if(fleet.getFacility() == tempContract.getBuyer())
			fleet.getDestinationsList().add(1,tempContract.getSupplier());
		else
			fleet.getDestinationsList().add(1,tempContract.getBuyer());		
	}
	
	/**
	 * plans the optimum fleet size required for a balanced delivery of the serving contract. i.e. based on cycle times, tries to finish delivery during the contract period.
	 * uses formula below to calculate the fleet size: <br> (contract amount/moving entity capacity)/(available hours in the period/cycle time).
	 * <br> <br>TODO right now fleets only serve one contract and one product at a time and all moving entities within fleet are
	 * identical.
	 * a balanced fleet over the course of a period. add more logic for optimized equipment utilizaiton,etc 
	 */
	public void scheduleBalancedFleet(Fleet fleet){
		Contract tempContract = fleet.getScheduledContracts().peek();
		MovingEntity tempMovingEntity = fleet.getFleetProtoType();
		BulkMaterial tempMaterial = tempContract.getProduct();
		//TODO sets the required amount to cargo capacity of the moving entity. refactor for multiple products
		double requiredAmount = tempMovingEntity.getAcceptingBulkMaterialList().getValueFor(tempMaterial, 0);
				
		LoadingBay buyerBay = tempContract.getBuyer().getOperationsManager().getBulkHandlingRoute(tempMovingEntity, tempMaterial,false).getLoadingBay();
		LoadingBay supplierBay = tempContract.getSupplier().getOperationsManager().getBulkHandlingRoute(tempMovingEntity, tempMaterial,true).getLoadingBay();
		double cycle;

		fleet.setEstimatedCycleAtBuyer(buyerBay.calcTurnAroundTime(tempMovingEntity, tempMaterial, requiredAmount));
		fleet.setEstimatedCycleAtSupplier(supplierBay.calcTurnAroundTime(tempMovingEntity, tempMaterial, requiredAmount));
		
		fleet.setEstimatedBuyerToSupplier(this.estimateTravelTimeOnRoute(tempMovingEntity, RouteManager.getBayToBayRoute(buyerBay, supplierBay, tempMovingEntity)));
		fleet.setEstimatedSupplierToBuyer(this.estimateTravelTimeOnRoute(tempMovingEntity, RouteManager.getBayToBayRoute(supplierBay, buyerBay, tempMovingEntity)));
		
		cycle = fleet.getEstimatedCycleAtBuyer()+fleet.getEstimatedBuyerToSupplier()+
				fleet.getEstimatedCycleAtSupplier()+fleet.getEstimatedSupplierToBuyer();
		
		double fleetSize = tempContract.getContractAmount() / fleet.getFleetProtoType().getAcceptingBulkMaterialList().getValueFor(tempMaterial, 0);
		
		
		// TODO This assumes contract period is from getSimTime until getSimTime+contractperiod which might not be true! 
		fleetSize /= tempContract.getSupplier().getNetAverageAvailability(this.getSimTime(), this.getSimTime() + tempContract.getContractPeriod()) *
				tempContract.getContractPeriod() / cycle;
		
		// Set fleet's parameters
		fleet.getTentativeLoadingBaysList().add(supplierBay);
		fleet.getTentativeUnLoadingBaysList().add(buyerBay);
		fleet.setEstimatedCycleTime(cycle);
		fleet.setOptimumSize((int) Math.ceil(fleetSize));
		if(supplierBay.getFacility()== fleet.getFacility())
			fleet.setDispatchInterval(fleet.getEstimatedCycleAtBuyer());
		else
			fleet.setDispatchInterval(fleet.getEstimatedCycleAtSupplier());
				
	}
	
	/**
	 *  List of transporters and costs and capacities
	 * <br> <b> 0- </b> transportation capacity for the transporter!
	 * <br> <b> 1- </b> transporter's reserved capacity (for the planning horizon)
	 */
	public TwoLinkedLists<MovingEntity> getTransportersList(){
		return transportationCapacityList;
	}
	
	/**
	 * 
	 * @return least cost per unit transporter that has remaining capacity for transporting bulkMaterial o.w. null
	 */
	public <T extends DiscreteHandlingLinkedEntity> Route getLeastCostTranspotationRoute(BulkMaterial bulkMaterial, T origin, T destination, double transportaionCostCap, ArrayList<T> tabuList){
		double cost = Double.POSITIVE_INFINITY;
		Route returnEntity = null;
		Route tempRoute;
		double tempCost;
		boolean foundMovingEntity = false;
		
		for(MovingEntity each: transportersList.getValue()){
			if(each.getAcceptingBulkMaterialList().contains(bulkMaterial)){
				foundMovingEntity = true;
				tempRoute = RouteManager.getRoute(origin, destination, each, bulkMaterial, Route_Type.LEASTCOST, transshipmentAllowed.getValue(), transportaionCostCap,tabuList);
				if(Tester.greaterOrEqualCheckTolerance(transportationCapacityList.getValueFor(each, 0) - transportationCapacityList.getValueFor(each, 1),0.0d) &&
					tempRoute != null){
					tempCost = tempRoute.estimateTransportationCostonRoute(bulkMaterial, true);
					if(tempCost < cost ){
						cost = tempCost;
						returnEntity = tempRoute;	
					}
				}
			}
		}
		
		if(!foundMovingEntity)
			throw new InputErrorException("Please check transportation structure for %s. None of the moving entities that "
					+ "this facility uses for transportation, carries %s!", this.getFacility(),  bulkMaterial.getName(), this.getName());
		
		return returnEntity;
	}
	
	/**
	 * 
	 * @return least cost per unit transporter that has remaining capacity for transporting bulkMaterial
	 */
	public <T extends DiscreteHandlingLinkedEntity> Route getFastestTranspotationRoute(BulkMaterial bulkMaterial,T origin, T destination, double travelTimeCap, ArrayList<T> tabuList){
		double leastTime = Double.POSITIVE_INFINITY;
		Route returnEntity = null;
		Route tempRoute;
		double tempTime;
		boolean foundMovingEntity = false;
		
		for(MovingEntity each: transportersList.getValue()){
			if(each.getAcceptingBulkMaterialList().contains(bulkMaterial)){
				foundMovingEntity = true;
				tempRoute = RouteManager.getRoute(origin, destination, each, bulkMaterial, Route_Type.FASTEST, transshipmentAllowed.getValue(), travelTimeCap, tabuList);
				if(Tester.greaterCheckTolerance(transportationCapacityList.getValueFor(each, 0) - transportationCapacityList.getValueFor(each, 1),0.0d) &&
					tempRoute != null){
					tempTime = tempRoute.estimateTravelTimeonRoute();
					if(tempTime < leastTime ){
						leastTime = tempTime;
						returnEntity = tempRoute;	
					}
				}
			}
		}
		
		if(!foundMovingEntity)
			throw new InputErrorException("Please check transportation structure for %s. None of the moving entities that "
					+ "this facility uses for transportation, carries %s!", this.getFacility(),  bulkMaterial.getName(), this.getName());
		
		return returnEntity;
	}
	
	/**
	 * 
	 * @return least cost per unit transporter that has remaining capacity for transporting bulkMaterial
	 */
	public <T extends DiscreteHandlingLinkedEntity> Route getShortestTranspotationRoute(BulkMaterial bulkMaterial, T origin, T destination, double distanceCap,ArrayList<T> tabuList){
		double length = Double.POSITIVE_INFINITY;
		Route returnEntity = null;
		Route tempRoute;
		double tempLength;
		boolean foundMovingEntity = false;
		
		for(MovingEntity each: transportersList.getValue()){
			if(each.getAcceptingBulkMaterialList().contains(bulkMaterial)){
				foundMovingEntity = true;
				tempRoute = RouteManager.getRoute(origin, destination, each, bulkMaterial, Route_Type.SHORTEST, transshipmentAllowed.getValue(), distanceCap, tabuList);
				if(Tester.greaterCheckTolerance(transportationCapacityList.getValueFor(each, 0) - transportationCapacityList.getValueFor(each, 1),0.0d) &&
					tempRoute != null){
					tempLength = tempRoute.getLength();
					if(tempLength < length ){
						length = tempLength;
						returnEntity = tempRoute;	
					}
				}
			}
		}
		
		if(!foundMovingEntity)
			throw new InputErrorException("Please check transportation structure for %s. None of the moving entities that "
					+ "this facility uses for transportation, carries %s!", this.getFacility(),  bulkMaterial.getName(), this.getName());
		
		return returnEntity;
	}
	public void resetTransportationPlan(){
		// TODO !!!!
	}
	
	/**
	 * @return the first entrance block that handles the passed entity and a path from loading bay to that entrance block exists. 
	 * @param loadingBay the loading bay 
	 */
	public EntranceBlock getFacilityEntranceBlock(MovingEntity movingEntity, LoadingBay loadingBay){
		//TODO rewrite!
		return null;
	}
	
	/**
	 * 
	 * @return the first exit block that handles the passed entity
	 */
	public ExitBlock getFacilityExitBlock(MovingEntity movingEntity, LoadingBay loadingBay){
		// TODO rewrite
		return null;
	}
	
	/**
	 * schedules destionation facility and destination entrance block for moving entity or adds to inactive contracts list
	 * @return
	 */
	public void scheduleDestination(MovingEntity movingEntity){
		Fleet fleet = movingEntity.getFleet();
		Contract contract = fleet.getScheduledContracts().peek();
		// if fleet is assigned a new contract(or still unassigned) and moving entities are called back
		if(movingEntity.isCalledBack()){
			///////////////////////////////////////
		} else{
			
		}
	}
	
	public void reserveDestinationForDelivery(MovingEntity movingEntity, Fleet fleet, Contract contract){
		for(BulkMaterial each: movingEntity.getCurrentlyHandlingBulkMaterialList())
			movingEntity.getDestinationFacility().addToStocksList(each, 8, movingEntity.getCurrentlyHandlingBulkMaterialAmount(each));
	}
	
	public void reserveDestinationForPickUp(MovingEntity movingEntity, Fleet fleet, Contract contract){
		BulkMaterial bulkMaterial = contract.getProduct();
		Facility facility = movingEntity.getDestinationFacility();
		BulkHandlingRoute tempBulkHandlingRoute = facility.getOperationsManager().getBulkHandlingRoute(movingEntity, bulkMaterial, true);
		facility.addToStocksList(tempBulkHandlingRoute.getInfeedBulkMaterial(), 7, movingEntity.getAcceptingBulkMaterialList().getValueFor(bulkMaterial, 0));
	}
	
	
	
	/**
	 * This method is used for standalone loader or processors. 
	 * schedules the next destination in the movingentity's destinations list after the passed currentloadingarea.
	 * if currentloadingarea is the last destination, schedules loops throw destinations again
	 * @return the loading area that this loader will travel to
	 */
	public Facility scheduleDestinationForStandalones (Facility currentFacility, MovingEntity movingEntity){
		
		ArrayList<Facility> tempDestinationsList =  movingEntity.getFacilitiesList();
		int index = tempDestinationsList.indexOf(currentFacility);
		if(index == tempDestinationsList.size()-1){
			index = 0;
		} else{
			index++;
		}
		
		movingEntity.setDestinationFacility(tempDestinationsList.get(index));;
		return tempDestinationsList.get(index);
		
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////
	// GETTERS AND SETTERS
	// ////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * 	TODO refactor to use empirical travel times when using inside simulation reports for optimizing results
	 * @return estimated travel time for the moving entity on the passed routesegmentslist including delays
	 * <br> travel time is calculated after the first entity in the routeSegmentList (assuming the movingEntity starts 
	 * from the end of the first entity in the routeSegmentList)
	 */
	public double estimateTravelTimeOnRoute(MovingEntity movingEntity, Route route){
		double travelTime = 0.0d;
		ArrayList<DiscreteHandlingLinkedEntity> routeSegmentsList = new ArrayList<>(route.getRouteSegmentsList());
		// assumes starting from the second entity in the passed routeSegmentList
		routeSegmentsList.remove(0);
		
		for (DiscreteHandlingLinkedEntity eachSegment: routeSegmentsList){			
			for(Delay eachDelay: eachSegment.getEnterDelay())
				travelTime += eachDelay.getNextDelayDuration(movingEntity);
			for(Delay eachDelay: eachSegment.getExitDelay())
				travelTime += eachDelay.getNextDelayDuration(movingEntity);
			try{
				RouteSegment tempRouteEntity = ((RouteSegment)eachSegment);
				 //  TODO speed limit is calculated just based on route's speed limit, add moving entity's speed limit as well
				travelTime += movingEntity.calcRemainingTravelTime(tempRouteEntity, tempRouteEntity.getSpeedLimit(movingEntity), 0.0d);
			}catch(ClassCastException e){};
		}
		return travelTime;
	}

	
	//////////////////////////////////////////////////////////////////////////////////////
	// Report METHODS
	//////////////////////////////////////////////////////////////////////////////////////


	
	// ////////////////////////////////////////////////////////////////////////////////////
	// UPDATERS
	// ////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);
		if(in == transportersList){
			for(MovingEntity each: transportersList.getValue()){
				transportersMap.add(each.getTransportMode(), each);
			}
		}
		
	}
}
