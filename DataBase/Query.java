package DataBase;

import java.awt.EventQueue;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class Query extends DisplayEntity {
	protected String statement = "";
	public ArrayList<String> coordinates;
	protected  String x = "";
	protected  String y = "";
	private ResultSet rs;
	@Keyword(description = "target databaseobject of the query")
	private  EntityInput<DataBaseObject> targetDB;	
	{
	targetDB = new EntityInput<>(DataBaseObject.class, "TargetDatabase","Key Inputs", null);
	this.addInput(targetDB);
	}
	
	
	
	public String getStatement(){
		return statement;
	}
    public  DefaultTableModel getTableContent(String statement) throws SQLException{  
		
		rs = targetDB.getValue().getResultset(statement);
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
	public  void Poptable(String statement) throws SQLException{
		   final JTable table = new JTable(getTableContent(statement));
		   EventQueue.invokeLater(new Runnable() {
		        @Override
		        public void run() {
		        	JOptionPane.showMessageDialog(null, new JScrollPane(table));	   
		        }
		   });
		   return;
	   }
	public  void excuteQuery(String statement) throws SQLException{
    	try{ 
    		Poptable(statement);
    	}catch(SQLException e){
    		System.out.println(e);
    		System.exit(1);
    	}
	}
	
	}

	