package com.jaamsim.input;

import java.util.ArrayList;

import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.InputErrorException;


public class WorldWindCameraInput extends Input<Vec3d> {
	
	//private Vec3d location;
	private DoubleVector location;
	private double time;
	
	public WorldWindCameraInput(String key, String cat) {
		super(key, cat, null);
	}
	
	@Override
	public void parse(KeywordIndex kw) throws InputErrorException {
		
		ArrayList<String> strings = new ArrayList<String>(kw.numArgs());
		for (int i = 0; i < kw.numArgs(); i++) {
			strings.add(kw.getArg(i));
		}
		ArrayList<ArrayList<String>> keys = InputAgent.splitForNestedBraces(strings);
		ArrayList<String> timeInput = keys.get(0);
		ArrayList<String> valInput = keys.get(1);
		if (timeInput.size() != 4 || !timeInput.get(0).equals("{") || !timeInput.get(timeInput.size()-1).equals("}")) {
			throw new InputErrorException("Time entry not formated correctly: %s", timeInput.toString());
		}
		if (valInput.size() != 7 || !valInput.get(0).equals("{") || !valInput.get(valInput.size()-1).equals("}")) {
			throw new InputErrorException("Value entry not formated correctly: %s", valInput.toString());
		}

		DoubleVector times = Input.parseDoubles(timeInput.subList(1, 3), 0.0d, Double.POSITIVE_INFINITY, TimeUnit.class);
		time = times.get(0);
		location = new DoubleVector();
		
		DoubleVector vals = Input.parseDoubles(valInput.subList(1, 4), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, DimensionlessUnit.class);
		for(int i=0;i<3;i++){
			location.add(vals.get(i));
		}
		if(isNumeric(valInput.get(3))){
			location.add(Input.parseDouble(valInput.get(3), Double.NEGATIVE_INFINITY,  Double.POSITIVE_INFINITY));
		}else{
			if(valInput.get(3).equals("none")){
				location.add(Double.NEGATIVE_INFINITY);
			}else{
				throw new InputErrorException("Value entry not formated correctly: %s", valInput.toString());
			}
		}
		if(isNumeric(valInput.get(4))){
			location.add(Input.parseDouble(valInput.get(4), Double.NEGATIVE_INFINITY,  Double.POSITIVE_INFINITY));
		}else{
			if(valInput.get(4).equals("none")){
				location.add(Double.NEGATIVE_INFINITY);
			}else{
				throw new InputErrorException("Value entry not formated correctly: %s", valInput.toString());
			}
		}
				
		}

	
	public DoubleVector getDoubles(){
		return location;
	}
	
	public double getTime(){
		return time;
	}
	
	public static boolean isNumeric(String str)
	{
	  return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
	}
}
