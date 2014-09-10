package worldwind;

import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwindx.examples.util.ShapefileLoader;

public class DefinedShapeLoader extends ShapefileLoader {

	private final PointPlacemarkAttributes nextPointAttributes;
	private final ShapeAttributes nextPolylineAttributes;
	private final ShapeAttributes nextPolygonAttributes;

	public DefinedShapeLoader(DefinedShapeAttributes attr){
		super();
		nextPointAttributes=attr.nextPointAttributes();
		nextPolylineAttributes=attr.nextPolylineAttributes();
		nextPolygonAttributes=attr.nextPolygonAttributes();
	}

	@Override
    protected PointPlacemarkAttributes nextPointAttributes(){
		return nextPointAttributes;
    }

	@Override
    protected ShapeAttributes nextPolylineAttributes(){
		return nextPolylineAttributes;
    }

	@Override
    protected ShapeAttributes nextPolygonAttributes(){
		return nextPolygonAttributes;
    }
}
