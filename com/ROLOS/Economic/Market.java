package com.ROLOS.Economic;

import java.util.ArrayList;
import java.util.Collections;

import com.ROLOS.ROLOSEntity;
import com.ROLOS.DMAgents.MarketManager;
import com.ROLOS.DMAgents.RouteManager;
import com.ROLOS.DMAgents.SimulationManager;
import com.ROLOS.Logistics.BulkMaterial;
import com.ROLOS.Logistics.Facility;
import com.ROLOS.Logistics.MovingEntity;
import com.jaamsim.input.InputAgent;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.EntityListInput;

import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Tester;

public class Market extends ROLOSEntity {
	
	@Keyword(description = "The product that this market regulates.", 
			example = "WoodchipsMarket Product { WoodChip }")
	private final EntityInput<BulkMaterial> product;
	
	@Keyword(description = "The list of facilities that participate in this market as sellers.", 
			example = "WoodchipsMarket Sellers { Bearn Hearst }")
	private final EntityListInput<Facility> sellers;
	
	@Keyword(description = "The list of facilities that participate in this market as buyers.", 
			example = "WoodchipsMarket Buyers { Bearn Hearst }")
	private final EntityListInput<Facility> buyers;
		
	private ArrayList<Facility> sellersList, buyersList;
	
	{
		product = new EntityInput<BulkMaterial>(BulkMaterial.class, "Product", "Key Inputs", null);
		this.addInput(product);
		
		sellers = new EntityListInput<>(Facility.class, "Sellers", "Key Inputs", null);
		this.addInput(sellers);
		
		buyers = new EntityListInput<>(Facility.class, "Buyers", "Key Inputs", null);
		this.addInput(buyers);
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
		InputAgent.processEntity_Keyword_Value(contract, "Transporter", offer.getTransporter().getName());
		InputAgent.processEntity_Keyword_Value(contract, "ContractPeriod", ((Double)SimulationManager.getPlanningHorizon()).toString() + " "+ "s");
		contract.setContractPrice(offer.getMarketOfferPrice());
		contract.setEstimatedTransportCost(offer.getEstimatedTransportCost());
		contract.setFlag(Entity.FLAG_GENERATED);

		//add to facilities' supply and demand contract lists
		contract.registerContract();
		offer.getBuyer().getGeneralManager().reinstateContract(contract);

		// adjust seller and buyers list
		if(Tester.equalCheckTolerance(0.0d,offer.getSeller().getStockList().getValueFor(product.getValue(), 2)))
			sellersList.remove(offer.getSeller());
		if(Tester.equalCheckTolerance(0.0d,offer.getBuyer().getStockList().getValueFor(product.getValue(), 2)))
			buyersList.remove(offer.getBuyer());
		
	}

	// This heuristic market model tries to assign maximum unit offer price, total amount and overall welfare is not considered!
	public void runSellersMarket(){
		int is, ib;
		ArrayList<MarketOffer> offersList= new ArrayList<>(5);
		while(!sellersList.isEmpty() && ! buyersList.isEmpty()){
			
			//populate all offers
			for(is=0; is< sellersList.size();){
 				if(Tester.equalCheckTolerance(sellersList.get(is).getStockList().getValueFor(this.getProduct(), 2),0.0d)){
					sellersList.remove(is);
					continue;
				}
				for(ib=0; ib< buyersList.size();){
					if(Tester.equalCheckTolerance(buyersList.get(ib).getStockList().getValueFor(this.getProduct(), 2),0.0d)){
						buyersList.remove(ib);
						continue;
					}
					// TODO assumes buyer is transporting and will remove from buyers list if transportation capacity is maxed out
					if (buyersList.get(ib).getTransportationManager().getLeastCostTransporter(product.getValue(),buyersList.get(ib), sellersList.get(is)) == null){
						buyersList.remove(ib);
						continue;
					}else{
						offersList.add(new MarketOffer(sellersList.get(is), buyersList.get(ib), 
								Tester.min(buyersList.get(ib).getStockList().getValueFor(product.getValue(), 2),
										sellersList.get(is).getStockList().getValueFor(product.getValue(), 2)),
								buyersList.get(ib).getStockList().getValueFor(product.getValue(), 7)));
						ib++;
					}
				}
				is++;
				if(buyersList.isEmpty() || sellersList.isEmpty())
					break;
			}
			if (!offersList.isEmpty()) {
				//select the highest offer
				Collections.sort(offersList);
				// if the highest offer is negative break
				if(Tester.lessCheckTolerance(offersList.get(0).getMarketOfferPrice(),0.0d))
					break;
				this.establishContracts(offersList.get(0));
				// clear and redo all market offers
				//TODO use better implementation to keep offers and only change the changed ones
				offersList.clear();
			}
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
	
	public class MarketOffer extends ROLOSEntity implements Comparable<MarketOffer>{
		private Facility seller, buyer;
		private MovingEntity transporter;
		
		private double amount, marketOfferPrice, estimatedTransportCost;
		public MarketOffer(Facility seller, Facility buyer, double amount, double offeredPrice) {
			this.seller = seller;
			this.buyer = buyer;
			// TODO this assumes buyer always transports
			transporter = buyer.getTransportationManager().getLeastCostTransporter(product.getValue(),buyer,seller);
			this.amount = Tester.min(amount,buyer.getTransportationManager().getTransportersList().getValueFor(transporter, 1)-buyer.getTransportationManager().getTransportersList().getValueFor(transporter, 2));
			estimatedTransportCost = buyer.getTransportationManager().estimateTransportationCostonRoute(transporter, getProduct(), 
					RouteManager.transportationNetworkManager.getRoute(buyer, seller, transporter));
			marketOfferPrice = offeredPrice - estimatedTransportCost; 
		}
		
		public MovingEntity getTransporter(){
			return transporter;
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
			return this.marketOfferPrice >= o.getMarketOfferPrice() ? -1 : +1;
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

	}

}
