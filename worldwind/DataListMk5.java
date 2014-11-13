package worldwind;

import java.util.HashMap;
import java.util.Map;

public class DataListMk5 {
	HashMap<Pair, Pair> structure;
	
	public DataListMk5(){
		structure = new HashMap<Pair, Pair>();
	}
	
	public void add(Object entity, Object movingEntity, Double gScore, Double fScore){
		Pair score = new Pair(gScore, fScore);
		Pair object = new Pair(entity, movingEntity);
		structure.put(object, score);
	}
	
	public void remove(Object entity, Object movingEntity){
		Pair object = new Pair(entity, movingEntity);
		structure.remove(object);
	}
	
	public Boolean contains(Object entity, Object movingEntity){
		Pair object = new Pair(entity, movingEntity);
		return structure.containsKey(object);
	}
	
	public Object[] getValue(double gScore, double fScore){
		Pair score = new Pair(gScore, fScore);
		for (Map.Entry<Pair, Pair> e : structure.entrySet()) {
			Pair target = e.getKey();
			if (target.equals(score)){
			   Object[] targetValue = {target.getA(), target.getB()};
			   return targetValue;
		   }
		}
		return null;
	}
	
	public Double[] getKey(Object entity, Object movingEntity){
		Pair object = new Pair(entity, movingEntity);
		Pair target = structure.get(object);
		if (target==null){
			return null;	
		}
		else {
			Double[] targetValue = {(Double)target.getA(), (Double)target.getB()};
			return targetValue;
		}
	}
	
	public Double[] getScoresIndex(int index){
		if (structure.isEmpty()){
			return null;
		}
		Pair score = new Pair(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
		Boolean changed = false;
		for (Map.Entry<Pair, Pair> e : structure.entrySet()) {
			Pair target = e.getValue();
			if ((Double)target.getB() < (Double)score.getB()){
			   score = target;
			   changed = true;
		   }
			else if ((Double)target.getB() == (Double)score.getB()){
				if((Double)target.getA() < (Double)score.getA()){
					score = target;
					changed = true;
				}
			}
		}
		if (changed == false){
			return null;
		}
		Double[] targetValue= {(Double)score.getA(), (Double)score.getB()};
		return targetValue;
	}
	
	public Object[] getObjectsIndex(int index){
		if (structure.isEmpty()){
			return null;
		}
		Pair score = new Pair(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
		Pair object = null;
		Boolean changed = false;
		for (Map.Entry<Pair, Pair> e : structure.entrySet()) {
			Pair target = e.getValue();
			if ((Double)target.getB() < (Double)score.getB()){
			   score = target;
			   object = e.getKey();
			   changed = true;
		   }
			else if ((Double)target.getB() == (Double)score.getB()){
				if((Double)target.getA() < (Double)score.getA()){
					score = target;
					object = e.getKey();
					changed = true;
				}
			}
		}
		if (changed == false){
			return null;
		}
		Object[] target = {object.getA(), object.getB()};
		return target;
	}
	
	public boolean isEmpty(){
		return structure.isEmpty();
	}
	
	public void printMap(){
		for (Map.Entry<Pair, Pair> e : structure.entrySet()) {
			Pair scores=e.getValue();
			Pair objects=e.getKey();
			System.out.println(scores.getA() + " " + scores.getB() + " " + objects.getA() + " " + objects.getB());
		}
	}
	
	private class Pair {
		private Object a;
		private Object b;
		
		private Pair(Object a, Object b){
			this.a=a;
			this.b=b;
		}
		
		private Object getA(){
			return a;
		}
		
		private Object getB(){
			return b;
		}
		
		@Override
		public int hashCode() {
			return a.hashCode() ^ b.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return (obj instanceof Pair) && ((Pair) obj).a.equals(a) && ((Pair) obj).b.equals(b);
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
		String ff = "a";
		
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
