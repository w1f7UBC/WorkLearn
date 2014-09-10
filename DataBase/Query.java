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

import worldwind.DefinedShapeAttributes;
import worldwind.LayerManager;
import worldwind.WorldWindFrame;

import com.ROLOS.ROLOSEntity;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.GUIFrame;

public class Query extends DisplayEntity {
	private static final ArrayList<Query> allInstances;
	static {
		allInstances = new ArrayList<Query>();
	}
	@Keyword(description = "target databaseobject of the query")
	private  StringInput targetDB;
	
	@Keyword(description = "target table within database to query off of")
	private StringInput table;
	
	@Keyword(description = "target table within database that describes the queriable area")
	private StringInput areaTable;
	
	@Keyword(description = "Statement that will be used should execute(boolean draw) should be called")
	private StringInput statement;
	
	@Keyword(description = "The column name where shapefile appear")
	private StringInput shapefileColumn;
	
	{
		targetDB = new StringInput("TargetDatabase","Query Properties", "InventoryDatabase");
		this.addInput(targetDB);
		table = new StringInput("TargetTable", "Query Properties", "");
		this.addInput(table);
		areaTable = new StringInput("AreaTable", "Query Properties", "");
		this.addInput(areaTable);
		statement = new StringInput("Statement", "Query Properties", "");
		this.addInput(statement);
		
		shapefileColumn = new StringInput("ShapeFileColumn", "Query Properties", "");
		this.addInput(shapefileColumn);
	}
	private Database database=Database.getDatabase(targetDB.getValue());

    @Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);
		if(in==targetDB){
			database=Database.getDatabase(targetDB.getValue());
		}
	}

	public static ArrayList<Query> getAll() {
		synchronized (allInstances) {
			return allInstances;
		}
	}

	public Query(){
		getAll().add(this);
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
		InputAgent.processEntity_Keyword_Value(this,statement,statements);
		return;
	}

	public void setTable(String tables){
		table.setValueString(tables);
		return;
	}

	public ResultSet execute(Boolean draw, DefinedShapeAttributes attributes){
		if (draw==true){
			File file = database.getLayermanager().sql2shp("Statement", statement.getValue());
			if (file!=null){
				new WorldWindFrame.WorkerThread(file, WorldWindFrame.AppFrame, attributes).start();
			}
		}
		return getResultSet(statement.getValue());
	}

	public ResultSet execute(String layerName, ArrayList<? extends ROLOSEntity> drawableEntities, Boolean draw, DefinedShapeAttributes attributes){
		if (drawableEntities.size()==0){
			return null;
		}
		String statements="SELECT * FROM " + table.getValue() + " WHERE " + shapefileColumn.getValue() +"= '" + drawableEntities.get(0).getName() +"'";
		for(int x=1; x<drawableEntities.size(); x++){
			statements+=" or " + shapefileColumn.getValue() +"= '" + drawableEntities.get(x).getName() + "'";
		}
		System.out.println(statements);
		if (draw==true && WorldWindFrame.AppFrame != null){
			File file = database.getLayermanager().sql2shp(layerName, statements);
			if (file!=null){
				new WorldWindFrame.WorkerThread(file, WorldWindFrame.AppFrame, attributes).start();
			}
		}
		return getResultSet(statements);
	}
	
	public ResultSet execute(String name, String latitude, String longitude, Boolean draw, DefinedShapeAttributes attributes){
		String statements="SELECT * FROM " + table.getValue() + " WHERE st_contains("+table.getValue()+".shape, ST_GeomFromText('POINT("+longitude+" "+latitude+")', 4269))=true";
		//System.out.println(statements);
		if (draw==true){
			File file = database.getLayermanager().sql2shp(name, statements);
			if (file!=null){
				new WorldWindFrame.WorkerThread(file, WorldWindFrame.AppFrame, attributes).start();
			}
		}
		return getResultSet(statements);
	}

	public void executeArea(DefinedShapeAttributes attributes){
		String statements="SELECT * FROM " + areaTable;
		//System.out.println(statements);
		File file = database.getLayermanager().sql2shp(areaTable.getValue(), statements);
		if (file!=null){
			new WorldWindFrame.WorkerThread(file, WorldWindFrame.AppFrame, attributes).start();
		}
	}

	public ResultSet getResultSet(String statements){
		try {
			Statement st = database.getConnection().createStatement();
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
	               dataBaseFrame.setLocation(GUIFrame.COL4_START, GUIFrame.LOWER_START);
	               dataBaseFrame.setSize(GUIFrame.COL4_WIDTH, GUIFrame.LOWER_HEIGHT);
				   dataBaseFrame.setIconImage(GUIFrame.getWindowIcon());
				   dataBaseFrame.setTitle(name);
				   dataBaseFrame.setAutoRequestFocus(false);
				   dataBaseFrame.setVisible(true);
				   dataBaseFrame.addWindowListener(new WindowAdapter(){
					   @Override
					   public void windowClosing(WindowEvent e){
						   WorldWindFrame.AppFrame.removeShapefileLayer(name+".shp");
					   }
				   });
			   }
		});
	}

	public LayerManager getLayerManager(){
		return database.getLayermanager();
	}
}


