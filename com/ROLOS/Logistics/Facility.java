package com.ROLOS.Logistics;

import java.util.ArrayList;

import com.ROLOS.ROLOSEntity;
import com.ROLOS.DMAgents.FacilityFinancialManager;
import com.ROLOS.DMAgents.FacilityGeneralManager;
import com.ROLOS.DMAgents.FacilityOperationsManager;
import com.ROLOS.DMAgents.FacilityTransportationManager;
import com.ROLOS.DMAgents.RouteManager;
import com.ROLOS.Economic.Contract;
import com.ROLOS.Utils.HashMapList;
import com.ROLOS.Utils.TwoLinkedLists;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.MassFlowUnit;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.FileEntity;
import com.jaamsim.input.Input;

import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Tester;

/**
 * Facilities always have entrance and exit blocks. those can be thought of as parking blocks from which
 * next available route or loading bay is planned.  
 */
public class Facility extends DiscreteHandlingLinkedEntity {
	private static final ArrayList<Facility> allInstances;

	@Keyword(description = "If TRUE, then statistics for all stocks will be printed for this facility"
			+ "everytime something is added or removed.",
     example = "Temiscaming PrintStocksReport { TRUE }")
	private final BooleanInput printStocksReport;

	@Keyword(description = "This shows whether this is where the cycle statistics of the moving entity should be gathered and reset." +
			"usually it is better to pick an end point where entities always endup there. default is false", 
			example = "Silverdale StartOfCycle { TRUE }")
	private final BooleanInput cycleStart;
	
	@Keyword(description = "Whether to show demand contracts Sankey diagram.", 
			example = "Silverdale ShowDemandSankeyDiagram { TRUE }")
	private final BooleanInput demandSankey;
	
	//TODO change to IRR when engineering economics calculations are implemented
	@Keyword(description = "Profit margin for this facility. (Revenue-cost)/Revenue",
	         example = "Temiscaming  ProfitMargin { 0.12 }")
	private final ValueInput profitMargin;
	
	// If facilities are not able to technically produce output products (feedstock piles ran out of content, output piles ran out of content and processing capability is lost).
	private boolean dormant;											
	// if facility is modeled as an abstract entity and is connected to route entities
	private boolean facilityConnectedToRoute;
	
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
	}
	
	{
		printStocksReport = new BooleanInput("PrintStocksReport", "Report", false);
		this.addInput(printStocksReport);
		
		cycleStart = new BooleanInput("StartOfCycle", "Key Inputs", false);
		this.addInput(cycleStart);
		
		profitMargin = new ValueInput("ProfitMargin", "Economic", 0.0d);
		this.addInput(profitMargin);
		
		demandSankey = new BooleanInput("ShowDemandSankeyDiagram", "Basic Graphics", false);
		this.addInput(demandSankey);
	}
	
	public Facility() {
		managersInitialized = false;
		dormant = false;
		insideFacilityLimits = new HashMapList<String,LogisticsEntity>(5);
		new TwoLinkedLists<>(4, new DescendingPriotityComparator<BulkMaterial>(ROLOSEntity.class, "getInternalPriority"),0);
		
		stocksList = new TwoLinkedLists<>(9, new DescendingPriotityComparator<BulkMaterial>(ROLOSEntity.class, "getInternalPriority"));
		new TwoLinkedLists<>(2, new DescendingPriotityComparator<BulkMaterial>(ROLOSEntity.class, "getInternalPriority"));
		
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

	public boolean isFacilityConnectedToRoute() {
		return facilityConnectedToRoute;
	}

	public void setFacilityConnectedToRoute(boolean facilityConnectedToRoute) {
		this.facilityConnectedToRoute = facilityConnectedToRoute;
	}

	// TODO use Enums instead of numbers for better readability
	/**
	 * List of feedstock and output materials
	 * <br> <b> 0- </b> Facility's technical capacity for each material (t or m3 /h)
	 * <br> <b> 1- </b> Target demand or throughput for the current planning horizon
	 * <br> <b> 2- </b> Unstatisfied target demand/throughput in contracts (to sell or to buy)
	 * <br> <b> 3- </b> Total stockpiles capacity for each material
	 * <br> <b> 4- </b> Total amount in all stockpiles
	 * <br> <b> 5- </b> Reserved amount for loading scheduled bulk cargos
	 * <br> <b> 6- </b> Reserved amount for unloading
	 * <br> <b> 7- </b> Current offer price per unit of material in the current planning horizon
	 * <br> <b> 8- </b> Average purchase price
	 */
	public TwoLinkedLists<BulkMaterial> getStockList(){
		return stocksList;
	}

	/**
	 * This method SHOULD NOT be used to add or remove amounts to stocks!
	 * List of feedstock and output materials
	 * <br> <b> 0- </b> Facility's technical capacity for each material (t or m3 /h)
	 * <br> <b> 1- </b> Target demand or throughput for the current planning horizon
	 * <br> <b> 2- </b> Unstatisfied target demand/throughput in contracts (to sell or to buy)
	 * <br> <b> 3- </b> Total stockpiles capacity for each material
	 * <br> <b> 4- </b> Total amount in all stockpiles
	 * <br> <b> 5- </b> Reserved amount for loading scheduled bulk cargos
	 * <br> <b> 6- </b> Reserved amount for unloading
	 * <br> <b> 7- </b> Current offer price per unit of material in the current planning horizon
	 * <br> <b> 8- </b> Average purchase price
	*/
	public void setStocksList(BulkMaterial bulkMaterial, int valueListIndex, double amount){
		stocksList.set(bulkMaterial, valueListIndex, amount);
		if(printStockReport())
			this.printStocksReport(bulkMaterial);
	}
	
	/**
	 * List of feedstock and output materials
	 * <br> <b> 0- </b> Facility's technical capacity for each material (t or m3 /h)
	 * <br> <b> 1- </b> Target demand or throughput for the current planning horizon
	 * <br> <b> 2- </b> Unstatisfied target demand/throughput in contracts (to sell or to buy)
	 * <br> <b> 3- </b> Total stockpiles capacity for each material
	 * <br> <b> 4- </b> Total amount in all stockpiles
	 * <br> <b> 5- </b> Reserved amount for loading scheduled bulk cargos
	 * <br> <b> 6- </b> Reserved amount for unloading
	 * <br> <b> 7- </b> Current offer price per unit of material in the current planning horizon
	 * <br> <b> 8- </b> Average purchase price
	*/
	public void addToStocksList(BulkMaterial bulkMaterial, int valueListIndex, double amount){
		// Whether supply contracts have been inactive due to material unavailability
		boolean activateContracts= false;
		if(valueListIndex == 4 && Tester.equalCheckTolerance(this.getStockList().getValueFor(bulkMaterial, 4), 0.0d)){
			activateContracts = true;
		}
		
		stocksList.add(bulkMaterial, valueListIndex, amount);
		
		// inactivate demand contracts if stockpiles filled up
		if(Tester.greaterOrEqualCheckTolerance(this.getStockList().getValueFor(bulkMaterial, 4), this.getStockList().getValueFor(bulkMaterial, 3))){
			for (Contract each : this.getGeneralManager()
					.getDemandContractsList().get(bulkMaterial)) {
				each.setFacilityActiveness(false, this);
			}
		}
		
		// active facility if just got material
		if (activateContracts) {
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
	 * <br> <b> 1- </b> Target demand or throughput for the current planning horizon
	 * <br> <b> 2- </b> Unstatisfied target demand/throughput in contracts (to sell or to buy)
	 * <br> <b> 3- </b> Total stockpiles capacity for each material
	 * <br> <b> 4- </b> Total amount in all stockpiles
	 * <br> <b> 5- </b> Reserved amount for loading scheduled bulk cargos
	 * <br> <b> 6- </b> Reserved amount for unloading
	 * <br> <b> 7- </b> Current offer price per unit of material in the current planning horizon
	 * <br> <b> 8- </b> Average purchase price
	*/
	public void removeFromStocksList(BulkMaterial bulkMaterial, int valueListIndex, double amount){
		// Whether demand contracts have been inactive due to stockpiles maxing out on capacity
		boolean activateContracts= false;
		if(valueListIndex == 4 && Tester.greaterOrEqualCheckTolerance(this.getStockList().getValueFor(bulkMaterial, 4), this.getStockList().getValueFor(bulkMaterial, 3))){
			activateContracts = true;
		}
		
		stocksList.remove(bulkMaterial, valueListIndex, amount);
		
		// inactivate supply contracts if stockpiles are empty
		if(Tester.equalCheckTolerance(this.getStockList().getValueFor(bulkMaterial, 4), 0.0d)){
			for (Contract each : this.getGeneralManager()
					.getSupplyContractsList().get(bulkMaterial)) {
				each.setFacilityActiveness(false, this);
			}
		}
		
		// active facility
		if (activateContracts) {
			for (Contract each : this.getGeneralManager()
					.getDemandContractsList().get(bulkMaterial)) {
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
	public double getProductionLevel(double simTime) {
		BulkMaterial tempBulkMaterial = this.getOperationsManager().getProcessingRoutesList().getValues().get(0).get(0).getProcessor().getPrimaryProduct();
		return this.getStockList().getValueFor(tempBulkMaterial, 1);
	}
	
	/* @Output(name = "ProductionLevel",
	 description = "Production level for the current planning horizon.",
	    unitType = MassFlowUnit.class)
	public double getProductionLevel( double simTime ) {
		BulkMaterial tempBulkMaterial = this.getOperationsManager().getProcessingRoutesList().getValues().get(0).get(0).getProcessor().getPrimaryProduct();
		return this.getStockList().getValueFor(tempBulkMaterial, 1);
	}
	*/
	//////////////////////////////////////////////////////////////////////////////////////
	// Report METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	public void printStocksReport(BulkMaterial bulkMaterial){
		if (this.printStockReport() && stocksReportFile != null) {
			stocksReportFile.putDoubleWithDecimalsTabs(this.getSimTime(),
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putStringTabs(bulkMaterial.getName(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 0),
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 1),
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 2),
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 3),
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 4),
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 5),
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 6),
					ReportAgent.getReportPrecision(), 1);
			stocksReportFile.putDoubleWithDecimalsTabs(stocksList.getValueFor(bulkMaterial, 7),
					ReportAgent.getReportPrecision(), 1);
			
			stocksReportFile.newLine();
			stocksReportFile.flush();
		}

	}
	
	public void printStocksReportHeader() {
		stocksReportFile.putStringTabs("Time", 1);
		stocksReportFile.putStringTabs("Material", 1);
		stocksReportFile.putStringTabs("Technical Capacity", 1);
		stocksReportFile.putStringTabs("Target demand/Throughput", 1);
		stocksReportFile.putStringTabs("Unsatisfied Amount in Contracts", 1);
		stocksReportFile.putStringTabs("Total stockpile capacities", 1);
		stocksReportFile.putStringTabs("Total amount in all stockpiles", 1);
		stocksReportFile.putStringTabs("Reserved amount for loading", 1);
		stocksReportFile.putStringTabs("Reserved amount for unloading", 1);
		stocksReportFile.putStringTabs("Offer price", 1);
		
		stocksReportFile.newLine();
		stocksReportFile.flush();	
		
		// Print units
		stocksReportFile.putStringTabs("(s)", 2);
		stocksReportFile.putStringTabs("(kg)", 1);
		stocksReportFile.putStringTabs("(kg)", 1);
		stocksReportFile.putStringTabs("(kg)", 1);
		stocksReportFile.putStringTabs("(kg)", 1);
		stocksReportFile.putStringTabs("(kg)", 1);
		stocksReportFile.putStringTabs("(kg)", 1);
		stocksReportFile.putStringTabs("(kg)", 1);
		stocksReportFile.putStringTabs("($/kg)", 1);

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
	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);
		if (demandSankey.getValue()) {
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
					for(DiscreteHandlingLinkedEntity eachSegment: RouteManager.transportationNetworkManager.getRoute(this, eachContract.getSupplier(), eachContract.getTransporterProtoType()).getRouteSegmentsList()){
						try{
							InputAgent.processEntity_Keyword_Value((RouteEntity)eachSegment, "Width", ((Double)(Math.ceil(eachContract.getContractAmount()/total)*10)).toString());
							InputAgent.processEntity_Keyword_Value((RouteEntity)eachSegment,"Colour",eachContract.getProduct().getColour());
						}catch (ClassCastException e){
							
						}
					}
					
				}
			}
		}
	}
	
}
