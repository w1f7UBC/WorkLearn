/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package worldwind;

import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.WWUtil;

import java.awt.*;

import com.jaamsim.math.Color4d;

/**
 * @author dcollins
 * @version $Id: RandomShapeAttributes.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class DefinedShapeAttributes
{
	private Color colorRGB=Color.CYAN;
	private int thick=3;
	private double alpha=0.03;

	//initialize with specific settings
    public DefinedShapeAttributes(Color4d color, int thickness, double opacity){
    	colorRGB = new Color((float)color.r, (float)color.g, (float)color.b);
    	thick = thickness;
    	alpha = opacity;
    }

    //initialize with default settings
    public DefinedShapeAttributes(){
    }

    public PointPlacemarkAttributes nextPointAttributes()
    {
        return this.createPointAttributes();
    }

    public ShapeAttributes nextPolylineAttributes()
    {
        return this.createPolylineAttributes();
    }

    public ShapeAttributes nextPolygonAttributes()
    {
        return this.createPolygonAttributes();
    }

    private PointPlacemarkAttributes createPointAttributes()
    {
        PointPlacemarkAttributes attrs = new PointPlacemarkAttributes();
        attrs.setUsePointAsDefaultImage(true);
        attrs.setLineMaterial(new Material(colorRGB));
        attrs.setScale((double)thick);
        return attrs;
    }

    private ShapeAttributes createPolylineAttributes()
    {
        ShapeAttributes attrs = new BasicShapeAttributes();
        attrs.setOutlineMaterial(new Material(colorRGB));
        attrs.setOutlineWidth(thick);
        return attrs;
    }

    private ShapeAttributes createPolygonAttributes()
    {
        ShapeAttributes attrs = new BasicShapeAttributes();
        attrs.setInteriorMaterial(new Material(colorRGB));
        attrs.setOutlineMaterial(new Material(WWUtil.makeColorBrighter(colorRGB)));
        attrs.setInteriorOpacity(alpha);
        attrs.setOutlineWidth(thick);
        return attrs;
    }
}
