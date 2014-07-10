package newt;

import java.awt.Component;
import javax.swing.SwingUtilities;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.NEWTEvent;
import com.jogamp.newt.event.NEWTEventFiFo;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowUpdateEvent;

/**
 * Helper class that processes NEWT events from a NEWT {@link Window}, converts
 * the events to AWT events, and forwards the AWT events to AWT component
 * provided in the constructor.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class NewtEventProcessor extends NEWTEventFiFo implements com.jogamp.newt.event.MouseListener,
		com.jogamp.newt.event.KeyListener, com.jogamp.newt.event.WindowListener
{
	protected final Component awtComponent;

	protected boolean mouseDragged = false;

	public NewtEventProcessor(Component awtComponent)
	{
		this.awtComponent = awtComponent;
	}

	@Override
	public synchronized void put(NEWTEvent event)
	{
		super.put(event);

		//process the added event on the EDT:
		Runnable task = new Runnable()
		{
			@Override
			public void run()
			{
				forwardEvents();
			}
		};
		
		//always invoke later, even if this is the EDT, because sometimes the EDT will
		//wait on other threads that can raise more events, causing a deadlock on this
		//synchonized method
		SwingUtilities.invokeLater(task);
	}

	/**
	 * Forward the NEWT events captured by this listener to the AWT handlers.
	 */
	protected void forwardEvents()
	{
		NEWTEvent event;
		while ((event = get()) != null)
		{
			java.awt.AWTEvent awtEvent = NewtEventConverter.createEvent(event, awtComponent);
			if (awtEvent != null)
			{
				awtComponent.dispatchEvent(awtEvent);
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		put(e);
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		put(e);
	}
	
	@Override
	public void mouseClicked(MouseEvent e)
	{
		//AWT doesn't raise click events after a drag, but NEWT does, so follow AWT behaviour.
		if (!mouseDragged)
		{
			put(e);
		}
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
		put(e);
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
		put(e);
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		System.out.println(e.getX());
		System.out.println(e.getY());
		put(e);
		mouseDragged = false;
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		put(e);
	}

	@Override
	public void mouseMoved(MouseEvent e)
	{
		put(e);
	}

	@Override
	public void mouseDragged(MouseEvent e)
	{
		put(e);
		mouseDragged = true;
	}

	@Override
	public void mouseWheelMoved(MouseEvent e)
	{
		put(e);
	}

	@Override
	public void windowGainedFocus(WindowEvent e)
	{
		put(e);
	}

	@Override
	public void windowLostFocus(WindowEvent e)
	{
		put(e);
	}

	@Override
	public void windowResized(WindowEvent e)
	{
		put(e);
	}

	@Override
	public void windowMoved(WindowEvent e)
	{
		put(e);
	}

	@Override
	public void windowDestroyNotify(WindowEvent e)
	{
		put(e);
	}

	@Override
	public void windowDestroyed(WindowEvent e)
	{
		put(e);
	}

	@Override
	public void windowRepaint(WindowUpdateEvent e)
	{
		put(e);
	}
}
