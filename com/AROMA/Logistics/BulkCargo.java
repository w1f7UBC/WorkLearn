package com.AROMA.Logistics;



public class BulkCargo extends BulkMaterialStorage {
	
	private Loader attachedLoader;
	private Loader attachedUnLoader;

	public BulkCargo() {
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
	public <T1 extends LogisticsEntity> void addToCurrentlyHandlingEntityList(
			T1 entityToAdd, double amountToAdd) {
		super.addToCurrentlyHandlingEntityList(entityToAdd, amountToAdd);
		this.getCurrentTow().getAcceptingBulkMaterialList().add((BulkMaterial) entityToAdd, 1, amountToAdd);
	}
	
	@Override
	public void attachToTow(MovingEntity towingMovingEntity) {
		super.attachToTow(towingMovingEntity);
		for(LogisticsEntity each: this.getHandlingEntityTypeList()){
			towingMovingEntity.getAcceptingBulkMaterialList().add((BulkMaterial) each, 0, this.getCapacity(each));
			if(attachedLoader != null)
				towingMovingEntity.getAcceptingBulkMaterialList().setMin((BulkMaterial) each, 2, attachedLoader.getMaxRate((BulkMaterial) each));
			if(attachedUnLoader != null)
				towingMovingEntity.getAcceptingBulkMaterialList().setMin((BulkMaterial) each, 3, attachedUnLoader.getMaxRate((BulkMaterial) each));
		}
	}
	
	@Override
	public void detachFromCurrentTow() {
		MovingEntity tempMovingEntity = this.getCurrentTow();
		super.detachFromCurrentTow();
		for(LogisticsEntity each: this.getHandlingEntityTypeList())
			tempMovingEntity.getAcceptingBulkMaterialList().remove((BulkMaterial) each, 0, this.getCapacity(each));			
	}	
	
	@Override
	public <T extends LogisticsEntity> void removeFromCurrentlyHandlingEntityList(
			T bulkMaterialToRemove, double amountToRemove) {
		super.removeFromCurrentlyHandlingEntityList(bulkMaterialToRemove,
				amountToRemove);
		this.getCurrentTow().getAcceptingBulkMaterialList().remove((BulkMaterial) bulkMaterialToRemove, 1, amountToRemove);
	}
	
	public Loader getAttachedLoader() {
		return attachedLoader;
	}

	public void setAttachedLoader(Loader attachedLoader) {
		this.attachedLoader = attachedLoader;
	}

	public Loader getAttachedUnLoader() {
		return attachedUnLoader;
	}

	public void setAttachedUnLoader(Loader attachedUnLoader) {
		this.attachedUnLoader = attachedUnLoader;
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
