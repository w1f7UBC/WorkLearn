package com.AROMA.Logistics;

import java.util.ArrayList;
import java.util.LinkedList;

import com.AROMA.DMAgents.SimulationManager;
import com.AROMA.DMAgents.RouteManager.Route_Type;
import com.AROMA.DMAgents.RouteManager.Transport_Mode;

/**
 * This class holds route information from the origin to destination. 
 */
public class Route {
		
	private DiscreteHandlingLinkedEntity origin;
	private DiscreteHandlingLinkedEntity destination;
	private LinkedList<DiscreteHandlingLinkedEntity> routeSegmentsList;
	// Transportation modes e.g. ROAD, RAIL (for multimodal)
	private ArrayList<Transport_Mode> transportModeList;
	// the list of moving entities this route is configured for. the first item is the one that 
	// also appears in the name. if multi modal, the rest would indicate the transporters for other sections
	private ArrayList<MovingEntity> movingEntitiesList;
	private Route_Type routeType;
	
	private double dijkstraWeight;
		
	public Route(DiscreteHandlingLinkedEntity origin, DiscreteHandlingLinkedEntity destination, double dijkstraWeight, MovingEntity movingEntity, Route_Type routingRule) {
		this.origin = origin;
		this.destination = destination;
		routeSegmentsList = new LinkedList<>();
		this.dijkstraWeight = dijkstraWeight;
		movingEntitiesList = new ArrayList<>(1);
		transportModeList = new ArrayList<>(1);
		if(movingEntity != null) {
			movingEntitiesList.add(movingEntity);
			transportModeList.add(movingEntity.getTransportMode());
		}		
	}			
	
	/**
	 * @return type of routes, i.e. for unimodal would be one element road, rail, or water. for multimodal would be combinaiton of those.  
	 */
	public ArrayList<Transport_Mode> getTransportModeList(){
		return transportModeList;
	}
	
	/**
	 * @return will return the sequence of route segments from origin to destination or null if a route from
	 * origin to destination doesn't exist. 
	 */
	public LinkedList<DiscreteHandlingLinkedEntity> getRouteSegmentsList(){
		LinkedList<DiscreteHandlingLinkedEntity> tempRoute = new LinkedList<>(routeSegmentsList);
		
		return tempRoute;
	}
	
	public void addRouteSegmentsLast (LinkedList<DiscreteHandlingLinkedEntity> routesListToAdd){
		routeSegmentsList.addAll(routesListToAdd);
	}
	
	public void removeLast(){
		routeSegmentsList.removeLast();
	}
	
	public ArrayList<MovingEntity> getMovingEntitiesList(){
		return movingEntitiesList;
	}
	
	/**
	 * this method sets the transport mode as well (routes at creation time or unimodal). multi modal routes are
	 * concatenated.
	 */
	public void setRoute(ArrayList<? extends DiscreteHandlingLinkedEntity> route){
		this.routeSegmentsList.clear();
		for(DiscreteHandlingLinkedEntity each: route){
			this.routeSegmentsList.addLast(each);
		}
		transportModeList.add(movingEntitiesList.get(0).getTransportMode());
	}
		
	/**
	 * 
	 * @return the calculated dikstra weight. i.e. based on routing rule it might be cost, distance, or time.
	 */
	public double getDijkstraWeight(){
		return dijkstraWeight;
	}
	
	public void setDijkstraWeight(double dijkstraWeight){
		this.dijkstraWeight = dijkstraWeight;
	}

	public DiscreteHandlingLinkedEntity getOrigin() {
		return origin;
	}

	public DiscreteHandlingLinkedEntity getDestination() {
		return destination;
	}
	
	public void setRouteType(Route_Type routeType) {
		this.routeType = routeType;
	}
	
	public Route_Type getRouteType(){
		return routeType;
	}
	
	/**
	 * TODO add logic to use cost structure passed in the movingEntity and etc to calculate transportation cost
	 * @param twoWay whether to include cost of travelling back empty on the route (assuming the exact same route is used for traveling back!) 
	 * @return $ per unit of bulkMaterial transported from origin to destination of the passed route. 
	 */
	public double estimateTransportationCostonRoute(BulkMaterial bulkMaterial, boolean twoWay){
		double unitCost = 0.0d;
		if(routeType == Route_Type.LEASTCOST)
			unitCost = dijkstraWeight;
		else{
			int index = 0;
			for (DiscreteHandlingLinkedEntity each: routeSegmentsList){
				if(each instanceof Transshipment && each != routeSegmentsList.getFirst() && each != routeSegmentsList.getLast()){
					unitCost += each.getTravelCost(movingEntitiesList.get(index));
					//index++;
				}
				else if(each instanceof Facility)
					continue;
				unitCost += each.getTravelCost(movingEntitiesList.get(index))/movingEntitiesList.get(index).getAcceptingBulkMaterialList().getValueFor(bulkMaterial, 0);
			}
		}
		
		// print transportation cost report
		SimulationManager.printTransportationCostReport(this, bulkMaterial, unitCost);
		return twoWay ? 2*unitCost : unitCost;
					
	}
	
	/**
	 * @return estimated travel time from origin to destination of the route. 
	 */
	public double estimateTravelTimeonRoute(){
		double tempTime = 0.0d;
		if(routeType == Route_Type.FASTEST)
			tempTime = dijkstraWeight;
		else {
			int index = 0;
			for (DiscreteHandlingLinkedEntity each: routeSegmentsList){
				if(each instanceof Transshipment){
					tempTime += each.getTravelTime(movingEntitiesList.get(index));
					//index++;
				}
				else if(each instanceof Facility)
					continue;
				tempTime += each.getTravelTime(movingEntitiesList.get(index));
			}
		}
		
		return tempTime;
					
	}
	
	/**
	 * @return Routes' length. 
	 */
	public double getLength(){
		double tempLength = 0.0d;
		if(routeType == Route_Type.SHORTEST)
			tempLength = dijkstraWeight;
		else{
			for (DiscreteHandlingLinkedEntity each: routeSegmentsList){
			if(each instanceof Facility)
				continue;
			tempLength += each.getLength();
			}
		}
		
		return tempLength;
					
	}
}

