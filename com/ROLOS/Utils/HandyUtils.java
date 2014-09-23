package com.ROLOS.Utils;

import java.text.DecimalFormat;
import java.util.ArrayList;

import com.sandwell.JavaSimulation.Entity;

public class HandyUtils {
	private static DecimalFormat formatter;
	
	static{
		formatter = new DecimalFormat( "##0.00" );
	}
	
	/**
	 * gets an array list of numbers or entities and returns the string representation (entity names are used for entities)
	 */
	public static String arraylistToString(ArrayList<?> arrayList){
		if (arrayList.isEmpty())
			return "";
		String returnString = "{";
		int i = 0;
		for (; i<arrayList.size()-1;i++){
			if (arrayList.get(i) instanceof Number)
				returnString = returnString+arrayList.get(i).toString()+",";
			else if (arrayList.get(i) instanceof Entity)
				returnString = returnString+((Entity) arrayList.get(i)).getName()+",";
			else if (arrayList.get(i) instanceof String)
				returnString = returnString+arrayList.get(i)+",";

		}
		
		if (arrayList.get(i) instanceof Number)
			returnString = returnString+arrayList.get(i).toString()+"}";
		else if (arrayList.get(i) instanceof Entity)
			returnString = returnString+((Entity) arrayList.get(i)).getName()+"}";
		else if (arrayList.get(i) instanceof String)
			returnString = returnString+arrayList.get(i)+"}";
		return returnString;
	}
	
	public static String arraylistToString(ArrayList<?> arrayList, int numberOfDecimals){
		StringBuilder pattern = new StringBuilder("##0");
		if( numberOfDecimals > 0 ) {
			pattern.append(".");
			for( int i = 0; i < numberOfDecimals; i++ ) {
				pattern.append("0");
			}
		}
		formatter.applyPattern(pattern.toString());
		
		if (arrayList.isEmpty())
			return "";
		String returnString = "{";
		int i = 0;
		for (; i<arrayList.size()-1;i++){
			if (arrayList.get(i) instanceof Number)
				returnString = returnString+formatter.format(arrayList.get(i))+", ";
		}
		if (arrayList.get(i) instanceof Number)
			returnString = returnString+formatter.format(arrayList.get(i))+"}";
		
		return returnString;
	}
	
	/**
	 * This method assumes that the amount of the added proportion is already added to whole
	 * @return
	 */
	public static double linearAverage(double addedPortion, double addedPropopertyValue, double blendPropertyValue){
		return addedPortion*addedPropopertyValue + (1-addedPortion)*blendPropertyValue;
	}

}
