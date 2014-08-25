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
	 * TODO won't work if same infeed is shared among multiple processing routes!
	 */
	public void setOfferPrices(ProcessingRoute processingRoute){
		for(LogisticsEntity eachMaterial: processingRoute.getProcessor().getHandlingEntityTypeList()){
			this.getFacility().setStocksList((BulkMaterial) eachMaterial, 9, calcPurchasePowerPrice((BulkMaterial) eachMaterial,processingRoute));
		}
		//TODO set min selling price?
	}
	/**
	 * TODO refactor to engineering economic calculations
	 * TODO this method assumes only one input and one output, refactor to allocate value based on 
	 * multiple inputs and outputs
	 * TODO Until negative revenue is figured out, will return 0 for negative purchase power!!!
	 * @return purchase offer per unit of inputMaterial so that processing breakevens!(or zero if offer is negative!- should make better production planning!)
	 */
	public double calcPurchasePowerPrice(BulkMaterial inputMaterial, ProcessingRoute processingRoute){
		double inputRatio = processingRoute.getCapacityRatio(inputMaterial, processingRoute.getProcessor().getPrimaryProduct());
		
		return Tester.greaterCheckTolerance(inputRatio, 0.0d)? 
				Tester.max(0.0d,this.calcRevenue(processingRoute)/inputRatio): 0.0d;
		
	}
	
	/**
	 * <b> This method should be called after production planning </b>
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
	public double calcRevenue(ProcessingRoute processingRoute){
		double revenue;
		revenue = (1-this.getFacility().getProfitMargin()) *((processingRoute.getProcessor().getPrimaryProduct().getPrice() -
						processingRoute.getProcessor().getVariableCost(processingRoute.getProcessor().getPrimaryProduct()))-
				//TODO use more generic definition for fixed cost (e.g. adding individual processes fixed costs)
						this.getFacility().getFixedCost(getSimTime(), this.getSimTime()+ SimulationManager.getPlanningHorizon())/
						this.getFacility().getStockList().getValueFor(processingRoute.getProcessor().getPrimaryProduct(), 2));
		
		return revenue;
	}

}
