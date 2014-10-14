package DataBase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.sandwell.JavaSimulation.StringListInput;

public class DatabaseInputList extends StringListInput {
	
	private ArrayList<String> validOptions = new ArrayList<String>();
	
	public DatabaseInputList(String key, String cat,  ArrayList<String> def) {
		super(key, cat, def);
	}
	
	public void updateValues(ResultSet resultSet){
		try {
			validOptions=new ArrayList<String>();
			validOptions.add("None");
				if (resultSet!=null){
				while (resultSet.next()){
					validOptions.add(resultSet.getString(1));
				}
			}
		} catch (
				SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public ArrayList<String> getValidOptions() {
		return validOptions;
	}
}