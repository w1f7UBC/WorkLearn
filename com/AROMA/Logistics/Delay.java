package com.AROMA.Logistics;

import com.jaamsim.Samples.SampleInput;
import com.jaamsim.units.TimeUnit;

import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class Delay extends DisplayEntity {
	
	@Keyword(description = "delay duration can be a single value, list of values, .",
	         example = "Loader1PreLoadingDelay  Duration { 2 h }")
	private final SampleInput delayDuration;
	
	@Keyword(description = "The list of entities that would triger this delay. "
			+ "if triggering entity not found in the defined groups will return 0. If triggering entities are not defined will return the set value for every entity",
	         example = "Loader1PreLoadingDelay DelayedEntities { AllTrucks }")
	private final EntityListInput<LogisticsEntity> trigeringEntityList;
		
	{
		delayDuration = new SampleInput("DelayDuration", "Key Inputs", null);
		delayDuration.setUnitType(TimeUnit.class);
		this.addInput(delayDuration);
		
		trigeringEntityList = new EntityListInput<>(LogisticsEntity.class, "DelayedEntities", "Key Inputs", null);
		this.addInput(trigeringEntityList);
	}
	
	public Delay() {	}
	
	
	@Override
	public void validate() {
		// TODO Auto-generated method stub
		super.validate();

		if(delayDuration.getValue() == null )
				throw new ErrorException("DelayDuratin - %s should be set", this.getName());
	}
	
	@Override
	public void earlyInit() {
		// TODO Auto-generated method stub
		super.earlyInit();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	//GETTER AND SETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * @return delay duration in hours. 
	 *  if a group of entities are passed, then it is assumed that the passed entities are arguments and hence all 
	 *  passed entities should be present in a group of triggering entities to return the corresponding value or will return 0.
	 */
	public <T extends LogisticsEntity> double getNextDelayDuration(T ... entities){
		// find the index of the entities, if a list that contains all the entities is not found return 0
		if (trigeringEntityList.getValue() != null){
			for (T eachEntity : entities) {
					if (!trigeringEntityList.getValue().contains(eachEntity))
						return 0;
			}
		}
		return delayDuration.getValue().getNextSample(getSimTime());
	}
	
}
