package com.jaamsim.units;


public class CostPerVolumeUnit extends Unit {
	static {
		Unit.setSIUnit(CostPerVolumeUnit.class, "$/m3");
	}

	public CostPerVolumeUnit() {
	}

}
