package DataBase;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.xml.crypto.Data;

import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation3D.DisplayEntity;



public class DataBase<T> extends Input<T>{
	 
	 public DataBase(String key, String cat, T def) {
		super(key, cat, def);
		// TODO Auto-generated constructor stub
	}
	 	

	{

	}

	private static String s = "SELECT A.grid_Code, B.ecozone2, B.stid2, B.curvtype2 "
			+ "FROM saeed_test A,saeed_gy B "
			+ "WHERE A.grid_code = B.grid_Code "
			+ "AND A.point_x=-96.2293862010 "
			+ "AND A.point_y=56.7500090970 "
			+ "ORDER BY A.grid_Code ASC";
	//public static String url = "jdbc:postgresql://25.141.219.39:5432/fom";
    public static  Properties props = new Properties();
    public static String Name;
    public static String[] names;
    public static ArrayList<String> Names ;
	private static String a;
	public static void loadDriver() throws ClassNotFoundException {
	try {
		Class.forName("org.postgresql.Driver");
	}
	catch(ClassNotFoundException e){
		System.out.print("Driver error");
	}

	}
	/*public static void Connection() throws SQLException{
		props.setProperty("user","sde");
		
	    props.setProperty("password","Fomsummer2014");
	    Connection conn = DriverManager.getConnection(url,props);

	    System.out.println("try to connect");

	}
	    public static void testStatement() throws SQLException{

		 Connection conn = DriverManager.getConnection(url,props);

		    Statement st = conn.createStatement();
	    	ResultSet rs = st.executeQuery("SELECT * FROM testproto");
	    	while (rs.next())
	    	{
	    	   Name = rs.getString(1);
	    	   System.out.println(Name);
	    	   String ad ="AttributeDefinitionList"+"{ "+rs.getString(2)+" }";
	    	   String position ="Position"+"{ "+ rs.getArray(3)+" }";
	    	   String allignment ="Alignment"+"{ "+rs.getArray(4)+" }";
	    	   String dm = "DisplayModel"+"{ "+rs.getString(5)+" }";
	 	       Entity ent1 = InputAgent.defineEntity(DisplayEntity.class, Name, rs.next());
	 	       String cat ="Basic Graphics";
	 	       ArrayList<String> kw = new ArrayList<String>() ;

	 	       ParseContext pc= new ParseContext();
	 	       kw.add(ad);
	 	       kw.add(position);
	 	       kw.add(allignment);
	 	       kw.add(dm);
	 	       KeywordIndex kwi= new KeywordIndex(kw,cat, 0,4,pc);
	 	       Input<?> in = ent1.getInput(kwi.keyword);
	 	      
	 	     InputAgent.apply(ent1, in, kwi); 
	    			   //Input.Input("DisplayModel",cat,rs.getString(5));
	 	        //  Input("DisplayModel",cat,rs.getString(5));
	    	  // ent1.updateForInput(in);
	 	      ent1.copyInputs(ent1);
	 //	       InputAgent.apply(ent1,kw);
	 	       System.out.println(ad);
	    	   System.out.println(position);
	    	   System.out.println(allignment);
	    	   System.out.println(dm);
               System.out.println();
	    	  // System.out.print(Entity.getNamedentities());
	    	}
	    	rs.close();
	    	st.close();
	 }
*/
 /*  public static ResultSet runSQL(String s) throws SQLException{
	     props.setProperty("user","sde");
		
	    props.setProperty("password","Fomsummer2014");
	     String url = DataObject.getURL();
	    Connection conn = DriverManager.getConnection(url,props);

	    System.out.println("try to connect"); 
	          boolean last;     
	           Integer i = 1;
	        
            Names = new ArrayList<String>();     
	        QueryStatement qs = new QueryStatement();
        //    Connection conn = DriverManager.getConnection(url,props);

		    Statement st = conn.createStatement();
		//    qs.setStatement();
		    System.out.println(s);
	    	
		    ResultSet rs = st.executeQuery(s);
	    	
		  //  while (rs.next()){
	    //	System.out.print(rs.getString(1)+" "+rs.getString(2));
		    
	    //	ResultSetMetaData rsmd = rs.getMetaData();
	    	/* System.out.print(rsmd.getColumnCount());
	    		while (rs.next()){
	    			int column = 1;
	    			while (column < rsmd.getColumnCount()){
	    				System.out.print(rs.getString(column));	
	    				column++;
	    			}
	    			
	    		}
	    	*/
		//    }
	//		return rs;
   //}
	    		/*while(){	
	    	     
	    		 Name =	Name + rs.getString(i);
	    		 
	    		 Names.iterator()
	    	     
	    		}
	    	   Names.add(Name);
	    	   System.out.println(Names);
	    	}
	    		
   */
   private static DefaultTableModel DisplayTable() throws SQLException{
	   
	    ResultSet rs= QueryObject.runSQL();
		ResultSetMetaData metaData = rs.getMetaData();

	    // names of columns
	    Vector<String> columnNames = new Vector<String>();
	    ArrayList<Integer> index = new ArrayList<Integer>();
	    String ageIndex = "5";
	    int columnCount = metaData.getColumnCount();
	    ArrayList<Integer> index2 = new ArrayList<Integer>();
	    
	    for (int column = 1; column <= columnCount; column++) { 
	    	
	    	if(metaData.getColumnName(column).contains("grid_code")||metaData.getColumnName(column).contains("ecozone2")||metaData.getColumnName(column).contains("stid2")||metaData.getColumnName(column).contains("si2")||metaData.getColumnName(column).contains("age")){	    		
	    		System.out.println(metaData.getColumnName(column));
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
	     //       Vector<Object> c = data.get(1);
	        }
	        data.add(vector);
	        
	       } 
	    String addCol = "vol"+"100"+"2";
	    String addCol2 = "bio"+"100"+"2";
	    columnNames.add(addCol);
	    columnNames.add(addCol2);
	    return new DefaultTableModel(data, columnNames);

	}
		
   public static void Poptable() throws SQLException{
	   JTable table = new JTable(DisplayTable());
	   JOptionPane.showMessageDialog(null, new JScrollPane(table));
	   
   }
	public static void test() throws SQLException{
    	try{ 
    	//	DataObject d = new DataObject();
    		
  //  		Connection();
    		
    	/*	runSQL("SELECT A.grid_Code, B.ecozone2, B.stid2, B.curvtype2 "
    				+ "FROM saeed_test A,saeed_gy B "
    				+ "WHERE A.grid_code = B.grid_Code "
    				+ "AND A.point_x=-96.2293862010 "
    				+ "AND A.point_y=56.7500090970 "
    				+ "ORDER BY A.grid_Code ASC");
    				*/
    		//Poptable();
    		//runSQL();
   // 		testStatement();
    	Poptable();
    	}catch(SQLException e)
    	{
    		System.out.println(e);
		System.exit(1);

	}

    }
	
	@Override
	public void parse(KeywordIndex kw)  {
		// TODO Auto-generated method stub
	}
	public static DefaultTableModel testing() throws SQLException { 
		     
		props.setProperty("user","sde");
			
		    props.setProperty("password","Fomsummer2014");
		    
		    Connection conn = DriverManager.getConnection("jdbc:postgresql://25.141.219.39:5432/fom",props);
	         
		    System.out.println("try to connect");
		  
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery("SELECT table_schema,table_name "
				+ "FROM information_schema.tables "
				+ "ORDER BY table_schema,table_name;");
	    System.out.println("query sent");

	//	DatabaseMetaData md = conn.getMetaData();
	//	ResultSet rs = md.getTables(null, null, "%", null);
		ResultSetMetaData metaData1 = rs.getMetaData();

	    int columnCount = metaData1.getColumnCount();

			   

		    // names of columns
		    Vector<String> columnNames = new Vector<String>();
		    columnNames.add("Database");
		    	
		        
		    	
		      
		    // data of the table
		    Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		    
		    while (rs.next()){
		    	
		    	
		    	Vector<Object> vector = new Vector<Object>();
		        

		        
		        for (int i = 1;i<=columnCount;i++) {

		                   vector.add(rs.getObject(i));

		        
		        data.add(vector);
		        
		       } 
		    
			  

		}
		/*ResultSet rs = st.executeQuery("SELECT * "
    			+ "FROM saeed_test "
    			+ "WHERE point_x <= -96.23 AND point_y <= 56.74 "
    			+ "ORDER BY point_x DESC, point_y DESC "
    			+ "LIMIT 1;"
    			+ "SELECT  point_x, point_y"
    			+ "FROM saeed_test "
    			+ "WHERE point_x <= -96.23 AND point_y <= 56.74"
    			+ "ORDER BY point_y DESC, point_x DESC "
    			+ "LIMIT 1;");
		/*ResultSet rs = st.executeQuery("SELECT gid, point_x, point_y "
	    			+ "FROM saeed_test "
	    			+ "WHERE" +point_x+" <= -96.23 AND "+point_y+" <= 56.74 "
	    			+ "ORDER BY point_x DESC, point_y DESC "
	    			+ "LIMIT 1;"
	    			+ "SELECT gid, point_x, point_y"
	    			+ "FROM saeed_test "
	    			+ "WHERE"+point_x+" <= -96.23 AND "+point_y+" <= 56.74"
	    			+ "ORDER BY point_y DESC, point_x DESC "
	    			+ "LIMIT 1;");
	    			*/
	    	while (rs.next()){
	    	System.out.println(rs.getString(1)+" "+rs.getString(2)+" "+rs.getString(3));
	    	}
	    	  return new DefaultTableModel(data, columnNames);
	}

	 public static void Poptabletest() throws SQLException{
		   JTable table = new JTable(testing());
		   JOptionPane.showMessageDialog(null, new JScrollPane(table));
		   
	   }
	
}




