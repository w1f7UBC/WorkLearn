package com.ROLOS.Logistics;

import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.CostPerEnergyUnit;
import com.jaamsim.units.CostPerMassUnit;
import com.jaamsim.units.CostPerVolumeUnit;
import com.jaamsim.units.DensityUnit;
import com.jaamsim.units.EnergyDensityUnit;
import com.jaamsim.units.MassUnit;
import com.jaamsim.units.VolumeUnit;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.ErrorException;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.Tester;
import com.sandwell.JavaSimulation3D.DisplayEntity;


public class BulkMaterial extends LogisticsEntity {
	
	@Keyword(description = "The graphical representation of this bulk material in bulk cargos",
	         example = "WoodChips BulkCargoDisplayModel { WoodCargo } ")
	protected final EntityInput<DisplayModel> bulkCargoDisplayModel;
	
	@Keyword(description = "The graphical representation of this bulk material in stock piles",
	         example = "WoodChips StockpilesDisplayModel { WoodStockpile } ")
	protected final EntityInput<DisplayModel> stockpileDisplayModel;
	
/*	@Keyword(description = "The List of bulk material that this material will be mixable with. E.g. saw dust and shavings can be mixed, the resulting material will "
			+ "be the storage which material is added to but with mixed properties.",
	         example = "WoodChips MixableWith { Shavings SawDust } ")
	protected final EntityListInput<BulkMaterial> mixableMaterialList;
	*/
	
	//TODO This price is set for the main selling output product of the facility (assuming only one main output)
	// Should be refactored to get the price from the market module
	@Keyword(description = "Product price as sold in the market.", 
			example = "Stockpile2 Price { 300 $/t }")
	private final SampleInput price;
	
	//TODO This cap is a test and used for CFS project. 
	// Should be refactored to get the price from the market module
	@Keyword(description = "Maximum transportation cost allowed for this bulk material. It's used "
			+ "in figuring out the transportation.", 
			example = "Stockpile2 TransportationCostCap { 300 $/t }")
	private final SampleInput transportationCostCap;
		
	@Keyword(description = "Fixed moisture content for the cargo", 
			example = "Stockpile2 MoistureContent { 0.55 }")
	private final SampleInput moistureContent;
	
	@Keyword(description = "Density of the bulk material. ",
	         example = "WoodChips Density { 0.4 g/cm3 } or Argon Density { 1.78 g/L } ")
	private final SampleInput density;
	
	@Keyword(description = "The colour of the bulk material in its representation.",
	         example = "Woodchip Color { tan }")
	private final ColourInput colorInput;
	
	@Keyword(description = "Dry Density of the bulk material. ",
	         example = "WoodChips Density { 0.4 g/cm3 } or Argon Density { 1.78 g/L } ")
	private final SampleInput dryDensity;
	
	@Keyword(description = "Energy density (volumetric based) of the bulk material",
	         example = "WoodPellet EnergyDensity { 5 MWh/m3 }")
	private final SampleInput energyDensity;
	
	// TODO refactor for when a product requires multiple products and can be produced from multiple paths
	private BulkMaterial processableFrom;
	
	private double blendMoistureContent;
	private double blendDensity;
	private double blendDryDensity;
	
	{
		bulkCargoDisplayModel = new EntityInput<DisplayModel>(DisplayModel.class, "BulkCargoDisplayModel", "Basic Graphics", this.getDisplayModelList().get(0));
		this.addInput(bulkCargoDisplayModel);		
		
		stockpileDisplayModel = new EntityInput<DisplayModel>(DisplayModel.class, "StockpileDisplayModel", "Basic Graphics", this.getDisplayModelList().get(0));
		this.addInput(stockpileDisplayModel);
		
	/*	mixableMaterialList = new  EntityListInput<>(BulkMaterial.class, "MixableWith", "Key Inputs", null);
		this.addInput(mixableMaterialList, true, "MixableEntities");
	*/	
		price = new SampleInput("Price", "Economic", null);
		this.addInput(price);
				
		transportationCostCap = new SampleInput("TransportationCostCap", "Economic", null);
		this.addInput(transportationCostCap);
		
		moistureContent = new SampleInput("MoistureContent", "Key Inputs", null);
		this.addInput(moistureContent);
		
		density = new SampleInput("Density", "Key Inputs", null);
		density.setUnitType(DensityUnit.class);
		this.addInput(density);
		
		dryDensity = new SampleInput("ODTDensity", "Key Inputs", null);
		dryDensity.setUnitType(DensityUnit.class);
		this.addInput(dryDensity);
		
		energyDensity = new SampleInput("EnergyDensity", "Key Inputs", null);
		energyDensity.setUnitType(EnergyDensityUnit.class);
		this.addInput(energyDensity);
		
		colorInput = new ColourInput("Colour", "Basic Graphics", ColourInput.BLACK);
		this.addInput(colorInput);
			
	}
	
	public BulkMaterial() {
	}
	
	@Override
	public void validate() {
		super.validate();
		
		if (this.getEntityMatterState() == Entity_State.SOLID_DISCRETE) {
			throw new ErrorException("The \"MatterState\" keyword should be set for %s and should define a bulk type!", this.getName());
		}
		
		
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	// GETTER AND SETTER METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	public String getColour(){
		return colorInput.getValueString();
	}
	
	public BulkMaterial getProcessableFrom(){
		return processableFrom;		
	}
	
	public void setProcessableFrom(BulkMaterial bulkMaterial){
		this.processableFrom = bulkMaterial;
	}
	
/*	public boolean isMixableWith(BulkMaterial bulkmaterial){
		return mixableMaterialList.getValue().contains(bulkmaterial.getProtoTypeEntity())? true : false;
	}
	*/
	public double getODTDensity(){
		if(dryDensity.getValue() != null){
			return dryDensity.getValue().getNextSample(getSimTime());
		} else{
			return blendDryDensity;
		}
	}
	
	/**
	 * @return price or null if price not set
	 */
	public double getPrice(){
		return price.getValue() != null ? price.getValue().getNextSample(this.getSimTime()) : 0.0d;
	}
	
	/**
	 * @return maximum allowable transportation cost or infinity if transportationCostCap not set
	 */
	public double getTransportationCostCap(){
		return transportationCostCap.getValue() != null ? transportationCostCap.getValue().getNextSample(this.getSimTime()) : Double.POSITIVE_INFINITY;
	}
	
	/**
	 * TODO this loosely returns density. it should be refactored to include mass and volume change in the cargo when moisture content changes.
	 * @return 1)blend density if this is currently handling material, 2)blend density 
	 */
	public double getDensity(){
	
		if(density.getValue() != null){
				return density.getValue().getNextSample(getSimTime());
		} else{
			return blendDensity;
		}
	}
	
	public double getMoistureContent(){
		if(moistureContent.getValue() != null)
			return moistureContent.getValue().getNextSample(getSimTime());
		else
			return blendMoistureContent;
	}
	
	public void setMoistureContent(double mcContent){
		this.blendMoistureContent = mcContent;
	}
	
	public void setODTDensity(double odtDensity){
		this.blendDryDensity = odtDensity;
	}
	
	public void setDensity(double density){
		this.blendDensity = density;
	}
	
	
	/**
	 * This method is called after the logistics entity matter state is figured out
	 */
	public void setInputUnits(){
		
		if(this.getEntityUnit().equals(MassUnit.class)){
			price.setUnitType(CostPerMassUnit.class);
			transportationCostCap.setUnitType(CostPerMassUnit.class);
		}
		else if (this.getEntityUnit().equals(VolumeUnit.class)){
			price.setUnitType(CostPerVolumeUnit.class);
			transportationCostCap.setUnitType(CostPerVolumeUnit.class);
		}
		else {
			price.setUnitType(CostPerEnergyUnit.class);
			transportationCostCap.setUnitType(CostPerEnergyUnit.class);
		}
	}
	
// linear blending formula
	//return odtDensity.getValue().getNextSample(getSimTime())+(odtDensity.getValue().getNextSample(getSimTime())*this.getCurrentCargoMaterialMoistureContent())/(1-this.getCurrentCargoMaterialMoistureContent());

/*	public void setMaterialProperties(BulkMaterial bulkMaterial, BulkMaterialStorage addingEntity){
		if(!mcInputDefined && this.getCurrentlyHandlingAmount() == 0.0d)
			this.blendMoistureContent = addingEntity.getCurrentCargoMaterialMoistureContent();
		if (!densityInputDefined && odtDensity.getValue() == null && this.getCurrentlyHandlingAmount()==0.0d 
				&& this.getHandlingEntityTypeList().contains(bulkMaterial))
			blendDensity = addingEntity.getCurrentCargoMaterialDensity();
	}
	*/
	//////////////////////////////////////////////////////////////////////////////////////
	// GRAPHICS
	//////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * generates display entity representation of this bulkmaterial to assign to the new bulkhandling linked entity
	 * @return the generated display entity
	 */
	public <T extends BulkHandlingLinkedEntity> DisplayEntity generateDisplayEntityFor(T currentlyHandledBy){
		DisplayEntity newDispEntity = InputAgent.defineEntityWithUniqueName(DisplayEntity.class, this.getName(), false);
		newDispEntity.setFlag(FLAG_GENERATED);
		if (currentlyHandledBy instanceof BulkCargo)
			InputAgent.processEntity_Keyword_Value(newDispEntity, "DisplayModel", bulkCargoDisplayModel.getValueString());
		else
			InputAgent.processEntity_Keyword_Value(newDispEntity, "DisplayModel", stockpileDisplayModel.getValueString());
		
		return newDispEntity;
	}
	
	public <T extends LinkedEntity> Vec3d getSizeforContent(T currentlyHandledBy, double simTime){
		double content;
		if(this.getEntityUnit().equals(VolumeUnit.class)){
			content = ((BulkMaterialStorage)currentlyHandledBy).getCurrentVolumetricContent(simTime);
		} else{
			content = ((BulkMaterialStorage)currentlyHandledBy).getCurrentMassBasedContent(simTime);
		}
		Vec3d size = new Vec3d(((BulkHandlingLinkedEntity) currentlyHandledBy).getCargoDimensions());
		size.z = (content/currentlyHandledBy.getCapacity(this))*size.z;
		return size;
	}
	
	private <T extends LinkedEntity> void setAlignmentForContent(T currentlyHandledBy, DisplayEntity dipslayEntity){
		Vec3d align = new Vec3d();
		align = ((BulkHandlingLinkedEntity)currentlyHandledBy).getCargoAlignment();
		
		align.z = -0.5; // this will result in fixed bottom and moving top when loading or unloading
		dipslayEntity.setAlignment(align);
	}

	@Override
	public <T extends LinkedEntity> void addToCurrentHandlersList(T currentlyHandledBy, double amountToAdd) {
		DisplayEntity newEnt;
		if(!(currentlyHandledBy instanceof BulkHandlingLinkedEntity)){
			super.addToCurrentHandlersList(currentlyHandledBy,amountToAdd);
			return;
		}
		if(!this.getCurrentlHandlersList().contains(currentlyHandledBy)){
			// add displayentity
			newEnt = generateDisplayEntityFor((BulkHandlingLinkedEntity)currentlyHandledBy);
			((BulkHandlingLinkedEntity)currentlyHandledBy).setCurrentContentDisplayEntity(newEnt);
			
			if (((BulkHandlingLinkedEntity)currentlyHandledBy).getCurrentTow() != null){
				InputAgent.processEntity_Keyword_Value(newEnt, "RelativeEntity", ((BulkHandlingLinkedEntity) currentlyHandledBy).getCurrentTow().getName());
			} else{
				newEnt.setPosition(currentlyHandledBy.getPosition());
			}
			this.setAlignmentForContent(currentlyHandledBy, newEnt);
			// TODO this shifts the z of position up to the bottom of cargo level. Should be refactored when z dynamically changes. Also can't set z in alignment as fixed bottom will be violated when loading/unloading.
			newEnt.getPositionVector().z = ((BulkHandlingLinkedEntity)currentlyHandledBy).getCargoAlignment().z;
			 
		} else{
			newEnt = ((BulkHandlingLinkedEntity) currentlyHandledBy).getCurrentContentDisplayEntity();
		}
		// TODO Auto-generated method stub
		super.addToCurrentHandlersList(currentlyHandledBy,amountToAdd);
		
	}
	
	@Override
	public <T extends LinkedEntity> void removeFromCurrentHandlersList(T currentlyHandledBy, double amountToRemove) {
		// TODO Auto-generated method stub
		super.removeFromCurrentHandlersList(currentlyHandledBy,amountToRemove);
		// set content's size
		if(!(currentlyHandledBy instanceof BulkHandlingLinkedEntity)){
			return;
		}
		DisplayEntity dispEnt = ((BulkHandlingLinkedEntity)currentlyHandledBy).getCurrentContentDisplayEntity();
		if (dispEnt != null) {
			dispEnt.setSize(this.getSizeforContent(currentlyHandledBy, this.getSimTime()));
			if(Tester.lessOrEqualCheckTolerance(dispEnt.getSize().z,0.0d)){
				dispEnt.kill();
				((BulkHandlingLinkedEntity)currentlyHandledBy).setCurrentContentDisplayEntity(null);
			}	
		}
	}
	
}
