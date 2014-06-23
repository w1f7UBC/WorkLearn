package map;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.PositionListener;

public class MapListener implements PositionListener{


	@Override
	public void moved(PositionEvent arg0) {
		// TODO Auto-generated method stub
			System.out.println(arg0.getPosition().getLatitude().toString());
			System.out.println(arg0.getPosition().getLongitude().toString());


	}



}
