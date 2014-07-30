package newt;

import gov.nasa.worldwind.formats.shapefile.ShapefileRecord;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwindx.examples.util.ShapefileLoader;

public class DefinedShapeLoader extends ShapefileLoader {
	DefinedShapeAttributes definedAttrs=new DefinedShapeAttributes();
	@Override
    protected PointPlacemarkAttributes createPointAttributes(ShapefileRecord record)
    {
        return definedAttrs.nextPointAttributes();
    }

    @Override
    protected ShapeAttributes createPolylineAttributes(ShapefileRecord record)
    {
        return definedAttrs.nextPolylineAttributes();
    }

    @Override
    protected ShapeAttributes createPolygonAttributes(ShapefileRecord record)
    {
        return definedAttrs.nextPolygonAttributes();
    }
}
