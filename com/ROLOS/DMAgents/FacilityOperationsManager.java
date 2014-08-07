package com.ROLOS.DMAgents;

import java.util.ArrayList;
import java.util.LinkedList;

import com.ROLOS.Logistics.BulkCargo;
import com.ROLOS.Logistics.BulkHandlingLinkedEntity;
import com.ROLOS.Logistics.BulkHandlingRoute;
import com.ROLOS.Logistics.BulkMaterial;
import com.ROLOS.Logistics.BulkMaterialProcessor;
import com.ROLOS.Logistics.Facility;
import com.ROLOS.Logistics.LinkedEntity;
import com.ROLOS.Logistics.Loader;
import com.ROLOS.Logistics.LoadingBay;
import com.ROLOS.Logistics.MovingEntity;
import com.ROLOS.Logistics.ProcessingRoute;
import com.ROLOS.Logistics.Stockpile;
import com.ROLOS.Utils.HashMapList;
import com.ROLOS.Utils.PriorityQueue;
import com.sandwell.JavaSimulation.EntityListListInput;
import com.jaamsim.events.ReflectionTarget;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Tester;

/**
 * Managing (reserving route, connecting, and disconnecting) bulk handling equipment networks.
 * Assigning loading docks 
 */
public class FacilityOperationsManager extends FacilityManager {

	//TODO add logic to only allow one process working at the same time
	@Keyword(description = "The list of processes that are mutually exclusive. Meaning that they produce the same primary product"
			+ "and that production levels and demands for infeed material are set together for these processes."
			+ "Can be thought of as a cogen plant that would process either hogfuel or saw dust or woodchip for energy production"
			+ "we'd define 3 different mutually exclusive processes for that."
			+ "Mutually exclusive processors should use the same throughput schedule ", 
			example = "Temiscaming/OperationsManager MutuallyExclusiveProcesses { { Cogen-Hogfuel Cogen-Sawdust Cogen-Woodchips} }")
	private final EntityListListInput<BulkMaterialProcessor> mutuallyExclusiveProcesses;
	
	private PriorityQueue<BulkHandlingRoute> loadingRoutesList;
	private PriorityQueue<BulkHandlingRoute> unloadingRoutesList;
	private HashMapList<BulkMaterial,ProcessingRoute> processingRoutesList;
	
	{
		mutuallyExclusiveProcesses = new EntityListListInput<>(BulkMaterialProcessor.class, "MutuallyExclusiveProcesses", "Key Inputs", null);
		this.addInput(mutuallyExclusiveProcesses);
	}
	
	public FacilityOperationsManager() {
		loadingRoutesList = new PriorityQueue<>(new DescendingPriotityComparator<BulkHandlingRoute>(BulkHandlingRoute.class, "getRoutePriority"));
		unloadingRoutesList = new PriorityQueue<>(new DescendingPriotityComparator<BulkHandlingRoute>(BulkHandlingRoute.class, "getRoutePriority"));
		processingRoutesList = new HashMapList<>(1);
		//set last plannedtime so that production planning is done at the beginning of the run
	}
	
	@Override
	public void earlyInit() {
		
		super.earlyInit();
		
	}
	
	@Override
	public void startUp() {
		super.startUp();
		this.scheduleProcess(0.0d, 1, new ReflectionTarget(this, "resetPlannedStocks"));
		
		// priotiy 1 to activate before market manager
		this.scheduleProcess(SimulationManager.getPlanningHorizon(), 1, new ReflectionTarget(this, "resetPlannedStocks"));
			
	}
	
	public void resetPlannedStocks(){
		for(BulkMaterial each: this.getFacility().getStockList().getEntityList()){
			this.getFacility().getStockList().set(each, 1, 0.0d);
			this.getFacility().getStockList().set(each, 2, 0.0d);
		}
	}
	
	public PriorityQueue<BulkHandlingRoute> getLoadingRoutesList() {
		return loadingRoutesList;
	}

	public PriorityQueue<BulkHandlingRoute> getUnloadingRoutesList() {
		return unloadingRoutesList;
	}

	/**
	 * TODO right now it is assumed that facilities only have one main process
	 */
	public HashMapList<BulkMaterial, ProcessingRoute> getProcessingRoutesList() {
		return processingRoutesList;
	}
	
	public Stockpile getStockpileForLoading(BulkMaterial bulkMaterial){
		for (Stockpile each: this.getFacility().getInsideFacilityLimits().get(Stockpile.class)){
			if(Tester.greaterCheckTolerance(each.getCurrentlyHandlingAmount(bulkMaterial),0.0d))
				return each;
		}
		return null;		
	}
	
	public Stockpile getStockpileForUnLoading(BulkMaterial bulkMaterial){
		for (Stockpile each: this.getFacility().getInsideFacilityLimits().get(Stockpile.class)){
			if(Tester.greaterCheckTolerance(each.getRemainingCapacity(bulkMaterial),0.0d))
				return each;
		}
		return null;		
	}
	
	public double getReservableCapacityForLoading(BulkMaterial bulkMaterial){
		Facility tempFacility = this.getFacility();
		return tempFacility.getStockList().contains(bulkMaterial) ? 
				tempFacility.getStockList().getValueFor(bulkMaterial, 4)-tempFacility.getStockList().getValueFor(bulkMaterial, 5)+tempFacility.getStockList().getValueFor(bulkMaterial, 6) :
					0.0d;
	}
		
	public double getReservableCapacityForUnloading(BulkMaterial bulkMaterial){
		Facility tempFacility = this.getFacility();
		return tempFacility.getStockList().contains(bulkMaterial) ? 
				tempFacility.getStockList().getValueFor(bulkMaterial, 3)-tempFacility.getStockList().getValueFor(bulkMaterial, 4)
				+tempFacility.getStockList().getValueFor(bulkMaterial, 5)- tempFacility.getStockList().getValueFor(bulkMaterial, 6) :
					0.0d;
	}

	/**
	 * @param bulkMaterial if null, it will return the first unloading bay that handles moving entity
	 * @param forLoading if true returns loading bay, if false returns unloading bay
	 * @return the first loading bay that handles movingEntity and has a connected stockpile that accepts the passed bulkMaterial or null if none found.
	 */
	// TODO this should ideally be called only once when fleet is scheduled and tentative loading bays are 
	// figured out otherwise this loops through every loadingbay in the facility. Refactor for a faster way when number of loading bays are too many
	public BulkHandlingRoute getBulkHandlingRoute(MovingEntity movingEntity, BulkMaterial bulkMaterial, boolean forLoading){
		LoadingBay tempBay;
		if(forLoading){
			for(BulkHandlingRoute eachRoute: loadingRoutesList){
				tempBay = eachRoute.getLoadingBay();
				if(tempBay.checkIfHandles(movingEntity)){
					if(bulkMaterial == null)
						return eachRoute;
					else if(eachRoute.getOutfeedBulkMaterial() == bulkMaterial.getProtoTypeEntity())
						return eachRoute;
				}
			}
		} else{
			for(BulkHandlingRoute eachRoute: unloadingRoutesList){
				tempBay = eachRoute.getLoadingBay();
				if(tempBay.checkIfHandles(movingEntity)){
					if(bulkMaterial == null)
						return eachRoute;
					else if(eachRoute.getOutfeedBulkMaterial() == bulkMaterial.getProtoTypeEntity())
						return eachRoute;
				}
			}
		}
		return null;
	}

	public ArrayList<ProcessingRoute> getMutuallyExclusiveProcesses(BulkMaterialProcessor bulkMaterialProcessor){
		ArrayList<ProcessingRoute> tempProcessingRoute = new ArrayList<ProcessingRoute>(1);
		if (mutuallyExclusiveProcesses.getValue() != null) {
			for (ArrayList<BulkMaterialProcessor> each : mutuallyExclusiveProcesses
					.getValue()) {
				if (each.contains(bulkMaterialProcessor))
					for (BulkMaterialProcessor eachProcessor : each)
						tempProcessingRoute.add(eachProcessor
								.getProcessingRoute());
			}
		}
		return tempProcessingRoute; 
	}
	
	/**
	 * @param bulkMaterial if null, it will return the first unloading bay that handles moving entity
	 * @param forLoading if true returns loading bay, if false returns unloading bay
	 * @return the first loading bay that equals the passed loadingBay and has a connected stockpile that accepts the passed bulkMaterial or null if none found.
	 */
	// TODO this should ideally be called only once when fleet is scheduled and tentative loading bays are 
	// figured out otherwise this loops through every loadingbay in the facility. Refactor for a faster way when number of loading bays are too many
	public BulkHandlingRoute getBulkHandlingRoute(LoadingBay loadingBay, BulkMaterial bulkMaterial, boolean forLoading){
		LoadingBay tempBay;
		if(forLoading){
			for(BulkHandlingRoute eachRoute: loadingRoutesList){
				tempBay = eachRoute.getLoadingBay();
				if(eachRoute.isConnected() && tempBay.equals(loadingBay)){
					if(bulkMaterial == null)
						return eachRoute;
					else if(eachRoute.getOutfeedBulkMaterial() == bulkMaterial.getProtoTypeEntity())
						return eachRoute;
				}
			}
		} else{
			for(BulkHandlingRoute eachRoute: unloadingRoutesList){
				tempBay = eachRoute.getLoadingBay();
				if(eachRoute.isConnected() && tempBay.equals(loadingBay)){
					if(bulkMaterial == null)
						return eachRoute;
					else if(eachRoute.getOutfeedBulkMaterial() == bulkMaterial.getProtoTypeEntity())
						return eachRoute;
				}
			}
		}
		return null;
	}
	
	// TODO refactor for shared equipment such as conveyors shared for multiple loading unloading ops
	//TODO this assumes that if bulkhandlingroute doesn't have active equipment only a loader or an unloader is attached to the cargo
	/**
	 * @return active equipment (whether active equipment of the route or the attached loader/unloader)
	 */
	public BulkHandlingLinkedEntity activateRoute(BulkHandlingRoute bulkHandlingRoute, BulkCargo bulkCargo){
		BulkHandlingLinkedEntity tempEquipment = null;
		double tempRate;

		if(bulkHandlingRoute.getActiveEquipment() == null){
			if(bulkHandlingRoute.isLoading())
				tempEquipment = bulkCargo.getAttachedLoader();
			else
				tempEquipment = bulkCargo.getAttachedUnLoader();
			tempRate = tempEquipment.getMaxRate();
		}else{
			tempEquipment = bulkHandlingRoute.getActiveEquipment();
			tempRate = bulkHandlingRoute.getInfeedRate();
		}
		
		// assign infeed/outfeeds and set states
		BulkMaterial tempMaterial = bulkHandlingRoute.getInfeedBulkMaterial();
		BulkHandlingLinkedEntity tempInfeed = null;
		
		if(bulkHandlingRoute.isLoading()){
			for(LinkedEntity each: bulkHandlingRoute.getRouteSegments()){
				if(each instanceof Stockpile){
					tempInfeed = (BulkHandlingLinkedEntity) each;
				} else if (each instanceof LoadingBay){
					tempInfeed.addToCurrentOutfeed(bulkCargo, tempMaterial, tempRate);
					bulkCargo.addToCurrentInfeed(tempInfeed, bulkHandlingRoute.getOutfeedBulkMaterial(), tempRate);
					each.setPresentState("Working");
				} else if(each instanceof BulkMaterialProcessor){
					tempInfeed.addToCurrentOutfeed((BulkHandlingLinkedEntity) each, tempMaterial, tempRate);
					tempMaterial = ((BulkMaterialProcessor) each).getOutfeedEntityTypeList().get(0);
					tempRate = ((BulkMaterialProcessor) each).getOutfeedRate(tempMaterial);
					((BulkMaterialProcessor) each).addToCurrentInfeed(tempInfeed, tempMaterial, tempRate);
					tempInfeed = (BulkHandlingLinkedEntity) each;
				} else {
					tempInfeed.addToCurrentOutfeed((BulkHandlingLinkedEntity) each, tempMaterial, tempRate);
					((BulkHandlingLinkedEntity) each).addToCurrentInfeed(tempInfeed, tempMaterial, tempRate);
					tempInfeed = (BulkHandlingLinkedEntity) each;			
				}
			}
		}
		// TODO unloading doesn't allow processing at the moment
		else{
			for(LinkedEntity each: bulkHandlingRoute.getRouteSegments()){
				 if (each instanceof LoadingBay){
					tempInfeed = (BulkHandlingLinkedEntity) each;
					each.setPresentState("Working");
				} 
				else if(each instanceof Stockpile){
					((BulkHandlingLinkedEntity) tempInfeed).addToCurrentOutfeed((BulkHandlingLinkedEntity) each, tempMaterial, tempRate);
					((BulkHandlingLinkedEntity) each).addToCurrentInfeed(tempInfeed, tempMaterial, tempRate);
				} else {
					tempInfeed.addToCurrentOutfeed((BulkHandlingLinkedEntity) each, tempMaterial, tempRate);
					((BulkHandlingLinkedEntity) each).addToCurrentInfeed(tempInfeed, tempMaterial, tempRate);
					tempInfeed = (BulkHandlingLinkedEntity) each;			
				}
			}
		}		
		return tempEquipment;
	}
	
	public void deactivateRoute(BulkHandlingRoute bulkHandlingRoute, BulkCargo bulkCargo){
		BulkHandlingLinkedEntity tempEquipment = null;
		double tempRate;

		if(bulkHandlingRoute.getActiveEquipment() == null){
			if(bulkHandlingRoute.isLoading())
				tempEquipment = bulkCargo.getAttachedLoader();
			else
				tempEquipment = bulkCargo.getAttachedUnLoader();
			tempRate = tempEquipment.getMaxRate();
		}else{
			tempEquipment = bulkHandlingRoute.getActiveEquipment();
			tempRate = bulkHandlingRoute.getInfeedRate();
		}
		
		// assign infeed/outfeeds and set states
		BulkMaterial tempMaterial = bulkHandlingRoute.getInfeedBulkMaterial();
		BulkHandlingLinkedEntity tempInfeed = null;
		
		if(bulkHandlingRoute.isLoading()){
			for(LinkedEntity each: bulkHandlingRoute.getRouteSegments()){
				if(each instanceof Stockpile){
					tempInfeed = (BulkHandlingLinkedEntity) each;
				} else if (each instanceof LoadingBay){
					tempInfeed.getCurrentOutfeedList().remove(bulkCargo, tempMaterial, 0, tempRate);
					bulkCargo.getCurrentInfeedList().remove(tempInfeed, bulkHandlingRoute.getOutfeedBulkMaterial(), 0, tempRate);
					tempInfeed = (BulkHandlingLinkedEntity) each;			
				} else if(each instanceof BulkMaterialProcessor){
					tempInfeed.getCurrentOutfeedList().remove((BulkHandlingLinkedEntity) each, tempMaterial, 0, tempRate);
					tempMaterial = ((BulkMaterialProcessor) each).getOutfeedEntityTypeList().get(0);
					tempRate = ((BulkMaterialProcessor) each).getOutfeedRate(tempMaterial);
					((BulkMaterialProcessor) each).getCurrentInfeedList().remove(tempInfeed, tempMaterial, 0, tempRate);
					tempInfeed = (BulkHandlingLinkedEntity) each;
				} else {
					tempInfeed.getCurrentOutfeedList().remove((BulkHandlingLinkedEntity) each, tempMaterial, 0, tempRate);
					((BulkHandlingLinkedEntity) each).getCurrentInfeedList().remove(tempInfeed, tempMaterial, 0, tempRate);
					tempInfeed = (BulkHandlingLinkedEntity) each;			
				}
				if(tempInfeed.getCurrentInfeedList().isEmpty() && tempInfeed.getCurrentOutfeedList().isEmpty())
					tempInfeed.setPresentState("Idle");
			}
		}
		// TODO unloading doesn't allow processing at the moment
		else{
			for(LinkedEntity each: bulkHandlingRoute.getRouteSegments()){
				 if (each instanceof LoadingBay){
					tempInfeed = (BulkHandlingLinkedEntity) each;
				} 
				else if(each instanceof Stockpile){
					((BulkHandlingLinkedEntity) tempInfeed).getCurrentOutfeedList().remove((BulkHandlingLinkedEntity) each, tempMaterial, 0, tempRate);
					((BulkHandlingLinkedEntity) each).getCurrentInfeedList().remove(tempInfeed, tempMaterial, 0, tempRate);
					tempInfeed = (BulkHandlingLinkedEntity) each;			
				} else {
					tempInfeed.getCurrentOutfeedList().remove((BulkHandlingLinkedEntity) each, tempMaterial, 0, tempRate);
					((BulkHandlingLinkedEntity) each).getCurrentInfeedList().remove(tempInfeed, tempMaterial, 0, tempRate);
					tempInfeed = (BulkHandlingLinkedEntity) each;			
				}
			}
			if(tempInfeed.getCurrentInfeedList().isEmpty() && tempInfeed.getCurrentOutfeedList().isEmpty())
				tempInfeed.setPresentState("Idle");
		}	
	}
	
	public void configureProcessingRoute(LinkedList<LinkedEntity> list, BulkHandlingLinkedEntity activeEquipment){
		for(ProcessingRoute each: processingRoutesList.get(((BulkMaterialProcessor)activeEquipment).getPrimaryProduct())){
			if(each.getProcessor() == activeEquipment){
				each.addNewStockPiles(list);
				return;
			} 
		}
		ProcessingRoute tempProcessingRoute = new ProcessingRoute(list, activeEquipment);
		processingRoutesList.add(((BulkMaterialProcessor)activeEquipment).getPrimaryProduct(), tempProcessingRoute);
		((BulkMaterialProcessor)activeEquipment).setProcessingRoute(tempProcessingRoute);
	}

	/**
	 * Sets target throughput/demand in facility's stocklist for all the products in the 
	 * processing routes based on the throughput of the production driving product.  
	 */
	public void planProduction(ProcessingRoute processingRoute, double startTime, double endTime){
		// TODO assuming mutually exclusive processing routes have the same throughput
		double throughput = processingRoute.getProcessor().getThroughput(startTime,endTime);
		double tempAmount;
		if(this.getMutuallyExclusiveProcesses(processingRoute.getProcessor()).isEmpty()){
			for (BulkMaterial eachMaterial : processingRoute.getInfeedMaterial()) {
				tempAmount = processingRoute.getCapacityRatio(eachMaterial,
						processingRoute.getProcessor().getPrimaryProduct()) * throughput;
				this.getFacility().addToStocksList(eachMaterial, 1, tempAmount);
				this.getFacility().addToStocksList(eachMaterial, 2, tempAmount);
				//set purchase price 0
				this.getFacility().setStocksList(eachMaterial, 8, 0.0d);
			}
			//TODO all logic assumes that mutually exclusive processes have the same outfeed with similar rates
			for (BulkMaterial eachMaterial : processingRoute.getOutfeedMaterial()) {
				tempAmount = processingRoute.getCapacityRatio(eachMaterial,
						processingRoute.getProcessor().getPrimaryProduct()) * throughput;
				this.getFacility().addToStocksList(eachMaterial, 1, tempAmount);
				this.getFacility().addToStocksList(eachMaterial, 2, tempAmount);
				//set purchase price 0
				this.getFacility().setStocksList(eachMaterial, 8, 0.0d);
			}
			processingRoute.setLastPlannedTime(this.getSimTime());
		}else{
			
			//TODO assumes infeeds for mutually exclusive processes are completely different
			//TODO change to enable having one processing route accept various infeed material!
			boolean outfeedSet = false;
			for (ProcessingRoute eachProcessor : this.getMutuallyExclusiveProcesses(processingRoute.getProcessor())) {
				for (BulkMaterial eachMaterial : eachProcessor.getInfeedMaterial()) {
					tempAmount = eachProcessor.getCapacityRatio(eachMaterial,
						eachProcessor.getProcessor().getPrimaryProduct()) * throughput;
					this.getFacility().addToStocksList(eachMaterial, 1, tempAmount);
					this.getFacility().addToStocksList(eachMaterial, 2, tempAmount);
					//set sell price 0
					this.getFacility().addToStocksList(eachMaterial, 8, 0.0d);
				}
				//TODO all logic assumes that mutually exclusive processes have the same outfeed with similar throughputs and outfeed (won't allow different 
				// outputs for different mutually exclusive processes
				if(!outfeedSet){
					for (BulkMaterial eachMaterial : eachProcessor.getOutfeedMaterial()) {
						tempAmount = eachProcessor.getCapacityRatio(eachMaterial,
								eachProcessor.getProcessor().getPrimaryProduct()) * throughput;
						this.getFacility().addToStocksList(eachMaterial, 1, tempAmount);
						this.getFacility().addToStocksList(eachMaterial, 2, tempAmount);
						//set sell price 0
						this.getFacility().setStocksList(eachMaterial, 8, 0.0d);
					}
					outfeedSet = true;
				}
				eachProcessor.setLastPlannedTime(this.getSimTime());
			}
		}
	}
	
	/**
	 * populates and registers bulk handling routes. Self operating routes are not figured out as bulk
	 * @param list	the list 
	 * @param loadingRoute true if a loading route false if an unloading route 
	 */
	public void populateBulkHanglingRoute(LinkedList<LinkedEntity> list, BulkHandlingLinkedEntity activeEquipment){
		BulkHandlingLinkedEntity tempActiveEquipment = activeEquipment;
		LinkedEntity lastAddedEntity = list.getLast();
		
		if(list.size() > 1){
			if (lastAddedEntity instanceof Stockpile){
				if(list.size() == 2){
					((LoadingBay)list.getFirst()).getSelfOperatingStockpiles().add((Stockpile) lastAddedEntity);
					return;
				}
				else if (list.getFirst() instanceof LoadingBay){
					unloadingRoutesList.add(new BulkHandlingRoute(list, activeEquipment));
					return;
				} else if(list.getFirst() instanceof Stockpile){
					this.configureProcessingRoute(list, activeEquipment);
					return;
				}
			} else if (lastAddedEntity instanceof LoadingBay){
				if(list.size() == 2){
					((LoadingBay)lastAddedEntity).getSelfOperatingStockpiles().add((Stockpile) list.getFirst());
					return;
				}
				else if (list.getFirst() instanceof LoadingBay){
					loadingRoutesList.add(new BulkHandlingRoute(list, activeEquipment));
					unloadingRoutesList.add(new BulkHandlingRoute(list, activeEquipment));
					return;
				} else if(list.getFirst() instanceof Stockpile){
					loadingRoutesList.add(new BulkHandlingRoute(list, activeEquipment));
					return;
				}
			}
		}
		
		//  TODO this assumes that the closest equipment closest to the last entity (destination) will be the active equipment
		for(LinkedEntity eachNext: lastAddedEntity.getOutfeedLinkedEntityList()){
			if(eachNext instanceof Loader || eachNext instanceof BulkMaterialProcessor)
				tempActiveEquipment = (BulkHandlingLinkedEntity) eachNext;
			list.addLast(eachNext);
			this.populateBulkHanglingRoute(list, tempActiveEquipment);
		}
	}
		

}
