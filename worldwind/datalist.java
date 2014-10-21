package worldwind;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;


public class datalist {
	private HashMap<?, ?> objects;
	
	private class data{
		private SortedMap<Double, Double> scores;
		private Map<Object, Object> objects;
	}
	public datalist(){
		scores=new TreeMap<Double, Double>();
		objects=new HashMap<Object, Object>();
	}
	
	public void add(Object entity, Object movingEntity, Double gScore, Double fScore){
		
	}
	
	public void remove(Object entity, Object movingEntity){
		
	}
	
	public Boolean contain(Object entity, Object movingEntity){
		return true;
	}
	
}
