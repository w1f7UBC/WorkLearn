package com.AROMA.Logistics;

import com.AROMA.DMAgents.RouteManager.Transport_Mode;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.EnumInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringListInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.SpeedUnit;


/**
 * Implicit representation of route. i.e. travel and traffic on routeentities are not modeled explicitly.
 * RouteEntities can contain multiple segments, each segment will be implicit, and only be represented by
 * an array of inputs (map) including name, segments length, speedlimit, type.
 *  TODO MERGE IT WITH ROUTE SEGMENT
 *  
 * @author Saeed Ghafghazi (email: saeedghaf@gmail.com) - Jul 25, 2014 
 * 
 * @modified_by
 */
public class RouteEntity extends DiscreteHandlingLinkedEntity {

	@Keyword(description = "The transportation mode that this moving entity is allowed to travel on.", 
			example = "Route1 TransportMode { ROAD }")
	private final EnumInput<Transport_Mode> transportMode;
	
	@Keyword(description = "The list of origins.", 
			example = "Route1 OriginList { Road1 Road2 }")
	private final EntityListInput<DiscreteHandlingLinkedEntity> originList;	
	
	@Keyword(description = "The list of destinations.", 
			example = "Route1 DestinationList { Enterblock1 Enterblock2 }")
	private final EntityListInput<DiscreteHandlingLinkedEntity> destinationList;	
	
	@Keyword(description = "The name of segments constituing this route entity.", 
			example = "Route1 Segments { 'Road1' 'Road2' 'Rail1' }")
	private final StringListInput segmentList;	
	
	@Keyword(description = "The length list of the route entity.", 
			example = "Route2 LengthList { 100 170 500 km }")
	private final ValueListInput lengthList;	
	
	@Keyword(description = "The speed limit list of the route entity.", 
			example = "Route2 SpeedLimitList { 100 70 50 km/h }")
	private final ValueListInput speedLimitList;	
	
	{
		transportMode = new EnumInput<>(Transport_Mode.class, "TransportMode", "Key Inputs", Transport_Mode.ROAD);
		this.addInput(transportMode);
		
		segmentList = new StringListInput("Segments", "Key Inputs", null);
		this.addInput(segmentList);
		
		originList = new EntityListInput<>(DiscreteHandlingLinkedEntity.class, "OriginList", "Key Inputs", null);
		this.addInput(originList);
		
		destinationList = new EntityListInput<>(DiscreteHandlingLinkedEntity.class, "DestinationList", "Key Inputs", null);
		this.addInput(destinationList);
		
		lengthList = new ValueListInput("LengthList", "Key Inputs", null);
		lengthList.setUnitType(DistanceUnit.class);
		lengthList.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(lengthList);
		
		speedLimitList = new ValueListInput("SpeedLimitList", "Key Inputs", null);
		speedLimitList.setUnitType(SpeedUnit.class);
		speedLimitList.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(speedLimitList);
	}
	
	public RouteEntity() {
	}
	
	@Override
	public void validate() {
		super.validate();
		if(segmentList.getValue() != null){
			if (lengthList.getValue() == null || lengthList.getValue().size() != segmentList.getValue().size())
				throw new InputErrorException("Lengthlist and SpeedLimitByEntityType must both be set together and of the same size!");
			
		}
	}

	public Transport_Mode getTransportMode(){
		return transportMode.getValue();
	}
	
	@Override
	public double getLength() {
		return lengthList.getValue().sum();
	}
	
	@Override
	public double getTravelTime(MovingEntity movingEntity) {
		double total=0;
		for(int i=0; i<lengthList.getValue().size(); i++)
			total += lengthList.getValue().get(i)/ Math.min(speedLimitList.getValue().get(i), movingEntity.getInternalSpeed());
		return total;
	}
	
	
	@Override
	public double getTravelCost(MovingEntity movingEntity) {
		return movingEntity.getOperatingCost() * getTravelTime(movingEntity);
	}
	
	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);
		// populate previous and next LinkedEntitylists
		if(in == originList){
			for (LinkedEntity each : originList.getValue()) {
				each.addToNextLinkedEntityList(this);
				this.addToPreviousLinkedEntityList(each);
			}
		}
		if(in == destinationList){
			for (LinkedEntity each : destinationList.getValue()) {
				this.addToNextLinkedEntityList(each);
				each.addToPreviousLinkedEntityList(this);
			}
		}
	}

}
