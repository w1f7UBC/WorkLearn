package com.ROLOS.Logistics;

import java.util.ArrayList;

import com.ROLOS.Economic.Contract;
import com.ROLOS.JavaSimulation.Tester_Rolos;
import com.jaamsim.math.Vec3d;

import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Tester;

public class Loader extends BulkHandlingLinkedEntity {

	private static final ArrayList<Loader> allInstances;
	
	@Keyword(description = "Whether this is a loader or a loader/unloader (identical to reclaimer, stacker, and stacker/reclaimer)." +
			"loaders reclaim from a stockpile, unloaders stack to a stockpile, and loader/unloaders do both. default is loader/unloader", 
			example = "FrontEndLoader LoaderOnly { TRUE }")
	private final BooleanInput loaderMode;
	
	@Keyword(description = "Whether this is a unloader or a loader/unloader (identical to reclaimer, stacker, and stacker/reclaimer)." +
			"loaders reclaim from a stockpile, unloaders stack to a stockpile, and loader/unloaders do both. default is loader/unloader", 
			example = "Stacker1 UnLoaderOnly { TRUE }")
	private final BooleanInput unloaderMode;
	
	private boolean loader;					// Loader and unloader concept are relative to the object they're working on. e.g. a loader loades cargo and a stacker (which is also a loader) loads stockpiles
	private boolean unloader;
	private Vec3d  lastOrientation, infeedOrientation, outfeedOrientation;
	private double rotationValue;
	
	static {
		allInstances = new ArrayList<Loader>(20);
	}
	
	{	
		loaderMode = new BooleanInput("LoaderOnly", "Key Inputs", false);
		this.addInput(loaderMode);
		
		unloaderMode = new BooleanInput("UnLoaderOnly", "Key Inputs", false);
		this.addInput(unloaderMode);
		
		lastOrientation = new Vec3d(this.getOrientationInput());
		infeedOrientation = new Vec3d(0, 0, 0);
		outfeedOrientation = new Vec3d(0, 0, 0);
		rotationValue = 0.1745;
	}
	
	public Loader() {
		loader = true;
		unloader = true;
		allInstances.add(this);
	}
	
	@Override
	public void validate() {
		super.validate();

		// set loader/unloader fields
		if(loaderMode.getValue())
			unloader = false;
		
		if(unloaderMode.getValue())
			loader = false;
		
		for(LinkedEntity each: this.getOutfeedLinkedEntityList()){
			if(each instanceof BulkCargo){
				if(loader)
					((BulkCargo) each).setAttachedLoader(this);
				if(unloader)
					((BulkCargo) each).setAttachedUnLoader(this);
				if(((BulkHandlingLinkedEntity) each).getCurrentTow() != null)
					this.attachToTow(((BulkCargo) each).getCurrentTow());
			} 	
		}
		
		if (loaderMode.getValue() == true && unloaderMode.getValue() == true)
			throw new InputErrorException("\"LoaderOnly\" and \"UnLoaderOnly\" are set simultaneously for %s", this.getName());
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();
		
	}
	
	public static ArrayList<? extends Loader> getAll() {
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

	//////////////////////////////////////////////////////////////////////////////////////
	// PROCESSING METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	public  void doLoading(Contract contract, Stockpile sourceStockpile, BulkMaterialStorage sinkBulkCargo, double loadingRate){
		double dt, tempAmount;
		
		// does loader feed a processor
		// start loading
		while(Tester.greaterCheckTolerance(sinkBulkCargo.getRemainingCapacity(contract.getProduct()), 0.0d)){
			tempAmount = Tester.min(sourceStockpile.getCurrentlyHandlingAmount(), sinkBulkCargo.getRemainingCapacity(contract.getProduct()));
			dt = tempAmount / loadingRate;
			sourceStockpile.setLastContentUpdateTime(this.getSimTime());
			sinkBulkCargo.setLastContentUpdateTime(this.getSimTime());
			this.simWait(dt);
			sinkBulkCargo.addToCurrentlyHandlingEntityList(contract.getProduct(), tempAmount);
			sourceStockpile.removeFromCurrentlyHandlingEntityList(contract.getProduct(), tempAmount);
			this.addToEntityAmountProcessed(contract.getProduct(), tempAmount);
			// add to fulfilled supply and demand
			contract.addToFulfilledAmount(tempAmount);
			
			// finish loading if loading area is exhaustible and stock pile is exhausted
			if(Tester.equalCheckTolerance(sourceStockpile.getCurrentlyHandlingAmount(),0.0d)){
				break;
			}
		}
	}
	
	public void doUnloading(Contract contract, BulkMaterialStorage sourceBulkCargo, Stockpile sinkStockpile,double unloadingRate){
		
		double dt,tempAmount, sourceMaterialDensity;
			
		// start loading
		while(Tester.greaterCheckTolerance(sourceBulkCargo.getCurrentlyHandlingAmount(contract.getProduct()), 0.0d)){
				
			tempAmount = Tester.min(sourceBulkCargo.getCurrentlyHandlingAmount(), sinkStockpile.getRemainingCapacity(contract.getProduct()));
			dt = tempAmount / this.getMaxRate(contract.getProduct()); 
			sourceBulkCargo.setLastContentUpdateTime(this.getSimTime());
			sinkStockpile.setLastContentUpdateTime(this.getSimTime());
			this.simWait(dt);
			sinkStockpile.addToCurrentlyHandlingEntityList(contract.getProduct(), tempAmount);
			sourceBulkCargo.removeFromCurrentlyHandlingEntityList(contract.getProduct(), tempAmount);
			this.addToEntityAmountProcessed(contract.getProduct(), tempAmount);
			// add to fulfilled supply and demand
			contract.addToFulfilledAmount(tempAmount);
		
			// finish unloading if stockpile is full
			if(Tester.equalCheckTolerance(sinkStockpile.getRemainingCapacity(contract.getProduct()),0.0d)){
				break;
			}
		}
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// REMOVER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////

	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// ADDER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * TODO Loader (unloader) with minimum rate passes on its rate as Moving entities infeed/outfeed rate
	 */
	@Override
	public void attachToTow(MovingEntity towingMovingEntity) {
		super.attachToTow(towingMovingEntity);
		
		if(this.isLoader()){
			for(LogisticsEntity each: this.getHandlingEntityTypeList())
				towingMovingEntity.getAcceptingBulkMaterialList().setMin((BulkMaterial) each,2,this.getMaxRate((BulkMaterial) each));
		} else {
			for(LogisticsEntity each: this.getHandlingEntityTypeList())
				towingMovingEntity.getAcceptingBulkMaterialList().setMin((BulkMaterial) each,3,this.getMaxRate((BulkMaterial) each));
		}
	}
		
	@Override
	public synchronized <T extends BulkHandlingLinkedEntity> void addToCurrentInfeed(
			T newInfeed, BulkMaterial newInfeedEntity, double newInfeedAmount) {
		super.addToCurrentInfeed(newInfeed, newInfeedEntity, newInfeedAmount);
		infeedOrientation = new Vec3d(newInfeed.getOrientation());
	}
	
	@Override
	public synchronized <T extends BulkHandlingLinkedEntity> void addToCurrentOutfeed(
			T newOutfeed, BulkMaterial newOutfeedEntity, double newOutfeedAmount) {
		super.addToCurrentOutfeed(newOutfeed, newOutfeedEntity, newOutfeedAmount);
		outfeedOrientation = new Vec3d(this.getOrientation());
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// GETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////

	public boolean isLoader(){
		return loader;
	}
	
	public boolean isUnloader(){
		return unloader;
	}
	
	public LoadingBay getCurrentLoadingArea() {
		return (LoadingBay) this.getCurrentlHandlersList().get(0);
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////
	// GRAPHICS
	// ////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);
		
		if(this.getPresentState() == "Idle")
			lastOrientation.z = this.getOrientation().z;
		
		if (this.getPresentState() == "Loading" || this.getPresentState() == "UnLoading") {
			double zInfeed, zOutfeed, minZ, maxZ;
			zInfeed = this.getCurrentInfeedList().getFirstEntityList().get(0).getOrientation().z;
			zOutfeed = this.getCurrentOutfeedList().getFirstEntityList().get(0).getOrientation().z ;
			
			minZ = Tester_Rolos.min(zInfeed,zOutfeed)+this.getOrientationInput().z +1.5707;
			maxZ = Tester_Rolos.max(zInfeed,zOutfeed)+this.getOrientationInput().z +1.047 ;
			
			if(lastOrientation.z<minZ || lastOrientation.z>maxZ){
				lastOrientation.z=minZ;
				rotationValue = 0.1745;
				this.setOrientation(lastOrientation);
			} else if (lastOrientation.z == minZ){
				rotationValue = Math.abs(rotationValue);
				lastOrientation.z += rotationValue;
				this.setOrientation(lastOrientation);
			} else if (lastOrientation.z == maxZ){
				rotationValue = -1 * Math.abs(rotationValue);
				lastOrientation.z += rotationValue;
				this.setOrientation(lastOrientation);
			} else{
				if (rotationValue < 0.0d)
					lastOrientation.z = Tester_Rolos.max(minZ,lastOrientation.z+rotationValue);
				else
					lastOrientation.z = Tester.min(maxZ,lastOrientation.z+rotationValue);
				this.setOrientation(lastOrientation);				
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
		
		// print units
		this.printStatesUnitsHeader();	
		stateReportFile.newLine(2);
		stateReportFile.flush();	
	
	}
	

}
