package com.AROMA.Utils;

import com.jaamsim.math.Vec3d;


public class MathUtilities {

	/**
	 * @param originPoint the point from which the nearest refPoint should be returned
	 * @param refPoint series of 3d points whose closest distance to originPoint is to be found
	 * @return the closest refPoint to the originPoint
	 */
	public static Vec3d nearestPoint(Vec3d originPoint, Vec3d ... refPoints){
		double dist = Double.POSITIVE_INFINITY;
		Vec3d returnVec = new Vec3d();
		Vec3d vec = new Vec3d();
		for (Vec3d each: refPoints){
			vec.sub3(originPoint, each);
			if(vec.mag3() < dist){
				dist = vec.mag3();
				returnVec.set3(each);
			}
		}
		return returnVec;
	}
	
	public static double distance(Vec3d firstPoint, Vec3d secondPoint){
		Vec3d tempVec = new Vec3d(firstPoint); 
		tempVec.sub3(secondPoint);
		return tempVec.mag3();
	}
}
