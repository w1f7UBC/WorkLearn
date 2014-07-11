package DataBase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.jaamsim.input.Keyword;
import com.jaamsim.math.Vec3d;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.StringListInput;
import com.sandwell.JavaSimulation.Vec3dInput;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public  class QueryObject extends AbstractQuery {
private static String statement = "SELECT A.grid_Code, B.ecozone2, B.stid2, B.curvtype2 "
		+ "FROM saeed_test A,saeed_gy B "
		+ "WHERE A.grid_code = B.grid_Code "
		+ "AND A.point_x=-96.2293862010 "
		+ "AND A.point_y=56.7500090970 "
		+ "ORDER BY A.grid_Code ASC";
private ArrayList<String> targets;
private ArrayList<String> tablenames;

@Keyword(description = "display targets of the query")
private  StringListInput target;
@Keyword(description = "tables to select from")
private  StringListInput tablename;
@Keyword(description = "Gerometric parameters")
private Vec3dInput  coordinate;
@Keyword(description = "query statement")
private StringInput querystatment;
{ 
	target = new StringListInput("target","Query property", new ArrayList<String>(0));
	this.addInput(target);
	tablename = new StringListInput("tablename","Query property",new ArrayList<String>(0));
	this.addInput(tablename);
	coordinate = new Vec3dInput("coordinate", "Query property", new Vec3d(-0.1d, -0.1d, -0.001d));
	this.addInput(coordinate);
	querystatment = new StringInput("querystatement","Query property","SELECT A.grid_Code, B.ecozone2, B.stid2, B.curvtype2 "
			+ "FROM saeed_test A,saeed_gy B "
			+ "WHERE A.grid_code = B.grid_Code "
			+ "AND A.point_x=-96.2293862010 "
			+ "AND A.point_y=56.7500090970 "
			+ "ORDER BY A.grid_Code ASC" );
	}


private void StatementBuild(){
	
	setStatement(statement);
			}
 	
public String getStatement(){
	  
	  return statement;


	}



public static ResultSet runSQL() throws SQLException{
	QueryObject q = new QueryObject();
	String s = q.getStatement(); 
	
     String url = DataObject.getURL();
    Connection conn = DriverManager.getConnection(url,DataObject.getProperties());

    System.out.println("try to connect"); 
          

	    Statement st = conn.createStatement();
	    System.out.println(s);
    	
	    ResultSet rs = st.executeQuery(s);
    	
	 
		return rs;
}

}

