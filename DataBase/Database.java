package DataBase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

public class Database {
	
	public static ArrayList<Connection> listOfConnection = new ArrayList<>(1);

	public synchronized static int Connect(DataBaseObject db){	
		
		Connection conn;
		try {
			conn = DriverManager.getConnection(db.getURL(),db.getProperties());
			listOfConnection.add(conn);
			return listOfConnection.size()-1;
		} catch (SQLException e) {
			System.out.println(e);
		}
		return listOfConnection.size();
	}
	
	public static Connection getConnection(int connectionIndex){
		return listOfConnection.get(connectionIndex);
	}
	
}
