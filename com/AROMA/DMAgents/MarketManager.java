package com.AROMA.DMAgents;

import java.util.ArrayList;

import com.AROMA.AROMAEntity;
import com.AROMA.Economic.Market;
import com.AROMA.Logistics.BulkMaterial;
import com.AROMA.Logistics.Facility;
import com.AROMA.Logistics.ProcessingRoute;
import com.AROMA.Utils.HashMapList;
import com.AROMA.Utils.PriorityQueue;
import com.jaamsim.basicsim.ReflectionTarget;
import com.sandwell.JavaSimulation.Entity;


public class MarketManager extends AROMAEntity {

	public static final MarketManager marketManager;
	private PriorityQueue<Market> marketsList;
	private HashMapList<BulkMaterial, Market> marketsMap;

	static {
		marketManager = new MarketManager();
	}
	
	public MarketManager() {
		
		marketsList = new PriorityQueue<>(new AscendingPriotityComparator<Market>(AROMAEntity.class,"getInternalPriority"));
		marketsMap = new HashMapList<BulkMaterial, Market> (1);
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////
	// ADDER METHODS
	// ////////////////////////////////////////////////////////////////////////////////////////////////////

	public void addToMarket(Market market){
		marketsList.add(market);
		marketsMap.add(market.getProduct(), market);
	}
	
	@Override
	public void startUp() {
		super.startUp();
		this.scheduleProcess(0.0d, 3, new ReflectionTarget(this, "activateMarkets"));

	}
	
	public ArrayList<Market> getMarkets(BulkMaterial bulkMaterial){
		return marketsMap.get(bulkMaterial);
	}
	
	/**
	 * Activates all markets 
	 * TODO Since it assumes there is only one market per product, only the first one
	 * is activated. refactor for when there would be multiple markets per product
	 */
	public void activateMarkets(){
		
		for(Market eachMarket: marketsList){
			eachMarket.populateLists();
			
			// TODO figuring out markets one at a time based on material's internal priority
			eachMarket.runSellersMarket();
			
			
		}
		
		// priotiy 2 to activate after reseting planed inputs from last period and planning production for this period by operations manager
		this.scheduleProcess(SimulationManager.getPlanningHorizon(), 4, new ReflectionTarget(this, "activateMarkets"));
	}
}
