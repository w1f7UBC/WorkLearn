/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.ROLOS.Logistics;

import java.util.ArrayList;

import com.ROLOS.Logistics.LogisticsEntity;
import com.jaamsim.math.Color4d;
import com.sandwell.JavaSimulation.BooleanListInput;
import com.sandwell.JavaSimulation.BooleanVector;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.DoubleListInput;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.ErrorException;
import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.input.Input;
import com.sandwell.JavaSimulation.IntegerVector;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Tester;

/**
 * Class ModelEntity - JavaSimulation3D
 */
public class ModelEntity extends LogisticsEntity {

	// Breakdowns

	@Keyword(description = "Reliability is defined as:\n" +
	                " 100% - (plant breakdown time / total operation time)\n " +
	                "or\n " +
	                "(Operational Time)/(Breakdown + Operational Time)",
	         example = "Object1 Reliability { 0.95 }")
	private final DoubleInput availability;
	protected double hoursForNextFailure;    // The number of working hours required before the next breakdown
	protected double iATFailure;             // inter arrival time between failures
	protected double iATFailureFactor;       // factor applied to time between failures to get desired availability
	protected boolean breakdownPending;          // true when a breakdown is to occur
	protected boolean brokendown;                // true => entity is presently broken down
	protected boolean maintenance;               // true => entity is presently in maintenance
	protected boolean associatedBreakdown;       // true => entity is presently in Associated Breakdown
	protected boolean associatedMaintenance;     // true => entity is presently in Associated Maintenance
	protected double breakdownStartTime;         // Start time of the most recent breakdown
	protected double breakdownEndTime;           // End time of the most recent breakdown

	// Breakdown Probability Distributions
	@Keyword(description = "A ProbabilityDistribution object that governs the duration of breakdowns (in hours).",
	         example = "Object1  DowntimeDurationDistribution { BreakdownProbDist1 }")
	private final SampleExpInput downtimeDurationDistribution;

	@Keyword(description = "A ProbabilityDistribution object that governs when breakdowns occur (in hours).",
	         example = "Object1  DowntimeIATDistribution { BreakdownProbDist1 }")
	private final SampleExpInput downtimeIATDistribution;

	// Maintenance

	@Keyword(description = "The simulation time for the start of the first maintenance for each maintenance cycle.",
	         example = "Object1 FirstMaintenanceTime { 24 h }")
	protected DoubleListInput firstMaintenanceTimes;

	@Keyword(description = "The time between maintenance activities for each maintenance cycle",
	         example = "Object1 MaintenanceInterval { 168 h }")
	protected DoubleListInput maintenanceIntervals;

	@Keyword(description = "The durations of a single maintenance event for each maintenance cycle.",
	         example = "Object1 MaintenanceDuration { 336 h }")
	protected DoubleListInput maintenanceDurations;
	protected IntegerVector maintenancePendings;  // Number of maintenance periods that are due

	@Keyword(description = "A Boolean value. Allows scheduled maintenances to be skipped if it overlaps " +
	                "with another planned maintenance event.",
	         example = "Object1 SkipMaintenanceIfOverlap { TRUE }")
	protected BooleanListInput skipMaintenanceIfOverlap;

	@Keyword(description = "A list of objects that share the maintenance schedule with this object. " +
	                "In order for the maintenance to start, all objects on this list must be available." +
	                "This keyword is for Handlers and Signal Blocks only.",
	         example = "Block1 SharedMaintenance { Block2 Block2 }")
	private final EntityListInput<ModelEntity> sharedMaintenanceList;
	protected ModelEntity masterMaintenanceEntity;  // The entity that has maintenance information

	protected boolean performMaintenanceAfterShipDelayPending;			// maintenance needs to be done after shipDelay

	// Maintenance based on hours of operations

	@Keyword(description = "Working time for the start of the first maintenance for each maintenance cycle",
	         example = "Object1 FirstMaintenanceOperatingHours { 1000 2500 h }")
	private final DoubleListInput firstMaintenanceOperatingHours;

	@Keyword(description = "Working time between one maintenance event and the next for each maintenance cycle",
	         example = "Object1 MaintenanceOperatingHoursIntervals { 2000 5000 h }")
	private final DoubleListInput maintenanceOperatingHoursIntervals;

	@Keyword(description = "Duration of maintenance events based on working hours for each maintenance cycle",
	         example = "Ship1 MaintenanceOperatingHoursDurations { 24 48 h }")
	private final DoubleListInput maintenanceOperatingHoursDurations;
	protected IntegerVector maintenanceOperatingHoursPendings;  // Number of maintenance periods that are due
	protected DoubleVector hoursForNextMaintenanceOperatingHours;

	protected double maintenanceStartTime; // Start time of the most recent maintenance
	protected double maintenanceEndTime; // End time of the most recent maintenance
	protected DoubleVector nextMaintenanceTimes; // next start time for each maintenance
	protected double nextMaintenanceDuration; // duration for next maintenance
	protected DoubleVector lastScheduledMaintenanceTimes;

	@Keyword(description = "If maintenance has been deferred by the DeferMaintenanceLookAhead keyword " +
	                "for longer than this time, the maintenance will start even if " +
	                "there is an object within the lookahead. There must be one entry for each " +
	                "defined maintenance schedule if DeferMaintenanceLookAhead is used.  This" +
	                "keyword is only used for signal blocks.",
	         example = "Object1 DeferMaintenanceLimit { 50 50 h }")
	private final DoubleListInput deferMaintenanceLimit;


	@Keyword(description = "If the duration of the downtime is longer than this time, equipment will be released",
	         example = "Object1 DowntimeToReleaseEquipment { 1.0 h }")
	protected final DoubleInput downtimeToReleaseEquipment;

	@Keyword(description = "A list of Boolean values corresponding to the maintenance cycles. If a value is TRUE, " +
	                "then routes/tasks are released before performing the maintenance in the cycle.",
	         example = "Object1 ReleaseEquipment { TRUE FALSE FALSE }")
	protected final BooleanListInput releaseEquipment;

	@Keyword(description = "A list of Boolean values corresponding to the maintenance cycles. If a value is " +
	                "TRUE, then maintenance in the cycle can start even if the equipment is presently " +
	                "working.",
	         example = "Object1 ForceMaintenance { TRUE FALSE FALSE }")
	protected final BooleanListInput forceMaintenance;

	// Graphics
	protected final static Color4d breakdownColor = ColourInput.DARK_RED; // Color of the entity in breaking down
	protected final static Color4d maintenanceColor = ColourInput.RED; // Color of the entity in maintenance

	{
		maintenanceDurations = new DoubleListInput("MaintenanceDurations", "Maintenance", new DoubleVector());
		maintenanceDurations.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		maintenanceDurations.setUnits("h");
		this.addInput(maintenanceDurations);

		maintenanceIntervals = new DoubleListInput("MaintenanceIntervals", "Maintenance", new DoubleVector());
		maintenanceIntervals.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		maintenanceIntervals.setUnits("h");
		this.addInput(maintenanceIntervals);

		firstMaintenanceTimes = new DoubleListInput("FirstMaintenanceTimes", "Maintenance", new DoubleVector());
		firstMaintenanceTimes.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		firstMaintenanceTimes.setUnits("h");
		this.addInput(firstMaintenanceTimes);

		forceMaintenance = new BooleanListInput("ForceMaintenance", "Maintenance", null);
		this.addInput(forceMaintenance);

		releaseEquipment = new BooleanListInput("ReleaseEquipment", "Maintenance", null);
		this.addInput(releaseEquipment);

		availability = new DoubleInput("Reliability", "Breakdowns", 1.0d);
		availability.setValidRange(0.0d, 1.0d);
		this.addInput(availability);

		downtimeIATDistribution = new SampleExpInput("DowntimeIATDistribution", "Breakdowns", null);
		this.addInput(downtimeIATDistribution);

		downtimeDurationDistribution = new SampleExpInput("DowntimeDurationDistribution", "Breakdowns", null);
		this.addInput(downtimeDurationDistribution);

		downtimeToReleaseEquipment = new DoubleInput("DowntimeToReleaseEquipment", "Breakdowns", 0.0d);
		downtimeToReleaseEquipment.setValidRange(0.0d,Double.POSITIVE_INFINITY );
		this.addInput(downtimeToReleaseEquipment);

		skipMaintenanceIfOverlap = new BooleanListInput("SkipMaintenanceIfOverlap", "Maintenance", new BooleanVector());
		this.addInput(skipMaintenanceIfOverlap);

		deferMaintenanceLimit = new DoubleListInput("DeferMaintenanceLimit", "Maintenance", null);
		deferMaintenanceLimit.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		deferMaintenanceLimit.setUnits("h");
		this.addInput(deferMaintenanceLimit);

		sharedMaintenanceList = new EntityListInput<ModelEntity>(ModelEntity.class, "SharedMaintenance", "Maintenance", new ArrayList<ModelEntity>(0));
		this.addInput(sharedMaintenanceList);

		firstMaintenanceOperatingHours = new DoubleListInput("FirstMaintenanceOperatingHours", "Maintenance", new DoubleVector());
		firstMaintenanceOperatingHours.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		firstMaintenanceOperatingHours.setUnits("h");
		this.addInput(firstMaintenanceOperatingHours);

		maintenanceOperatingHoursDurations = new DoubleListInput("MaintenanceOperatingHoursDurations", "Maintenance", new DoubleVector());
		maintenanceOperatingHoursDurations.setValidRange(1e-15, Double.POSITIVE_INFINITY);
		maintenanceOperatingHoursDurations.setUnits("h");
		this.addInput(maintenanceOperatingHoursDurations);

		maintenanceOperatingHoursIntervals = new DoubleListInput("MaintenanceOperatingHoursIntervals", "Maintenance", new DoubleVector());
		maintenanceOperatingHoursIntervals.setValidRange(1e-15, Double.POSITIVE_INFINITY);
		maintenanceOperatingHoursIntervals.setUnits("h");
		this.addInput(maintenanceOperatingHoursIntervals);

	}

	public ModelEntity() {
		super();
		lastHistogramUpdateTime = 0.0;
		secondToLastHistogramUpdateTime = 0.0;
		hoursForNextFailure = 0.0;
		iATFailure = 0.0;

		maintenancePendings = new IntegerVector( 1, 1 );

		maintenanceOperatingHoursPendings = new IntegerVector( 1, 1 );
		hoursForNextMaintenanceOperatingHours = new DoubleVector( 1, 1 );

		performMaintenanceAfterShipDelayPending = false;
		lastScheduledMaintenanceTimes = new DoubleVector();

		breakdownStartTime = 0.0;
		breakdownEndTime = Double.POSITIVE_INFINITY;
		breakdownPending = false;
		brokendown = false;
		associatedBreakdown = false;
		maintenanceStartTime = 0.0;
		maintenanceEndTime = Double.POSITIVE_INFINITY;
		maintenance = false;
		associatedMaintenance = false;
		
	}

	public void clearInternalProperties() {
		hoursForNextFailure = 0.0;
		performMaintenanceAfterShipDelayPending = false;
		breakdownPending = false;
		brokendown = false;
		associatedBreakdown = false;
		maintenance = false;
		associatedMaintenance = false;
	}

	@Override
	public void validate() {
		super.validate();
		this.validateMaintenance();
		Input.validateIndexedLists(firstMaintenanceOperatingHours.getValue(), maintenanceOperatingHoursIntervals.getValue(), "FirstMaintenanceOperatingHours", "MaintenanceOperatingHoursIntervals");
		Input.validateIndexedLists(firstMaintenanceOperatingHours.getValue(), maintenanceOperatingHoursDurations.getValue(), "FirstMaintenanceOperatingHours", "MaintenanceOperatingHoursDurations");

		if( getAvailability() < 1.0 ) {
			if( getDowntimeDurationDistribution(this.getSimTime()) == null ) {
				throw new ErrorException("When availability is less than one you must define downtimeDurationDistribution in your input file!");
			}
		}

		if( downtimeIATDistribution.getValue() != null ) {
			if( getDowntimeDurationDistribution(this.getSimTime()) == null ) {
				throw new ErrorException("When DowntimeIATDistribution is set, DowntimeDurationDistribution must also be set.");
			}
		}

		if( skipMaintenanceIfOverlap.getValue().size() > 0 )
			Input.validateIndexedLists(firstMaintenanceTimes.getValue(), skipMaintenanceIfOverlap.getValue(), "FirstMaintenanceTimes", "SkipMaintenanceIfOverlap");

		if( releaseEquipment.getValue() != null )
			Input.validateIndexedLists(firstMaintenanceTimes.getValue(), releaseEquipment.getValue(), "FirstMaintenanceTimes", "ReleaseEquipment");

		if( forceMaintenance.getValue() != null ) {
			Input.validateIndexedLists(firstMaintenanceTimes.getValue(), forceMaintenance.getValue(), "FirstMaintenanceTimes", "ForceMaintenance");
		}

		if(downtimeDurationDistribution.getValue() != null &&
		   downtimeDurationDistribution.getValue().getMinValue() < 0)
			throw new ErrorException("DowntimeDurationDistribution cannot allow negative values");

		if(downtimeIATDistribution.getValue() != null &&
		   downtimeIATDistribution.getValue().getMinValue() < 0)
			throw new ErrorException("DowntimeIATDistribution cannot allow negative values");
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		
		this.initialize();
		
/*// This block should be removed as initialize in the probability distribution entity is changed to earlyInit
		if( downtimeDurationDistribution.getValue() != null ) {
			downtimeDurationDistribution.getValue().initialize();
		}

		if( downtimeIATDistribution.getValue() != null ) {
			downtimeIATDistribution.getValue().initialize();
		}
		
		*/
	}

	// ******************************************************************************************************
	// INPUT
	// ******************************************************************************************************

	public void validateMaintenance() {
		Input.validateIndexedLists(firstMaintenanceTimes.getValue(), maintenanceIntervals.getValue(), "FirstMaintenanceTimes", "MaintenanceIntervals");
		Input.validateIndexedLists(firstMaintenanceTimes.getValue(), maintenanceDurations.getValue(), "FirstMaintenanceTimes", "MaintenanceDurations");

		for( int i = 0; i < maintenanceIntervals.getValue().size(); i++ ) {
			if( maintenanceIntervals.getValue().get( i ) < maintenanceDurations.getValue().get( i ) ) {
				throw new ErrorException("MaintenanceInterval should be greater than MaintenanceDuration  (%f) <= (%f)",
											  maintenanceIntervals.getValue().get(i), maintenanceDurations.getValue().get(i));
			}
		}
	}

	// ******************************************************************************************************
	// INITIALIZATION METHODS
	// ******************************************************************************************************

	public void clearStatistics() {

		for( int i = 0; i < getMaintenanceOperatingHoursIntervals().size(); i++ ) {
			hoursForNextMaintenanceOperatingHours.set( i, hoursForNextMaintenanceOperatingHours.get( i ) - this.getWorkingTime() );
		}

		// Determine the time for the first breakdown event
		/*if ( downtimeIATDistribution == null ) {
			if( breakdownSeed != 0 ) {
				breakdownRandGen.initialiseWith( breakdownSeed );
				hoursForNextFailure = breakdownRandGen.getUniformFrom_To( 0.5*iATFailure, 1.5*iATFailure );
			} else {
				hoursForNextFailure = getNextBreakdownIAT();
			}
		}
		 else {
			hoursForNextFailure = getNextBreakdownIAT();
		}*/
	}

	/**
	 * Initialize statistics
	 */
	public void initialize() {
		brokendown = false;
		maintenance = false;
		associatedBreakdown = false;
		associatedMaintenance = false;

		//  Calculate the average downtime duration if distributions are used
		double average = 0.0;
		if(getDowntimeDurationDistribution(this.getSimTime()) != null)
			average = downtimeDurationDistribution.getValue().getMeanValue(this.getSimTime());

		//  Calculate the average downtime inter-arrival time
		iATFailureFactor = 1.0;
		if( (getAvailability() == 1.0 || average == 0.0) ) {
			iATFailure = 10.0E10;
		}
		else {
			iATFailure = (average / (1.0 - getAvailability())) - average;
			if( getDowntimeIATDistribution(this.getSimTime()) != null ) {
				double expectedIATFailure = downtimeIATDistribution.getValue().getMeanValue(this.getSimTime());

				// Adjust the downtime inter-arrival time to get the specified availability
				  if( ! Tester.equalCheckTolerance( expectedIATFailure, ( (average / (1.0 - getAvailability())) - average ) ) ) {
					 iATFailureFactor = ( (average / (1.0 - getAvailability())) - average) / expectedIATFailure;        
				}
			}
		}

		// Determine the time for the first breakdown event
		hoursForNextFailure = getNextBreakdownIAT();

		this.setPresentState( "Idle");
		brokendown = false;

		//  Start the maintenance network
		if( firstMaintenanceTimes.getValue().size() != 0 ) {
			maintenancePendings.fillWithEntriesOf( firstMaintenanceTimes.getValue().size(), 0 );
			lastScheduledMaintenanceTimes.fillWithEntriesOf( firstMaintenanceTimes.getValue().size(), Double.POSITIVE_INFINITY );

			this.doMaintenanceNetwork();
		}

		// calculate hours for first operating hours breakdown
		for ( int i = 0; i < getMaintenanceOperatingHoursIntervals().size(); i++ ) {
			hoursForNextMaintenanceOperatingHours.add( firstMaintenanceOperatingHours.getValue().get( i ) );
			maintenanceOperatingHoursPendings.add( 0 );
		}
	}

	// ******************************************************************************************************
	// ACCESSOR METHODS
	// ******************************************************************************************************

	/**
	 * Return the time at which the most recent maintenance is scheduled to end
	 */
	public double getMaintenanceEndTime() {
		return maintenanceEndTime;
	}

	/**
	 * Return the time at which a the most recent breakdown is scheduled to end
	 */
	public double getBreakdownEndTime() {
		return breakdownEndTime;
	}

	/**
	 * Returns the availability proportion.
	 */
	public double getAvailability() {
		return availability.getValue();
	}

	public DoubleListInput getFirstMaintenanceTimes() {
		return firstMaintenanceTimes;
	}

	public boolean isBrokendown() {
		return brokendown;
	}

	public boolean isBreakdownPending() {
		return breakdownPending;
	}

	public boolean isInAssociatedBreakdown() {
		return associatedBreakdown;
	}

	public boolean isInMaintenance() {
		return maintenance;
	}

	public boolean isInAssociatedMaintenance() {
		return associatedMaintenance;
	}

	public boolean isInService() {
		return ( brokendown || maintenance || associatedBreakdown || associatedMaintenance );
	}

	public void setBrokendown( boolean bool ) {
		brokendown = bool;
		this.setPresentState("Breakdown");
	}

	public void setMaintenance( boolean bool ) {
		maintenance = bool;
		this.setPresentState("Maintenance");
	}

	public void setAssociatedBreakdown( boolean bool ) {
		associatedBreakdown = bool;
	}

	public void setAssociatedMaintenance( boolean bool ) {
		associatedMaintenance = bool;
	}

	public Double getDowntimeDurationDistribution(double simTime) {
		return downtimeDurationDistribution.getValue() == null? null : downtimeDurationDistribution.getValue().getNextSample(simTime);
	}

	public double getDowntimeToReleaseEquipment() {
		return downtimeToReleaseEquipment.getValue();
	}

	public boolean hasServiceDefined() {
		return( maintenanceDurations.getValue().size() > 0 || getDowntimeDurationDistribution(this.getSimTime()) != null );
	}


	// *******************************************************************************************************
	// MAINTENANCE METHODS
	// *******************************************************************************************************

	/**
	 * Perform tasks required before a maintenance period
	 */
	public void doPreMaintenance() {
	//@debug@ cr 'Entity should be overloaded' print
	}

	/**
	 * Start working again following a breakdown or maintenance period
	 */
	public void restart() {
	//@debug@ cr 'Entity should be overloaded' print
	}

	/**
	 * Disconnect routes, release truck assignments, etc. when performing maintenance or breakdown
	 */
	public void releaseEquipment() {}

	public boolean releaseEquipmentForMaintenanceSchedule( int index ) {

		if( releaseEquipment.getValue() == null )
			return true;

		return releaseEquipment.getValue().get( index );
	}

	public boolean forceMaintenanceSchedule( int index ) {

		if( forceMaintenance.getValue() == null )
			return false;

		return forceMaintenance.getValue().get( index );
	}

	/**
	 * Perform all maintenance schedules that are due
	 */
	public void doMaintenance() {

		// scheduled maintenance
		for( int index = 0; index < maintenancePendings.size(); index++ ) {
			if( this.getMaintenancePendings().get( index ) > 0 ) {
				if( traceFlag ) this.trace( "Starting Maintenance Schedule: " + index );
				this.doMaintenance(index);
			}
		}

		// Operating hours maintenance
		for( int index = 0; index < maintenanceOperatingHoursPendings.size(); index++ ) {
			if( this.getWorkingTime() > hoursForNextMaintenanceOperatingHours.get( index ) ) {
				hoursForNextMaintenanceOperatingHours.set(index, this.getWorkingTime() + getMaintenanceOperatingHoursIntervals().get( index ));
				maintenanceOperatingHoursPendings.addAt( 1, index );
				this.doMaintenanceOperatingHours(index);
			}
		}
	}

	/**
	 * Perform all the planned maintenance that is due for the given schedule
	 */
	public void doMaintenance( int index ) {

		double wait;
		if( masterMaintenanceEntity != null ) {
			wait = masterMaintenanceEntity.getMaintenanceDurations().getValue().get( index );
		}
		else {
			wait = this.getMaintenanceDurations().getValue().get( index );
		}

		if( wait > 0.0 && maintenancePendings.get( index ) != 0 ) {

			if( traceFlag ) this.trace( "ModelEntity.doMaintenance_Wait() -- start of maintenance" );

			// Keep track of the start and end of maintenance times
			maintenanceStartTime = getSimTime();

			if( masterMaintenanceEntity != null ) {
				maintenanceEndTime = maintenanceStartTime + ( maintenancePendings.get( index ) * masterMaintenanceEntity.getMaintenanceDurations().getValue().get( index ) );
			}
			else {
				maintenanceEndTime = maintenanceStartTime + ( maintenancePendings.get( index ) * maintenanceDurations.getValue().get( index ) );
			}

			this.setPresentState( "Maintenance");
			maintenance = true;
			this.doPreMaintenance();

			// Release equipment if necessary
			if( this.releaseEquipmentForMaintenanceSchedule( index ) ) {
				this.releaseEquipment();
			}

			while( maintenancePendings.get( index ) != 0 ) {
				maintenancePendings.subAt( 1, index );
				scheduleWait( wait );

				// If maintenance pending goes negative, something is wrong
				if( maintenancePendings.get( index ) < 0 ) {
					this.error( "ModelEntity.doMaintenance_Wait()", "Maintenace pending should not be negative", "maintenacePending = "+maintenancePendings.get( index ) );
				}
			}
			if( traceFlag ) this.trace( "ModelEntity.doMaintenance_Wait() -- end of maintenance" );

			//  The maintenance is over
			this.setPresentState( "Idle");
			maintenance = false;
			this.restart();
		}
	}

	/**
	 * Perform all the planned maintenance that is due
	 */
	public void doMaintenanceOperatingHours( int index ) {
		if(maintenanceOperatingHoursPendings.get( index ) == 0 )
			return;

		if( traceFlag ) this.trace( "ModelEntity.doMaintenance_Wait() -- start of maintenance" );

		// Keep track of the start and end of maintenance times
		maintenanceStartTime = getSimTime();
		maintenanceEndTime = maintenanceStartTime +
		   (maintenanceOperatingHoursPendings.get( index ) * getMaintenanceOperatingHoursDurationFor(index));

		this.setPresentState( "Maintenance");
		maintenance = true;
		this.doPreMaintenance();

		while( maintenanceOperatingHoursPendings.get( index ) != 0 ) {
			//scheduleWait( maintenanceDurations.get( index ) );
			scheduleWait( maintenanceEndTime - maintenanceStartTime );
			maintenanceOperatingHoursPendings.subAt( 1, index );

			// If maintenance pending goes negative, something is wrong
			if( maintenanceOperatingHoursPendings.get( index ) < 0 ) {
				this.error( "ModelEntity.doMaintenance_Wait()", "Maintenace pending should not be negative", "maintenacePending = "+maintenanceOperatingHoursPendings.get( index ) );
			}

		}
		if( traceFlag ) this.trace( "ModelEntity.doMaintenance_Wait() -- end of maintenance" );

		//  The maintenance is over
		maintenance = false;
		this.setPresentState( "Idle");
		this.restart();
	}

	/**
	 * Check if a maintenance is due.  if so, try to perform the maintenance
	 */
	public boolean checkMaintenance() {
		if( traceFlag ) this.trace( "checkMaintenance()" );
		if( checkOperatingHoursMaintenance() ) {
			return true;
		}

		// List of all entities going to maintenance
		ArrayList<ModelEntity> sharedMaintenanceEntities;

		// This is not a master maintenance entity
		if( masterMaintenanceEntity != null ) {
			sharedMaintenanceEntities = masterMaintenanceEntity.getSharedMaintenanceList();
		}

		// This is a master maintenance entity
		else {
			sharedMaintenanceEntities = getSharedMaintenanceList();
		}

		// If this entity is in shared maintenance relation with a group of entities
		if( sharedMaintenanceEntities.size() > 0 || masterMaintenanceEntity != null ) {

			// Are all entities in the group ready for maintenance
			if( this.areAllEntitiesAvailable() ) {

				// For every entity in the shared maintenance list plus the master maintenance entity
				for( int i=0; i <= sharedMaintenanceEntities.size(); i++ ) {
					ModelEntity aModel;

					// Locate master maintenance entity( after all entity in shared maintenance list have been taken care of )
					if( i == sharedMaintenanceEntities.size() ) {

						// This entity is manster maintenance entity
						if( masterMaintenanceEntity == null ) {
							aModel = this;
						}

						// This entity is on the shared maintenannce list of the master maintenance entity
						else {
							aModel = masterMaintenanceEntity;
						}
					}

					// Next entity in the shared maintenance list
					else {
						aModel = sharedMaintenanceEntities.get( i );
					}

					// Check for aModel maintenances
					for( int index = 0; index < maintenancePendings.size(); index++ ) {
						if( aModel.getMaintenancePendings().get( index ) > 0 ) {
							if( traceFlag ) this.trace( "Starting Maintenance Schedule: " + index );
							aModel.startProcess("doMaintenance", index);
						}
					}
				}
				return true;
			}
			else {
				return false;
			}
		}

		// This block is maintained indipendently
		else {

			//  Check for maintenances
			for( int i = 0; i < maintenancePendings.size(); i++ ) {
				if( maintenancePendings.get( i ) > 0 ) {
					if( this.canStartMaintenance( i ) ) {
						if( traceFlag ) this.trace( "Starting Maintenance Schedule: " + i );
						this.startProcess("doMaintenance", i);
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Determine how many hours of maintenance is scheduled between startTime and endTime
	 */
	public double getScheduledMaintenanceHoursForPeriod( double startTime, double endTime ) {
		if( traceFlag ) this.trace("Handler.getScheduledMaintenanceHoursForPeriod( "+startTime+", "+endTime+" )" );

		double totalHours = 0.0;
		double firstTime = 0.0;

		// Add on hours for all pending maintenance
		for( int i=0; i < maintenancePendings.size(); i++ ) {
			totalHours += maintenancePendings.get( i ) * maintenanceDurations.getValue().get( i );
		}

		if( traceFlag ) this.traceLine( "Hours of pending maintenances="+totalHours );

		// Add on hours for all maintenance scheduled to occur in the given period from startTime to endTime
		for( int i=0; i < maintenancePendings.size(); i++ ) {

			// Find the first time that maintenance is scheduled after startTime
			firstTime = firstMaintenanceTimes.getValue().get( i );
			while( firstTime < startTime ) {
				firstTime += maintenanceIntervals.getValue().get( i );
			}
			if( traceFlag ) this.traceLine(" first time maintenance "+i+" is scheduled after startTime= "+firstTime );

			// Now have the first maintenance start time after startTime
			// Add all maintenances that lie in the given interval
			while( firstTime < endTime ) {
				if( traceFlag ) this.traceLine(" Checking for maintenances for period:"+firstTime+" to "+endTime );
				// Add the maintenance
				totalHours += maintenanceDurations.getValue().get( i );

				// Update the search period
				endTime += maintenanceDurations.getValue().get( i );

				// Look for next maintenance in new interval
				firstTime += maintenanceIntervals.getValue().get( i );
				if( traceFlag ) this.traceLine(" Adding Maintenance duration = "+maintenanceDurations.getValue().get( i ) );
			}
		}

		// Return the total hours of maintenance scheduled from startTime to endTime
		if( traceFlag ) this.traceLine( "Maintenance hours to add= "+totalHours );
		return totalHours;
	}

	public boolean checkOperatingHoursMaintenance() {
		if( traceFlag ) this.trace("checkOperatingHoursMaintenance()");

		//  Check for maintenances
		for( int i = 0; i < getMaintenanceOperatingHoursIntervals().size(); i++ ) {

			// If the entity is not available, maintenance cannot start
			if( ! this.canStartMaintenance( i ) )
				continue;

			if( this.getWorkingTime() > hoursForNextMaintenanceOperatingHours.get( i ) ) {
				hoursForNextMaintenanceOperatingHours.set(i, (this.getWorkingTime() + getMaintenanceOperatingHoursIntervals().get( i )));
				maintenanceOperatingHoursPendings.addAt( 1, i );

				if( traceFlag ) this.trace( "Starting Maintenance Operating Hours Schedule : " + i );
				this.startProcess("doMaintenanceOperatingHours", i);
				return true;
			}
		}
		return false;
	}

	/**
	 * Wrapper method for doMaintenance_Wait.
	 */
	public void doMaintenanceNetwork() {
		this.startProcess("doMaintenanceNetwork_Wait");
	}

	/**
	 * Network for planned maintenance.
	 * This method should be called in the initialize method of the specific entity.
	 */
	public void doMaintenanceNetwork_Wait() {

		// Initialize schedules
		for( int i=0; i < maintenancePendings.size(); i++ ) {
			maintenancePendings.set( i, 0 );
		}
		nextMaintenanceTimes = new DoubleVector(firstMaintenanceTimes.getValue());
		nextMaintenanceDuration = 0;

		// Find the next maintenance event
		int index = 0;
		double earliestTime = Double.POSITIVE_INFINITY;
		for( int i=0; i < nextMaintenanceTimes.size(); i++ ) {
			double time = nextMaintenanceTimes.get( i );
			if( Tester.lessCheckTolerance( time, earliestTime ) ) {
				earliestTime = time;
				index = i;
				nextMaintenanceDuration = maintenanceDurations.getValue().get( i );
			}
		}

		// Make sure that maintenance for entities on the shared list are being called after those entities have been initialize (AT TIME ZERO)
		scheduleLastLIFO();
		while( true ) {

			double dt = earliestTime - getSimTime();

			// Wait for the maintenance check time
			/*if( dt > Process.getEventTolerance() ) {
				scheduleWait( dt );
			}
*/
			// Increment the number of maintenances due for the entity
			maintenancePendings.addAt( 1, index );

			// If this is a master maintenance entity
			if (getSharedMaintenanceList().size() > 0) {

				// If all the entities on the shared list are ready for maintenance
				if( this.areAllEntitiesAvailable() ) {

					// Put this entity to maintenance
					if( traceFlag ) this.trace( "Starting Maintenance Schedule: " + index );
					this.startProcess("doMaintenance", index);
				}
			}

			// If this entity is maintained independently
			else {

				// Do maintenance if possible
				if( ! this.isInService() && this.canStartMaintenance( index ) ) {
					// if( traceFlag ) this.trace( "doMaintenanceNetwork_Wait: Starting Maintenance.  PresentState = "+presentState+" IsAvailable? = "+this.isAvailable() );
					if( traceFlag ) this.trace( "Starting Maintenance Schedule: " + index );
					this.startProcess("doMaintenance", index);
				}
				// Keep track of the time the maintenance was attempted
				else {
					lastScheduledMaintenanceTimes.set( index, getSimTime() );

					// If skipMaintenance was defined, cancel the maintenance
					if( this.shouldSkipMaintenance( index ) ) {

						// if a different maintenance is due, cancel this maintenance
						boolean cancelMaintenance = false;
						for( int i=0; i < maintenancePendings.size(); i++ ) {
							if( i != index ) {
								if( maintenancePendings.get( i ) > 0 ) {
									cancelMaintenance = true;
									break;
								}
							}
						}

						if( cancelMaintenance || this.isInMaintenance() ) {
							maintenancePendings.subAt( 1, index );
						}
					}

					// Do a check after the limit has expired
					if( this.getDeferMaintenanceLimit( index ) > 0.0 ) {
						this.startProcess( "scheduleCheckMaintenance", this.getDeferMaintenanceLimit( index ) );
					}
				}
			}

			// Determine the next maintenance time
			nextMaintenanceTimes.addAt( maintenanceIntervals.getValue().get( index ), index );

			// Find the next maintenance event
			index = 0;
			earliestTime = Double.POSITIVE_INFINITY;
			for( int i=0; i < nextMaintenanceTimes.size(); i++ ) {
				double time = nextMaintenanceTimes.get( i );
				if( Tester.lessCheckTolerance( time, earliestTime ) ) {
					earliestTime = time;
					index = i;
					nextMaintenanceDuration = maintenanceDurations.getValue().get( i );
				}
			}
		}
	}

	public double getDeferMaintenanceLimit( int index ) {

		if( deferMaintenanceLimit.getValue() == null )
			return 0.0d;

		return deferMaintenanceLimit.getValue().get( index );
	}

	public void scheduleCheckMaintenance( double wait ) {
		scheduleWait( wait );
		this.checkMaintenance();
	}

	public boolean shouldSkipMaintenance( int index ) {

		if( skipMaintenanceIfOverlap.getValue().size() == 0 )
			return false;

		return skipMaintenanceIfOverlap.getValue().get( index );
	}

	/**
	 * Return TRUE if there is a pending maintenance for any schedule
	 */
	public boolean isMaintenancePending() {
		for( int i = 0; i < maintenancePendings.size(); i++ ) {
			if( maintenancePendings.get( i ) > 0 ) {
				return true;
			}
		}

		for( int i = 0; i < hoursForNextMaintenanceOperatingHours.size(); i++ ) {
			if( this.getWorkingTime() > hoursForNextMaintenanceOperatingHours.get( i ) ) {
				return true;
			}
		}

		return false;
	}

	public boolean isForcedMaintenancePending() {

		if( forceMaintenance.getValue() == null )
			return false;

		for( int i = 0; i < maintenancePendings.size(); i++ ) {
			if( maintenancePendings.get( i ) > 0 && forceMaintenance.getValue().get(i) ) {
				return true;
			}
		}
		return false;
	}

	public ArrayList<ModelEntity> getSharedMaintenanceList () {
		return sharedMaintenanceList.getValue();
	}

	public IntegerVector getMaintenancePendings () {
		return maintenancePendings;
	}

	public DoubleListInput getMaintenanceDurations() {
		return maintenanceDurations;
	}


	/**
	 * Return the start of the next scheduled maintenance time if not in maintenance,
	 * or the start of the current scheduled maintenance time if in maintenance
	 */
	public double getNextMaintenanceStartTime() {
		if( nextMaintenanceTimes == null )
			return Double.POSITIVE_INFINITY;
		else
			return nextMaintenanceTimes.getMin();
	}

	/**
	 * Return the duration of the next maintenance event (assuming only one pending)
	 */
	public double getNextMaintenanceDuration() {
		return nextMaintenanceDuration;
	}

	// Shows if an Entity would ever go on service
	public boolean hasServiceScheduled() {
		if( firstMaintenanceTimes.getValue().size() != 0 || masterMaintenanceEntity != null ) {
			return true;
		}
		return false;
	}
	public void setMasterMaintenanceBlock( ModelEntity aModel ) {
		masterMaintenanceEntity =  aModel;
	}

	// *******************************************************************************************************
	// BREAKDOWN METHODS
	// *******************************************************************************************************

	/**
	 * No Comments Given.
	 */
	public void calculateTimeOfNextFailure() {
		hoursForNextFailure = (this.getWorkingTime() + this.getNextBreakdownIAT());
	}

	/**
	 * Activity Network for Breakdowns.
	 */
	public void doBreakdown() {
	}

	

	/**
	 * This method must be overridden in any subclass of ModelEntity.
	 */
	public boolean isAvailable() {
		throw new ErrorException( "Must override isAvailable in any subclass of ModelEntity." );
	}

	/**
	 * This method must be overridden in any subclass of ModelEntity.
	 */
	public boolean canStartMaintenance( int index ) {
		return isAvailable();
	}

	/**
	 * This method must be overridden in any subclass of ModelEntity.
	 */
	public boolean canStartForcedMaintenance() {
		return isAvailable();
	}

	/**
	 * This method must be overridden in any subclass of ModelEntity.
	 */
	public boolean areAllEntitiesAvailable() {
		throw new ErrorException( "Must override areAllEntitiesAvailable in any subclass of ModelEntity." );
	}

	/**
	 * Return the time of the next breakdown duration
	 */
	public double getBreakdownDuration() {
		// if( traceFlag ) this.trace( "getBreakdownDuration()" );

		//  If a distribution was specified, then select a duration randomly from the distribution
		if ( getDowntimeDurationDistribution(this.getSimTime()) != null ) {
			return getDowntimeDurationDistribution(this.getSimTime());
		}
		else {
			return 0.0;
		}
	}

	/**
	 * Return the time of the next breakdown IAT
	 */
	public double getNextBreakdownIAT() {
		if( getDowntimeIATDistribution(this.getSimTime()) != null ) {
			return getDowntimeIATDistribution(this.getSimTime()) * iATFailureFactor;
		}
		else {
			return iATFailure;
		}
	}

	public double getHoursForNextFailure() {
		return hoursForNextFailure;
	}

	public void setHoursForNextFailure( double hours ) {
		hoursForNextFailure = hours;
	}

	protected DoubleVector getMaintenanceOperatingHoursIntervals() {
		return maintenanceOperatingHoursIntervals.getValue();
	}

	protected double getMaintenanceOperatingHoursDurationFor(int index) {
		return maintenanceOperatingHoursDurations.getValue().get(index);
	}

	protected Double getDowntimeIATDistribution(double simTime) {
		return downtimeIATDistribution.getValue()==null? null: downtimeIATDistribution.getValue().getNextSample(simTime);
	}
}