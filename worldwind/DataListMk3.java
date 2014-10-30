package worldwind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class DataListMk3 {
	ArrayList<Object[]> key;
	ArrayList<Object[]> value;
	
	public DataListMk3(){
		key=new ArrayList<Object[]>();
		value=new ArrayList<Object[]>();
	}
	
	public void add(Object entity, Object movingEntity, Object fromEntity, Object fromMovingEntity){
		Object[] score = {fromEntity, fromMovingEntity};
		Object[] object = {entity, movingEntity};
			key.add(object);
			value.add(score);
	}
	
	public void removeValue(Object entity, Object movingEntity){
		Object[] object = {entity, movingEntity};
		for (int x=0; x<value.size(); x++){
			if (Arrays.equals(value.get(x), object)){
				key.remove(x);
				value.remove(x);
			}
		}
	}
	
	public Boolean containsValue(Object entity, Object movingEntity){
		Object[] object = {entity, movingEntity};
		for (int x=0; x<value.size(); x++){
			if (Arrays.equals(value.get(x), object)){
				return true;
			}
		}
		return false;
	}
	
	public Object[] getValue(Object gScore, Object fScore){
		Object[] score = {gScore, fScore};
		for (int x=0; x<key.size(); x++){
			if (Arrays.equals(key.get(x), score)){
				return value.get(x);
			}
		}
		return null;
	}
	
	public Object[] getKey(Object entity, Object movingEntity){
		Object[] object = {entity, movingEntity};
		for (int x=0; x<value.size(); x++){
			if (Arrays.equals(value.get(x), object)){
				return key.get(x);
			}
		}
		return null;
	}
	
	public boolean containsKey(Object entity, Object movingEntity){
		Object[] object = {entity, movingEntity};
		for (int x=0; x<key.size(); x++){
			if (Arrays.equals(key.get(x), object)){
				return true;
			}
		}
		return false;
	}
	
	public Object[] getScoresIndex(int index){
		return key.get(index);
	}
	
	public Object[] getObjectsIndex(int index){
		return value.get(index);
	}
	
	public boolean isEmpty(){
		return key.isEmpty();
	}
	
	
	public void printMap(){
		for (int x=0; x<key.size(); x++){
			System.out.println(key.get(x)[0] + " " + key.get(x)[1] + " " + value.get(x)[0] + " " + value.get(x)[1]);
		}
	}
	
	public static void main(String args[]){
		System.out.println("TEST");
		DataListMk2 list= new DataListMk2();
		double a = 1;
		double b = 2;
		double c = 3;
		double d = 4;
		double e = 5;
		
		double aaa = 1;
		double bbb = 2;
		
		String aa = "a";
		String bb = "b";
		String cc = "c";
		String dd = "d";
		String ee = "e";
		
		list.add(aa, aa, a, a);
		list.add(cc, cc, c, c);
		list.add(bb, bb, b, b);
		list.add(ee, ee, e, e);
		list.add(dd, dd, d, d);
		list.add(aa, aa, a, a);
		list.add(aa, bb, a, c);
		list.add(aa, bb, a, c);
		list.add(aa, bb, a, b);
		list.printMap();
		System.out.println("Does list contain c, c " + list.contains(cc, cc));
		System.out.println(" ");
		
		System.out.println("Removing c, c");
		list.remove(cc, cc);
		
		list.printMap();
		
		System.out.println("Does list contain c, c " + list.contains(cc, cc));
		System.out.println(" ");
		list.add(cc,  cc, c, c);
		
		list.printMap();
		
		System.out.println("Does list contain b, c " +list.contains(bb, cc));
		System.out.println("Does list contain c, c " +list.contains(cc, cc));
		
		System.out.println("Get value for key 1, 1 " + list.getValue(a, a)[0] + list.getValue(a, a)[1]);
		System.out.println("Get value for key 1, 2 " + list.getValue(a, b)[0] + list.getValue(a, b)[1]);
		
		System.out.println("Get value for key 1, 1 (different object) " + list.getValue(aaa, aaa)[0] + list.getValue(aaa, aaa)[1]);
		System.out.println("Get value for key 1, 2 (different object) " + list.getValue(aaa, bbb)[0] + list.getValue(aaa, bbb)[1]);
		
		System.out.println("Print first key " + list.getScoresIndex(0)[0] + list.getScoresIndex(0)[1] + list.getObjectsIndex(0)[0] + list.getObjectsIndex(0)[1]);
	}
}
