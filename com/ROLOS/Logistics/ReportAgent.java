/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2008-2011 Ausenco Engineering Canada Inc.
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

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;

import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.units.TimeUnit;

import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Group;

import com.sandwell.JavaSimulation.Simulation;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.ROLOS.Logistics.ReportableEntity;

public class ReportAgent extends DisplayEntity {

	private final ValueListInput reportIntervals;
	private static final IntegerInput reportPrecision;

	private static final ArrayList<FileEntity> allReportFiles;
	private final ArrayList<Group> groupList;  // groups for reporting in the .grp file
	protected static double lastReportIntervalTime; // time of the last report printing
	protected static boolean initialized; // flag to avoid re-entering collectInitializationStats() method

	// Constants for the bottom line information of the group report columns
	public static final int TOTAL_NO_DEC = 0;
	public static final int TOTAL_ONE_DEC = 1;
	public static final int TOTAL_TWO_DEC = 2;
	public static final int TOTAL_THREE_DEC = 3;
	public static final int TOTAL_FOUR_DEC = 4;
	public static final int TOTAL_FIVE_DEC = 5;
	public static final int AVERAGE_PCT_NO_DEC = 6;
	public static final int AVERAGE_PCT_ONE_DEC = 7;
	public static final int AVERAGE_PCT_TWO_DEC = 8;
	public static final int AVERAGE_PCT_THREE_DEC = 9;
	public static final int AVERAGE_PCT_FOUR_DEC = 10;
	public static final int AVERAGE_PCT_FIVE_DEC = 11;
	public static final int MINIMUM_NO_DEC = 12;
	public static final int MINIMUM_ONE_DEC = 13;
	public static final int MINIMUM_TWO_DEC = 14;
	public static final int MINIMUM_THREE_DEC = 15;
	public static final int MINIMUM_FOUR_DEC = 16;
	public static final int MINIMUM_FIVE_DEC = 17;
	public static final int MAXIMUM_NO_DEC = 18;
	public static final int MAXIMUM_ONE_DEC = 19;
	public static final int MAXIMUM_TWO_DEC = 20;
	public static final int MAXIMUM_THREE_DEC = 21;
	public static final int MAXIMUM_FOUR_DEC = 22;
	public static final int MAXIMUM_FIVE_DEC = 23;
	public static final int AVERAGE_NO_DEC = 24;
	public static final int AVERAGE_ONE_DEC = 25;
	public static final int AVERAGE_TWO_DEC = 26;
	public static final int AVERAGE_THREE_DEC = 27;
	public static final int AVERAGE_FOUR_DEC = 28;
	public static final int AVERAGE_FIVE_DEC = 29;
	public static final int BLANK = 30;
	
	static{
		allReportFiles = new ArrayList<>();
		reportPrecision = new IntegerInput("NumberOfDecimals", "Key Inputs", 1);
		reportPrecision.setValidRange(0, 20);
	}
	
	{
		
		reportIntervals = new ValueListInput("ReportIntervals", "Key Inputs", new DoubleVector(0));
		reportIntervals.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		reportIntervals.setUnitType(TimeUnit.class);
		this.addInput(reportIntervals);
		
		this.addInput(reportPrecision);
	}

	public ReportAgent() {
		groupList = new ArrayList<Group>();
		lastReportIntervalTime = Simulation.getInitializationHours()*3600.0d;

		initialized = false;
	}
	
	public static int getReportPrecision(){
		return reportPrecision.getValue();
	}

	@Override
	public void validate() {
		super.validate();
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();
		if (!groupList.isEmpty())
			for(Group each: groupList){
				ReportAgent.initializeFile(each, ".grp");
			}
		
	}
	
	// ******************************************************************************************************
	// INITIALIZATION METHODS
	// ******************************************************************************************************

	/**
	 * Collect stats for each model entity at the end of initialization period
	 */
	protected static void resetInitializationStats() {
		if (initialized)
			return;

		for ( ReportableEntity each : Simulation.getClonesOf(ReportableEntity.class) ) {
			each.clearReportStats();
		}
	}

	public static void addReport(FileEntity reportToAdd) {
		allReportFiles.add(reportToAdd);
	}
		
	public static ArrayList<FileEntity> getAllReportFiles(){
		return allReportFiles;
	}

	/**
 	 * Clear the file with the given name
 	 * TODO refactor to iterate over all report files
 	 */
 	public static <T extends Entity> FileEntity initializeFile(T entity, String ext) {
 		if(!initialized){
 			InputAgent.setReportDirectory(new File(InputAgent.getReportDirectory()+"/Report"));
 			InputAgent.prepareReportDirectory();
 			initialized = true;
 		}
 		
 		String pathName;
 		// make subfolder for entities whose name include "/" such as facility managers
		pathName = InputAgent.getReportDirectory() + InputAgent.getRunName();
		File dirPath = new File(pathName);
 		dirPath.mkdir();
 		
 		if(entity.getInputName().contains("/")){
 			pathName = InputAgent.getReportDirectory() + InputAgent.getRunName()+"/"+entity.getInputName().substring(0, entity.getInputName().indexOf("/"));
 		}
 		dirPath = new File(pathName);
 		dirPath.mkdir();
 		
		String fileName = pathName+ "/" + entity.getName()+ext;

		try {
			if( InputAgent.fileExists( InputAgent.getFileURI(null, fileName, null) ) ) {
					new File(fileName).delete();
			}
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
		FileEntity reportFile = new FileEntity(fileName, false);	
		allReportFiles.add(reportFile);
		ReportAgent.printReportHeaderOn(reportFile);
	//	ReportAgent.printReportIntervalHeader(entity, ext);
		
		return reportFile;
 	}
 	
 	@Override
 	public void doEnd() {
 		// TODO Auto-generated method stub
 		super.doEnd();
 	
 	}
 	
	// ******************************************************************************************************
	// INPUT METHODS
	// ******************************************************************************************************

	/**
	 * Interpret the input data in the given buffer of strings corresponding to the given keyword.
	 */
/*	@Override
	public void readData_ForKeyword(StringVector data, String keyword)
	throws InputErrorException {

		// --------------- ReportDirectory ---------------
		if( "ReportDirectory".equalsIgnoreCase( keyword ) ) {
			Input.assertCountRange(data, 0, 1);
			if (data.size() == 0)
				InputAgent.setReportDirectory("");
			else
				InputAgent.setReportDirectory(data.get(0));

			return;
		}
		if ("GroupList".equalsIgnoreCase(keyword)) {
			ArrayList<Group> temp = new ArrayList<Group>(data.size());

			for (int i = 0; i < data.size(); i++) {
				temp.add(Input.parseEntity(data.get(i), Group.class));
			}
			groupList.clear();
			groupList.addAll(temp);
			return;
		}

		super.readData_ForKeyword( data, keyword );
	}/*

	public boolean hasReportIntervals() {
		return reportIntervals.getValue().size() > 0;
	}

	public DoubleVector getReportIntervals() {
		return reportIntervals.getValue();
	}

	// ******************************************************************************************************
	// WORKING METHODS
	// ******************************************************************************************************

	/**
	 * Close the reports
	 */
	public void closeReports() {

		for(FileEntity each: allReportFiles){
			if( each != null ) {
				each.flush();
				each.close();
			}
		}
	}

	/**
	 * Print the report header on the given file
	 */
	public static void printReportHeaderOn( FileEntity anOut ) {
		String executableName = System.getProperty( "TLS.name" );
		if (executableName == null)
			executableName = System.getProperty( "exe4j.moduleName" );

		// If an executable name exists, print it
		if( executableName != null ) {
			anOut.format( "Executable File:\t%s\n\n", executableName );
		}

		anOut.format( "Simulation Run Label:  %s\n\n", InputAgent.getRunName() );
		anOut.format( "Run Duration:\t%s\n\n", Simulation.getRunDurationHours()*3600.0d );
		
		anOut.format( "Report Start:\t%f\nReport End:\t%f\n",
				   lastReportIntervalTime, Simulation.getRunDurationHours()*3600.0d);
		anOut.format( "=======================\n\n" );
		anOut.newLine();
		anOut.flush();
	}

	/**
	 * Prepare the group report file for writing by opening it
	 */
	public void doGroupReport() {
		
	}

	/**
	 * Print the reports at the end of the run
	 */
/*	private static <T extends Entity> void printReportIntervalHeader(T entity, String ext) {
		FileEntity reportFile = ReportAgent.getReportFile(entity, ext);
		reportFile.format( "Report Start:\t%f\nReport End:\t%f\n",
				   lastReportIntervalTime, entity.getCurrentTime());
		reportFile.format( "=======================\n\n" );
		reportFile.newLine();
		reportFile.flush();
	}
*/
	/**
	 * Print the utilization statistics for individual group members and as a total to the given file
	 * @param grp a group of entities can be passed on
	 */
	public static void printUtilizationOn(String title, ArrayList<? extends LogisticsEntity> grp, FileEntity anOut) {
		// If there are no elements in the group, do nothing
		if (grp.size() == 0)
			return;

		// Print the name of the group
		anOut.putString(title);
		anOut.newLine();
		for (int i = 0; i < title.length(); i++) {
			anOut.putString("-");
		}
		anOut.newLine();

		// Print the header and determine if the values on the bottom line are TOTAL, AVERAGE, or BLANK
		IntegerVector bottomLine = grp.get(0).printUtilizationHeaderOn( anOut );

		// Set up the totals, minimum, and maximum for the columns
		DoubleVector columnTotals = new DoubleVector( bottomLine.size() );
		DoubleVector columnMinimums = new DoubleVector( bottomLine.size() );
		DoubleVector columnMaximums = new DoubleVector( bottomLine.size() );
		columnTotals.fillWithEntriesOf( bottomLine.size(), 0.0 );
		columnMinimums.fillWithEntriesOf( bottomLine.size(), Double.POSITIVE_INFINITY );
		columnMaximums.fillWithEntriesOf( bottomLine.size(), Double.NEGATIVE_INFINITY );

		// Loop through each member of the group
		int count = 0;
		for (int i = 0; i < grp.size(); i++) {
			LogisticsEntity ent = grp.get(i);
			if (!ent.isActive())
				continue;

			// Print and store the values for this member
			DoubleVector columnValues = ent.printUtilizationOn( anOut );

			// Update the total, minimum, and maximum column values
			for( int j = 0; j < columnValues.size(); j++ ) {
				columnTotals.addAt( columnValues.get( j ), j );
				if( columnMinimums.get( j ) > columnValues.get( j ) ) {
					columnMinimums.set( j, columnValues.get( j ) );
				}
				if( columnMaximums.get( j ) < columnValues.get( j ) ) {
					columnMaximums.set( j, columnValues.get( j ) );
				}
			}
			count++;
		}

		// Print the bottom line for the group
		anOut.putStringTabs( "Total", 1 );
		for( int i = 1; i < bottomLine.size(); i++ ) {
			switch (bottomLine.get(i)) {
			case TOTAL_NO_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i), 0);
				break;
			case TOTAL_ONE_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i), 1);
				break;
			case TOTAL_TWO_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i), 2);
				break;
			case TOTAL_THREE_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i), 3);
				break;
			case TOTAL_FOUR_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i), 4);
				break;
			case TOTAL_FIVE_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i), 5);
				break;
			case MINIMUM_NO_DEC:
				anOut.putDoubleWithDecimals(columnMinimums.get(i), 0);
				break;
			case MINIMUM_ONE_DEC:
				anOut.putDoubleWithDecimals(columnMinimums.get(i), 1);
				break;
			case MINIMUM_TWO_DEC:
				anOut.putDoubleWithDecimals(columnMinimums.get(i), 2);
				break;
			case MINIMUM_THREE_DEC:
				anOut.putDoubleWithDecimals(columnMinimums.get(i), 3);
				break;
			case MINIMUM_FOUR_DEC:
				anOut.putDoubleWithDecimals(columnMinimums.get(i), 4);
				break;
			case MINIMUM_FIVE_DEC:
				anOut.putDoubleWithDecimals(columnMinimums.get(i), 5);
				break;
			case MAXIMUM_NO_DEC:
				anOut.putDoubleWithDecimals(columnMaximums.get(i), 0);
				break;
			case MAXIMUM_ONE_DEC:
				anOut.putDoubleWithDecimals(columnMaximums.get(i), 1);
				break;
			case MAXIMUM_TWO_DEC:
				anOut.putDoubleWithDecimals(columnMaximums.get(i), 2);
				break;
			case MAXIMUM_THREE_DEC:
				anOut.putDoubleWithDecimals(columnMaximums.get(i), 3);
				break;
			case MAXIMUM_FOUR_DEC:
				anOut.putDoubleWithDecimals(columnMaximums.get(i), 4);
				break;
			case MAXIMUM_FIVE_DEC:
				anOut.putDoubleWithDecimals(columnMaximums.get(i), 5);
				break;
			case AVERAGE_NO_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i) / count, 0);
				break;
			case AVERAGE_ONE_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i) / count, 1);
				break;
			case AVERAGE_TWO_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i) / count, 2);
				break;
			case AVERAGE_THREE_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i) / count, 3);
				break;
			case AVERAGE_FOUR_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i) / count, 4);
				break;
			case AVERAGE_FIVE_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i) / count, 5);
				break;
			case BLANK:
				break;
			}
			anOut.putTab();
		}
		anOut.newLine();
	}
	
	
	public static <T extends ReportableEntity> String entitiesStatesToString (ArrayList<T> entitiesLsit){
		String returnString = "{";
		int i= 0;
		for (; i < entitiesLsit.size()-1; i++ ){
			returnString = returnString + entitiesLsit.get(i).getPresentState() +", ";
		}
		
		return entitiesLsit.isEmpty() ? "" : returnString + entitiesLsit.get(i).getPresentState() +"}";

	}
}
