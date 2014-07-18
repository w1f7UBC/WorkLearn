
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

import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Vec3d;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.StringListInput;
import com.sandwell.JavaSimulation.Vec3dInput;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public  class QueryObject extends AbstractQuery {
	
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
//	@Keyword(description ="x coordiantes")
//	private static StringInput x;
//	@Keyword(description ="y coordiantes")
//	private static StringInput y;
	public static String x = "";
	public static String y = "";

	{ 
		target = new StringListInput("target","Query property", new ArrayList<String>(0));
		this.addInput(target);
		tablename = new StringListInput("tablename","Query property",new ArrayList<String>(0));
		this.addInput(tablename);
		coordinate = new Vec3dInput("coordinate", "Query property", new Vec3d(-0.1d, -0.1d, -0.001d));
		this.addInput(coordinate);
		querystatment = new StringInput("statement","Query property",statement);
		this.addInput(querystatment);  
	}
	
	public static String statement = 
			"SELECT *"
				+ " FROM saeed_gy"
				+ " WHERE grid_code"
				+ " IN (SELECT grid_code"
				+ " FROM saeed_test"
				+ " WHERE point_x>"+(x)+" AND point_y<"+(y)
				+ " ORDER BY point_x ASC, point_y DESC"
				+ " LIMIT 1)";
	
	public String getStatement(){
		return statement;
	}
	/*public String setCoordinateX(String a){		
		return x=a;
		
	}
	public String setCoordinate1Y(String b){
		return y=b;
	}
	*/
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );
	}
	public static void updateStatement(String a,String b){
		x=a;
		y=b;
		statement =  "SELECT *"
					+ " FROM saeed_gy"
					+ " WHERE grid_code"
					+ " IN (SELECT grid_code"
					+ " FROM saeed_test"
					+ " WHERE point_x>"+(x)+" AND point_y<"+(y)
					+ " ORDER BY point_x ASC, point_y DESC"
					+ " LIMIT 1)";
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

