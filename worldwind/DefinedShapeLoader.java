package worldwind;

import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwindx.examples.util.ShapefileLoader;

public class DefinedShapeLoader extends ShapefileLoader {
	DefinedShapeAttributes attribute=new DefinedShapeAttributes();
	@Override
    protected PointPlacemarkAttributes nextPointAttributes()
    {
        synchronized (attribute)
        {
            return attribute.nextPointAttributes();
        }
    }

	@Override
    protected ShapeAttributes nextPolylineAttributes()
    {
        synchronized (attribute)
        {
            return attribute.nextPolylineAttributes();
        }
    }

	@Override
    protected ShapeAttributes nextPolygonAttributes()
    {
        synchronized (attribute)
        {
            return attribute.nextPolygonAttributes();
        }
    }
}
