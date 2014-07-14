package newt;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.awt.AWTInputHandler;
import gov.nasa.worldwind.event.InputHandler;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.MouseWheelEvent;

import com.jogamp.newt.opengl.GLWindow;

/**
 * {@link InputHandler} implementation that captures NEWT events from a
 * {@link GLWindow} and dispatches them as AWT events to the event source.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class NewtInputHandler extends AWTInputHandler
{
	protected GLWindow window;
	protected NewtEventProcessor eventProcessor;

	@Override
	public void setEventSource(WorldWindow newWorldWindow)
	{
		super.setEventSource(newWorldWindow);

		if (!(newWorldWindow instanceof WorldWindowNewtCanvas))
		{
			throw new IllegalArgumentException("newWorldWindow must be an instanceof "
					+ WorldWindowNewtCanvas.class.getSimpleName());
		}

		GLWindow window = newWorldWindow == null ? null : ((WorldWindowNewtCanvas) newWorldWindow).getWindow();
		if (this.window == window)
		{
			return;
		}

		if (this.window != null)
		{
			this.window.removeMouseListener(eventProcessor);
			this.window.removeKeyListener(eventProcessor);
			this.window.removeWindowListener(eventProcessor);
		}

		this.window = window;

		if (window == null)
		{
			return;
		}

		eventProcessor = new NewtEventProcessor((Component) newWorldWindow);
		window.addMouseListener(eventProcessor);
		window.addKeyListener(eventProcessor);
		window.addWindowListener(eventProcessor);
	}

	//focus and mouse wheel events still get passed to AWT, so only process them if they are from the NEWT component:

	@Override
	public void focusGained(FocusEvent focusEvent)
	{
		if (isEventFromNewt(focusEvent))
		{
			super.focusGained(focusEvent);
		}
	}

	@Override
	public void focusLost(FocusEvent focusEvent)
	{
		if (isEventFromNewt(focusEvent))
		{
			super.focusLost(focusEvent);
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent)
	{
		if (isEventFromNewt(mouseWheelEvent))
		{
			super.mouseWheelMoved(mouseWheelEvent);
		}
	}

	public void setCursor(int mode){
		eventProcessor.setCursor(mode);
	}
	
	protected boolean isEventFromNewt(AWTEvent event)
	{
		return event instanceof NewtEventConverter.AWTEventFromNewt;
	}
}
