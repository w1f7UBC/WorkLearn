package DataBase;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.StringListInput;
import com.sandwell.JavaSimulation.Vec3dInput;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public  class QueryObject extends AbstractQuery {
private static String s = "SELECT A.grid_Code, B.ecozone2, B.stid2, B.curvtype2 "
		+ "FROM saeed_test A,saeed_gy B "
		+ "WHERE A.grid_code = B.grid_Code "
		+ "AND A.point_x=-96.2293862010 "
		+ "AND A.point_y=56.7500090970 "
		+ "ORDER BY A.grid_Code ASC";
private ArrayList<String> targets;
private ArrayList<String> tablenames;

@Keyword(description = "display targets of the query")
private  StringListInput target;
@Keyword(description = "tables to select from")
private  StringListInput tablename;
@Keyword(description = "Gerometric parameters")
private  StringListInput coordinate;

{ 
	target = new StringListInput("target","Query property", targets);
	tablename = new StringListInput("tablename","Query property",tablenames);
	coordinate = new StringListInput("coordinates","Query property",getCoordinates());
	}


private void StatementBuild(){
	
	setStatement(s);
	}

private DefaultTableModel DisplayTable() throws SQLException{
	ResultSet rs= DataObject.runQuery(getStatement());
	ResultSetMetaData metaData = rs.getMetaData();

    // names of columns
    Vector<String> columnNames = new Vector<String>();
    int columnCount = metaData.getColumnCount();
    for (int column = 1; column <= columnCount; column++) {
        columnNames.add(metaData.getColumnName(column));
    }

    // data of the table
    Vector<Vector<Object>> data = new Vector<Vector<Object>>();
    while (rs.next()) {
        Vector<Object> vector = new Vector<Object>();
        for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
            vector.add(rs.getObject(columnIndex));
        }
        data.add(vector);
    }

    return new DefaultTableModel(data, columnNames);

}
   public  void Poptable() throws SQLException{
	   JTable table = new JTable(DisplayTable());
	   JOptionPane.showMessageDialog(null, new JScrollPane(table));
	   
}
	}

//constants for sql statment



//@Query (Desccription £º
//		Arguments:  )
//Public String getInventory£¨DatabaseObject dataBase£©{
//	statement = "SELECT " + vol1002,ss,sse +" FROM "+ dataBase.getName() +" WHERE  "+ Num + "ORDER BY objectid";

	

