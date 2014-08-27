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
		for (ArrayList<ProcessingRoute> eachProcessingRouteList : this.getFacility().getOperationsManager().getProcessingRoutesListOutfeed().getValues()) {
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
	
	public double getBalancedAmountToFullfill(Contract contract){
		double totalRemainingSupplyContractsAmount = 
				this.getFacility().getStockList().getValueFor(contract.getProduct(), 13) -
				this.getFacility().getStockList().getValueFor(contract.getProduct(), 11);
		
		double totalAvailable = this.getFacility().getStockList().getValueFor(contract.getProduct(), 6);
		double amountToAssign = Tester.min(contract.getUnfulfilledAmount(),totalAvailable * contract.getUnfulfilledAmount()/totalRemainingSupplyContractsAmount);
		
		return amountToAssign;
	}
	
	// make fleets waiting for the inactive contract active
	public void reinstateContract(Contract contract){

		if(!contract.isActive())
				return;
		
		// TODO uses a balance transportation when supplier has multiple supply contracts. 
		this.getFacility().getTransportationManager().scheduleTransportation(contract,this.getBalancedAmountToFullfill(contract));
						
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
			this.getFacility().addToStocksList(contract.getProduct(), 4, contract.getContractAmount());

		}else{
			demandContractList.add(contract.getProduct(), contract);
			this.getFacility().removeFromStocksList(contract.getProduct(), 3, contract.getContractAmount());
			//reserve transportation capacity
			// TODO amount reserved is converted to amount per time unit for the contract's period meaning that a balanced transportation is assumed overtime
			this.getFacility().getTransportationManager().getTransportersList().add(contract.getTransporterProtoType(), 1, (contract.getContractAmount()/contract.getContractPeriod())*SimulationManager.getPlanningHorizon());
			//add to realized production 
			this.getFacility().getOperationsManager().updateRealizedProduction(contract.getProduct(), contract.getContractAmount());
			
			//adjust infeed level for other mutually exclusive processes
			this.getFacility().getOperationsManager().adjustMutuallyExclusiveProcessesDemands(contract.getProduct(), contract.getContractAmount());
			
		}
		
		//Assign transportation capacity
		
	}
		
	
	//////////////////////////////////////////////////////////////////////////////////////
	// REMOVER METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	

}
