package com.ROLOS.Logistics;

import java.util.ArrayList;

import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.units.DistanceUnit;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.StringListInput;

/**
 * Implicit representation of route. i.e. travel and traffic on routeentities are not modeled explicitly.
 * RouteEntities can contain multiple segments, each segment will be implicit, and only be represented by
 * an array of inputs (map) including name, segments length, speedlimit, type.
 *  
 * @author Saeed Ghafghazi (email: saeedghaf@gmail.com) - Jul 25, 2014 
 * 
 * @modified_by
 */
public class RouteEntity extends DiscreteHandlingLinkedEntity {

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
		segmentList = new StringListInput("Segments", "Key Inputs", null);
		this.addInput(segmentList);
		
		lengthList = new ValueListInput("LengthList", "Key Inputs", null);
		lengthList.setUnitType(DistanceUnit.class);
		lengthList.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(lengthList);
		
		speedLimitList = new ValueListInput("SpeedList", "Key Inputs", null);
		speedLimitList.setUnitType(DistanceUnit.class);
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

}
