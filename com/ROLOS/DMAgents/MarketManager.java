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
	private HashMapList<BulkMaterial, Market> marketsList;

	static {
		marketManager = new MarketManager();
	}
	
	public MarketManager() {
		marketsList = new HashMapList<BulkMaterial, Market>(1);
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////
	// ADDER METHODS
	// ////////////////////////////////////////////////////////////////////////////////////////////////////

	public void addToMarket(Market market){
		marketsList.add(market.getProduct(), market);
	}
	
	@Override
	public void startUp() {
		super.startUp();
		this.scheduleProcess(0.0d, 1, new ReflectionTarget(this, "activateMarkets"));

	}
	
	/**
	 * Activates all markets 
	 * TODO Since it assumes there is only one market per product, only the first one
	 * is activated. refactor for when there would be multiple markets per product
	 */
	public void activateMarkets(){
		// prioritize markets
		PriorityQueue<BulkMaterial> tempMarketList = new PriorityQueue<>(new DescendingPriotityComparator<BulkMaterial>(ROLOSEntity.class,"getInternalPriority"));
		tempMarketList.addAll(marketsList.getKeys());
		
		for(BulkMaterial eachProduct: tempMarketList){
			for(Market eachMarket: marketsList.get(eachProduct)){
				eachMarket.populateLists();
				// plan production for all facilities and all processing routes
				for(Facility eachFacility: eachMarket.getSellersList()){
					for (ArrayList<ProcessingRoute> processingRoutesList: eachFacility.getOperationsManager().getProcessingRoutesList().getValues()) {
						for (ProcessingRoute processingRoute: processingRoutesList) {
							if(!processingRoute.getOutfeedMaterial().contains(eachProduct))
								continue;
							if (processingRoute.getLastPlannedTime() != this.getSimTime()) {
								eachFacility.getOperationsManager().planProduction(processingRoute,
												this.getSimTime(),this.getSimTime()+ SimulationManager.getPlanningHorizon());
								processingRoute.setLastPlannedTime(this.getSimTime());							
							}
								//TODO set selling prices?								
						}
					}
				}
				// TODO since this is just using a heuristic sellers market, when planning buyers production, offer prices from a buyer point of view is set
				for(Facility eachFacility: eachMarket.getBuyersList()){
					for (ArrayList<ProcessingRoute> processingRoutesList: eachFacility.getOperationsManager().getProcessingRoutesList().getValues()) {
						for (ProcessingRoute processingRoute: processingRoutesList) {
							if(!processingRoute.getInfeedMaterial().contains(eachProduct))
								continue;
							if (processingRoute.getLastPlannedTime() != this.getSimTime()) {
								eachFacility.getOperationsManager().planProduction(processingRoute,
												this.getSimTime(),this.getSimTime()+ SimulationManager.getPlanningHorizon());
								// TODO This resets all transportation plans. Refactor to only reset plans serving this processing route's contracts
								eachFacility.getTransportationManager().resetTransportationPlan();
							}
							//set offer prices
							eachFacility.getFinancialManager().setOfferPrices(processingRoute);
							processingRoute.setLastPlannedTime(this.getSimTime());	
							
						}
					}
				}
				// TODO figuring out markets one at a time based on material's internal priority
				eachMarket.runSellersMarket();
			}
		}
		this.scheduleProcess(SimulationManager.getPlanningHorizon(), 1, new ReflectionTarget(this, "activateMarkets"));
	}
}
