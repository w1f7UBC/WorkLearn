package worldwind;

import java.util.ArrayList;
import java.util.Arrays;

public class DataListMk2 {
	ArrayList<Double[]> scores;
	ArrayList<Object[]> objects;
	
	public DataListMk2(){
		scores=new ArrayList<Double[]>();
		objects=new ArrayList<Object[]>();
	}
	
	public void add(Object entity, Object movingEntity, Double gScore, Double fScore){
		Double[] score = {gScore, fScore};
		Object[] object = {entity, movingEntity};
		if (scores.isEmpty() && objects.isEmpty()){
			scores.add(score);
			objects.add(object);
		}
		else {
			for(int x=0; x<scores.size(); x++){
				int fCompare = Double.compare(fScore, scores.get(x)[1]);
				if (fCompare>0){
					continue;
				}
				else if (fCompare<0){
					scores.add(x, score);
					objects.add(x, object);
					return;
				}
				else {
					int gCompare = Double.compare(gScore, scores.get(x)[0]);
					if (gCompare>0){
						continue;
					}
					if (gCompare<0){
						scores.add(x, score);
						objects.add(x, object);
						return;
					}
					else{
						scores.add(x, score);
						objects.add(x, object);
						return;
					}
				}
			}
			scores.add(score);
			objects.add(object);
		}
	}
	
	public void remove(Object entity, Object movingEntity){
		Object[] object = {entity, movingEntity};
		for (int x=0; x<objects.size(); x++){
			if (Arrays.equals(objects.get(x), object)){
				scores.remove(x);
				objects.remove(x);
				return;
			}
		}
	}
	
	public Boolean contains(Object entity, Object movingEntity){
		Object[] object = {entity, movingEntity};
		for (int x=0; x<objects.size(); x++){
			if (Arrays.equals(objects.get(x), object)){
				return true;
			}
		}
		return false;
	}
	
	public Object[] getValue(double gScore, double fScore){
		Double[] score = {gScore, fScore};
		for (int x=0; x<scores.size(); x++){
			if (Arrays.equals(scores.get(x), score)){
				return objects.get(x);
			}
		}
		return null;
	}
	
	public Double[] getKey(Object entity, Object movingEntity){
		Object[] object = {entity, movingEntity};
		for (int x=0; x<objects.size(); x++){
			if (Arrays.equals(objects.get(x), object)){
				return scores.get(x);
			}
		}
		return null;
	}
	
	public Double[] getScoresIndex(int index){
		return scores.get(index);
	}
	
	public Object[] getObjectsIndex(int index){
		return objects.get(index);
	}
	
	public boolean isEmpty(){
		return scores.isEmpty();
	}
	
	
	public void printMap(){
		for (int x=0; x<scores.size(); x++){
			System.out.println(scores.get(x)[0] + " " + scores.get(x)[1] + " " + objects.get(x)[0] + " " + objects.get(x)[1]);
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
