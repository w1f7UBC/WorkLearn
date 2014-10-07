package com.ROLOS.Logistics;

import java.util.ArrayList;
import java.util.Comparator;

import com.ROLOS.ROLOSEntity;
import com.ROLOS.Utils.HandyUtils;
import com.ROLOS.Utils.PriorityQueue;
import com.ROLOS.Utils.ThreeLinkedLists;

import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.ReflectionTarget;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;


import com.jaamsim.input.Keyword;

public class DiscreteHandlingLinkedEntity extends LinkedEntity {
	private static final ArrayList<DiscreteHandlingLinkedEntity> allInstances;
	
	private PriorityQueue<LogisticsEntity> queuedEntitiesList;			//Queue used for discrete entities only. Entities added by infeed waiting to be processed; 
	
	// TODO enter and exit delays apply to both ends. refactor to include direction and possibly destinations for applying enter or exit delays.
	@Keyword(description = "EnterDelay applies when this entity wants to start processing an entity "
			+ "(applied to every entity unless specific entities that are passed as argument are defined in the delay).",
	         example = "Silverdale  EnterDelay { SilverdaleEnterDelay }")
	private final EntityListInput<Delay> enterDelay;
	
	@Keyword(description = "ExitDelay is when this entity whants to finish processing an entity "
			+ "(applied to every entity unless specific entities that are passed as argument are defined in the delay).",
	         example = "Silverdale  ExitDelay { Loader1PreLoadingDelayTimeSeries }")
	private final EntityListInput<Delay> exitDelay;
	
	/**
	 * dijkstra comparator list is implemented as a three linked list. the first list keeps dijkstra's previous and the second list keeps dijkstra's value
	 */
	private ThreeLinkedLists<DijkstraComparator,DiscreteHandlingLinkedEntity> dijkstraComparatorList;
	
	static {
		allInstances = new ArrayList<DiscreteHandlingLinkedEntity>(100);
	}
	
	{
		enterDelay = new EntityListInput<Delay>(Delay.class, "EnterDelay", "Key Inputs", null);
		this.addInput(enterDelay);
		
		exitDelay = new EntityListInput<Delay>(Delay.class, "ExitDelay", "Key Inputs", null);
		this.addInput(exitDelay);
	}

	public DiscreteHandlingLinkedEntity() {
		super();
		
		synchronized (allInstances) {
			allInstances.add(this);
		}
		
		// See if passing null for comparators work!
		dijkstraComparatorList = new ThreeLinkedLists<>(1, new Comparator<DijkstraComparator>() {
			@Override
			public int compare(DijkstraComparator o1, DijkstraComparator o2) {
				return 0;
			}
		}, new DescendingPriotityComparator<DiscreteHandlingLinkedEntity>(ROLOSEntity.class, "getInternalPriority"));
		//TODO figure out meaning of queued entities for this!
		queuedEntitiesList = new PriorityQueue<>(new DescendingPriotityComparator<LogisticsEntity>(ROLOSEntity.class,"getInternalPriority"));
	}

	@Override
	public void validate() {
		super.validate();
		
	}

	@Override
	public void earlyInit() {
		// TODO Auto-generated method stub
		super.earlyInit();

		// initializing the bulkHandlingNetworkManager if not yet initialized
	/*	if (!RouteManager.transportationNetworkManager.isInitialized()) 
			RouteManager.transportationNetworkManager.initialize();
			*/
	}

	public static ArrayList<? extends DiscreteHandlingLinkedEntity> getAll() {
		synchronized (allInstances) {
			return allInstances;
		}
	}

	@Override
	public void kill() {
		// TODO Auto-generated method stub
		super.kill();
		synchronized (allInstances) {
			allInstances.remove(this);
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	// DIJKSTRA'S HELPERS
	//////////////////////////////////////////////////////////////////////////////////////

	public ThreeLinkedLists<DijkstraComparator, DiscreteHandlingLinkedEntity> getDijkstraComparatorList(){
		return dijkstraComparatorList;	
	}
		
	public static class DijkstraComparator implements Comparator<DiscreteHandlingLinkedEntity>{

		public DijkstraComparator() {
		}
				
		@Override
		public int compare(DiscreteHandlingLinkedEntity o1, DiscreteHandlingLinkedEntity o2) {
			return Double.compare(o1.getDijkstraComparatorList().getValueListFor(this, 0).get(0), o2.getDijkstraComparatorList().getValueListFor(this, 0).get(0));
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	// PROCESSING ENTITIES METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	/**
	 * MUST BE OVERWRITTEN in entities which process entities of some sort
	 * cycling through queued entities and Enterdelay should be set at the subclass level
	 */
	public void startProcessingQueuedEntities() {
		if (this.isTriggered()) {
			throw new ErrorException("%s is starting to process entities and should not be triggered already at time:%f", this.getName(), this.getCurrentTime());
		}
		
		this.setTriggered(true);
		
	}
	
	/**
	 * @param nextLinkedEntity The next linked entity that the moving entity should be passed to
	 */
	public <T extends MovingEntity> void finishProcessingEntity (T entity) {

		//TODO refactor for multiple exit delays
		if(exitDelay.getValue() != null){
			for (Delay eachDelay: exitDelay.getValue()) {
				entity.setPresentState("Exit Delay - "
						+ eachDelay.getName());
				this.simWait(eachDelay.getNextDelayDuration(entity));
			}	
		}
		
		this.addToEntityAmountProcessed(entity);

		entity.setPresentState("Idle");
		
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	// BREAKDOWN AND MAINTENANCE METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	

	//////////////////////////////////////////////////////////////////////////////////////
	// ADDER METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Queue for discrete entities waiting to be processed by this linked entity.
	 * @param entity
	 */
	public <T extends MovingEntity> void addToQueuedEntityList(T entity) {
		if(queuedEntitiesList.contains(entity))
			return;
		
		queuedEntitiesList.add(entity);
		entity.setPresentState("Queued");
		if (!this.isTriggered()) {
			this.startProcess(new ReflectionTarget(this,"startProcessingQueuedEntities"));
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	// UPDATER METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	// REMOVER METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Priority to remove from the list should be checked through the traffic control manager.
	 */
	public <T extends MovingEntity> void removeFromQueuedEntityList(T discreteEntity) {
		queuedEntitiesList.remove(discreteEntity);
	}
	
	@Override
	public <T extends LogisticsEntity> void removeFromCurrentlyHandlingEntityList(
			T entityToRemove, double amountToRemove) {
		// set idle or trigger 
		if (this.getCurrentlyHandlingList().size() ==1)
				this.setPresentState("Idle");
		else
			this.updateStatesReport();
		
		super.removeFromCurrentlyHandlingEntityList(entityToRemove, amountToRemove);
		
		if(entityToRemove instanceof MovingEntity && !this.getQueuedEntitiesList().isEmpty()){
			if (!this.isTriggered())
				this.startProcess(new ReflectionTarget(this,"startProcessingQueuedEntities"));
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// ASSERTION METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public <T extends MovingEntity> boolean isReadyToHandle(T ent){
		if (this.getRemainingCapacity(ent) >  0){
			return true;
		} else 
			return false;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// SETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	//GETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	public ArrayList<? extends LogisticsEntity> getQueuedEntitiesList() {
		return queuedEntitiesList.getList();
	}
	
	public ArrayList<Delay> getEnterDelay(){
		return enterDelay.getValue();
	}
	
	public ArrayList<Delay> getExitDelay(){
		return exitDelay.getValue();
	}
	
	public double getLength() {
		return 0;
	}
	
	/**
	 * 
	 * @return travel time of movingEntity based on maximum allowable speed and internal speed of the movingEntity
	 */
	public double getTravelTime(MovingEntity movingEntity) {
		return 0;
	}
	
	/**
	 * 
	 * @return travel cost of the moving entity based on its operating cost. 
	 */
	public double getTravelCost(MovingEntity movingEntity) {
		return 0;
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////
	// REPORTING
	// ////////////////////////////////////////////////////////////////////////////////////
	@Override
	public boolean setPresentState(String state) {
		boolean reportPrinted = false;
		if (super.setPresentState(state)) {
			// add bottom line for states report
			stateReportFile.putStringTabs(HandyUtils.arraylistToString(this.getCurrentlyHandlingList().getEntityList()), 1);
			
			reportPrinted = true;
		}
			
		//update dependent entities states
		/*if (!externalCall){
			for (GeneratableEntity each: this.getCurrentlyHandlingEntityList()){
				each.setPresentState(each.getPresentState(), true);
			}
		}
		*/				
		return reportPrinted;
	}
		
	@Override
	public void printStatesHeader() {
		// TODO Auto-generated method stub
		super.printStatesHeader();
		stateReportFile.putStringTabs("Handling Entities List", 1);

	}
		
	@Override
	public void printStatesUnitsHeader() {
		// TODO Auto-generated method stub
		super.printStatesUnitsHeader();
		stateReportFile.putTabs(1);
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////
	// OUTPUTS
	// ////////////////////////////////////////////////////////////////////////////////////
	
	@Output(name = "QueuedEntities",
	 description = "List of queued entities.",
	 	unitType = DimensionlessUnit.class)
	public String getQueuedEntities(double simtime) {
		return queuedEntitiesList.getList().toString();
	}

}
