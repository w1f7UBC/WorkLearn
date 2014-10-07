package DataBase;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.ROLOS.Input.InputAgent_Rolos;
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
 		InputAgent_Rolos.processEntity_Keyword_Value(findEnt(name),keyword,value); 	
 			}	
 		}
 	}
 	
	private String findKeyword(String cellContent) throws IOException {
		String keywordposition = "";
		for (Row row : returnSheet()) {
			for (Cell cell : row) {
				if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
					if (cell.getRichStringCellValue().getString().trim()
							.equals(cellContent)) {
						keywordposition = "1" + cell.getColumnIndex();
					}
				}
			}
		}
		return keywordposition;
	}

	private String getKeyword(String cellContent) throws IOException {
		CellReference ref = new CellReference(findKeyword(cellContent));
		Row r = returnSheet().getRow(ref.getRow());
		Cell c = r.getCell(ref.getCol());
		String keyword = c.getStringCellValue();

		return keyword;
	}

	private void processAttribute(String cellContent, String entName)
			throws IOException {
		
		InputAgent_Rolos.processEntity_Keyword_Value(this, getInput(getKeyword(cellContent)), cellContent);
	}

	private Entity findEnt(String entName) {
		
		return this.getNamedEntity(entName);
	}
}
