
package DataBase;

import gov.nasa.worldwind.geom.Position;


import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import newt.LayerManager;

import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;

import com.sandwell.JavaSimulation.StringInput;

import com.sandwell.JavaSimulation.Vec3dInput;

public  class InventoryQuery extends Query {

	public InventoryQuery(){
		allInstances.add(this);
	}

	private static  ArrayList<InventoryQuery> allInstances;
	

	@Keyword(description = "Gerometric parameters")
	private Vec3dInput  coordinate;
	@Keyword(description = "query statement")
	private StringInput querystatment;

	static {
		allInstances = new ArrayList<InventoryQuery>();
	}
	{
	
		
		
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
		String latitude = position.latitude.toString().split("бу")[0];
		String longitude = position.longitude.toString().split("бу")[0];
		//System.out.println(latitude + " " + longitude);
		//for the shape generator/loader in LayerManager class to create the shape show on map
		String statement = "\"SELECT geom FROM fmu_1km"
				+ " WHERE gis_key=("
				+ "SELECT gis_key"
				+ " FROM fmu_1km"
				+ " WHERE st_contains(fmu_1km.geom, ST_GeomFromText('POINT("+longitude+" "+latitude+")', 4269))=true);\"";
		//for querying the actual data that is represented in the selected/generated area
		String s = "SELECT DISTINCT ON (stid2) stid2, ABS(si2-si_1),giskey,si_1,primspec,landuse, primage, ecozone2,"
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
		
		   rs = getResultset();
	    	ResultSetMetaData metaData = rs.getMetaData();
		    // names of columns
		    Vector<String> columnNames = new Vector<String>();
		    int columnCount = metaData.getColumnCount();
		    columnNames.add("STANDz_ID");
			   columnNames.add("Site_Index");
			   columnNames.add("Prime_Species");
			   columnNames.add("Landuse");
			   columnNames.add("STAND_AGE");
			   columnNames.add("ecozone");
			   columnNames.add("volumn");
			   columnNames.add("Biomass");
		    
		    // data of the table
		    Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		  
		    while (rs.next()) {
		    	  int age=0;
				    String volumn  =" ";
			    	String biomass =" ";
		    	Vector<Object> vector = new Vector<Object>();
    	for (int columnIndex = 1; columnIndex <= columnCount;columnIndex++ ){   
		    		if(rs.getMetaData().getColumnName(columnIndex).contains("giskey")||
		    				rs.getMetaData().getColumnName(columnIndex).contains("si_1")||
		    				rs.getMetaData().getColumnName(columnIndex).contains("primspec")||
		    				rs.getMetaData().getColumnName(columnIndex).contains("landuse")||
		    				rs.getMetaData().getColumnName(columnIndex).contains("primage")||
		    				rs.getMetaData().getColumnName(columnIndex).contains("ecozone2")
		    			){
		    			if(rs.getMetaData().getColumnName(columnIndex).contains("primage")){
		    				age = rs.getInt(columnIndex);
		    			}
		    		   vector.add(rs.getObject(columnIndex));
		    		   }
		    	}
		    	     int remainder = age%10;
		    	     if (remainder>5){
		    	    	 age+=10-remainder;
		    	     }
		    	     else{
		    	    	 age-=remainder;
		    	     }
		    		 volumn = "vol"+age+"2";
		    		 biomass="bio"+age+"2";
			    	for(int columnIndex = 1; columnIndex <= columnCount;columnIndex++){
			    	   if(rs.getMetaData().getColumnName(columnIndex).contains(volumn)||
			    			   rs.getMetaData().getColumnName(columnIndex).contains(biomass)   ){
			    		vector.add(rs.getObject(columnIndex));
			    	   
		    	} 
		    	
		    	}
			    data.add(vector);
		    }
		    return new DefaultTableModel(data, columnNames);
		}
	}