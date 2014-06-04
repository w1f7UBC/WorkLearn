package com.ROLOS.Utils;

import java.util.ArrayList;
import com.sandwell.JavaSimulation.ErrorException;

/**
 * Data structure for storing a list of arraylists of type T2 associated with type T1. Duplicates are permitted.
 */
public class HashMapList<T1, T2> {

	private ArrayList<T1> hashList;
	private ArrayList<ArrayList<T2>> entityList;
	
	public HashMapList() {
		hashList = new ArrayList<>(5);
		entityList = new ArrayList<>(5);
	}
	
	public HashMapList(int initialCapacity) {
		hashList = new ArrayList<>(initialCapacity);
		entityList = new ArrayList<>();
	}
	
	/**
	 * will add element to the arraylist associated with name. 
	 */
	public <V extends T2> void add(T1 hashIdentifier,V element){
		if (!hashList.contains(hashIdentifier)){
			ArrayList<T2> temp = new ArrayList<>(1);
			temp.add(element);
			hashList.add(hashIdentifier);
			entityList.add(temp);
		} 
		else{
			int index = hashList.indexOf(hashIdentifier);
			entityList.get(index).add(element);
		}
	}
	
	/**
	 * will append passed arraylist to the list associated with name or create a list with the passed elements if name doesn't exist.
	 */
	public <V extends T2> void add(T1 hashIdentifier, ArrayList<T2> list){
		if (!hashList.contains(hashIdentifier)){
			hashList.add(hashIdentifier);
			entityList.add(list);
		}
		else{
			int index = hashList.indexOf(hashIdentifier);
			entityList.get(index).addAll(list);
		}
	}
	
	public void clear(){
		hashList.clear();
	}
	
	public boolean contains(T1 hashIdentifier){
		return hashList.contains(hashIdentifier)? true : false;
	}
	
	/**
	 * @return arraylist associated with the hashIdentifier or null if not found
	 */
	public ArrayList<T2> get(T1 hashIdentifier){
		return hashList.contains(hashIdentifier) ? entityList.get(hashList.indexOf(hashIdentifier)) : new ArrayList<T2>(1);
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
		return new ArrayList<T1>(hashList);
	}
	
	/**
	 *@return the array list of entitylists contained in the hashmap list
	 */
	public ArrayList<ArrayList<T2>> getValues(){
		return entityList;
	}
	
	public void remove(T1 hashIdentifier){
		int index = hashList.indexOf(hashIdentifier);
		if (index < 0)
			return;
		entityList.remove(index);
		hashList.remove(index);
		
	}
	
	/**
	 * @return true if hashIdentifier doesn't exist or associated arraylist with hashIdentifier is empty.
	 */
	public boolean isEmpty(T1 hashIdentifier){
		if(!hashList.contains(hashIdentifier))
			return true;
		else if (entityList.get(hashList.indexOf(hashIdentifier)).isEmpty())
				return true;	
		else return false;		
	}
	
	public void remove(T1 hashIdentifier, T2 element){
		if (hashList.contains(hashIdentifier))
			entityList.get(hashList.indexOf(hashIdentifier)).remove(element);
		else
			return;
	}
	
	public void remove(T1 hashIdentifier, ArrayList<T2> list){
		if (hashList.contains(hashIdentifier))
			entityList.get(hashList.indexOf(hashIdentifier)).removeAll(list);
		else
			return;
	}

}
