package com.AROMA.Logistics;

import java.util.ArrayList;
import java.util.LinkedList;

import com.jaamsim.basicsim.ReflectionTarget;
import com.jaamsim.input.InputErrorException;
import com.sandwell.JavaSimulation.Tester;

/**
 * TODO add process routes including conveyors
 */
public class ProcessingRoute extends LogisticsEntity {

	private ArrayList<Stockpile> infeedPiles;
	private ArrayList<Stockpile> outfeedPiles;
	private ArrayList<BulkMaterial> infeedMaterial;
	private ArrayList<BulkMaterial> outfeedMaterial;
	private double lastPlannedTime;

	private boolean active;
	private BulkMaterialProcessor processor;
	// number of connected route segments. When this number is equal to the sum of stockpiles and equipment size, this route is connected.
	private int connectedSegmentsCount;
	
	public ProcessingRoute(LinkedList<LinkedEntity> list, BulkHandlingLinkedEntity activeEquipment) {
		infeedPiles = new ArrayList<>(1);
		outfeedPiles = new ArrayList<>(1);
		infeedMaterial = new ArrayList<>(1);
		outfeedMaterial = new ArrayList<>(1);
		
		infeedPiles.add((Stockpile) list.getFirst());
		outfeedPiles.add((Stockpile) list.getLast());
		infeedMaterial.add((BulkMaterial) list.getFirst().getHandlingEntityTypeList().get(0));
		outfeedMaterial.add((BulkMaterial) list.getLast().getHandlingEntityTypeList().get(0));

		processor = (BulkMaterialProcessor) activeEquipment;
		
		if(!processor.getHandlingEntityTypeList().containsAll(infeedMaterial))
			throw new InputErrorException("Handling entity of %s isn't included in the handling entity type list of %s", list.getFirst().getName(), processor.getName());
		if(!processor.getOutfeedEntityTypeList().containsAll(outfeedMaterial))
			throw new InputErrorException("Handling entity of %s isn't included in the oufeed entity type list of %s", list.getLast().getName(), processor.getName());
				
		// TODO This assumes that all processing routes are figured out at the begining
		// and that outfeed piles are empty (incremented by one for processor and one for outfeed pile)
		this.incrementConnectedSegmentsCount(2);
		
		lastPlannedTime = -1;
	}
	
	/**
	 * This method assumes that the processor is the same and adds checks and adds the new stockpiles
	 */
	public void addNewStockPiles(LinkedList<LinkedEntity> list){
		if(!infeedPiles.contains(list.getFirst())){
			infeedPiles.add((Stockpile) list.getFirst());
			infeedMaterial.add((BulkMaterial) ((Stockpile) list.getFirst()).getHandlingEntityTypeList().get(0));
		}
		if(!outfeedPiles.contains(list.getLast())){
			outfeedPiles.add((Stockpile) list.getLast());
			outfeedMaterial.add((BulkMaterial) ((Stockpile) list.getLast()).getHandlingEntityTypeList().get(0));
			// TODO This assumes that all processing routes are figured out at the begining
			// and that outfeed piles are empty (incremented by one for outfeed pile)
			this.incrementConnectedSegmentsCount(1);
		}
		
		if(!processor.getHandlingEntityTypeList().containsAll(infeedMaterial))
			throw new InputErrorException("Handling entity of %s isn't included in the handling entity type list of %s", list.getFirst().getName(), processor.getName());
		if(!processor.getOutfeedEntityTypeList().containsAll(outfeedMaterial))
			throw new InputErrorException("Handling entity of %s isn't included in the oufeed entity type list of %s", list.getLast().getName(), processor.getName());
		
	}
	
	/**
	 * adds the passed amount to the count of connected segments. enter +1 or -1 
	 */
	public void incrementConnectedSegmentsCount(int incrementedAmount) {
		this.connectedSegmentsCount += incrementedAmount;
		
		//TODO see if this prevents from starting before startup
		if(incrementedAmount > 0 && this.isConnected() && Tester.greaterCheckTimeStep(this.getSimTime(), 0.0d)){
			this.connectRoute();
			this.scheduleProcess(0.0d, 10, new ReflectionTarget(processor, "doProcessing",this));			
		}
	}

	public boolean isConnected() {
		return connectedSegmentsCount == infeedPiles.size() + outfeedPiles.size() + 1 ? true : false;
	}
	
	public double getLastPlannedTime(){
		return lastPlannedTime;
	}
	
	public void setLastPlannedTime(double time){
		lastPlannedTime = time;
	}
			
	/**
	 * 
	 * @return (bulkMaterial handling rate/ baseBulkMateral's handling rate)
	 */
	public double getCapacityRatio(BulkMaterial bulkMaterial, BulkMaterial baseBulkMaterial){
		double numerator, denominator;
		if(infeedMaterial.contains(bulkMaterial))
			numerator = processor.getMaxRate(bulkMaterial);
		else
			numerator = processor.getOutfeedRate(bulkMaterial);
		
		if(infeedMaterial.contains(baseBulkMaterial))
			denominator = processor.getMaxRate(baseBulkMaterial);
		else
			denominator = processor.getOutfeedRate(baseBulkMaterial);
		
		return numerator / denominator;
	}
	
	public ArrayList<Stockpile> getInfeedPiles() {
		return infeedPiles;
	}

	public ArrayList<Stockpile> getOutfeedPiles() {
		return outfeedPiles;
	}

	public ArrayList<BulkMaterial> getInfeedMaterial() {
		return infeedMaterial;
	}

	public ArrayList<BulkMaterial> getOutfeedMaterial() {
		return outfeedMaterial;
	}

	//TODO add route priority logic
	public int getRoutePriority(){
		return 5;
	}
	
	public boolean isActive() {
		return active;
	}

	public BulkMaterialProcessor getProcessor() {
		return processor;
	}

	public void setInfeedPiles(ArrayList<Stockpile> infeedPiles) {
		this.infeedPiles = infeedPiles;
	}

	public void setOutfeedPiles(ArrayList<Stockpile> outfeedPiles) {
		this.outfeedPiles = outfeedPiles;
	}

	public void setActive(boolean active) {
		this.active = active;
		
		if(!active){
			this.disconnectRoute();
		}
	}

	public void setProcessor(BulkMaterialProcessor processor) {
		this.processor = processor;
	}

	@Override
	public void startUp() {
		super.startUp();
		if(this.isConnected()){
			this.connectRoute();
			this.scheduleProcess(0.0d, 10, new ReflectionTarget(processor, "doProcessing",this));			
		}
			
	}
	
	/**
	 * This method assumes that only processor sits between infeed and outfeed piles 
	 */
	public void connectRoute(){
		BulkMaterial tempBulkMaterial = null;
		for(Stockpile each: infeedPiles){
			tempBulkMaterial = (BulkMaterial) each.getCurrentlyHandlingList().getEntityList().get(0);
			each.addToCurrentOutfeed(processor, tempBulkMaterial, processor.getMaxRate(tempBulkMaterial));
			processor.addToCurrentInfeed(each, tempBulkMaterial, processor.getMaxRate(tempBulkMaterial));
		}
		for(Stockpile each: outfeedPiles){
			tempBulkMaterial = (BulkMaterial) each.getHandlingEntityTypeList().get(0);
			each.addToCurrentInfeed(processor, tempBulkMaterial, processor.getOutfeedRate(tempBulkMaterial));
			processor.addToCurrentOutfeed(each, tempBulkMaterial, processor.getOutfeedRate(tempBulkMaterial));
		}
	}
	
	public void disconnectRoute(){
		for(Stockpile each: infeedPiles){
			each.removeFromCurrenOutfeed(processor);
			processor.removeFromCurrenInfeed(each);
		}
		for(Stockpile each: outfeedPiles){
			each.removeFromCurrenInfeed(processor);
			processor.removeFromCurrenOutfeed(each);
		}
	}

}
