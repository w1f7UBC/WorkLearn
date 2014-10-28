package com.AROMA;

import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.UserSpecifiedUnit;
import com.sandwell.JavaSimulation.Entity;

public class AROMAGraphTester extends Entity {
	@Keyword(description = "The number of data points that can be displayed on the graph.\n" +
			" This parameter determines the resolution of the graph.",
	         example = "Graph1 NumberOfPoints { 200 }")
	protected final ValueInput testNumberX;
	
	@Keyword(description = "The number of data points that can be displayed on the graph.\n" +
			" This parameter determines the resolution of the graph.",
	         example = "Graph1 NumberOfPoints { 200 }")
	protected final ValueInput testNumberY;
	
	protected final UnitTypeInput unitX;
	
	protected final UnitTypeInput unitY;
	{
		// Key Inputs category

		testNumberX = new ValueInput("testNumberX", "Key Inputs", 5.0);
		testNumberX.setUnitType(UserSpecifiedUnit.class);
		this.addInput(testNumberX);
		
		testNumberY = new ValueInput("testNumberY", "Key Inputs", 5.0);
		testNumberY.setUnitType(UserSpecifiedUnit.class);
		this.addInput(testNumberY);
		
		unitX = new UnitTypeInput("unitX", "Key Inputs", UserSpecifiedUnit.class);
		this.addInput(unitX);
		
		unitY = new UnitTypeInput("unitY", "Key Inputs", UserSpecifiedUnit.class);
		this.addInput(unitY);
	}
	
	public Double getX(){
		return testNumberX.getValue();
	}
	
	public Double getY(){
		return testNumberY.getValue();
	}
	
	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);
		if (in == unitX){
			testNumberX.setUnitType(unitX.getUnitType());
			this.getOutputHandle("testNumberX").setUnitType(unitX.getUnitType());
		}
		if (in == unitY){
			testNumberY.setUnitType(unitY.getUnitType());
			this.getOutputHandle("testNumberY").setUnitType(unitY.getUnitType());
		}
	}
	
	@Output(name = "testNumberX", 
			description = "Price of the bulkmaterial for the current period.")
	public double getX(double simTime) {
		return this.getX();
	}
	
	@Output(name = "testNumberY", 
			description = "Price of the bulkmaterial for the current period.")
	public double getY(double simTime) {
		return this.getY();
	}
}
