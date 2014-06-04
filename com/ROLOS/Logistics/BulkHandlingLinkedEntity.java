package com.ROLOS.Logistics;

import java.util.ArrayList;

import com.ROLOS.ROLOSEntity;
import com.ROLOS.Utils.HandyUtils;
import com.ROLOS.Utils.ThreeLinkedLists;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.ErrorException;

import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Vec3dInput;
import com.sandwell.JavaSimulation3D.DisplayEntity;

/**
 * Bulk material handling routes originate from a loader/unloader or a processor and are destined at a loader/unloader or a processor. 
 * Stock piles or bulk cargos are not can not be origin or destination.
 * @author Saeed
 */

public class BulkHandlingLinkedEntity extends LinkedEntity {
	private static final ArrayList<BulkHandlingLinkedEntity> allInstances;
	
	@Keyword(description = "Maximum rate for bulk handling LinkedEntities. User should pay attention when setting both Rate" +
			"and RateByEntityType together. ", 
			example = "Conveyor1 MaxRate { 6 t/h } ")
	private final ValueInput rate;
	
	@Keyword(description = "Maximum rate of each entity that this bulk handling LinkedEntity handles." +
			"For each of the entities apearing in Handles a respective rate should be defined.", 
			example = "Conveyor1 RateByEntityType { 20 30 m3/h }")
	private final ValueListInput rateByEntityType;	
	
	@Keyword(description = "the inteternal size of a container, x and y are the limits of the rectangle"
			+ "base and z is the maximum height for the material loaded on to this cargo.",
			example = "TruckContainer CargoDimensions {10 2 2 m} stockpile CargoDimensions { 10 10 5 m }")
	private final Vec3dInput cargoDimensions;

	@Keyword(description = "The allignment point of cargo for this bulkHandlingLinkedEntity. The x y content of this entity"
			+ "will be alligned based on these x ys and the bottom of the cargo will be set to the z.",
			example = "TruckContainer CargoAlignment {1 1 0.1 } ")
	private final Vec3dInput cargoAlignment;
	
	@Keyword(description = "Delay for connecting a new infeed. "
			+ "(applied to every entity unless specific entities that are passed as argument are defined in the delay).",
	         example = "Stockpile  InfeedConnectionDelay { StackingDelay }")
	private final EntityInput<Delay> infeedConnectionDelay;
	
	@Keyword(description = "Delay for connecting a new outfeed. "
			+ "(applied to every entity unless specific entities that are passed as argument are defined in the delay).",
	         example = "Stockpile  OutfeedConnectionDelay { ReclaimingDelay }")
	private final EntityInput<Delay> outfeedConnectionDelay;
	
	@Keyword(description = "Delay for disconnecting an infeed. "
			+ "(applied to every entity unless specific entities that are passed as argument are defined in the delay).",
	         example = "Tank1  InfeedDisconnectionDelay { InfeedpipeDisconnectDelay }")
	private final EntityInput<Delay> infeedDisconnectionDelay;
	
	@Keyword(description = "Delay for disconnecting an outfeed. "
			+ "(applied to every entity unless specific entities that are passed as argument are defined in the delay).",
	         example = "Stockpile  OutfeedDisconnectionDelay { ReclaimerMovementDelay }")
	private final EntityInput<Delay> outfeedDisconnectionDelay;	
	
	//Whether this equipment is a stationary in the facility. default is true unless it appears in the towable list of a moving entity.
	private boolean stationary;
	
	private final ThreeLinkedLists<BulkHandlingLinkedEntity, BulkMaterial> currentInfeedList;
	private final ThreeLinkedLists<BulkHandlingLinkedEntity, BulkMaterial> currentOutfeedList;

	private MovingEntity currentTow;
	private DisplayEntity currentContentDisplayEntity;
	
	static {
		allInstances = new ArrayList<BulkHandlingLinkedEntity>(20);
	}

	{		
		
		rate = new ValueInput("Rate", "Key Inputs", 0.0d);
		rate.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(rate);

		rateByEntityType = new ValueListInput("RateByEntityType", "Key Inputs", null);
		rateByEntityType.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(rateByEntityType);

		cargoDimensions = new Vec3dInput("CargoDimensions", "Basic Graphics", null);
		cargoDimensions.setUnitType(DistanceUnit.class);
		this.addInput(cargoDimensions);
		
		cargoAlignment = new Vec3dInput("CargoAlignment", "Basic Graphics", null);
		this.addInput(cargoAlignment);
		
		infeedConnectionDelay = new EntityInput<Delay>(Delay.class, "InfeedConnectionDelay", "Key Inputs", null);
		this.addInput(infeedConnectionDelay);
		
		outfeedConnectionDelay = new EntityInput<Delay>(Delay.class, "OutfeedConnectionDelay", "Key Inputs", null);
		this.addInput(outfeedConnectionDelay);
		
		infeedDisconnectionDelay = new EntityInput<Delay>(Delay.class, "InfeedDisconnectionDelay", "Key Inputs", null);
		this.addInput(infeedDisconnectionDelay);
		
		outfeedDisconnectionDelay = new EntityInput<Delay>(Delay.class, "OutfeedDisconnectionDelay", "Key Inputs", null);
		this.addInput(outfeedDisconnectionDelay);
		
	}
	
	public BulkHandlingLinkedEntity() {
		super();
		
		stationary = true;
		
		synchronized (allInstances) {
			allInstances.add(this);
		}
		
		//TODO figure out meaning of priority for infeed/outfeed and material when multiple entities are handled
		currentInfeedList = new ThreeLinkedLists<>(1,new DescendingPriotityComparator<BulkHandlingLinkedEntity>(LinkedEntity.class, "getOriginPriority"), new DescendingPriotityComparator<BulkMaterial>(ROLOSEntity.class, "getInternalPriority"),0);
		currentOutfeedList = new ThreeLinkedLists<>(1,new DescendingPriotityComparator<BulkHandlingLinkedEntity>(LinkedEntity.class, "getOriginPriority"), new DescendingPriotityComparator<BulkMaterial>(ROLOSEntity.class, "getInternalPriority"),0);
	}

	@Override
	public void validate() {

		super.validate();
		
		// check to see if each ratebytype is less than the total capacity
		if (rateByEntityType.getValue() !=null){
			for (int i=0; i< rateByEntityType.getValue().size();i++){
				if (rateByEntityType.getValue().get(i) > rate.getValue())
					throw new ErrorException("Rate for %s is higher than maximum rate that %s handles", this.getHandlingEntityTypeList().get(i),this.getName());
			}
		}
		
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

	}

	public static ArrayList<? extends LinkedEntity> getAll() {
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

	//////////////////////////////////////////////////////////////////////////////////////
	// ADDER METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	
	public void attachToTow (MovingEntity towingMovingEntity){
		
		if(currentTow != null)
			throw new ErrorException("%s is currently towed by %s and can not be attached to %s at time: %f", this.getName(),currentTow.getName(),towingMovingEntity.getName(), this.getCurrentTime());
		
		this.currentTow = towingMovingEntity;
		towingMovingEntity.getCurrentlyTowingEntityList().get(this.getClass());
		InputAgent.processEntity_Keyword_Value(this, "RelativeEntity", towingMovingEntity.getName());
	}
	
	public void setLoadingBay(LoadingBay loadingBay){
		for(BulkHandlingRoute eachRoute: loadingBay.getConnectableRoutesList()){
			if(eachRoute.getRouteSegments().contains(this))
				eachRoute.incrementConnectedSegmentsCount(1);
		}
	}
	
	
	public synchronized <T extends BulkHandlingLinkedEntity> void addToCurrentInfeed(T newInfeed, BulkMaterial newInfeedEntity, double newInfeedAmount){
		if(currentInfeedList.getValueFor(newInfeed, newInfeedEntity,0) == 0.0d){	
			//TODO refactor for multiple exit delays
			if(infeedConnectionDelay.getValue() != null){
				this.setPresentState("Connecting - "+ newInfeed.getName()+" - "+infeedConnectionDelay.getValue().getName());
				newInfeed.setPresentState("Connecting - "+ this.getName()+" - "+infeedConnectionDelay.getValue().getName());
				this.simWait(infeedConnectionDelay.getValue().getNextDelayDuration(newInfeed,newInfeedEntity));	
			}
		}
			
		currentInfeedList.add(newInfeed, newInfeedEntity,0, newInfeedAmount);
		this.setPresentState("Working");
//			this.balanceInfeedFor(newInfeed,newInfeedEntity);
		
	}
	
	public synchronized <T extends BulkHandlingLinkedEntity> void addToCurrentOutfeed(T newOutfeed, BulkMaterial newOutfeedEntity, double newOutfeedAmount){
		if(currentOutfeedList.getValueFor(newOutfeed, newOutfeedEntity,0) == 0.0d){
			//TODO refactor for multiple exit delays
			if(outfeedConnectionDelay.getValue() != null){
				this.setPresentState("Connecting - "+ newOutfeed.getName()+" - "+outfeedConnectionDelay.getValue().getName());
				newOutfeed.setPresentState("Connecting - "+ this.getName()+" - "+outfeedConnectionDelay.getValue().getName());
				this.simWait(outfeedConnectionDelay.getValue().getNextDelayDuration(newOutfeed,newOutfeedEntity));	
			}
		}
		currentOutfeedList.add(newOutfeed, newOutfeedEntity, 0, newOutfeedAmount);
		this.setPresentState("Working");

//			this.balanceOutfeedFor(newOutfeed,newOutfeedEntity);
			
	}
	
	@Override
	public <T1 extends LogisticsEntity> void addToCurrentlyHandlingEntityList(T1 entityToAdd, double amountToAdd) {
		super.addToCurrentlyHandlingEntityList(entityToAdd, amountToAdd);	
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// UPDATER METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	// REMOVER METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	
	public void removeFromLoadingBay(LoadingBay loadingBay){
		for(BulkHandlingRoute eachRoute: loadingBay.getConnectableRoutesList()){
			if(eachRoute.getRouteSegments().contains(this))
				eachRoute.incrementConnectedSegmentsCount(-1);
		}
	}

	public void detachFromCurrentTow (){
		currentTow.getCurrentlyTowingEntityList().remove(this.getClass().getSimpleName(), this);
		currentTow = null;
		InputAgent.processEntity_Keyword_Value(this, "RelativeEntity", null);
	}
	
	public <T extends LogisticsEntity> void removeFromCurrentlyHandlingEntityList(T bulkMaterialToRemove, double amountToRemove) {
		super.removeFromCurrentlyHandlingEntityList(bulkMaterialToRemove, amountToRemove);
	}
	
	/**
	 * removes the passed linkedEntity from the currentinfeedlist of this entity. will not do anything if infeedToRemove does not exist in the list
	 * TODO when overwriting in subclasses, disconnect route when origin to destination infeed/outfeeds are updated and removed
	 */
	public synchronized <T extends BulkHandlingLinkedEntity> void removeFromCurrenInfeed(T infeedToRemove) {
		currentInfeedList.remove(infeedToRemove);
		//TODO refactor for multiple delays
		if(infeedDisconnectionDelay.getValue() != null){
			this.setPresentState("Disconnecting - "+ infeedToRemove.getName()+" - "+infeedDisconnectionDelay.getValue().getName());
			infeedToRemove.setPresentState("Disconnecting - "+ this.getName()+" - "+infeedDisconnectionDelay.getValue().getName());
			this.simWait(infeedDisconnectionDelay.getValue().getNextDelayDuration(infeedToRemove));	
		}
		
		if(currentInfeedList.isEmpty() && currentOutfeedList.isEmpty())
			this.setPresentState("Idle");			
	}
	
	/**
	 * removes the passed linkedEntity from the currentoutfeedlist of this entity. will not do anything if outfeedToRemove does not exist in the list
 	 * TODO when overwriting in subclasses, disconnect route when origin to destination infeed/outfeeds are updated and removed
	 */
	public synchronized <T extends BulkHandlingLinkedEntity> void removeFromCurrenOutfeed(T outfeedToRemove) {
		currentOutfeedList.remove(outfeedToRemove);
		
		//TODO refactor for multiple delays
		if(outfeedDisconnectionDelay.getValue() != null){
			this.setPresentState("Disconnecting - "+ outfeedToRemove.getName()+" - "+outfeedDisconnectionDelay.getValue().getName());
			outfeedToRemove.setPresentState("Disconnecting - "+ this.getName()+" - "+outfeedDisconnectionDelay.getValue().getName());
			this.simWait(outfeedDisconnectionDelay.getValue().getNextDelayDuration(outfeedToRemove));	
		}		

		if(currentInfeedList.isEmpty() && currentOutfeedList.isEmpty())
			this.setPresentState("Idle");
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// ASSERTION METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////
		
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// SETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	public void setStationary(boolean stationary){
		this.stationary = stationary;
	}	
	
	public void setRateUnits(Class<? extends Unit> rateUnit){
		rate.setUnitType(rateUnit);
		rateByEntityType.setUnitType(rateUnit);
	}
	

	//////////////////////////////////////////////////////////////////////////////////////////////////////
	//GETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////

	public Vec3d getCargoDimensions(){
		return cargoDimensions.getValue();
	}
	
	public MovingEntity getCurrentTow(){
		return this.currentTow;
	}

	public Vec3d getCargoAlignment(){
		return new Vec3d(cargoAlignment.getValue());
	}
	
	public boolean isStationary(){
		return stationary;
	}

	public double getMaxRate() {
		return rate.getValue();
	}
	
	public  double getMaxRate(BulkMaterial bulkMaterial) {
		if (this.checkIfHandles(bulkMaterial)) {
			if (rateByEntityType.getValue() == null) 
				return rate.getValue();
			return rateByEntityType.getValue().get(this.getHandlingEntityTypeList().indexOf(bulkMaterial));
		}
		return 0;		
	}
	
	public ThreeLinkedLists<BulkHandlingLinkedEntity, BulkMaterial> getCurrentOutfeedList() {
		return currentOutfeedList;
	}
	
	public ThreeLinkedLists<BulkHandlingLinkedEntity, BulkMaterial> getCurrentInfeedList() {
		return currentInfeedList;
	}

	/**
	 * @return current total infeed rate regardless of handling entity type by each infeed
	 */
	public double getCurrentInfeedRate(){
		double tempRate = 0;
		for(Double each : currentInfeedList.getValueList(0))
			tempRate += each;
		return tempRate;
	}
	
	public <T extends LogisticsEntity> double getCurrentInfeedRateFor(T entity){
		double tempRate = 0;
		for (int i=0;i<currentInfeedList.size();i++){
			if(currentInfeedList.getSecondEntityList().get(i) == entity)
				tempRate += currentInfeedList.getValueList(0).get(i);
		}
		return tempRate;
	}
	
	/**
	 * @return current total outfeed rate regardless of handling entity type by each outfeed
	 */
	public double getCurrentOutfeedRate(){
		double tempRate = 0;
		for(Double each : currentOutfeedList.getValueList(0))
			tempRate += each;
		return tempRate;
	}
	
	public <T extends LogisticsEntity> double getCurrentOutfeedRateFor(T entity){
		double tempRate = 0;
		for (int i=0;i<currentOutfeedList.size();i++){
			if(currentOutfeedList.getSecondEntityList().get(i) == entity)
				tempRate += currentOutfeedList.getValueList(0).get(i);
		}
		return tempRate;
	}
		
	// ////////////////////////////////////////////////////////////////////////////////////
	// GRAPHICS
	// ////////////////////////////////////////////////////////////////////////////////////
	
	// TODO currently assumes only one type of product can be content, refactor for multiple product types content
	public DisplayEntity getCurrentContentDisplayEntity(){
		return currentContentDisplayEntity;
	}
	
	public void setCurrentContentDisplayEntity(DisplayEntity currentContentDisplayEntity){
		this.currentContentDisplayEntity = currentContentDisplayEntity;
	}
		
	// TODO currently assumes only one type of product can be content, refactor for multiple product types content
	public void updateContentGraphics(){
		if(currentContentDisplayEntity != null){
			Vec3d orient = new Vec3d(currentContentDisplayEntity.getOrientationInput());
			orient.add3(this.getOrientation());
			orient.sub3(currentTow.getOrientationInput());
			currentContentDisplayEntity.setOrientation(orient);
		}
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////
	// REPORTING
	// ////////////////////////////////////////////////////////////////////////////////////
	@Override
	public boolean setPresentState(String state) {
		boolean reportPrinted = false;
		if (super.setPresentState(state)) {
			// add bottom line for states report
			stateReportFile.putStringTabs(HandyUtils.arraylistToString(this.getCurrentlHandlersList()), 1);
			
			if (currentTow != null) {
				stateReportFile.putStringTabs(currentTow.getName(), 1);
			} else {stateReportFile.putTabs(1); }
			
			if (!getCurrentlyHandlingList().isEmpty()) {
				stateReportFile.putStringTabs(HandyUtils.arraylistToString(this
						.getCurrentlyHandlingList().getEntityList()), 1);
				stateReportFile.putStringTabs(HandyUtils.arraylistToString(
						this.getCurrentlyHandlingList().getValueLists().get(0),
						ReportAgent.getReportPrecision()), 1);
			} else{
				stateReportFile.putStringTabs(HandyUtils.arraylistToString(this
						.getHandlingEntityTypeList()), 1);
				stateReportFile.putStringTabs(HandyUtils.arraylistToString(
						this.getEntityAmountProcessedList(),ReportAgent.getReportPrecision()), 1);
			}
			
			stateReportFile.putStringTabs(HandyUtils.arraylistToString(this.getCurrentInfeedList().getFirstEntityList()), 1);
			stateReportFile.putStringTabs(HandyUtils.arraylistToString(this.getCurrentInfeedList().getSecondEntityList()), 1);
			stateReportFile.putStringTabs(HandyUtils.arraylistToString(this.getCurrentInfeedList().getValueList(0),ReportAgent.getReportPrecision()), 1);
			
			stateReportFile.putStringTabs(HandyUtils.arraylistToString(this.getCurrentOutfeedList().getFirstEntityList()), 1);
			stateReportFile.putStringTabs(HandyUtils.arraylistToString(this.getCurrentOutfeedList().getSecondEntityList()), 1);
			stateReportFile.putStringTabs(HandyUtils.arraylistToString(this.getCurrentOutfeedList().getValueList(0),ReportAgent.getReportPrecision()), 1);

			reportPrinted = true;
		}
		
		//update dependent entities states
	/*	if (!externalCall){
			for (GeneratableEntity each: this.getCurrentlyHandledByEntityList()){
				each.setPresentState(each.getPresentState(), true);
			}
				if (currentTow != null) {
					currentTow.setPresentState(currentTow.getPresentState(),
							true);
				}
			
		}	
	*/			
		return reportPrinted;
	}
	
	@Override
	public void printStatesHeader() {
		super.printStatesHeader();
		stateReportFile.putStringTabs("Handled By", 1);
		stateReportFile.putStringTabs("Towed By", 1);
		
		stateReportFile.putStringTabs("Handled", 1);
		stateReportFile.putStringTabs("Handled Amount(s)", 1);

		stateReportFile.putStringTabs("Infeed List", 1);
		stateReportFile.putStringTabs("Infeed Entity(ies) List", 1);
		stateReportFile.putStringTabs("Infeed Rate(s) List", 1);
		stateReportFile.putStringTabs("Outfeed List", 1);
		stateReportFile.putStringTabs("Outfeed Entity(ies) List", 1);
		stateReportFile.putStringTabs("Outfeed Rate(s) List", 1);
		
	}
	
	@Override
	public void printStatesUnitsHeader() {
		super.printStatesUnitsHeader();
		stateReportFile.putTabs(3);
		stateReportFile.putStringTabs("("+this.getHandlingEntityUnit()+")",3);

		stateReportFile.putStringTabs("("+this.getHandlingEntityUnit().getName()+"/h)",3);
		stateReportFile.putStringTabs("("+this.getHandlingEntityUnit().getName()+"/h)",1);		
		
	}

}
