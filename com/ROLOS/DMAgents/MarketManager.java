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

	static {
		marketManager = new MarketManager();
	}
	
	public MarketManager() {
		marketsList = new PriorityQueue<>(new DescendingPriotityComparator<Market>(ROLOSEntity.class,"getInternalPriority"));
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////
	// ADDER METHODS
	// ////////////////////////////////////////////////////////////////////////////////////////////////////

	public void addToMarket(Market market){
		marketsList.add(market);
	}
	
	@Override
	public void startUp() {
		super.startUp();
		this.scheduleProcess(0.0d, 2, new ReflectionTarget(this, "activateMarkets"));

	}
	
	/**
	 * Activates all markets 
	 * TODO Since it assumes there is only one market per product, only the first one
	 * is activated. refactor for when there would be multiple markets per product
	 */
	public void activateMarkets(){
		
		for(Market eachMarket: marketsList){
			eachMarket.populateLists();
			// plan production for all facilities and all processing routes
			for(Facility eachFacility: eachMarket.getSellersList()){
				for (ArrayList<ProcessingRoute> processingRoutesList: eachFacility.getOperationsManager().getProcessingRoutesList().getValues()) {
					for (ProcessingRoute processingRoute: processingRoutesList) {
						if(!processingRoute.getOutfeedMaterial().contains(eachMarket.getProduct()))
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
			// Reseting planning information should be moved to the corresponding managers and scheduled for every planning horizon
			for(Facility eachFacility: eachMarket.getBuyersList()){
				for (ArrayList<ProcessingRoute> processingRoutesList: eachFacility.getOperationsManager().getProcessingRoutesList().getValues()) {
					for (ProcessingRoute processingRoute: processingRoutesList) {
						if(!processingRoute.getInfeedMaterial().contains(eachMarket.getProduct()))
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
		
		// priotiy 2 to activate after reseting planed inputs from last period by operations manager
		this.scheduleProcess(SimulationManager.getPlanningHorizon(), 2, new ReflectionTarget(this, "activateMarkets"));
	}
}
