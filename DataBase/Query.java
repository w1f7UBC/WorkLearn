package DataBase;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfaceCircle;
import gov.nasa.worldwind.render.SurfaceShape;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import worldwind.DefinedShapeAttributes;
import worldwind.LayerManager;
import worldwind.QueryFrame;
import worldwind.WorldWindFrame;
import worldwind.WorldWindFrame.WorkerThread;

import com.AROMA.AROMAEntity;
import com.AROMA.Input.InputAgent_Rolos;
import com.AROMA.Logistics.DiscreteHandlingLinkedEntity;
import com.AROMA.Utils.HandyUtils;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.ValueInput;


import com.jaamsim.math.Color4d;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.GUIFrame;


public class Query extends Entity {
	private static final ArrayList<Query> allInstances;
	private static ArrayList<JFrame> resultFrames;
	static {
		allInstances = new ArrayList<Query>();
		resultFrames = new ArrayList<JFrame>();
	}
	@Keyword(description = "target databaseobject of the query")
	private DatabaseInput database;
	
	@Keyword(description = "turn on/off automatic querybuilding")
	private BooleanInput queryBuild;
	
	@Keyword(description = "target table within database to query off of")
	private DatabaseInput table;
	
	@Keyword(description = "The column name that specifies the specific row to be returned")
	private DatabaseInput column;
	
	@Keyword(description = "The operator that specifies the row and column comparison")
	private DatabaseInput operator;
	
	@Keyword(description = "The row identifier that specifies the specific row to be returned")
	private DatabaseInputList row;
	
	@Keyword(description = "Statement that will be used should execute(boolean draw) should be called")
	private StringInput statement;
	
	@Keyword(description = "Determines which type of query to use, point specific queries only eg. uses latitude and longitude")
	private IntegerInput mode;
	
	@Keyword(description = "Used in specific mode to determine radius/size of query")
	private IntegerInput radius;
	
	@Keyword(description = "Determines draw mode, true=draw, false=dont draw")
	private BooleanInput draw;
	
	@Keyword(description = "Determines what happens after a shape is drawn, true=zoom to, false=do nothing")
	private BooleanInput zoom;
	
	@Keyword(description = "Determines whether to print result set or not, true=print, false=do nothing")
	private BooleanInput print;
	
	@Keyword(description = "Determines the print orientation, true=print vertical, false=print horizontal")
	private BooleanInput printOrientation;
	
	@Keyword(description = "Determines the color of the primary objects")
	private ColourInput primaryColor;
	
	@Keyword(description = "Determines the color of the secondary objects")
	private ColourInput secondaryColor;
	
	@Keyword(description = "Determines the thickness of the lines used to draw objects")
	private IntegerInput thickness;
	
	@Keyword(description = "Determines the opacity of the objects")
	private ValueInput opacity;
	
	@Keyword(description = "The column name where latitude information is recoreded")
	private StringInput latitudeColumn;
	
	@Keyword(description = "The column name where longitude information is recoreded")
	private StringInput longitudeColumn;
	
	{
		database = new DatabaseInput("TargetDatabase","Query Properties", "None");
		this.addInput(database);
	
		queryBuild = new BooleanInput("AutoQueryBuilding", "Query Properties", false);
		this.addInput(queryBuild);
		
		table = new DatabaseInput("TargetTable", "Query Properties", "None");
		this.addInput(table);
		
		column = new DatabaseInput("TargetCollumn", "Query Properties", "None");
		this.addInput(column);

		operator = new DatabaseInput("TargetOperator", "Query Properties", "=");
		this.addInput(operator);
		
		ArrayList<String> input = new ArrayList<String>();
		input.add("None");
		row = new DatabaseInputList("TargetRow", "Query Properties", input);
		this.addInput(row);
		
		statement = new StringInput("Statement", "Query Properties", "");
		this.addInput(statement);
		
		mode = new IntegerInput("Mode", "Query Properties", 0);
		this.addInput(mode);
		
		radius = new IntegerInput("Radius", "Query Properties", 25);
		this.addInput(radius);
		
		draw = new BooleanInput("Draw", "Display Properties", true);
		this.addInput(draw);
		
		zoom = new BooleanInput("Zoom", "Display Properties", true);
		this.addInput(zoom);
		
		primaryColor = new ColourInput("PrimaryColor", "Display Properties", ColourInput.BLUE);
		this.addInput(primaryColor);
		
		secondaryColor = new ColourInput("SecondaryColor", "Display Properties", ColourInput.RED);
		this.addInput(secondaryColor);
		
		thickness = new IntegerInput("Thickness", "Display Properties", 3);
		this.addInput(thickness);
		
		opacity = new ValueInput("Opacity", "Display Properties", 0.03);
		this.addInput(opacity);
		
		print = new BooleanInput("Printresults", "Display Properties", true);
		this.addInput(print);
		
		printOrientation = new BooleanInput("Printorientation", "Display Properties", true);
		this.addInput(printOrientation);
		
		latitudeColumn = new StringInput("LatitudeColumn", "Query Properties", "latitude");
		this.addInput(latitudeColumn);
		
		longitudeColumn = new StringInput("LongitudeColumn", "Query Properties", "longitude");
		this.addInput(longitudeColumn);
	}
	
	private Database targetDatabase;
	private boolean drawVar=draw.getValue();
	private boolean zoomVar=zoom.getValue();
	private boolean printVar=print.getValue();
	private boolean orientVar=printOrientation.getValue();
	private Color4d priColVar=primaryColor.getValue();
	private Color4d secColVar=secondaryColor.getValue();
	private int thickVar=thickness.getValue();
	private double opacityVar=opacity.getValue();
	
	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);
		if(in==database){
			targetDatabase=Database.getDatabase(database.getValue());
		}
		if(in==queryBuild){
			if (queryBuild.getValue()==true){
				table.updateValues(getResultSet("SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name"));
			}
		}
		if(in==table){
			if (queryBuild.getValue()==true){
				column.updateValues(getResultSet("SELECT column_name FROM information_schema.columns WHERE table_name='" + table.getValue()+"' ORDER BY column_name"));
			}
		}
		if(in==column){
			if (queryBuild.getValue()==true){
				row.updateValues(getResultSet("SELECT " + column.getValue() +" FROM " + table.getValue() + " ORDER BY " + column.getValue()));
			}
		}
		if(in==radius){
			if (queryBuild.getValue()==true){
				QueryFrame.setSliderValue(radius.getValue());
			}
		}
		if(in==mode){
			if (queryBuild.getValue()==true){
				QueryFrame.setMode(mode.getValue());
			}
		}
		if(in==draw){
			drawVar=draw.getValue();
		}
		if(in==zoom){
			zoomVar=zoom.getValue();
		}
		if(in==print){
			printVar=print.getValue();
		}
		if(in==printOrientation){
			orientVar=printOrientation.getValue();
		}
		if(in==primaryColor){
			priColVar=primaryColor.getValue();
		}
		if(in==secondaryColor){
			secColVar=secondaryColor.getValue();
		}
		if(in==thickness){
			thickVar=thickness.getValue();
		}
		if(in==opacity){
			opacityVar=opacity.getValue();
		}
	}

	public static ArrayList<Query> getAll() {
		synchronized (allInstances) {
			return allInstances;
		}
	}

	public Query(){
		getAll().add(this);
		database.updateValues(Database.getAll());
		ArrayList<String> operators = new ArrayList<String>();
		operators.add("=");
		operators.add("<");
		operators.add(">");
		operator.updateValues(operators);
	}

	@Override
	public void validate() {
		super.validate();
		// Confirm that prototype entity has been specified
		if( database.getValue() == null ) {
			throw new InputErrorException( "The keyword database must be set." );
		}
	}

	public String execute(){
		String targetStatement = statement.getValue();
		String targetTable = table.getValue();
		String targetColumn = column.getValue();
		String targetOperator = operator.getValue();
		ArrayList<String> targetRow = row.getValue();
		String executeStatement = "";
		String executeName = "";
		if ((!targetStatement.isEmpty() && !targetTable.equals("None")) || (targetStatement.isEmpty() && targetTable.equals("None"))){
			System.out.println("Statement and TargetTable are mutually exclusive but are (or not) set, please use one only");
			return null;
		} 
		if (!targetStatement.isEmpty()){
			executeStatement= targetStatement;
			executeName= targetStatement;
		}
		else if (!targetTable.equals("None")){
			if (!targetColumn.equals("None")){
				if(!targetRow.get(0).equals("None")){
					Iterator<String> iterator = targetRow.iterator();
					String value = iterator.next();
					executeStatement= "SELECT * FROM " + targetTable + " WHERE " + targetColumn + targetOperator + value;
					executeName=targetTable + "_" + targetColumn + "_" + value;
					while (iterator.hasNext()){
						value = iterator.next();
						executeStatement+= " OR " + targetColumn + targetOperator + value;
						executeName+= value;
					}
				}
			}
			else{
				executeStatement= "SELECT * FROM " + targetTable;
				executeName= targetTable;
			}
		}
		//System.out.println("TargetTable= "+ targetTable + System.lineSeparator() + "Draw= " + getDraw() + System.lineSeparator()+ "Zoom= "+zoom+ System.lineSeparator() +executeStatement);
		executeName=executeName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
		if (getDraw()==true && WorldWindFrame.AppFrame != null){
			//System.out.println(executeStatement);
			File file =getLayerManager().sql2shp(executeName, executeStatement);
			if (file!=null){
				Thread thread=new WorldWindFrame.WorkerThread(file, WorldWindFrame.AppFrame, getZoom(), getSecondaryColor());
				thread.start();
				try {
					thread.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		if (getPrint()==true){
			printResultContent(executeName, getResultSet(executeStatement), getPrintOrientation());
		}
		return executeName;
	}

	public String execute(String latitude, String longitude){
		String targetTable=table.getValue();
		Integer modes=mode.getValue();
		Integer radiuses=null;
		String executeStatement = "";
		String executeName = table.getValue()+"("+latitude+","+longitude+")mode" + mode;
		String geomColumn="";
		Renderable currentShape=null;
		if(modes==0){
			return null;
		}
		if (targetTable.equals("None")){
			System.out.println("Table must be specified for selection mode to work");
			return null;
		}
		//closest point
		if(modes==1){
			geomColumn=findGeomColumn();
			executeStatement="SELECT * FROM "+ targetTable + " ORDER BY "+ targetTable +"."+geomColumn+" <->  ST_GeomFromText('POINT("+longitude+" "+latitude+")', st_srid("+geomColumn+")) LIMIT 1";
		}
		//radius search
		else if(modes==2){	
			geomColumn=findGeomColumn();
			radiuses = radius.getValue();
			executeStatement="SELECT DISTINCT * FROM " + targetTable + " WHERE st_distance(ST_Transform("+targetTable+"."+geomColumn+",26986), ST_Transform(ST_GeomFromText('POINT("+longitude+" "+latitude+")', st_srid("+geomColumn+")),26986))<"+radiuses*1000;	
		}
		//System.out.println("TargetTable= "+ targetTable + System.lineSeparator() + "Draw= " + getDraw() + System.lineSeparator()+ "Zoom= "+zoom+ System.lineSeparator() +executeStatement);
		if (getDraw()==true && WorldWindFrame.AppFrame != null){
			File file=null;
			file = getLayerManager().sql2shp(executeName, executeStatement);			
			if (file!=null){
				if(modes==2){
					LatLon position = new LatLon(Angle.fromDegrees(Double.parseDouble(latitude)), Angle.fromDegrees(Double.parseDouble(longitude)));
					Object circle= new SurfaceCircle(position, radiuses*1000);
					currentShape = (Renderable) circle;
					SurfaceShape shape = (SurfaceShape) currentShape;
					ShapeAttributes attr = new BasicShapeAttributes();
					attr.setDrawOutline(false);
					Color color = new Color((float)secondaryColor.getValue().r, (float)secondaryColor.getValue().g, (float)secondaryColor.getValue().b);
					attr.setInteriorMaterial(new Material(color));
					attr.setInteriorOpacity(0.2f);
					attr.setDrawInterior(true);
					shape.setAttributes(attr);
					RenderableLayer layer = new RenderableLayer();
					layer.setName(executeName+".shp");
					layer.removeAllRenderables();
					layer.addRenderable(currentShape);
					layer.setPickEnabled(false);
					WorldWindFrame.AppFrame.addShapefileLayer(layer);
					WorldWindFrame.AppFrame.getWwjPanel().getWwd().redraw();
				}
				WorkerThread thread =new WorldWindFrame.WorkerThread(file, WorldWindFrame.AppFrame, getZoom(), getPrimaryColor());
				thread.start();
				try {
					thread.join();	
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		if (getPrint()==true){
			printResultContent(executeName, getResultSet(executeStatement), getPrintOrientation());
		}
		return executeName;
	}
	
	public String execute(ArrayList<DiscreteHandlingLinkedEntity> routesList, boolean draws, boolean zooms, boolean prints, Color4d primaryColors, int widths, double opacities) {
		setDraw(draws);
		setZoom(zooms);
		setPrint(prints);
		setPrimaryColor(primaryColors);
		setThickness(widths);
		setOpacity(opacities);
		return execute(routesList);
	}
	
	public String execute(ArrayList<? extends AROMAEntity> drawableEntities){
		if (drawableEntities.size()==0){
			return null;
		}
		String executeStatement="SELECT * FROM " + table.getValue() + " WHERE " + column.getValue() +"= '" + drawableEntities.get(0).getName() +"'";
		for(int x=1; x<drawableEntities.size(); x++){
			executeStatement+=" or " + row.getValue() +"= '" + drawableEntities.get(x).getName() + "'";
		}
		// System.out.println(executeStatement);
		String executeName=executeStatement.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
		if (getDraw()==true && WorldWindFrame.AppFrame != null){
			File file = getLayerManager().sql2shp(executeName, executeStatement);
			if (file!=null){
				Thread thread=new WorldWindFrame.WorkerThread(file, WorldWindFrame.AppFrame, getZoom(), getPrimaryColor());
				thread.start();
				try {
					thread.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		if (getPrint()==true){
			printResultContent(executeName, getResultSet(executeStatement), getPrintOrientation());
		}
		return executeName;
	}

	/**
	 * updates position based on lat longs passed on in the shapefile database. Will draw the entity if its WVShow is set to true.
	 * @param entitiesList list of entities to update
	 * @return 
	 */
	public ResultSet updatePosition(ArrayList<? extends AROMAEntity> entitiesList){
		if (entitiesList.size()==0){
			return null;
		}
		String statements="SELECT " + column.getValue()+ ", "+ latitudeColumn.getValue() + ", " + longitudeColumn.getValue() +" FROM " + 
				table.getValue() + " WHERE " + column.getValue() +"= '" + entitiesList.get(0).getName() +"'";
		for(int x=1; x<entitiesList.size(); x++){
			statements+=" or " + column.getValue() +"= '" + entitiesList.get(x).getName() + "'";
		}
		 System.out.println(statements);
		ResultSet tempResultSet = this.getResultSet(statements);
		//map of entitieslist to pass on the entity
		ArrayList<String> entitiesNames = new ArrayList<String>();
		for(AROMAEntity each: entitiesList){
			entitiesNames.add(each.getName());
		}
		try {
			if (tempResultSet!=null){
				while(tempResultSet.next()){
					AROMAEntity tempEntity=null;
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
			Statement st = targetDatabase.getConnection().createStatement();
			ResultSet resultset = st.executeQuery(statements);
			return resultset;
		} catch (SQLException e) {
			System.out.println(e);
		}
		return null;
    }
	
	private String findGeomColumn(){   
		String targetTable = table.getValue(); 
		String statements="SELECT column_name, data_type FROM information_schema.columns WHERE table_name ='"+targetTable+"'";
		ResultSet resultset= getResultSet(statements);
		try {
			if (resultset!=null){
				 ResultSetMetaData metaData =resultset.getMetaData();
				 while(resultset.next()){
					for(int i=1;i<=metaData.getColumnCount();i++){	
						//System.out.println(metaData.getColumnName(i)+" "+resultset.getObject(i));
						if(resultset.getObject(i).toString().equals("USER-DEFINED")){
							return resultset.getObject(i-1).toString();
						}
					}
				 }	
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	
	public void printResultContent(String name, ResultSet resultset, boolean verticalOrientation){
				//If vertical orientation
		if(verticalOrientation == true){
			try{
				if (resultset!=null){

				    ResultSetMetaData metaData = resultset.getMetaData();
				    
				    //Sets column titles
					Vector<String> columnName = new Vector<String>();
					columnName.add("Key");
					
					//Creates vectors that fill table line by line
					Vector<Vector<Object>> data = new Vector<Vector<Object>>();
					int keyCount = metaData.getColumnCount();
					//Makes key column
					for (int key = 1; key <= keyCount; key++){
						String keyName = metaData.getColumnName(key);
						Vector<Object> keyVector = new Vector<Object>();
						keyVector.add(keyName);
						data.add(keyVector);
					}
					//Makes a new column for every object
					while(resultset.next()){
						columnName.add(" ");
						for (int key = 1; key <= keyCount; key++){
							//finds respective rows and adds values to them
							Object keyValue = resultset.getObject(key);
							data.get(key-1).add(keyValue);
						}
					}
					displayResultContent(name, new JTable(new DefaultTableModel(data, columnName)));
				}
			} catch (SQLException e) {
				System.out.println(e);
				return;
			}
		}
		// If horizontal orientation
		else{
			try{
				if (resultset!=null){
				    ResultSetMetaData metaData = resultset.getMetaData();
				    // names of columns
				    Vector<String> columnNames = new Vector<String>();
				    int columnCount = metaData.getColumnCount();
				    for (int column = 1; column <= columnCount; column++) {
				    	columnNames.add(metaData.getColumnName(column));
				    }
				    // data of the table
				    Vector<Vector<Object>> data = new Vector<Vector<Object>>();
				    while (resultset.next()) {
				    	Vector<Object> vector = new Vector<Object>();
				    	for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
				    		vector.add(resultset.getObject(columnIndex));
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
	               dataBaseFrame.setSize(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width-GUIFrame.COL2_START, GUIFrame.LOWER_HEIGHT);
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
				   JMenuBar menuBar = new JMenuBar();
					JButton save = new JButton( "Save" );
					save.addActionListener( new ActionListener(){
						@Override
						public void actionPerformed(ActionEvent e) {
							// TODO Auto-generated method stub
							final JFileChooser chooser = new JFileChooser();
							chooser.setSelectedFile(new File(name+".csv"));
							int returnVal = chooser.showSaveDialog(dataBaseFrame);
							if (returnVal == JFileChooser.APPROVE_OPTION) {
								File file = chooser.getSelectedFile();
								String filePath = file.getPath();
								// Confirm overwrite if file already exists
								File temp = new File(filePath);
								if (temp.exists()) {
									int userOption = JOptionPane.showConfirmDialog( null,
											file.getName() + " already exists.\n" +
											"Do you wish to replace it?", "Confirm Save As",
											JOptionPane.YES_NO_OPTION,
											JOptionPane.WARNING_MESSAGE );
									if (userOption == JOptionPane.NO_OPTION) {
										return;
									}
								}
								jTable2CSV(content, filePath);
							}
						}
					});
					menuBar.add(save);
					dataBaseFrame.setJMenuBar(menuBar);
			   }
		});
	}
	
	public void jTable2CSV(JTable content, String location){
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(location), "utf-8"));
			int colCount = content.getColumnCount();
			int rowCount = content.getRowCount();
			//System.out.println(colCount + " "+ rowCount);
			StringBuffer reader = new StringBuffer();
			for(int x=0;x<colCount;x++){
				reader.append(content.getColumnName(x));
				//System.out.print(content.getColumnName(x));
				if (x!=colCount){
					reader.append(", ");
				}
			}
			//System.out.println("");
			writer.write(reader.toString()+ "\r\n");
			for(int y=0;y<rowCount;y++){
				reader = new StringBuffer();
				for(int x=0;x<colCount;x++){
					reader.append(content.getValueAt(y,x));
					//System.out.print(content.getValueAt(y,x));
					if (x!=colCount){
						reader.append(", ");
					}
				}
				writer.write(reader.toString()+ "\r\n");
				//System.out.println("");
			}
			 writer.flush();
			 writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public LayerManager getLayerManager(){
		return targetDatabase.getLayermanager();
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
	
	public DefinedShapeAttributes getPrimaryColor(){
		return new DefinedShapeAttributes(priColVar, thickVar, opacityVar);
	}
	
	public DefinedShapeAttributes getSecondaryColor(){
		return new DefinedShapeAttributes(secColVar, thickVar, opacityVar);
	}
	
	public Boolean getDraw(){
		return drawVar;
	}
	
	public Boolean getZoom(){
		return zoomVar;
	}
	
	public Boolean getPrint(){
		return printVar;
	}
	
	public Boolean getPrintOrientation(){
		return orientVar;
	}
	
	public void setMode(int modes){
		InputAgent.processEntity_Keyword_Value(this, mode, Integer.toString(modes));
	}
	
	public void setRadius(int radiuses){
		InputAgent.processEntity_Keyword_Value(this, radius, Integer.toString(radiuses));
	}
	
	public void setDraw(boolean draws){
		drawVar=draws;
	}
	
	public void setPrint(boolean prints){
		printVar=prints;
	}
	
	public void setZoom(boolean zooms){
		zoomVar=zooms;
	}
	
	public void setOpacity(double opacities){
		opacityVar=opacities;
	}
	
	public void setThickness(int thicknesses){
		thickVar=thicknesses;
	}
	
	public void setPrimaryColor(Color4d colors){
		priColVar=colors;
	}
	
	public void setSecondaryColor(Color4d colors){
		secColVar=colors;
	}
	
	//change the default statement in this object
	public void setStatement(String statements){
		InputAgent_Rolos.processEntity_Keyword_Value(this, statement, statements);
	}

	public void setTable(String tables){
		InputAgent_Rolos.processEntity_Keyword_Value(this, table, tables);
	}
	
	public void setColumn(String columns){
		InputAgent_Rolos.processEntity_Keyword_Value(this, column, columns);
	}
	
	public void setOperator(String operators){
		InputAgent_Rolos.processEntity_Keyword_Value(this, operator, operators);
	}
	
	public void setRow(String rows){
		InputAgent_Rolos.processEntity_Keyword_Value(this, row, rows);
	}
}


