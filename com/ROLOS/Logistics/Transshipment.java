package com.ROLOS.Logistics;

/**
 * Transshipment facility is a facility that is used as intermitent point for transportation.
 * transshipment facilities handle transportation from transshipment to destination, hence should define transporters list.
 * @author Saeed Ghafghazi (email: saeedghaf@gmail.com) - Jul 26, 2014 
 * 
 * @modified_by
 */
public class Transshipment extends Facility {

	public Transshipment() {
	}
	
	/**
	 * cost of transporting via this transshipment per unit of material. set to operating cost as long as operating cost represents cost per unit of material.
	 */
	@Override
	public double getTravelCost(MovingEntity movingEntity) {
		return this.getOperatingCost();
	}

}
