package com.ROLOS.DMAgents;

import com.ROLOS.Economic.Contract;
import com.ROLOS.Logistics.BulkMaterial;
import com.ROLOS.Logistics.MovingEntity;
import com.ROLOS.Logistics.ReportAgent;
import com.ROLOS.Logistics.Route;
import com.ROLOS.Utils.HandyUtils;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.FileEntity;

import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class SimulationManager extends DisplayEntity {


	@Keyword(description = "Planning horizon for this facility. this value is used for production planning and establishing contracts."
			+ "Default is 1 yr.", 
		example = "PlanningManager PlanningHorizon { 5 yr }")
	private static final ValueInput planningHorizon;

	@Keyword(description = "If TRUE, then reports for established contracts will be printed out.",
     example = "PlanningManager PrintContractsReport { TRUE }")
	private static final BooleanInput printContractsReport;

	@Keyword(description = "If TRUE, then statistics for all managers of this facility are " +
            "included in a separate report folder.",
     example = "PlanningManager PrintTransportReport { TRUE }")
	private static final BooleanInput printTransportReport;

	@Keyword(description = "Simulation time step - defining how refined the process oriented events (such as bulk material processing) works."
			+ "Default is 1 h.", 
		example = "Temiscaming TimeStep { 1 d }")
	private static final ValueInput timeStep;
	
	protected static FileEntity contractsReportFile;      
	protected static FileEntity transportReportFile;      

	static {
		planningHorizon = new ValueInput("PlanningHorizon", "Key Inputs", 31536000.0d);
		planningHorizon.setUnitType(TimeUnit.class);
		
		timeStep = new ValueInput("TimeStep", "Key Inputs", 3600.0d);
		timeStep.setUnitType(TimeUnit.class);
		
		printContractsReport = new BooleanInput("PrintContractsReport", "Report", false);
		
		printTransportReport = new BooleanInput("PrintTransportReport", "Report", false);
	}
	
	{
		this.addInput(planningHorizon);
		this.addInput(timeStep);
		this.addInput(printContractsReport);
		this.addInput(printTransportReport);
	}
	
	//TODO import it like report or simulation classes
	public SimulationManager() {
	}
	
	@Override
	public void validate() {
		super.validate();
		
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();
		// initiate file if print report is true
		if(printContractsReport.getValue()){
			contractsReportFile = ReportAgent.initializeFile(this,".ctr");
			this.printContractsReportHeader();
		}
		if(printTransportReport.getValue()){
			transportReportFile = ReportAgent.initializeFile(this,".trp");
			this.printTransportReportHeader();
		}
	}
	
	public static double getPlanningHorizon(){
		return planningHorizon.getValue();
	}
	
	public static double getTimeStep(){
		return timeStep.getValue();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	// Report METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	public static void printContractReport(Contract contract){
		if (printContractsReport.getValue()) {
			contractsReportFile.putDoubleWithDecimalsTabs(contract.getSimTime(),
					ReportAgent.getReportPrecision(), 1);
			contractsReportFile.putStringTabs(contract.getName(), 1);
			contractsReportFile.putStringTabs(contract.getProduct().getName(), 1);
			contractsReportFile.putStringTabs(contract.getSupplier().getName(), 1);
			contractsReportFile.putStringTabs(contract.getBuyer().getName(), 1);
			contractsReportFile.putDoubleWithDecimalsTabs(contract.getContractAmount(),
					ReportAgent.getReportPrecision(), 1);
			contractsReportFile.putDoubleWithDecimalsTabs(contract.getContractPrice(),
					ReportAgent.getReportPrecision(), 1);
			contractsReportFile.putDoubleWithDecimalsTabs(contract.getEstimatedTransportCost(),
							ReportAgent.getReportPrecision(), 1);
			contractsReportFile.putStringTabs(HandyUtils.arraylistToString(contract.getDedicatedFleetsList()), 1);
			contractsReportFile.putStringTabs(contract.getTransporterProtoType().getName(), 1);
			contractsReportFile.putDoubleWithDecimalsTabs(contract.getBuyer().getStockList().getValueFor(contract.getProduct(), 2),
					ReportAgent.getReportPrecision(), 1);
			contractsReportFile.putDoubleWithDecimalsTabs(contract.getSupplier().getStockList().getValueFor(contract.getProduct(), 2),
					ReportAgent.getReportPrecision(), 1);
			contractsReportFile.newLine();
			contractsReportFile.flush();
		}

	}
	
	public static void printContractsReportHeader() {
		contractsReportFile.putStringTabs("Time", 1);
		contractsReportFile.putStringTabs("ContractID", 1);
		contractsReportFile.putStringTabs("Product", 1);
		contractsReportFile.putStringTabs("Supplier", 1);
		contractsReportFile.putStringTabs("Buyer", 1);
		contractsReportFile.putStringTabs("Amount", 1);
		contractsReportFile.putStringTabs("Unit Price", 1);
		contractsReportFile.putStringTabs("Estimated Transportation Cost", 1);
		contractsReportFile.putStringTabs("Dedicated Fleet", 1);
		contractsReportFile.putStringTabs("Transporter Proto Type", 1);
		contractsReportFile.putStringTabs("Buyer's UnFullfilled Amount", 1);
		contractsReportFile.putStringTabs("Supplier's UnFullfilled Amount", 1);
		contractsReportFile.putStringTabs("Contract Status", 1);
		
		contractsReportFile.newLine();
		contractsReportFile.flush();	
		
		// Print units
		contractsReportFile.putStringTabs("(s)", 5);
		contractsReportFile.putStringTabs("(kg)", 1);
		contractsReportFile.putStringTabs("($/kg)", 1);
		contractsReportFile.putStringTabs("($/kg)", 3);
		contractsReportFile.putStringTabs("(kg)", 1);

		contractsReportFile.newLine();
		contractsReportFile.flush();	
	}
	
	public static void printTransportationCostReport(MovingEntity movingEntity, Route route, BulkMaterial bulkMaterial, double unitCost){
		if (printTransportReport.getValue()) {
			transportReportFile.putDoubleWithDecimalsTabs(movingEntity.getSimTime(),
					ReportAgent.getReportPrecision(), 1);
			transportReportFile.putStringTabs(route.getOrigin().getName(), 1);
			transportReportFile.putStringTabs(route.getDestination().getName(), 1);
			transportReportFile.putStringTabs(movingEntity.getName(), 1);
			transportReportFile.putStringTabs(bulkMaterial.getName(), 1);
			transportReportFile.putDoubleWithDecimalsTabs(route.getDijkstraWeight(),
					ReportAgent.getReportPrecision(), 1);
			transportReportFile.putDoubleWithDecimalsTabs(unitCost,
					ReportAgent.getReportPrecision(), 1);
			transportReportFile.newLine();
			transportReportFile.flush();
		}

	}
	public void printTransportReportHeader() {
		transportReportFile.putStringTabs("Time", 1);
		transportReportFile.putStringTabs("Origin", 1);
		transportReportFile.putStringTabs("Destination", 1);
		transportReportFile.putStringTabs("Moving Entity", 1);
		transportReportFile.putStringTabs("Bulk Material", 1);
		transportReportFile.putStringTabs("Distance", 1);
		transportReportFile.putStringTabs("Unit Cost", 1);
		
		transportReportFile.newLine();
		transportReportFile.flush();	
		
		// Print units
		transportReportFile.putStringTabs("(s)", 5);
		transportReportFile.putStringTabs("(m)", 1);
		transportReportFile.putStringTabs("($/kg)", 1);

		transportReportFile.newLine();
		transportReportFile.flush();	
	}
	
	
}
