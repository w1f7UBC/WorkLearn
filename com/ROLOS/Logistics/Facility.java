package com.ROLOS.Logistics;

import java.util.ArrayList;

import worldwind.DefinedShapeAttributes;

import com.ROLOS.ROLOSEntity;
import com.ROLOS.DMAgents.FacilityFinancialManager;
import com.ROLOS.DMAgents.FacilityGeneralManager;
import com.ROLOS.DMAgents.FacilityOperationsManager;
import com.ROLOS.DMAgents.FacilityTransportationManager;
import com.ROLOS.Economic.Contract;
import com.ROLOS.Utils.HashMapList;
import com.ROLOS.Utils.TwoLinkedLists;
import com.jaamsim.events.ReflectionTarget;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.MassFlowUnit;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.FileEntity;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Color4d;
import com.sandwell.JavaSimulation.Tester;

/**
 * Facilities always have entrance and exit blocks. those can be thought of as parking blocks from which
 * next available route or loading bay is planned.  
 */
public class Facility extends DiscreteHandlingLinkedEntity {

	private static final ArrayList<Facility> allInstances;
	
	//TODO poor implementation. find a more consistent way for maping objects coloring scheme
	// private static final HashMapList<Color4d, Facility> colorScheme;
	
	@Keyword(description = "If TRUE, then statistics for all stocks will be printed for this facility"
			+ "everytime something is added or removed.",
     example = "Temiscaming PrintStocksReport { TRUE }")
	private final BooleanInput printStocksReport;

	@Keyword(description = "This shows whether this is where the cycle statistics of the moving entity should be gathered and reset." +
			"usually it is better to pick an end point where entities always endup there. default is false", 
			example = "Silverdale StartOfCycle { TRUE }")
	private final BooleanInput cycleStart;
		
	//TODO change to IRR when engineering economics calculations are implemented
	@Keyword(description = "Profit margin for this facility. (Revenue-cost)/Revenue",
	         example = "Temiscaming  ProfitMargin { 0.12 }")
	private final ValueInput profitMargin;
	
	// If facilities are not able to technically produce output products (feedstock piles ran out of content, output piles ran out of content and processing capability is lost).
	private boolean dormant;											
		
	// Report files
	protected FileEntity stocksReportFile;       

	private final HashMapList<String,LogisticsEntity> insideFacilityLimits;
	
	//TODO stocks list adjustment for multiple processing/loading routes are unsafe! possibly move stockslist to processing/loading routes 
	private TwoLinkedLists<BulkMaterial> stocksList;
	
	// Managers list
	private FacilityGeneralManager generalManager;
	private FacilityFinancialManager financialManager;	
	private FacilityOperationsManager operationsManager;
	private FacilityTransportationManager transportationManager;
	
	private boolean managersInitialized;
	
	static {
		allInstances = new ArrayList<Facility>(2);
	//	colorScheme = new HashMapList<Color4d, Facility>();
	}
	
	{
		printStocksReport = new BooleanInput("PrintStocksReport", "Report", false);
		this.addInput(printStocksReport);
		
		cycleStart = new BooleanInput("StartOfCycle", "Key Inputs", false);
		this.addInput(cycleStart);
		
		profitMargin = new ValueInput("ProfitMargin", "Economic", 0.0d);
		this.addInput(profitMargin);
		
	}
	
	public Facility() {
		managersInitialized = false;
		dormant = false;
		insideFacilityLimits = new HashMapList<String,LogisticsEntity>(5);
		new TwoLinkedLists<>(4, new DescendingPriotityComparator<BulkMaterial>(ROLOSEntity.class, "getInternalPriority"),0);
		
		stocksList = new TwoLinkedLists<>(17, new DescendingPriotityComparator<BulkMaterial>(ROLOSEntity.class, "getInternalPriority"));
		
		synchronized (allInstances) {
			allInstances.add(this);
		}
		
	}
	
	public static ArrayList<? extends Facility> getAll() {
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
	
	@Override
	public void validate() {
		super.validate();
			
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();
		// initiate file if print report is true
		if(printStocksReport.getValue()){
			stocksReportFile = ReportAgent.initializeFile(this,".stc");
			this.printStocksReportHeader();
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// ASSERTION METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////

	public boolean isCycleStart(){
		return cycleStart.getValue();
	}
	
	public boolean isDormant(){
		return dormant;
	}
		
	/**
	 * This method is called by stockpiles. When each stockpile runs out of content, it calls this method to set the loading area exhausted if 
	 * all stock piles are empty.
	 */
	public void checkIfDormant(BulkMaterial bulkMaterial){
		
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// ADDER and SETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////
			
	public void setDormant(boolean dormant){
		this.dormant = dormant;
		this.setPresentState("Dormant");	
	}
	
	public <T extends LogisticsEntity> void addToInsideFacilityLimits(T entityToAdd){
		insideFacilityLimits.add(entityToAdd.getClass().getSimpleName(), entityToAdd);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// GETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////
		
	public boolean printStockReport(){
		return printStocksReport.getValue();
	}
	
	public double getProfitMargin(){
		return profitMargin.getValue();
	}

	public FacilityGeneralManager getGeneralManager() {
		return generalManager;
	}

	public FacilityFinancialManager getFinancialManager() {
		return financialManager;
	}

	public FacilityOperationsManager getOperationsManager() {
		return operationsManager;
	}
	
	public FacilityTransportationManager getTransportationManager(){
		return transportationManager;
	}

	public HashMapList<String,LogisticsEntity> getInsideFacilityLimits(){
		return insideFacilityLimits;
	}

	// TODO use Enums instead of numbers for better readability
	/**
	 * List of feedstock and output materials
	 * <br> <b> 0- </b> Facility's technical capacity for each material (t or m3 /h)
	 * <br> <b> 1- </b> Target demand for the current planning horizon
	 * <br> <b> 2- </b> Target throughput for the current planning horizon
	 * <br> <b> 3- </b> Unstatisfied target demand in contracts or internally (infeed)
	 * <br> <b> 4- </b> sold throughput in contracts or used internally (outfeed)
	 * <br> <b> 5- </b> Total stockpiles capacity for each material
	 * <br> <b> 6- </b> Total amount in all stockpiles
	 * <br> <b> 7- </b> Reserved amount for loading scheduled bulk cargos
	 * <br> <b> 8- </b> Reserved amount for unloading
	 * <br> <b> 9- </b> Current offer price per unit of material in the current planning horizon
	 * <br> <b> 10- </b> Average purchase price
	 * <br> <b> 11- </b> fullfilled supply contracts amount
	 * <br> <b> 12- </b> fullfilled demand contracts amount
	 * <br> <b> 13- </b> realized throughput through feedstock supply
	 * <br> <b> 14- </b> Total amount produced (by processors)
	 * <br> <b> 15- </b> Total amount received (from suppliers)
	 * <br> <b> 16- </b> Total amount shipped (to buyers)
	 */
	public TwoLinkedLists<BulkMaterial> getStockList(){
		return stocksList;
	}

	/**
	 * List of feedstock and output materials
	 * <br> <b> 0- </b> Facility's technical capacity for each material (t or m3 /h)
	 * <br> <b> 1- </b> Target demand for the current planning horizon
	 * <br> <b> 2- </b> Target throughput for the current planning horizon
	 * <br> <b> 3- </b> Unstatisfied target demand in contracts or internally (infeed)
	 * <br> <b> 4- </b> sold throughput in contracts or used internally (outfeed)
	 * <br> <b> 5- </b> Total stockpiles capacity for each material
	 * <br> <b> 6- </b> Total amount in all stockpiles
	 * <br> <b> 7- </b> Reserved amount for loading scheduled bulk cargos
	 * <br> <b> 8- </b> Reserved amount for unloading
	 * <br> <b> 9- </b> Current offer price per unit of material in the current planning horizon
	 * <br> <b> 10- </b> Average purchase price
	 * <br> <b> 11- </b> fullfilled supply contracts amount
	 * <br> <b> 12- </b> fullfilled demand contracts amount
	 * <br> <b> 13- </b> realized throughput through feedstock supply
	 * <br> <b> 14- </b> Total amount produced (by processors)
	 * <br> <b> 15- </b> Total amount received (from suppliers)
	 * <br> <b> 16- </b> Total amount shipped (to buyers)
	 */
	public void setStocksList(BulkMaterial bulkMaterial, int valueListIndex, double amount){
		stocksList.set(bulkMaterial, valueListIndex, amount);
		if(printStockReport())
			this.printStocksReport(bulkMaterial);
	}
	
	/**
	 * List of feedstock and output materials
	 * <br> <b> 0- </b> Facility's technical capacity for each material (t or m3 /h)
	 * <br> <b> 1- </b> Target demand for the current planning horizon
	 * <br> <b> 2- </b> Target throughput for the current planning horizon
	 * <br> <b> 3- </b> Unstatisfied target demand in contracts or internally (infeed)
	 * <br> <b> 4- </b> sold throughput in contracts or used internally (outfeed)
	 * <br> <b> 5- </b> Total stockpiles capacity for each material
	 * <br> <b> 6- </b> Total amount in all stockpiles
	 * <br> <b> 7- </b> Reserved amount for loading scheduled bulk cargos
	 * <br> <b> 8- </b> Reserved amount for unloading
	 * <br> <b> 9- </b> Current offer price per unit of material in the current planning horizon
	 * <br> <b> 10- </b> Average purchase price
	 * <br> <b> 11- </b> fullfilled supply contracts amount
	 * <br> <b> 12- </b> fullfilled demand contracts amount
	 * <br> <b> 13- </b> realized throughput through feedstock supply
	 * <br> <b> 14- </b> Total amount produced (by processors)
	 * <br> <b> 15- </b> Total amount received (from suppliers)
	 * <br> <b> 16- </b> Total amount shipped (to buyers)
	 */
	public void addToStocksList(BulkMaterial bulkMaterial, int valueListIndex, double amount){
		// Whether supply contracts have been inactive due to material unavailability
		boolean activateSupplyContracts= false;
		if(valueListIndex == 6 && Tester.greaterCheckTolerance(amount, 0.0d)
				){
			activateSupplyContracts = true;
		}
		
		stocksList.add(bulkMaterial, valueListIndex, amount);
		
		// inactivate demand contracts if stockpiles are filled up
		if(Tester.greaterOrEqualCheckTolerance(this.getStockList().getValueFor(bulkMaterial, 6), this.getStockList().getValueFor(bulkMaterial, 5))){
			for (Contract each : this.getGeneralManager()
					.getDemandContractsList().get(bulkMaterial)) {
				this.scheduleProcess(0.0d, PRIO_DEFAULT, new ReflectionTarget(each, "updatePlanningTimes",false,this));
			}
		}
		
		// active facility if just got material
		if (activateSupplyContracts) {
			if(printStockReport())
				this.printStocksReport(bulkMaterial);
			for (Contract each : this.getGeneralManager()
					.getSupplyContractsList().get(bulkMaterial)) {
				
				each.setFacilityActiveness(true, this);
			}
		}	
		
		if(printStockReport())
			this.printStocksReport(bulkMaterial);
	}

	/**
	 * List of feedstock and output materials
	 * <br> <b> 0- </b> Facility's technical capacity for each material (t or m3 /h)
	 * <br> <b> 1- </b> Target demand for the current planning horizon
	 * <br> <b> 2- </b> Target throughput for the current planning horizon
	 * <br> <b> 3- </b> Unstatisfied target demand in contracts or internally (to buy)
	 * <br> <b> 4- </b> Unsold target throughput in contracts or used internally (to sell)
	 * <br> <b> 5- </b> Total stockpiles capacity for each material
	 * <br> <b> 6- </b> Total amount in all stockpiles
	 * <br> <b> 7- </b> Reserved amount for loading scheduled bulk cargos
	 * <br> <b> 8- </b> Reserved amount for unloading
	 * <br> <b> 9- </b> Current offer price per unit of material in the current planning horizon
	 * <br> <b> 10- </b> Average purchase price
	 * <br> <b> 11- </b> fullfilled supply contracts amount
	 * <br> <b> 12- </b> fullfilled demand contracts amount
	 * <br> <b> 13- </b> realized throughput through feedstock supply
	 * <br> <b> 14- </b> Total amount produced (by processors)
	 * <br> <b> 15- </b> Total amount received (from suppliers)
	 * <br> <b> 16- </b> Total amount shipped (to buyers)
	 */
	public void removeFromStocksList(BulkMaterial bulkMaterial, int valueListIndex, double amount){
		// Whether demand contracts have been inactive due to stockpiles maxing out on capacity
		boolean activateDemandContracts= false;
		if(valueListIndex == 6 && Tester.greaterCheckTolerance(amount, 0.0d) &&
				Tester.equalCheckTolerance(this.getStockList().getValueFor(bulkMaterial, 6), this.getStockList().getValueFor(bulkMaterial, 5))){
			activateDemandContracts = true;
		}
		
		stocksList.remove(bulkMaterial, valueListIndex, amount);
		
		// inactivate supply contracts if stockpiles are empty
		if(Tester.equalCheckTolerance(this.getStockList().getValueFor(bulkMaterial, 6), 0.0d)){
			for (Contract each : this.getGeneralManager()
					.getSupplyContractsList().get(bulkMaterial)) {
				each.setFacilityActiveness(false, this);
			}
		}
		
		// active facility
		if (activateDemandContracts) {
			for (Contract each : this.getGeneralManager()
					.getDemandContractsList().get(bulkMaterial)) {
				if(printStockReport())
					this.printStocksReport(bulkMaterial);
				each.setFacilityActiveness(true, this);
			}
		}

		if(printStockReport())
			this.printStocksReport(bulkMaterial);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// Output METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////

	@Output(name = "ProductionLevel",
	 description = "Production level for the current planning horizon.",
	    unitType = MassFlowUnit.class)
	public double getProductionLevel(double simTime, BulkMaterial tempBulkMaterial) {
		return this.getStockList().getValueFor(tempBulkMaterial, 2);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	// Report METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	public void printStocksReport(BulkMaterial bulkMaterial){
		if (this.printStockReport() && stocksReportFile != null) {
			stocksReportFile.putDoubleWithDecimalsTabs(this.getSimTime()/3600,
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putStringTabs(bulkMaterial.getName(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 0)/1000,
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 1)/1000,
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 2)/1000,
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 3)/1000,
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 4)/1000,
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 5)/1000,
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 6)/1000,
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 7)/1000,
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 8)/1000,
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 9)*1000,
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 10)*1000,
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 11)/1000,
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 12)/1000,
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 13)/1000,
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 14)/1000,
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 15)/1000,
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 16)/1000,
					ReportAgent.getReportPrecision(), 1);
			
			stocksReportFile.newLine();
			stocksReportFile.flush();
		}

	}
	
	public void printStocksReportHeader() {
		stocksReportFile.putStringTabs("Time", 1);
		stocksReportFile.putStringTabs("Material", 1);
		stocksReportFile.putStringTabs("Technical Capacity", 1);
		stocksReportFile.putStringTabs("Target demand", 1);
		stocksReportFile.putStringTabs("Target Throughput", 1);
		stocksReportFile.putStringTabs("Unsatisfied Demand Amount in Contracts", 1);
		stocksReportFile.putStringTabs("Sold Throughput Amount in Contracts", 1);
		stocksReportFile.putStringTabs("Total stockpile capacities", 1);
		stocksReportFile.putStringTabs("Total amount in all stockpiles", 1);
		stocksReportFile.putStringTabs("Reserved for loading", 1);
		stocksReportFile.putStringTabs("Reserved for unloading", 1);
		stocksReportFile.putStringTabs("Current offer price per", 1);
		stocksReportFile.putStringTabs("Average purchase price", 1);
		stocksReportFile.putStringTabs("Fullfilled supply", 1);
		stocksReportFile.putStringTabs("Fullfilled demand", 1);
		stocksReportFile.putStringTabs("Realized throughput through feedstock supply", 1);
		stocksReportFile.putStringTabs("Total produced (by processors)", 1);
		stocksReportFile.putStringTabs("Total received (from suppliers)", 1);
		stocksReportFile.putStringTabs("Total shipped (to buyers)", 1);
		
		stocksReportFile.newLine();
		stocksReportFile.flush();	
		
		// Print units
		stocksReportFile.putStringTabs("(h)", 2);
		stocksReportFile.putStringTabs("(tonne)", 1);
		stocksReportFile.putStringTabs("(tonne)", 1);
		stocksReportFile.putStringTabs("(tonne)", 1);
		stocksReportFile.putStringTabs("(tonne)", 1);
		stocksReportFile.putStringTabs("(tonne)", 1);
		stocksReportFile.putStringTabs("(tonne)", 1);
		stocksReportFile.putStringTabs("(tonne)", 1);
		stocksReportFile.putStringTabs("(tonne)", 1);
		stocksReportFile.putStringTabs("(tonne)", 1);
		stocksReportFile.putStringTabs("($/tonne)", 1);
		stocksReportFile.putStringTabs("($/tonne)", 1);
		stocksReportFile.putStringTabs("(tonne)", 1);
		stocksReportFile.putStringTabs("(tonne)", 1);
		stocksReportFile.putStringTabs("(tonne)", 1);
		stocksReportFile.putStringTabs("(tonne)", 1);
		stocksReportFile.putStringTabs("(tonne)", 1);

		stocksReportFile.newLine();
		stocksReportFile.flush();	
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// UPDATER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if(!managersInitialized){
			managersInitialized = true;
			// initializing managers
			generalManager = InputAgent.defineEntity(FacilityGeneralManager.class,this.getInputName()+"/GeneralManager", false);
			financialManager = InputAgent.defineEntity(FacilityFinancialManager.class, this.getInputName()+"/FinancialManager", false);
			operationsManager = InputAgent.defineEntity(FacilityOperationsManager.class, this.getInputName()+"/OperationsManager", false);
			transportationManager = InputAgent.defineEntity(FacilityTransportationManager.class, this.getInputName()+"/TransportationManager", false);
			
			generalManager.setFlag(FLAG_GENERATED);;
			financialManager.setFlag(FLAG_GENERATED);
			operationsManager.setFlag(FLAG_GENERATED);
			transportationManager.setFlag(FLAG_GENERATED);
			
			generalManager.setFacility(this);
			financialManager.setFacility(this);
			operationsManager.setFacility(this);
			transportationManager.setFacility(this);
		}
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////
	// GRAPHICS
	// ////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/*public void draw(){
		if(this.getShapeFileQuery() != null){
			this.getShapeFileQuery().setStatement("'SELECT *  FROM milllocations_30aug2014 WHERE objectid < 100 ;'");
		this.getShapeFileQuery().execute(true, new DefinedShapeAttributes(sdaf));
		}
	}*/
	
	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);
	// TODO configure for when route is active
		/*	if (demandSankey.getValue()) {
			double total = 0.0d;
			for (ArrayList<Contract> eachMaterialList : this.getGeneralManager()
					.getDemandContractsList().getValues()) {
				for (Contract eachContract: eachMaterialList) {
					total += eachContract.getContractAmount();
				}
			}
			for (ArrayList<Contract> eachMaterialList : this.getGeneralManager()
					.getDemandContractsList().getValues()) {
				for (Contract eachContract: eachMaterialList) {
					for(DiscreteHandlingLinkedEntity eachSegment: RouteManager.getRoute(this, eachContract.getSupplier(), eachContract.getTransporterProtoType()).getRouteSegmentsList()){
						try{
							InputAgent.processEntity_Keyword_Value((RouteSegment)eachSegment, "Width", ((Double)(Math.ceil(eachContract.getContractAmount()/total)*10)).toString());
							InputAgent.processEntity_Keyword_Value((RouteSegment)eachSegment,"Colour",eachContract.getProduct().getColour());
						}catch (ClassCastException e){
							
						}
					}
					
				}
			}
		}*/
	}
	
}
