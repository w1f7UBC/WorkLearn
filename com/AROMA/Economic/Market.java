package com.AROMA.Economic;

import java.util.ArrayList;
import java.util.Collections;

import com.AROMA.AROMAEntity;
import com.AROMA.DMAgents.MarketManager;
import com.AROMA.DMAgents.SimulationManager;
import com.AROMA.Logistics.BulkMaterial;
import com.AROMA.Logistics.Facility;
import com.AROMA.Logistics.MovingEntity;
import com.AROMA.Logistics.Route;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.InputAgent;
import com.sandwell.JavaSimulation.Entity;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Tester;

public class Market extends AROMAEntity {
	
	@Keyword(description = "The product that this market regulates.", 
			example = "WoodchipsMarket Product { WoodChip }")
	private final EntityInput<BulkMaterial> product;
	
	@Keyword(description = "The list of facilities that participate in this market as sellers.", 
			example = "WoodchipsMarket Sellers { Bearn Hearst }")
	private final EntityListInput<Facility> sellers;
	
	@Keyword(description = "The list of facilities that participate in this market as buyers.", 
			example = "WoodchipsMarket Buyers { Bearn Hearst }")
	private final EntityListInput<Facility> buyers;
	
	//TODO add inventory carry over logic for facilities
	@Keyword(description = "Whether market tries to clear supplies. if true, negative offer prices will be accepted."
			+ "Default is true.", 
			example = "WoodchipsMarket ClearSupply { FALSE }")
	private final BooleanInput clearSupply;
		
	private ArrayList<Facility> sellersList, buyersList;
	
	{ 
		product = new EntityInput<BulkMaterial>(BulkMaterial.class, "Product", "Key Inputs", null);
		this.addInput(product);
		
		sellers = new EntityListInput<>(Facility.class, "Sellers", "Key Inputs", null);
		this.addInput(sellers);
		
		buyers = new EntityListInput<>(Facility.class, "Buyers", "Key Inputs", null);
		this.addInput(buyers);
		
		clearSupply = new BooleanInput("ClearSupply", "Key Inputs", true);
		this.addInput(clearSupply);
	}
	
	public Market() {
		buyersList = new ArrayList<Facility>(5);
		sellersList = new ArrayList<Facility>(5);
	}
	
	@Override
	public void validate() {
		MarketManager.marketManager.addToMarket(this);
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////////////////////
	// GETTER METHODS
	// ////////////////////////////////////////////////////////////////////////////////////////////////////
	public BulkMaterial getProduct(){
		return product.getValue();
	}
	
	public ArrayList<Facility> getSellersList(){
		return sellersList;
	}
	
	public ArrayList<Facility> getBuyersList(){
		return buyersList;
	}
	
	public void establishContracts(MarketOffer offer){
		// generate a new contract
		Contract contract = InputAgent.defineEntityWithUniqueName(Contract.class,"Contract/"+ offer.getSeller().getName()+"-"+offer.getBuyer().getName() +"-"+product.getValue().getName(), false);
		InputAgent.processEntity_Keyword_Value(contract, "Supplier", offer.getSeller().getName());
		InputAgent.processEntity_Keyword_Value(contract, "Product", product.getValueString());
		InputAgent.processEntity_Keyword_Value(contract, "Buyer", offer.getBuyer().getName());
		InputAgent.processEntity_Keyword_Value(contract, "ContractAmount", ((Double)offer.getAmount()).toString() + " "+ product.getValue().getEntityUnitString());
				
		// TODO this assumes buyer is always the delivery company
		// TODO assumes transportation is not modeled for the established contracts through market!
		InputAgent.processEntity_Keyword_Value(contract, "DeliveryCompany", offer.getBuyer().getName());
		InputAgent.processEntity_Keyword_Value(contract, "Transporter", offer.getRoute().getMovingEntitiesList().get(0).getName());
		InputAgent.processEntity_Keyword_Value(contract, "ContractPeriod", ((Double)SimulationManager.getPlanningHorizon()).toString() + " "+ "s");
		
		contract.setContractPrice(offer.getMarketOfferPrice());
		contract.setAssignedRoute(offer.getRoute());
		contract.setEstimatedTransportCost(offer.getEstimatedTransportCost());
		contract.setFlag(Entity.FLAG_GENERATED);

		//add to facilities' supply and demand contract lists
		contract.registerContract();
	//	offer.getBuyer().getGeneralManager().reinstateContract(contract);

		// adjust seller and buyers list
		if(Tester.equalCheckTolerance(offer.getSeller().getStockList().getValueFor(product.getValue(), 13),offer.getSeller().getStockList().getValueFor(product.getValue(), 4)))
			sellersList.remove(offer.getSeller());
		if(Tester.equalCheckTolerance(0.0d,offer.getBuyer().getStockList().getValueFor(product.getValue(), 3)))
			buyersList.remove(offer.getBuyer());
		
	}
	
	public ArrayList<MarketOffer> setOffers(){
		int is, ib;
		ArrayList<MarketOffer> offersList= new ArrayList<>(5);
		//populate all offers
		for(is=0; is< sellersList.size();){
				if(Tester.equalCheckTolerance(sellersList.get(is).getStockList().getValueFor(this.getProduct(), 4),sellersList.get(is).getStockList().getValueFor(this.getProduct(), 13))){
				sellersList.remove(is);
				continue;
			}
			for(ib=0; ib< buyersList.size();){
				if(Tester.equalCheckTolerance(buyersList.get(ib).getStockList().getValueFor(this.getProduct(), 3),0.0d)){
					buyersList.remove(ib);
					continue;
				}
				// TODO assumes seller is transporting and will remove from buyers list if transportation capacity is maxed out
				// TODO URGENT! transportation cost cap should be set properly !
				
				// if it's the first time going through the buyers list, set buyers breakeaven prices!
				if(is == 0)
					buyersList.get(ib).getFinancialManager().setFeedstockBreakevenPrice(this.getProduct());
				
				if (sellersList.get(is).getTransportationManager().getLeastCostTranspotationRoute(product.getValue(), sellersList.get(is), buyersList.get(ib), buyersList.get(ib).getStockList().getValueFor(product.getValue(), 9),null) == null){
					ib++;
					continue;
				}else{
										
					offersList.add(new MarketOffer(sellersList.get(is), buyersList.get(ib), 
							buyersList.get(ib).getStockList().getValueFor(product.getValue(), 9)));
					ib++;
				}
			}
			is++;
			if(buyersList.isEmpty() || sellersList.isEmpty())
				break;
		}
		return offersList;
	}
	
	// This heuristic market model tries to assign maximum unit offer price, total amount and overall welfare is not considered!
	public void runSellersMarket(){
		int is, ib;
		// TODO Satisfies demand internally if infeed is produced within the facility
		for(Facility eachBuyer: buyersList){
			eachBuyer.getOperationsManager().satisfyDemandInternally(product.getValue());
		}
					
		ArrayList<MarketOffer> offersList= setOffers();
		//Sort offers higher to lowest
		Collections.sort(offersList);
		MarketOffer tempOffer = null;
		
		while(!sellersList.isEmpty() && ! buyersList.isEmpty() && !offersList.isEmpty()){
			//Sellect highest offer
			tempOffer = offersList.get(0);
			
			if(Tester.equalCheckTolerance(tempOffer.getSeller().getStockList().getValueFor(this.getProduct(), 4),tempOffer.getSeller().getStockList().getValueFor(this.getProduct(), 13))){
				sellersList.remove(tempOffer.getSeller());
				offersList.remove(tempOffer);
				continue;
			} else if(Tester.equalCheckTolerance(tempOffer.getBuyer().getStockList().getValueFor(this.getProduct(), 3),0.0d)){
				buyersList.remove(tempOffer.getBuyer());
				offersList.remove(tempOffer);
				continue;
			} 
			
			if(!clearSupply.getValue() && Tester.lessCheckTolerance(tempOffer.getMarketOfferPrice(),0.0d))
				break;
					
			// set offers amount
			//TODO offer amount is set here to avoid readjusting offer's amount every time a contract is established
			MovingEntity tempTransporter = tempOffer.getRoute().getMovingEntitiesList().get(0);
			tempOffer.setAmount(Tester.min(tempOffer.getSeller().getTransportationManager().getTransportersList().getValueFor(tempTransporter, 0)-tempOffer.getSeller().getTransportationManager().getTransportersList().getValueFor(tempTransporter, 1),
									tempOffer.getBuyer().getStockList().getValueFor(product.getValue(), 3),
									//amount seller hasn't sold yet
									tempOffer.getSeller().getStockList().getValueFor(product.getValue(), 13)-
									tempOffer.getSeller().getStockList().getValueFor(product.getValue(), 4)));
			
			if(Tester.greaterCheckTolerance(tempOffer.getAmount(), 0.0d))
				this.establishContracts(tempOffer);
			
			//TODO use better implementation to keep offers and only change the changed ones
			offersList.remove(tempOffer);	
			tempOffer.kill();
		}
	}

	/**
	 * populates sellers and buyers list at the begining of running the market
	 */
	public void populateLists(){
		buyersList.clear();
		sellersList.clear();
		buyersList.addAll(buyers.getValue());
		sellersList.addAll(sellers.getValue());
	}
	
	public class MarketOffer extends AROMAEntity implements Comparable<MarketOffer>{
		private Facility seller, buyer;
		private Route route;
		
		private double amount, marketOfferPrice, estimatedTransportCost;
		public MarketOffer(Facility seller, Facility buyer, double offeredPrice) {
			this.seller = seller;
			this.buyer = buyer;
			// TODO this assumes seller always transports
			// TODO URGENT! add proposer transportaion cost cap!
			route = seller.getTransportationManager().getLeastCostTranspotationRoute(product.getValue(), seller, buyer, offeredPrice,null);
			estimatedTransportCost = route.estimateTransportationCostonRoute(product.getValue(),true);
			marketOfferPrice = offeredPrice - estimatedTransportCost; 
		}
		
		public Route getRoute(){
			return route;
		}
		
		public double getEstimatedTransportCost(){
			return estimatedTransportCost;
		}
		
		public double getMarketOfferPrice(){
			return marketOfferPrice;
		}
		
		// descending comparator
		@Override
		public int compareTo(MarketOffer o) {
			
			int tempCompare =  Double.compare(o.getMarketOfferPrice(), this.marketOfferPrice);
			if(tempCompare != 0)
				return tempCompare;
			
			return (this.getEntityNumber() > o.getEntityNumber() ? -1 :
	               (this.getEntityNumber() == o.getEntityNumber() ? 0 : 1));
		}
		
		public Facility getSeller() {
			return seller;
		}

		public Facility getBuyer() {
			return buyer;
		}

		public double getAmount() {
			return amount;
		}
		
		public void setAmount(double amount){
			this.amount = amount;
		}
	}

}
