package com.ROLOS.Logistics;

import java.util.ArrayList;



public class EntityTerminator extends DiscreteHandlingLinkedEntity {
	
	public EntityTerminator() {
		
		super();
		
	}
	
	@Override
	public void validate() {
		// TODO Auto-generated method stub
		super.validate();
	}
	
	@Override
	public void startProcessingQueuedEntities() {
		// TODO Auto-generated method stub
		super.startProcessingQueuedEntities();
		this.setTriggered(true);
		while(!this.getQueuedEntitiesList().isEmpty()) {
			MovingEntity entityUnderProcess = (MovingEntity) this.getQueuedEntitiesList().get(0);
			while (!this.isReadyToHandle(entityUnderProcess)) {
				//TODO set present states of this and the queued entity
				waitUntil();
			} waitUntilEnded();
			
			this.removeFromQueuedEntityList(entityUnderProcess);
			
			ArrayList<LogisticsEntity> tempHandledByList = new ArrayList<>(entityUnderProcess.getCurrentlHandlersList());
			// TODO refactor when entities that have length (e.g. trains) is figured out
			for (LogisticsEntity each : tempHandledByList) {
				((LinkedEntity) each).removeFromCurrentlyHandlingEntityList(entityUnderProcess,1);
			}
			this.finishProcessingEntity(entityUnderProcess);
			entityUnderProcess.kill();
		}
		this.setTriggered(false);		
	}
	
}
