package DataBase;

import java.util.ArrayList;

import com.sandwell.JavaSimulation3D.DisplayEntity;

public class Query extends DisplayEntity {
	private String statement;
	public ArrayList<String> coordinates;
    public Query(){
    	
    }
	
	public String getStatement(){
		return statement;
	}
	
}
