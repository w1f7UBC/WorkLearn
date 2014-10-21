package worldwind;

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
		if (structure.containsValue(objects)){
			for (Entry<Double[], Object[]> target : structure.entrySet()){
				if (target.getValue().equals(objects));
					structure.remove(target);
			}
		}
	}
	
	public Double[] getKey(Object entity, Object movingEntity){
		Object[] objects = {entity, movingEntity};
		if (structure.containsValue(objects)){
			for (Entry<Double[], Object[]> target : structure.entrySet()){
				if (target.getValue().equals(objects));
					return target.getKey();
			}
		}
		return null;
	}
	
	public TreeMap<Double[], Object[]> getMap(){
		return structure;
	}
}

class scoreCompare implements Comparator<Double[]>{

	@Override
	public int compare(Double[] o1, Double[] o2) {
		// TODO Auto-generated method stub
		return 0;
	}
	
}