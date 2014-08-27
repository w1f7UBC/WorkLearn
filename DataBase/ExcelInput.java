package DataBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.formula.functions.Column;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;

import com.jaamsim.input.InputAgent;
import com.sandwell.JavaSimulation.Entity;

	public class ExcelInput extends ExcelObject{
	public ExcelInput(){
	
}   
	public ArrayList<String> NameList;
	

	public Row findKeywordRow() throws IOException{
		Row keywordRow = returnSheet().getRow(0);
		return keywordRow;
	}
	private int findTagetRow(String entName) throws IOException {
    for (Row row : returnSheet()) {
        for (Cell cell : row) {
            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                if (cell.getRichStringCellValue().getString().trim().equals(entName)) {
                    return row.getRowNum();  
                }
            }
        }
    }               
    return 0;
	}
    private int findNameCol() throws IOException{
    	
        for (Row row : returnSheet()) {
        	for (Cell cell : row) {
        		cell.setCellType(Cell.CELL_TYPE_STRING);
        		if(cell.getRichStringCellValue().getString().trim().contains("name")){
        			return cell.getColumnIndex(); 
        			
        		}
        	} 
        }
		return 0;
    }
    private ArrayList<String> findEntityName() throws IOException{
    	 for (int j=0; j< returnSheet().getLastRowNum() + 1; j++) {
    	        Row row = returnSheet().getRow(j);
    	        Cell cell = row.getCell(findNameCol()); //get first cell
    	        cell.setCellType(Cell.CELL_TYPE_STRING);
    	        NameList.add(cell.getStringCellValue());
    	 }
    	return NameList;
    }
	private Row returnRow(String entName) throws IOException{
	Row row = returnSheet().getRow(findTagetRow(entName));
	return row;
	}
	
    
 
 	public void processAttribute() throws IOException{
 		
 		
 		findNameCol();
 		findEntityName();
 		for(String name: NameList){
 		Iterator<Cell> valueIterator = returnRow(name).cellIterator();
 		Iterator<Cell> keywordIterator = findKeywordRow().cellIterator();
 		
 		while(valueIterator.hasNext()){
 	    Cell keywordcell = keywordIterator.next();
 	    keywordcell.setCellType(Cell.CELL_TYPE_STRING);
 	    Cell valuecell = valueIterator.next();
 	    valuecell.setCellType(Cell.CELL_TYPE_STRING);
 	    valuecell.setCellType(Cell.CELL_TYPE_STRING);
 		String	keyword = keywordcell.getStringCellValue();
 		String	value =valuecell.getStringCellValue();
 		InputAgent.processEntity_Keyword_Value(findEnt(name),keyword,value); 	
 			}	
 		}
 	}
 	private Entity findEnt(String entName){
 		
 		return getNamedEntity(entName);
 	}
	}
