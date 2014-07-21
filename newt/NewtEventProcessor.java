package newt;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.AbstractBalloon;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.PointPlacemark;

import java.awt.Component;
import java.awt.Rectangle;
import java.sql.SQLException;

import javax.swing.SwingUtilities;

import DataBase.TestingMethod;
import DataBase.QueryObject;

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
	protected RenderableLayer layer;
	protected WorldWindowNewtCanvas canvas;
	protected boolean mouseDragged = false;
	protected int cursorMode;

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
		put(e);
		mouseDragged = false;
		canvas = (WorldWindowNewtCanvas) awtComponent;
		Position pos = canvas.getCurrentPosition();
	
	    if(e.getButton()==3){
	        //if is right click
			if (pos!=null && cursorMode==1){
				String latitude = method(pos.latitude.toDecimalDegreesString(10));
				String longtitude = method(pos.longitude.toDecimalDegreesString(10));
			//	String x = method(pos.getLatitude().toDecimalDegreesString(10).toString());
		//		String y = method(pos.getLongitude().toDecimalDegreesString(10).toString());
			//	System.out.println(x);
			//	System.out.println(y);
				Container.getInstance().setPosition(pos);
				try {
					QueryObject.updateStatement(longtitude, latitude);
					QueryObject.test();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				//PointPlacemark point = new PointPlacemark(canvas.getCurrentPosition());
				//layer = new RenderableLayer();
				//layer.addRenderable(point);
				//canvas.getModel().getLayers().add(layer);
				pos=null;
			}
	    }
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

	public void setCursor(int mode) {
		cursorMode=mode;
		System.out.println(mode);
	}

	public String method(String str) {

		  if (str.length() > 0 && str.charAt(str.length()-1)== 'бу') {
		    str = str.substring(0, str.length()-1);
		  }
		  return str;
		}
}
