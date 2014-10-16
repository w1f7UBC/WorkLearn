package com.AROMA.Logistics;

import com.AROMA.JavaSimulation.Tester_Rolos;
import com.AROMA.Utils.HandyUtils;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.MassUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.VolumeUnit;


import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Tester;

public class BulkMaterialStorage extends BulkHandlingLinkedEntity {

	@Keyword(description = "Mass based cargo capacity" 
			, example = "Trailer MassCargo { 40 t } ")
	private final ValueInput massCargo;
	
	@Keyword(description = "Volumetric cargo capacity" 
			, example = "Trailer VolumetricCargo { 60 m3 } ")
	private final ValueInput volumetricCargo;
	
	@Keyword(description = "initial content" 
			, example = "Trailer InitialContent { 10 t } ")
	private final ValueInput initialContent;
	
	@Keyword(description = "Delay that applies after the storage is emptied out.",
	         example = "Stockpile  ExhaustionDelay { ReclaimerMovementDelay }")
	private final EntityInput<Delay> exhaustionDelay;
	
	private double massContent, volumetricContent;
		
	private double lastContentUpdateTime;
	
	{

		massCargo = new ValueInput("MassCargo", "Key Inputs", Double.POSITIVE_INFINITY);
		massCargo.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		massCargo.setUnitType(MassUnit.class);
		this.addInput(massCargo);
		
		volumetricCargo = new ValueInput("VolumetricCargo", "Key Inputs", Double.POSITIVE_INFINITY);
		volumetricCargo.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		volumetricCargo.setUnitType(VolumeUnit.class);
		this.addInput(volumetricCargo);
		
		initialContent = new ValueInput("InitialContent", "Key Inputs", null);
		initialContent.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(initialContent);
		
		exhaustionDelay = new EntityInput<Delay>(Delay.class, "Exhaustiondelay", "Key Inputs", null);
		this.addInput(exhaustionDelay);
	}
	
	public BulkMaterialStorage() {
		
		massContent = 0;
		volumetricContent = 0;
		lastContentUpdateTime = 0.0d;
	}
	
	@Override
	public void validate() {
		super.validate();
		
		// interpret capacity based on mass/volumetric capacity and capacity
		this.setFlag(FLAG_GENERATED);
		BulkMaterial tempBulkMaterial = (BulkMaterial) this.getHandlingEntityTypeList().get(0);
		// TODO check if Double.POSITIVE_INFINITY works
		if (this.getCapacity(tempBulkMaterial) == Double.POSITIVE_INFINITY ){
			if(tempBulkMaterial.getEntityUnit().equals(VolumeUnit.class)){
				if (volumetricCargo.getValue() == null)
					throw new ErrorException("Neither capacity nor volumetric capacity are set for %s!", this.getName());
				InputAgent.processEntity_Keyword_Value(this, "Capacity", volumetricCargo.getValueString());
			} else if(tempBulkMaterial.getEntityUnit().equals(MassUnit.class)){
				if (massCargo.getValue() == null)
					throw new ErrorException("Neither capacity nor mass-based capacity are set for %s!", this.getName());
				InputAgent.processEntity_Keyword_Value(this, "Capacity", massCargo.getValueString());
			}
		} else {
			if (tempBulkMaterial.getEntityUnit().equals(VolumeUnit.class)){
				if (volumetricCargo.getValue() == Double.POSITIVE_INFINITY)
					InputAgent.processEntity_Keyword_Value(this, "VolumetricCargo", ((Double)this.getCapacity(tempBulkMaterial)).toString()+" m3");
				else
					if(!Tester.equalCheckTolerance(volumetricCargo.getValue(), this.getCapacity(tempBulkMaterial)))
						throw new ErrorException("VolumetricCargo and Capacity don't match for %s in %s", tempBulkMaterial.getName(),this.getName());
			} else if(tempBulkMaterial.getEntityUnit().equals(MassUnit.class)){
				if (massCargo.getValue() == Double.POSITIVE_INFINITY)
					InputAgent.processEntity_Keyword_Value(this, "MassCargo", ((Double)this.getCapacity(tempBulkMaterial)).toString()+" t");
				else
					if(!Tester.equalCheckTolerance(massCargo.getValue(), this.getCapacity(tempBulkMaterial)))
						throw new ErrorException("MassCargo and Capacity don't match for %s in %s", tempBulkMaterial.getName(),this.getName());
			}			
		}
		this.clearFlag(FLAG_GENERATED);
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();
		// add initial content
		if(initialContent.getValue() != null)
			this.addToCurrentlyHandlingEntityList(this.getHandlingEntityTypeList().get(0), initialContent.getValue());
	
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	// GETTER AND SETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////
	
	
	public double getMassBasedContent() {
		return massContent;
	}

	public double getVolumetricBasedContent() {
		return volumetricContent;
	}

	public double getMassBasedCargo() {
		return massCargo.getValue();
	}

	public double getVolumetricBasedCargo() {
		return volumetricCargo.getValue();
	}
	
	public Delay getExhaustionDelay(){
		return exhaustionDelay.getValue();
	}
	
	/**
	 * @return the remaining mass based capacity for the passed bulkmaterial if this cargo is currently handling this type. 
	 * The returned value is based on the entity's natural unit. If the density of the bulk material is defined
	 * it checks both volumetric and mass based remaining capacities and returns the minimum of the two.
	 */
	public double getRemainingCapacity(BulkMaterial bulkMaterial){
		if(this.getCurrentlyHandlingList().getEntityList().isEmpty()){
			if(!this.getHandlingEntityTypeList().contains(bulkMaterial.getProtoTypeEntity()))
				return 0.0d;
		} else {
			if(this.getCurrentlyHandlingList().getEntityList().get(0).getProtoTypeEntity() != bulkMaterial.getProtoTypeEntity())
				return 0.0d;
		}
			
		double tempDensity = bulkMaterial.getDensity();
		if(tempDensity == 0.0d && !this.getCurrentlyHandlingList().isEmpty())
			tempDensity = ((BulkMaterial) this.getCurrentlyHandlingList().getEntityList().get(0)).getDensity();
		double tempRemainingCapacity=0.0d;
		if(bulkMaterial.getEntityUnit().equals(VolumeUnit.class)){
			tempRemainingCapacity = volumetricCargo.getValue() - volumetricContent;
			if(tempDensity != 0.0d){
				tempRemainingCapacity = Tester.min(tempRemainingCapacity,(massCargo.getValue() - massContent)/tempDensity);
			}
		}
		else{
			tempRemainingCapacity = massCargo.getValue() - massContent;
			if(tempDensity != 0.0d ){
				tempRemainingCapacity = Tester.min(tempRemainingCapacity,(volumetricCargo.getValue() - volumetricContent)*tempDensity);
			}
		}
		return tempRemainingCapacity;
	}
	
	public void setContentUnits(Class<? extends Unit> unit){
		initialContent.setUnitType(unit);
	}
	
	
	// ////////////////////////////////////////////////////////////////////////////////////
	// ADDERS AND REMOVERS
	// ////////////////////////////////////////////////////////////////////////////////////

	/**
	 * updates volume based on content's density and mass values. Called when natural unit of content is mass.
	 */
	public void updateVolumeContent(){
		BulkMaterial content;
		if (this.getCurrentlyHandlingAmount() != 0.0d){
			content = (BulkMaterial) this.getCurrentlyHandlingList().getEntityList().get(0);
			if(content.getDensity() != 0.0d)
				volumetricContent = massContent / content.getDensity();
		}
	}
	
	/**
	 * updates mass based on content's density and volume values. Called when natural unit of content is volume.
	 */
	public void updateMassContent(){
		BulkMaterial content;
		if (this.getCurrentlyHandlingAmount() != 0.0d){
			content = (BulkMaterial) this.getCurrentlyHandlingList().getEntityList().get(0);
			if(content.getDensity() != 0.0d)
				massContent = volumetricContent * content.getDensity();
		}
	}
	
	/**
	 * BulkMaterialStorages only handle one BulkMaterial type at each time
	 */
	@Override
	public <T1 extends LogisticsEntity> void addToCurrentlyHandlingEntityList(T1 entityToAdd, double amountToAdd) {
		BulkMaterial materialToAdd = (BulkMaterial) entityToAdd;
		
		if (!this.getCurrentlyHandlingList().isEmpty() && !(((BulkMaterial) this.getCurrentlyHandlingList().getEntityList().get(0).getProtoTypeEntity()).equals(materialToAdd.getProtoTypeEntity()) || this.getCurrentlyHandlingList().size()>1))
			throw new ErrorException("%s currently carries %s. Attempt was made to add %s at time: %f", this.getName(), this.getCurrentlyHandlingList().getEntityList().get(0), materialToAdd.getName(), this.getCurrentTime());
	
		//if storage doesn't carry any material, create a new one; if already carries material, assumes that added material is addable
		if(this.getCurrentlyHandlingList().isEmpty()){
			materialToAdd = EntityGenerator.generateEntity(materialToAdd,true);
			super.addToCurrentlyHandlingEntityList(materialToAdd, amountToAdd);
		}else{
			super.addToCurrentlyHandlingEntityList(this.getCurrentlyHandlingList().getEntityList().get(0), amountToAdd);
		}
		
		if (materialToAdd.getEntityUnit().equals(VolumeUnit.class)) {
			volumetricContent += amountToAdd;	
			linearlyBlendMaterial(materialToAdd, amountToAdd);
			updateMassContent();
		} else {
			massContent += amountToAdd;
			linearlyBlendMaterial(materialToAdd, amountToAdd);
			updateVolumeContent();
		}		
	}
	
	@Override
	public <T extends LogisticsEntity> void removeFromCurrentlyHandlingEntityList(T bulkMaterialToRemove, double amountToRemove) {
		if(!this.getCurrentlyHandlingList().isEmpty() && !this.getCurrentlyHandlingList().getEntityList().get(0).getProtoTypeEntity().equals(bulkMaterialToRemove.getProtoTypeEntity()))
			throw new ErrorException("Attempt was made to remove %s from %s even though it was carrying %s at time %f", bulkMaterialToRemove.getProtoTypeEntity().getName(),
					this.getName(), this.getCurrentlyHandlingList().getEntityList().get(0).getProtoTypeEntity().getName(), getSimTime());
		
		//Assumes bulkmaterialtoremove is similar to the currently carrying bulk material
		BulkMaterial tempBulkMaterial = (BulkMaterial) this.getCurrentlyHandlingList().getEntityList().get(0);
		super.removeFromCurrentlyHandlingEntityList(tempBulkMaterial, amountToRemove);
		
		if (bulkMaterialToRemove.getEntityUnit().equals(VolumeUnit.class)) {
			volumetricContent -= amountToRemove;	
			updateMassContent();
			if(Tester.equalCheckTolerance(0.0d, volumetricContent) || 
					(tempBulkMaterial.getDensity() != 0.0d && 
					Tester.equalCheckTolerance(0.0d, massContent))){
				massContent = 0.0d;
				volumetricContent = 0.0d;
				this.getCurrentlyHandlingList().remove(bulkMaterialToRemove);
				tempBulkMaterial.kill();
			}
		} else {
			massContent -= amountToRemove;
			updateVolumeContent();
			if(Tester.equalCheckTolerance(0.0d, massContent) || 
					(tempBulkMaterial.getDensity() != 0.0d && 
					Tester.equalCheckTolerance(0.0d, volumetricContent))){
				massContent = 0.0d;
				volumetricContent = 0.0d;
				this.getCurrentlyHandlingList().remove(bulkMaterialToRemove);
				tempBulkMaterial.kill();
			}
		}		
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	// UPDATER METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	public void setLastContentUpdateTime(double currentTime){
		lastContentUpdateTime = currentTime;
	}
	
	/**
	 * This method should be called after the addToCurrentlyHandlingEntity so that the current amount reflects the amountToAdd (for ratio calculation)
	 * <br> Sets cargo quality variables based on a linear blending logic. 
	 * If blend's values are zero, the added material's values will be assigned to the blend.
	 * (added amount/contained amount)*parameter
	 * @param bulkMaterialToAdd the bulk material that is being added to the blend
	 * @param amountToAdd the amount to add
	 */
	public void linearlyBlendMaterial(BulkMaterial bulkMaterialToAdd, double amountToAdd){
		
		double addedMaterialRatio = amountToAdd/this.getCurrentlyHandlingAmount();
		if (addedMaterialRatio == Double.POSITIVE_INFINITY)
			return;
		
		BulkMaterial blendMaterial = ((BulkMaterial) this.getCurrentlyHandlingList().getEntityList().get(0));
		double blendODTDensity = blendMaterial.getODTDensity();
		double blendDensity = blendMaterial.getDensity();
		double blendMC = blendMaterial.getMoistureContent();

		double addingODTDensity = bulkMaterialToAdd.getODTDensity();
		double addingDensity = bulkMaterialToAdd.getDensity();
		double addingMC = bulkMaterialToAdd.getMoistureContent();

		// blending ODT Density
		if(blendODTDensity == 0.0d)
			blendMaterial.setODTDensity(bulkMaterialToAdd.getODTDensity());
		else if(addingODTDensity != 0.0d)
			blendMaterial.setODTDensity(HandyUtils.linearAverage(addedMaterialRatio, addingODTDensity, blendODTDensity));
			
		// blending MC
		if(blendMC == 0.0d)
			blendMaterial.setMoistureContent(bulkMaterialToAdd.getMoistureContent());
		else if(addingMC != 0.0d)
			blendMaterial.setMoistureContent(HandyUtils.linearAverage(addedMaterialRatio, addingMC, blendMC));

		// blending Density
		if(blendDensity == 0.0d)
			blendMaterial.setDensity(bulkMaterialToAdd.getDensity());
		else if(addingDensity != 0.0d)
			blendMaterial.setDensity(HandyUtils.linearAverage(addedMaterialRatio, addingDensity, blendDensity));
		else if(((BulkMaterial) this.getCurrentlyHandlingList().getEntityList().get(0)).getODTDensity() != 0.0d &&
				((BulkMaterial) this.getCurrentlyHandlingList().getEntityList().get(0)).getMoistureContent()!= 0.0d){
			blendODTDensity = ((BulkMaterial) this.getCurrentlyHandlingList().getEntityList().get(0)).getODTDensity();
			blendMC = ((BulkMaterial) this.getCurrentlyHandlingList().getEntityList().get(0)).getMoistureContent();
			
			blendMaterial.setDensity(blendODTDensity+(blendODTDensity*blendMC)/(1-blendMC));
		}			
	}
	// ////////////////////////////////////////////////////////////////////////////////////
	// REPORTING
	// ////////////////////////////////////////////////////////////////////////////////////
	@Override
	public boolean setPresentState(String state) {
		boolean reportPrinted = false;
		if (super.setPresentState(state)) {
			// add bottom line for states report
			stateReportFile.putDoubleWithDecimalsTabs(massContent, ReportAgent.getReportPrecision(), 1);
			stateReportFile.putDoubleWithDecimalsTabs(volumetricContent, ReportAgent.getReportPrecision(), 1);
			if(!this.getCurrentlyHandlingList().isEmpty()){
				stateReportFile.putDoubleWithDecimalsTabs(((BulkMaterial) this.getCurrentlyHandlingList().getEntityList().get(0)).getMoistureContent(), ReportAgent.getReportPrecision(), 1);
				stateReportFile.putDoubleWithDecimalsTabs(((BulkMaterial) this.getCurrentlyHandlingList().getEntityList().get(0)).getDensity(), ReportAgent.getReportPrecision(), 1);
			}
			reportPrinted = true;
		}
			
		return reportPrinted;

	}	
	
	@Override
	public void printStatesHeader() {
		super.printStatesHeader();
		stateReportFile.putStringTabs("Mass-based Content", 1);
		stateReportFile.putStringTabs("Volumetric Content", 1);
		stateReportFile.putStringTabs("Moisture Content", 1);		
		stateReportFile.putStringTabs("Density", 1);
	}
	
	@Override
	public void printStatesUnitsHeader() {
		super.printStatesUnitsHeader();
		stateReportFile.putStringTabs("(kg)",1);		
		stateReportFile.putStringTabs("(m3)",2);
		stateReportFile.putStringTabs("(kg/m3)",1);
	}
	

	// ////////////////////////////////////////////////////////////////////////////////////
	// OUTPUTS
	// ////////////////////////////////////////////////////////////////////////////////////

	@Output(name = "CurrentVolumetricContent",
			description = "The current volumetric content of this bulk Material Storage.",
			unitType = VolumeUnit.class)
			
	public Double getCurrentVolumetricContent( double simtime) {
		double dt = (simtime - lastContentUpdateTime);
		double tempAmount;

		if(this.getCurrentlyHandlingList().isEmpty())
			return 0.0d;
		
		BulkMaterial tempBulkMaterial = (BulkMaterial) this.getCurrentlyHandlingList().getEntityList().get(0);
		if(this.getPresentState() == "Loading" || this.getPresentState() == "Stacked") {
			tempAmount = this.getCurrentInfeedRate()*dt;
			if (tempBulkMaterial.getEntityUnit().equals(VolumeUnit.class)) {
				return Tester.min(this.getVolumetricBasedCargo(),this.getVolumetricBasedContent()+tempAmount);	
			} else if (tempBulkMaterial.getDensity() != 0.0d){
				return Tester.min(this.getVolumetricBasedCargo(),this.getVolumetricBasedContent() + tempAmount / tempBulkMaterial.getDensity());
			} else return 0.0d;
		} else if(this.getPresentState() == "UnLoading" || this.getPresentState() == "Reclaimed") {
			tempAmount = this.getCurrentOutfeedRate()*dt;
			if (this.getCurrentlyHandlingList().getEntityList().get(0).getEntityUnit().equals(VolumeUnit.class)) {
				return Tester_Rolos.max(0.0d,this.getVolumetricBasedContent()-tempAmount);	
			} else if (tempBulkMaterial.getDensity() != 0.0d){
				return Tester_Rolos.max(0.0d,this.getVolumetricBasedContent() - tempAmount / tempBulkMaterial.getDensity());
			} else return 0.0d;
		} else {
			return this.getVolumetricBasedContent();
		}
	}
	
	@Output(name = "CurrentMassBasedContent",
			description = "The current mass-based content of this bulk Material Storage.",
			unitType = MassUnit.class)
	public Double getCurrentMassBasedContent( double simtime) {
		double dt = (simtime - lastContentUpdateTime);
		double tempAmount;
//TODO add content: to set current content based on infeed/outfeed balance
			return 0.0d;

	}
	
	// ////////////////////////////////////////////////////////////////////////////////////
	// GRAPHICS
	// ////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);
		if(this.getCurrentContentDisplayEntity()!=null)
			this.getCurrentContentDisplayEntity().setSize(((BulkMaterial)this.getCurrentlyHandlingList().getEntityList().get(0)).getSizeforContent(this,simTime));
	}
}
