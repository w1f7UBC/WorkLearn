package DataBase;

import java.io.IOException;
import java.util.Iterator;

import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.formula.functions.Column;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.jaamsim.input.InputAgent;
import com.sandwell.JavaSimulation.Entity;

public class ExcelInput extends ExcelObject {
	public ExcelInput() {

	}

	private int findRow(String cellContent) throws IOException {
		for (Row row : returnSheet()) {
			for (Cell cell : row) {
				if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
					if (cell.getRichStringCellValue().getString().trim()
							.equals(cellContent)) {
						return row.getRowNum();
					}
				}
			}
		}
		return 0;
	}

	private Row returnRow(String CellContent) throws IOException {
		Row row = returnSheet().getRow(findRow(CellContent));
		return row;
	}

	private void rowToAttribute(String CellContent) throws IOException {
		Iterator<Cell> cellIterator = returnRow(CellContent).cellIterator();
		while (cellIterator.hasNext()) {
			Cell cell = cellIterator.next();
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
		
		InputAgent.processEntity_Keyword_Value(this, getInput(getKeyword(cellContent)), cellContent);
	}

	private Entity findEnt(String entName) {
		
		return this.getNamedEntity(entName);
	}
}
