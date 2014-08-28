package com.ROLOS.Logistics;

import java.util.ArrayList;

import com.ROLOS.DMAgents.SimulationManager;
import com.jaamsim.events.ReflectionTarget;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.TimeSeries;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Tester;

/**
 * Generates a flow of bulkmaterial based on the set rate and adds to the specified stock pile.
 */

public class EntitySource extends BulkHandlingLinkedEntity {

	private static final ArrayList<EntitySource> allInstances;
	
	@Keyword(description = "TimeSeries of production levels for the output of this souce (sources only handle one type of product). Units of time series should be in"
			+ "flow unit (Mass or volume flow unit) meaning throughputs are t/h or m3/h.", 
			example = "Temiscaming Throughput { WoodChipeProductionSchedule }")
	private final EntityInput<TimeSeries> throughput;
	
	// MaxRate defines generation rate
	// Capacity defines the maximum amount of bulk material to be generated
	@Keyword(description = "Generation interval. IAT is used to regulate the speed of generated amount passed to the stockpile."
			+ "i.e. amounts are added in batches corresponding to the amount generated during the interval. is used to control generation speed", 
			example = "Logharvest GenerationIAT { 5 h }"
			+ "Default value is 6 min")
	private final ValueInput generationIAT;
	
	private double generatedAmountThisPeriod, generatedAmountToDate;

	static {
		allInstances = new ArrayList<EntitySource>();
	}

	{

		throughput = new EntityInput<>(TimeSeries.class, "Throughput", "Key Inputs", null);
		this.addInput(throughput);
		
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
		this.scheduleProcess(0.0d, 2, new ReflectionTarget(this, "pushSupply",SimulationManager.getPreviousPlanningTime(),this.getSimTime()+SimulationManager.getNextPlanningTime()));
	}

	// /////////////////////////////////////////////////////////////////////////////
	// MAIN METHODS
	// /////////////////////////////////////////////////////////////////////////////
	public void pushSupply(double startTime, double endTime){
		generatedAmountThisPeriod = 0.0d;
		
		Stockpile stockpile = (Stockpile) this.getOutfeedLinkedEntityList().get(0);
		double amount = this.getThroughput(startTime, endTime);
		stockpile.getFacility().getOperationsManager().updateRealizedProduction((BulkMaterial) this.getHandlingEntityTypeList().get(0), amount);
		
		this.scheduleProcess(SimulationManager.getPreviousPlanningTime(), 2, new ReflectionTarget(this, "generate"));

		this.scheduleProcess(SimulationManager.getPlanningHorizon(), 2, new ReflectionTarget(this, "pushSupply",SimulationManager.getPreviousPlanningTime(),SimulationManager.getNextPlanningTime()));

	}
	
	public void generate() {

		this.setPresentState("Working"); // Generator starts working
		this.setTriggered(true);
		
		Stockpile stockpile = (Stockpile) this.getOutfeedLinkedEntityList().get(0);
		BulkMaterial bulkMaterial = (BulkMaterial) this.getHandlingEntityTypeList().get(0);
		double tempAmount = this.getThroughput(this.getSimTime(), SimulationManager.getNextPlanningTime());
		
		while (Tester.greaterCheckTolerance(tempAmount, generatedAmountThisPeriod) &&
				Tester.greaterCheckTolerance(stockpile.getRemainingCapacity(bulkMaterial), 0.0d)&&
				(Tester.greaterCheckTolerance(this.getCapacity(bulkMaterial),generatedAmountToDate))) {

			double dt = this.getGenerationIAT() / 3600.0d; // Set the next generation time

			this.scheduleWait(dt); // Wait until the next generation time
			
			double amount = Tester.min(dt*3600.0d*this.getMaxRate(bulkMaterial),stockpile.getRemainingCapacity(bulkMaterial),
					this.getCapacity(bulkMaterial)-generatedAmountToDate,tempAmount-generatedAmountThisPeriod);
			
			stockpile.addToCurrentlyHandlingEntityList(bulkMaterial, amount);
			generatedAmountThisPeriod += amount;
			generatedAmountToDate += amount;
						
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

	/**
	 * Attention!! Production level should be defined in exact planning periods.
	 * e.g. if planning is 1 year production levels for end of each year should be defined 
	 * until the end of simulation run otherwise will will throw an error or result will be unknown!
	 * @return infinity if throughput not set!
	 * total production level for the passed time slot. Will not interpolate levels if start time or end time are 
	 * fractions of the time defined in the time series. i.e. it will add production levels for the 
	 * starting time one after the start time until the end time. if the start or end times are greater than max value in the 
	 * time series, it'll loop?.
	 */
	public double getThroughput(double startTime, double endTime){

		double timeSlot, currentTime, nextTime;
		currentTime = startTime/3600.0d;
		double tempThroughput = 0.0d;
		
		if(throughput.getValue() == null){
			return Double.POSITIVE_INFINITY;
		}
		
		if(Tester.greaterCheckTimeStep(endTime, throughput.getValue().getMaxTimeValue())){
			
		}		
		
		nextTime = throughput.getValue().getNextChangeTimeAfterHours(currentTime);
		while(Tester.lessCheckTimeStep(currentTime, endTime/3600.0d)){
			timeSlot = Tester.min(nextTime,endTime/3600.0d) - currentTime;
			tempThroughput += throughput.getValue().getValueForTimeHours(currentTime) * timeSlot*3600.0d;
			currentTime = nextTime;
			nextTime = throughput.getValue().getNextChangeTimeAfterHours(currentTime);
		}		
		return tempThroughput;
	}
	// ////////////////////////////////////////////////////////////////////////////////////
	// GRAPHICS
	// ////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void updateGraphics(double simTime) {
		// TODO Auto-generated method stub
		
	}
	
}
