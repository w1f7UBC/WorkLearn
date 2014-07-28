
package DataBase;

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
	@Keyword(description = "target databaseobject of the query")
	private  EntityInput<DataBaseObject> targetDB;
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
		targetDB = new EntityInput<>(DataBaseObject.class, "TargetDatabase","Key Inputs", null);
		this.addInput(targetDB);
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

	public String updateStatement(String longtitude, String latitude) throws IOException {
		String exe = System.getProperty("user.dir")+"\\resources\\exe\\pgsql2shp.exe";
		String destination = System.getProperty("user.dir")+"\\resources\\temp";
		File destinationDir = new File(destination);
		if(!destinationDir.exists()){
			destinationDir.mkdirs();
		}
		destination+="\\queryResults"+latitude+".shp";
		Process process = new ProcessBuilder(exe, "-f", destination, "-h", "25.141.219.39", "-p", "5432", "-u", "sde", "-P", "Fomsummer2014", "fom", "\"SELECT geom FROM fmu_1km WHERE gis_key=(SELECT gis_key from fmu_1km where st_contains(fmu_1km.geom, ST_GeomFromText('POINT("+longtitude+" "+latitude+")', 4269))=true);\"").start();
		String s =  "SELECT gis_key"
				+ " FROM fmu_1km"
				+ " WHERE st_contains(fmu_1km.geom, ST_GeomFromText('POINT(-117.67 56.3798)', 4269))=true;";
		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;
		while ((line = br.readLine()) != null) {
			System.out.println(line);
		}
		this.setStatement(s);
		return destination;
	}

	public String queryAreaGenerate() throws IOException{
		String exe = System.getProperty("user.dir")+"\\resources\\exe\\pgsql2shp.exe";
		String destination = System.getProperty("user.dir")+"\\resources\\temp";
		File destinationDir = new File(destination);
		if(!destinationDir.exists()){
			destinationDir.mkdirs();
		}
		destination+="\\queryArea.shp";
		//System.out.println(exe);
		//System.out.println(destination);
		Process process = new ProcessBuilder(exe, "-f", destination, "-h", "25.141.219.39", "-p", "5432", "-u", "sde", "-P", "Fomsummer2014", "fom", "\"SELECT geom FROM ab_ten;\"").start();
		//Process process = new ProcessBuilder(exe, "-f", destination, "-h", "25.141.219.39", "-p", "5432", "-u", "sde", "-P", "Fomsummer2014", "fom", "\"SELECT geom from fmu_1km where st_contains(fmu_1km.geom, ST_GeomFromText('POINT(-117.67 56.3798)', 4269))=true;\"").start();
		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;
		while ((line = br.readLine()) != null) {
			System.out.println(line);
		}
		return destination;
	}

	@Override
	public  DefaultTableModel getTableContent(String statement) throws SQLException{
    	rs = getResultset();
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
}

