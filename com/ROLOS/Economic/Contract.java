package com.ROLOS.Economic;

import java.util.ArrayList;

import com.ROLOS.ROLOSEntity;
import com.ROLOS.DMAgents.SimulationManager;
import com.ROLOS.Logistics.BulkMaterial;
import com.ROLOS.Logistics.Facility;
import com.ROLOS.Logistics.Fleet;
import com.ROLOS.Logistics.MovingEntity;
import com.ROLOS.Logistics.Stockpile;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.ErrorException;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.Tester;

public class Contract extends ROLOSEntity {
	private static final ArrayList<Contract> allInstances;

	/**
	 * demand and supply is from facility's point of view. i.e. demand list keeps suppliers and demanded products information
	 * and supplylist keeps buyers and produced (supplied) products information.
	 */
	@Keyword(description = "the contracted company to supply demand.", 
			example = "WoodchipContract Supplier { LedcorSawMill1  }")
	private final EntityInput<Facility> supplier;
	
	@Keyword(description = "Contracted product. ", 
			example = "WoodchipContract Product { WoodChip }")
	private final EntityInput<BulkMaterial> product;
	
	@Keyword(description = "Contract Amount for the whole period", 
			example = "WoodchipContract ContractAmount { 100 kt }")
	private final ValueInput contractAmount;
	
	@Keyword(description = "Contract period", 
			example = "WoodchipContract ContractPeriod { 5 yr }")
	private final ValueInput contractPeriodInput;
	
	@Keyword(description = "Contract start date and time", 
			example = "WoodchipContract ContractPeriod { '2014-01-20 00:00:00' }")
	private final StringInput contractStartTimeInput;
	
	@Keyword(description = "Contract end date.", 
			example = "WoodchipContract ContractPeriod { '2014-01-25 00:00:00' }")
	private final StringInput contractEndTimeInput;
	
	@Keyword(description = "Whether transportation is explicitly modeled. default is false", 
			example = "Silverdale TransportationModeled { True }")
	private final BooleanInput transportationModeled;
		
	@Keyword(description = "Company(Facility) responsible for delivery ", 
			example = "WoodchipContract DeliveryCompany { Ledcor }")
	private final EntityInput<Facility> deliveryCompany;
	
	@Keyword(description = "Prototype of the dedicated transporter (identical to fleet's prototype) for this contract.", 
			example = "WoodchipContract Transporter { Truck1 }")
	private final EntityInput<MovingEntity> transporter;
	
	@Keyword(description = "the contracted company to buy the product. ", 
			example = "WoodchipContract Buyer { TembecPulpMill1 }")
	private final EntityInput<Facility> buyer;
	
	private ArrayList<Fleet> dedicatedFleetsList;
	private double fulfilledAmount;
	// Amount of cargo capacity currently reserved for loading/unloading 
	private double reservedForFullfilling;
	//Whether this contract is currently active (i.e. supplier has capacity in supplying stock piles and buyer has capacity in receiving stockpiles, 
	// or fullfilled+reserved equals contract amount and therefore waiting to be ratified)
	private boolean activeForScheduling, activeSupplier, activeBuyer; 
	private int supplierContractPriority; 	// Default is 5
	private int buyerContractPriority;		// Default is 5
	private double estimatedTransportCost; // estimated transportation cost per unit of product from supplier to buyer
	private double price;					// price per unit of product, transportation excluded 
	
	static {
		allInstances = new ArrayList<Contract>();
	}
	
	{
		supplier = new EntityInput<>(Facility.class, "Supplier", "Key Inputs", null);
		this.addInput(supplier);
		
		product = new EntityInput<>(BulkMaterial.class, "Product", "Key Inputs", null);
		this.addInput(product);
		
		contractAmount = new ValueInput("ContractAmount", "Key Inputs", Double.POSITIVE_INFINITY);
		this.addInput(contractAmount);
		
		contractPeriodInput = new ValueInput("ContractPeriod", "Key Inputs", Double.POSITIVE_INFINITY);
		contractPeriodInput.setUnitType(TimeUnit.class);
		this.addInput(contractPeriodInput);

		transportationModeled = new BooleanInput("TransportationModeled", "Key Inputs", false);
		this.addInput(transportationModeled);
		
		deliveryCompany = new EntityInput<>(Facility.class, "DeliveryCompany", "Key Inputs", null);
		this.addInput(deliveryCompany);
		
		buyer = new EntityInput<>(Facility.class, "Buyer", "Key Inputs", null);
		this.addInput(buyer);
		
		contractStartTimeInput = new StringInput("StartTime", "Key Inputs", null);
		this.addInput(contractStartTimeInput);

		contractEndTimeInput =  new StringInput("EndTime", "Key Inputs", null);
		this.addInput(contractEndTimeInput);
		
		transporter = new EntityInput<MovingEntity>(MovingEntity.class, "Transporter", "Key Inputs", null);
		this.addInput(transporter);
		
	}
	
	public Contract() {
		synchronized (allInstances) {
			allInstances.add(this);
		}
		
		fulfilledAmount = 0.0d;
		supplierContractPriority = 5;
		buyerContractPriority = 5;
		dedicatedFleetsList = new ArrayList<>(1);
	}
	
	public static ArrayList<? extends Contract> getAll() {
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
		
	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);
		if(in == product)
			contractAmount.setUnitType(product.getValue().getEntityUnit());
	}
	
	@Override
	public void validate() {
		super.validate();
		
		if(supplier.getValue() == null )
			throw new ErrorException("Supplier should be defined for %s", this.getName());
		if(buyer.getValue() == null)
			throw new ErrorException("Buyer should be defined for %s", this.getName());
		if(contractAmount.getValue() == null)
			throw new ErrorException("ContractAmount should be defined for %s", this.getName());
		if(deliveryCompany.getValue() == null)
			throw new ErrorException("DeliveryCompany should be defined for %s", this.getName());
		
		registerContract();
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	// GETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	public boolean isActive(){
		return (activeSupplier && activeBuyer && activeForScheduling) ? true : false;
	}
		
	public Facility getSupplier() {
		return supplier.getValue();
	}

	public BulkMaterial getProduct() {
		return product.getValue();
	}

	public double getContractPeriod() {
		return contractPeriodInput.getValue();
	}
	
	public double getContractPrice(){
		return this.price;
	}

	public Facility getDeliveryCompany() {
		return deliveryCompany.getValue();
	}

	//TODO refactor to look for loading bay at origin and destination and bulkcargo on the transporters to set value for this
	public boolean isTransportationModeled(){
		return transportationModeled.getValue();
	}
	
	public Facility getBuyer() {
		return buyer.getValue();
	}

	public double getFulfilledAmount(){
		return fulfilledAmount;
	}
	
	public double getUnfulfilledAmount(){
		return contractAmount.getValue() - fulfilledAmount;
	}
	
	public ArrayList<Fleet> getFleetsList(){
		return dedicatedFleetsList;
	}
	
	/**
	 * TODO add third party fleet management
	 * @return if facility is neither the buyer nor the supplier, will return 5 (default priority!)
	 */
	public int getContractPriority(Facility facility){
		if (facility.equals(buyer.getValue()))
			return buyerContractPriority;
		else if (facility.equals(supplier.getValue()))
			return supplierContractPriority;
		else
			return 5;
	}
	
	public int getSupplierContractPriority() {
		return supplierContractPriority;
	}

	public int getBuyerContractPriority() {
		return buyerContractPriority;
	}
	
	public MovingEntity getTransporterProtoType(){
		return transporter.getValue();
	}

	public double getContractAmount() {
		return contractAmount.getValue();
	}

	public ArrayList<Fleet> getDedicatedFleetsList() {
		return dedicatedFleetsList;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	// SETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	public void setContractPrice(double price){
		this.price = price;
	}
	
	public void reserveForFulfilling(double amountToReserve){
		reservedForFullfilling += amountToReserve;
		if(Tester.greaterOrEqualCheckTolerance(fulfilledAmount+reservedForFullfilling,contractAmount.getValue()))
				activeForScheduling = false;
	}
	
	public void removeFromReservedForFulfilling(double amountToRemove){
		reservedForFullfilling -= amountToRemove;
		if(Tester.lessCheckTolerance(fulfilledAmount+reservedForFullfilling,contractAmount.getValue())){
				activeForScheduling = true;
		/*		if(this.isActive()){
					buyer.getValue().getGeneralManager().reinstateContract(this);
					supplier.getValue().getGeneralManager().reinstateContract(this);
				}*/
		}
	}
	
	public void registerContract(){
		supplier.getValue().getGeneralManager().addToContracts(this,true);
		buyer.getValue().getGeneralManager().addToContracts(this,false);
				
		// assuming a new contract starts ready for scheduling
		this.activeForScheduling = true;
		if(Tester.greaterCheckTolerance(buyer.getValue().getStockList().getValueFor(getProduct(), 5)
				, buyer.getValue().getStockList().getValueFor(getProduct(), 6))){
			activeBuyer = true;
		}
		
		if(Tester.greaterCheckTolerance(supplier.getValue().getStockList().getValueFor(getProduct(), 6)
				, 0.0d)){
			activeSupplier = true;
		}
		
		SimulationManager.printContractReport(this);

	}
	
	public void setFacilityActiveness(boolean active,Facility facility){
		
		//TODO add logic for inactivating facility!
					
		if(facility.equals(buyer))
			this.activeBuyer = active;
		else
			this.activeSupplier = active;

		if(active && this.isActive())
			supplier.getValue().getGeneralManager().reinstateContract(this);
		
	}
	
	public void addToFulfilledAmount(double fulfilledAmount) {
		this.fulfilledAmount += fulfilledAmount;
		
		// update buyer and supplier's fullfilled contract amounts
		this.getSupplier().addToStocksList(this.getProduct(), 11, fulfilledAmount);
		this.getBuyer().addToStocksList(this.getProduct(), 12, fulfilledAmount);
				
		if(Tester.greaterOrEqualCheckTolerance(fulfilledAmount, contractAmount.getValue()))
			this.voidContract();
	}

	public void setSupplierContractPriority(int supplierContractPriority) {
		this.supplierContractPriority = supplierContractPriority;
	}

	public void setBuyerContractPriority(int buyerContractPriority) {
		this.buyerContractPriority = buyerContractPriority;
	}
	
	public void voidContract(){
		buyer.getValue().getGeneralManager().voidContract(this);
	// TODO just call back moving entities and keep dormant fleet and contracts
			supplier.getValue().getGeneralManager().voidContract(this);
		activeBuyer= false;
		activeForScheduling = false;
		activeSupplier= false;
		
		for (Fleet eachFleet: dedicatedFleetsList){
			eachFleet.removeContract(this);
		}

		dedicatedFleetsList.clear();
		
		//TODO Does killing contract interfere with reporting and reset of the run or not?
		this.kill();
	}

	public double getEstimatedTransportCost() {
		return estimatedTransportCost;
	}

	public void setEstimatedTransportCost(double estimatedTransportCost) {
		this.estimatedTransportCost = estimatedTransportCost;
	}
	
	
}
