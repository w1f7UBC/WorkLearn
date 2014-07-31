package com.ROLOS.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import com.sandwell.JavaSimulation.ErrorException;

/**
 * Data structure for storing a list of arraylists of type T2 associated with type T1. Duplicates are permitted.
 */
public class HashMapList<T1, T2> {

	private HashMap<T1, ArrayList<T2>> hashList; 
	
	public HashMapList() {
		hashList = new HashMap<>(5);
	
	}
	
	public HashMapList(int initialCapacity) {
		hashList = new HashMap<>(initialCapacity);

	}
	
	/**
	 * will add element to the arraylist associated with name. 
	 * <br> <b>WARNING: </b> wont check for duplicate elements. 
	 */
	public <V extends T2> void add(T1 hashIdentifier,V element){
		if (!hashList.containsKey(hashIdentifier)){
			ArrayList<T2> temp = new ArrayList<>(1);
			temp.add(element);
			hashList.put(hashIdentifier, temp);
		} 
		else{
			hashList.get(hashIdentifier).add(element);
		}
	}
	
	/**
	 * will append passed arraylist to the list associated with name or create a list with the passed elements if name doesn't exist.
	 * <br> <b>WARNING: </b> wont check for duplicate elements.
	 */
	public <V extends T2> void add(T1 hashIdentifier, ArrayList<T2> list){
		if (!hashList.containsKey(hashIdentifier)){
			hashList.put(hashIdentifier,list);
		}
		else{
			hashList.get(hashIdentifier).addAll(list);
		}
	}
	
	/**
	 * will add element to the arraylist associated with name. 
	 * <br> <b>WARNING: </b> wont check for duplicate elements. 
	 */
	public <V extends T2> void add(Class klass,V element){
		if (!hashList.containsKey(klass.getSimpleName())){
			ArrayList<T2> temp = new ArrayList<>(1);
			temp.add(element);
			hashList.put((T1) klass.getSimpleName(), temp);
		} 
		else{
			hashList.get(klass).add(element);
		}
	}
	
	/**
	 * will append passed arraylist to the list associated with name or create a list with the passed elements if name doesn't exist.
	 * <br> <b>WARNING: </b> wont check for duplicate elements.
	 */
	public <V extends T2> void add(Class klass, ArrayList<T2> list){
		if (!hashList.containsKey(klass.getSimpleName())){
			hashList.put((T1) klass.getSimpleName(),list);
		}
		else{
			hashList.get(klass).addAll(list);
		}
	}
	
	public void clear(){
		hashList.clear();
	}
	
	public boolean contains(T1 hashIdentifier){
		return hashList.containsKey(hashIdentifier)? true : false;
	}
	
	/**
	 * @return arraylist associated with the hashIdentifier or null if not found
	 */
	public ArrayList<T2> get(T1 hashIdentifier){
		return hashList.get(hashIdentifier) == null ? new ArrayList<T2>(0) : hashList.get(hashIdentifier);
	}
	
	/**
	 * @param klass of entities to cast to 
	 * @return the casted array list for the passed klass if arraylists are stored through their simplenames as key
	 */
	public <U extends T2> ArrayList<U> get(Class<U> klass){
		ArrayList<U> tempArrayList = new ArrayList<>(1);

		for (T2 each: get((T1) klass.getSimpleName())){
			try {
				tempArrayList.add(klass.cast(each));
			} catch (ClassCastException e) {
				throw new ErrorException("Found %s in the list of %s when casting hash map list for %s class!", each.getClass().getSimpleName(),this.toString(),klass.getSimpleName());
			}
		}
		return tempArrayList;
	}
	
	public ArrayList<T1> getKeys(){
		ArrayList<T1> tempList = new ArrayList<>(hashList.keySet());
		
		return tempList;
	}
	
	/**
	 * TODO get rid of this and make processing route better
	 *@return the array list of entitylists contained in the hashmap list
	 */
	public ArrayList<ArrayList<T2>> getValues(){
		ArrayList<ArrayList<T2>> tempList = new ArrayList<>(1);
		for(T1 each: hashList.keySet())
			tempList.add(hashList.get(each));
		return tempList;
	}
	
	
	public void remove(T1 hashIdentifier){
		
		hashList.remove(hashIdentifier);
		
	}
	
	/**
	 * @return true if hashIdentifier doesn't exist or associated arraylist with hashIdentifier is empty.
	 */
	public boolean isEmpty(T1 hashIdentifier){
		return (!hashList.containsKey(hashIdentifier) || hashList.get(hashIdentifier).isEmpty()) ? true : false; 		
	}
	
	public void remove(T1 hashIdentifier, T2 element){
		if (hashList.containsKey(hashIdentifier))
			hashList.get(hashIdentifier).remove(element);
		else
			return;
	}
	
	public void remove(T1 hashIdentifier, ArrayList<T2> list){
		if (hashList.containsKey(hashIdentifier))
			hashList.get(hashIdentifier).removeAll(list);
		else
			return;
	}

}
