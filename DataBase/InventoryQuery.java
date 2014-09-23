
package DataBase;

import java.io.File;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.StringInput;

import worldwind.DefinedShapeAttributes;
import worldwind.WorldWindFrame;

public  class InventoryQuery extends Query {
	
	//@Keyword(description = "target table within database that describes the queriable area")
	private StringInput areaTable;
	
	{
	areaTable = new StringInput("AreaTable", "Query Properties", "ab_ten");
	this.getEditableInputs().set(5, areaTable);
	}
	
	@Override
	public ResultSet execute(String name, String latitude, String longitude, Boolean draw, Boolean zoom, DefinedShapeAttributes attributes){
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
		if (draw==true){
			String toDraw = "\"SELECT geom FROM fmu_1km"
					+ " WHERE gis_key=("
					+ "SELECT gis_key"
					+ " FROM fmu_1km"
					+ " WHERE st_contains(fmu_1km.geom, ST_GeomFromText('POINT("+longitude+" "+latitude+")', 4269))=true);\"";
			File file = getLayerManager().sql2shp(name, toDraw);
			if (file!=null){
				Thread thread=new WorldWindFrame.WorkerThread(file, WorldWindFrame.AppFrame, zoom, attributes);
				thread.start();
				try {
					thread.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return resultset;
	}

	@Override

	public void executeArea(Boolean zoom, DefinedShapeAttributes attributes){
		String statements="SELECT * FROM " + this.areaTable;
		File file = getLayerManager().sql2shp(this.areaTable.getValue(), statements);
		if (file!=null){
			Thread thread=new WorldWindFrame.WorkerThread(file, WorldWindFrame.AppFrame, zoom, attributes);
			thread.start();
			try {
				thread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void printResultContent(String name, ResultSet resultset){
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
		    displayResultContent(name, new JTable(new DefaultTableModel(data, columnNames)));
		}catch (SQLException e) {
			e.printStackTrace();
			return;
		}
	}
}
