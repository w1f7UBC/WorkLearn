
package DataBase;

import gov.nasa.worldwind.geom.Position;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import newt.LayerManager;

import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Vec3d;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.StringListInput;
import com.sandwell.JavaSimulation.Vec3dInput;

public  class InventoryQuery extends Query {

	public InventoryQuery(){
		allInstances.add(this);
	}
	private ArrayList<String> targets;
	private ArrayList<String> tablenames;
	private static  ArrayList<InventoryQuery> allInstances;
	
	@Keyword(description = "display targets of the query")
	private  StringListInput target;
	@Keyword(description = "tables to select from")
	private  StringListInput tablename;
	@Keyword(description = "Gerometric parameters")
	private Vec3dInput  coordinate;
	@Keyword(description = "query statement")
	private StringInput querystatment;

	static {
		allInstances = new ArrayList<InventoryQuery>();
	}
	{
	
		
		target = new StringListInput("target","Query property",targets);
		this.addInput(target);
		tablename = new StringListInput("tablename","Query property",tablenames);
		this.addInput(tablename);
		coordinate = new Vec3dInput("coordinate", "Query property", new Vec3d(-0.1d, -0.1d, -0.001d));
		this.addInput(coordinate);
		querystatment = new StringInput("statement","Query property","");
		this.addInput(querystatment);
	}

	public static ArrayList<? extends InventoryQuery> getAll() {
		synchronized (allInstances) {
			return allInstances;
		}
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );
	}
	
	public LayerManager getCredentials(){
		return new LayerManager(targetDB.getValue().getURL(), targetDB.getValue().getUserName(), targetDB.getValue().getPassword());
	}

	public String updateStatement(Position position){
		String latitude = position.latitude.toString().split("°")[0];
		String longitude = position.longitude.toString().split("°")[0];
		//System.out.println(latitude + " " + longitude);
		//for the shape generator/loader in LayerManager class to create the shape show on map
		String statement = "\"SELECT geom FROM fmu_1km"
				+ " WHERE gis_key=("
				+ "SELECT gis_key"
				+ " FROM fmu_1km"
				+ " WHERE st_contains(fmu_1km.geom, ST_GeomFromText('POINT("+longitude+" "+latitude+")', 4269))=true);\"";
		//for querying the actual data that is represented in the selected/generated area
		String s = "SELECT DISTINCT ON (stid2) stid2, ABS(si2-si_1), landuse, ecozone2, primage,"
				+ " vol02, vol102, vol202, vol302, vol402, vol502, vol602, vol702, vol802, vol902, vol1002,"
				+ " vol1102,vol1202, vol1302, vol1402, vol1502, vol1602, vol1702, vol1802, vol1902, vol2002,"
				+ " bio02, bio102, bio202, bio302, bio402, bio502, bio602, bio702, bio802, bio902, bio1002,"
				+ " bio1102, bio1202, bio1302, bio1402, bio1502, bio1602, bio1702, bio1802, bio1902, bio2002"
				+ " FROM ab_yc02, ab_03 WHERE primspec=stid2 AND giskey IN(SELECT CAST(gis_key AS CHAR(30)) FROM fmu_1km"
				+ " WHERE st_contains(fmu_1km.geom, ST_GeomFromText('POINT("+longitude+" "+latitude+")', 4269))=true)"
				+ " ORDER BY stid2, ABS(si2-si_1) ASC";
		this.setStatement(s);
		return statement;
	}
	
	public String queryAreaStatement(){
		return "SELECT geom FROM ab_ten;";
	}

	@Override
	public  DefaultTableModel getTableContent(String statement) throws SQLException{
    	rs = this.getResultset();
		ResultSetMetaData metaData = rs.getMetaData();
	    // names of columns
	    Vector<String> columnNames = new Vector<String>();
	    ArrayList<Integer> index = new ArrayList<Integer>();
	    int columnCount = metaData.getColumnCount();
	    ArrayList<Integer> index2 = new ArrayList<Integer>();
	    for (int column = 1; column <= columnCount; column++) {
	    	if(metaData.getColumnName(column).contains("primspec")||metaData.getColumnName(column).contains("secspec")||metaData.getColumnName(column).contains("landuse")||metaData.getColumnName(column).contains("primage")||metaData.getColumnName(column).contains("city")){
    			columnNames.add(metaData.getColumnName(column));
	    		index.add(column);
			}
    	}
	    // data of the table
	    Vector<Vector<Object>> data = new Vector<Vector<Object>>();
	    while (rs.next()){
	    	Vector<Object> vector = new Vector<Object>();
	        for ( int columnIndex : index) {
	        	if (columnIndex == 5){

	        	}
	        	vector.add(rs.getObject(columnIndex));
	        }
	        data.add(vector);
	    }
	 //  String addCol = "vol"+"100"+"2";
	  // String addCol2 = "bio"+"100"+"2";
  // columnNames.add(addCol);
	//   columnNames.add(addCol2);
	    return new DefaultTableModel(data, columnNames);
	}
	
}

