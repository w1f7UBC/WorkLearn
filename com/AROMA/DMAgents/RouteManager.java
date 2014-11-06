package com.AROMA.DMAgents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.PriorityQueue;

import worldwind.DataListMk2;
import worldwind.DataListMk3;
import worldwind.DataListMk5;
import DataBase.Database;

import com.AROMA.Logistics.BulkMaterial;
import com.AROMA.Logistics.DiscreteHandlingLinkedEntity;
import com.AROMA.Logistics.EntranceBlock;
import com.AROMA.Logistics.ExitBlock;
import com.AROMA.Logistics.Facility;
import com.AROMA.Logistics.LinkedEntity;
import com.AROMA.Logistics.LoadingBay;
import com.AROMA.Logistics.MovingEntity;
import com.AROMA.Logistics.ReportAgent;
import com.AROMA.Logistics.Route;
import com.AROMA.Logistics.RouteEntity;
import com.AROMA.Logistics.RouteSegment;
import com.AROMA.Logistics.Transshipment;
import com.AROMA.Logistics.DiscreteHandlingLinkedEntity.DijkstraComparator;
import com.AROMA.Utils.HandyUtils;
import com.AROMA.Utils.HashMapList;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Vec3d;

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
	private static final EntityInput<Database> routeDB;

	protected static FileEntity managerReportFile;        // The file to store the manager reports
	
	static {		
		// TODO bad implementation! set true to print out the configured or unresolved routes report
		printManagerReport = new BooleanInput("PrintRoutesReport", "Report", false);
		routeDB = new EntityInput<Database>(Database.class, "RouteDataBase", "Key Inputs", null);
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
			tempRoute = RouteManager.getRoute(originBay, exit,movingEntity, null, Route_Type.SHORTEST, false, Double.POSITIVE_INFINITY, null);
			distance += tempRoute.getDijkstraWeight();
			ArrayList<DiscreteHandlingLinkedEntity> tempRouteSegments = new ArrayList<>(
				tempRoute.getRouteSegmentsList());
			
			//exit to entrance
			tempRoute = RouteManager.getRoute(exit, entrance,movingEntity, null, Route_Type.SHORTEST, false, Double.POSITIVE_INFINITY, null);
			distance += tempRoute.getDijkstraWeight();
			tempRouteSegments.addAll(tempRoute.getRouteSegmentsList());

			//entrance to destination bay
			tempRoute = RouteManager.getRoute(entrance,destinationBay,movingEntity, null, Route_Type.SHORTEST, false, Double.POSITIVE_INFINITY, null);
			distance += tempRoute.getDijkstraWeight();
			tempRouteSegments.addAll(tempRoute.getRouteSegmentsList());
			
			finalRoute = new Route(originBay, destinationBay, distance, movingEntity, Route_Type.SHORTEST);
			finalRoute.setRoute(tempRouteSegments);
			routesList.add(routeName, finalRoute);
		} else{
			finalRoute = RouteManager.getRoute(originBay, destinationBay, movingEntity, null, Route_Type.SHORTEST, false, Double.POSITIVE_INFINITY, null);
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
			T destination, MovingEntity movingEntity, BulkMaterial bulkMaterial, 
			Route_Type routingRule, boolean transshipmentAllowed, double weightCap, ArrayList<T> tabuList) {
		System.out.println("------------------------Getting new Route ---------------------------------");
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
			return computeAStarPath(origin, destination, movingEntity, bulkMaterial, routingRule,transshipmentAllowed, weightCap, tabuList);
			//return computeDijkstraPath(origin, destination, movingEntity, bulkMaterial, routingRule,transshipmentAllowed, weightCap, tabuList);
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
			T origin, T destination, MovingEntity movingEntity, BulkMaterial bulkMaterial, Route_Type routingRule,
			boolean transshipmentAllowed, double weightCap, ArrayList<T> tabuList) {
		double destinationWeight = Double.POSITIVE_INFINITY;
		double weightThroughTransshipment = Double.POSITIVE_INFINITY;
		Route routeThroughTransshipment = null;
		ArrayList<T> tempTabuList = new ArrayList<T> (0);
		if(tabuList != null)
			tempTabuList.addAll(tabuList);
		
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
				if(each instanceof Transshipment && !transshipmentAllowed || tempTabuList.contains(each))
					continue;
				
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
						
						// add transshipment to tabulist so it won't visit current tabu list over and over again!
						tempTabuList.add((T) each);
						
						//explore routes from transshipment to destination based on routing rule.
						if(routingRule == Route_Type.FASTEST)
							tempRoute = ((Facility)each).getTransportationManager().getFastestTranspotationRoute(bulkMaterial, (T)each, destination, weightCap-weightThroughU, tempTabuList);
						else if (routingRule == Route_Type.SHORTEST)
							tempRoute = ((Facility)each).getTransportationManager().getShortestTranspotationRoute(bulkMaterial, (T)each, destination, weightCap-weightThroughU, tempTabuList);
						// leastcost should be passed on as a per unit of material transportationcost in transshipment
						else if (routingRule == Route_Type.LEASTCOST)
							tempRoute = ((Facility)each).getTransportationManager().getLeastCostTranspotationRoute(bulkMaterial, (T)each, destination, weightCap-weightThroughU, tempTabuList);
						
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
	
	
	public static <T extends DiscreteHandlingLinkedEntity> Route computeAStarPath(T origin, T destination, MovingEntity movingEntity, BulkMaterial bulkMaterial, Route_Type routingRule,
			boolean transshipmentAllowed, double weightCap, ArrayList<T> tabuList) {
		int openedEntities = 0;
		int iterationCount =0;
		int containsC = 0;
		int removeC=0;
		int addC =0;
		int getc=0;
		
		System.out.println("In AStar");
		DataListMk5 closedset = new DataListMk5();
		DataListMk5 openset = new DataListMk5();
		DataListMk3 camefrom = new DataListMk3();
		
		Vec3d originPos = origin.getPosition();
		Vec3d destinationPos = destination.getPosition();
		double heuristicCost = computeHeuristic(originPos, destinationPos);
		double gScoreStart = 0.0;
		double fScoreStart = gScoreStart + heuristicCost;

		openset.add(origin, movingEntity, gScoreStart, fScoreStart);
		addC++;
		
		while(!openset.isEmpty()){
			openedEntities++;
			getc+=2;
			Object[] current = openset.getObjectsIndex(0);
			System.out.println("object " + current[0] + current[1]);
			DiscreteHandlingLinkedEntity currentEntity = (DiscreteHandlingLinkedEntity) current[0];
			MovingEntity currentME = (MovingEntity) current[1];
			Double[] currentScores = openset.getScoresIndex(0);

			if (currentEntity == destination){
				System.out.println("Opened Entities: " + openedEntities);
				System.out.println("Iterations: " + iterationCount);
				System.out.println("Contains: " + containsC);
				System.out.println("Remove: " + removeC);
				System.out.println("Add: " + addC);
				System.out.println("Get: " + getc);

				Object[] reconstructedpath = reconstructAStarPath(origin, movingEntity, destination, currentME, camefrom);
				ArrayList<DiscreteHandlingLinkedEntity> arrayList = (ArrayList<DiscreteHandlingLinkedEntity>) reconstructedpath[0];
				ArrayList<DiscreteHandlingLinkedEntity> path = arrayList;
				return setAStarRoute(origin, destination, movingEntity, currentScores[1], path, routingRule);
			} //is destination end
			
//			if (currentEntity instanceof Transshipment && !transshipmentAllowed){
//				openset.remove(currentEntity, currentME);
//				closedset.add(currentEntity, currentME, 0.0, 0.0);
//				continue;
//			} // is invalid end
			
			else if (closedset.contains(currentEntity, currentME)){
				System.out.println("in Closedset");
				openset.remove(currentEntity, currentME);
				iterationCount+=2;
				containsC ++;
			} // is invalid end
			
			else if ((currentEntity instanceof RouteSegment && ((RouteSegment) currentEntity).getTransportMode() == currentME.getTransportMode()) || 
					(currentEntity instanceof RouteEntity && ((RouteEntity) currentEntity).getTransportMode() == currentME.getTransportMode()) ||  
					currentEntity instanceof Facility && !(currentEntity instanceof Transshipment)) {
				System.out.println("in route segment");
				openset.remove(currentEntity, currentME);
				closedset.add(currentEntity, currentME, 0.0, 0.0);
				iterationCount += 3;
				containsC++;
				removeC++;
				addC++;
				
				Object[] current1 = openset.getObjectsIndex(0);
				System.out.println("object after remove " + current1[0] + current1[1]);
				
				for(LinkedEntity neighborEntity : currentEntity.getNextLinkedEntityList()){
	
					Object[] neighborObject = {neighborEntity, currentME};

					if(closedset.contains(neighborEntity, currentME)){
						iterationCount+=1;
						containsC++;
						continue;
					}
					else{ // if it does not contain neighborObject
						iterationCount+=1;
						containsC++;
						Double weight = null;
						Double tentativeGScore;
						if(routingRule == Route_Type.FASTEST)
							weight =  ((DiscreteHandlingLinkedEntity) neighborEntity).getTravelTime((MovingEntity) currentME);
						else if (routingRule == Route_Type.SHORTEST)
							weight = ((DiscreteHandlingLinkedEntity) neighborEntity).getLength();
						// leastcost is calculated based on per unit of material transportationcost
						else if (routingRule == Route_Type.LEASTCOST)
							weight = ((DiscreteHandlingLinkedEntity) neighborEntity).getTravelTime((MovingEntity) currentME)*
							movingEntity.getTransportationCost(bulkMaterial);
					
						tentativeGScore = currentScores[0] + weight*100000;
						//if the openset contains neighbor entity and moving entity already or if it contains one with a higher gscore then...
						if (tentativeGScore < weightCap*10000){
							
							if(!openset.contains(neighborEntity, currentME)){
								iterationCount+=3;
								containsC++;
								addC+=2;
								camefrom.add(neighborEntity, currentME, currentEntity, currentME);
								double gscoreNeighbor = tentativeGScore;
								double fscoreNeighbor = gscoreNeighbor + computeHeuristic(neighborEntity.getPosition(), destination.getPosition());
								openset.add(neighborEntity, currentME, gscoreNeighbor, fscoreNeighbor);
							}
							else if(tentativeGScore < openset.getKey(neighborEntity, currentME)[0]){
								getc++;
								iterationCount+=5;
								containsC++;
								addC+=2;
								removeC++;
								camefrom.add(neighborEntity, currentME, currentEntity, currentME);
								double gscoreNeighbor = tentativeGScore;
								double fscoreNeighbor = gscoreNeighbor + computeHeuristic(neighborEntity.getPosition(), destination.getPosition());
								openset.remove(neighborEntity, currentME);
								openset.add(neighborEntity, currentME, gscoreNeighbor, fscoreNeighbor);
							}
							else{
								getc++;
								containsC++;
								iterationCount+=2;
							}
						}
						else{
							closedset.add(neighborEntity, currentME, 0.0, 0.0);
							addC++;
							iterationCount++;
						}
					}
				}
				
			} // route end
			
			else if (currentEntity instanceof Transshipment && transshipmentAllowed){
				System.out.println("in Closedset");
				openset.remove(currentEntity, currentME);
				closedset.add(currentEntity, currentME, 0.0, 0.0);
				iterationCount+=3;
				containsC++;
				addC++;
				removeC++;
				//for neighbors 
				for(LinkedEntity neighborEntity : currentEntity.getNextLinkedEntityList()){
					for (MovingEntity currentMovingEntity: ((Facility)current[0]).getTransportationManager().getTransporters()){
						Object[] neighborObject = {neighborEntity, currentMovingEntity};
						if (currentMovingEntity.getAcceptingBulkMaterialList().contains(bulkMaterial)){
							
							if (closedset.contains(neighborEntity, currentMovingEntity)){
								iterationCount+=1;
								containsC++;
								continue;
							}
							else{
								containsC++;
								iterationCount+=1;
								Double weight = null;
								Double tentativeGScore;
								if(routingRule == Route_Type.FASTEST)
									weight =  ((DiscreteHandlingLinkedEntity) neighborEntity).getTravelTime((MovingEntity) currentMovingEntity);
								else if (routingRule == Route_Type.SHORTEST)
									weight = ((DiscreteHandlingLinkedEntity) neighborEntity).getLength();
								// leastcost is calculated based on per unit of material transportationcost
								else if (routingRule == Route_Type.LEASTCOST)
									weight = ((DiscreteHandlingLinkedEntity) neighborEntity).getTravelTime((MovingEntity) currentMovingEntity)*
										movingEntity.getTransportationCost(bulkMaterial);
								
								tentativeGScore = currentScores[0] + weight*100000;
								
								if (tentativeGScore < weightCap){
									//if the openset contains neighbor entity and moving entity already or if it contains one with a higher gscore then...
									if(!openset.contains(neighborEntity, currentME)){
										containsC++;
										addC+=2;
										iterationCount+=3;
										camefrom.add(neighborEntity, currentMovingEntity, currentEntity, currentME);
										double gscoreNeighbor = tentativeGScore;
										double fscoreNeighbor = gscoreNeighbor + computeHeuristic(neighborEntity.getPosition(), destination.getPosition());
										openset.add(neighborEntity, currentMovingEntity, gscoreNeighbor, fscoreNeighbor);
									}
									else if(tentativeGScore < openset.getKey(neighborEntity, currentME)[0]){
										getc++;
										containsC++;
										removeC++;
										addC+=2;
										iterationCount+=5;
										camefrom.add(neighborEntity, currentMovingEntity, currentEntity, currentME);
										double gscoreNeighbor = tentativeGScore;
										double fscoreNeighbor = gscoreNeighbor + computeHeuristic(neighborEntity.getPosition(), destination.getPosition());
										openset.remove(neighborEntity, currentMovingEntity);
										openset.add(neighborEntity, currentMovingEntity, gscoreNeighbor, fscoreNeighbor);
									}
								}
								else{
									addC++;
									iterationCount+=1;
									closedset.add(neighborEntity, currentME, 0.0, 0.0);
								}
						}
						}
					}
				}
			}
			else{
				System.out.println("in else");
			//	Must not have been allowed for some reason. Removed from front
				openset.remove(currentEntity, currentME);
				containsC++;
				removeC++;
				iterationCount+=2;
			}
		} // while openset empty end
		return null;
	}
		
		
	public static <T extends DiscreteHandlingLinkedEntity> Object[] reconstructAStarPath(T origin, MovingEntity originMovingEntity, T destination, MovingEntity destinationMovingEntity, DataListMk3 camefrom){
		Object[] originObjectPair = {origin, originMovingEntity};
		Object[] destinationObjectPair = {destination, destinationMovingEntity};
		
		
		LinkedList<DiscreteHandlingLinkedEntity> reversepath = new LinkedList<>();
		ArrayList<DiscreteHandlingLinkedEntity> path = new ArrayList<>();
		ArrayList<MovingEntity> movingEntitylist = new ArrayList<>();
		Object[] currentEntity = destinationObjectPair;

		
		while (camefrom.containsKey(currentEntity[0], currentEntity[1])){
			reversepath.add((DiscreteHandlingLinkedEntity) currentEntity[0]);
			if(!movingEntitylist.contains(currentEntity[1])){
				movingEntitylist.add((MovingEntity) currentEntity[1]);
			}
			currentEntity = camefrom.getValue(currentEntity[0], currentEntity[1]);
		}

		reversepath.add(origin);
		
		while (!reversepath.isEmpty()) {
			path.add(reversepath.pollLast());
		}
		
		if(!movingEntitylist.contains(originMovingEntity))
			movingEntitylist.add(originMovingEntity);
//		for (MovingEntity m : movingEntitylist){
//			//System.out.println("ssdg" + origin + destination + m);
//		}
		
		
		Object[] endSets = {path, movingEntitylist};
		//System.out.println("reached end of reconstructing Path");
		return endSets;
	}
	
	public static double computeHeuristic(Vec3d from, Vec3d to){
		
		double distance = Math.pow(from.x-to.x,2)+Math.pow(from.y-to.y,2)+Math.pow(from.z-to.z,2);
		return (Math.sqrt(distance))/100000;
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
	

	
//	private static <T extends DiscreteHandlingLinkedEntity> Route setAStarRoute(T origin,
//			T destination, MovingEntity movingEntityOrigin, MovingEntity movingEntityDestination, Route_Type routingRule) {
//		LinkedList<DiscreteHandlingLinkedEntity> reversePath = new LinkedList<>();
//		ArrayList<DiscreteHandlingLinkedEntity> path = new ArrayList<>();
//		
//		
//		return tempRoute;
//	}
//	
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
	
	private static <T extends DiscreteHandlingLinkedEntity> Route setAStarRoute(T origin, T destination, MovingEntity movingEntity, Double weight, 	ArrayList<DiscreteHandlingLinkedEntity> routeData, Route_Type routingRule){
		
		Route tempRoute = new Route(origin, destination, weight, movingEntity, routingRule);
		String tempKey = getRouteName(origin, destination, movingEntity);
		
		tempRoute.setRoute(routeData);
		routesList.add(tempKey, tempRoute);
		
		RouteManager.printRouteReport(origin, destination, tempRoute, null);
		
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

