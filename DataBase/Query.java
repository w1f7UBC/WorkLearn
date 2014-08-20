package DataBase;

import java.awt.EventQueue;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import worldwind.LayerManager;

import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.GUIFrame;

public class Query extends DisplayEntity {
	private static final ArrayList<Query> allInstances;
	static {
		allInstances = new ArrayList<Query>();
	}
	
	@Keyword(description = "target databaseobject of the query")
	private  EntityInput<DataBaseObject> targetDB;
	{
		targetDB = new EntityInput<>(DataBaseObject.class, "TargetDatabase","Key Inputs", null);
		this.addInput(targetDB);
	}
	
	protected String table;
	protected String statement;
	private LayerManager manager;
		
	public static ArrayList<Query> getAll() {
		synchronized (allInstances) {
			return allInstances;
		}
	}
	
	@Override
	public void validate() {
		super.validate();
		// Confirm that prototype entity has been specified
		if( targetDB.getValue() == null ) {
			throw new InputErrorException( "The keyword targetDB must be set." );
		}
	}
	
	//change the default statement in this object
	public void setStatement(String statements){
		statement=statements;
		return;
	}
	
	public void setTable(String tables){
		table=tables;
		return;
	}
	
	public ResultSet execute(Boolean draw){
		ResultSet resultSet=getResultSet(statement);
		if (draw==true && resultSet!=null){

		}
		return resultSet;
	}
	
	public ResultSet execute(String name, Boolean draw){
		String statements="SELECT * FROM " + table + " WHERE name=" + name;
		ResultSet resultSet=getResultSet(statements);
		if (draw==true && resultSet!=null){
			
		}
		return resultSet;
	}
	
	public ResultSet execute(String latitude, String longitude, Boolean draw){
		String statements="SELECT * FROM " + table + "WHERE st_contains("+table+".geom, ST_GeomFromText('POINT("+longitude+" "+latitude+")', 4269)";
		ResultSet resultSet=getResultSet(statements);
		if (draw==true && resultSet!=null){
			
		}
		return resultSet;
	}
	
	protected ResultSet getResultSet(String statements){
		try {
			Statement st = targetDB.getValue().getConnection().createStatement();
			ResultSet resultset = st.executeQuery(statements);
			return resultset;
		} catch (SQLException e) {
			System.out.println(e);
		}
		return null;
    }
    
	public void drawResultContent(ResultSet resultset){
		
	}
	
	public void printResultContent(ResultSet restultset){  
		try{
			if (restultset!=null){
			    ResultSetMetaData metaData = restultset.getMetaData();
			    // names of columns
			    Vector<String> columnNames = new Vector<String>();
			    int columnCount = metaData.getColumnCount();
			    for (int column = 1; column <= columnCount; column++) {
			    	columnNames.add(metaData.getColumnName(column));
			    }
			    // data of the table
			    Vector<Vector<Object>> data = new Vector<Vector<Object>>();
			    while (restultset.next()) {
			    	Vector<Object> vector = new Vector<Object>();
			    	for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
			    		vector.add(restultset.getObject(columnIndex));
			    	}
			    	data.add(vector);
			    }
			    displayResultContent(new JTable(new DefaultTableModel(data, columnNames)));
			}
		} catch (SQLException e) {
			System.out.println(e);
			return;
		}
	}
	
	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);
		if(in== targetDB)
			manager = new LayerManager(targetDB.getValue().getURL(), targetDB.getValue().getUserName(), targetDB.getValue().getPassword());
			
	}
	
	protected void displayResultContent(final JTable content){
		EventQueue.invokeLater(new Runnable() {
			   @Override
			   public void run() {  
				   final JFrame dataBaseFrame = new JFrame(); 
				   final JScrollPane dataBasePanel = new JScrollPane();
				   dataBasePanel.setViewportView(content);
				   dataBaseFrame.add(dataBasePanel);
				   dataBaseFrame.setBounds(GUIFrame.COL3_START+GUIFrame.COL3_WIDTH,GUIFrame.LOWER_START,GUIFrame.COL3_WIDTH,GUIFrame.LOWER_HEIGHT);
				   dataBaseFrame.setVisible(true);
			   }
		});  
	}
}

	
