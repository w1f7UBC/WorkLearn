package com.AROMA.DMAgents;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;

import java.util.ArrayList;

import worldwind.DefinedShapeAttributes;
import worldwind.QueryFrame;
import worldwind.WorldWindFrame;
import DataBase.Query;

import com.AROMA.Economic.Contract;
import com.AROMA.Logistics.BulkMaterial;
import com.AROMA.Logistics.Facility;
import com.AROMA.Logistics.ReportAgent;
import com.AROMA.Logistics.Route;
import com.AROMA.Utils.HandyUtils;
import com.AROMA.Utils.HashMapList;
import com.jaamsim.basicsim.ReflectionTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.Tester;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Color4d;
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
	
	@Keyword(description = "If TRUE, then statistics for surplus and deficit of all products of all facilities are " +
            "included in a separate report folder.",
     example = "PlanningManager printSurplusDeficitReport { TRUE }")
	private static final BooleanInput printSurplusDeficitReport;

	@Keyword(description = "Simulation time step - defining how refined the process oriented events (such as bulk material processing) works."
			+ "Default is 1 h.", 
		example = "Temiscaming TimeStep { 1 d }")
	private static final ValueInput timeStep;
	
	private static double previousPlanningTime, nextPlanningTime;
	
	protected static FileEntity contractsReportFile;      
	protected static FileEntity transportReportFile;      
	protected static FileEntity surplusDeficitReportFile;
	
	//TODO move to a worldwind class. layers to remove at the begining of the planning time!
	private static final ArrayList<String> removeableWorldViewLayers;

	static {
		planningHorizon = new ValueInput("PlanningHorizon", "Key Inputs", 31536000.0d);
		planningHorizon.setUnitType(TimeUnit.class);
		
		timeStep = new ValueInput("TimeStep", "Key Inputs", 3600.0d);
		timeStep.setUnitType(TimeUnit.class);
		
		printContractsReport = new BooleanInput("PrintContractsReport", "Report", false);
		
		printTransportReport = new BooleanInput("PrintTransportReport", "Report", false);
		
		printSurplusDeficitReport = new BooleanInput("PrintSurplusDeficitReport", "Report", false);
		
		removeableWorldViewLayers = new ArrayList<String>(1);

	}
	
	{
		this.addInput(planningHorizon);
		this.addInput(timeStep);
		this.addInput(printContractsReport);
		this.addInput(printTransportReport);
		this.addInput(printSurplusDeficitReport);
	}
	
	//TODO import it like report or simulation classes
	public SimulationManager() {
	}
	
	@Override
	public void validate() {
		super.validate();
		
		//TODO bad implementation for showing facilities with different color. refactor when overriding shapefile based on input is figured out
		if(WorldWindFrame.AppFrame != null){
			//Populate colors list
			HashMapList<Color4d, Facility> colorScheme = new HashMapList<Color4d, Facility>();
			for(Facility facility: Facility.getAll()){
				colorScheme.add(facility.getColor(), facility);
			}
			//Show objects
			for(Color4d eachColor: colorScheme.getKeys()){
				Facility tempFacility = colorScheme.get(eachColor).get(0);

				//if(tempFacility.getShapeFileQuery() != null)				
				//	tempFacility.getShapeFileQuery().execute(tempFacility.getColorInput().getValueString()+"Facilities", colorScheme.get(eachColor), false, false, 
						//new DefinedShapeAttributes(eachColor, tempFacility.getWidth(), tempFacility.getOpacity()));

			}
			Facility.getAll().get(0).getShapeFileQuery().updatePosition(Facility.getAll());
    	}
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
		
		if(printSurplusDeficitReport.getValue()){
			surplusDeficitReportFile = ReportAgent.initializeFile(this,".sdr");
			this.printSurplusDeficitReportHeader();
		}
	}
	
	@Override
	public void startUp() {
		// TODO Auto-generated method stub
		super.startUp();
		this.updatePlanningTimes();
	}
	
	public void updatePlanningTimes(){
		previousPlanningTime = this.getSimTime();
		nextPlanningTime = this.getSimTime() + SimulationManager.getPlanningHorizon();
		
		// remove the removable layers and empty the list
		if(WorldWindFrame.AppFrame != null){
			for(String eachLayer: removeableWorldViewLayers){
				WorldWindFrame.AppFrame.removeShapefileLayer(eachLayer);
			}
			removeableWorldViewLayers.clear();
		}		
		this.scheduleProcess(SimulationManager.getPlanningHorizon(), 1, new ReflectionTarget(this, "updatePlanningTimes"));
	}
	
	public static ArrayList<String> getRemoveablebleWorldWindLayers(){
		return removeableWorldViewLayers;
	}
	
	public static double getPlanningHorizon(){
		return planningHorizon.getValue();
	}
	
	public static double getTimeStep(){
		return timeStep.getValue();
	}
	
	public static double getPreviousPlanningTime(){
		return previousPlanningTime;
	}
	
	public static double getNextPlanningTime(){
		return nextPlanningTime;
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	// Report METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	public static void printContractReport(Contract contract){
		if (printContractsReport.getValue()) {
			contractsReportFile.putDoubleWithDecimalsTabs(contract.getSimTime()/3600,
					ReportAgent.getReportPrecision(), 1);
			contractsReportFile.putStringTabs(contract.getName(), 1);
			contractsReportFile.putStringTabs(contract.getProduct().getName(), 1);
			contractsReportFile.putStringTabs(contract.getSupplier().getName(), 1);
			contractsReportFile.putStringTabs(contract.getBuyer().getName(), 1);
			contractsReportFile.putDoubleWithDecimalsTabs(contract.getContractAmount()/1000,
					ReportAgent.getReportPrecision(), 1);
			contractsReportFile.putDoubleWithDecimalsTabs(contract.getContractPrice()*1000,
					ReportAgent.getReportPrecision(), 1);
			contractsReportFile.putDoubleWithDecimalsTabs(contract.getEstimatedTransportCost()*1000,
							ReportAgent.getReportPrecision(), 1);
			contractsReportFile.putStringTabs(HandyUtils.arraylistToString(contract.getDedicatedFleetsList()), 1);
			contractsReportFile.putStringTabs(HandyUtils.arraylistToString(new ArrayList<>(contract.getAssignedRoute().getRouteSegmentsList())), 1);
			contractsReportFile.putDoubleWithDecimalsTabs(contract.getBuyer().getStockList().getValueFor(contract.getProduct(), 3)/1000,
					ReportAgent.getReportPrecision(), 1);
			contractsReportFile.putDoubleWithDecimalsTabs(contract.getSupplier().getStockList().getValueFor(contract.getProduct(), 4)/1000,
					ReportAgent.getReportPrecision(), 1);
			contractsReportFile.putDoubleWithDecimalsTabs(contract.getSupplier().getStockList().getValueFor(contract.getProduct(), 13)/1000,
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
		contractsReportFile.putStringTabs("Contract Route", 1);
		contractsReportFile.putStringTabs("Buyer's unmet demand", 1);
		contractsReportFile.putStringTabs("Supplier's sold amount", 1);
		contractsReportFile.putStringTabs("Supplier's actual throughput", 1);
	//	contractsReportFile.putStringTabs("Contract Status", 1);
		
		contractsReportFile.newLine();
		contractsReportFile.flush();	
		
		// Print units
		contractsReportFile.putStringTabs("(h)", 5);
		contractsReportFile.putStringTabs("(tonne)", 1);
		contractsReportFile.putStringTabs("($/tonne)", 1);
		contractsReportFile.putStringTabs("($/tonne)", 3);
		contractsReportFile.putStringTabs("(tonne)", 1);
		contractsReportFile.putStringTabs("(tonne)", 1);
		contractsReportFile.putStringTabs("(tonne)", 1);

		contractsReportFile.newLine();
		contractsReportFile.flush();	
	}
	
	public static void printSurplusDeficitReportHeader() {
		surplusDeficitReportFile.putStringTabs("Time", 1);
		surplusDeficitReportFile.putStringTabs("Facility_Name", 1);		
		surplusDeficitReportFile.putStringTabs("Product", 1);
		surplusDeficitReportFile.putStringTabs("Actual_Throughput", 1);
		surplusDeficitReportFile.putStringTabs("Production_Surplus", 1);
		surplusDeficitReportFile.putStringTabs("Production_Deficit", 1);
		surplusDeficitReportFile.putStringTabs("Satisfied_Demand", 1);
		surplusDeficitReportFile.putStringTabs("Unstaisfied_Demand", 1);
		
		surplusDeficitReportFile.newLine();
		surplusDeficitReportFile.flush();	
		
		// Print units
		surplusDeficitReportFile.putStringTabs("(h)", 4);
		surplusDeficitReportFile.putStringTabs("(tonne)", 1);
		surplusDeficitReportFile.putStringTabs("(tonne)", 1);
		surplusDeficitReportFile.putStringTabs("(tonne)", 1);
		surplusDeficitReportFile.putStringTabs("(tonne)", 1);
		
		surplusDeficitReportFile.newLine();
		surplusDeficitReportFile.flush();	
	}
	
	public static void printSurplusDeficitReport(Facility facility){
		if (printSurplusDeficitReport.getValue()) {
			
			for (BulkMaterial eachMaterial: facility.getStockList().getEntityList()) {
				surplusDeficitReportFile.putDoubleWithDecimalsTabs(
						facility.getSimTime() / 3600,
						ReportAgent.getReportPrecision(), 1);
				surplusDeficitReportFile.putStringTabs(facility.getName(), 1);
				surplusDeficitReportFile.putStringTabs(eachMaterial.getName(), 1);
				
				surplusDeficitReportFile.putDoubleWithDecimalsTabs(Tester.min(facility.getStockList().getValueFor(eachMaterial, 2),(facility.getStockList().getValueFor(eachMaterial, 13)))/1000, ReportAgent.getReportPrecision(), 1);
				surplusDeficitReportFile.putDoubleWithDecimalsTabs((facility.getStockList().getValueFor(eachMaterial, 13)-
						facility.getStockList().getValueFor(eachMaterial, 4))/1000, ReportAgent.getReportPrecision(), 1);
				surplusDeficitReportFile.putDoubleWithDecimalsTabs(Tester.max(0.0d,(facility.getStockList().getValueFor(eachMaterial, 2)-facility.getStockList().getValueFor(eachMaterial, 13)))/1000, ReportAgent.getReportPrecision(), 1);
				surplusDeficitReportFile.putDoubleWithDecimalsTabs((facility.getStockList().getValueFor(eachMaterial, 1)-facility.getStockList().getValueFor(eachMaterial, 3))/1000, ReportAgent.getReportPrecision(), 1);
				surplusDeficitReportFile.putDoubleWithDecimalsTabs(facility.getStockList().getValueFor(eachMaterial, 3)/1000, ReportAgent.getReportPrecision(), 1);
				surplusDeficitReportFile.newLine();
			}
			surplusDeficitReportFile.flush();
		}

	}
	
	public static void printTransportationCostReport(Route route, BulkMaterial bulkMaterial, double unitCost){
		if (printTransportReport.getValue()) {
			transportReportFile.putDoubleWithDecimalsTabs(bulkMaterial.getSimTime()/3600,
					ReportAgent.getReportPrecision(), 1);
			transportReportFile.putStringTabs(route.getOrigin().getName(), 1);
			transportReportFile.putStringTabs(route.getDestination().getName(), 1);
			transportReportFile.putStringTabs(route.getRouteSegmentsList().toString(), 1);
			transportReportFile.putStringTabs(route.getMovingEntitiesList().toString(), 1);
			transportReportFile.putStringTabs(bulkMaterial.getName(), 1);
			transportReportFile.putDoubleWithDecimalsTabs(route.getLength()/1000,
					ReportAgent.getReportPrecision(), 1);
			transportReportFile.putDoubleWithDecimalsTabs(unitCost*1000,
					ReportAgent.getReportPrecision(), 1);
			transportReportFile.newLine();
			transportReportFile.flush();
		}

	}
	public void printTransportReportHeader() {
		transportReportFile.putStringTabs("Time", 1);
		transportReportFile.putStringTabs("Origin", 1);
		transportReportFile.putStringTabs("Destination", 1);
		transportReportFile.putStringTabs("Route Segments List", 1);
		transportReportFile.putStringTabs("Moving Entities List", 1);
		transportReportFile.putStringTabs("Bulk Material", 1);
		transportReportFile.putStringTabs("Distance", 1);
		transportReportFile.putStringTabs("Unit Cost", 1);
		
		transportReportFile.newLine();
		transportReportFile.flush();	
		
		// Print units
		transportReportFile.putStringTabs("(h)", 6);
		transportReportFile.putStringTabs("(km)", 1);
		transportReportFile.putStringTabs("($/tonne)", 1);

		transportReportFile.newLine();
		transportReportFile.flush();	
	}
	
	
}
