package com.ROLOS;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;

import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.ErrorException;

import com.sandwell.JavaSimulation.IntegerInput;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class ROLOSEntity extends DisplayEntity  {

	@Keyword(description = "priority for when this entity is compared against another of its type to be put in an ordered list. " +
			"can be changed during the run. Default is 0", 
			example = "WoodchipTrucks Priority { 5 }")
	private final IntegerInput priority;
	
	/**
	 * priority of the entity at the current state; default priority is 0; 
	 */
	private int internalPriority;											
	
	{
		priority = new IntegerInput("Priority", "Key Inputs", 0);
		priority.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(priority);
	}
	
	public ROLOSEntity() {
	}
		
	@Override
	public void validate() {
		super.validate();
	}
	
	@Override
	public void earlyInit() {
		super.earlyInit();
		this.setInternalPriority(priority.getValue());
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
		public PriorityComparator(Class<? extends ROLOSEntity> targetClass, String methodName, Object... arguments) {
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
		private Method findEntityMethod(Class<? extends ROLOSEntity> targetClass, String methodName, Object... arguments) {
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
	public static class AscendingPriotityComparator<T extends ROLOSEntity> extends PriorityComparator implements Comparator<T> {
		
		public AscendingPriotityComparator(Class<? extends ROLOSEntity> targetClass, String methodName, Object... arguments) {
			super(targetClass, methodName, arguments);
		}
		
		@Override
		public int compare(T o1, T o2) {
			return Double.compare(this.getPriority(o1), this.getPriority(o2));
		}		
	}
	
	/**
	 * reverse implementation to keep lists in descending format (e.g. queues)
	 */
	public static class DescendingPriotityComparator<T extends ROLOSEntity> extends PriorityComparator implements Comparator<T>{
		
		public DescendingPriotityComparator(Class<? extends ROLOSEntity> targetClass, String methodName, Object... arguments) {
			super(targetClass, methodName, arguments);

		}
		
		@Override
		public int compare(T o1, T o2) {
			return Double.compare(this.getPriority(o2), this.getPriority(o1));
		}		
	}
	

}
