package com.ROLOS.Logistics;

import com.ROLOS.DMAgents.TrafficController;
import com.ROLOS.Economic.Contract;



/**
 * Entrance blocks can be thought as parking areas before loading bays of a facility. Moving entities will be parked
 * until a loading bay can be assigned. 
 */
public class EntranceBlock extends ParkBlock {

	public EntranceBlock() {
	}
	
	@Override
	public void validate() {
		super.validate();
		
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();
		
	}
	
	@Override
	public <T extends MovingEntity> void finishProcessingEntity(T entity) {
		super.finishProcessingEntity(entity);
		LoadingBay tempLoadingBay;
		Contract tempContract = entity.getFleet().getScheduledContracts().peek();
		if(tempContract.getBuyer() == this.getFacility())
			tempLoadingBay = this.getFacility().getOperationsManager().getBulkHandlingRoute(entity,tempContract.getProduct(),false).getLoadingBay();
		else
			tempLoadingBay = this.getFacility().getOperationsManager().getBulkHandlingRoute(entity,tempContract.getProduct(),true).getLoadingBay();

		entity.setCurrentDestination(tempLoadingBay);
		
		TrafficController.trafficControlManager.planNextMove(entity);
		entity.getPlannedNextRouteSegments().get(0).addToQueuedEntityList(entity);
		
		entity.setStartTravelingTimeOnCurrentRoute(this.getSimTime());

	}

}
