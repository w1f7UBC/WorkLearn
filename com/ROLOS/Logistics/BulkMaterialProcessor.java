package com.ROLOS.Logistics;

import java.util.ArrayList;

import com.ROLOS.DMAgents.SimulationManager;
import com.ROLOS.Economic.Contract;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.units.CostPerMassUnit;
import com.jaamsim.units.CostPerVolumeUnit;
import com.jaamsim.units.MassFlowUnit;
import com.jaamsim.units.MassUnit;
import com.jaamsim.units.RateUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.VolumeFlowUnit;
import com.jaamsim.units.VolumeUnit;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.ErrorException;
import com.jaamsim.input.Input;

import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Tester;
import com.sandwell.JavaSimulation.TimeSeries;

/**
 * Processors can load material on to stock piles or bulkcargo directly. such as a wood chipper that blows chips onto
 * a stock pile or a reactor that unloads liquid onto a tank. 
 * A continuous processor receives handlingEntityTypes at the infeed rate and oufeeds outfeedEntityTypes at the outfeed rate.
 * Processors that load onto a bulk cargo right now process one handlingEntityType to one or multiple outfeed entity types. conversion rate is calculated based on
 * outfeed/infeed rate.
 * Batch processors (processors that infeed from stockpile and outfeed to stock piles) assume that max rate by entity type defined in the 
 * bulkhandling linked entity's are all infeed rates 
 */
public class BulkMaterialProcessor extends BulkHandlingLinkedEntity {

	private static final ArrayList<BulkMaterialProcessor> allInstances;
	
	@Keyword(description = "Amount of time it'll take the processor to process infeeds into outfeeds.", 
				example = "DrumDryer ResistanceTime { 2 hr }")
	private final ValueInput resistanceTime;
			
	@Keyword(description = "The bulk material entity types that this procssor converts the received bulk material into. for example" +
			"a woodchipper receives (Handles) WoodResidue BulkMaterial and outfeeds SawDust BulkMaterial.", 
			example = "WoodChipper OutfeedEntity { Sawdust }")
	private final EntityListInput<BulkMaterial> outfeedEntityTypeList;
	
	@Keyword(description = "the outfeed rate for the outfed entity types. Can thought of as having an unloader for each outfed entity with the specified rate. " +
			"Conversion rate for each entity type is calculated using outfeedrate/infeedrate formula."
			+ "For each of the entities apearing in outfeedEntityTypeList a respective rate should be defined.", 
			example = "WoodChipper OutfeedRate { 2 t/h } ")
	private final ValueListInput outfeedRateByEntityType;
	
	@Keyword(description = "The bulk material (end product or feedstock) whose demand/supply level"
			+ "drives the production level. i.e. the main production of the process "
			+ "and every feedstock requirement and all production levels of other byproducts are set based on the demand"
			+ "or supply level of this material. The proportions for other feedstocks/products are set to the process proportions for this." +
			"Facilities may produce multiple products. the list provided here are material types that can be produced.", 
			example = "Sawmill-1 PrimaryProduct { Lumber }")
	private final EntityInput<BulkMaterial> primaryProduct;
	
	@Keyword(description = "Maximum production (feedstock handling) capacity for the productionDrivingMaterial. production per hour."
			+ "capacities for all other products (feedstock and end products) will be set proportional to the main process of the facility.", 
		example = "Temiscaming ProductionCapacity { 300 kt/h }")
	private final ValueInput mainProductCapacity;
	
	@Keyword(description = "TimeSeries of production levels for the productionDrivingMaterial. Units of time series should be in"
			+ "flow unit (Mass or volume flow unit) meaning throughputs are t/h or m3/h.", 
			example = "Temiscaming Throughput { WoodChipeProductionSchedule }")
	private final EntityInput<TimeSeries> mainProductThroughput;
	
	@Keyword(description = "the operating cost per tonne of material output.", 
			example = "WoodChipper OperatingCost { 2 $/t } ")
	private final ValueInput operatingCost;
	
	private ProcessingRoute processingRoute;
	
	static {
		allInstances = new ArrayList<BulkMaterialProcessor>(10);
	}
	
	{
		resistanceTime = new ValueInput("ResistanceTime", "Key Inputs", 0.0d);
		this.addInput(resistanceTime);
		resistanceTime.setUnitType(TimeUnit.class);
		
		outfeedEntityTypeList = new EntityListInput<>(BulkMaterial.class, "OutfeedEntity", "Key Inputs", null);
		this.addInput(outfeedEntityTypeList);
		
		outfeedRateByEntityType = new ValueListInput("OutfeedRate", "Key Inputs", null);
		this.addInput(outfeedRateByEntityType);
		
		operatingCost = new ValueInput("OperatingCost", "Economic", null);
		this.addInput(operatingCost);

		primaryProduct = new EntityInput<>(BulkMaterial.class, "PrimaryProduct", "Key Inputs", null);
		this.addInput(primaryProduct);
		
		mainProductCapacity = new ValueInput("ProductionCapacity", "Key Inputs", Double.POSITIVE_INFINITY);
		this.addInput(mainProductCapacity);
		
		mainProductThroughput = new EntityInput<>(TimeSeries.class, "Throughput", "Key Inputs", null);
		this.addInput(mainProductThroughput);
		
	}
	
	public BulkMaterialProcessor() {
	}

	@Override
	public void validate() {
		super.validate();
		
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();
		
		// TODO refactor for when processable from multiple products and paths is figured out 
		for(BulkMaterial eachOutfeed: outfeedEntityTypeList.getValue())
			for(LogisticsEntity eachInfeed: this.getHandlingEntityTypeList())
				eachOutfeed.setProcessableFrom((BulkMaterial) eachInfeed);
	}
	
	public static ArrayList<? extends BulkMaterialProcessor> getAll() {
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

	/**
	 * This assumes processor can only be used for loading a cargo (unloading can't outfeed from processor)
	 * TODO unlike loaders processors are not attached to bulk cargo and if a moving entity towed a
	 * processor, it's assumed that it can be shared 
	 */
	@Override
	public void attachToTow(MovingEntity towingMovingEntity) {
		super.attachToTow(towingMovingEntity);
		
		for(LogisticsEntity each: this.getHandlingEntityTypeList())
			if(towingMovingEntity.getAcceptingBulkMaterialList().getValueFor((BulkMaterial) each, 3) != 0)
				towingMovingEntity.getAcceptingBulkMaterialList().setMin((BulkMaterial) each,3,this.getMaxRate((BulkMaterial) each)); 
	}
	
	/**
	 * Processing for when processor directly loads onto a bulk cargo (infeeds and outfeeds only one material type)
	 */
	public void doProcessing(Contract contract, Stockpile sourceStockpile, BulkMaterialStorage sinkBulkCargo, double infeedRate, double outfeedRate, BulkMaterial infeedBulkMaterial, BulkMaterial outfeedBulkMaterial){
		
		//TODO this currently outfeeds the first defined outfeed entity only. refactor to loop over all outfeeds with the possibility to outfeed to different destinations		
		double  dt, conversionRate, tempAmount;
			
		conversionRate = outfeedRate/infeedRate;
			// start loading the sinkLinkedEntity
			while (Tester.greaterCheckTolerance(sinkBulkCargo.getRemainingCapacity(outfeedBulkMaterial), 0.0d)) {
				//TODO refactor to include volume conversion when using volume
				tempAmount = Tester.min(sinkBulkCargo.getRemainingCapacity(outfeedBulkMaterial), sourceStockpile.getCurrentlyHandlingAmount()*conversionRate);
				dt = tempAmount/outfeedRate;
				sourceStockpile.setLastContentUpdateTime(this.getSimTime());
				sinkBulkCargo.setLastContentUpdateTime(this.getSimTime());
				this.simWait(dt);
				sinkBulkCargo.addToCurrentlyHandlingEntityList(outfeedBulkMaterial, tempAmount);
				sourceStockpile.removeFromCurrentlyHandlingEntityList(infeedBulkMaterial,tempAmount/conversionRate);
				// add to fulfilled supply and demand
				Facility supplyCompany = sourceStockpile.getFacility();
				Facility demandCompany = sinkBulkCargo.getFacility();
				if (supplyCompany != demandCompany){
					if(contract.getProduct() == outfeedBulkMaterial)
						contract.addToFulfilledAmount(tempAmount);
					else
						contract.addToFulfilledAmount(tempAmount/conversionRate);
				}
				if(Tester.equalCheckTolerance(sourceStockpile.getCurrentlyHandlingAmount(),0.0d)){
					break;
				}
			}
		}
	/**
	 * Processing for when processor infeeds from infeed stockpiles and outfeeds to outfeedstockpiles.
	 * This method assumes that infeed piles and outfeed piles are correct (have expected material -based on 
	 * handling entities listed in linked entity's handling entity type.)
	 * If capacity is defined for the processor as in batch processors, there should be a capacity defined for each handling entity type  
	 */
	public void doProcessing(ProcessingRoute processingRoute){
		ArrayList<Stockpile> infeedPiles = processingRoute.getInfeedPiles();
		ArrayList<Stockpile> outfeedPiles = processingRoute.getOutfeedPiles();
		BulkMaterial tempMaterial = null;
		double  dt, conversionRate, tempAmount;
		// While infeed stock piles aren't dried out process
		while(processingRoute.isConnected()){
			processingRoute.setActive(true);
			dt = SimulationManager.getTimeStep();
			for(Stockpile each:infeedPiles){
				tempMaterial = (BulkMaterial) each.getCurrentlyHandlingList().getEntityList().get(0);
				tempAmount = each.getCurrentlyHandlingAmount();
				dt = Tester.min(dt,tempAmount/this.getMaxRate(tempMaterial)); 
			}
			
			for(Stockpile each: outfeedPiles){
				tempMaterial = (BulkMaterial) each.getHandlingEntityTypeList().get(0);
				tempAmount = this.getOutfeedRate(tempMaterial) * dt;
				tempAmount = Tester.min(tempAmount, each.getRemainingCapacity(tempMaterial));
				dt = Tester.min(dt,tempAmount / this.getOutfeedRate(tempMaterial));
			}
			
			//remove material from infeed piles
			for(Stockpile each:infeedPiles){
				tempMaterial = (BulkMaterial) each.getCurrentlyHandlingList().getEntityList().get(0);
				each.removeFromCurrentlyHandlingEntityList(tempMaterial, this.getMaxRate() * dt);
				each.setLastContentUpdateTime(this.getSimTime());
			}
			// wait for dt and resistance time
			//TODO if resistance time is not zero separately connect infeed and outfeed and wait for material to be loaded
			this.simWait(dt + resistanceTime.getValue());
			
			// add outfeed material to outfeed piles
			for(Stockpile each:outfeedPiles){
				tempMaterial = (BulkMaterial) each.getHandlingEntityTypeList().get(0);
				each.addToCurrentlyHandlingEntityList(tempMaterial, this.getOutfeedRate(tempMaterial) * dt);
				each.setLastContentUpdateTime(this.getSimTime());
			}
		}	
		processingRoute.setActive(false);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// SETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	

	//////////////////////////////////////////////////////////////////////////////////////////////////////
	//REMOVER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	//GETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public LoadingBay getCurrentLoadingArea() {
		return (LoadingBay) this.getCurrentlHandlersList().get(0);
	}
		
	public ArrayList<BulkMaterial> getOutfeedEntityTypeList(){
		return outfeedEntityTypeList.getValue();
	}
	
	public double getOutfeedRate(BulkMaterial bulkMaterial){
		return outfeedRateByEntityType.getValue().get(outfeedEntityTypeList.getValue().indexOf(bulkMaterial));
	}
	
	public double getConverstionRate(BulkMaterial bulkMaterial){
		return this.getOutfeedRate(bulkMaterial)/this.getMaxRate();
	}
	
	public ProcessingRoute getProcessingRoute(){
		return processingRoute;
	}
	
	@Override
	public <T extends LogisticsEntity> double getVariableCost(T... args) {
		
		return operatingCost.getValue();	
	}

	public BulkMaterial getPrimaryProduct(){
		return primaryProduct.getValue();
	}

	public Double getPrimaryProductCapacity(){
		return mainProductCapacity.getValue();
	}

	/**
	 * Attention!! Production level should be defined in exact planning periods.
	 * e.g. if planning is 1 year production levels for end of each year should be defined 
	 * until the end of simulation run otherwise will will throw an error or result will be unknown!
	 * @return total production level for the passed time slot. Will not interpolate levels if start time or end time are 
	 * fractions of the time defined in the time series. i.e. it will add production levels for the 
	 * starting time one after the start time until the end time.
	 */
	public double getThroughput(double startTime, double endTime){
		if(Tester.greaterCheckTimeStep(endTime, mainProductThroughput.getValue().getMaxTimeValue()))
			throw new ErrorException("the production time series defined for %s in facility %s includes production levels until"
					+ "%f. Try was made to check production level until %f!", 
					primaryProduct.getValue().getName(), this.getName(), mainProductThroughput.getValue().getMaxTimeValue(),endTime);
		double timeSlot, currentTime, nextTime;
		currentTime = startTime/3600.0d;
		double throughput = 0.0d;
		nextTime = mainProductThroughput.getValue().getNextChangeTimeAfterHours(currentTime);
		while(Tester.lessCheckTimeStep(currentTime, endTime/3600.0d)){
			timeSlot = Tester.min(nextTime,endTime/3600.0d) - currentTime;
			throughput += mainProductThroughput.getValue().getValueForTimeHours(currentTime) * timeSlot*3600.0d;
			currentTime = nextTime;
			nextTime = mainProductThroughput.getValue().getNextChangeTimeAfterHours(currentTime);
		}		
		return throughput;
	}
	
	public void setProcessingRoute(ProcessingRoute processingRoute){
		this.processingRoute = processingRoute;
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
		stateReportFile.putStringTabs("Head Route", 1);

		// print units
		this.printStatesUnitsHeader();	
		stateReportFile.newLine(2);
		stateReportFile.flush();	
	
	}	

	@Override
	public void updateForInput(Input<?> in) {
		// TODO Auto-generated method stub
		super.updateForInput(in);
		if (in == outfeedEntityTypeList){
			if(outfeedEntityTypeList.getValue().get(0).getEntityUnit().equals(VolumeUnit.class)){
				outfeedRateByEntityType.setUnitType(VolumeFlowUnit.class);
				operatingCost.setUnitType(CostPerVolumeUnit.class);
			}
			else if(outfeedEntityTypeList.getValue().get(0).getEntityUnit().equals(MassUnit.class)){
				outfeedRateByEntityType.setUnitType(MassFlowUnit.class);
				operatingCost.setUnitType(CostPerMassUnit.class);
			}
			else
				outfeedRateByEntityType.setUnitType(RateUnit.class);
		}

		if(in == primaryProduct){
			if(primaryProduct.getValue().getEntityUnit() == MassUnit.class){
				mainProductCapacity.setUnitType(MassFlowUnit.class);
			}
			else{
				mainProductCapacity.setUnitType(VolumeFlowUnit.class);
			}
		}
	}
}
