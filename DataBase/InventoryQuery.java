
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
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.StringListInput;
import com.sandwell.JavaSimulation.Vec3dInput;

public  class InventoryQuery extends Query {

	private final  String DataBaseName ="";
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
	//	databaseEntity = new EntityInput<DataBaseObject>( DataBaseObject.class, "DataBaseEntity", "Key Inputs", null);
	//	this.addInput(databaseEntity);
		
		target = new StringListInput("target","Query property", new ArrayList<String>(0));
		this.addInput(target);
		tablename = new StringListInput("tablename","Query property",new ArrayList<String>(0));
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
		System.out.println(latitude + " " + longitude);
		//for the shape generator/loader in LayerManager class to create the shape show on map
		String statement = "\"SELECT geom FROM fmu_1km "
				+ "WHERE gis_key=("
				+ "SELECT gis_key "
				+ "FROM fmu_1km "
				+ "WHERE st_contains(fmu_1km.geom, ST_GeomFromText('POINT("+longitude+" "+latitude+")', 4269))=true);\"";
		//for querying the actual data that is represented in the selected/generated area
		String s = "SELECT * FROM ab_03"
				+ " WHERE giskey IN("
				+ "SELECT CAST(gis_key AS CHAR(30))"
				+ " FROM fmu_1km "
				+ " WHERE st_contains(fmu_1km.geom, ST_GeomFromText('POINT("+longitude+" "+latitude+")', 4269))=true)";
		this.setStatement(s);
		return statement;
	}
	
	public String queryAreaStatement(){
		return "SELECT geom FROM ab_ten;";
	}
/*
	@Override
	public  DefaultTableModel getTableContent(String statement) throws SQLException{
		//this.validate();
    	rs = this.getResultset();
		ResultSetMetaData metaData = rs.getMetaData();
	    // names of columns
	    Vector<String> columnNames = new Vector<String>();
	    ArrayList<Integer> index = new ArrayList<Integer>();
	    String ageIndex = "5";
	    int columnCount = metaData.getColumnCount();
	    ArrayList<Integer> index2 = new ArrayList<Integer>();
	    for (int column = 1; column <= columnCount; column++) {
	    	if(metaData.getColumnName(column).contains("grid_code")||metaData.getColumnName(column).contains("ecozone2")||metaData.getColumnName(column).contains("stid2")||metaData.getColumnName(column).contains("si2")||metaData.getColumnName(column).contains("age")){
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
	    String addCol = "vol"+"100"+"2";
	    String addCol2 = "bio"+"100"+"2";
	    columnNames.add(addCol);
	    columnNames.add(addCol2);
	    return new DefaultTableModel(data, columnNames);
	}
	*/
}

