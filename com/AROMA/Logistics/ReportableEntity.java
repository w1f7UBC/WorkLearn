package com.AROMA.Logistics;

import java.util.ArrayList;

import com.AROMA.AROMAEntity;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;
import com.sandwell.JavaSimulation.FileEntity;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Simulation;
import com.sandwell.JavaSimulation.Tester;

public class ReportableEntity extends AROMAEntity {

	private static final ArrayList<ReportableEntity> allInstances;

	// Statistics

	@Keyword(description = "If TRUE, then statistics for this object are " +
	                "included in the main output report.",
	         example = "Object1 PrintStateReport { TRUE }")
	private final BooleanInput printStateReport;

	// States
	private final ArrayList<StateRecord> states;
	protected double workingTime;                   	  // Accumulated working time spent in working states
	private double timeOfLastStateChange;
	private int numberOfCompletedCycles;
	private double startOfCollectStatsTime;
	private double startOfCycleTime;
	private double maxCycleDur;
	private double minCycleDur;
	private double totalCompletedCycleTime;
	
	protected double lastHistogramUpdateTime;   // Last time at which a histogram was updated for this entity
	protected double secondToLastHistogramUpdateTime;   // Second to last time at which a histogram was updated for this entity
	private StateRecord presentState; // The present state of the entity
	protected FileEntity stateReportFile;        // The file to store the state information

	static {
		allInstances = new ArrayList<>(100);
	}
	
	{
		printStateReport = new BooleanInput("PrintStateReport", "Report", false);
		this.addInput(printStateReport);
	}
	
	public ReportableEntity() {
		super();
		workingTime = 0.0;
		states = new ArrayList<StateRecord>();
		
		StateRecord idle = this.validate("Idle");
		presentState=idle;
		idle.lastStartTimeInState = Simulation.getStartHours()*3600.0d;
		idle.secondLastStartTimeInState = Simulation.getStartHours()*3600.0d;
		initStateMap();
		stateReportFile = null;
	}
	
	@Override
	public void validate() {
		// TODO Auto-generated method stub
		super.validate();

	}
	
	@Override
	public void earlyInit() {
		// TODO Auto-generated method stub
		super.earlyInit();
		timeOfLastStateChange = getSimTime();
		startOfCollectStatsTime = getSimTime();
		
		// initiate file if print report is true
		if(printStateReport.getValue()){
			stateReportFile = ReportAgent.initializeFile(this,".rep");
			this.printStatesHeader();
		}		
	}
	
	@Override
	public void doEnd() {
		// TODO Auto-generated method stub
		super.doEnd();
		this.setPresentState("Simulation Ended");
		
	}

	public static ArrayList<? extends ReportableEntity> getAll() {
		synchronized (allInstances) {
			return allInstances;
		}
	}

	@Override
	public void kill() {
		// TODO Auto-generated method stub
		super.kill();
		synchronized (allInstances) {
			allInstances.remove(this);
		}
	}
	
	// ******************************************************************************************************
	// HOURS AND STATES
	// ******************************************************************************************************

public static class StateRecord {
	public final String name;
	double totalTime;
	double completedCycleTime;
	double currentCycleTime;
	double lastStartTimeInState;
	double secondLastStartTimeInState;

	private StateRecord(String state) {
		name = state;
	}

	public double getLastStartTimeInState() {
		return lastStartTimeInState;
	}

	public double getSecondLastStartTimeInState() {
		return secondLastStartTimeInState;
	}
	
	@Override
	public String toString() {
		return name;
	}
}	

	public int getNumberOfCompletedCycles() {
		return numberOfCompletedCycles;
	}

	public double getTimeOfLastStateChange() {
		return timeOfLastStateChange;
	}

	public boolean printStateReport() {
		return printStateReport.getValue();
	}
	
	public void initStateMap() {
		timeOfLastStateChange = Simulation.getStartHours()*3600.0d;
		maxCycleDur = 0.0d;
		minCycleDur = Double.POSITIVE_INFINITY;
		totalCompletedCycleTime = 0.0d;
		startOfCycleTime = Simulation.getStartHours()*3600.0d;
	}

	public StateRecord validate(String state) {

		StateRecord rec = new StateRecord(state);
		states.add(rec);
		return rec;

	}

	/**
	 * Runs when cycle is finished
	 */
	public void collectCycleStats() {
		collectPresentTime();

		// finalize cycle for each state record
		for (StateRecord each : states) {
			each.completedCycleTime += each.currentCycleTime;
			each.currentCycleTime = 0.0d;
		}
		numberOfCompletedCycles++;

		double dur = getSimTime() - startOfCycleTime;
		maxCycleDur = Math.max(maxCycleDur, dur);
		minCycleDur = Math.min(minCycleDur, dur);
		totalCompletedCycleTime += dur;
		startOfCycleTime = getSimTime();
	}

	/**
	 * Clear the current cycle time, also reset the start of cycle time
	 */
	protected void clearCurrentCycleTime() {
		collectPresentTime();

		// clear current cycle time for each state record
		for (StateRecord each : states)
			each.currentCycleTime = 0.0d;

		startOfCycleTime = getSimTime();
	}

	/**
	 * Runs after each report interval
	 */
	public void clearReportStats() {
		collectPresentTime();

		for (StateRecord each : states) {
			each.totalTime = 0.0d;
			each.completedCycleTime = 0.0d;
		}
		numberOfCompletedCycles = 0;
		totalCompletedCycleTime = 0.0d; 
		workingTime = 0.0d;

		maxCycleDur = 0.0d;
		minCycleDur = Double.POSITIVE_INFINITY;
		totalCompletedCycleTime = 0.0d;
		try{
			
		} catch(ClassCastException e){}
	}

	/**
	 * Update the time for the present state and set new timeofLastStateChange
	 */
	private void collectPresentTime() {
		double curTime = getSimTime();

		if (curTime == timeOfLastStateChange)
			return;

		double duration = curTime - timeOfLastStateChange;
		timeOfLastStateChange = curTime;

		presentState.totalTime += duration;
		presentState.currentCycleTime += duration;
		if (this.isWorking())
			workingTime += duration;
	}
	
	/**
	 * A callback subclasses can override that is called on each state transition.
	 *
	 * The state has not been changed when this is called, so presentState is still
	 * valid.
	 *
	 * @param next the state being transitioned to
	 */
	public void stateChanged(StateRecord prev, StateRecord next) {}

	/**
	 * Updates the statistics, then sets the present status to be the specified value.
	 * @param externalCall whether this is called by a dependent entity whose state change should be reported back in this entity's report
	 * @return whether states report has been printed or not
	 */
	public boolean setPresentState( String state) {
		
		StateRecord nextState = this.getState(state);
		if (nextState == null)
			nextState = validate(state);

		if (traceFlag) {
			StringBuffer buf = new StringBuffer("setState( ");
			buf.append(nextState.name).append(" )");
			this.trace(buf.toString());

			buf.setLength(0);
			buf.append(" Old State = ").append(presentState.name);
			this.traceLine(buf.toString());
		}

		//Check with report agent to set initialized if initialization time is passed
		// TODO Bad implementation. refactor to check once only.
		double curTime = getSimTime();
		if (curTime >= Simulation.getInitializationHours()*3600.0d){
			ReportAgent.resetInitializationStats();			
			startOfCollectStatsTime = Simulation.getInitializationHours()*3600.0d;
		}
		double time = timeOfLastStateChange;		
		double duration = curTime - timeOfLastStateChange;
		collectPresentTime();
		
		nextState.secondLastStartTimeInState = nextState.getLastStartTimeInState();
		nextState.lastStartTimeInState = timeOfLastStateChange;

		StateRecord prev = presentState;
		presentState = nextState;
		stateChanged(prev, presentState);
		if(Tester.equalCheckTolerance(duration, 0.0d))
			return false;
		if (stateReportFile !=null ) {
			// add bottom line for states report
			stateReportFile.putDoubleWithDecimalsTabs(time-Simulation.getInitializationHours(), ReportAgent.getReportPrecision(), 1);
			stateReportFile.putStringTabs(this.getName(), 1);
			stateReportFile.putStringTabs(prev.name, 1);
			stateReportFile.putDoubleWithDecimalsTabs(duration, ReportAgent.getReportPrecision(),1);
			stateReportFile.flush();
			return true;
		}
		return false;
	}
	
	/**
	 * reprints states report line. it should be called when one of the fields other than the state is changed
	 */
	public void updateStatesReport(){
		this.setPresentState(this.getPresentState());
	}

	public StateRecord getState(String state) {
		for (StateRecord each : states) {
			if (each.name.equals(state)) {
				return each;
			}
		}

		return null;
	}

	public double getTotalTime(StateRecord state) {
		if (state == null)
			return 0.0d;

		double time = state.totalTime;
		if (presentState == state)
			time += getSimTime() - timeOfLastStateChange;

		return time;
	}

	public double getTotalTime() {
		return getSimTime() - startOfCollectStatsTime;
	}

	public double getCompletedCycleTime() {
		return totalCompletedCycleTime;
	}

	public double getCompletedCycleTime(StateRecord state) {
		if (state == null)
			return 0.0d;

		return state.completedCycleTime;
	}

	public double getCurrentCycleTime(StateRecord state) {
		if (state == null)
			return 0.0d;

		double time = state.currentCycleTime;
		if (presentState == state)
			time += getSimTime() - timeOfLastStateChange;

		return time;
	}

	/**
	 * Return the total time in current cycle for all the states
	 */
	public double getCurrentCycleTime() {
		return getSimTime() - startOfCycleTime;
	}

	public double getStartCycleTime() {
		return startOfCycleTime;
	}

	/**
	 * Returns the present state name
	 */
	public String getPresentState() {
		return presentState.name;
	}

	public StateRecord getState() {
		return presentState;
	}

	public boolean presentStateEquals(String state) {
		return getPresentState().equals(state);
	}

	public boolean presentStateMatches(String state) {
		return getPresentState().equalsIgnoreCase(state);
	}

	public boolean presentStateStartsWith(String prefix) {
		return getPresentState().startsWith(prefix);
	}

	public boolean presentStateEndsWith(String suffix) {
		return getPresentState().endsWith(suffix);
	}

	/**
	 * Set the last time a histogram was updated for this entity
	 */
	public void setLastHistogramUpdateTime( double time ) {
		secondToLastHistogramUpdateTime = lastHistogramUpdateTime;
		lastHistogramUpdateTime = time;
	}

	/**
	 * Returns the time from the start of the start state to the start of the end state
	 */
	public double getTimeFromStartState_ToEndState( String startState, String endState) {

		StateRecord startStateRec = this.getState(startState);
		StateRecord endStateRec = this.getState(endState);
		if (startStateRec == null || endStateRec == null) {
			return Double.NaN;
		}

		// Is the start time of the end state greater or equal to the start time of the start state?
		if (endStateRec.getLastStartTimeInState() >= startStateRec.getLastStartTimeInState()) {

			// If either time was not in the present cycle, return NaN
			if (endStateRec.getLastStartTimeInState() <= lastHistogramUpdateTime ||
			   startStateRec.getLastStartTimeInState() <= lastHistogramUpdateTime ) {
				return Double.NaN;
			}
			// Return the time from the last start time of the start state to the last start time of the end state
			return endStateRec.getLastStartTimeInState() - startStateRec.getLastStartTimeInState();
		}
		else {
			// If either time was not in the present cycle, return NaN
			if (endStateRec.getLastStartTimeInState() <= lastHistogramUpdateTime ||
			   startStateRec.getSecondLastStartTimeInState() <= secondToLastHistogramUpdateTime ) {
				return Double.NaN;
			}
			// Return the time from the second to last start time of the start date to the last start time of the end state
			return endStateRec.getLastStartTimeInState() - startStateRec.getSecondLastStartTimeInState();
		}
	}

	/**
	 * Returns the total time the entity has been in use.
	 * *!*!*!*! OVERLOAD !*!*!*!*
	 */
	public double getWorkingTime() {
		double time = 0.0d;
		if ( this.isWorking() )
			time = getSimTime() - timeOfLastStateChange;

		return workingTime + time;
	}

	public double getMaxCycleDur() {
		return maxCycleDur;
	}

	public double getMinCycleDur() {
		return minCycleDur;
	}

	public ArrayList<StateRecord> getStateList() {
		return states;
	}
	
	/**
	 * Return true if the entity is working
	 */
	public boolean isWorking() {
		return false;
	}

	/**
	 * prints headers for the statesreportfile. add line and flush file after implementation.
	 */
	public void printStatesHeader(){
		stateReportFile.putStringTabs("Time",1);
		stateReportFile.putStringTabs("Entity Name", 1);
		stateReportFile.putStringTabs("State",1);
		stateReportFile.putStringTabs("Duration",1);
	}
	
	public void printStatesUnitsHeader(){
		// Print units
		stateReportFile.newLine();
		stateReportFile.putStringTabs("(s)", 3);
		stateReportFile.putStringTabs("(s)", 1);
	}

	/**
	 * Prints the header for the entity's state list.
	 * @return bottomLine contains format for each column of the bottom line of the group report
	 */
	public IntegerVector printUtilizationHeaderOn( FileEntity anOut ) {

		IntegerVector bottomLine = new IntegerVector();

		if( getStateList().size() != 0 ) {
			anOut.putStringTabs( "Name", 1 );
			bottomLine.add( ReportAgent.BLANK );

			int doLoop = getStateList().size();
			for( int x = 0; x < doLoop; x++ ) {
				String state = getStateList().get( x ).name;
				anOut.putStringTabs( state, 1 );
				bottomLine.add( ReportAgent.AVERAGE_PCT_ONE_DEC );
			}
			anOut.newLine();
		}
		return bottomLine;
	}

	/**
	 * Print the entity's name and percentage of time spent in each state.
	 * @return columnValues are the values for each column in the group report (0 if the value is a String)
	 */
	public DoubleVector printUtilizationOn( FileEntity anOut ) {

		double total;
		DoubleVector columnValues = new DoubleVector();
		
		total = getTotalTime();
		if (total == 0.0d)
			return columnValues;

		anOut.format("%s\t", getName());
		columnValues.add(0.0d);

		// print fraction of time per state
		for (int i = 0; i < getStateList().size(); i++) {
			String state = getStateList().get( i ).name;
			double timeFraction = getTotalTime(getState(state))/total;
			anOut.format("%.1f%%\t", timeFraction * 100.0d);
			columnValues.add(timeFraction);
		}
		anOut.format("%n");
		return columnValues;
	}


	// ////////////////////////////////////////////////////////////////////////////////////
	// OUTPUTS
	// ////////////////////////////////////////////////////////////////////////////////////

	@Output(name = "CurrentState",
			description = "Returns the current state of this reportable entity.",
			unitType = DimensionlessUnit.class)
			
	public String getCurrentState( double simtime) {
		return this.getPresentState();
	}
		
}
