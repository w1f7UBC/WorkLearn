package worldwind;

import com.jaamsim.render.RenderException;

public class Renderer extends com.jaamsim.render.Renderer {

	public Renderer(boolean safeGraphics) throws RenderException {
		super(safeGraphics);
		WorldWindFrame.initialize();
	}
}
