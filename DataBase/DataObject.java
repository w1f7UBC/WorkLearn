package DataBase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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
private final   StringInput url;

private final   StringInput username;
private final   StringInput pwd;


	{
		url = new StringInput("url","DataBase Properties","jdbc:postgresql://25.141.219.39:5432/fom");
		this.addInput(url);
		username = new StringInput("username","DataBase Properties","sde");
        this.addInput(username);
        pwd = new StringInput("password","DataBase Properties","Fomsummer2014");
	    this.addInput(pwd);
	}
	
public  void Connection() throws SQLException{
	 Properties props = new Properties();   
	  props.setProperty(username.getKeyword(),username.getValue());
		
	    props.setProperty(pwd.getKeyword(),pwd.getValue());
	    Connection conn = DriverManager.getConnection(url.getValue(),props);

	    
	}
}

