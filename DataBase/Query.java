package DataBase;

import java.awt.EventQueue;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class Query extends DisplayEntity {
	private String statement = "";
	
	public ArrayList<String> coordinates;

	protected ResultSet rs;
	@Keyword(description = "target databaseobject of the query")
	private  EntityInput<DataBaseObject> targetDB;	
	{
	targetDB = new EntityInput<>(DataBaseObject.class, "TargetDatabase","Key Inputs", null);
	this.addInput(targetDB);
	}
	@Override
	public void validate() {
		super.validate();

		// Confirm that prototype entity has been specified
		if( targetDB.getValue() == null ) {
			throw new InputErrorException( "The keyword targetDB must be set." );
		}
	}
	
	public String getStatement(){
		return statement;
	}
	public void setStatement(String s){
		statement = s;
	}
    public ResultSet getResultset() throws SQLException{
    	Statement st = targetDB.getValue().getConnection().createStatement();
		 rs = st.executeQuery(statement);
		 return rs;
    }
	public  DefaultTableModel getTableContent(String statement) throws SQLException{  
    	rs = getResultset();
    ResultSetMetaData metaData = rs.getMetaData();

    // names of columns
    Vector<String> columnNames = new Vector<String>();
    int columnCount = metaData.getColumnCount();
    for (int column = 1; column <= columnCount; column++) {
        columnNames.add(metaData.getColumnName(column));
    }
  // data of the table
    Vector<Vector<Object>> data = new Vector<Vector<Object>>();
      while (rs.next()) {
	      Vector<Object> vector = new Vector<Object>();
	        for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
	         vector.add(rs.getObject(columnIndex));
	      }
		     data.add(vector);
			    }

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

	