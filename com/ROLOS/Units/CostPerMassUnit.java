package com.ROLOS.Units;

import com.jaamsim.units.Unit;


public class CostPerMassUnit extends Unit {
	static {
		Unit.setSIUnit(CostPerMassUnit.class, "$/kg");
	}

	public CostPerMassUnit() {
	}

}
