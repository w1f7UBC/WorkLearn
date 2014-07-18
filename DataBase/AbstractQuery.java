package DataBase;

import java.util.ArrayList;

import com.sandwell.JavaSimulation3D.DisplayEntity;

public class AbstractQuery extends DisplayEntity {
	private String statement;
	public ArrayList<String> coordinates;
    public AbstractQuery(){
    	
    }
	
	public String getStatement(){
		return statement;
	}
	
}
