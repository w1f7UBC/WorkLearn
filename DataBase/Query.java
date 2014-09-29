package DataBase;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import worldwind.DefinedShapeAttributes;
import worldwind.LayerManager;
import worldwind.QueryFrame;
import worldwind.WorldWindFrame;
import worldwind.WorldWindFrame.WorkerThread;

import com.ROLOS.ROLOSEntity;
import com.ROLOS.Utils.HandyUtils;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation3D.GUIFrame;

public class Query extends Entity {
	private static final ArrayList<Query> allInstances;
	private static ArrayList<JFrame> resultFrames;
	static {
		allInstances = new ArrayList<Query>();
		resultFrames = new ArrayList<JFrame>();
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
	
	@Keyword(description = "The column name where latitude information is recoreded")
	private StringInput latitudeColumn;
	
	@Keyword(description = "The column name where longitude information is recoreded")
	private StringInput longitudeColumn;
	
	{
		targetDB = new StringInput("TargetDatabase","Query Properties", "InventoryDatabase");
		this.addInput(targetDB);
		table = new StringInput("TargetTable", "Query Properties", "");
		this.addInput(table);
		areaTable = new StringInput("AreaTable", "Query Properties", "");
		this.addInput(areaTable);
		statement = new StringInput("Statement", "Query Properties", "");
		this.addInput(statement);
		
		shapefileColumn = new StringInput("NameColumn", "Query Properties", "");
		this.addInput(shapefileColumn);
		
		latitudeColumn = new StringInput("LatitudeColumn", "Query Properties", "latitude");
		this.addInput(latitudeColumn);
		
		longitudeColumn = new StringInput("LongitudeColumn", "Query Properties", "longitude");
		this.addInput(longitudeColumn);
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
		InputAgent.processEntity_Keyword_Value(this, statement, statements);
		return;
	}

	public void setTable(String tables){
		InputAgent.processEntity_Keyword_Value(this, table, tables);
		return;
	}

	public ResultSet execute(Boolean draw, Boolean zoom, DefinedShapeAttributes attributes){
		if (draw==true){
			File file = database.getLayermanager().sql2shp("Statement", statement.getValue());
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
		return getResultSet(statement.getValue());
	}
		
	public ResultSet execute(String layerName, ArrayList<? extends ROLOSEntity> drawableEntities, Boolean draw, Boolean zoom, DefinedShapeAttributes attributes){
		if (drawableEntities.size()==0){
			return null;
		}
		String statements="SELECT * FROM " + table.getValue() + " WHERE " + shapefileColumn.getValue() +"= '" + drawableEntities.get(0).getName() +"'";
		for(int x=1; x<drawableEntities.size(); x++){
			statements+=" or " + shapefileColumn.getValue() +"= '" + drawableEntities.get(x).getName() + "'";
		}
		// System.out.println(statements);
		if (draw==true && WorldWindFrame.AppFrame != null){
			File file = database.getLayermanager().sql2shp(layerName, statements);
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
		return getResultSet(statements);
	}
	
	public ResultSet execute(String name, String latitude, String longitude, Boolean draw, Boolean zoom, DefinedShapeAttributes attributes){
		String radius_statements ="",statements="";
		/*
		 *  RADIUS SEARCH
		 */
		if(QueryFrame.getMode()==2){
			// TODO bad implementation! hacky way of passing selection statement!
		 statements="SELECT objectid_1 as Stand_ID, round(site_index,1) as Site_index, round(poly_area,1) as Area, concat(spec_cd_1,'(', round(spec_pct_1,0), '%)', case when round(spec_pct_2,0) > 0 then "
		 		+ "('-' || spec_cd_2||'('||round(spec_pct_2,0)|| '%)') end) as Species_Pct,"+
       "round(proj_ht_1,1) as Avg_Height, round(proj_age_1,1) as Avg_Age, round(lvlsp1_125,1) as merchant_vol, round(dvltot_125,1) as dead_vol, round(wstem_biom,1) as stemwood_biomass, round(bark_biom,1) as Bark_biomass, round(brnch_biom,1) as Branch_biomass, "
       + "round(folg_biom,1) as Foliage_biomass FROM " + areaTable.getValue() + " WHERE st_distance(ST_Transform("+areaTable.getValue()+".shape,26986), ST_Transform(ST_GeomFromText('POINT("+longitude+" "+latitude+")', 4269),26986))<"+QueryFrame.getSliderValue()*1000;
		 radius_statements="SELECT ST_Buffer(ST_MakePoint("+longitude+","+ latitude+")::geography, "+QueryFrame.getSliderValue()*1000+")";
		 //color of circle
		 attributes.setColor(Color.red);
		}
		/*
		 *  CLOSEST POINT
		 */
		else if(QueryFrame.getMode()==3){
		 statements="SELECT * FROM "+ areaTable.getValue() + " ORDER BY "+ areaTable.getValue() +".shape <->  ST_GeomFromText('POINT("+longitude+" "+latitude+")', 4269) LIMIT 1";
			}
		if (draw==true){
			File file=null;
			if(QueryFrame.getMode()==2){
				File circle = database.getLayermanager().sql2shp(name+"circle", radius_statements);
				WorkerThread prethread =new WorldWindFrame.WorkerThread(circle, WorldWindFrame.AppFrame, zoom, new DefinedShapeAttributes());
				prethread.start();
				try {
					prethread.join();
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				file = database.getLayermanager().sql2shp(name, statements);
			}
			else if(QueryFrame.getMode()==3)
			{
				 file = database.getLayermanager().sql2shp(name, statements);
			}
			if (file!=null){
				
				WorkerThread thread =new WorldWindFrame.WorkerThread(file, WorldWindFrame.AppFrame, zoom, attributes);
				thread.start();
				try {
					thread.join();
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return getResultSet(statements);
	}

	public void executeArea(Boolean zoom, DefinedShapeAttributes attributes){
		String statements="SELECT * FROM " + areaTable;
		//System.out.println(statements);
		File file = database.getLayermanager().sql2shp(areaTable.getValue(), statements);
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
	
	/**
	 * updates position based on lat longs passed on in the shapefile database. Will draw the entity if its WVShow is set to true.
	 * @param entitiesList list of entities to update
	 * @return 
	 */
	public ResultSet updatePosition(ArrayList<? extends ROLOSEntity> entitiesList){
		if (entitiesList.size()==0){
			return null;
		}
		
		String statements="SELECT " + shapefileColumn.getValue()+ ", "+ latitudeColumn.getValue() + ", " + longitudeColumn.getValue() +" FROM " + 
				table.getValue() + " WHERE " + shapefileColumn.getValue() +"= '" + entitiesList.get(0).getName() +"'";
		for(int x=1; x<entitiesList.size(); x++){
			statements+=" or " + shapefileColumn.getValue() +"= '" + entitiesList.get(x).getName() + "'";
		}
		// System.out.println(statements);
		ResultSet tempResultSet = this.getResultSet(statements);
		
		//map of entitieslist to pass on the entity
		ArrayList<String> entitiesNames = new ArrayList<String>();
		for(ROLOSEntity each: entitiesList)
			entitiesNames.add(each.getName());
		
		try {
			if (tempResultSet!=null){
				while(tempResultSet.next()){
					ROLOSEntity tempEntity=null;
					if(entitiesNames.contains(tempResultSet.getObject(1).toString())){
						int index = entitiesNames.indexOf(tempResultSet.getObject(1).toString());
						tempEntity = entitiesList.get(index);
						entitiesNames.remove(index);
						entitiesList.remove(tempEntity);
					}
					if(tempEntity != null)
						InputAgent.processEntity_Keyword_Value(tempEntity, "WVPosition", String.format((Locale)null, "%s %s 0", tempResultSet.getObject(2).toString(), tempResultSet.getObject(3).toString()));
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(!entitiesNames.isEmpty())
			System.out.println(String.format("Shapefiles database %s didn't include any of these entities: %s!", table.getValue(),HandyUtils.arraylistToString(entitiesNames)));
		
		return getResultSet(statements);
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
				   final JFrame dataBaseFrame = new JFrame(name);
				   final JScrollPane dataBasePanel = new JScrollPane();
				   dataBasePanel.setViewportView(content);
				   dataBaseFrame.add(dataBasePanel);
	               dataBaseFrame.setLocation(GUIFrame.COL2_START, GUIFrame.LOWER_START);
	               dataBaseFrame.setSize(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width-GUIFrame.COL2_START, GUIFrame.VIEW_HEIGHT);
				   dataBaseFrame.setIconImage(GUIFrame.getWindowIcon());
				   dataBaseFrame.setAutoRequestFocus(false);
				   dataBaseFrame.setVisible(true);
				   dataBaseFrame.addWindowListener(new WindowAdapter(){
					   @Override
					   public void windowClosing(WindowEvent e){
						   WorldWindFrame.AppFrame.removeShapefileLayer(name+".shp");
					   }
				   });
				   dataBaseFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				   resultFrames.add(dataBaseFrame);
			   }
		});
	}

	public LayerManager getLayerManager(){
		return database.getLayermanager();
	}
	
	public static ArrayList<JFrame> getResultFrames(){
		return resultFrames;
	}
	
	public static boolean deleteAllResultFrames(){
		Iterator<JFrame> iterator =  resultFrames.iterator();
		while (iterator.hasNext()){
			JFrame target = iterator.next();

			WorldWindFrame.AppFrame.removeShapefileLayer(target.getTitle()+".shp");
			target.setVisible(false);
			target.dispose();
		}
		return true;
	}
	
	public static boolean deleteResultFrame(String name){
		Iterator<JFrame> iterator =  resultFrames.iterator();
		while (iterator.hasNext()){
			JFrame target = iterator.next();
			if (target.getName()==name){

				WorldWindFrame.AppFrame.removeShapefileLayer(target.getTitle()+".shp");
				target.setVisible(false);
				target.dispose();
				return true;
			}
		}
		return false;
	}
}


