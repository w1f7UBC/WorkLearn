package DataBase;


import javax.swing.JOptionPane;



public class QueryStatement  {
  
  
  // statement = "SELECT * FROM testproto";
  public String target;
  public String tablename;
  
  public String Num;
  public String statement = "SELECT " + target +" FROM "+ tablename;

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
	  
  

