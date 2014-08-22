package DataBase;

import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
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
import worldwind.WorldWindFrame;

import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.GUIFrame;

public class Query extends DisplayEntity {
	private LayerManager manager;
	private static final ArrayList<Query> allInstances;
	static {
		allInstances = new ArrayList<Query>();
	}
	@Keyword(description = "target databaseobject of the query")
	private  EntityInput<DataBaseObject> targetDB;
	@Keyword(description = "target table within database to query off of")
	private StringInput table;
	@Keyword(description = "target table within database that describes the queriable area")
	private StringInput areaTable;
	@Keyword(description = "Statement that will be used should execute(boolean draw) should be called")
	private StringInput statement;
	{
		targetDB = new EntityInput<>(DataBaseObject.class, "TargetDatabase","Query Properties", null);
		this.addInput(targetDB);
		table = new StringInput("TargetTable", "Query Properties", "");
		this.addInput(table);
		areaTable = new StringInput("AreaTable", "Query Properties", "");
		this.addInput(areaTable);
		statement = new StringInput("Statement", "Query Properties", "");
		this.addInput(statement);
	}
		
	public static ArrayList<Query> getAll() {
		synchronized (allInstances) {
			return allInstances;
		}
	}
	
	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);
		if(in== targetDB)
			manager = new LayerManager(targetDB.getValue().getURL(), targetDB.getValue().getUserName(), targetDB.getValue().getPassword());
			
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
		statement.setValueString(statements);
		return;
	}
	
	public void setTable(String tables){
		table.setValueString(tables);
		return;
	}
	
	public ResultSet execute(Boolean draw){
		if (draw==true){
			File file = manager.sql2shp("Statement", statement.getValue());
			if (file!=null){
				new WorldWindFrame.WorkerThread(file, WorldWindFrame.AppFrame).start();
			}
		}
		return getResultSet(statement.getValue());
	}
	
	public ResultSet execute(String name, Boolean draw){
		String statements="SELECT * FROM " + table.getValue() + " WHERE name=" + name;
		if (draw==true){
			File file = manager.sql2shp(name, statements);
			if (file!=null){
				new WorldWindFrame.WorkerThread(file, WorldWindFrame.AppFrame).start();
			}
		}
		return getResultSet(statements);
	}
	
	public ResultSet execute(String name, String latitude, String longitude, Boolean draw){
		String statements="SELECT * FROM " + table.getValue() + "WHERE st_contains("+table+".geom, ST_GeomFromText('POINT("+longitude+" "+latitude+")', 4269)";
		if (draw==true){
			File file = manager.sql2shp(name, statements);
			if (file!=null){
				new WorldWindFrame.WorkerThread(file, WorldWindFrame.AppFrame).start();
			}
		}
		return getResultSet(statements);
	}
	
	public void executeArea(){
		String statements="SELECT * FROM " + areaTable;
		File file = manager.sql2shp(areaTable.getValue(), statements);
		if (file!=null){
			new WorldWindFrame.WorkerThread(file, WorldWindFrame.AppFrame).start();
		}
	}
	
	public ResultSet getResultSet(String statements){
		try {
			Statement st = targetDB.getValue().getConnection().createStatement();
			ResultSet resultset = st.executeQuery(statements);
			return resultset;
		} catch (SQLException e) {
			System.out.println(e);
		}
		return null;
    }
	
	public void printResultContent(String name, ResultSet restultset){  
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
			    displayResultContent(name, new JTable(new DefaultTableModel(data, columnNames)));
			}
		} catch (SQLException e) {
			System.out.println(e);
			return;
		}
	}
	
	public void displayResultContent(final String name, final JTable content){
		 EventQueue.invokeLater(new Runnable() {
			   @Override
			   public void run() {  
				   final JFrame dataBaseFrame = new JFrame(); 
				   final JScrollPane dataBasePanel = new JScrollPane();
				   dataBasePanel.setViewportView(content);
				   dataBaseFrame.add(dataBasePanel);
				   dataBaseFrame.setSize(750, 305);
	               dataBaseFrame.setLocation(1160, 735);
				   dataBaseFrame.setIconImage(GUIFrame.getWindowIcon());
				   dataBaseFrame.setTitle(name);
				   dataBaseFrame.setAutoRequestFocus(false);
				   dataBaseFrame.setVisible(true);
				   dataBaseFrame.addWindowListener(new WindowAdapter(){
					   @Override
					   public void windowClosing(WindowEvent e){
						   WorldWindFrame.AppFrame.removeShapefileLayer(name);
					   }
				   });
			   }
		});  
	}
	
	public LayerManager getLayerManager(){
		return manager;
	}
}

	
