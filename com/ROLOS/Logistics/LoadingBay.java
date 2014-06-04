package com.ROLOS.Logistics;

import java.util.ArrayList;
import java.util.LinkedList;

import com.ROLOS.DMAgents.TrafficController;
import com.ROLOS.Economic.Contract;
import com.jaamsim.events.ReflectionTarget;
import com.jaamsim.math.Vec3d;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.ErrorException;

import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Tester;


public class LoadingBay extends DiscreteHandlingLinkedEntity {
			
	// TODO if need be refactor to have loading areas that are loading and unloading at the same time
	@Keyword(description = "Whether the loading area is an unloading operation. default is loading.", 
			example = "LoadingArea1 UnloadingOperation { true }")
	private final BooleanInput unloading;
	
	@Keyword(description = "List of Loaders that work on this loading bay. Moving equipment should also be added in this list "
			+ "to populate the lists.", 
			example = "LoadingArea1 Loaders { Dumper1 }")
	private final EntityListInput<Loader> loadersList;
	
	private BulkHandlingRoute lastActiveBulkHandlingRoute;
	private ArrayList<BulkHandlingRoute> connectableRoutesList;
	private ArrayList<Stockpile> selfOperatingStockPilesList;
	
	{	
		loadersList = new EntityListInput<>(Loader.class, "Loaders", "Key Inputs", null);
		this.addInput(loadersList);
		
		unloading = new BooleanInput("UnloadingOperation", "Key Inputs",false);
		this.addInput(unloading);
		
	}
	
	public LoadingBay() {
		connectableRoutesList = new ArrayList<>(1);
		selfOperatingStockPilesList = new ArrayList<>(1);
	}
	
	@Override
	public void validate() {
		super.validate();
		
		// populate bulk handling routes
		LinkedList<LinkedEntity> tempList= new LinkedList<>();
		tempList.add(this);
		this.getFacility().getOperationsManager().populateBulkHanglingRoute(tempList, null);
		
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();
		
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// GETTER/CALCULATION METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	/**
	 * @return estimate of turnaround time at this loading bay including enter and exit delays.
	 */
	public double calcTurnAroundTime(MovingEntity movingEntity, BulkMaterial bulkMaterial, double amount){
		double loadingRate = 0.0d;
		double totalTime = 0.0d;
		// TODO it assumes if self loading/unloading is allowed it will use attached loader/unloader regardless of stockpile's content and loading priorities
		if (!selfOperatingStockPilesList.isEmpty()) {
			if (this.isUnloading())
				loadingRate = movingEntity.getAcceptingBulkMaterialList()
						.getValueFor(bulkMaterial, 3);
			else
				loadingRate = movingEntity.getAcceptingBulkMaterialList()
						.getValueFor(bulkMaterial, 2);
		}
			BulkHandlingRoute tempRoute = null;
			if(loadingRate == 0.0d){
				if(this.isUnloading())
					tempRoute = this.getFacility().getOperationsManager().getBulkHandlingRoute(movingEntity, bulkMaterial, false);	
				else
					tempRoute = this.getFacility().getOperationsManager().getBulkHandlingRoute(movingEntity, bulkMaterial, true);	
				loadingRate = tempRoute.getOutfeedRate();
			}
					
			if (loadingRate == 0.0d)
				throw new ErrorException("Couldn't find a loader/unloader to estimate turn around time at %s for %s!", this.getName(), movingEntity.getName());
			
			totalTime = movingEntity.getAcceptingBulkMaterialList().getValueFor(bulkMaterial, 1) / loadingRate;
			
			for (Delay each: this.getEnterDelay()){
				totalTime += each.getNextDelayDuration(movingEntity);
			}
			for (Delay each: this.getExitDelay()){
				totalTime += each.getNextDelayDuration(movingEntity);
			}
			
			return totalTime;	
	}
	
	public BulkHandlingRoute getLastActiveBulkHandlingRoute(){
		return  lastActiveBulkHandlingRoute;
	}
	
	public ArrayList<BulkHandlingRoute> getConnectableRoutesList(){
		return connectableRoutesList;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	// ADDER METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	// REMOVER METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public <T extends LogisticsEntity> void removeFromCurrentlyHandlingEntityList(
			T entityToRemove, double amountToRemove) {
		super.removeFromCurrentlyHandlingEntityList(entityToRemove, amountToRemove);
	/*	if((entityToRemove instanceof MovingEntity) && !this.getCurrentlyHandlingEntityList().isEmpty() && this.getQueuedEntitiesList().isEmpty()){
			for(LogisticsEntity each: this.getCurrentlyHandlingEntityList()){
				if((each instanceof MovingEntity) && ((MovingEntity)each).getCurrentlyTowingEntityList().get(BulkCargo.class.getSimpleName()).isEmpty()){
					Process.start(new ReflectionTarget(this,"finishProcessingEntity",((MovingEntity)each)));
					break;
				}
			}
		}
		*/
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	// PROCESSING ENTITIES METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * starts loading or unloading bulk cargos. 
	 * TODO should be refactored when complex loading and unloading bulk material is possible. right now, only standalone
	 * loader and unloaders are assumed
	 */
	@Override
	public void startProcessingQueuedEntities() {
		super.startProcessingQueuedEntities();
		int i = 0;
		for (; i<this.getQueuedEntitiesList().size();) {
			MovingEntity entityUnderProcess =  (MovingEntity) this.getQueuedEntitiesList().get(i);
			
			if (this.isReadyToHandle(entityUnderProcess)) {
				this.removeFromQueuedEntityList(entityUnderProcess);
				ArrayList<? extends LogisticsEntity> tempHandledByList = new ArrayList<>(entityUnderProcess.getCurrentlHandlersList());
				// TODO refactor when entities that have length (e.g. trains) are figured out
				for (LogisticsEntity each : tempHandledByList) {
					((LinkedEntity) each).removeFromCurrentlyHandlingEntityList(entityUnderProcess, 1);
				}
				this.addToCurrentlyHandlingEntityList(entityUnderProcess, 1);
				entityUnderProcess.setStartTravelingTimeOnCurrentRouteSegment(this.getSimTime());
				entityUnderProcess.setLastPosition(getPositionForAlignment(new Vec3d()));
				entityUnderProcess.setDistanceTraveledOnCurrentRouteSegment(0);
				entityUnderProcess.setCurrentSpeed(0.0d);
				
				//if contract between the two companies is voided and they are not carrying anything, finish processing without delay
				if((entityUnderProcess.getFleet().getScheduledContracts().get(0).getSupplier() != this.getFacility() || 
						entityUnderProcess.getFleet().getScheduledContracts().get(0).getBuyer() != this.getFacility()) && entityUnderProcess.getCurrentlyHandlingBulkMaterialAmount()!= 0.0d){
					this.finishProcessingEntity(entityUnderProcess);
					this.setPresentState("Idle");
				} else{
									
					if(this.getEnterDelay() != null){
						for (Delay eachDelay: this.getEnterDelay()) {
							entityUnderProcess.setPresentState("Enter Delay - "
									+ eachDelay.getName());
							this.simWait(eachDelay.getNextDelayDuration(
									entityUnderProcess));
						}
					}
					if (entityUnderProcess.isMovingEquipment()){
						for(ArrayList<ModelEntity> eachList: entityUnderProcess.getCurrentlyTowingEntityList().getValues())
							for(ModelEntity each: eachList)
								((BulkHandlingLinkedEntity)each).setLoadingBay(this);
						
						entityUnderProcess.setPresentState("Operating in Facility");
						continue;
					}
					// schedule last so that loaders and processors queued are processed first
					scheduleLastFIFO();
					if (unloading.getValue()) {
						this.startProcess(new ReflectionTarget(this,"startUnloading",entityUnderProcess));
					} else {
						this.startProcess(new ReflectionTarget(this,"startLoading",entityUnderProcess));
					}
				}
				
			} else
				i++;
		}
		this.setTriggered(false);
	}
	
	public void startLoading(MovingEntity entityUnderProcess){
		
		BulkMaterial tempBulkMaterial;
		BulkHandlingRoute tempRoute;
		for(BulkCargo tempBulkCargo: entityUnderProcess.getCurrentlyTowingEntityList().get(BulkCargo.class)){
			Contract contract =  entityUnderProcess.getFleet().getScheduledContracts().peek();
			tempBulkMaterial = contract.getProduct();
			BulkHandlingLinkedEntity activeEquipment = null;

			while(Tester.greaterCheckTolerance(tempBulkCargo.getRemainingCapacity(tempBulkMaterial),0.0d) && contract.isActive()){
				// assign reserved route
				if(this.lastActiveBulkHandlingRoute != null && this.lastActiveBulkHandlingRoute.isConnected() && this.lastActiveBulkHandlingRoute.getOutfeedBulkMaterial().equals(tempBulkMaterial.getProtoTypeEntity()))
					tempRoute = this.lastActiveBulkHandlingRoute;
				else
					tempRoute = this.getFacility().getOperationsManager().getBulkHandlingRoute(this, tempBulkMaterial, true);
				
				
				activeEquipment = this.getFacility().getOperationsManager().activateRoute(tempRoute, tempBulkCargo);
				
				// start loading through processor if processor is active 
				if(activeEquipment instanceof BulkMaterialProcessor){					
					((BulkMaterialProcessor) activeEquipment).doProcessing(contract, tempRoute.getStockpile(),tempBulkCargo,tempRoute.getInfeedRate(),tempRoute.getOutfeedRate(),tempRoute.getInfeedBulkMaterial(),tempRoute.getOutfeedBulkMaterial());
				}
				// ow start loading through loader
				else {
					((Loader) activeEquipment).doLoading(contract, tempRoute.getStockpile(),tempBulkCargo,activeEquipment.getMaxRate());
				}
				this.getFacility().getOperationsManager().deactivateRoute(tempRoute, tempBulkCargo);
				
				if(tempRoute.getStockpile().getCurrentlyHandlingAmount() == 0.0d && tempRoute.getStockpile().getExhaustionDelay() != null){
					tempRoute.getStockpile().setPresentState("Exhaustion Delay - "+ tempRoute.getStockpile().getExhaustionDelay().getName());
					this.simWait(tempRoute.getStockpile().getExhaustionDelay().getNextDelayDuration());		
				}
			} 	
			//remove from facilities reserved bulk material list
			this.getFacility().removeFromStocksList(tempBulkMaterial, 5, tempBulkCargo.getCapacity());
			if(tempBulkCargo.getFacility() != this.getFacility())
				contract.removeFromReservedForFulfilling(tempBulkCargo.getCapacity());
		}
		this.finishProcessingEntity(entityUnderProcess);
		this.setPresentState("Idle");
	}
	
	public void startUnloading(MovingEntity entityUnderProcess){
		BulkMaterial tempBulkMaterial;
		BulkHandlingRoute tempRoute;
		for(BulkCargo tempBulkCargo: entityUnderProcess.getCurrentlyTowingEntityList().get(BulkCargo.class)){
			Contract contract =  entityUnderProcess.getFleet().getScheduledContracts().peek();
			BulkHandlingLinkedEntity activeEquipment = null;
			tempBulkMaterial = (BulkMaterial) tempBulkCargo.getCurrentlyHandlingList().getEntityList().get(0);
			// reserved amount for unloading is the exact loaded amount on bulk cargo
			//TODO refactor if bulkcargo's density change and as a result the reserved amount for unloading would be different than the actual bulk cargo
			double reservedAmount = tempBulkCargo.getCurrentlyHandlingAmount();
			
			while(Tester.greaterCheckTolerance(
					tempBulkCargo.getCurrentlyHandlingAmount(), 0.0d)){

				// assign reserved route
				if(this.lastActiveBulkHandlingRoute != null && this.lastActiveBulkHandlingRoute.isConnected() && this.lastActiveBulkHandlingRoute.getOutfeedBulkMaterial().equals(tempBulkMaterial.getProtoTypeEntity()))
					tempRoute = this.lastActiveBulkHandlingRoute;
				else
					tempRoute = this.getFacility().getOperationsManager().getBulkHandlingRoute(this, tempBulkMaterial, false);
				
				
				activeEquipment = this.getFacility().getOperationsManager().activateRoute(tempRoute, tempBulkCargo);
				
				// start loading through loader
				((Loader) activeEquipment).doUnloading(contract, tempBulkCargo, tempRoute.getStockpile(),activeEquipment.getMaxRate());
			
				this.getFacility().getOperationsManager().deactivateRoute(tempRoute, tempBulkCargo);
				
				if(tempRoute.getStockpile().getCurrentlyHandlingAmount() == 0.0d && tempRoute.getStockpile().getExhaustionDelay() != null){
					tempRoute.getStockpile().setPresentState("Exhaustion Delay - "+ tempRoute.getStockpile().getExhaustionDelay().getName());
					this.simWait(tempRoute.getStockpile().getExhaustionDelay().getNextDelayDuration());		
				}
			} 
			//remove from facilities reserved bulk material list
			this.getFacility().removeFromStocksList(tempBulkMaterial, 6, reservedAmount);
			if(tempBulkCargo.getFacility() != this.getFacility())
				contract.removeFromReservedForFulfilling(tempBulkCargo.getCapacity());
		}
		this.finishProcessingEntity(entityUnderProcess);
		this.setPresentState("Idle");
	}
	
	@Override
	public <T extends MovingEntity> void finishProcessingEntity(T entityUnderProcess) {
		super.finishProcessingEntity(entityUnderProcess);

		entityUnderProcess.setLastLoadingBay(this);
		// wakeup parked entities that waited for reserved amounts to be rectified
	/*	for (ExitBlock each: ExitBlock.getAll()){
			each.wakeupParkedForDestinationSchedulingEntities(this.getFacility());
		}
		*/
		if (entityUnderProcess.isMovingEquipment()){
			for(ArrayList<ModelEntity> eachList: entityUnderProcess.getCurrentlyTowingEntityList().getValues())
				for(ModelEntity each: eachList)
					((BulkHandlingLinkedEntity)each).removeFromLoadingBay(this);					
		}
			
		ExitBlock tempExitBlock = this.getFacility().getTransportationManager().getFacilityExitBlock(entityUnderProcess,this);
		entityUnderProcess.setCurrentDestination(tempExitBlock);
		TrafficController.trafficControlManager.planNextMove(entityUnderProcess);
		entityUnderProcess.getPlannedNextRouteSegments().get(0).addToQueuedEntityList(entityUnderProcess);
		
		entityUnderProcess.setStartTravelingTimeOnCurrentRoute(this.getSimTime());
		
	/*	if (this.getFacility().isCycleStart() == true)
			entityUnderProcess.collectCycleStats();*/
		
		TrafficController.trafficControlManager.planNextMove(entityUnderProcess);
		entityUnderProcess.getPlannedNextRouteSegments().get(0).addToQueuedEntityList(entityUnderProcess);
		
		entityUnderProcess.setStartTravelingTimeOnCurrentRoute(this.getSimTime());

	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// ASSERTION METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public boolean isUnloading(){
		return unloading.getValue();
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// SETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	public void setLastActiveBulkHandlingRoute(BulkHandlingRoute lastActiveRoute){
		this.lastActiveBulkHandlingRoute = lastActiveRoute;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	//GETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////

	
	public <T extends LogisticsEntity> double getReservableAmountForRemoving(T entity){
		return this.getCurrentlyHandlingAmount(entity) - this.getCurrentlyReservedAmount(entity);
	}
	
	@Override
	public <T extends LogisticsEntity> double getReservableAmountForAdding(T ent){
		return this.getRemainingCapacity(ent) - this.getCurrentlyReservedAmount(ent);
	}
	
	public ArrayList<Stockpile> getSelfOperatingStockpiles(){
		return selfOperatingStockPilesList;
	}
	
	
	
	// ////////////////////////////////////////////////////////////////////////////////////
	// GRAPHICS 
	// ////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void updateGraphics(double simTime) {
		//place generated entities at the centre of the LoadingArea
		if (!this.getCurrentlyHandlingList().isEmpty()) {
			
			for (LogisticsEntity each : this.getCurrentlyHandlingList().getEntityList()) {
				if (each instanceof MovingEntity) {
					Vec3d loadingAreaCentre = this.getPositionForAlignment(new Vec3d());
					Vec3d orient = new Vec3d(this.getOrientation());
					loadingAreaCentre.add3(((MovingEntity) each).getLoadingAreaPosition());
					each.setPositionForAlignment(each.getAlignmentInput(),
							loadingAreaCentre);
					orient.add3(each.getOrientationInput());
					each.setOrientation(orient);
				}
			}
		}	
		
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
		stateReportFile.newLine();
		
		// print units
		this.printStatesUnitsHeader();
		stateReportFile.flush();

	}
	
	@Override
	public void printStatesUnitsHeader() {
		super.printStatesUnitsHeader();
	}
	
}
