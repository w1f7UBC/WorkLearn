package com.jaamsim.units;


public class CostPerMassUnit extends Unit {
	static {
		Unit.setSIUnit(CostPerMassUnit.class, "$/kg");
	}

	public CostPerMassUnit() {
	}

}
