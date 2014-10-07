package DataBase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import worldwind.LayerManager;

import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringInput;
import com.sandwell.JavaSimulation.Entity;


public class Database extends Entity {
	private static final ArrayList<Database> allInstances;
	static {
		allInstances = new ArrayList<Database>();
	}
	@Keyword(description = "URL",example = "Test URL {jdbc:postgresql://142.103.228.134:5432/fom}")
	private  StringInput url;
	private  StringInput username;
	private StringInput password;
	private Connection conn=null;	
	private Properties props = new Properties();
    {
    	url = new StringInput("url","Database Properties","jdbc:postgresql://142.103.228.134:5432/fom");
    	this.addInput(url);
    	username = new StringInput("username","Database Properties","sde");
    	this.addInput(username);
    	password = new StringInput("password","Database Properties","Fomsummer2014");
    	this.addInput(password);
    }
    
	public static ArrayList<Database> getAll() {
		synchronized (allInstances) {
			return allInstances;
		}
	}
	
	public static Database getDatabase(String target){
		Iterator<Database> iterator = allInstances.iterator();
		while (iterator.hasNext()){
			Database database=iterator.next();
			if (database.getName().equals(target)){
				return database;
			}
		}
		return  null;
	}
	
	public Database(){
		getAll().add(this);
	}
    
	@Override 
	public void validate(){
		getConnection();
	}
	
	public Connection getConnection(){
		if (conn==null){
			try {
				conn = DriverManager.getConnection(getURL(),getProperties());
			} catch (SQLException e) {
				System.out.println(e);
			}
		}
	    return conn;
	}
	
	public String getURL(){
		return url.getValue();
	}
	
	public String getUserName(){
		return username.getValue();
	}
	
	public String getPassword(){
		return password.getValue();
	}
	
	public Properties getProperties() {
		props.setProperty("user",username.getValue());
	    props.setProperty("password",password.getValue());
		return props;
	}	 
	
	public LayerManager getLayermanager(){
		return new LayerManager(getURL(), getUserName(), getPassword());
	}
}

