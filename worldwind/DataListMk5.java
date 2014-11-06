package worldwind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DataListMk5 {
	HashMap<Object[], Double[]> structure;
	
	public DataListMk5(){
		structure = new HashMap<Object[], Double[]>();
	}
	
	public void add(Object entity, Object movingEntity, Double gScore, Double fScore){
		Double[] score = {gScore, fScore};
		Object[] object = {entity, movingEntity};
		structure.put(object, score);
	}
	
	public void remove(Object entity, Object movingEntity){
		Object[] object = {entity, movingEntity};
		structure.remove(object);
	}
	
	public Boolean contains(Object entity, Object movingEntity){
		Object[] object = {entity, movingEntity};
		return structure.containsKey(object);
	}
	
	public Object[] getValue(double gScore, double fScore){
		Double[] score = {gScore, fScore};
		for (Map.Entry<Object[], Double[]> e : structure.entrySet()) {
		   if (Arrays.equals(e.getValue(), score)){
			   return e.getKey();
		   }
		}
		return null;
	}
	
	public Double[] getKey(Object entity, Object movingEntity){
		Object[] object = {entity, movingEntity};
		structure.get(object);
		return null;
	}
	
	public Double[] getScoresIndex(int index){
		Double[] score = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
		for (Map.Entry<Object[], Double[]> e : structure.entrySet()) {
			Double[] temp = e.getValue();
			if (temp[1] < score[1]){
			   score = temp;
		   }
			else if (temp[1] == score[1]){
				if(temp[0] < temp[1]){
					score=temp;
				}
			}
		}
		return score;
	}
	
	public Object[] getObjectsIndex(int index){
		Double[] score = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
		Object[] object = null;
		for (Map.Entry<Object[], Double[]> e : structure.entrySet()) {
			Double[] temp = e.getValue();
			if (temp[1] < score[1]){
			   score = temp;
			   object = e.getKey();
		   }
			else if (temp[1] == score[1]){
				if(temp[0] < temp[1]){
					score=temp;
					object = e.getKey();
				}
			}
		}
		return object;
	}
	
	public boolean isEmpty(){
		return structure.isEmpty();
	}
	
	public void printMap(){
		for (Map.Entry<Object[], Double[]> e : structure.entrySet()) {
			Double[] scores=e.getValue();
			Object[] objects=e.getKey();
			System.out.println(scores[0] + " " + scores[1] + " " + objects[0] + " " + objects[1]);
		}
	}
	
	public static void main(String args[]){
		long startTime = System.nanoTime();
		System.out.println("TEST");
		DataListMk2 list= new DataListMk2();
		System.out.println("Is List empty? " + list.isEmpty());
		double a = 1;
		double b = 2;
		double c = 3;
		double d = 4;
		double e = 5;
		double f = 6;
		
		double aaa = 1;
		double bbb = 2;
		
		String aa = "a";
		String bb = "b";
		String cc = "c";
		String dd = "d";
		String ee = "e";
		
		list.add(ee, ee, e, e);
		list.add(aa, aa, a, a);
		System.out.println("Removing ee, ee and aa, aa");
		list.remove(ee, ee);
		list.remove(aa, aa);
		System.out.println("Is List empty? " + list.isEmpty());
		list.add(cc, cc, c, c);
		list.add(bb, bb, b, b);
		list.add(ee, ee, e, e);
		list.add(dd, dd, d, d);
		list.add(aa, aa, a, a);
		list.add(aa, bb, a, c);
		list.add(aa, bb, a, c);
		list.add(aa, bb, a, b);
		list.add(ee, ee, a, e);
		list.add(ee, ee, f, e);
		list.printMap();
		System.out.println("Does list contain c, c " + list.contains(cc, cc));
		System.out.println(" ");
		System.out.println("Is List empty? " + list.isEmpty());
		
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
		long endTime   = System.nanoTime();
		long totalTime = endTime - startTime;
		System.out.println("Runtime= "+totalTime + " nanoseconds");
	}
}
