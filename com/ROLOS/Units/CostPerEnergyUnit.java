package com.ROLOS.Units;

import com.jaamsim.units.Unit;

public class CostPerEnergyUnit extends Unit {
	
	static {
		Unit.setSIUnit(CostPerEnergyUnit.class, "$/J");
	}
	public CostPerEnergyUnit() {
	}

}
