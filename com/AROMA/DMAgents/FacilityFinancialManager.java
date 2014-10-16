package com.AROMA.DMAgents;

import java.util.ArrayList;
import java.util.LinkedList;

import com.AROMA.JavaSimulation.Tester_Rolos;
import com.AROMA.Logistics.BulkMaterial;
import com.AROMA.Logistics.BulkMaterialProcessor;
import com.AROMA.Logistics.LogisticsEntity;
import com.AROMA.Logistics.ProcessingRoute;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringInput;
import com.sandwell.JavaSimulation.Simulation;
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
	
	@Override
	public void startUp() {
		super.startUp();
		
	}
	
	/** sets offerprice (including transportation) for infeed material. assumes only one processor infeeds/outfeeds the material.
	 * if production is staged (e.g. log to chips- chips to pulp- pulp to paper) will look forward for the price of the end product.
	 * it'll stop when the price schedule for the outfeed is set. 
	 * TODO won't work if same infeed is shared among multiple processing routes!
	 * TODO assumes that the primary product of the last processing route in the chain is what drives production and sets the 
	 * breakeven price of all the infeeds in the chain
	 */
	public void setFeedstockBreakevenPrice(BulkMaterial infeedMaterial){
		BulkMaterial tempInfeed, chainOutfeed;
		LinkedList<ProcessingRoute> processingRouteList = this.getFacility().getOperationsManager().getChainProcessingRoutes(infeedMaterial);
		ProcessingRoute tempProcessingRoute = processingRouteList.pollLast();
		chainOutfeed = tempProcessingRoute.getProcessor().getPrimaryProduct();

		double tempOutputPrice = chainOutfeed.getPrice();
		if(tempOutputPrice == 0.0d){
		/*	throw new ErrorException("price schedule for %s returns ZERO at time %f! Check price schedule for this material. this material"
					+ "is the primary outfeed of %s and should have a non-zero price in order to be able to calculate breakeven price for the feedstock!", chainOutfeed.getName(),
					this.getSimTime(), this.getFacility().getName());
				*/
		}
		
		while(tempProcessingRoute != null){
			//TODO assuming only one infeed
			tempInfeed = tempProcessingRoute.getInfeedMaterial().get(0);
			// if value for breakeven price already set (non-zero) jump to the previous process other wise set the price
			if(this.getFacility().getStockList().getValueFor(tempInfeed, 9) == 0.0d){
				this.getFacility().setStocksList(tempInfeed, 9, calcPurchasePowerPrice(tempInfeed,tempProcessingRoute,tempOutputPrice));
			}
			tempOutputPrice = this.getFacility().getStockList().getValueFor(tempInfeed, 9);			
			tempProcessingRoute = processingRouteList.pollLast();
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
	public double calcPurchasePowerPrice(BulkMaterial inputMaterial, ProcessingRoute processingRoute, double expectedPrimaryProductPric){
		double inputRatio = processingRoute.getCapacityRatio(inputMaterial, processingRoute.getProcessor().getPrimaryProduct());
		
		return Tester.greaterCheckTolerance(inputRatio, 0.0d)? 
				Tester_Rolos.max(0.0d,this.calcRevenue(processingRoute,expectedPrimaryProductPric)/inputRatio): 0.0d;	
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
	public double calcRevenue(ProcessingRoute processingRoute, double expectedPrimaryProductPrice){
		double revenue;
		revenue = (1-this.getFacility().getProfitMargin()) *((expectedPrimaryProductPrice -
						processingRoute.getProcessor().getVariableCost(processingRoute.getProcessor().getPrimaryProduct()))-
				//TODO use more generic definition for fixed cost (e.g. adding individual processes fixed costs)
						this.getFacility().getFixedCost(SimulationManager.getPreviousPlanningTime(), SimulationManager.getNextPlanningTime())/
						this.getFacility().getStockList().getValueFor(processingRoute.getProcessor().getPrimaryProduct(), 2));
		
		return revenue;
	}

}
