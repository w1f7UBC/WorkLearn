package DataBase;


import javax.swing.JOptionPane;



public class QueryStatement  {
  
  
  // statement = "SELECT * FROM testproto";
  public String target;
  public String tablename;
  
  public String Num;
  public String statement = "SELECT A.grid_Code, B.ecozone2, B.stid2, B.curvtype2 "
			+ "FROM saeed_test A,saeed_gy B "
			+ "WHERE A.grid_code = B.grid_Code "
			+ "AND A.point_x=-96.2293862010 "
			+ "AND A.point_y=56.7500090970 "
			+ "ORDER BY A.grid_Code ASC";
		  //"SELECT " + target +" FROM "+ tablename;

  public QueryStatement(){
	}
  public String getStatement(){
	  
	  return statement;
  }
  public void setStatement(){
	  target = JOptionPane.showInputDialog("Target");
	  tablename = JOptionPane.showInputDialog("Database tablename:");
	  Num = JOptionPane.showInputDialog("Number of rows showing");
	  statement = "SELECT " + target +" FROM "+ tablename+" LIMIT "+ Num + "ORDER BY objectid";
  }
	  
}
	  
  

