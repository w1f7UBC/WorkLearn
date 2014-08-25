package com.ROLOS.Logistics;

import java.util.LinkedList;

import com.jaamsim.input.ValueInput;
import com.jaamsim.units.Unit;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.Tester;

public class Stockpile extends BulkMaterialStorage {
	
	@Keyword(description = "Minimum amount of bulk material required in the stockpile for reclaiming."
			+ "Default is 0.",
	         example = "Stockpile1  MinimumReclaimableAmount { 45 m3 }")
	private final ValueInput minReclaimableAmount;
	
	//TODO refactor for multiple source and sinks
	private EntitySource source;
	private EntitySink sink;
	
	// whether last time this was disconnected
	private boolean lastDisconnectedFromLoading, lastDisconnectedFromUnloading;
	
	{
		minReclaimableAmount = new ValueInput("MinimumReclaimableAmount", "Key Inputs", 0.0d);
		minReclaimableAmount.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(minReclaimableAmount);
	}
	
	public Stockpile() {
		lastDisconnectedFromLoading = true;
		lastDisconnectedFromUnloading = true;
	}
		
	@Override
	public void validate() {
		super.validate();
		if(this.getFacility() == null)
			throw new ErrorException("Facility for %s is not set!", this.getName());
		
		this.getFacility().addToStocksList((BulkMaterial) this.getHandlingEntityTypeList().get(0), 5, this.getCapacity());
		
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
	// ADDER METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	@Override
	public <T1 extends LogisticsEntity> void addToCurrentlyHandlingEntityList(
			T1 entityToAdd, double amountToAdd) {
		super.addToCurrentlyHandlingEntityList(entityToAdd, amountToAdd);
				
		this.getFacility().addToStocksList((BulkMaterial) entityToAdd.getProtoTypeEntity(), 6, amountToAdd);
		
		//disconnect if filled up
		if(Tester.equalCheckTolerance(this.getRemainingCapacity((BulkMaterial) entityToAdd), 0.0d)){
			lastDisconnectedFromUnloading = true;

			//remove reserved routes for unloading bays when this stock pile gets full
			for(BulkHandlingRoute eachRoute: this.getFacility().getOperationsManager().getUnloadingRoutesList()){
				if(eachRoute.getStockpile() == this)
					eachRoute.incrementConnectedSegmentsCount(-1);	
			}
			for(ProcessingRoute eachRoute: this.getFacility().getOperationsManager().getProcessingRoutesList().getValues().get(0)){
				if(eachRoute.getOutfeedPiles().contains(this))
					eachRoute.incrementConnectedSegmentsCount(-1);	
			}
		}
		
		// connect if just got material
		if(lastDisconnectedFromLoading){
 			lastDisconnectedFromLoading = false;
			//reactivate sink
			if(sink != null)
				sink.startProcess("consume");
			
			for(BulkHandlingRoute eachRoute: this.getFacility().getOperationsManager().getLoadingRoutesList()){
				if(eachRoute.getStockpile() == this)
					eachRoute.incrementConnectedSegmentsCount(1);	
			}
			for(ProcessingRoute eachRoute: this.getFacility().getOperationsManager().getProcessingRoutesList().getValues().get(0)){
				if(eachRoute.getInfeedPiles().contains(this))
					eachRoute.incrementConnectedSegmentsCount(1);	
			}
		}
				
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	// REMOVER METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	@Override
	public <T extends LogisticsEntity> void removeFromCurrentlyHandlingEntityList(T entityToRemove, double amountToRemove) {
		super.removeFromCurrentlyHandlingEntityList(entityToRemove, amountToRemove);
		
		
		this.getFacility().removeFromStocksList((BulkMaterial) entityToRemove.getProtoTypeEntity(), 6, amountToRemove);
		
		if(Tester.lessOrEqualCheckTolerance(this.getCurrentlyHandlingAmount(), minReclaimableAmount.getValue())){
			lastDisconnectedFromLoading = true;
			for(BulkHandlingRoute eachRoute: this.getFacility().getOperationsManager().getLoadingRoutesList()){
				if(eachRoute.getStockpile() == this)
					eachRoute.incrementConnectedSegmentsCount(-1);	
			}
			
			// TODO assuming only one processing route!
			for(ProcessingRoute eachRoute: this.getFacility().getOperationsManager().getProcessingRoutesList().getValues().get(0)){
				if(eachRoute.getInfeedPiles().contains(this))
					eachRoute.incrementConnectedSegmentsCount(-1);	
			}
		}
		
		if(lastDisconnectedFromUnloading && Tester.greaterCheckTolerance(amountToRemove , 0.0d)){
			lastDisconnectedFromUnloading = false;
			//reactivate source
			if(source != null)
				source.startProcess("generate");
			for(BulkHandlingRoute eachRoute: this.getFacility().getOperationsManager().getUnloadingRoutesList()){
				if(eachRoute.getStockpile() == this)
					eachRoute.incrementConnectedSegmentsCount(1);	
			}
			for(ProcessingRoute eachRoute: this.getFacility().getOperationsManager().getProcessingRoutesList().getValues().get(0)){
				if(eachRoute.getOutfeedPiles().contains(this))
					eachRoute.incrementConnectedSegmentsCount(1);	
			}
		}

	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// ASSERTION METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// SETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////

	public void setReclaimableAmountUnits(Class<? extends Unit> unit){
		minReclaimableAmount.setUnitType(unit);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	//GETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public double getReclaimableAmount(){
		return minReclaimableAmount.getValue();
	}
	
	public boolean lastDisconnectedFromLoading(){
		return lastDisconnectedFromLoading;
	}
	
	public boolean lastDisconnectedFromUnloading(){
		return lastDisconnectedFromUnloading;
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
