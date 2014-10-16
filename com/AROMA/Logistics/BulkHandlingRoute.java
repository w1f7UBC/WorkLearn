package com.AROMA.Logistics;

import java.util.LinkedList;

import com.sandwell.JavaSimulation.Tester;

/**
 * Loading or Unloading Routes assume that the stock pile only accepts one type of bulk material
 */
public class BulkHandlingRoute extends LogisticsEntity {

	// sequence of route segments from loadingbay to stock pile (for unlaoding bays) or stock pile to loading bay (for loading bays)
	private LinkedList<LinkedEntity> routeSegments;
	
	// number of connected route segments. When this number is equal to the routesegments size, this route is connected.
	private int connectedSegmentsCount;
	
	// infeed and outfeed rates are set by the operationsmanager when connecting the route if active equipment is null
	private double infeedRate, outfeedRate;
	private int routePriority;
	private boolean active;
	private double conversionRate;
	
	private BulkHandlingLinkedEntity activeEquipment;
	private BulkMaterial infeedBulkMaterial, outfeedBulkMaterial;
	
	/**
	 * 
	 * @param activeEquipment if null route is selfoperating, or the loader or processor that adjusts stockpile and bulkcargo's contents
	 */
	public BulkHandlingRoute(LinkedList<LinkedEntity> linkedEntitiesList, BulkHandlingLinkedEntity activeEquipment) {
				
		routePriority = linkedEntitiesList.getFirst().getOriginPriority()+linkedEntitiesList.getLast().getDestinationPriority();
		
		routeSegments = new LinkedList<>(linkedEntitiesList);
				
		this.activeEquipment = activeEquipment;
		
		// assumes that balanced rate is dictated by the active equipment's rate
		if(activeEquipment == null){
			infeedRate = 0.0d;
			outfeedRate = 0.0d;
			infeedBulkMaterial = (BulkMaterial) this.getStockpile().getHandlingEntityTypeList().get(0);
			outfeedBulkMaterial = infeedBulkMaterial;
		} else{
			infeedRate = activeEquipment.getMaxRate();
			// this assumes that unloading routes are not able to infeed anything except stockpile's content (no processing)
			infeedBulkMaterial = (BulkMaterial) this.getStockpile().getHandlingEntityTypeList().get(0);
			
			if(activeEquipment instanceof BulkMaterialProcessor){
				outfeedRate = ((BulkMaterialProcessor) activeEquipment).getOutfeedRate(((BulkMaterialProcessor)activeEquipment).getOutfeedEntityTypeList().get(0));
				outfeedBulkMaterial = ((BulkMaterialProcessor) activeEquipment).getOutfeedEntityTypeList().get(0);
			}
			else{ 
				outfeedRate = infeedRate;
				outfeedBulkMaterial = infeedBulkMaterial;
			}
			
			if(infeedRate != 0.0d && outfeedRate != 0.0d)
				conversionRate = outfeedRate / infeedRate;
		}
		
		// for the loading bay
		connectedSegmentsCount++;
		// for the unloading stockpile
		if(routeSegments.getLast() instanceof Stockpile && Tester.greaterCheckTolerance(routeSegments.getLast().getRemainingCapacity(outfeedBulkMaterial),0.0d))
			connectedSegmentsCount++;

		for(LinkedEntity each: routeSegments){
			try{
				if(((BulkHandlingLinkedEntity) each).isStationary())
					connectedSegmentsCount++;
				//assuming bulkhandlingmaterial only handle one type of material	
			} catch(ClassCastException e){}
		}				
	}
	
	public boolean isLoading(){
		return routeSegments.getFirst() instanceof Stockpile ? true : false;
	}
	
	public double getConversionRate(){
		return conversionRate;
	}
	
	public Stockpile getStockpile(){
		return (Stockpile) (isLoading() ? routeSegments.getFirst() : routeSegments.getLast());
	}
	
	public LoadingBay getLoadingBay(){
		return (LoadingBay) (!isLoading() ? routeSegments.getFirst() : routeSegments.getLast());
	}
	
	public LinkedList<LinkedEntity> getRouteSegments(){
		return routeSegments;
	}

	public double getInfeedRate() {
		return infeedRate;
	}
	
	public double getOutfeedRate(){
		return outfeedRate;
	}
	
	public int getRoutePriority(){
		return routePriority;
	}

	//TODO it is assumed that each bulk material route only outfeeds one bulk material type 
	public BulkMaterial getOutfeedBulkMaterial() {
		if(activeEquipment != null)
			return outfeedBulkMaterial;
		else
			return (BulkMaterial) this.getStockpile().getHandlingEntityTypeList().get(0);
	}

	//TODO it is assumed that each bulk material route only outfeeds one bulk material type 
	public BulkMaterial getInfeedBulkMaterial() {
		if(activeEquipment != null)
			return infeedBulkMaterial;
		else
			return (BulkMaterial) this.getStockpile().getHandlingEntityTypeList().get(0);
	}
		
	public BulkHandlingLinkedEntity getActiveEquipment() {
		return activeEquipment;
	}

	public boolean isConnected() {
		return connectedSegmentsCount == routeSegments.size() ? true : false;
	}

	/**
	 * adds the passed amount to the count of connected segments. enter +1 or -1 
	 */
	public void incrementConnectedSegmentsCount(int incrementedAmount) {
		this.connectedSegmentsCount += incrementedAmount;
	}
	
	public void setInfeedRate(double infeedRate){
		this.infeedRate = infeedRate;
	}
	
	public void setOutfeedRate(double outfeedRate){
		this.outfeedRate = outfeedRate;
	}
	
	public boolean isActive(){
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
}
