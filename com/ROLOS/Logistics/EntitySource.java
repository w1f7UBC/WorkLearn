package com.ROLOS.Logistics;

import java.util.ArrayList;

import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.InputErrorException;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Tester;

/**
 * Generates a flow of bulkmaterial based on the set rate and adds to the specified stock pile.
 */

public class EntitySource extends BulkHandlingLinkedEntity {

	private static final ArrayList<EntitySource> allInstances;

	// MaxRate defines generation rate
	// Capacity defines the maximum amount of bulk material to be generated
	@Keyword(description = "Generation interval. IAT is used to regulate the speed of generated amount passed to the stockpile."
			+ "i.e. amounts are added in batches corresponding to the amount generated during the interval. is used to control generation speed", 
			example = "Logharvest GenerationIAT { 5 h }"
			+ "Default value is 6 min")
	private final ValueInput generationIAT;
	
	private double generatedAmount;

	static {
		allInstances = new ArrayList<EntitySource>();
	}

	{
		generationIAT = new ValueInput("GenerationIAT", "Key Inputs", 3600.0d);
		generationIAT.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		generationIAT.setUnitType(TimeUnit.class);
		this.addInput(generationIAT);
	}

	public EntitySource() {
		super();

		synchronized (allInstances) {
			allInstances.add(this);
		}

	}

	// TODO complete the validate for EntityGenerator
	@Override
	public void validate() {
		super.validate();

		if(!(this.getOutfeedLinkedEntityList().get(0) instanceof Stockpile))
			throw new InputErrorException("%s is a source and should outfeed to a stockpile. Recieved %d entity types!", this.getName(), this.getOutfeedLinkedEntityList().get(0).getName());
	
		if (this.getHandlingEntityTypeList().size() != 1)
			throw new InputErrorException("%s is a source and should handle only one type of entity. Recieved %d entity types!", this.getName(), this.getHandlingEntityTypeList().size());
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
	}

	public static ArrayList<? extends EntitySource> getAll() {
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
	// WORKING LOGIC METHODS
	// /////////////////////////////////////////////////////////////////////////////

	@Override
	public void startUp() {
		// TODO Auto-generated method stub
		super.startUp();
		// TODO make this talk to the generation manager to figure out the
		// entities to generate
		this.startProcess("generate");
	}

	// /////////////////////////////////////////////////////////////////////////////
	// MAIN METHODS
	// /////////////////////////////////////////////////////////////////////////////

	public void generate() {

		this.setPresentState("Working"); // Generator starts working
		this.setTriggered(true);
		
		Stockpile stockpile = (Stockpile) this.getOutfeedLinkedEntityList().get(0);
		BulkMaterial bulkMaterial = (BulkMaterial) this.getHandlingEntityTypeList().get(0);
		
		while (Tester.greaterCheckTolerance(stockpile.getRemainingCapacity(bulkMaterial), 0.0d)&&
				Tester.greaterCheckTolerance(this.getCapacity(bulkMaterial),generatedAmount)) {

			double dt = this.getGenerationIAT() / 3600.0d; // Set the next generation time

			this.scheduleWait(dt); // Wait until the next generation time
			
			double amount = Tester.min(dt*3600.0d*this.getMaxRate(bulkMaterial),stockpile.getRemainingCapacity(bulkMaterial),this.getCapacity(bulkMaterial)-generatedAmount);
			stockpile.addToCurrentlyHandlingEntityList(bulkMaterial, amount);
			generatedAmount += amount;
						
		}
		
		// Stop working when finished
		this.setPresentState("Idle");
		this.setTriggered(false);
	}

	// ////////////////////////////////////////////////////////////////////////////////////
	// GETTERS AND SETTERS
	// ////////////////////////////////////////////////////////////////////////////////////

	public double getGenerationIAT() {
		return generationIAT.getValue();
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////
	// GRAPHICS
	// ////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void updateGraphics(double simTime) {
		// TODO Auto-generated method stub
		
	}
	
}
