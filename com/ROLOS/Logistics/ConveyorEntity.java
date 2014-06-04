package com.ROLOS.Logistics;

import java.util.ArrayList;

import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.ErrorException;

import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Tester;

public class ConveyorEntity extends BulkHandlingLinkedEntity {

	private static final ArrayList<ConveyorEntity> allInstances;

	@Keyword(description = "Maximum speed for this conveyor.", 
			example = "Conveyor1 SpeedLimit { 2 m/s } ")
	private final DoubleInput speedLimit;
	
	@Keyword(description = "The length of the route entity.", 
			example = "Conveyor1 Length { 100 km }")
	private final DoubleInput length;					// Actual length
	
	static {
		allInstances = new ArrayList<ConveyorEntity>(20);
	}
	
	{
		speedLimit = new DoubleInput("SpeedLimit", "Key Inputs", Double.POSITIVE_INFINITY);
		speedLimit.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(speedLimit);
		speedLimit.setUnits("m/s");
		
		length = new DoubleInput("Length", "Key Inputs", 0.0d);
		length.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(length);
		length.setUnits("m");
	}
	
	public ConveyorEntity() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void validate() {
		// TODO Auto-generated method stub
		super.validate();
		
		// TODO conveyours should not be connected to an origin/destination and other conveyours at the same time at each end (creates internal priority problem). at each end they can either be connected to conveyours or origin/destination
		
	}
	
	@Override
	public void earlyInit() {
		// TODO Auto-generated method stub
		super.earlyInit();
	}
	
	public static ArrayList<? extends ConveyorEntity> getAll() {
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

	public double getSpeedLimit() {
		return speedLimit.getValue();
	}
	
/*	public <T extends GeneratableEntity> double getSpeedLimit(T entity) {
		GeneratableEntity ent;
		if (entity.testFlag(Entity.FLAG_GENERATED)) {
			ent = entity.getParentEntity();
		} else {
			ent = entity;
		}
		
		if (speedByEntityType.containsKey(ent)) {
			return speedByEntityType.get(ent);
		}
		throw new ErrorException("%s does not handle %s! at time: %f speed was checked for this entity.",this.getName(),ent.getName(),this.getCurrentTime());
	}
	*/
	public double getLength() {
		return length.getValue();
	}

	/**
	 * 	
	 * @return
	 */
/*	public synchronized <T1 extends LinkedEntity,T2 extends LogisticsEntity> double getReservableCapacity (T1 origin,T1 destination,T2 bulkEntity) {
		if (!this.checkIfHandles(bulkEntity)) return 0;
		double tempReservableCapacity = this.getCapacity(bulkEntity);
		if (this.getCurrentInfeedList().isEmpty()) {
			return tempReservableCapacity;
		} else {
			int i = 0;
			int passedObjectPriority = origin.getOriginPriority()+destination.getDestinationPriority();
			while (passedObjectPriority < ((LinkedEntity) this.getCurrentInfeedList().get(i)).getInternalPriority()) {
				i++;
				tempReservableCapacity -= this.getCurrentInfeedRateList().get(i);
			}
			if (Tester.lessCheckTolerance(tempReservableCapacity, 0)){
				throw new ErrorException("%s is currently handling more than its capacity! caught at time %f during " +
						"checking for reservable capacity", this.getName(),this.getCurrentTime());
			}
			return tempReservableCapacity;
		}	
	}
	*/
	/**
	 * balances outfeed and infeeds with less priority. i.e. newinfeed and amount is added first and then 
	 * newamount is removed from the least important infeed up until capacity is reached
	 * Conveyors carry only one type of product at a time
	 */
/*	@Override
	public synchronized <T extends GeneratableEntity> void addToCurrentInfeed(
			T newInfeed, double newInfeedAmount) {
		// TODO Auto-generated method stub
		super.addToCurrentInfeed(newInfeed, newInfeedAmount);
		int newInfeedIndex = this.getCurrentInfeedList().indexOf(newInfeed);
		int lastInfeedIndex = this.getCurrentInfeedList().size();
		double tempCapacity = this.getCapacityByEntityType((GeneratableEntity) this.getCurrentlyHandlingEntityList().get(0));
		double tempCurrentInfeedAmount = 0;
		// get the currentinfeedAmount from highest priority item and up to the newinfeed
		for (int i = newInfeedIndex; i>=0; i--){
			tempCurrentInfeedAmount += this.getCurrentInfeedRateList().get(i);
		}
		if (Tester.lessCheckTolerance(tempCapacity,tempCurrentInfeedAmount)) {
			throw new ErrorException("Adding %s to %s infeed exceeded its capacity for %s at time: %f", newInfeed.getName(),
					this.getName(),this.getCurrentlyHandlingEntityList().get(0),this.getCurrentTime());
		}
		
		for (int i = newInfeedIndex+1; i<= lastInfeedIndex; i++){
			//TODO
		}
	}
	
	@Override
	public synchronized <T extends GeneratableEntity> void addToCurrentOutfeed(
			T newOutfeed, double newOutfeedAmount) {
		// TODO Auto-generated method stub
		super.addToCurrentOutfeed(newOutfeed, newOutfeedAmount);
	}
*/
}
