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
	
	public static ResultSet runQuery(String st) throws SQLException{
		     
	        QueryStatement qs = new QueryStatement();
	     // System.out.println(username.getKeyword());
	    	// System.out.println(username.getValue());
	    	//	props.setProperty("username","sde");
	    	  props.setProperty(username.getKeyword(),username.getValue());
	    	//    props.setProperty("password","Fomsummer2014");
	    	  props.setProperty(pwd.getKeyword(),pwd.getValue());
	    	    System.out.println(props.getProperty("username"));

	    	    System.out.println(props.getProperty("password"));
	    	    System.out.println(url.getValue());
	    	    Connection conn = DriverManager.getConnection(url.getValue(),props);
	    	    System.out.println("connected");

		    Statement st1 = conn.createStatement();
		    qs.setStatement();
		    System.out.println(qs.getStatement());
	    	ResultSet rs = st1.executeQuery(qs.getStatement());
			return rs;
	    	
	    	
	}

}

