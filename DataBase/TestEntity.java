package DataBase;

import java.util.ArrayList;

import com.AROMA.AROMAEntity;
import com.jaamsim.input.KeyInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.sandwell.JavaSimulation.Entity;


public class TestEntity extends Entity {
	
	@Keyword (description = "test")
	private KeyInput test;
	//@Keyword (description = "test")
	//private OneOrTwoKeyInput moretests; 
    
	{
		 test = new KeyInput(null, Double.class, "a", "TESTS", 1.0);
		 this.addInput(test);
		 //moretests = new OneOrTwoKeyInput(null, null, null, "moretests", "TESTS", "default");
		 //this.addInput(moretests);
	}
	
    public TestEntity(){
    }
}
