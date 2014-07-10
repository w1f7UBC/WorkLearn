package newt;

import gov.nasa.worldwind.WorldWindowGLDrawable;

import java.awt.Component;

import javax.media.opengl.GLAutoDrawable;

import com.jogamp.newt.opengl.GLWindow;

/**
 * {@link WorldWindowGLDrawable} subinterface used by the
 * {@link WorldWindowNewtCanvas}.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public interface WorldWindowNewtDrawable extends WorldWindowGLDrawable
{
	/**
	 * @deprecated Use the {@link #initDrawable(GLWindow, Component)} function
	 *             instead.
	 */
	@Deprecated
	@Override
	void initDrawable(GLAutoDrawable glAutoDrawable);

	void initDrawable(GLWindow window, Component awtComponent);
}
