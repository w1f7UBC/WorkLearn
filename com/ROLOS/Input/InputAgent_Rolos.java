package com.ROLOS.Input;

import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.sandwell.JavaSimulation.Entity;

public final class InputAgent_Rolos extends InputAgent {
	
	/**
	 * Prepares the keyword and input value for processing.
	 *
	 * @param ent - the entity whose keyword and value have been entered.
	 * @param in - the input object for the keyword.
	 * @param value - the input value String for the keyword.
	 */
	public static void processEntity_Keyword_Value(Entity ent, Input<?> in, String value){
		processEntity_Keyword_Value(ent, in.getKeyword(), value);
	}


}
