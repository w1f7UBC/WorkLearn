package DataBase;

import java.sql.Connection;
import java.util.Properties;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.StringInput;

public class DataBaseObject extends Entity {
	
	@Keyword(description = "URL",example = "Test URL {jdbc:postgresql://142.103.228.134:5432/fom}")
	private  StringInput url;
	private  StringInput username;
	private StringInput password;
	private int connectionIndex = 0;	
	private Properties props = new Properties();
    private boolean established = false;
    public  DataBaseObject(){
    }

    {
    	url = new StringInput("url","DataBase Properties","jdbc:postgresql://142.103.228.134:5432/fom");
    	this.addInput(url);
    	username = new StringInput("username","DataBase Properties","sde");
    	this.addInput(username);
    	password = new StringInput("password","DataBase Properties","Fomsummer2014");
    	this.addInput(password);
    }
    
	@Override 
	public void validate(){
		connectionIndex = Database.Connect(this);
		established = true;
	}
	
	
	public Connection getConnection(){
		if (!established){
			connectionIndex = Database.Connect(this);
			established = true;
		} 
	    return Database.getConnection(connectionIndex);
	}
		
	public DataBaseObject getDataBaseObject(){
		return this;
		
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
	  
}

