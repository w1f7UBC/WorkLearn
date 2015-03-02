package com.AROMA.Logistics;

import java.util.ArrayList;

import com.AROMA.AROMAEntity;
import com.AROMA.Units.CostPerEnergyUnit;
import com.AROMA.Units.CostPerMassUnit;
import com.AROMA.Units.CostPerVolumeUnit;
import com.AROMA.Utils.TwoLinkedLists;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.EnumInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.units.CostRateUnit;
import com.jaamsim.units.CostUnit;
import com.jaamsim.units.EnergyUnit;
import com.jaamsim.units.MassFlowUnit;
import com.jaamsim.units.MassUnit;
import com.jaamsim.units.PowerUnit;
import com.jaamsim.units.RateUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.VolumeFlowUnit;
import com.jaamsim.units.VolumeUnit;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.TimeSeries;

/* Superclass between ModelEntity and DisplayEntity, kept in MiNaReLS to avoid overwriting due to Ausenco's updates
 * 
 */

public class LogisticsEntity extends ReportableEntity {
	
	public enum Entity_State {
		SOLID_DISCRETE (false, null, RateUnit.class,CostUnit.class),
		SOLID_BULK_massbased (true,MassUnit.class,MassFlowUnit.class,CostPerMassUnit.class),
		SOLID_BULK_volumetric (true,VolumeUnit.class,VolumeFlowUnit.class,CostPerVolumeUnit.class),
		LIQUID (true, VolumeUnit.class,VolumeFlowUnit.class,CostPerVolumeUnit.class),
		GAS (true, VolumeUnit.class,VolumeFlowUnit.class,CostPerVolumeUnit.class),
		ENERGY (true,EnergyUnit.class, PowerUnit.class,CostPerEnergyUnit.class);
		
		private final boolean isBulk; 						//whether entity is a bulk entity
		private final Class<? extends Unit> unit;			//entity's  unit of measurement
		private final Class<? extends Unit> flowUnit; 		//entity's flow unit of measurement
		private final Class<? extends Unit> costUnit; 		//entity's cost unit of measurement
		private final String unitString;
		private final String unitFlowString;
		private final String costUnitString;
		
		Entity_State (boolean isBulk, Class<? extends Unit> unit, Class<? extends Unit> flowUnit, Class<? extends Unit> costUnit) {
			this.isBulk = isBulk;
			this.unit = unit;
			this.flowUnit = flowUnit;
			this.costUnit = costUnit;
			if(unit == null){
				unitString = "";
				unitFlowString = "/s";
				costUnitString = "$";
			}else if (unit.getSimpleName().equals("MassUnit")){
				unitString = "kg";
				unitFlowString = "kg/s";
				costUnitString = "$/kg";
			} else if (unit.getSimpleName().equals("VolumeUnit")){
				unitString = "m3";
				unitFlowString = "m3/s";
				costUnitString = "$/m3";
			} else {
				unitString = "J";
				unitFlowString = "J/s";
				costUnitString = "$/J";
			}
		}
	}
	
	@Keyword(description = "The state of matter for this entity. By default entities are created SOLID_DISCRETE.", 
			example = "WoodChips MatterState { SOLID_BULK }")
	private final EnumInput<Entity_State> entityMatterState;
	
	@Keyword(description = "The facility that this entity belongs to", 
			example = "BulkCargo1 OwningFacility { HSPP }")
	private final EntityInput<Facility> facility;
	
	@Keyword(description = "The entity which resembles in properties. for example, similar trucks"
			+ "that can be used in place of each other in fleets. It is generally advisable that fleets and etc use a generator but if fixed "
			+ "moving enetities are used they should resemble to a proto type so that the dynamic fleet logic would work. This basically sets"
			+ "the parent entity of this entity to the proto type eventhough this was not a generated entity.", 
			example = "Truck1 ProtoType { Truck }")
	private final EntityInput<LogisticsEntity> protoType;
	
	@Keyword(description = "Time sluts when this logistics entity works. It is used for facilities for example"
			+ "every time a parkblock wants to dispatch a truck to this facility, the "
			+ "operating window will be checked and if not ready it'll park the truck. values for the time series should be either 0 or 1 corresponding"
			+ "to non-operating and operating respectively. default for cutblocks is operational.",
	         example = "EM101Cutblock  OperatingWindow { EM101OperatingWindow }")
	private final EntityInput<TimeSeries> operatingWindow;				
	
	//TODO move fixed cost and variable cost to LogisticsEntity level giving ability to define
	//that for a range of different items such as time spent at each state, activity level, ...
	// TODO define fixed cost items such separately
	@Keyword(description = "Fixed costs (capital,...) of this logistics entity."
			+ "For the owning facility, all the fixed costs of the owned entities will be added.",
	         example = "Temiscaming  FixedCost { 1000000 $ }")
	private final ValueInput fixedCost;
	
	@Keyword(description = "the operating cost rate (time-based).", 
			example = "WoodChipper OperatingCost { 2 $/h } ")
	private final ValueInput operatingCost;
	
	@Keyword(description = "Lifespan of this logistics entity - used for calculating fixed cost contribution per planning horizon."
			+ "Later to be used for depreciation calculation/replacement decisions",
	         example = "Temiscaming  LifeSpan { 25 yr }")
	private final ValueInput lifeSpan;
	
	private TwoLinkedLists<LogisticsEntity> currentHandlersList;
	private LogisticsEntity generationSource;
	private LogisticsEntity protoTypeEntity;								// this is the parent entity for generated entities or and entity which resembles in properties to this entity
	private boolean usedForGeneration;									// whether this entity is generated by an entity generator or not defaul is false;
	
	{
		entityMatterState = new EnumInput<Entity_State>(Entity_State.class, "MatterState", "Key Inputs", Entity_State.SOLID_DISCRETE);
		this.addInput(entityMatterState);	

		facility = new EntityInput<Facility>(Facility.class, "Facility", "Key Inputs", null);
		this.addInput(facility);
		
		operatingWindow = new EntityInput<TimeSeries>(TimeSeries.class, "OperatingWindow", "Key Inputs", null);
		this.addInput(operatingWindow);
	
		protoType = new EntityInput<LogisticsEntity>(LogisticsEntity.class, "ProtoType", "Key Inputs", this);
		this.addInput(protoType);

		fixedCost = new ValueInput("FixedCost", "Economic", 0.0d);
		fixedCost.setUnitType(CostUnit.class);
		this.addInput(fixedCost);

		operatingCost = new ValueInput("OperatingCost", "Economic", 0.0d);
		operatingCost.setUnitType(CostRateUnit.class);
		this.addInput(operatingCost);

		lifeSpan = new ValueInput("LifeSpan", "Economic", Double.POSITIVE_INFINITY);
		lifeSpan.setUnitType(TimeUnit.class);
		this.addInput(lifeSpan);
		
	}
		
	public LogisticsEntity() {
		super();
		currentHandlersList = new TwoLinkedLists<>(1, new DescendingPriotityComparator<LogisticsEntity>(AROMAEntity.class, "getInternalPriority"));
	}
	
	@Override
	public void validate() {
		super.validate();
		if(facility.getValue() != null)
			facility.getValue().addToInsideFacilityLimits(this);
		
		this.setProtoTypeEntity(protoType.getValue());
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();	
	}	
	
	////////////////////////////////////////////////////////////////////////
	// Setting Entity's state
	////////////////////////////////////////////////////////////////////////
	
	public Entity_State getEntityMatterState() {
		return entityMatterState.getValue();
	}
	
	public boolean isOperating (){
		if (operatingWindow.getValue() != null)
			return operatingWindow.getValue().getNextSample(getSimTime())==1 ? true : false;
		else
			return true;
	}
	
	public boolean isBulk() {
		return entityMatterState.getValue().isBulk;
	}
	
	public TimeSeries getOperatingWindow(){
		return operatingWindow.getValue();
	}
	
	/**
	 * TODO this returns 100% available right now. refactor when Model entity and this class are pulled together and breakdown and maintenance are figured out
	 * @return net average available hours percentage of total time for the passed period. i.e. this is the total time from operating window
	 * minus average maintenance and breakdown time in during that time.
	 */
	public double getNetAverageAvailability(double startTime,double endTime){
		return 1.0d;
	}
	
	public Facility getFacility(){
		return facility.getValue();
	}
	
	public Class<? extends Unit> getEntityUnit() {
		return entityMatterState.getValue().unit;
	}
	
	public String getEntityUnitString(){
		return entityMatterState.getValue().unitString;
	}
	
	public String getEntityFlowUnitString(){
		return entityMatterState.getValue().unitFlowString;
	}
	
	public Class<? extends Unit> getEntityFlowUnit() {
		return entityMatterState.getValue().flowUnit;
	}
	
	public Class<? extends Unit> getEntityCostUnit() {
		return entityMatterState.getValue().costUnit;
	}
	
	public String getEntityCostUnitString(){
		return entityMatterState.getValue().costUnitString;
	}

	@SuppressWarnings("unchecked")
	public <T extends LogisticsEntity> T getGenerationSource() {
		return (T) generationSource;
	}

	public <T extends LogisticsEntity> void setGenerationSource(T generationSource) {
		this.generationSource = generationSource;
	}

	public ArrayList<? extends LogisticsEntity> getCurrentlHandlersList() {
		return currentHandlersList.getEntityList();
	}
	
	
	public <T extends LinkedEntity> void addToCurrentHandlersList(T currentlyHandledBy, double amountToAdd) {
		currentHandlersList.add(currentlyHandledBy, 0, amountToAdd);
	}

	public <T extends LinkedEntity> void removeFromCurrentHandlersList(T currentlyHandledBy, double amountToRemove) {
		currentHandlersList.remove(currentlyHandledBy, 0, amountToRemove);
	}

	/**
	 * @return returns the entity itself if parent entity is null
	 */
	public LogisticsEntity getProtoTypeEntity() {
		return protoTypeEntity == null ? this : protoTypeEntity;
	}
	
	public boolean isUsedForGeneration(){
		return usedForGeneration;
	}

	public void setProtoTypeEntity(LogisticsEntity parrentEntity) {
		this.protoTypeEntity = parrentEntity;
	}
	
	public void setUsedForGeneration(){
		this.usedForGeneration = true;
	}
	

	/**
	 * @return total fixed cost
	 */
	public double getFixedCost(){
		return fixedCost.getValue();
	}
	
	public double getOperatingCost(){
		return operatingCost.getValue();
	}
	
	/**
	 * @return fixed cost for the passed time span. simply devides the fixed cost by the life span and 
	 * calculates the projected cost in the period. assumes fixed cost is only distributed over the life span
	 * TODO refactor when engineering economics calculations are figured out
	 */
	public double getFixedCost(double startTime, double endTime){
		return fixedCost.getValue()*((endTime-startTime)/lifeSpan.getValue());
	}
	
	/**
	 * May be overwritten for when args are defined
	 */
	public <T extends LogisticsEntity> double getVariableCost(T ...args) {
		return 0;
	}
	
	public double getLifeSpan() {
		return lifeSpan.getValue();
	}
	
	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);
		if(in == entityMatterState){
			if(this instanceof BulkMaterial)
				try{
					((BulkMaterial)this).setInputUnits();
				}catch (ClassCastException e){}
		}	
				
	}
}
