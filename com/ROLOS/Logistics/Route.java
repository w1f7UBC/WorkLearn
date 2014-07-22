package com.ROLOS.Logistics;

import java.util.ArrayList;

/**
 * This class holds route information from the origin to destination. 
 */
public class Route {
		
	private DiscreteHandlingLinkedEntity origin;
	private DiscreteHandlingLinkedEntity destination;
	private ArrayList<DiscreteHandlingLinkedEntity> routeSegmentsList;
	private ArrayList<LogisticsEntity> handlingMovingEntitiesList;
	
	private double distance;
	
	public Route(){
		
	}
	
	public Route(DiscreteHandlingLinkedEntity origin, DiscreteHandlingLinkedEntity destination, double distance,ArrayList<LogisticsEntity> handlingEntitiesList) {
		this.origin = origin;
		this.destination = destination;
		routeSegmentsList = new ArrayList<>();
		this.distance = distance;
		handlingMovingEntitiesList = new ArrayList<>(handlingEntitiesList);
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
	
	public String getRouteName(){
		return 	origin.getName() + "-" + destination.getName();
	}
	
	public double getDistance(){
		return distance;
	}

	public DiscreteHandlingLinkedEntity getOrigin() {
		return origin;
	}

	public DiscreteHandlingLinkedEntity getDestination() {
		return destination;
	}

	public ArrayList<LogisticsEntity> getHandlingMovingEntitiesList() {
		return handlingMovingEntitiesList;
	}
}

