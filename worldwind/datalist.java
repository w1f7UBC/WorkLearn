package worldwind;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.TreeMap;

public class datalist {
	private TreeMap<Double[], Object[] > structure;
	
	public datalist(){
		structure=new TreeMap<Double[], Object[]>(new scoreCompare());
	}
	
	public void add(Object entity, Object movingEntity, Double gScore, Double fScore){
		Double[] scores = {gScore, fScore};
		Object[] objects = {entity, movingEntity};
		structure.put(scores, objects);
	}
	
	public void remove(Object entity, Object movingEntity){
		Object[] objects = {entity, movingEntity};
		for (Entry<Double[], Object[]> target : structure.entrySet()){
			if (Arrays.equals(target.getValue(), objects)){
				structure.remove(target.getKey());
				return;
			}
		}
	}
	
	public Double[] getKey(Object entity, Object movingEntity){
		Object[] objects = {entity, movingEntity};
		for (Entry<Double[], Object[]> target : structure.entrySet()){
			if (Arrays.equals(target.getValue(), objects)){
				return target.getKey();
			}
		}
		return null;
	}
	
	public Object[] getValue(double gScore, double fScore){
		Object[] scores = {gScore, fScore};
		for (Entry<Double[], Object[]> target : structure.entrySet()){
			if (Arrays.equals(target.getKey(), scores)){
				return target.getValue();
			}
		}
		return null;
	}
	
	public Boolean contains(Object entity, Object movingEntity){
		Object[] objects = {entity, movingEntity};
		for (Entry<Double[], Object[]> target : structure.entrySet()){
			if (Arrays.equals(target.getValue(), objects)){
				return true;
			}
		}
		return false;	
	}
	
	public void printMap(){
		for (Entry<Double[], Object[]> target : structure.entrySet()){
			System.out.println(target.getKey()[0] + " " + target.getKey()[1] + " " + target.getValue()[0] + " " + target.getValue()[1]);
		}
	}
	
	public TreeMap<Double[], Object[]> getMap(){
		return structure;
	}
	
	public static void main(String args[]){
		System.out.println("TEST");
		datalist list= new datalist();
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
		
		list.printMap();
		System.out.println("Does list contain c, c " + list.contains(cc, cc));
		System.out.println(" ");
		list.remove(cc, cc);
		
		list.printMap();
		
		System.out.println("Does list contain c, c " + list.contains(cc, cc));
		System.out.println(" ");
		list.add(cc,  cc, c, c);
		
		list.printMap();
		
		System.out.println("Does list contain b, c " +list.contains(bb, cc));
		System.out.println("Does list contain c, c " +list.contains(cc, cc));
		
		System.out.println("Get value for key 1, 1 " + list.getValue(a, a)[0] + list.getValue(a, a)[1]);
		//System.out.println("Get value for key 1, 2 " + list.getValue(a, b)[0] + list.getValue(a, b)[1]);
		
		System.out.println("Get value for key 1, 1 (different object) " + list.getValue(aaa, aaa)[0] + list.getValue(aaa, aaa)[1]);
		//System.out.println("Get value for key 1, 2 (different object) " + list.getValue(aaa, bbb)[0] + list.getValue(aaa, bbb)[1]);
		
		System.out.println("Print first key " + list.getMap().firstEntry().getKey()[0] + list.getMap().firstEntry().getKey()[1] + list.getMap().firstEntry().getValue()[0] + list.getMap().firstEntry().getValue()[1]);
	}
}

class scoreCompare implements Comparator<Double[]>{

	@Override
	public int compare(Double[] o1, Double[] o2) {
		// TODO Auto-generated method stub
		if (o1[0]<o2[0]){
			return -1;
		}
		else if(o1[0]>o2[0]){
			return 1;
		}
		return 0;
	}	
}