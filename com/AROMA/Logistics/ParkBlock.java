package com.AROMA.Logistics;

import com.AROMA.DMAgents.TrafficController;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Keyword;

public class ParkBlock extends RouteSegment {

	@Keyword(description = "List of facilities that use this parkblock", 
			example = "SilverdaleParkBlock FacilitiesList { Silverdale HSPP }")
	private final EntityListInput<Facility> facilitiesList;
	
	{
		facilitiesList = new EntityListInput<>(Facility.class, "FacilitiesList", "Key Inputs", null);
		this.addInput(facilitiesList);
	}
	
	public ParkBlock() {
	}
		
	@Override
	public void validate() {
		super.validate();
		if (facilitiesList.getValue() != null)
			for (Facility each: facilitiesList.getValue())
				each.addToInsideFacilityLimits(this);
				
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();
	}

	@Override
	public <T extends MovingEntity> void finishProcessingEntity(T entity) {
		super.finishProcessingEntity(entity);
		// if entity's been called back or fleet is dynamic and needs adjustment and entity doesn't carry any material, put in the unassigned fleet and unwind from fleet 
		if (this.getFacility() == entity.getFacility() && entity.getCurrentlyHandlingBulkMaterialAmount() == 0.0d)
			if(entity.isCalledBack() || (entity.getFleet().isDynamic() && entity.getFleet().getFleetSizeAdjustment() > 0.0d)){
				entity.getFacility().getTransportationManager().getUnassignedMovingEntityList().add((MovingEntity)entity.getProtoTypeEntity(), entity);
				entity.getFleet().getCurrentFleetList().remove(entity);
			}
			
	}
	
	/**
	 * triggers movingEntity's travel towards destination. Is used for scheduling dispatching rules
	 */
	public void dispatch(MovingEntity movingEntity){

		// reset origin facility (destination facility is already scheduled)
		movingEntity.setOriginFacility(this.getFacility());
		
		TrafficController.trafficControlManager.planNextMove(movingEntity);
		movingEntity.getPlannedNextRouteSegments().get(0).addToQueuedEntityList(movingEntity);
		
		movingEntity.setStartTravelingTimeOnCurrentRoute(this.getSimTime());
	}
}
