package com.ROLOS.Logistics;

import java.util.ArrayList;

import com.ROLOS.DMAgents.TrafficController;

import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.ReflectionTarget;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.Entity;

import com.jaamsim.input.Keyword;

/**
 * EntityGenerator creates a sequence Entities (extends DisplayEntity) at random
 * or constant intervals and number of entities per creation, or based on a file
 * schedule, and up to a maximum number (if defined) which are placed in a
 * target Queue.
 */

public class EntityGenerator extends DiscreteHandlingLinkedEntity {

	private static final ArrayList<EntityGenerator> allInstances;

	/*
	 * @Keyword(desc =
	 * "Probability Distribution for number of entities generated at each interval for discrete entities or FlowRate for bulk materials."
	 * , example = "ShipGenerator1 GenerationRateDistribution { GenDist1 }")
	 * private final EntityInput<ProbabilityDistribution> generationRateDist;
	 */
	
	@Keyword(description = "Generation rate (number of entities generated at each IAT) for discrete entities .", 
			example = "ShipGenerator1 GenerationRate { 5 h }. Default value is 1")
	private final IntegerInput generationRate;
	
	@Keyword(description = "Generation interval for discrete entities (bulk materials will not use this as their production is continuous).", example = "ShipGenerator1 GenerationIAT { 5 h }"
			+ "Default value is 0")
	private final ValueInput generationIAT;
	/*
	 * @Keyword(description =
	 * "Probability Distribution for generation interval for discrete entities (bulk materials will not use have this as their production is continuous)."
	 * , example = "ShipGenerator1 GenerationIntervalDist { GenIntDist1 }")
	 * private final EntityInput<ProbabilityDistribution> generationIntervalDist;
	 * 
	 * @Keyword(description = "Schedule file defines generation intervals from the start of simulation "
	 * + " (first column) and the generation rate (second column) for discrete entities or "
	 * + "FlowRate for bulk materials (for bulk materials, flow rate remains the same until the next time)."
	 * , example = "ShipGenerator1 GenerationScheduleFile { '..\\IncrementFiles\\ShipGenSchedule.txt' }")
	 * private final FileInput generationScheduleFile;
	 */

	double generatedAmount;

	static {
		allInstances = new ArrayList<EntityGenerator>();
	}

	{
		generationRate = new IntegerInput("GenerationRate", "Key Inputs", 1);
		generationRate.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(generationRate);
		
		generationIAT = new ValueInput("GenerationIAT", "Key Inputs", 0.0d);
		generationIAT.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		generationIAT.setUnitType(TimeUnit.class);
		this.addInput(generationIAT);

	}

	public EntityGenerator() {
		super();

		synchronized (allInstances) {
			allInstances.add(this);
		}

	}

	// TODO complete the validate for EntityGenerator
	@Override
	public void validate() {
		// TODO Auto-generated method stub
		super.validate();
		
		for (LogisticsEntity each: this.getHandlingEntityTypeList()){
			each.setUsedForGeneration();
		}

		if (this.getHandlingEntityTypeList().size() != 1)
			throw new ErrorException("%s is a generator and should have handle only one type of entity. Recieved %d entity types!", this.getName(), this.getHandlingEntityTypeList().size());

	}

	@Override
	public void earlyInit() {
		// TODO Auto-generated method stub
		super.earlyInit();
	}

	public static ArrayList<? extends EntityGenerator> getAll() {
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

	// /////////////////////////////////////////////////////////////////////////////
	// WORKING LOGIC METHODS
	// /////////////////////////////////////////////////////////////////////////////

	@Override
	public void startUp() {
		// TODO Auto-generated method stub
		super.startUp();
		// TODO make this talk to the generation manager to figure out the
		// entities to generate
		this.startProcess(new ReflectionTarget(this,"generateDiscreteEntity", this.getHandlingEntityTypeList().get(0)));
	}

	// /////////////////////////////////////////////////////////////////////////////
	// MAIN METHODS
	// /////////////////////////////////////////////////////////////////////////////

	public static <T extends LogisticsEntity> T generateEntity(T entityToGenerate, boolean flagGenerated) {

		T protoType = (T) entityToGenerate.getProtoTypeEntity();
		T ent = (T) InputAgent.defineEntityWithUniqueName(protoType.getClass(), protoType.getName(), false);
		ent.setProtoTypeEntity(protoType);
		
		ent.setFlag(Entity.FLAG_ADDED);
		
		ent.copyInputs(protoType);
		
		if (flagGenerated)
			ent.setFlag(Entity.FLAG_GENERATED);
		
		return ent;
	}
	
	@Override
	//TODO right now assumes that entity generators have one destination only! 
	public <T extends MovingEntity> void finishProcessingEntity(T entity) {
		// TODO Auto-generated method stub
		super.finishProcessingEntity(entity);
		// get that one destination!
		LinkedEntity destination = this.getDestinationsList().get(0);
		TrafficController.trafficControlManager.planNextMove(entity);
		entity.setStartTravelingTimeOnCurrentRoute(this.getSimTime());
	}

	// ////////////////////////////////////////////////////////////////////////////////////
	// GETTERS AND SETTERS
	// ////////////////////////////////////////////////////////////////////////////////////

	@Override
	public <T extends LogisticsEntity> double getRemainingCapacity(T ent) {
		double tempRemainingCapacityByType = this.getCapacity(ent) - this.getEntityAmountProcessed(ent);
		if (tempRemainingCapacityByType < 0) {
			throw new ErrorException("Found negative remaining capacity in %s at time: %f", this.getName(),this.getCurrentTime());
		}
		return tempRemainingCapacityByType;
	}
	
	//TODO modify when dist and file are added
	public int getGenerationRate(){
		return generationRate.getValue();
	}
	
	//TODO modify when dist and file are added
	public double getGenerationIAT() {
		return generationIAT.getValue();
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////
	// GRAPHICS
	// ////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void updateGraphics(double simTime) {
		// TODO Auto-generated method stub
		//place generated entities at the centre of the generator
		if (!this.getCurrentlyHandlingList().isEmpty()) {
			Vec3d generatorCentre = this.getPositionForAlignment(new Vec3d());
			for (LogisticsEntity each : this.getCurrentlyHandlingList().getEntityList()) {
				each.setPosition(generatorCentre);
				each.setAlignment(new Vec3d());
				each.setOrientation(this.getOrientation());
			}
		}
	}
}
