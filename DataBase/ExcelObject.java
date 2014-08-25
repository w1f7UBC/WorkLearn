package DataBase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.StringInput;


public class ExcelObject extends Entity {
    private StringInput directory;
    private StringInput sheetname;
    private static final String USER_HOME = System.getProperty("user.home");
    public ExcelObject(){
    	
    }
    
    {
    	directory = new StringInput("directory","DataBase Properties",USER_HOME + "/Desktop/test.xls");
        this.addInput(directory);
        sheetname = new StringInput("sheetname","DataBase Properties","");
        this.addInput(sheetname);
    }
    public HSSFWorkbook returnWorkbook() throws IOException{
	    FileInputStream file = new FileInputStream(new File(directory.getValue()));
	    HSSFWorkbook workbook = new HSSFWorkbook(file);
    	return workbook;
    	
    } 
    public HSSFSheet returnSheet() throws IOException {  
    	HSSFSheet sheet = returnWorkbook().getSheetAt(0);
        return sheet;
    	
    }
    /*public static final void main(String[] 
    		args) {

    	try {
    	    System.out.println(USER_HOME);
    	    FileInputStream file = new FileInputStream(new File(USER_HOME + "/Desktop/test.xls"));
    	     
    	    //Get the workbook instance for XLS file 
    	    HSSFWorkbook workbook = new HSSFWorkbook(file);
    	 
    	    //Get first sheet from the workbook
    	    HSSFSheet sheet = workbook.getSheetAt(0);
    	     
    	    //Iterate through each rows from first sheet
    	    Iterator<Row> rowIterator = sheet.iterator();
    	    while(rowIterator.hasNext()) {
    	        Row row = rowIterator.next();
    	         
    	        //For each row, iterate through each columns
    	        Iterator<Cell> cellIterator = row.cellIterator();
    	        while(cellIterator.hasNext()) {
    	             
    	            Cell cell = cellIterator.next();
    	             
    	            switch(cell.getCellType()) {
    	                case Cell.CELL_TYPE_BOOLEAN:
    	                    System.out.print(cell.getBooleanCellValue() + "\t\t");
    	                    break;
    	                case Cell.CELL_TYPE_NUMERIC:
    	                    System.out.print(cell.getNumericCellValue() + "\t\t");
    	                    break;
    	                case Cell.CELL_TYPE_STRING:
    	                    System.out.print(cell.getStringCellValue() + "\t\t");
    	                    break;
    	            }
    	        }
    	        System.out.println("");
    	    }
    	    file.close();
    	     
    	} catch (FileNotFoundException e) {
    	    e.printStackTrace();
    	} catch (IOException e) {
    	    e.printStackTrace();
    	}
    	*/
    		}

