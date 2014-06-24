package map;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.PositionListener;

public class MapListener implements PositionListener, MouseListener{
	String a="";
	String b="";

	@Override
	public void moved(PositionEvent arg0) {
		// TODO Auto-generated method stub
			//System.out.println(arg0.getPosition().getLatitude().toString());
			//System.out.println(arg0.getPosition().getLongitude().toString());
			a=arg0.getPosition().getLatitude().toString();
			b=arg0.getPosition().getLongitude().toString();

	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		System.out.println("Lat: "+a+ " Long: "+b);

	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		//System.out.println("asdf");
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		//System.out.println("asdf");
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		//System.out.println("asdf");
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		//System.out.println("asdf");
	}


}
