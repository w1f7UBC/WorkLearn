package com.ROLOS.DMAgents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.PriorityQueue;

import DataBase.DataBaseObject;

import com.ROLOS.Logistics.BulkMaterial;
import com.ROLOS.Logistics.DiscreteHandlingLinkedEntity;
import com.ROLOS.Logistics.DiscreteHandlingLinkedEntity.DijkstraComparator;
import com.ROLOS.Logistics.EntranceBlock;
import com.ROLOS.Logistics.ExitBlock;
import com.ROLOS.Logistics.Facility;
import com.ROLOS.Logistics.LinkedEntity;
import com.ROLOS.Logistics.LoadingBay;
import com.ROLOS.Logistics.LogisticsEntity;
import com.ROLOS.Logistics.MovingEntity;
import com.ROLOS.Logistics.ReportAgent;
import com.ROLOS.Logistics.Route;
import com.ROLOS.Logistics.RouteEntity;
import com.ROLOS.Logistics.RouteSegment;
import com.ROLOS.Logistics.Transshipment;
import com.ROLOS.Utils.HandyUtils;
import com.ROLOS.Utils.HashMapList;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.EnumInput;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.jaamsim.input.Keyword;

public class RouteManager extends DisplayEntity {
	
	public enum Transport_Mode {
		ROAD,
		RAIL,
		WATER;
	}
	
	public enum Route_Type {
		LEASTCOST,
		FASTEST,
		SHORTEST;
	}
	
	@Keyword(description = "If TRUE, then reports for established contracts will be printed out.",
		     example = "TransportationManager PrintRoutesReport { TRUE }")
	private static final BooleanInput printManagerReport;

	@Keyword(description = "the database entity containing routes information. "
			+ "the code is very specific to the data base format.",
		     example = "TransportationManager RouteDataBase { Somedatabase }")
	private static final EntityInput<DataBaseObject> routeDB;

	protected static FileEntity managerReportFile;        // The file to store the manager reports
	
	static {		
		// TODO bad implementation! set true to print out the configured or unresolved routes report
		printManagerReport = new BooleanInput("PrintRoutesReport", "Report", false);
		routeDB = new EntityInput<DataBaseObject>(DataBaseObject.class, "RouteDataBase", "Key Inputs", null);
	}
	
	{
		this.addInput(printManagerReport);
		this.addInput(routeDB);
	}
	
	public static boolean lockedDijkstraParameters = false;
	
	private static HashMapList<String, Route> routesList;
	// list of routes that won't allow movingEntity
	private static HashMapList<MovingEntity, String> unResolvedRoutesList;
	
	public RouteManager() {
		routesList = new HashMapList<String, Route>();
		unResolvedRoutesList = new HashMapList<MovingEntity, String>(2);
		// initiate file if print report is true
		
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		if( printManagerReport.getValue()){
			managerReportFile = ReportAgent.initializeFile(this,".mgmt");
			this.printManagerReportHeader();
		}
	}
	// ////////////////////////////////////////////////////////////////////////////////////////////////////
	// ADDER METHODS
	// ////////////////////////////////////////////////////////////////////////////////////////////////////

	// ////////////////////////////////////////////////////////////////////////////////////////////////////
	// GETTER METHODS
	// ////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return This method constructs the shortest route from origin loading bay to the destination loading bay if 
	 * route doesn't already exist. 
	 */
	public static Route getBayToBayRoute(LoadingBay originBay, LoadingBay destinationBay, MovingEntity movingEntity) {
		String routeName = getRouteName(originBay, destinationBay, movingEntity);
		Route finalRoute = null;
		if(!routesList.contains(routeName)){
			ExitBlock exit;
			EntranceBlock entrance;
			double distance = 0;
			Route tempRoute;
			// Assumes origin and destination bays handle moving entity
			exit = originBay.getFacility().getTransportationManager()
				.getFacilityExitBlock(movingEntity, originBay);
			entrance = destinationBay.getFacility().getTransportationManager()
				.getFacilityEntranceBlock(movingEntity, destinationBay);

			//origin to exit block 
			tempRoute = RouteManager.getRoute(originBay, exit,movingEntity, null, Route_Type.SHORTEST, Double.POSITIVE_INFINITY);
			distance += tempRoute.getDijkstraWeight();
			ArrayList<DiscreteHandlingLinkedEntity> tempRouteSegments = new ArrayList<>(
				tempRoute.getRouteSegmentsList());
			
			//exit to entrance
			tempRoute = RouteManager.getRoute(exit, entrance,movingEntity, null, Route_Type.SHORTEST, Double.POSITIVE_INFINITY);
			distance += tempRoute.getDijkstraWeight();
			tempRouteSegments.addAll(tempRoute.getRouteSegmentsList());

			//entrance to destination bay
			tempRoute = RouteManager.getRoute(entrance,destinationBay,movingEntity, null, Route_Type.SHORTEST, Double.POSITIVE_INFINITY);
			distance += tempRoute.getDijkstraWeight();
			tempRouteSegments.addAll(tempRoute.getRouteSegmentsList());
			
			finalRoute = new Route(originBay, destinationBay, distance, movingEntity, Route_Type.SHORTEST);
			finalRoute.setRoute(tempRouteSegments);
			routesList.add(routeName, finalRoute);
		} else{
			finalRoute = RouteManager.getRoute(originBay, destinationBay, movingEntity, null, Route_Type.SHORTEST, Double.POSITIVE_INFINITY);
		}
		return finalRoute;
	}

	// Returns route that handles the passed moving entity or computes one if
	// existed

	/**
	 * @param origin if facility is passed as origin and destination, 
	 * this method should be used for estimating distance from facility to facility and not for planning moving entity's moves
	 * @return the best (shortest,fastest, or least cost)route that starts with the same transport mode as that of moving entity or computes one if
	 * existed. null if such route doesn't exist. 
	 */
	public static <T extends DiscreteHandlingLinkedEntity> Route getRoute(T origin,
			T destination, MovingEntity movingEntity, BulkMaterial bulkMaterial, Route_Type routingRule, double weightCap) {
		String tempKey = getRouteName(origin, destination, movingEntity);
		
		if(unResolvedRoutesList.get(movingEntity).contains(tempKey))
			return null;
		
		// finds the best (shortest,fastest, or least cost) route that begins with the same transport mode as moving entity
		Route tempRoute= null;
		for (Route each : routesList.get(tempKey))
			if (each.getTransportModeList().get(0) == movingEntity.getTransportMode())
				if(tempRoute == null)
					tempRoute = each;
				else
					if(each.getDijkstraWeight() < tempRoute.getDijkstraWeight())
						tempRoute = each;
		if (tempRoute != null)
				return tempRoute;
		else
			return computeDijkstraPath(origin, destination, movingEntity, bulkMaterial, routingRule,weightCap);
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////
	// DIJKSTRA ALGORITHM CLASS
	// ////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * if movingEntity is passed, origin and destination are not checked for
	 * handling movingEntity condition. this is to allow for facilities to be
	 * origin and destinations and also that it's assumed that caller checks for
	 * origin and destination's compatibility
	 * 
	 * @param movingEntity is always the carrier from the begining of the route (from origin)
	 * route is computed that handles movingEntity throughout. if
	 * null is passed shortest path is calculated
	 * @param weightCap is the maximum weight that will be considered in dijkstra calculation. e.g. if 
	 * travel time cap is set to 12 hrs and routes exceed that, they'd get cut off.
	 */
	public static <T extends DiscreteHandlingLinkedEntity> Route computeDijkstraPath(
			T origin, T destination, MovingEntity movingEntity, BulkMaterial bulkMaterial, Route_Type routingRule, double weightCap) {
		double destinationWeight = Double.POSITIVE_INFINITY;
		double weightThroughTransshipment = Double.POSITIVE_INFINITY;
		Route routeThroughTransshipment = null;
		
		DijkstraComparator dijkstraComparator = new DijkstraComparator();
		origin.getDijkstraComparatorList().add(dijkstraComparator, null, 0,
				0.0d);
		PriorityQueue<T> vertexQueue = new PriorityQueue<>(5,dijkstraComparator);
		vertexQueue.add(origin);

		while (!vertexQueue.isEmpty()) {
			T u = vertexQueue.poll();
			double weightThroughU;
			// Visit each edge exiting u
			for (LinkedEntity each : u.getNextLinkedEntityList()) {
				// create a new entry for discrete entity's dijkstra list and
				// set value to infinity
				if (!((DiscreteHandlingLinkedEntity) each)
						.getDijkstraComparatorList().contains(
								dijkstraComparator)) {
					((DiscreteHandlingLinkedEntity) each)
							.getDijkstraComparatorList().add(
									dijkstraComparator, null, 0,
									Double.POSITIVE_INFINITY);
				}
				weightThroughU = u.getDijkstraComparatorList()
						.getValueListFor(dijkstraComparator, 0).get(0);
				double weight = 0;
				if ((each instanceof RouteSegment && ((RouteSegment) each).getTransportMode() == movingEntity.getTransportMode()) || 
					(each instanceof RouteEntity && ((RouteEntity) each).getTransportMode() == movingEntity.getTransportMode()) ) {
					//assign weight based on routing rule
					if(routingRule == Route_Type.FASTEST)
						weight =  ((DiscreteHandlingLinkedEntity) each).getTravelTime(movingEntity);
					else if (routingRule == Route_Type.SHORTEST)
						weight = ((DiscreteHandlingLinkedEntity) each).getLength();
					// leastcost is calculated based on per unit of material transportationcost
					else if (routingRule == Route_Type.LEASTCOST)
						weight = ((DiscreteHandlingLinkedEntity) each).getTravelTime(movingEntity)*
							movingEntity.getTransportationCost(bulkMaterial);
					
					weightThroughU += weight;
					if (weightThroughU < destinationWeight && weightThroughU <= weightCap && 
							weightThroughU < ((DiscreteHandlingLinkedEntity) each).getDijkstraComparatorList()
							.getValueListFor(dijkstraComparator, 0).get(0)) {
						vertexQueue.remove(each);
					    ((DiscreteHandlingLinkedEntity) each)
								.getDijkstraComparatorList().remove(
										dijkstraComparator);
						((DiscreteHandlingLinkedEntity) each)
								.getDijkstraComparatorList().add(
										dijkstraComparator, u, 0,
										weightThroughU);
						
						vertexQueue.add((T) each);
					}
				} 

				if (each.equals(destination)) {
						if (weightThroughU < destinationWeight && weightThroughU < weightCap) {
							destinationWeight = weightThroughU;
							vertexQueue.remove(each);
							((DiscreteHandlingLinkedEntity) each)
									.getDijkstraComparatorList().remove(
											dijkstraComparator);
							((DiscreteHandlingLinkedEntity) each)
									.getDijkstraComparatorList().add(
											dijkstraComparator, u, 0,
											weightThroughU);
					}
				} 
				// if reached a transshipment, see if going through transshipment would be better and hold information until the end to compare to a unimodal route.
				else if (each instanceof Transshipment){
					//assign weight based on routing rule
					if(routingRule == Route_Type.FASTEST)
						weight =  ((DiscreteHandlingLinkedEntity) each).getTravelTime(movingEntity);
					else if (routingRule == Route_Type.SHORTEST)
						weight = ((DiscreteHandlingLinkedEntity) each).getLength();
					// leastcost should be passed on as a per unit of material transportationcost in transshipment
					else if (routingRule == Route_Type.LEASTCOST)
						weight = ((DiscreteHandlingLinkedEntity) each).getTravelCost(movingEntity);
					
					weightThroughU += weight;
					if (weightThroughU < destinationWeight && weightThroughU < weightCap && 
							weightThroughU < ((DiscreteHandlingLinkedEntity) each).getDijkstraComparatorList()
							.getValueListFor(dijkstraComparator, 0).get(0)){
						vertexQueue.remove(each);
					    ((DiscreteHandlingLinkedEntity) each)
								.getDijkstraComparatorList().remove(
										dijkstraComparator);
						((DiscreteHandlingLinkedEntity) each)
								.getDijkstraComparatorList().add(
										dijkstraComparator, u, 0,
										weightThroughU);
						
						Route tempRoute = null;
						//explore routes from transshipment to destination based on routing rule.
						if(routingRule == Route_Type.FASTEST)
							tempRoute = ((Facility)each).getTransportationManager().getFastestTranspotationRoute(bulkMaterial, (T)each, destination, weightCap-weightThroughU);
						else if (routingRule == Route_Type.SHORTEST)
							tempRoute = ((Facility)each).getTransportationManager().getShortestTranspotationRoute(bulkMaterial, (T)each, destination, weightCap-weightThroughU);
						// leastcost should be passed on as a per unit of material transportationcost in transshipment
						else if (routingRule == Route_Type.LEASTCOST)
							tempRoute = ((Facility)each).getTransportationManager().getLeastCostTranspotationRoute(bulkMaterial, (T)each, destination, weightCap-weightThroughU);
						
						// if route through transshipment was found and result is betther than so far, save information for later evaluation of unimodal and intermodal
						if(tempRoute != null ){
							double tempWeight = tempRoute.getDijkstraWeight() + weightThroughU;
							if(tempWeight < weightThroughTransshipment){
								weightThroughTransshipment = tempWeight;
								routeThroughTransshipment = tempRoute;
							}
						}
					}
				}
			}
		}
		if (weightThroughTransshipment < destinationWeight){
			return concatenateRoutes(setRoute(origin, routeThroughTransshipment.getOrigin(), movingEntity, dijkstraComparator,routingRule),routeThroughTransshipment);
		} else if (destinationWeight < Double.POSITIVE_INFINITY)
			return setRoute(origin, destination, movingEntity, dijkstraComparator,routingRule);
		
		// if method has reached here and not returned yet, it means that there
		// isn't a path from origin to destination, to save this knowledge
		// initiate a route with null routeSementsList and add to routesList! or add it to the unresolved list if moving entity is specified
		String tempKey = getRouteName(origin, destination, movingEntity);
		
		unResolvedRoutesList.add(movingEntity, tempKey);
		RouteManager.printRouteReport(origin, destination, null, movingEntity);
		
		return null;
	}
	
	public static <T extends DiscreteHandlingLinkedEntity> String getRouteName(T origin,
			T destination, MovingEntity movingEntity){
		return origin.getName() + "-" + destination.getName() + "-" + movingEntity.getName();
	}

	/**
	 * Attaches the two
	 * @param routeList
	 * @return
	 */
	private static Route concatenateRoutes (Route... routeList){
		LinkedList<Route> tempRoutesList = new LinkedList<>(Arrays.asList(routeList));
		Route returnRoute = new Route(tempRoutesList.getFirst().getOrigin(), tempRoutesList.getLast().getDestination(), 0.0d, null, tempRoutesList.get(0).getRouteType());
		double tempWeight = 0.0d;
		for(int i =0; i < tempRoutesList.size(); i++){
			tempWeight += tempRoutesList.get(i).getDijkstraWeight();
			returnRoute.getTransportModeList().addAll(tempRoutesList.get(i).getTransportModeList());
			returnRoute.getMovingEntitiesList().addAll(tempRoutesList.get(i).getMovingEntitiesList());
			returnRoute.addRouteSegmentsLast(tempRoutesList.get(i).getRouteSegmentsList());
			if (i<tempRoutesList.size()-1)
					returnRoute.removeLast();
		}
		
		returnRoute.setDijkstraWeight(tempWeight);
		
		String tempKey = getRouteName(returnRoute.getRouteSegmentsList().getFirst(), returnRoute.getRouteSegmentsList().getLast(),
				returnRoute.getMovingEntitiesList().get(0));
		routesList.add(tempKey, returnRoute);
		RouteManager.printRouteReport(returnRoute.getRouteSegmentsList().getFirst(), returnRoute.getRouteSegmentsList().getLast(), 
				returnRoute, null);
		
		return returnRoute;
	}
	
	private static <T extends DiscreteHandlingLinkedEntity> Route setRoute(T origin,
			T destination, MovingEntity movingEntity, DijkstraComparator dijkstraComparator, Route_Type routingRule) {

		LinkedList<DiscreteHandlingLinkedEntity> reversePath = new LinkedList<>();
		ArrayList<DiscreteHandlingLinkedEntity> path = new ArrayList<>();
		for (DiscreteHandlingLinkedEntity vertex = destination; vertex != null; vertex = vertex
				.getDijkstraComparatorList()
				.getSecondEntityList()
				.get(vertex.getDijkstraComparatorList().indexOf(
						dijkstraComparator)))
			reversePath.add(vertex);

		path.add(reversePath.pollLast());

		while (!reversePath.isEmpty()) {
			path.add(reversePath.pollLast());
		}

		String tempKey = getRouteName(origin, destination, movingEntity);
		Route tempRoute = new Route(origin, destination, destination
				.getDijkstraComparatorList()
				.getValueListFor(dijkstraComparator, 0).get(0), movingEntity, routingRule);
		tempRoute.setRoute(path);
		routesList.add(tempKey, tempRoute);
		RouteManager.printRouteReport(origin, destination, tempRoute, null);
		// remove dijkstraComparator in all route segments after setting up the
		// route
		removeDijkstraComparator(path,dijkstraComparator);
		
		return tempRoute;

	}
	
	/**
	 * removes the dijkstracomparator from all elements of the path
	 */
	private static void removeDijkstraComparator(ArrayList<DiscreteHandlingLinkedEntity> path, DijkstraComparator dijkstraComparator){
		for (DiscreteHandlingLinkedEntity each : path)
			each.getDijkstraComparatorList().remove(dijkstraComparator);
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////////////////////
	// Reporting
	// ////////////////////////////////////////////////////////////////////////////////////////////////////


	// TODO- REFACTOR FOR BETTER IMPLEMENTATION - used for quick checking of all routes configured or left unresolved!
	public static void printRouteReport(DiscreteHandlingLinkedEntity origin, DiscreteHandlingLinkedEntity destination, 
			Route route, MovingEntity movingEntity){
		
		if (printManagerReport.getValue()) {
			if(route != null){
				managerReportFile.putStringTabs(origin.getName(), 1);
				managerReportFile.putStringTabs(destination.getName(), 1);
				managerReportFile.putStringTabs(HandyUtils.arraylistToString(new ArrayList<>(route.getRouteSegmentsList())), 1);
				managerReportFile.putDoubleWithDecimalsTabs(route.getDijkstraWeight(),
					ReportAgent.getReportPrecision(), 1);
				managerReportFile.putStringTabs("FoundRoute", 1);
				if(movingEntity != null)
					managerReportFile.putStringTabs(movingEntity.getName(), 1);

				managerReportFile.newLine();
				managerReportFile.flush();
			} else{ 
				managerReportFile.putStringTabs(origin.getName(), 1);
				managerReportFile.putStringTabs(destination.getName(), 3);
				managerReportFile.putStringTabs("Unresolved", 1);
				if(movingEntity != null)
					managerReportFile.putStringTabs(movingEntity.getName(), 1);

				managerReportFile.newLine();
				managerReportFile.flush();
			}
		}
	}
	
	public void printManagerReportHeader(){	
		managerReportFile.putStringTabs("Origin", 1);
		managerReportFile.putStringTabs("Destination", 1);
		managerReportFile.putStringTabs("RouteSegments", 1);
		managerReportFile.putStringTabs("Distance", 1);
		managerReportFile.putStringTabs("Status", 1);
		managerReportFile.putStringTabs("Unrsolved Enttiy", 1);

		managerReportFile.newLine();
		managerReportFile.flush();	
		
		// Print units
		managerReportFile.putTabs(3);
		managerReportFile.putStringTabs("m",1);

		managerReportFile.newLine();
		managerReportFile.flush();	

	}

}
