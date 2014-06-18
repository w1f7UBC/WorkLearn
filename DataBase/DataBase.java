package DataBase;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.DisplayEntity;



public class DataBase<T> extends Input<T>{

	 public DataBase(String key, String cat, T def) {
		super(key, cat, def);
		// TODO Auto-generated constructor stub
	}


	{

	}


	public static String url = "jdbc:postgresql://25.186.195.33:5432/fom";
    public static Properties props = new Properties();
    public static String Name;
    public static String[] names;
	public static void loadDriver() throws ClassNotFoundException {
	try {
		Class.forName("org.postgresql.Driver");
	}
	catch(ClassNotFoundException e){
		System.out.print("Driver error");
	}

	}
	public static void Connection() throws SQLException{
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
	 	       KeywordIndex kwi= new KeywordIndex(kw,0,4,pc);
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


    public static void test() throws SQLException{
    	try{
    		Connection();
    		testStatement();
    	}catch(SQLException e)
    	{
    		System.out.println("SQLException!");
		System.exit(1);

	}

    }
}