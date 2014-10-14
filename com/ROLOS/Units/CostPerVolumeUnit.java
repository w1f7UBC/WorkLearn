package com.ROLOS.Units;

import com.jaamsim.units.Unit;


public class CostPerVolumeUnit extends Unit {
	static {
		Unit.setSIUnit(CostPerVolumeUnit.class, "$/m3");
	}

	public CostPerVolumeUnit() {
	}

}
