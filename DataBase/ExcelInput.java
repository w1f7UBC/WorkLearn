package DataBase;

import java.io.IOException;


import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

public class ExcelInput extends ExcelObject{
public ExcelInput(){
	
}
private int findRow(String cellContent) throws IOException {
    for (Row row : returnSheet()) {
        for (Cell cell : row) {
            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                if (cell.getRichStringCellValue().getString().trim().equals(cellContent)) {
                    return row.getRowNum();  
                }
            }
        }
    }               
    return 0;
	}

private Row returnRow(String CellContent) throws IOException{
	Row row = returnSheet().getRow(findRow(CellContent));
	return row;
	}
private void rowToAttribute(String CellContent) throws IOException{
    Iterator<Cell> cellIterator = returnRow(CellContent).cellIterator();
    while(cellIterator.hasNext()) {
        Cell cell = cellIterator.next();
			}
		}
}