package com.AROMA;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.IconLayer;
import gov.nasa.worldwind.render.UserFacingIcon;

import java.awt.Dimension;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;

import worldwind.WorldWindFrame;
import DataBase.Query;




import com.sandwell.JavaSimulation.Entity;
import com.AROMA.Logistics.Facility;
import com.AROMA.Logistics.RouteEntity;
import com.AROMA.Utils.HashMapList;
import com.jaamsim.DisplayModels.ColladaModel;
import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.DisplayModels.ImageModel;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.VisibilityInfo;
import com.sandwell.JavaSimulation3D.DisplayEntity;

/**
 * Rolos entity includes display 
 * @author Saeed Ghafghazi (email: saeedghaf@gmail.com) - Sep 3, 2014 
 * 
 * @modified_by
 */
public class AROMAEntity extends DisplayEntity  {
	private static final ArrayList<AROMAEntity> allInstances;

	private static boolean drawnColladas = false;
	
	// map of displaymodels used for drawing as a group in worldwind 
	private static final HashMapList<DisplayModel,AROMAEntity> wvDisplayModelGroups;
	
	//SHAPES
	@Keyword(description = "priority for when this entity is compared against another of its type to be put in an ordered list. " +
			"can be changed during the run. Default is 0", 
			example = "WoodchipTrucks Priority { 5 }")
	private final IntegerInput priority;
	
	@Keyword(description = "Query that contains the shape file for showing this entity.", 
			example = "Sawmills ShapeFileQuery { FacilityShapes }")
	private final EntityInput<Query> worldViewShapeFile;
	
	@Keyword(description = "The colour of this entity. This color will be used both in Jaamsim viewer and in Worldviewer."
			+ "Default is black.",
	         example = "Road1 Color { black }")
	private final ColourInput colorInput;
	
	@Keyword(description = "The width of worldviewer objects or lines as in route segments in pixels.",
	         example = "Road1 Width { 1 }")
	private final IntegerInput widthInput;
	
	@Keyword(description = "The opacity of this entity in worldviewer. Sclae is between 0 and 1."
			+ "Default is 0.03",
	         example = "Road1 Opacity { 0.1 }")
	private final ValueInput opacity;
		
	//COLLADAS
	@Keyword(description = "The size of the object in { x, y, z } coordinates. If only the x and y coordinates are given " +
            "then the z dimension is assumed to be zero.",
     example = "Object1 WVSize { 15 12 0 }")
	private final Vec3dInput wvSizeInput;
	
	@Keyword(description = "The point in the region at which the alignment point of the object is positioned.",
	         example = "Object1 WVPosition { -3.922 -1.830 0.000 m }")
	private final Vec3dInput wvPositionInput;
	
	@Keyword(description = "If TRUE, the object is displayed in the simulation view windows.",
	         example = "Object1 WVShow { FALSE }")
	private final BooleanInput wvShow;
	
	/**
	 * priority of the entity at the current state; default priority is 0; 
	 */
	private int internalPriority;		
	
	static {
		allInstances = new ArrayList<AROMAEntity>();
		wvDisplayModelGroups = new HashMapList<DisplayModel, AROMAEntity>();
	}
	
	{
		//SHAPES
		priority = new IntegerInput("Priority", "Key Inputs", 0);
		priority.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(priority);
		
		worldViewShapeFile = new EntityInput<Query>(Query.class, "ShapeFileQuery", "Basic Graphics", null);
		this.addInput(worldViewShapeFile);
		
		colorInput = new ColourInput("Colour", "Basic Graphics", ColourInput.BLACK);
		this.addInput(colorInput);

		widthInput = new IntegerInput("Width", "Basic Graphics", 1 );
		widthInput.setValidRange(1, Integer.MAX_VALUE);
		this.addInput(widthInput);
		
		opacity = new ValueInput("Opacity", "Basic Graphics", 0.01d);
		opacity.setValidRange(0.0d, 1.0d);
		this.addInput(opacity);
		
		//COLLADAS
		wvSizeInput = new Vec3dInput("WVSize", "Basic Graphics", new Vec3d(1.0d, 1.0d, 1.0d));
		this.addInput(wvSizeInput);
		
		wvPositionInput = new Vec3dInput("WVPosition", "Basic Graphics", new Vec3d());
		this.addInput(wvPositionInput);
		
		wvShow = new BooleanInput("WVShow", "Basic Graphics", false);
		this.addInput(wvShow);
		
		//IMAGES
	}
	
	public AROMAEntity() {
		synchronized (allInstances) {
			allInstances.add(this);
		}
		
	}
	
	public static ArrayList<? extends AROMAEntity> getAll() {
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
		if(in == priority)
			this.setInternalPriority(priority.getValue());
		if(in == colorInput && this.getClass().equals(Facility.class)){
			
		}
		//TODO delete old shapes/layers
		if (in ==wvShow && wvShow.getValue()==true){
			if (wvPositionInput !=null && this.getDisplayModelList()!=null){
				//System.out.println("got in here");
				DisplayModel target = getDisplayModelList().get(0);
				if (target.getClass().isAssignableFrom(ColladaModel.class)){
					//System.out.println("got in here1");
					ColladaModel colladaTarget = (ColladaModel) target;
					File uri = new File(colladaTarget.getColladaFile());
					Thread thread = new WorldWindFrame.ColladaThread(uri, wvPositionInput.getValue(),  wvSizeInput.getValue(), target.getVisibilityInfo(), false);
					thread.start();
				}
				else if (target.getClass().isAssignableFrom(ImageModel.class)){
					//System.out.println("got in here2");
					ImageModel imageTarget = (ImageModel) target;
					IconLayer layer = new IconLayer();
		            layer.setPickEnabled(true);
		            layer.setAllowBatchPicking(false);
		            layer.setRegionCulling(true);
		            Position actualPosition = Position.fromDegrees(wvPositionInput.getValue().x, wvPositionInput.getValue().y, wvPositionInput.getValue().z);
		            UserFacingIcon icon = new UserFacingIcon(imageTarget.getImageFile().getPath(), actualPosition);
		            icon.setSize(new Dimension(24, 24));
		            layer.addIcon(icon);
		            layer.setMaxActiveAltitude(target.getVisibilityInfo().getMax());
		            layer.setMinActiveAltitude(target.getVisibilityInfo().getMin());
		            WorldWindFrame.AppFrame.getWwd().getModel().getLayers().add(layer);
				}
			}
		}
	}
		
	@Override
	public void validate() {
		super.validate();
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();
		
		if (WorldWindFrame.AppFrame != null && !drawnColladas) {
			for (DisplayModel eachDisplaymodel : wvDisplayModelGroups.getKeys()) {
				try {
					ColladaModel target = (ColladaModel) eachDisplaymodel;
					
					File uri = new File(target.getColladaFile());
					ArrayList<Vec3d> positionList = new ArrayList<Vec3d>();
					ArrayList<Vec3d> scaleList = new ArrayList<Vec3d>();
					ArrayList<VisibilityInfo> visList = new ArrayList<VisibilityInfo>();
					for (AROMAEntity eachEntity : wvDisplayModelGroups
							.get(eachDisplaymodel)) {
						positionList.add(eachEntity.getWVPositionInput());
						scaleList.add(eachEntity.getWVSizeInput());
						visList.add(eachEntity.getDisplayModelList().get(0).getVisibilityInfo());
					}
					Thread thread = new WorldWindFrame.ColladaThread(uri, positionList, scaleList, visList, false);
					thread.start();
					try {
						thread.join();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (ClassCastException e) {
					// TODO: handle exception
				}
			}
			drawnColladas = true;
		}
	}
	
	public void addToWVDisplayModelList(){
		for(DisplayModel each: this.getDisplayModelList())
			wvDisplayModelGroups.add(each, this);
	}
	
	public Query getShapeFileQuery(){
		return worldViewShapeFile.getValue();
	}
	
	public Color4d getColor(){
		return colorInput.getValue();
	}
	
	public ColourInput getColorInput(){
		return colorInput;
	}

	public int getWidth(){
		return widthInput.getValue();
	}
	
	public double getOpacity(){
		return opacity.getValue();
	}
	
	public String getWVShowString(){
		return wvShow.getValueString();
	}
	
	public Vec3d getWVPositionInput(){
		return wvPositionInput.getValue();
	}
	
	public Vec3d getWVSizeInput(){
		return wvSizeInput.getValue();
	}
	
	/**
	 * Internal priority can be thought of as natural priority (ordering) of entities. Other types of
	 * priority can be defined and accessed from within each object through ascending and descending priority comparators 
	 * @return internal (natural) priority of entities
	 */
	public int getInternalPriority() {
		return internalPriority;
	}

	public void setInternalPriority(int internalPriority) {
		this.internalPriority = internalPriority;
	}
	
	/**
	 * TODO this is a general method invoking class. might be willing to move it to a separate class.
	 */
	public static class PriorityComparator {
		private final Method method; // The method to be executed
		private final Object[] arguments; // The arguments passed to the method to be executed

		/**
		 * 
		 * @param targetClass entity class 
		 * @param methodName the target whose returned int value will be used as the basis for comparison
		 * @param arguments static arguments that the method uses (should be static since the "compare" method does not update for arguments")
		 */
		public PriorityComparator(Class<? extends AROMAEntity> targetClass, String methodName, Object... arguments) {
			method = findEntityMethod(targetClass, methodName, arguments);
			this.arguments = arguments;
		}
		
		/**
		 * 
		 * @param ent the entity for which priority is calculated. <br> ent should be of the same class as was klass at construction time.
		 * @return the invoked methods' return which should be of int type
		 */
		public int getPriority(Entity ent) {
			try {
				return ((int)method.invoke(ent, arguments));
			}
			// Normal exceptions thrown by the method called by invoke are wrapped
			catch (InvocationTargetException e) {
				Throwable cause = e.getCause();
				if (cause instanceof RuntimeException)
					throw (RuntimeException)cause;
				else
					throw new ErrorException(cause);
			}
			catch (IllegalArgumentException e) {
				throw e;
			}
			catch (IllegalAccessException e) {
				throw new ErrorException(e);
			}
			catch (ClassCastException e) {
				throw new ErrorException("%s was passed to check priority of %s which is a non double returning method!",method.getName(),ent.getName());
			}
		}
		
		// Look up the method with the given name for the given entity and argument list.
		private Method findEntityMethod(Class<? extends AROMAEntity> targetClass, String methodName, Object... arguments) {
			Class<?>[] argClasses = new Class<?>[arguments.length];

			// Fill in the class of each argument, if there are any
			for (int i = 0; i < arguments.length; i++) {
				// The argument itself is null, no class information available
				if (arguments[i] == null) {
					argClasses[i] = null;
					continue;
				}

				argClasses[i] = arguments[i].getClass();

				// We wrap primitive doubles as Double, put back the primitive type
				if (argClasses[i] == Double.class) {
					argClasses[i] = Double.TYPE;
				}

				// We wrap primitive integers as Integer, put back the primitive type
				if (argClasses[i] == Integer.class) {
					argClasses[i] = Integer.TYPE;
				}
			}

			// Attempt to lookup the method using exact type information
			try {
				return targetClass.getMethod(methodName, argClasses);
			}
			catch (SecurityException e) {
				throw new ErrorException("Security Exception when finding method: %s", methodName);
			}
			catch (NullPointerException e) {
				throw new ErrorException("Name passed to startProcess was NULL");
			}
			catch (NoSuchMethodException e) {
				// Get a list of all our methods
				Method[] methods = targetClass.getMethods();

				// Loop over all methods looking for a unique method name
				int matchIndexHolder = -1;
				int numMatches = 0;
				for (int i = 0; i < methods.length; i++) {
					if (methods[i].getName().equals(methodName)) {
						numMatches++;
						matchIndexHolder = i;
					}
				}

				// If there was only one method found, use it
				if (numMatches == 1)
					return methods[matchIndexHolder];
				else
				throw new ErrorException("Method: %s does not exist, could not invoke.", methodName);
			}
		}
	}
	
	/**
	 * keeps queues in ascending format. for descending format use reverseinternalprioritycomparator
	 */
	public static class AscendingPriorityComparator<T extends AROMAEntity> extends PriorityComparator implements Comparator<T> {
		
		public AscendingPriorityComparator(Class<? extends AROMAEntity> targetClass, String methodName, Object... arguments) {
			super(targetClass, methodName, arguments);
		}
		
		@Override
		public int compare(T o1, T o2) {
			int tempCompare =  Double.compare(this.getPriority(o1), this.getPriority(o2));
			if(tempCompare != 0)
				return tempCompare;
			
			return (o1.getEntityNumber() < o2.getEntityNumber() ? -1 :
	               (o1.getEntityNumber() == o2.getEntityNumber() ? 0 : 1));
		}		
	}
	
	/**
	 * reverse implementation to keep lists in descending format (e.g. queues)
	 */
	public static class DescendingPriorityComparator<T extends AROMAEntity> extends PriorityComparator implements Comparator<T>{
		
		public DescendingPriorityComparator(Class<? extends AROMAEntity> targetClass, String methodName, Object... arguments) {
			super(targetClass, methodName, arguments);

		}
		
		@Override
		public int compare(T o1, T o2) {
			int tempCompare = Double.compare(this.getPriority(o2), this.getPriority(o1));
			if(tempCompare != 0)
				return tempCompare;
			
			return (o1.getEntityNumber() > o2.getEntityNumber() ? -1 :
	               (o1.getEntityNumber() == o2.getEntityNumber() ? 0 : 1));
		}		
	}
}
