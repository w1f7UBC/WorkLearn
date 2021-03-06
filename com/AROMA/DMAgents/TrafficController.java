package com.AROMA.DMAgents;

import java.util.ArrayList;
import java.util.LinkedList;

import com.AROMA.DMAgents.RouteManager.Route_Type;
import com.AROMA.Logistics.DiscreteHandlingLinkedEntity;
import com.AROMA.Logistics.MovingEntity;
import com.AROMA.Logistics.Route;
import com.jaamsim.basicsim.ErrorException;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class TrafficController extends DisplayEntity {

	public static final TrafficController trafficControlManager;
	
	static {
		trafficControlManager = new TrafficController();
	}
	
	public TrafficController() {
	}
	
	// TODO refactor for complex traffic rules 
	public <T extends MovingEntity> void planNextMove(T movingEntity){
		DiscreteHandlingLinkedEntity origin = movingEntity.getHeadRoute();
		DiscreteHandlingLinkedEntity destination = movingEntity.getCurrentDestination();
		// TODO add logic for planning route i.e. loaded vs. unloaded for passing bulkmaterial or route-type!
		Route tempRoute = RouteManager.getRoute(origin, destination, movingEntity, null, Route_Type.FASTEST, false, Double.POSITIVE_INFINITY, null);
		if (tempRoute == null){
			throw new ErrorException("%s tried to travel from %s to %s but there is not any accessible route between the two destinations!",movingEntity.getName(),
					origin.getName(),destination.getName());
		}
		
		movingEntity.setPlannedNextRouteSegments(null);
		LinkedList<? extends DiscreteHandlingLinkedEntity> tempRouteSegments = tempRoute.getRouteSegmentsList();

		//remove the head route
		tempRouteSegments.remove(0);
		
		movingEntity.setPlannedNextRouteSegments(tempRouteSegments);
		
	}

}
