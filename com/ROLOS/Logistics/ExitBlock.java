package com.ROLOS.Logistics;

import java.util.ArrayList;

import com.ROLOS.Economic.Contract;
import com.jaamsim.events.ReflectionTarget;
import com.sandwell.JavaSimulation.Tester;

/**
 * Exitblock is a routeentity that is defined after a main loading facility and governs
 * dispatch or scheduling of moving entities to their destinations. Every exit route from loading bays should lead to a parkblock. Parkblocks may be shared between multiple facilities.
 */
public class ExitBlock extends ParkBlock {
	private static final ArrayList<ExitBlock> allInstances;
	
	private ArrayList<MovingEntity> destinationSchedulingQueue;
	
	private boolean wakeupScheduleList;
	
	static {
		allInstances = new ArrayList<ExitBlock>(1);
	}
	
	{
		
	}
	

	public ExitBlock() {
		super();
				
		synchronized (allInstances) {
			allInstances.add(this);
		}				
		
		destinationSchedulingQueue = new ArrayList<>(1);
	}
	
	@Override
	public void validate() {
		super.validate();
		
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();
		
	}
	
	public static ArrayList<? extends ExitBlock> getAll() {
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

	// ////////////////////////////////////////////////////////////////////////////////////
	// GETTERS AND SETTERS
	// ////////////////////////////////////////////////////////////////////////////////////

		
	@Override
	public <T extends MovingEntity> void finishProcessingEntity(T movingEntity) {
		super.finishProcessingEntity(movingEntity);
		Facility destinationFacility = movingEntity.getDestinationFacility();
		
		if(destinationFacility.getOperatingWindow() != null)
			scheduleForOperatingWindow(movingEntity);
		else
			scheduleForDispatch(movingEntity);
		
	}
	
	/**
	 * schedules dispatch based on dispatch wait time or the next available operating window for the 
	 * destination facility. This method should be called after currentDestination is set for the entity.
	 */
	public void scheduleForOperatingWindow(MovingEntity entity){
		Fleet fleet = entity.getFleet();
		Contract contract = fleet.getScheduledContracts().peek();
		DiscreteHandlingLinkedEntity destination = entity.getCurrentDestination();
		Facility destinationFacility = destination.getFacility();
		double waitTime,operating, estimatedHalfCycle;
		
		if(this.getFacility() == contract.getBuyer())
			estimatedHalfCycle = fleet.getEstimatedBuyerToSupplier()+fleet.getEstimatedCycleAtSupplier();
		else
			estimatedHalfCycle = fleet.getEstimatedSupplierToBuyer()+fleet.getEstimatedCycleAtBuyer();
		
		//first check if operatingWindow for the destination facility is closed or closing
		operating = destinationFacility.getOperatingWindow().getValueForTimeHours(this.getSimTime()/3600.0d);
		if(operating == 1) {
			// checks to see if the moving entity makes it out of the destination facility before the next closed window (assuming correct entry)
			if(Tester.greaterOrEqualCheckTolerance(destinationFacility.getOperatingWindow().getNextChangeTimeAfterHours(this.getSimTime()/3600.0d)*3600.0d - this.getSimTime() ,
					estimatedHalfCycle + fleet.getEarliestNextDispatchTime())){
				this.scheduleForDispatch(entity);
			}else{
				// TODO make sure this works. first finds the next change time and then sets wait time to the change time after minus current time
				waitTime = destinationFacility.getOperatingWindow().getNextChangeTimeAfterHours(this.getSimTime()/3600.0d);
				waitTime = destinationFacility.getOperatingWindow().getNextChangeTimeAfterHours(waitTime)*3600.0d-this.getSimTime();
				this.scheduleProcess(waitTime / 3600.0d, 5, new ReflectionTarget(this, "scheduleForOperatingWindow", entity));
			}			
		} else{
			if(Tester.lessOrEqualCheckTolerance(destinationFacility.getOperatingWindow().getNextChangeTimeAfterHours(this.getSimTime()/3600.0d)*3600.0d - this.getSimTime(),
					estimatedHalfCycle + fleet.getEarliestNextDispatchTime())){
				this.scheduleForDispatch(entity);
			}else{
				waitTime = destinationFacility.getOperatingWindow().getNextChangeTimeAfterHours(this.getSimTime()/3600.0d)*3600.0d-this.getSimTime();
				this.scheduleProcess(waitTime, 5, new ReflectionTarget(this, "scheduleForOperatingWindow", entity));
			}	
		}
	}
	
	/**
	 * schedules dispatch based on dispatch wait time for the fleet. 
	 * This method should be called after currentDestination is set for the entity and if operatingwindow in effect after scheduleoperatingwindow.
	 */
	public void scheduleForDispatch(MovingEntity movingEntity){
		Fleet fleet = movingEntity.getFleet();
		
		if(Tester.greaterOrEqualCheckTolerance(this.getSimTime(),fleet.getEarliestNextDispatchTime())){
			fleet.setLastDispatchTime(this.getSimTime());
			fleet.setEarliestNextDispatchTime(this.getSimTime()+fleet.getDispatchInterval());
			this.dispatch(movingEntity);
		}else{
			movingEntity.setPresentState("Parked for Dispatch");
			double waitTime = (fleet.getEarliestNextDispatchTime() - this.getSimTime());
			fleet.setEarliestNextDispatchTime(fleet.getEarliestNextDispatchTime()+fleet.getDispatchInterval());
			this.scheduleProcess(waitTime/3600.0d, 5, new ReflectionTarget(this, "dispatch", movingEntity));
		}
	}
	
	@Override
	public <T extends LogisticsEntity> void removeFromCurrentlyHandlingEntityList(
			T entityToRemove, double amountToRemove) {
		super.removeFromCurrentlyHandlingEntityList(entityToRemove, amountToRemove);
		
	}
}
