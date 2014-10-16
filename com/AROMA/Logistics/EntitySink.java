package com.AROMA.Logistics;

import java.util.ArrayList;


import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.Tester;

public class EntitySink extends BulkHandlingLinkedEntity {
	private static final ArrayList<EntitySink> allInstances;

	// MaxRate defines generation rate
	// Capacity defines the maximum amount of bulk material to be generated
	@Keyword(description = "Consumption interval. IAT is used to regulate the speed of consumption amount removed from the stockpile."
			+ "i.e. amounts are removed in batches corresponding to the amount condumed during the interval. is used to control consumption speed", 
			example = "Pulpmarket ConsumptionIAT { 5 h }"
			+ "Default value is 6 min")
	private final ValueInput consumptionIAT;
	
	private double consumedAmount;
	
	static {
		allInstances = new ArrayList<EntitySink>();
	}

	{
		consumptionIAT = new ValueInput("ConsumptionIAT", "Key Inputs", 0.1d);
		consumptionIAT.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		consumptionIAT.setUnitType(TimeUnit.class);
		this.addInput(consumptionIAT);
	}
	
	public EntitySink() {
		super();

		synchronized (allInstances) {
			allInstances.add(this);
		}
	}
	
	@Override
	public void validate() {
		super.validate();
		if(!(this.getInfeedEntityList().get(0) instanceof Stockpile))
			throw new ErrorException("%s is a sink and should infeed from a stockpile. Recieved %d entity types!", this.getName(), this.getInfeedEntityList().get(0).getName());
	
		if (this.getHandlingEntityTypeList().size() != 1)
			throw new ErrorException("%s is a sink and should handle only one type of entity. Recieved %d entity types!", this.getName(), this.getHandlingEntityTypeList().size());
	
	}
	
	public static ArrayList<? extends EntitySink> getAll() {
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
	// /////////////////////////////////////////////////////////////////////////////
	// MAIN METHODS
	// /////////////////////////////////////////////////////////////////////////////

	public void consume() {

		this.setPresentState("Working"); // Generator starts working
		this.setTriggered(true);
		
		Stockpile stockpile = (Stockpile) this.getInfeedEntityList().get(0);
		BulkMaterial bulkMaterial = (BulkMaterial) this.getHandlingEntityTypeList().get(0);
		
		while (Tester.greaterCheckTolerance(stockpile.getCurrentlyHandlingAmount(bulkMaterial), 0.0d) && 
				Tester.greaterCheckTolerance(this.getCapacity(bulkMaterial),consumedAmount)) {

			double dt = this.getConsumptionIAT()/3600.0d;

			this.scheduleWait(dt); // Wait until the next consumption time
			
			double amount = Tester.min(dt*3600.0d*this.getMaxRate(bulkMaterial),stockpile.getCurrentlyHandlingAmount(bulkMaterial),this.getCapacity(bulkMaterial)-consumedAmount);
			stockpile.removeFromCurrentlyHandlingEntityList(bulkMaterial, amount);
			consumedAmount += amount;				
		}
		
		// Stop working when finished
		this.setPresentState("Idle");
		this.setTriggered(false);
	}

	// ////////////////////////////////////////////////////////////////////////////////////
	// GETTERS AND SETTERS
	// ////////////////////////////////////////////////////////////////////////////////////

	public double getConsumptionIAT() {
		return consumptionIAT.getValue();
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////
	// GRAPHICS
	// ////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void updateGraphics(double simTime) {
		// TODO Auto-generated method stub
		
	}
	
}
