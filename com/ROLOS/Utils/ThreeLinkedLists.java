package com.ROLOS.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.ROLOS.JavaSimulation.Tester_Rolos;
import com.sandwell.JavaSimulation.Tester;

/**
 * Wrapper for linking two arraylists of type T and T and a series of arraylists of type double.
 * add and remove are done on all lists or double lists specified at the same time, keeps order based on the defined comparator.
 * first and second lists are kept descending (descending). first list is checked first. multiple instances of 
 * an object in the first list is allowed if the passed object for the second list is different.
 * Accessor should obtain lock to avoid concurrent adding or removing of value/entity.
 * will remove entities from lists if value is 0. implementor should check for consistency.
 * @param <T> Leading list type (first arraylist type)
 */
public class ThreeLinkedLists<T1,T2> {
	private Object listLock;
	private ArrayList<T1> firstList;
	private ArrayList<T2> secondList;
	private ArrayList<ArrayList<Double>> valueList;
	Comparator<T1> firstComparator;
	Comparator<T2> secondComparator;
	private int[] zeroCheckIndices;

	/**
	 * @param valueLists number of value lists to generate
	 * @param firstComparator comparator for the first list
	 * @param secondComparator comparator for the second list
	 */
	public ThreeLinkedLists(int valueLists, Comparator<T1> firstComparator,Comparator<T2> secondComparator,int ...zeroCheckIndices) {
		listLock = new Object();
		firstList = new ArrayList<>(1);
		secondList = new ArrayList<>(1);
		valueList = new ArrayList<ArrayList<Double>>(1);
		for(int i = 0; i<valueLists; i++)
			valueList.add(new ArrayList<Double>(1));
		
		this.firstComparator = firstComparator;
		this.secondComparator = secondComparator;
		this.zeroCheckIndices = zeroCheckIndices;

	}
	
	public ArrayList<T1> getFirstEntityList(){
		return firstList;
	}
	
	public ArrayList<T2> getSecondEntityList(){
		return secondList;
	}
	
	public ArrayList<ArrayList<Double>> getAllValueLists(){
		return valueList;
	}
	
	public ArrayList<Double> getValueList(int valueListIndex){
		return valueList.get(valueListIndex);
	}
	
	public ArrayList<Double> getValueListFor(T1 firstEntity, int valueListIndex){
		synchronized (listLock) {
			ArrayList<Double> tempList = new ArrayList<>(1);
			int index = -1;
			int fromIndex = 0;
			int toIndex = firstList.size();
			while(fromIndex < toIndex){
				index = firstList.subList(fromIndex, toIndex).indexOf(firstEntity);
				if(index < 0)
					break;
				else{
					tempList.add(valueList.get(valueListIndex).get(firstList.indexOf(firstEntity)));
					fromIndex += index + 1;					
				}
			}
			return tempList;
		}
	}
	
	/**
	 * @param valueListInd index of sought value list- to get the value in the first list pass 0 and so on.
	 * @return value in the value list corresponding to the entity, or 0 if entity doesn't exist in the list;
	 */
	public double getValueFor(T1 firstEntity, T2 secondEntity, int valueListIndex){
		synchronized (listLock) {
			int index = -1;
			int fromIndex = 0;
			int toIndex = firstList.size();
			while(fromIndex < toIndex){
				index = firstList.subList(fromIndex, toIndex).indexOf(firstEntity);
				if(index < 0)
					return 0;
				else{
					if (secondList.get(fromIndex + index).equals(secondEntity))
						return valueList.get(valueListIndex).get(fromIndex + index);
					else
						fromIndex += index + 1;					
				}
			}
			return 0;
		}
	}
		
	public void clear(){
		firstList.clear();
		secondList.clear();
		for(ArrayList<Double> each: valueList)
			each.clear();
	}
	
	public boolean contains (T1 firstEntity){
		return firstList.contains(firstEntity)? true : false;
	}
	
	public boolean contains(T1 firstEntity, T2 secondEntity){		
		synchronized (listLock) {
			int index = -1;
			int fromIndex = 0;
			int toIndex = firstList.size();
			while(fromIndex < toIndex){
				index = firstList.subList(fromIndex, toIndex).indexOf(firstEntity);
				if(index < 0)
					return false;
				else{
					if (secondList.get(fromIndex + index).equals(secondEntity))
						return true;
					else
						fromIndex += index + 1;					
				}
			}
			return false;
		}
	}
	
	/**
	 * @return position of first occurance of firstEntity or null if not foud
	 */
	public int indexOf(T1 firstEntity){
		synchronized (listLock) {
			return firstList.contains(firstEntity)? firstList.indexOf(firstEntity) : null;
		}
	}
	
	public int indexOf(T1 firstEntity, T2 secondEntity){
		synchronized (listLock) {
			int index = -1;
			int fromIndex = 0;
			int toIndex = firstList.size();
			while(fromIndex < toIndex){
				index = firstList.subList(fromIndex, toIndex).indexOf(firstEntity);
				if(index < 0)
					return -1;
				else{
					if (secondList.get(fromIndex + index).equals(secondEntity))
						return fromIndex + index;
					else
						fromIndex += index + 1;					
				}
			}
			return -1;
		}
	}
	
	public int size(){
		return firstList.isEmpty()? 0 : firstList.size();
	}
	
	/**
	 * finds appropriate position of entity in the first list based on firstComparator
	 */
	private int findPosition(List<T1> list, T1 entity){
		int index = 0;
		for (T1 each: list){
			if(firstComparator.compare(entity, each) >= 0)
				break;
			else
				index++;
		}
		return index;
	}
	
	/**
	 * adds the amount of value for firstentity and secondentity. If a new entity is inserted, zero is added for all other 
	 * value lists. maximum allowable value is unchecked. 
	 * caller should check for consistency.
	 */
	public void add(T1 firstEntity, T2 secondEntity, int valueListIndex, double value){
		synchronized (listLock) {
			int index = -1;
			int fromIndex = 0;
			int toIndex = firstList.size();
			while(fromIndex < toIndex){
				index = firstList.subList(fromIndex, toIndex).indexOf(firstEntity);
				if(index < 0){
					index = findPosition(firstList.subList(fromIndex, toIndex), firstEntity);
					firstList.add(index,firstEntity);
					secondList.add(index, secondEntity);
					for(int i = 0; i <valueList.size(); i++)
						if(i == valueListIndex)
							valueList.get(i).add(index, value);
						else
							valueList.get(i).add(index, 0.0d);
					return;
				}
				else{
					if (secondList.get(fromIndex + index).equals(secondEntity)){
						valueList.get(valueListIndex).set(fromIndex + index,value+valueList.get(valueListIndex).get(fromIndex+index));
						return;
					}
					else{
						if(secondComparator.compare(secondEntity,secondList.get(fromIndex + index)) >= 0){
							firstList.add(fromIndex + index,firstEntity);
							secondList.add(fromIndex + index, secondEntity);
							for(int i = 0; i <valueList.size(); i++)
								if(i == valueListIndex)
									valueList.get(i).add(fromIndex + index, value);
								else
									valueList.get(i).add(fromIndex + index, 0.0d);
							return;
						}							
						else
							fromIndex += index + 1;					
					}
				}
			}
			firstList.add(fromIndex,firstEntity);
			secondList.add(fromIndex, secondEntity);
			for(int i = 0; i <valueList.size(); i++)
				if(i == valueListIndex)
					valueList.get(i).add(fromIndex, value);
				else
					valueList.get(i).add(fromIndex, 0.0d);
		}
	}
		
	/**
	 * sets the amount of value for firstentity and secondentity. maximum allowable value is unchecked. 
	 * caller should check for consistency. <br> 0 is allowable; whereas in remove, if value became 0, the corresponding entities would get removed!
	 */
	public void set(T1 firstEntity, T2 secondEntity, int valueListIndex, double value){
		synchronized (listLock) {
			int index = -1;
			int fromIndex = 0;
			int toIndex = firstList.size();
			while(fromIndex < toIndex){
				index = firstList.subList(fromIndex, toIndex).indexOf(firstEntity);
				if(index < 0){
					index = findPosition(firstList.subList(fromIndex, toIndex), firstEntity);
					firstList.add(index,firstEntity);
					secondList.add(index, secondEntity);
					for(int i = 0; i <valueList.size(); i++)
						if(i == valueListIndex)
							valueList.get(i).add(index, value);
						else
							valueList.get(i).add(index, 0.0d);
					return;
				}
				else{
					if (secondList.get(fromIndex + index).equals(secondEntity)){
						valueList.get(valueListIndex).set(fromIndex + index,value);
						return;
					}
					else{
						if(secondComparator.compare(secondEntity,secondList.get(fromIndex + index)) >= 0){
							firstList.add(fromIndex + index,firstEntity);
							secondList.add(fromIndex + index, secondEntity);
							for(int i = 0; i <valueList.size(); i++)
								if(i == valueListIndex)
									valueList.get(i).add(fromIndex + index, value);
								else
									valueList.get(i).add(fromIndex + index, 0.0d);
							return;
						}							
						else
							fromIndex += index + 1;					
					}
				}
			}
			firstList.add(fromIndex,firstEntity);
			secondList.add(fromIndex, secondEntity);
			for(int i = 0; i <valueList.size(); i++)
				if(i == valueListIndex)
					valueList.get(i).add(fromIndex, value);
				else
					valueList.get(i).add(fromIndex, 0.0d);
		}
	}
	
	/**
	 * sets the amount of value for firstentity and secondentity to the min of the current value and the passed value or adds the value if new entry. 
	 * caller should check for consistency. <br> 0 is allowable; whereas in remove, if value became 0, the corresponding entities would get removed!
	 */
	public void setMin(T1 firstEntity, T2 secondEntity, int valueListIndex, double value){
		synchronized (listLock) {
			int index = -1;
			int fromIndex = 0;
			int toIndex = firstList.size();
			while(fromIndex < toIndex){
				index = firstList.subList(fromIndex, toIndex).indexOf(firstEntity);
				if(index < 0){
					index = findPosition(firstList.subList(fromIndex, toIndex), firstEntity);
					firstList.add(index,firstEntity);
					secondList.add(index, secondEntity);
					for(int i = 0; i <valueList.size(); i++)
						if(i == valueListIndex)
							valueList.get(i).add(index, value);
						else
							valueList.get(i).add(index, 0.0d);
					return;
				}
				else{
					if (secondList.get(fromIndex + index).equals(secondEntity)){
						double tempValue = valueList.get(valueListIndex).get(fromIndex + index);
						valueList.get(valueListIndex).set(fromIndex + index,Tester.min(tempValue,value));
						return;
					}
					else{
						if(secondComparator.compare(secondEntity,secondList.get(fromIndex + index)) >= 0){
							firstList.add(fromIndex + index,firstEntity);
							secondList.add(fromIndex + index, secondEntity);
							for(int i = 0; i <valueList.size(); i++)
								if(i == valueListIndex)
									valueList.get(i).add(fromIndex + index, value);
								else
									valueList.get(i).add(fromIndex + index, 0.0d);
							return;
						}							
						else
							fromIndex += index + 1;					
					}
				}
			}
			firstList.add(fromIndex,firstEntity);
			secondList.add(fromIndex, secondEntity);
			for(int i = 0; i <valueList.size(); i++)
				if(i == valueListIndex)
					valueList.get(i).add(fromIndex, value);
				else
					valueList.get(i).add(fromIndex, 0.0d);
		}
	}
	
	/**
	 * sets the amount of value for firstentity and secondentity to the max of the current value and the passed value or adds the value if new entry. 
	 * caller should check for consistency. <br> 0 is allowable; whereas in remove, if value became 0, the corresponding entities would get removed!
	 */
	public void setMax(T1 firstEntity, T2 secondEntity, int valueListIndex, double value){
		synchronized (listLock) {
			int index = -1;
			int fromIndex = 0;
			int toIndex = firstList.size();
			while(fromIndex < toIndex){
				index = firstList.subList(fromIndex, toIndex).indexOf(firstEntity);
				if(index < 0){
					index = findPosition(firstList.subList(fromIndex, toIndex), firstEntity);
					firstList.add(index,firstEntity);
					secondList.add(index, secondEntity);
					for(int i = 0; i <valueList.size(); i++)
						if(i == valueListIndex)
							valueList.get(i).add(index, value);
						else
							valueList.get(i).add(index, 0.0d);
					return;
				}
				else{
					if (secondList.get(fromIndex + index).equals(secondEntity)){
						double tempValue = valueList.get(valueListIndex).get(fromIndex + index);
						valueList.get(valueListIndex).set(fromIndex + index,Tester_Rolos.max(tempValue,value));
						return;
					}
					else{
						if(secondComparator.compare(secondEntity,secondList.get(fromIndex + index)) >= 0){
							firstList.add(fromIndex + index,firstEntity);
							secondList.add(fromIndex + index, secondEntity);
							for(int i = 0; i <valueList.size(); i++)
								if(i == valueListIndex)
									valueList.get(i).add(fromIndex + index, value);
								else
									valueList.get(i).add(fromIndex + index, 0.0d);
							return;
						}							
						else
							fromIndex += index + 1;					
					}
				}
			}
			firstList.add(fromIndex,firstEntity);
			secondList.add(fromIndex, secondEntity);
			for(int i = 0; i <valueList.size(); i++)
				if(i == valueListIndex)
					valueList.get(i).add(fromIndex, value);
				else
					valueList.get(i).add(fromIndex, 0.0d);
		}
	}
	
	/**
	 * will remove all instances of first and associated second entities and value from lists. if entity is not found in the list, it won't do anything.
	 */
	public void remove(T1 firstEntity){
		synchronized (listLock) {
			int index;
			while(!firstList.isEmpty()){
				index = firstList.indexOf(firstEntity);
				if(index < 0)
					return;
				else{
					firstList.remove(index);
					secondList.remove(index);
					for(int i= 0; i<valueList.size();i++)
						valueList.get(i).remove(index);
					}				
			}
			return;
		}
	}
	
	/**
	 * will remove first and second entities and the value lists. if entity is not found in the list, it won't do anything.
	 */
	public void remove(T1 firstEntity, T2 secondEntity){
		synchronized (listLock) {
			int index = -1;
			int fromIndex = 0;
			int toIndex = firstList.size();
			while(fromIndex < toIndex){
				index = firstList.subList(fromIndex, toIndex).indexOf(firstEntity);
				if(index < 0)
					return;
				else{
					if (secondList.get(fromIndex + index).equals(secondEntity)){
						firstList.remove(fromIndex+index);
						secondList.remove(fromIndex+index);
						for(int i = 0;i<valueList.size();i++)
							valueList.get(i).remove(fromIndex+index);
						return;
					}
					else
						fromIndex += index + 1;					
				}
			}
			return;
		}
	}
	
	/**
	 * will remove amount of value from entity. if entity is not found in the list, it won't do anything.
	 * will remove entity if amount is 0 caller should check for consistency.
	 */
	public void remove(T1 firstEntity, T2 secondEntity, int valueListIndex, double value){
		synchronized (listLock) {
			int index = -1;
			int fromIndex = 0;
			int toIndex = firstList.size();
			while(fromIndex < toIndex){
				index = firstList.subList(fromIndex, toIndex).indexOf(firstEntity);
				if(index < 0)
					return;
				else{
					if (secondList.get(fromIndex + index).equals(secondEntity)){
						double val = valueList.get(valueListIndex).get(fromIndex+index)-value;
						for(int each: zeroCheckIndices){
							if (each == valueListIndex && Tester.equalCheckTimeStep(val, 0.0d)){
								firstList.remove(fromIndex+index);
								secondList.remove(fromIndex+index);
								for(int i = 0;i<valueList.size();i++)
									valueList.get(i).remove(fromIndex+index);
								return;
							}
						}
						valueList.get(valueListIndex).set(fromIndex+index, val);
						return;
					} else
						fromIndex += index + 1;					
				}
			}
			return;
		}
	}

	public boolean isEmpty() {
		return firstList.isEmpty() ? true : false;
	}

}

