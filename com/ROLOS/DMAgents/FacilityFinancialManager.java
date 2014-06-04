package com.ROLOS.DMAgents;

import java.util.ArrayList;

import com.ROLOS.Logistics.BulkMaterial;
import com.ROLOS.Logistics.BulkMaterialProcessor;
import com.ROLOS.Logistics.LogisticsEntity;
import com.ROLOS.Logistics.ProcessingRoute;
import com.sandwell.JavaSimulation.ErrorException;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Simulation;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.Tester;

public class FacilityFinancialManager extends FacilityManager {

	private static final ArrayList<FacilityFinancialManager> allInstances;

	@Keyword(description = "facilities financial objective. options can be: PROFIT_MAX, ", 
			example = "HSPPManager FinancialObjective { 'PROFIT_MAX' }")
	private final StringInput financialObj;
	
	
	static {
		allInstances = new ArrayList<>();
	}
	
	{		
		financialObj = new StringInput("FinancialObjective", "Key Inputs", "PROFIT_MAX");
		this.addInput(financialObj);
	}
	
	public FacilityFinancialManager() {
		
		allInstances.add(this);
		

	}
	
	public static ArrayList<? extends FacilityFinancialManager> getAll() {
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
	
	/** sets offerprice (including transportation) for alternative infeed material. 
	 * should be called after production planning and called everytime one of the markets is established
	 * and contracts are rectified. 
	 */
	public void setOfferPrices(ProcessingRoute processingRoute){
		for(LogisticsEntity eachMaterial: processingRoute.getProcessor().getHandlingEntityTypeList()){
			this.getFacility().setStocksList((BulkMaterial) eachMaterial, 7, calcPurchaseOfferPrice((BulkMaterial) eachMaterial,processingRoute));
		}
		//TODO set min selling price?
	}
	/**
	 * TODO refactor to engineering economic calculations
	 * TODO this method assumes only one input and one output, refactor to allocate value based on 
	 * multiple inputs and outputs
	 * @return purchase offer per unit of inputMaterial so that processing breakevens!(or zero if offer is negative!- should make better production planning!)
	 */
	public double calcPurchaseOfferPrice(BulkMaterial inputMaterial, ProcessingRoute processingRoute){
		return Tester.max(0.0d,this.calcRevenue(inputMaterial,processingRoute)/processingRoute.getCapacityRatio(inputMaterial, processingRoute.getProcessor().getPrimaryProduct()));
	}
	
	/**
	 * <b> This method should be caled after production planning </b>
	 * @param inputMaterial the infeed material (alternative) whose revenue will be calculated for the processing route
	 * TODO refactor for engineering economic calculations, assumes revenue is only generated from
	 * the main product!
	 * TODO add complex multi-output/input. 
	 * TODO applies one profit margin for all processing routes
	 * TODO only considers fixed costs for the processor in the processing route
	 * TODO assumes only one processing route and main product for the facility
	 * TODO assumes revenue is realized only through the primary product
	 * @return revenue per unit of primary product (after removing profit margin)
	 */
	public double calcRevenue(BulkMaterial inputMaterial, ProcessingRoute processingRoute){
		double revenue;
		revenue = (1-this.getFacility().getProfitMargin()) *((processingRoute.getProcessor().getPrimaryProduct().getPrice() -
						processingRoute.getProcessor().getVariableCost(inputMaterial))-
				//TODO use more generic definition for fixed cost (e.g. adding individual processes fixed costs)
						this.getFacility().getFixedCost(getSimTime(), this.getSimTime()+ SimulationManager.getPlanningHorizon())/
						this.getFacility().getStockList().getValueFor(processingRoute.getProcessor().getPrimaryProduct(), 1));
		// Until negative revenue is figured out, throw an error for those situations
		if(Tester.lessCheckTolerance(revenue, 0.0d))
			return 0.0d;
		return revenue;
	}

}
