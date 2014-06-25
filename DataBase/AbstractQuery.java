package DataBase;

import java.util.ArrayList;

import com.sandwell.JavaSimulation3D.DisplayEntity;

public class AbstractQuery extends DisplayEntity {
	private String statement;
	private ArrayList<String> coordinates = null;
    public AbstractQuery(){
    	
    }
	
	public String getStatement(){
		return statement;
	}
	//public void StatementBuilder(String s) {
	//	 TODO Auto-generated method stub
		
//	}
	public ArrayList<String> getCoordinates(){
		
		coordinates.add("100");
		coordinates.add("100");
		return coordinates;
		
	}
	public void setStatement(String statement) {
		this.statement = statement;
	}
}
