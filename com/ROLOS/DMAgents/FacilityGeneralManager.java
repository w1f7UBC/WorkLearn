package com.ROLOS.DMAgents;

import java.util.ArrayList;

import com.ROLOS.Economic.Contract;
import com.ROLOS.Logistics.BulkMaterial;
import com.ROLOS.Logistics.LogisticsEntity;
import com.ROLOS.Logistics.ProcessingRoute;
import com.ROLOS.Logistics.ReportAgent;
import com.ROLOS.Utils.HandyUtils;
import com.ROLOS.Utils.HashMapList;

import com.sandwell.JavaSimulation.Tester;

/**
 * Contract management for supply and demand
 */

public class FacilityGeneralManager extends FacilityManager {

	private static final ArrayList<FacilityGeneralManager> allInstances;

	
	// TODO contract priorities should be calculated based on facility's internal policies (i.e. higher cost first)
	// When a contract is inactive its priority is set to 0.
	private HashMapList<BulkMaterial,Contract> supplyContractList; //This facility is a supplier in the contract
	private HashMapList<BulkMaterial,Contract> demandContractList; //This facility is a buyer in the contract
	
	static {
		allInstances = new ArrayList<FacilityGeneralManager>(2);
	}
	
	{
		
	}
	
	public FacilityGeneralManager() {
		supplyContractList = new HashMapList<>();
		demandContractList = new HashMapList<>();		
	}
		
	@Override
	public void validate() {
		super.validate();
		
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();
		
		// populate the feedstock and output material list and facility's capacities for each output material based on 
		// main product's capacity level
		for (ArrayList<ProcessingRoute> eachProcessingRouteList : this.getFacility().getOperationsManager().getProcessingRoutesList().getValues()) {
			for (ProcessingRoute tempProcessingRoute: eachProcessingRouteList) {
				for (LogisticsEntity each : tempProcessingRoute.getProcessor()
						.getHandlingEntityTypeList()) {
					this.getFacility().addToStocksList((BulkMaterial) each,0,
							tempProcessingRoute.getCapacityRatio((BulkMaterial) each,
											tempProcessingRoute.getProcessor().getPrimaryProduct())
									* tempProcessingRoute.getProcessor().getPrimaryProductCapacity());
				}
				for (BulkMaterial each : tempProcessingRoute.getProcessor()
						.getOutfeedEntityTypeList()) {
					this.getFacility().addToStocksList(each,0,tempProcessingRoute.getCapacityRatio(each,
									tempProcessingRoute.getProcessor().getPrimaryProduct())
									* tempProcessingRoute.getProcessor().getPrimaryProductCapacity());
				}
			}
		}	
		
	}
	
	public static ArrayList<? extends FacilityGeneralManager> getAll() {
		synchronized (allInstances) {
			return allInstances;
		}
	}

	@Override
	public void kill() {
		super.kill();
		synchronized (allInstances) {
			allInstances.remove(this);
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	// GETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////
		
	public HashMapList<BulkMaterial, Contract> getDemandContractsList(){
		return demandContractList;
	}
	
	//TODO develop logic for contract priority!
	public int calcContractPriotiry(Contract contract){
		if (contract.getSupplier().equals(this))
			return 5;
		else
			return 5;
	}
	
	public void setContractPriority(Contract contract, int priority){
		if(this.equals(contract.getBuyer()))
			contract.setBuyerContractPriority(priority);
		else if(this.equals(contract.getSupplier()))
			contract.setSupplierContractPriority(priority);
	}
	
	// make fleets waiting for the inactive contract active
	public void reinstateContract(Contract contract){
		if(!contract.isActive())
			return;
		
		this.getFacility().getTransportationManager().scheduleTransportation(contract);
				
	}
	
	/**
	 * Will assign balanced amount so that all contracts will have proportional amount(based on contract amount)
	 * fullfilled
	 * @param contract
	 * @param facility
	 * @return
	 */
	public double getContractBalancedAmountToFullfill(Contract contract){
		BulkMaterial bulkMaterial = contract.getProduct();
		double totalContractAmounts = this.getFacility().getStockList().getValueFor(bulkMaterial, 1) - this.getFacility().getStockList().getValueFor(bulkMaterial, 2);
		return contract.getContractAmount()/totalContractAmounts;
	}
	
	public void voidContract(Contract contract){
		if(this.equals(contract.getBuyer()))
			demandContractList.remove(contract.getProduct(), contract);
		else if(this.equals(contract.getSupplier()))
			supplyContractList.remove(contract.getProduct(), contract);
	}
	
	public HashMapList<BulkMaterial,Contract> getSupplyContractsList(){
		return supplyContractList;
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// SETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	//  When adding contract/adjust unsatisfied in facility's stock list. use this to initiate defined contracts, assign fleet/etc
	public void addToContracts(Contract contract, boolean seller){
		if(seller){
			supplyContractList.add(contract.getProduct(), contract);
			this.getFacility().removeFromStocksList(contract.getProduct(), 2, contract.getContractAmount());

		}else{
			demandContractList.add(contract.getProduct(), contract);
			this.getFacility().removeFromStocksList(contract.getProduct(), 2, contract.getContractAmount());
			//reserve transportation capacity
			// TODO amount reserved is converted to amount per time unit for the contract's period meaning that a balanced transportation is assumed overtime
			this.getFacility().getTransportationManager().getTransportersList().add(contract.getTransporterProtoType(), 2, (contract.getContractAmount()/contract.getContractPeriod())*SimulationManager.getPlanningHorizon());
			
			// TODO when a contract is established for one of the mutually exclusive processes, unsatisfied demand
			// for infeeds of all other processes are adjusted
			// TODO assuming infeed material is unique to the process whose mutually exclusive processes are adjusted
			//find the infeed in process and adjust stocklist amounts for the mutually exclusive processes
			for (ArrayList<ProcessingRoute> eachProcessingRoute: this.getFacility().getOperationsManager().getProcessingRoutesList().getValues()) {
				for (ProcessingRoute each: eachProcessingRoute) {
					if(each.getProcessor().getHandlingEntityTypeList().contains(contract.getProduct()) &&
							!this.getFacility().getOperationsManager().getMutuallyExclusiveProcesses(each.getProcessor()).isEmpty()){
						// TODO assumes mutually exclusive processes use the same throughput schedule
						double tempAmount = contract.getContractAmount();
						//TODO assumes only one infeed is used!
						// amount of primary product potentially produced by the mutually exclusive process
						tempAmount *= each.getProcessor().getOutfeedRate(each.getProcessor().getPrimaryProduct())/each.getProcessor().getMaxRate(contract.getProduct());
																		
						for(ProcessingRoute eachMutuallyExclusiveRoute: this.getFacility().getOperationsManager().getMutuallyExclusiveProcesses(each.getProcessor())){
							if (each != eachMutuallyExclusiveRoute && Tester.greaterCheckTolerance(this.getFacility().getStockList().getValueFor((BulkMaterial) eachMutuallyExclusiveRoute.getProcessor().getHandlingEntityTypeList()
												.get(0),2), 0.0d)) {
								//TODO assumes only one infeed is used!
								eachMutuallyExclusiveRoute.getProcessor().getFacility().removeFromStocksList(
											(BulkMaterial) eachMutuallyExclusiveRoute.getProcessor().getHandlingEntityTypeList()
												.get(0),2,tempAmount* eachMutuallyExclusiveRoute.getProcessor()
																.getMaxRate((BulkMaterial) eachMutuallyExclusiveRoute.getProcessor().getHandlingEntityTypeList().get(0))
														/ eachMutuallyExclusiveRoute.getProcessor().getOutfeedRate(
																		eachMutuallyExclusiveRoute.getProcessor().getPrimaryProduct()));
							}
						}
					}
				}
			}
			
		}
		
		//Assign transportation capacity
		
	}
		
	
	//////////////////////////////////////////////////////////////////////////////////////
	// REMOVER METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	

}
