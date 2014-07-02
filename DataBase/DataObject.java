package DataBase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation3D.Region;

public class DataObject extends Entity {
	public DataObject(){
		
	}
	@Keyword(description = "URL",example = "Test URL {jdbc:postgresql://25.141.219.39:5432/fom}")
public static  StringInput url;

public static  StringInput username;
public static  StringInput pwd;
public static Properties props = new Properties();

	{
		url = new StringInput("url","DataBase Properties","jdbc:postgresql://25.141.219.39:5432/fom");
		this.addInput(url);
		username = new StringInput("username","DataBase Properties","sde");
        this.addInput(username);
        pwd = new StringInput("password","DataBase Properties","Fomsummer2014");
	    this.addInput(pwd);
	}

	public  void Connection() throws SQLException{
	   
	

	    
	}
	
	public Connection getConnection(){
		return null;
		
	}
	
	public static String getURL(){
		return url.getValue();
	}
	public static Properties getProperties(){
		props.setProperty("user","sde");
		
	    props.setProperty("password","Fomsummer2014");
		return props;
	}

}

