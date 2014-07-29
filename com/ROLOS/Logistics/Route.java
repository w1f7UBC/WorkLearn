package com.ROLOS.Logistics;

import java.util.ArrayList;

import com.ROLOS.DMAgents.SimulationManager;
import com.ROLOS.DMAgents.RouteManager.Route_Type;
import com.ROLOS.DMAgents.RouteManager.Transport_Mode;
import com.sandwell.JavaSimulation.InputErrorException;

/**
 * This class holds route information from the origin to destination. 
 */
public class Route {
		
	private DiscreteHandlingLinkedEntity origin;
	private DiscreteHandlingLinkedEntity destination;
	private ArrayList<DiscreteHandlingLinkedEntity> routeSegmentsList;
	private ArrayList<Transport_Mode> transportModeList;
	Route_Type routeType;
	
	private double dijkstraWeight;
	
	public Route(){
		
	}
	
	public Route(DiscreteHandlingLinkedEntity origin, DiscreteHandlingLinkedEntity destination, double dijkstraWeight) {
		this.origin = origin;
		this.destination = destination;
		routeSegmentsList = new ArrayList<>();
		this.dijkstraWeight = dijkstraWeight;
		transportModeList = new ArrayList<>(1);
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
	public ArrayList<DiscreteHandlingLinkedEntity> getRouteSegmentsList(){
		ArrayList<DiscreteHandlingLinkedEntity> tempRoute = new ArrayList<>(routeSegmentsList);
		
		return tempRoute;
	}
			
	public void setRoute(ArrayList<? extends DiscreteHandlingLinkedEntity> route){
		this.routeSegmentsList.clear();
		for(DiscreteHandlingLinkedEntity each: route){
			this.routeSegmentsList.add(each);
		}
	}
		
	/**
	 * 
	 * @return the calculated dikstra weight. i.e. based on routing rule it might be cost, distance, or time.
	 */
	public double getDijkstraWeight(){
		return dijkstraWeight;
	}

	public DiscreteHandlingLinkedEntity getOrigin() {
		return origin;
	}

	public DiscreteHandlingLinkedEntity getDestination() {
		return destination;
	}
	
	public Route_Type getRouteType() {
		return routeType;
	}

	public void setRouteType(Route_Type routeType) {
		this.routeType = routeType;
	}
	
	
	/**
	 * TODO add logic to use cost structure passed in the movingEntity and etc to calculate transportation cost 
	 * @return $ per unit of bulkMaterial transported from origin to destination of the passed route. 
	 */
	public double estimateTransportationCostonRoute(MovingEntity movingEntity, BulkMaterial bulkMaterial){
		if(!transportersList.getValue().contains(movingEntity) || !movingEntity.getAcceptingBulkMaterialList().contains(bulkMaterial))
			throw new InputErrorException("Please check transportation structure for %s. Transpor"
					+ "%s doesn't carry %s or doesn't appear in the transporters list of %s", this.getFacility(), movingEntity.getName(), bulkMaterial.getName(), this.getName());
		double unitCost = movingEntity.getTransportationCost(bulkMaterial) * route.getTravelTime(movingEntity);
		
		// print transportation cost report
		SimulationManager.printTransportationCostReport(movingEntity, route, bulkMaterial, unitCost);
		return unitCost;
					
	}
}

