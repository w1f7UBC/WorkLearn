package com.ROLOS.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.ROLOS.ROLOSEntity;
/**
	 * arraylist implementation for queue. keeps order ascending or descending depending on construction type.
	 */
public class PriorityQueue<T extends ROLOSEntity> implements Collection<T>{

	private ArrayList<T> queueList;
	Comparator<T> comparator;
	
	public PriorityQueue(Comparator<T> comparator) {
		queueList = new ArrayList<>(1);
		this.comparator = comparator;
	}
		
	public boolean isEmpty(){
		return queueList.isEmpty();
	}
	
	@Override
	public boolean add(T entity){
		queueList.add(entity);
		Collections.sort(queueList, comparator);
		return true;
	}
	
	public void remove(T entity){
		queueList.remove(entity);
	}
	
	public T get(int index){
		return queueList.isEmpty()? null : queueList.get(index);
	}
	
	public boolean contains(T entity){
		return queueList.contains(entity);
	}
	
	public T peek(){
		return queueList.isEmpty()? null : queueList.get(0);
	}
	
	public T poll(){
		T entity = null;
		if(!queueList.isEmpty())
			entity = queueList.get(0);
		queueList.remove(0);
		return entity == null ? null : entity;
	}
	
	public int size(){
		return queueList.size();
	}

	public ArrayList<T> getList() {
		return queueList;
	}
	
	/**
	 * sorts the list based on the defined comparator again
	 */
	public void update(){
		Collections.sort(queueList, comparator);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		for(T each: c)
			queueList.add(each);
		Collections.sort(queueList, comparator);
		return true;
	}

	@Override
	public void clear() {
		queueList.clear();
	}

	@Override
	public boolean contains(Object o) {
		return queueList.contains(o) ? true: false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for(Object each: c)
			if(!queueList.contains(each))
				return false;
		return true;
	}

	@Override
	public Iterator<T> iterator() {
		return queueList.iterator();
	}

	@Override
	public boolean remove(Object o) {
		if(queueList.contains(o)){
			queueList.remove(o);
			return true;
		}
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		for(Object each: c){
			queueList.remove(each);
		}
		return true;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		// TODO Auto-generated method stub
		return null;
	}

}
