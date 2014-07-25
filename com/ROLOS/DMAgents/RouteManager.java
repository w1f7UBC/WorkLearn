package com.ROLOS.DMAgents;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;

import DataBase.DataBaseObject;

import com.ROLOS.Logistics.DiscreteHandlingLinkedEntity;
import com.ROLOS.Logistics.DiscreteHandlingLinkedEntity.DijkstraComparator;
import com.ROLOS.Logistics.EntranceBlock;
import com.ROLOS.Logistics.ExitBlock;
import com.ROLOS.Logistics.LinkedEntity;
import com.ROLOS.Logistics.LoadingBay;
import com.ROLOS.Logistics.LogisticsEntity;
import com.ROLOS.Logistics.MovingEntity;
import com.ROLOS.Logistics.ReportAgent;
import com.ROLOS.Logistics.Route;
import com.ROLOS.Logistics.RouteSegment;
import com.ROLOS.Utils.HandyUtils;
import com.ROLOS.Utils.HashMapList;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.jaamsim.input.Keyword;

public class RouteManager extends DisplayEntity {
	
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
	 * @return This method constructs the route from origin loading bay to the destination loading bay if 
	 * route doesn't already exist. 
	 */
	public static Route getBayToBayRoute(LoadingBay originBay, LoadingBay destinationBay, MovingEntity movingEntity) {
		String routeName = originBay.getName()+ "-" +destinationBay.getName();
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
			tempRoute = RouteManager.getRoute(originBay, exit,movingEntity);
			distance += tempRoute.getDistance();
			ArrayList<DiscreteHandlingLinkedEntity> tempRouteSegments = new ArrayList<>(
				tempRoute.getRouteSegmentsList());
			ArrayList<LogisticsEntity> retainedEntities = new ArrayList<>(tempRoute.getHandlingMovingEntitiesList());
			
			//exit to entrance
			tempRoute = RouteManager.getRoute(exit, entrance,movingEntity);
			distance += tempRoute.getDistance();
			tempRouteSegments.addAll(tempRoute.getRouteSegmentsList());
			retainedEntities.retainAll(tempRoute.getHandlingMovingEntitiesList());

			//entrance to destination bay
			tempRoute = RouteManager.getRoute(entrance,destinationBay,movingEntity);
			distance += tempRoute.getDistance();
			tempRouteSegments.addAll(tempRoute.getRouteSegmentsList());
			retainedEntities.retainAll(tempRoute.getHandlingMovingEntitiesList());
			
			finalRoute = new Route(originBay, destinationBay, distance,retainedEntities);
			finalRoute.setRoute(tempRouteSegments);
			routesList.add(routeName, finalRoute);
		} else{
			finalRoute = RouteManager.getRoute(originBay, destinationBay, movingEntity);
		}
		return finalRoute;
	}

	/**
	 * @return route from origin to destination or tries to compute one if doesn't exist.
	 */
	public static <T extends DiscreteHandlingLinkedEntity> ArrayList<Route> getRoute(
			T origin, T destination) {
		String tempKey = origin.getName() + "-" + destination.getName();
		if (!routesList.contains(tempKey)) {
			RouteManager.computeDijkstraPath(origin, destination, null);
		}

		if(routesList.get(tempKey).get(0).getDistance() == Double.POSITIVE_INFINITY)
			return new ArrayList<Route>();
		else
			return routesList.get(tempKey);
	}

	
	// Returns route that handles the passed moving entity or computes one if
	// existed

	/**
	 * @param origin if facility is passed as origin and destination, 
	 * this method should be used for estimating distance from facility to facility and not for planning moving entity's moves
	 * @return route that handles the passed moving entity or computes one if
	 * existed. null if such route doesn't exist. 
	 */
	public static <T extends DiscreteHandlingLinkedEntity> Route getRoute(T origin,
			T destination, MovingEntity movingEntity) {
		String tempKey = origin.getName() + "-" + destination.getName();
		
		if(unResolvedRoutesList.get(movingEntity).contains(tempKey))
			return null;
		
		for (Route each : RouteManager.getRoute(origin, destination))
			if (each.getHandlingMovingEntitiesList().contains(
					movingEntity.getProtoTypeEntity()))
				return each;

		return computeDijkstraPath(origin, destination, movingEntity);
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
	 * @param movingEntity
	 *            route is computed that handles movingEntity throughout. if
	 *            null is passed shortest path is calculated
	 */
	public static <T extends DiscreteHandlingLinkedEntity> Route computeDijkstraPath(
			T origin, T destination, MovingEntity movingEntity) {
		DijkstraComparator dijkstraComparator = new DijkstraComparator();
		origin.getDijkstraComparatorList().add(dijkstraComparator, null, 0,
				0.0d);
		PriorityQueue<T> vertexQueue = new PriorityQueue<>(10,dijkstraComparator);
		vertexQueue.add(origin);

		while (!vertexQueue.isEmpty()) {
			T u = vertexQueue.poll();
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

				double weight;
				if (each.checkIfHandles(movingEntity)
						&& each instanceof RouteSegment) {
					weight = ((RouteSegment) each).getLength();
					double weightThroughU = u.getDijkstraComparatorList()
							.getValueListFor(dijkstraComparator, 0).get(0)
							+ weight;
					if (weightThroughU < ((DiscreteHandlingLinkedEntity) each)
							.getDijkstraComparatorList()
							.getValueListFor(dijkstraComparator, 0).get(0)) {
						vertexQueue.remove(each);
						((DiscreteHandlingLinkedEntity) each)
								.getDijkstraComparatorList().remove(
										dijkstraComparator);
						((DiscreteHandlingLinkedEntity) each)
								.getDijkstraComparatorList().add(
										dijkstraComparator, u, 0,
										weightThroughU);
						if (each.equals(destination)) {
							return setRoute(origin, destination, dijkstraComparator);
						} else
							vertexQueue.add((T) each);
					}
				} else {
					if (each.equals(destination)) {
						double weightThroughU = u.getDijkstraComparatorList()
								.getValueListFor(dijkstraComparator, 0).get(0);
						if (weightThroughU < ((DiscreteHandlingLinkedEntity) each)
								.getDijkstraComparatorList()
								.getValueListFor(dijkstraComparator, 0).get(0)) {
							vertexQueue.remove(each);
							((DiscreteHandlingLinkedEntity) each)
									.getDijkstraComparatorList().remove(
											dijkstraComparator);
							((DiscreteHandlingLinkedEntity) each)
									.getDijkstraComparatorList().add(
											dijkstraComparator, u, 0,
											weightThroughU);
							return setRoute(origin, destination, dijkstraComparator);
						}
					}
				}
			}
		}
		// if method has reached here and not returned yet, it means that there
		// isn't a path from origin to destination, to save this knowledge
		// initiate a route with null routeSementsList and add to routesList! or add it to the unresolved list if moving entity is specified
		String tempKey = origin.getName() + "-" + destination.getName();
		if (movingEntity == null) {
			Route tempRoute = new Route(origin, destination,
					Double.POSITIVE_INFINITY, new ArrayList<LogisticsEntity>());
			routesList.add(tempKey, tempRoute);
			RouteManager.printRouteReport(origin, destination, null, null);
		} else{
			unResolvedRoutesList.add(movingEntity, tempKey);
			RouteManager.printRouteReport(origin, destination, null, movingEntity);
		}
		return null;
	}

	private static <T extends DiscreteHandlingLinkedEntity> Route setRoute(T origin,
			T destination, DijkstraComparator dijkstraComparator) {
		ArrayList<LogisticsEntity> retainedEntities = new ArrayList<>(1);

		LinkedList<DiscreteHandlingLinkedEntity> reversePath = new LinkedList<>();
		ArrayList<DiscreteHandlingLinkedEntity> path = new ArrayList<>();
		for (DiscreteHandlingLinkedEntity vertex = destination; vertex != null; vertex = vertex
				.getDijkstraComparatorList()
				.getSecondEntityList()
				.get(vertex.getDijkstraComparatorList().indexOf(
						dijkstraComparator)))
			reversePath.add(vertex);

		path.add(reversePath.pollLast());
		retainedEntities.addAll(path.get(0).getHandlingEntityTypeList());

		while (!reversePath.isEmpty()) {
			path.add(reversePath.pollLast());
			retainedEntities.retainAll(path.get(path.size() - 1)
					.getHandlingEntityTypeList());
		}

		String tempKey = origin.getName() + "-" + destination.getName();
		Route tempRoute = new Route(origin, destination, destination
				.getDijkstraComparatorList()
				.getValueListFor(dijkstraComparator, 0).get(0),
				retainedEntities);
		tempRoute.setRoute(path);
		routesList.add(tempKey, tempRoute);
		RouteManager.printRouteReport(origin, destination, tempRoute, null);
		// remove dijkstraComparator in all route segments after setting up the
		// route
		for (DiscreteHandlingLinkedEntity each : path)
			each.getDijkstraComparatorList().remove(dijkstraComparator);
		
		return tempRoute;

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
				managerReportFile.putStringTabs(HandyUtils.arraylistToString(route.getRouteSegmentsList()), 1);
				managerReportFile.putDoubleWithDecimalsTabs(route.getDistance(),
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
