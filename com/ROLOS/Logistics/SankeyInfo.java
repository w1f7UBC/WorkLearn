package com.ROLOS.Logistics;

import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.OutputHandle;
import com.jaamsim.units.UserSpecifiedUnit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.Group;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Tester;

public class SankeyInfo extends Entity {

	@Keyword(description = "The bulk material entity types that this sankey info entity will trace.", 
			example = "WoodChipper TracedProduct { Sawdust }")
	private final EntityInput<BulkMaterial> tracedProduct;
	
	@Keyword(description = "The group of facilities whose values for the product will be calculated in the output.", 
			example = "WoodChipper FacilityGroup { Sawmills }")
	private final EntityInput<Group> facilityGroup;
		
	{
		tracedProduct = new EntityInput<>(BulkMaterial.class, "TracedProduct", "Key Inputs", null);
		this.addInput(tracedProduct);
		
		facilityGroup = new EntityInput<>(Group.class, "FacilityGroup", "Key Inputs", null);
		this.addInput(facilityGroup);
	}
	
	public SankeyInfo() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void validate() throws InputErrorException {
		super.validate();
		if(facilityGroup == null || tracedProduct == null || facilityGroup.getValue().getType() != Facility.class){
			throw new InputErrorException("TracedProduct and FacilityGroup keywords should be set and FacilityGroup should be a group of type Facility for %s!",this.getName());
		}
	}
		
	@Override
	public OutputHandle getOutputHandle(String outputName) {
		OutputHandle out = super.getOutputHandle(outputName);
		if( out.getUnitType() == UserSpecifiedUnit.class )
			out.setUnitType(tracedProduct.getValue().getEntityUnit());
		return out;
	}
	
	@Output(name = "TotalProduction", 
			description = "The total production of tracedproduct at present time.", 
			unitType = UserSpecifiedUnit.class)
	public double getTotalProduction(double simTime) {
		double tempAmount = 0.0d;
		for (Entity eachFacility : facilityGroup.getValue().getList()) {
			tempAmount += ((Facility) eachFacility).getStockList().getValueFor(
					tracedProduct.getValue(), 14);
		}
		return tempAmount;
	}
	
	@Output(name = "TotalReceived", 
			description = "The total tracedproduct that all facilitygroup facilities received at present time.", 
			unitType = UserSpecifiedUnit.class)
	public double getTotalReceived(double simTime) {
		double tempAmount = 0.0d;
		for (Entity eachFacility : facilityGroup.getValue().getList()) {
			tempAmount += ((Facility) eachFacility).getStockList().getValueFor(
					tracedProduct.getValue(), 15);
		}
		return tempAmount;
	}

}
