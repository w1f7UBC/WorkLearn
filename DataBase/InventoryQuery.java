
package DataBase;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.Vec3dInput;

public  class InventoryQuery extends Query {
	public InventoryQuery(){
		super.getAll().add(this);
	}

	@Keyword(description = "query statement")
	private StringInput querystatment;
	
	{
		
		querystatment = new StringInput("statement","Query property","");
		this.addInput(querystatment);
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );
	}
	
	@Override
	public ResultSet execute(String latitude, String longitude, Boolean draw){
		//System.out.println(latitude + " " + longitude);
		//for the shape generator/loader in LayerManager class to create the shape show on map
		//for querying the actual data that is represented in the selected/generated area
		String statements = "SELECT DISTINCT ON (stid2) stid2, ABS(si2-si_1),giskey,si_1,primspec,landuse, primage, ecozone2,"
				+ " vol02, vol102, vol202, vol302, vol402, vol502, vol602, vol702, vol802, vol902, vol1002,"
				+ " vol1102,vol1202, vol1302, vol1402, vol1502, vol1602, vol1702, vol1802, vol1902, vol2002,"
				+ " bio02, bio102, bio202, bio302, bio402, bio502, bio602, bio702, bio802, bio902, bio1002,"
				+ " bio1102, bio1202, bio1302, bio1402, bio1502, bio1602, bio1702, bio1802, bio1902, bio2002"
				+ " FROM ab_yc02, ab_03 WHERE primspec=stid2 AND giskey IN(SELECT CAST(gis_key AS CHAR(30)) FROM fmu_1km"
				+ " WHERE st_contains(fmu_1km.geom, ST_GeomFromText('POINT("+longitude+" "+latitude+")', 4269))=true)"
				+ " ORDER BY stid2, ABS(si2-si_1) ASC";
		ResultSet resultset=getResultSet(statements);
		if (draw==true && resultset!=null){
			String toDraw = "\"SELECT geom FROM fmu_1km"
					+ " WHERE gis_key=("
					+ "SELECT gis_key"
					+ " FROM fmu_1km"
					+ " WHERE st_contains(fmu_1km.geom, ST_GeomFromText('POINT("+longitude+" "+latitude+")', 4269))=true);\"";
		}
		return getResultSet(statements);
	}
	
	public String queryAreaStatement(){
		return "SELECT geom FROM ab_ten;";
	}

	@Override
	public void printResultContent(ResultSet resultset){
		try {
	    	ResultSetMetaData metaData = resultset.getMetaData();
		    // names of columns
		    Vector<String> columnNames = new Vector<String>();
		    int columnCount = metaData.getColumnCount();
		    columnNames.add("Stand ID");
			columnNames.add("Site Index");
			columnNames.add("Prime Species");
			columnNames.add("Landuse");
			columnNames.add("Stand Age");
			columnNames.add("Ecozone");
			columnNames.add("Volume");
			columnNames.add("Biomass");
		    // data of the table
		    Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		    while (resultset.next()) {
		    	int age=0;
		    	String volume  =" ";
		    	String biomass =" ";
		    	Vector<Object> vector = new Vector<Object>();
		    	for (int columnIndex = 1; columnIndex <= columnCount;columnIndex++ ){   
		    		if(resultset.getMetaData().getColumnName(columnIndex).contains("giskey")||
		    				resultset.getMetaData().getColumnName(columnIndex).contains("si_1")||
	    			resultset.getMetaData().getColumnName(columnIndex).contains("primspec")||
	    			resultset.getMetaData().getColumnName(columnIndex).contains("landuse")||
	    			resultset.getMetaData().getColumnName(columnIndex).contains("primage")||
	    			resultset.getMetaData().getColumnName(columnIndex).contains("ecozone2")){
		    			if(resultset.getMetaData().getColumnName(columnIndex).contains("primage")){
		    					age = resultset.getInt(columnIndex);
		    				}
		    		vector.add(resultset.getObject(columnIndex));
		    		}
		    	}
		    	int remainder = age%10;
		    	if (remainder>5){
		    		age+=10-remainder;
		    	}
		    	else{
		    		age-=remainder;
		    	}
		    	volume = "vol"+age+"2";
		    	biomass="bio"+age+"2";
			    for(int columnIndex = 1; columnIndex <= columnCount;columnIndex++){
			    	if(resultset.getMetaData().getColumnName(columnIndex).contains(volume)||resultset.getMetaData().getColumnName(columnIndex).contains(biomass)   ){
			    		vector.add(resultset.getObject(columnIndex));
			    	} 
		    	}
			    data.add(vector);
		    }
		    displayResultContent(new JTable(new DefaultTableModel(data, columnNames)));		
		}catch (SQLException e) {
			e.printStackTrace();
			return;
		}
	}
}