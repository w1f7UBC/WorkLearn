package com.ROLOS.DMAgents;

import java.util.ArrayList;

import com.ROLOS.ROLOSEntity;
import com.ROLOS.Economic.Market;
import com.ROLOS.Logistics.BulkMaterial;
import com.ROLOS.Logistics.Facility;
import com.ROLOS.Logistics.ProcessingRoute;
import com.ROLOS.Utils.HashMapList;
import com.ROLOS.Utils.PriorityQueue;
import com.jaamsim.events.ReflectionTarget;

public class MarketManager extends ROLOSEntity {

	public static final MarketManager marketManager;
	private PriorityQueue<Market> marketsList;
	private HashMapList<BulkMaterial, Market> marketsMap;

	static {
		marketManager = new MarketManager();
	}
	
	public MarketManager() {
		marketsList = new PriorityQueue<>(new DescendingPriotityComparator<Market>(ROLOSEntity.class,"getInternalPriority"));
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
		this.scheduleProcess(SimulationManager.getPlanningHorizon(), 3, new ReflectionTarget(this, "activateMarkets"));
	}
}
