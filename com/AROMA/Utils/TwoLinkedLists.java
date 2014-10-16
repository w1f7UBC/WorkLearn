package com.AROMA.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import com.AROMA.AROMAEntity;
import com.AROMA.JavaSimulation.Tester_Rolos;
import com.sandwell.JavaSimulation.Tester;
import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

/**
 * Wrapper for linking two arraylists of type T and an arraylist of type double.
 * add and remove are done on all lists at the same time, keeps order based on the defined comparator.
 * first list is kept descending (descending)
 * Accessor should obtain lock to avoid concurrent adding or removing of value/entity.
 * will remove entity from list if value is 0. implementor should check for consistency.
 * @param <T> Leading list type (first arraylist type)
 */
public class TwoLinkedLists<T extends AROMAEntity> {
	private Object listLock;
	private ArrayList<T> firstList;
	private ArrayList<ArrayList<Double>> valueList;
	Comparator<T> comparator;
	private int[] zeroCheckIndices;
	
	/**
	 * 
	 * @param valueLists number of value lists associated with the entity list
	 * @param comparator
	 * @param zeroCheckIndices index of value lists that will remove the entity if their value equaled zero.
	 */
	public TwoLinkedLists(int valueLists, Comparator<T> comparator, int ...zeroCheckIndices) {
		listLock = new Object();
		firstList = new ArrayList<>(1);
		valueList = new ArrayList<ArrayList<Double>>(1);
		for(int i = 0; i<valueLists; i++)
			valueList.add(new ArrayList<Double>(1));
		
		this.comparator = comparator;
		this.zeroCheckIndices = zeroCheckIndices;
	}
	
	public ArrayList<T> getEntityList(){
		return firstList;
	}
	
	public ArrayList<ArrayList<Double>> getValueLists(){
		return valueList;
	}
	
	/**
	 * Value index starts from 0. i.e. 4th value list's index is 3!
	 * @return value in the value list corresponding to the entity, or 0 if entity doesn't exist in the list;
	 */
	public double getValueFor(T entity, int valueListIndex){
		return firstList.contains(entity) ? valueList.get(valueListIndex).get(firstList.indexOf(entity)) : 0;
	}
	
	public void clear(){
		firstList.clear();
		for(ArrayList<Double> each: valueList)
			each.clear();
	}
	
	public boolean contains(T entity){
		return firstList.contains(entity)? true : false;
	}
	
	public int indexOf(T entity){
		return firstList.contains(entity) ? firstList.indexOf(entity) : -1;
	}
	
	public int size(){
		return firstList.isEmpty()? 0 : firstList.size();
	}
	

	public boolean isEmpty() {
		return firstList.isEmpty() ? true : false;
	}
	
	/**
	 * adds the amount of value for entity to the value list specified by the index. <br> Adds zero
	 * to other value lists. Maximum allowable value us unchecked. 
	 * caller should check for consistency.
	 */
	public void add(T entity, int valueListIndex, double value){
		synchronized (listLock) {
			int index;
			if (firstList.contains(entity)){
				index = firstList.indexOf(entity);
				double val = valueList.get(valueListIndex).get(index);
			
				valueList.get(valueListIndex).set(index, value+val);
			}
			else{
				index = 0;
				for (T each: firstList){
					if(comparator.compare(entity, each) >= 0)
						break;
					else
						index++;
				}
				firstList.add(index, entity);
				for(int i = 0; i <valueList.size(); i++)
					if(i == valueListIndex)
						valueList.get(i).add(index, value);
					else
						valueList.get(i).add(index, 0.0d);
			}
		}
	}
	
	public void remove(T entity){
		synchronized (listLock) {
			if(firstList.contains(entity)){
				for (int i = 0; i<valueList.size(); i++)
					valueList.get(i).remove(firstList.indexOf(entity));
			}
		}
	}
	
	/**
	 * will remove amount of value from entity. if entity is not found in the list, it won't do anything.
	 * will remove entity if amount is 0. caller should check for consistency.
	 */
	public void remove(T entity, int valueListIndex, double value){
		synchronized (listLock) {
			int index;
			if (firstList.contains(entity)){
				index = firstList.indexOf(entity);
				double val = valueList.get(valueListIndex).get(index);
			
				valueList.get(valueListIndex).set(index, val-value);
				for(int each: zeroCheckIndices){
					if(each == valueListIndex && Tester.equalCheckTolerance(valueList.get(valueListIndex).get(index), 0.0d)){
						firstList.remove(index);
						for(int i = 0; i <valueList.size(); i++)
							valueList.get(i).remove(index);
						return;
					}			
				}
			}
			else
				return;
		}
	}
	
	public void set(T entity, int valueListIndex, double value){
		synchronized (listLock) {
			if(firstList.contains(entity)){
				valueList.get(valueListIndex).set(firstList.indexOf(entity), value);
			} else{
				int index = 0;
				for (T each: firstList){
					if(comparator.compare(entity, each) >= 0)
						break;
					else
						index++;
				}
				firstList.add(index, entity);
				for(int i = 0; i <valueList.size(); i++)
					if(i == valueListIndex)
						valueList.get(i).add(index, value);
					else
						valueList.get(i).add(index, 0.0d);
			}
		}
	}

	/**
	 * sets minimum of the current value in the list and the passed value or adds the value if doesn't exist
	 */
	public void setMin(T entity, int valueListIndex, double value){
		synchronized (listLock) {
			if(firstList.contains(entity)){
				double tempValue = valueList.get(valueListIndex).get(firstList.indexOf(entity));
				valueList.get(valueListIndex).set(firstList.indexOf(entity), Tester.min(tempValue,value));
			} else{
				int index = 0;
				for (T each: firstList){
					if(comparator.compare(entity, each) >= 0)
						break;
					else
						index++;
				}
				firstList.add(index, entity);
				for(int i = 0; i <valueList.size(); i++)
					if(i == valueListIndex)
						valueList.get(i).add(index, value);
					else
						valueList.get(i).add(index, 0.0d);
				}
		}
	}
	
	/**
	 * sets maximum of the current value in the list and the passed value or adds the value if doesn't exist
	 */
	public void setMax(T entity, int valueListIndex, double value){
		synchronized (listLock) {
			if(firstList.contains(entity)){
				double tempValue = valueList.get(valueListIndex).get(firstList.indexOf(entity));
				valueList.get(valueListIndex).set(firstList.indexOf(entity), Tester_Rolos.max(tempValue,value));
			} else{
				int index = 0;
				for (T each: firstList){
					if(comparator.compare(entity, each) >= 0)
						break;
					else
						index++;
				}
				firstList.add(index, entity);
				for(int i = 0; i <valueList.size(); i++)
					if(i == valueListIndex)
						valueList.get(i).add(index, value);
					else
						valueList.get(i).add(index, 0.0d);
				}
		}
	}
}
