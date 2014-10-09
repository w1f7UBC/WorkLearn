package com.ROLOS.Units;

import com.jaamsim.units.Unit;
public class CostPerMassPerDistUnit extends Unit {
	static {
		Unit.setSIUnit(CostPerMassPerDistUnit.class, "$/kg/m");
	}

	public CostPerMassPerDistUnit() {}

}
