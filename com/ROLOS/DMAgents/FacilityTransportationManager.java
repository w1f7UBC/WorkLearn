package com.ROLOS.DMAgents;

import java.util.ArrayList;

import com.ROLOS.ROLOSEntity;
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
import com.ROLOS.Logistics.ReportAgent;
import com.ROLOS.Logistics.Route;
import com.ROLOS.Logistics.RouteSegment;
import com.ROLOS.Logistics.Stockpile;
import com.ROLOS.Utils.HandyUtils;
import com.ROLOS.Utils.HashMapList;
import com.ROLOS.Utils.TwoLinkedLists;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.units.CostPerMassPerDistUnit;
import com.jaamsim.units.CostPerVolumePerDistUnit;
import com.jaamsim.units.MassFlowUnit;
import com.jaamsim.units.VolumeFlowUnit;
import com.jaamsim.units.VolumeUnit;
import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Tester;


public class FacilityTransportationManager extends FacilityManager {
	
	@Keyword(description = "List of material that this facility has the capacity to transport.", 
			example = "Temiscaming/TransportationManager TransportableMaterialGroups { WoodChips }")
	private final EntityListInput<BulkMaterial> transportableMaterialList;
	
	@Keyword(description = "Moving Entity list that have cost and capacity defined for them and apear in the TransportableMaterialGroups list."
			+ "These transporters should correspond to fleets' prototypes. I.e. these cost structures apply to the fleet", 
			example = "Temiscaming/TransportationManager TransporterCostGroups { Truck1 Truck2 Train1 Train2 }")
	private final EntityListInput<MovingEntity> transportersList;
	
	@Keyword(description = "Yearly transportation capacity corresponding to TransporterCostGroups.", 
			example = "Temiscaming/TransportationManager TransportionCapacity { 50 20 30 20 kt/y }")
	private final ValueListInput transportationCapacity;

	@Keyword(description = "Cost of transportation per material unit corresponding to TransporterCostGroups per distance of route.", 
			example = "Temiscaming/TransportationManager TransportionCost { 5 2 1 1.8 $/t/km }")
	private final ValueListInput transportationCostGroups;
	
	private TwoLinkedLists<MovingEntity> transportationCapacityList;

	private HashMapList<MovingEntity,MovingEntity> unassignedMovingEntityList;
	private HashMapList<Contract,MovingEntity> inactiveContractsList;
		
	{
		transportableMaterialList = new EntityListInput<>(BulkMaterial.class, "TransportableMaterialGroups", "Economic", null);
		this.addInput(transportableMaterialList);
		
		transportersList = new EntityListInput<>(MovingEntity.class, "TransporterCostGroups", "Economic", null);
		this.addInput(transportersList);
		
		transportationCostGroups = new ValueListInput("TransportationCost", "Economic", null);
		this.addInput(transportationCostGroups);
		
		transportationCapacity = new ValueListInput("TransportationCapacity", "Key Inputs", null);
		this.addInput(transportationCapacity);

	}
	
	public FacilityTransportationManager() {
		transportationCapacityList = new TwoLinkedLists<>(3, new DescendingPriotityComparator<MovingEntity>(ROLOSEntity.class, "getInternalPriority"));

		unassignedMovingEntityList = new HashMapList<MovingEntity,MovingEntity>(1);
		inactiveContractsList = new HashMapList<Contract,MovingEntity>(1);
	}

	@Override
	public void validate() {
		super.validate();
		
		if(transportableMaterialList.getValue() != null){
			if(transportationCostGroups.getValue() == null || 
				(transportersList.getValue() != null && transportersList.getValue().size() != transportationCostGroups.getValue().size()))
				throw new InputErrorException("Number of groups in transportationCostGroups doesn't match TransporterCost list!");				
		}		
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();

		// the transportation capacity is added 
		if(transportersList.getValue() != null){
			for(int i = 0; i<transportersList.getValue().size(); i++){
				transportationCapacityList.set(transportersList.getValue().get(i), 0, transportationCostGroups.getValue().get(i));
				transportationCapacityList.set(transportersList.getValue().get(i), 1, transportationCapacity.getValue().get(i)*SimulationManager.getPlanningHorizon());
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
	public void scheduleTransportation(Contract contract){
		
		if(contract.isTransportationModeled()){
			
		} 
		// if transportation is not modeled for this contract, tele port material from one stock pile to the other
		else{
			// if transportation is not modeled, and if contract is active, remove material from supplier's 
			// stockpile(s) and add to buyer's stock pile(s)
			Stockpile supplierPile, buyerPile;
			supplierPile = contract.getSupplier().getOperationsManager().getStockpileForLoading(contract.getProduct());
			buyerPile = contract.getBuyer().getOperationsManager().getStockpileForUnLoading(contract.getProduct());
						
			// TODO uses a balance transportation when supplier has multiple supply contracts. 
			double tempAmount = Tester.min(contract.getUnfulfilledAmount(), supplierPile.getCurrentlyHandlingAmount()*contract.getSupplier().getGeneralManager().getContractBalancedAmountToFullfill(contract),
									buyerPile.getRemainingCapacity(contract.getProduct()));
			// do transaction between piles and contract
			supplierPile.removeFromCurrentlyHandlingEntityList(contract.getProduct(), tempAmount);
			buyerPile.addToCurrentlyHandlingEntityList(contract.getProduct(), tempAmount);
			contract.addToFulfilledAmount(tempAmount);			
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
	 * <br> <b> 0- </b> Transporters' cost per unit of material per distance
	 * <br> <b> 1- </b> transporters' total capacity (for the planning horizon)
	 * <br> <b> 2- </b> transporter's reserved capacity (for the planning horizon)
	 */
	public TwoLinkedLists<MovingEntity> getTransportersList(){
		return transportationCapacityList;
	}
	
	/**
	 * 
	 * @return least cost per unit transporter that has remaining capacity for transporting bulkMaterial
	 */
	public MovingEntity getLeastCostTransporter(BulkMaterial bulkMaterial,Facility origin, Facility destination){
		double cost = Double.POSITIVE_INFINITY;
		MovingEntity returnEntity = null;
		Route tempRoute;
		double tempCost;
		if(!transportableMaterialList.getValue().contains(bulkMaterial))
			return null;
		for(MovingEntity each: transportersList.getValue()){
			tempRoute = RouteManager.getRoute(origin, destination, each);
			if(Tester.greaterCheckTolerance(transportationCapacityList.getValueFor(each, 1) - transportationCapacityList.getValueFor(each, 2),0.0d) &&
					tempRoute != null){
				tempCost = this.estimateTransportationCostonRoute(each, bulkMaterial, tempRoute);
				if(tempCost < cost ){
					cost = tempCost;
					returnEntity = each;	
				}
			}
		}
		return returnEntity;
	}
	
	public void resetTransportationPlan(){
		if(transportersList.getValue() != null){
			for(int i = 0; i<transportersList.getValue().size(); i++){
				transportationCapacityList.set(transportersList.getValue().get(i), 2, 0.0d);
			}
		}
	}
	
	/**
	 * @return the first entrance block that handles the passed entity and a path from loading bay to that entrance block exists. 
	 * @param loadingBay the loading bay 
	 */
	public EntranceBlock getFacilityEntranceBlock(MovingEntity movingEntity, LoadingBay loadingBay){
		for(EntranceBlock each: this.getFacility().getInsideFacilityLimits().get(EntranceBlock.class)){
			if(each.checkIfHandles(movingEntity) && 
					RouteManager.getRoute(each, loadingBay,movingEntity).getRouteSegmentsList() != null)
				return each;
		}
		return null;
	}
	
	/**
	 * 
	 * @return the first exit block that handles the passed entity
	 */
	public ExitBlock getFacilityExitBlock(MovingEntity movingEntity, LoadingBay loadingBay){
		for(ExitBlock each: this.getFacility().getInsideFacilityLimits().get(ExitBlock.class)){
			if(each.checkIfHandles(movingEntity) && 
					RouteManager.getRoute(loadingBay,each,movingEntity).getRouteSegmentsList() != null)
				return each;
		}
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
			movingEntity.getDestinationFacility().addToStocksList(each, 6, movingEntity.getCurrentlyHandlingBulkMaterialAmount(each));
	}
	
	public void reserveDestinationForPickUp(MovingEntity movingEntity, Fleet fleet, Contract contract){
		BulkMaterial bulkMaterial = contract.getProduct();
		Facility facility = movingEntity.getDestinationFacility();
		BulkHandlingRoute tempBulkHandlingRoute = facility.getOperationsManager().getBulkHandlingRoute(movingEntity, bulkMaterial, true);
		facility.addToStocksList(tempBulkHandlingRoute.getInfeedBulkMaterial(), 5, movingEntity.getAcceptingBulkMaterialList().getValueFor(bulkMaterial, 0));
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
	
	/**
	 * TODO add logic to use cost structure passed in the movingEntity and etc to calculate transportation cost 
	 * @return $ per unit of bulkMaterial transported from origin to destination of the passed route. 
	 */
	public double estimateTransportationCostonRoute(MovingEntity movingEntity, BulkMaterial bulkMaterial, Route route){
		if(!transportableMaterialList.getValue().contains(bulkMaterial)|| !transportersList.getValue().contains(movingEntity))
			throw new InputErrorException("Please check transportation structure for %s. Transportation keywords aren't set correctly for"
					+ "%s to carry %s", this.getFacility(), movingEntity.getName(), bulkMaterial.getName());
		double unitCost = transportationCapacityList.getValueFor(movingEntity, 0) * route.getDistance();
		
		// print transportation cost report
		SimulationManager.printTransportationCostReport(movingEntity, route, bulkMaterial, unitCost);
		return unitCost;
					
	}
	
	public boolean canTransport(BulkMaterial bulkMaterial){
		return transportableMaterialList.getValue().contains(bulkMaterial) ? true : false;
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
		if(in == transportableMaterialList)
			if(transportableMaterialList.getValue().get(0).getEntityUnit() == VolumeUnit.class){
				transportationCostGroups.setUnitType(CostPerVolumePerDistUnit.class);
				transportationCapacity.setUnitType(VolumeFlowUnit.class);
			} else {
				transportationCostGroups.setUnitType(CostPerMassPerDistUnit.class);
				transportationCapacity.setUnitType(MassFlowUnit.class);
			}
	}		
}
