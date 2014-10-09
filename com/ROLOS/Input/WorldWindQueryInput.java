package com.ROLOS.Input;

import java.util.ArrayList;

import DataBase.Query;

import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.units.TimeUnit;


public class WorldWindQueryInput extends Input {


	private double time;
	private Query query;
	
	public WorldWindQueryInput(String key, String cat) {
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
		if (valInput.size() != 3 || !valInput.get(0).equals("{") || !valInput.get(valInput.size()-1).equals("}")) {
			throw new InputErrorException("Value entry not formated correctly: %s", valInput.toString());
		}
		DoubleVector times = Input.parseDoubles(timeInput.subList(1, 3), 0.0d, Double.POSITIVE_INFINITY, TimeUnit.class);
		time = times.get(0);
		query = Input.parseEntity(keys.get(1).get(1), Query.class);
	}
	
	
	public Query getQuery(){
		return query;
	}
	
	public double getTime(){
		return time;
	}
}
