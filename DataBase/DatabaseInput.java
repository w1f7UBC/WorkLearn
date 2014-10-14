package DataBase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import com.jaamsim.input.StringInput;



public class DatabaseInput extends StringInput {
	
	private ArrayList<String> validOptions = new ArrayList<String>();
	
	public DatabaseInput(String key, String cat,  String def) {
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
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void updateValues(ArrayList<?> dataList){
		Iterator<?> iterator = dataList.iterator();
		validOptions=new ArrayList<String>();
		validOptions.add("None");
		if(iterator.hasNext()){
			Object target = iterator.next();
			if (target.getClass()==String.class){
				validOptions.add((String) target);
				while(iterator.hasNext()){
					target = iterator.next();
					//System.out.println(target);
					validOptions.add((String) target);
				}
			}
			else if (target.getClass()==Database.class){
				validOptions.add(((Database) target).getName());
				while(iterator.hasNext()){
					target = iterator.next();
					//System.out.println(((Database) target).getName());
					validOptions.add(((Database) target).getName());
				}
			}
		}
	}
	
	@Override
	public ArrayList<String> getValidOptions() {
		return validOptions;
	}
}
