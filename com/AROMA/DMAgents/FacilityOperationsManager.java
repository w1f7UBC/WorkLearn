package com.AROMA.DMAgents;

import java.util.ArrayList;
import java.util.LinkedList;

import com.AROMA.Logistics.BulkCargo;
import com.AROMA.Logistics.BulkHandlingLinkedEntity;
import com.AROMA.Logistics.BulkHandlingRoute;
import com.AROMA.Logistics.BulkMaterial;
import com.AROMA.Logistics.BulkMaterialProcessor;
import com.AROMA.Logistics.Facility;
import com.AROMA.Logistics.LinkedEntity;
import com.AROMA.Logistics.Loader;
import com.AROMA.Logistics.LoadingBay;
import com.AROMA.Logistics.MovingEntity;
import com.AROMA.Logistics.ProcessingRoute;
import com.AROMA.Logistics.Stockpile;
import com.AROMA.Utils.HashMapList;
import com.AROMA.Utils.PriorityQueue;
import com.jaamsim.basicsim.ReflectionTarget;
import com.jaamsim.input.EntityListListInput;
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
	//TODO processing routes based on outfeed (primary product) and feedstock
	private HashMapList<BulkMaterial,ProcessingRoute> processingRoutesListOutfeed;
	//TODO it assumes processors only accept one infeed material
	private HashMapList<BulkMaterial,ProcessingRoute> processingRoutesListInfeed;
	
	{
		mutuallyExclusiveProcesses = new EntityListListInput<>(BulkMaterialProcessor.class, "MutuallyExclusiveProcesses", "Key Inputs", null);
		this.addInput(mutuallyExclusiveProcesses);
	}
	
	public FacilityOperationsManager() {
		loadingRoutesList = new PriorityQueue<>(new DescendingPriorityComparator<BulkHandlingRoute>(BulkHandlingRoute.class, "getRoutePriority"));
		unloadingRoutesList = new PriorityQueue<>(new DescendingPriorityComparator<BulkHandlingRoute>(BulkHandlingRoute.class, "getRoutePriority"));
		processingRoutesListOutfeed = new HashMapList<>(1);
		processingRoutesListInfeed = new HashMapList<>(1);
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
		this.scheduleProcess(0.0d, 2, new ReflectionTarget(this, "planProduction"));
		this.scheduleProcess(0.0d, 8, new ReflectionTarget(this, "printSurplusDeficitReport"));
		
	}
	
	public void resetPlannedStocks(){
		for(BulkMaterial each: this.getFacility().getStockList().getEntityList()){
			this.getFacility().setStocksList(each, 1, 0.0d);
			this.getFacility().setStocksList(each, 2, 0.0d);
			this.getFacility().setStocksList(each, 3, 0.0d);
			this.getFacility().setStocksList(each, 4, 0.0d);
			this.getFacility().setStocksList(each, 9, 0.0d);
			this.getFacility().setStocksList(each, 11, 0.0d);
			this.getFacility().setStocksList(each, 12, 0.0d);
			this.getFacility().setStocksList(each, 13, 0.0d);
			this.getFacility().setStocksList(each, 15, 0.0d);
			this.getFacility().setStocksList(each, 16, 0.0d);
		}
		// priotiy 1 to activate before market manager
		this.scheduleProcess(SimulationManager.getPlanningHorizon(), 1, new ReflectionTarget(this, "resetPlannedStocks"));
					
	}
	
	public void printSurplusDeficitReport(){
		SimulationManager.printSurplusDeficitReport(this.getFacility());
		this.scheduleProcess(SimulationManager.getPlanningHorizon(), 8, new ReflectionTarget(this, "printSurplusDeficitReport"));

	}
	
	public PriorityQueue<BulkHandlingRoute> getLoadingRoutesList() {
		return loadingRoutesList;
	}

	public PriorityQueue<BulkHandlingRoute> getUnloadingRoutesList() {
		return unloadingRoutesList;
	}

	/**
	 * @return processing routes list based on their outfeed bulk material
	 * TODO right now it is assumed that facilities only have one primary product
	 */
	public HashMapList<BulkMaterial, ProcessingRoute> getProcessingRoutesListOutfeed() {
		return processingRoutesListOutfeed;
	}
	
	/**
	 * TODO assumes only one processor has infeedMaterial
	 * @param infeed if true will start from bulkMaterial as infeed of a chain of processors, if false will consider bulkMaterial as the outfeed of a series of processors
	 * @return the chain of processing routes that receive infeed material processes to outfeed and all the way to the last outfeed.
	 * (e.g. log to chips- chips to pulp- pulp to paper) 
	 */
	public LinkedList<ProcessingRoute> getChainProcessingRoutes(BulkMaterial bulkMaterial, boolean infeed){
		LinkedList<ProcessingRoute> processingRouteList = new LinkedList<ProcessingRoute> ();
		if (infeed) {
			BulkMaterial tempInfeed = bulkMaterial;
			ProcessingRoute tempProcessingRoute = this.getFacility()
					.getOperationsManager().getProcessingRoutesListInfeed()
					.get(tempInfeed).isEmpty() ? null : this.getFacility()
					.getOperationsManager().getProcessingRoutesListInfeed()
					.get(tempInfeed).get(0);

			while (tempProcessingRoute != null) {
				processingRouteList.addLast(tempProcessingRoute);
				// This is using primary product because forward looking is used to set prices only and primary products
				// price matters. TODO this should really loop over all the possible outfeeds
				tempInfeed = tempProcessingRoute.getProcessor()
						.getPrimaryProduct();
				// TODO assumes only one processing route is handling infeed!
				tempProcessingRoute = this.getFacility().getOperationsManager()
						.getProcessingRoutesListInfeed().get(tempInfeed)
						.isEmpty() ? null : this.getFacility()
						.getOperationsManager().getProcessingRoutesListInfeed()
						.get(tempInfeed).get(0);
			}
		} else {
			BulkMaterial tempOutfeed = bulkMaterial;
			ProcessingRoute tempProcessingRoute = this.getFacility()
					.getOperationsManager().getProcessingRoutesListOutfeed()
					.get(tempOutfeed).isEmpty() ? null : this.getFacility()
					.getOperationsManager().getProcessingRoutesListOutfeed()
					.get(tempOutfeed).get(0);

			while (tempProcessingRoute != null) {
				processingRouteList.addFirst(tempProcessingRoute);
				// TODO assumes processing route has only one handling infeed!
				tempOutfeed = tempProcessingRoute.getInfeedMaterial().get(0);
				tempProcessingRoute = this.getFacility().getOperationsManager()
						.getProcessingRoutesListOutfeed().get(tempOutfeed)
						.isEmpty() ? null : this.getFacility()
						.getOperationsManager().getProcessingRoutesListOutfeed()
						.get(tempOutfeed).get(0);
			}
		}
		
		return processingRouteList;
				
	}
	
	/**
	 * @return processing routes list based on their Infeed bulk material
	 * TODO right now it is assumed that facilities only have one main input feedstock
	 */
	public HashMapList<BulkMaterial, ProcessingRoute> getProcessingRoutesListInfeed() {
		return processingRoutesListInfeed;
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
				tempFacility.getStockList().getValueFor(bulkMaterial, 6)-tempFacility.getStockList().getValueFor(bulkMaterial, 7)+tempFacility.getStockList().getValueFor(bulkMaterial, 8) :
					0.0d;
	}
		
	public double getReservableCapacityForUnloading(BulkMaterial bulkMaterial){
		Facility tempFacility = this.getFacility();
		return tempFacility.getStockList().contains(bulkMaterial) ? 
				tempFacility.getStockList().getValueFor(bulkMaterial, 5)-tempFacility.getStockList().getValueFor(bulkMaterial, 6)
				+tempFacility.getStockList().getValueFor(bulkMaterial, 7)- tempFacility.getStockList().getValueFor(bulkMaterial, 8) :
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
		for(ProcessingRoute each: processingRoutesListOutfeed.get(((BulkMaterialProcessor)activeEquipment).getPrimaryProduct())){
			if(each.getProcessor() == activeEquipment){
				each.addNewStockPiles(list);
				return;
			} 
		}
		ProcessingRoute tempProcessingRoute = new ProcessingRoute(list, activeEquipment);
		processingRoutesListOutfeed.add(((BulkMaterialProcessor)activeEquipment).getPrimaryProduct(), tempProcessingRoute);
		//TODO assumes processors only have one input
		processingRoutesListInfeed.add((BulkMaterial)activeEquipment.getHandlingEntityTypeList().get(0), tempProcessingRoute);
		((BulkMaterialProcessor)activeEquipment).setProcessingRoute(tempProcessingRoute);
	}
	
	/**
	 * Tries to internally satisfy demand for infeed from processes that produce that infeed!
	 */
	public void satisfyDemandInternally(BulkMaterial infeedMaterial){
			double internalExchangeAmount = Tester.min(this.getFacility().getStockList().getValueFor(infeedMaterial, 1),
					this.getFacility().getStockList().getValueFor(infeedMaterial, 13));
			
			if(Tester.equalCheckTolerance(internalExchangeAmount, 0.0d))
				return;
			
			this.updateRealizedProduction(infeedMaterial, internalExchangeAmount);
			this.getFacility().addToStocksList(infeedMaterial, 4, internalExchangeAmount);
								
	}
	
	/**
	 * updates realized production based on feedstock supply
	 * TODO assumes only one processor and infeed per infeed material type exists
	 */
	public void updateRealizedProduction(BulkMaterial infeedMaterial, double amount){
		
		for (ProcessingRoute tempProcessoringRoute : this.getProcessingRoutesListInfeed().get(infeedMaterial)) {
			BulkMaterialProcessor tempProcessor = tempProcessoringRoute.getProcessor();
			
			//remove from unsatisfied demand
			this.getFacility().setStocksList(infeedMaterial, 3, 
					Tester.max(0.0d,this.getFacility().getStockList().getValueFor(infeedMaterial, 3)-amount));
			this.adjustMutuallyExclusiveProcessesDemands(infeedMaterial, amount);
			
			//this.getFacility().addToStocksList(infeedMaterial,13,amount);
			if (tempProcessor != null) {
				for (BulkMaterial eachOutfeed : tempProcessor
						.getOutfeedEntityTypeList()) {
					//TODO sets the realized throughput to the minimum of throughput or realized amount... assuming the first processor returned

					//resolves infinity*0=NaN issue when input amount is infinity but processor's outfeed rate is zero 
					if (Tester.equalCheckTolerance(tempProcessor
							.getConverstionRate(eachOutfeed, infeedMaterial),
							0.0d)) {
						return;
					}
					double tempAmount= Tester.min(this.getFacility().getStockList()
							.getValueFor(eachOutfeed, 2),amount* tempProcessor.getConverstionRate(
											eachOutfeed,infeedMaterial)+ this.getFacility()
									.getStockList().getValueFor(eachOutfeed,13));
					this.getFacility().setStocksList(eachOutfeed,13,tempAmount);
					this.updateRealizedProduction(eachOutfeed, tempAmount);
				}
			}
		}
	}
	
	public void adjustMutuallyExclusiveProcessesDemands(BulkMaterial infeedMaterial, double infeedAmount){
		// TODO when a contract is established for one of the mutually exclusive processes, unsatisfied demand
		// for infeeds of all other processes are adjusted
		// TODO assuming infeed material is unique to the process whose mutually exclusive processes are adjusted
		//find the infeed in process and adjust stocklist amounts for the mutually exclusive processes
		for (ArrayList<ProcessingRoute> eachProcessingRoute: this.getProcessingRoutesListOutfeed().getValues()) {
			for (ProcessingRoute each: eachProcessingRoute) {
				if(each.getProcessor().getHandlingEntityTypeList().contains(infeedMaterial) &&
						!this.getMutuallyExclusiveProcesses(each.getProcessor()).isEmpty()){
					// WARNING! mutually exclusive processes use the same throughput schedule
					//TODO assumes only one infeed is used!
					// amount of primary product potentially produced by the mutually exclusive process
					 double outfeedAmount = infeedAmount* each.getProcessor().getConverstionRate(each.getProcessor().getPrimaryProduct(),infeedMaterial);
																	
					for(ProcessingRoute eachMutuallyExclusiveRoute: this.getFacility().getOperationsManager().getMutuallyExclusiveProcesses(each.getProcessor())){
						if (each != eachMutuallyExclusiveRoute && Tester.greaterCheckTolerance(this.getFacility().getStockList().getValueFor((BulkMaterial) eachMutuallyExclusiveRoute.getProcessor().getHandlingEntityTypeList()
											.get(0),3), 0.0d)) {
							//TODO assumes only one infeed is used!
							double amountToRemove = outfeedAmount* eachMutuallyExclusiveRoute.getProcessor()
									.getConverstionRate(((BulkMaterial) eachMutuallyExclusiveRoute.getProcessor().getHandlingEntityTypeList().get(0)),eachMutuallyExclusiveRoute.getProcessor().getPrimaryProduct());
							
							eachMutuallyExclusiveRoute.getProcessor().getFacility().setStocksList(
									(BulkMaterial) eachMutuallyExclusiveRoute.getProcessor().getHandlingEntityTypeList()
										.get(0),1,Tester.max(0.0d,eachMutuallyExclusiveRoute.getProcessor().getFacility().getStockList().getValueFor((BulkMaterial) eachMutuallyExclusiveRoute.getProcessor().getHandlingEntityTypeList()
										.get(0),1)-amountToRemove));
							eachMutuallyExclusiveRoute.getProcessor().getFacility().setStocksList(
										(BulkMaterial) eachMutuallyExclusiveRoute.getProcessor().getHandlingEntityTypeList()
											.get(0),3,Tester.max(0.0d,eachMutuallyExclusiveRoute.getProcessor().getFacility().getStockList().getValueFor((BulkMaterial) eachMutuallyExclusiveRoute.getProcessor().getHandlingEntityTypeList()
											.get(0),3)-amountToRemove));
						}
					}
				}
			}
		}
	}
	
	/**
	 * plans production for all processing routes. It should be run after resetplannedstocks method.
	 */
	public void planProduction(){
		
		for (BulkMaterial eachOutfeed: processingRoutesListOutfeed.getKeys()) {
			// if there is a throughput file defined for this outfeed, plan production for the whole chain based on the throuhput schedule of the last outfeed in the chain
			// TODO check to see if this actually adds throughput and target demand for the processors that outfeed multiple processing routes
			for (ProcessingRoute processingRoute: processingRoutesListOutfeed.get(eachOutfeed)) {
				// get the forward and backward processing route and plan from the end (usefule if it's a middle product)
				LinkedList<ProcessingRoute> tempList = this.getChainProcessingRoutes(eachOutfeed,false);
				tempList.addAll(this.getChainProcessingRoutes(eachOutfeed,true));
				while(!tempList.isEmpty()){
					this.planProduction(tempList.pollLast(),
						SimulationManager.getLastPlanningTime(),SimulationManager.getNextPlanningTime());
				}
			}
		}
				
		this.scheduleProcess(SimulationManager.getPlanningHorizon(), 2, new ReflectionTarget(this, "planProduction"));
	}

	/**
	 * Sets target throughput/demand in facility's stocklist for all the products in the 
	 * processing routes based on the throughput of the production driving product.  
	 */
	public void planProduction(ProcessingRoute processingRoute, double startTime, double endTime){
		// TODO assuming mutually exclusive processing routes use the same throughput schedule
		if(processingRoute.getLastPlannedTime() == this.getSimTime())
			return;
		
		double throughput = processingRoute.getProcessor().getThroughput(startTime,endTime);
		double tempAmount;
		
		//TODO all logic assumes that mutually exclusive processes have the same outfeed with similar rates
		for (BulkMaterial eachMaterial : processingRoute.getOutfeedMaterial()) {
			tempAmount = processingRoute.getCapacityRatio(eachMaterial,
					processingRoute.getProcessor().getPrimaryProduct()) * throughput;
			this.getFacility().addToStocksList(eachMaterial, 2, tempAmount);
			//set purchase price 0
			this.getFacility().setStocksList(eachMaterial, 10, 0.0d);
		}
		if(this.getMutuallyExclusiveProcesses(processingRoute.getProcessor()).isEmpty()){
			for (BulkMaterial eachMaterial : processingRoute.getInfeedMaterial()) {
				tempAmount = processingRoute.getCapacityRatio(eachMaterial,
						processingRoute.getProcessor().getPrimaryProduct()) * throughput;
				this.getFacility().addToStocksList(eachMaterial, 1, tempAmount);
				this.getFacility().addToStocksList(eachMaterial, 3, tempAmount);
				//set purchase price 0
				this.getFacility().setStocksList(eachMaterial, 10, 0.0d);
			}	
			processingRoute.setLastPlannedTime(this.getSimTime());
		}else{
			
			//TODO assumes infeeds for mutually exclusive processes are completely different
			//TODO change to enable having one processing route accept various infeed material!
			for (ProcessingRoute eachProcessor : this.getMutuallyExclusiveProcesses(processingRoute.getProcessor())) {
				for (BulkMaterial eachMaterial : eachProcessor.getInfeedMaterial()) {
					tempAmount = eachProcessor.getCapacityRatio(eachMaterial,
						eachProcessor.getProcessor().getPrimaryProduct()) * throughput;
					this.getFacility().addToStocksList(eachMaterial, 1, tempAmount);
					this.getFacility().addToStocksList(eachMaterial, 3, tempAmount);
					//set sell price 0
					this.getFacility().setStocksList(eachMaterial, 10, 0.0d);
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
