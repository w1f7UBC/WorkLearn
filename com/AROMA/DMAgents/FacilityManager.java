package com.AROMA.DMAgents;

import java.util.ArrayList;

import com.AROMA.AROMAEntity;
import com.AROMA.Logistics.Facility;


public class FacilityManager extends AROMAEntity {
	private static final ArrayList<FacilityManager> allInstances;

	{
		
	}
	
	private Facility facility;
	
	static {
		allInstances = new ArrayList<FacilityManager>();
	}
	
	public FacilityManager() {
		
		synchronized (allInstances) {
			allInstances.add(this);
		}
	}

	
	public static ArrayList<? extends FacilityManager> getAll() {
		synchronized (allInstances) {
			return allInstances;
		}
	}

	@Override
	public void kill() {
		super.kill();
		synchronized (allInstances) {
			allInstances.remove(this);
		}
	}
	
	@Override
	public void validate() {
		super.validate();
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();
		
	}
 	
	public void printManagerReportHeader(){	}
	
	public Facility getFacility() {
		return facility;
	}

	
	public void setFacility(Facility facility) {
		this.facility = facility;
	}
}
