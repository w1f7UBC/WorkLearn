package com.ROLOS.Logistics;

import java.util.ArrayList;

import com.ROLOS.ROLOSEntity;
import com.ROLOS.Utils.ThreeLinkedLists;
import com.ROLOS.Utils.TwoLinkedLists;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.EntityListListInput;
import com.sandwell.JavaSimulation.ErrorException;
import com.jaamsim.input.Input;

import com.sandwell.JavaSimulation.IntegerInput;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Tester;

/**
 * LinkedEntity is the superclass of all connectable entities
 */

public class LinkedEntity extends LogisticsEntity {

	private static final ArrayList<LinkedEntity> allInstances;
	 
	@Keyword(description = "The next entity(ies) recieving the handeled entity. Dir1 is finish to end, Dir2 is end to finish." +
			" two way routes should define next linkedentities at both ends. This is used for discrete entities only, bulk handling equipment should use Outfeeds keyword.", 
			example = "Route1 Next { Route2 Route3 }, Stacker1 Next { Stockpile1 Stockpile2 }")
	private final EntityListInput<LinkedEntity> nextLinkedEntityList;
	
	@Keyword(description = "This is only used for loading bays and bulk handling equipment. and identifies the next"
			+ "linked entity which this entity outfeeds its bulk material to.", 
			example = "LoadingBay1 Outfeeds { Dumper1 }, Stacker1 Outfeeds { Stockpile1 Stockpile2 }")
	private final EntityListInput<LinkedEntity> outfeedLinkedEntityList;
	
	@Keyword(description = "The type of entity(ies) that this Linked entity handels.",
			example = "Road1 Handles { AllTrucks } or Stockpile1 Handles { WoodChip }")
	private final EntityListInput<LogisticsEntity> handlingEntityTypeList;
	
	@Keyword(description = "the group of entities that have a common capacity. this should be set if capacity by entity type is set, then each group of entities "
			+ "for which a capacity is set in the capacityByEntityType should be put in braces. for example if LoadingArea1 CapacityGroups { { AllTrucks } { AllLoaders } } "
			+ "and LoadingArea1 CapacityByEntityType { 1 1 }, then 1 truck and 1 loader can be handled at a time", 
			example = "ShipGenerator1 CapacityGroups { AllShip } or ShipGenerator1 CapacityGroups { { AllTrucks } { AllLoaders } }")
	private final EntityListListInput<LogisticsEntity> capacityGroups;
	
	@Keyword(description = "Maximum number (capacity) of entities that this LinkedEntity can handle. If capacityByEntityType is set, capacity can be ommited but if defined"
			+ "will act as the total capacity; it should also be greater than each capacitybyEntityType individually",
			example = "Road1 Capacity { 100 } or Stockpile1 Capacity { 100 kt }")
	private final ValueInput capacity;
	
	@Keyword(description = "Maximum number (capacity) of each entity that this LinkedEntity handles. User should pay attention when setting both capacity" +
			"and CapacityByEntityType together. Remaining capacity at each time is the minimum of total remaining capacity and each entity's remaining capacity." +
			"For each of the entities apearing in \"Handles\" a respective capacity should be defined.", 
			example = "Road1 CapacityByEntityType { 2 1 3 } or Mixer CapacityByEntityType { 20 30 m3 }")
	private final ValueListInput capacityByEntityType;	
	
	@Keyword(description = "Origin priority for when this linked entity is the origin in discrete handling cases. " +
			"The RouteManager uses this priority.", 
			example = "LoadingArea1 OriginPriority { 10 }")
	private final IntegerInput originPriority;
	
	@Keyword(description = "Destination priority for when this linked entity is the destination in discrete handling cases" +
			"The RouteManager uses this priority.",
			example = "Berth1 DestinationPriority { 10 } ")
	private final IntegerInput destinationPriority;
	
	private final ArrayList<LinkedEntity> previousLinkedEntityList,infeedEntityList;
	private final TwoLinkedLists<LogisticsEntity> currentlyHandlingList;		
	// following list correspond to the handlingList
	private final ArrayList<Double> entityAmountProcessed; 						// this list corresponds to the handledEntityTypeList

	//???
	private final ThreeLinkedLists<LinkedEntity,LogisticsEntity> currentOriginList;			
	private final ThreeLinkedLists<LinkedEntity,LogisticsEntity> currentDestinationList;
	
	// ????
	private final ArrayList<LinkedEntity> originsList;
	private final ArrayList<LinkedEntity> prioritizedDestinationsList;
	private final ArrayList<LinkedEntity> prioritizedOriginsList;
	
	// TODO this is used mainly to trigger processing entities. deprecate when states are figured out, State "Idle" may be a good candidate
	private boolean triggered;												// Checks this is currently checking its queue for handling entities
		
	static {
		allInstances = new ArrayList<LinkedEntity>();
	}

	{
		nextLinkedEntityList = new EntityListInput<LinkedEntity>(LinkedEntity.class, "Next", "Key Inputs", new ArrayList<LinkedEntity>(2));
		this.addInput(nextLinkedEntityList);
		
		outfeedLinkedEntityList = new EntityListInput<>(LinkedEntity.class, "Outfeeds", "Key Inputs", new ArrayList<LinkedEntity>(1));
		this.addInput(outfeedLinkedEntityList);
		
		handlingEntityTypeList = new EntityListInput<>(LogisticsEntity.class, "Handles", "Key Inputs", new ArrayList<LogisticsEntity>(1));
		this.addInput(handlingEntityTypeList);
		
		capacityGroups = new EntityListListInput<>(LogisticsEntity.class, "CapacityGroups", "Key Inputs", new ArrayList<ArrayList<LogisticsEntity>>());
		this.addInput(capacityGroups);
		
		capacity = new ValueInput("Capacity", "Key Inputs", 0.0d);
		capacity.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(capacity);
		
		capacityByEntityType = new ValueListInput("CapacityByEntityType", "Key Inputs", null);
		capacityByEntityType.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(capacityByEntityType);
		
		originPriority = new IntegerInput("SourcePriority", "Key Inputs", 0);
		originPriority.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(originPriority);
		
		destinationPriority = new IntegerInput("DestinationPriority", "Key Inputs", 0);
		destinationPriority.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(destinationPriority);

	}

	public LinkedEntity() {
		super();
		
		synchronized (allInstances) {
			allInstances.add(this);
		}
		
		// TODO figure out meaning of entity priority
		currentlyHandlingList = new TwoLinkedLists<>(2,new DescendingPriotityComparator<LogisticsEntity>(ROLOSEntity.class, "getInternalPriority"),0);
		entityAmountProcessed = new ArrayList<>(1);
		
		originsList = new ArrayList<LinkedEntity>(1);
		currentOriginList = new ThreeLinkedLists<>(1,new DescendingPriotityComparator<LinkedEntity>(LinkedEntity.class, "getOriginPriority"), new DescendingPriotityComparator<LogisticsEntity>(ROLOSEntity.class, "getInternalPriority"));
		currentDestinationList = new ThreeLinkedLists<>(1,new DescendingPriotityComparator<LinkedEntity>(LinkedEntity.class, "getOriginPriority"), new DescendingPriotityComparator<LogisticsEntity>(ROLOSEntity.class, "getInternalPriority"));
		prioritizedOriginsList = new ArrayList<>(1);
		prioritizedDestinationsList = new ArrayList<>(1);				

		previousLinkedEntityList = new ArrayList<>(1);	
		infeedEntityList = new ArrayList<>(1);
	}
	
	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);
		
		if (in == handlingEntityTypeList){
			if(this.handlesBulkMaterial()){ 
				capacity.setUnitType(handlingEntityTypeList.getValue().get(0).getEntityUnit());
				capacityByEntityType.setUnitType(handlingEntityTypeList.getValue().get(0).getEntityUnit());
				try{
					((BulkHandlingLinkedEntity)this).setRateUnits(handlingEntityTypeList.getValue().get(0).getEntityFlowUnit());
					((BulkMaterialStorage)this).setContentUnits(this.getHandlingEntityUnit());
				} catch(ClassCastException e){}
				try{
					((Stockpile)this).setReclaimableAmountUnits(handlingEntityTypeList.getValue().get(0).getEntityUnit());					
				}catch(ClassCastException e){}		
				try{
				//	((Facility)this).setUnits(handlingEntityTypeList.getValue().get(0).getEntityFlowUnit());					
				}catch(ClassCastException e){}
			}	
		}
		
		if(in == nextLinkedEntityList){
			// populate previousLinkedEntityAssertion
			for (LinkedEntity each : nextLinkedEntityList.getValue()) {
				each.addToPreviousLinkedEntityList(this);
			}
			try{
				
			}catch(ClassCastException e){}	
		}
		
		if(in == outfeedLinkedEntityList){
			// populate infeedEntityAssertion
			for (LinkedEntity each : outfeedLinkedEntityList.getValue()) {
				each.addToInfeedEntityList(this);
			}
		}	
	}

	@Override
	public void validate() {

		super.validate();
		
		// initiate the entityAmountProcessed; 
		for (int i=0; i< handlingEntityTypeList.getValue().size();i++) {
			entityAmountProcessed.add(0.0d);	
		}
				
		if (this.getHandlingEntityTypeList().isEmpty() && !(this instanceof Facility))
			throw new ErrorException ("Handles keyword must be set for %s",this.getName());
				
		if (capacityByEntityType.getValue() !=null){
			if (capacityByEntityType.getValue().size()!=capacityGroups.getValue().size())
				throw new ErrorException("%s and %s must be of equal size for %s", capacityGroups.getKeyword(),capacityByEntityType.getKeyword(), this.getName());
			for (int i=0; i< capacityByEntityType.getValue().size();i++){
				if (capacityByEntityType.getValue().get(i) > capacity.getValue())
					throw new ErrorException("Capacity for %s is higher than total Capacity that %s handles", handlingEntityTypeList.getValue().get(i),this.getName());
			}
		}		
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		
		triggered = false;		
	}

	public static ArrayList<? extends LinkedEntity> getAll() {
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
	// BREAKDOWN AND MAINTENANCE METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////
	// ADDER METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	public <T extends LinkedEntity> void addToPreviousLinkedEntityList(T ent){
		previousLinkedEntityList.add(ent);
	}
	
	public <T extends LinkedEntity> void addToInfeedEntityList(T ent){
		infeedEntityList.add(ent);
	}
	public <T extends LinkedEntity> void addToOriginsList(T origin){
		if (originsList.contains(origin)) return;
		originsList.add(origin);
	}
	
	public <T1 extends LinkedEntity,T2 extends LogisticsEntity> void addToCurrentOriginList(T1 origin,T2 passedEntity,  double receivedAmountFromOrigin) {
		currentOriginList.add(origin, passedEntity, 0, receivedAmountFromOrigin);
	}
	
	public <T1 extends LinkedEntity,T2 extends LogisticsEntity> void addToCurrentDestinationList(T1 destination,T2 passedEntity,  double sentAmountToDestination) {
		currentDestinationList.add(destination, passedEntity, 1, sentAmountToDestination);
	}
	
	public <T extends LogisticsEntity> void addToEntityAmountProcessed(T discreteEnt) {
		LogisticsEntity ent = discreteEnt.getProtoTypeEntity();
		
		if (handlingEntityTypeList.getValue().contains(ent)) {
			int index = handlingEntityTypeList.getValue().indexOf(ent);
			entityAmountProcessed.set(index,entityAmountProcessed.get(index)+1);
		} else {
			throw new ErrorException("%s does not handle %s! at time: %f attempt was made to add amount for this entity.",this.getName(),ent.getName(),this.getCurrentTime());
		}
	}
	
	public <T extends LogisticsEntity> void addToEntityAmountProcessed(T bulkEntity, double amountToAdd) {
		if (handlingEntityTypeList.getValue().contains(bulkEntity)) {
			int index = handlingEntityTypeList.getValue().indexOf(bulkEntity);
			entityAmountProcessed.set(index,entityAmountProcessed.get(index)+amountToAdd);
		} else {
			throw new ErrorException("%s does not handle %s! at time: %f attempt was made to add amount for this entity.",this.getName(),bulkEntity.getName(),this.getCurrentTime());
		}
	}

	/**
	 * This method does not check internal consistency such as "isReadyToHandle", or capacity. 
	 */
	public <T extends LogisticsEntity> void addToCurrentlyHandlingEntityList(T entityToAdd, double amountToAdd) {
		
		currentlyHandlingList.add(entityToAdd, 0, amountToAdd);
		entityToAdd.addToCurrentHandlersList(this,amountToAdd);	
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	// UPDATER METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	// REMOVER METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	
	public <T extends LogisticsEntity> void setEntityAmountProcessed(T entity, double amount) {
		LogisticsEntity ent = entity.getProtoTypeEntity();
		
		if (handlingEntityTypeList.getValue().contains(ent)) {
			int index = handlingEntityTypeList.getValue().indexOf(ent);
			entityAmountProcessed.set(index,amount);
		} else {
			throw new ErrorException("%s does not handle %s! at time: %f attempt was made to reset amount processed for this entity.",this.getName(),ent.getName(),this.getCurrentTime());
		}
	}
		
	/** 
	 * it updates the currentlyHandledByEntityList of the removed entity
	 */
	public <T extends LogisticsEntity> void removeFromCurrentlyHandlingEntityList(T entityToRemove, double amountToRemove) {
		int index = currentlyHandlingList.indexOf(entityToRemove);
		
		currentlyHandlingList.remove(entityToRemove, 0, amountToRemove);
		entityToRemove.removeFromCurrentHandlersList(this,amountToRemove);
		
	}
		
	public <T1 extends LinkedEntity, T2 extends LogisticsEntity> void removeFromCurrentOriginList (T1 origin, T2 entityToRemove, double amountToRemove){
		currentOriginList.remove(origin, entityToRemove, 0, amountToRemove); 
	}
	
	public <T1 extends LinkedEntity, T2 extends LogisticsEntity> void removeFromCurrentDestinationList (T1 destination,T2 entityToRemove, double amountToRemove){
		currentDestinationList.remove(destination, entityToRemove, 0, amountToRemove); 
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// ASSERTION METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public boolean handlesBulkMaterial() {
		if(!handlingEntityTypeList.getValue().isEmpty() && handlingEntityTypeList.getValue().get(0).isBulk()){
			return true; 
		} else {
			return false;
		}
	}
	
	public <T extends LogisticsEntity> boolean checkIfHandles(T entity) {
		if(entity == null)
			return true;
		
		LogisticsEntity ent = entity.getProtoTypeEntity();
		
		return this.getHandlingEntityTypeList().contains(ent);
	}
	
	public boolean isTriggered() {
		return triggered;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// SETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////
		
	public void setTriggered(boolean triggered) {
		this.triggered = triggered;
		
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	//GETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////

	public Enum<Entity_State> getHandlingEntityMatterState() {
		return handlingEntityTypeList.getValue().get(0).getEntityMatterState();
	}

	public Class<? extends Unit> getHandlingEntityUnit() {
		return handlingEntityTypeList.getValue().get(0).getEntityUnit();
	}
		
	public ArrayList<LogisticsEntity> getHandlingEntityTypeList() {
		return handlingEntityTypeList.getValue();
	}
	
	/**
	 * @return the list of entities in the braces of Handles keyword that contains ent.
	 */
	public <T extends LogisticsEntity> ArrayList<LogisticsEntity> getCapacityGroupFor(T ent){
		ArrayList<LogisticsEntity> returnList = new ArrayList<>(1); 
		for (ArrayList<LogisticsEntity> each: capacityGroups.getValue()){
			if (each.contains(ent)){
				for (LogisticsEntity entity : each){
					returnList.add(entity);
				}
			}
		}
		return returnList;	
	}
	
	/**
	 * @return the index of list of entities in the braces of Handles keyword that contains ent. -1 if list does not contain ent;
	 */
	public <T extends LogisticsEntity> int getCapacityGroupIndex(T ent){
		for (ArrayList<LogisticsEntity> each: capacityGroups.getValue()){
			if (each.contains(ent)){
				return capacityGroups.getValue().indexOf(each);
			}
		}
		return -1;	
	}

	public ArrayList<? extends LinkedEntity> getNextLinkedEntityList() {
		return nextLinkedEntityList.getValue();
	}
	
	public ArrayList<? extends LinkedEntity> getOutfeedLinkedEntityList() {
		return outfeedLinkedEntityList.getValue();
	}
	
	public ArrayList<? extends LinkedEntity> getPreviousLinkedEntityList() {
		return previousLinkedEntityList;
	}
	
	public ArrayList<? extends LinkedEntity> getInfeedEntityList() {
		return infeedEntityList;
	}
	
	/**
	 * @return the ranked origins list based on source priorities
	 */
	public ArrayList<? extends LinkedEntity> getOriginsList(){
		return prioritizedOriginsList;
	}
	

	/**
	 * @return the ranked destinations list based on destination priorities
	 */
	public ArrayList<? extends LinkedEntity> getDestinationsList() {
		return prioritizedDestinationsList;
	}

	public ArrayList<? extends LinkedEntity> getCurrentOriginList() {
		return currentOriginList.getFirstEntityList();
	}

	public ArrayList<? extends LinkedEntity> getCurrentDestinationList() {
		return currentDestinationList.getFirstEntityList();
	}

	public ArrayList<? extends LogisticsEntity> getCurrentOriginEntityList() {
		return currentOriginList.getSecondEntityList();
	}

	public ArrayList<Double> getCurrentOriginAmountList() {
		return currentOriginList.getValueList(0);
	}

	public ArrayList<? extends LogisticsEntity> getCurrentDestinationEntityList() {
		return currentDestinationList.getSecondEntityList();
	}

	public ArrayList<Double> getCurrentDestinationAmountList() {
		return currentDestinationList.getValueList(0);
	}
	
	/**
	 * this method does not check for generated entities and assumes that parent entities are stored in both destination and passed as argument
	 */
	public <T1 extends LinkedEntity, T2 extends LogisticsEntity> double getCurrentEntityAmountForOrigin(T1 origin,T2 entity){
		return currentOriginList.getValueFor(origin, entity,0);
	}
	
	/**
	 * this method does not check for generated entities and assumes that parent entities are stored in both destination and passed as argument
	 */
	public <T1 extends LinkedEntity, T2 extends LogisticsEntity> double getCurrentEntityAmountForDestination(T1 destination,T2 entity){
		return currentDestinationList.getValueFor(destination, entity,0);
	}

	public int getOriginPriority() {
		return originPriority.getValue();
	}

	public int getDestinationPriority() {
		return destinationPriority.getValue();
	}
	
	/**
	 * SHOULD BE OVERWRITTEN BY THE IMPLEMETNING CLASS
	 * @return the amounf of the entity that this linked entity can handle at present time
	 */
/*	public <T1 extends LinkedEntity,T2 extends GeneratableEntity> double getReservableCapacity(T1 origin,T1 destination, T2 entity) {
		return 0;
	}
*/
	
	/**
	 * @return remaining capacity for ent. if capacity by entity type is defined, it assumes that the group of entities that ent is defined with
	 * in the capacity group braces has the defined capacity in total.
	 * if capacity and capacityByEntityType are set together, the remaining capacity will be the minimum of total capacity minus total currently
	 * handling amount and the remaining capacity of ent group.
	 * if entitiy is not appearing in the capacity group, the total remaining capacity will be returned
	 */
	public <T extends LogisticsEntity> double getRemainingCapacity(T ent) {
		double tempRemainingCapacity;
		tempRemainingCapacity = capacity.getValue() - this.getCurrentlyHandlingAmount();
		if (capacityByEntityType.getValue() != null){
			int i = this.getCapacityGroupIndex(ent);
			if(i >-1){
				ArrayList<LogisticsEntity> groupList = this.getCapacityGroupFor(ent);
				tempRemainingCapacity = Tester.min(tempRemainingCapacity,capacityByEntityType.getValue().get(i));
				for (LogisticsEntity each: groupList){
					tempRemainingCapacity -= this.getCurrentlyHandlingAmount(each);
				}
			}
		}
		if (Tester.lessCheckTolerance(tempRemainingCapacity, 0)) {
			throw new ErrorException("Negative remaining capacity caught in %s at time: %f", this.getName(),this.getCurrentTime());
		}

		return tempRemainingCapacity;
	}
	
	public <T extends LogisticsEntity> double getReservableAmountForRemoving(T ent){
		return this.getCurrentlyHandlingAmount(ent) - this.getCurrentlyReservedAmount(ent);
	}
	
	public <T extends LogisticsEntity> double getReservableAmountForAdding(T ent){
		return this.getRemainingCapacity(ent) + this.getCurrentlyReservedAmount(ent);
	}
	
	public double getCapacity() {
		return capacity.getValue();
	}
	
	/**
	 * @return if proto type for the entity is defined, it returns capacity for the proto type entity. also if capacityByEntityType is set, capacity for the
	 * whole group in which entity is defined will be returned.
	 */
	public <T extends LogisticsEntity> double getCapacity(T entity) {
				
		LogisticsEntity ent = entity.getProtoTypeEntity();
		
		if (capacityByEntityType.getValue() == null) {
			if (handlingEntityTypeList.getValue().contains(ent)){
				return capacity.getValue();
			}
		}
		else{
			int index = this.getCapacityGroupIndex(ent);
			return capacityByEntityType.getValue().get(index);
		}
		
		return 0;
	}

	public <T extends LogisticsEntity> double getEntityAmountProcessed(T entity) {
		LogisticsEntity ent = entity.getProtoTypeEntity();
		
		if (handlingEntityTypeList.getValue().contains(ent)) {
			return entityAmountProcessed.get(handlingEntityTypeList.getValue().indexOf(ent));
		}
		return 0;
	}
	
	public ArrayList<Double> getEntityAmountProcessedList(){
		return entityAmountProcessed;
	}
	
	public <T extends LogisticsEntity> double getCurrentlyHandlingAmount(T entity) {
		LogisticsEntity ent = entity.getProtoTypeEntity();
		double tempAmount = 0;
		for (LogisticsEntity each: currentlyHandlingList.getEntityList()) {
			if (each.getProtoTypeEntity() == ent){
					tempAmount += currentlyHandlingList.getValueFor(each, 0);
			}
		}
			return tempAmount;
	}
	
	public <T extends LogisticsEntity> double getCurrentlyReservedAmount(T entity) {
		LogisticsEntity ent = entity.getProtoTypeEntity();
		double tempAmount = 0;
		for (LogisticsEntity each: currentlyHandlingList.getEntityList()) {
			if (each.getProtoTypeEntity() == ent){
				tempAmount += currentlyHandlingList.getValueFor(each, 1);
			}
		}
			return tempAmount;
	}
	
	/**
	 * @return Total amount of currently handling amount regardless of entity type
	 */
	public double getCurrentlyHandlingAmount() {
		double tempAmount = 0;
		for (double each : currentlyHandlingList.getValueLists().get(0)){
			tempAmount += each;
		}
		return tempAmount;
	}
	
	/**
	 * <b> 0 </b> currently handling amount
	 * <br> <b> 1 </b>  currently reserved amount
	 */
	public TwoLinkedLists<LogisticsEntity> getCurrentlyHandlingList() {
		return currentlyHandlingList;
	}
	
}
