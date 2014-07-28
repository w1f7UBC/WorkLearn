package newt;
/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.exception.WWAbsentRequirementException;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.placename.PlaceNameLayer;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwindx.examples.ClickAndGoSelectListener;
import gov.nasa.worldwindx.examples.LayerPanel;
import gov.nasa.worldwindx.examples.util.*;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a base application framework for simple WorldWind examples. Examine other examples in this package to see
 * how it's used.
 *
 * @version $Id: ApplicationTemplate.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class Initializer
{
    public static class AppPanel extends JPanel
    {
        protected WorldWindow wwd;
        protected StatusBar statusBar;
        protected ToolTipController toolTipController;
        protected HighlightController highlightController;

        public AppPanel(Dimension canvasSize, boolean includeStatusBar)
        {
            super(new BorderLayout());

            this.wwd = this.createWorldWindow();
            ((Component) this.wwd).setPreferredSize(canvasSize);

            // Create the default model as described in the current worldwind properties.
            Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
            this.wwd.setModel(m);

            // Setup a select listener for the worldmap click-and-go feature
            this.wwd.addSelectListener(new ClickAndGoSelectListener(this.getWwd(), WorldMapLayer.class));

            this.add((Component) this.wwd, BorderLayout.CENTER);
            if (includeStatusBar)
            {
                this.statusBar = new StatusBar();
                this.add(statusBar, BorderLayout.PAGE_END);
                this.statusBar.setEventSource(wwd);
            }

            // Add controllers to manage highlighting and tool tips.
            this.toolTipController = new ToolTipController(this.getWwd(), AVKey.DISPLAY_NAME, null);
            this.highlightController = new HighlightController(this.getWwd(), SelectEvent.ROLLOVER);
        }

        protected WorldWindow createWorldWindow()
        {
			Configuration.setValue(AVKey.WORLD_WINDOW_CLASS_NAME, WorldWindowNewtAutoDrawable.class.getName());
			Configuration.setValue(AVKey.INPUT_HANDLER_CLASS_NAME, NewtInputHandler.class.getName());
			return new WorldWindowNewtCanvas();
        }

        public WorldWindow getWwd()
        {
            return wwd;
        }

        public StatusBar getStatusBar()
        {
            return statusBar;
        }
    }

    public static class AppFrame extends JFrame
    {
        private Dimension canvasSize = new Dimension(800, 600);

        protected AppPanel wwjPanel;
        protected LayerPanel layerPanel;
        protected StatisticsPanel statsPanel;
        protected List<Layer> layers = new ArrayList<Layer>();
        protected BasicDragger dragger;
        protected JFileChooser fc = new JFileChooser(Configuration.getUserHomeDirectory());
        protected JCheckBox pickCheck, dragCheck;

        public AppFrame()
        {
            this.initialize(true, true);
        }

        public AppFrame(Dimension size)
        {
            this.canvasSize = size;
            this.initialize(true, true);
        }

        public AppFrame(boolean includeStatusBar, boolean includeLayerPanel)
        {
            this.initialize(includeStatusBar, includeLayerPanel);
        }

        protected void initialize(boolean includeStatusBar, boolean includeLayerPanel)
        {
            // Create the WorldWindow.
            this.wwjPanel = this.createAppPanel(this.canvasSize, includeStatusBar);
            this.wwjPanel.setPreferredSize(canvasSize);

            // Put the pieces together.
            this.getContentPane().add(wwjPanel, BorderLayout.CENTER);
            if (includeLayerPanel)
            {
                this.layerPanel = new LayerPanel(this.wwjPanel.getWwd(), null);
                this.getContentPane().add(this.layerPanel, BorderLayout.WEST);
            }

            // Create and install the view controls layer and register a controller for it with the World Window.
            ViewControlsLayer viewControlsLayer = new ViewControlsLayer();
            insertBeforeCompass(getWwd(), viewControlsLayer);
            this.getWwd().addSelectListener(new ViewControlsSelectListener(this.getWwd(), viewControlsLayer));

            // Register a rendering exception listener that's notified when exceptions occur during rendering.
            this.wwjPanel.getWwd().addRenderingExceptionListener(new RenderingExceptionListener()
            {
                public void exceptionThrown(Throwable t)
                {
                    if (t instanceof WWAbsentRequirementException)
                    {
                        String message = "Computer does not meet minimum graphics requirements.\n";
                        message += "Please install up-to-date graphics driver and try again.\n";
                        message += "Reason: " + t.getMessage() + "\n";
                        message += "This program will end when you press OK.";

                        JOptionPane.showMessageDialog(AppFrame.this, message, "Unable to Start Program",
                            JOptionPane.ERROR_MESSAGE);
                        System.exit(-1);
                    }
                }
            });

            // Search the layer list for layers that are also select listeners and register them with the World
            // Window. This enables interactive layers to be included without specific knowledge of them here.
            for (Layer layer : this.wwjPanel.getWwd().getModel().getLayers())
            {
                if (layer instanceof SelectListener)
                {
                    this.getWwd().addSelectListener((SelectListener) layer);
                }
            }

            this.pack();

            // Center the application on the screen.
            WWUtil.alignComponent(null, this, AVKey.CENTER);
            this.setResizable(true);
            
            // Add our control panel.
            this.makeControlPanel();

            // Create a select listener for shape dragging but do not add it yet. Dragging can be enabled via the user
            // interface.
            this.dragger = new BasicDragger(this.getWwd());

            // Setup file chooser
            this.fc = new JFileChooser();
            this.fc.addChoosableFileFilter(new FileNameExtensionFilter("ESRI Shapefile", "shp"));
        }

        protected void makeControlPanel()
        {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            panel.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(0, 9, 9, 9),
                new TitledBorder("Shapefiles")));

            // Open shapefile buttons.
            JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 0, 5)); // nrows, ncols, hgap, vgap
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // top, left, bottom, right
            panel.add(buttonPanel);
            // Open shapefile from File button.
            JButton openFileButton = new JButton("Open File...");
            openFileButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    showOpenFileDialog();
                }
            });
            buttonPanel.add(openFileButton);
            // Open shapefile from URL button.
            JButton openURLButton = new JButton("Open URL...");
            openURLButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    showOpenURLDialog();
                }
            });
            buttonPanel.add(openURLButton);

            // Picking and dragging checkboxes
            JPanel pickPanel = new JPanel(new GridLayout(1, 1, 10, 10)); // nrows, ncols, hgap, vgap
            pickPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // top, left, bottom, right
            this.pickCheck = new JCheckBox("Allow picking");
            this.pickCheck.setSelected(true);
            this.pickCheck.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    enablePicking(((JCheckBox) actionEvent.getSource()).isSelected());
                }
            });
            pickPanel.add(this.pickCheck);

            this.dragCheck = new JCheckBox("Allow dragging");
            this.dragCheck.setSelected(false);
            this.dragCheck.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    enableDragging(((JCheckBox) actionEvent.getSource()).isSelected());
                }
            });
            pickPanel.add(this.dragCheck);

            panel.add(pickPanel);

            this.getLayerPanel().add(panel, BorderLayout.SOUTH);
        }

        protected void enablePicking(boolean enabled)
        {
            for (Layer layer : this.layers)
            {
                layer.setPickEnabled(enabled);
            }

            // Disable the drag check box. Dragging is implicitly disabled since the objects cannot be picked.
            this.dragCheck.setEnabled(enabled);
        }

        protected void enableDragging(boolean enabled)
        {
            if (enabled)
                this.getWwd().addSelectListener(this.dragger);
            else
                this.getWwd().removeSelectListener(this.dragger);
        }

        public void showOpenFileDialog()
        {
            int retVal = AppFrame.this.fc.showOpenDialog(this);
            if (retVal != JFileChooser.APPROVE_OPTION)
                return;

            Thread t = new WorkerThread(this.fc.getSelectedFile(), this);
            t.start();
            ((Component) getWwd()).setCursor(new Cursor(Cursor.WAIT_CURSOR));
        }

        public void showOpenURLDialog()
        {
            String retVal = JOptionPane.showInputDialog(this, "Enter Shapefile URL", "Open",
                JOptionPane.INFORMATION_MESSAGE);
            if (WWUtil.isEmpty(retVal)) // User cancelled the operation entered an empty URL.
                return;

            URL url = WWIO.makeURL(retVal);
            if (url == null)
            {
                JOptionPane.showMessageDialog(this, retVal + " is not a valid URL.", "Open", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Thread t = new WorkerThread(url, this);
            t.start();
            ((Component) getWwd()).setCursor(new Cursor(Cursor.WAIT_CURSOR));
        }

    	public static class WorkerThread extends Thread
    	{
    	    protected Object source;
    	    protected AppFrame appFrame;
    	
    	    public WorkerThread(Object source, AppFrame appFrame)
    	    {
    	        this.source = source;
    	        this.appFrame = appFrame;
    	    }
    	
    	    public void run()
    	    {
    	        try
    	        {
    	            final List<Layer> layers = this.makeShapefileLayers();
    	            for (int i = 0; i < layers.size(); i++)
    	            {
    	                String name = this.makeDisplayName(this.source);
    	                layers.get(i).setName(i == 0 ? name : name + "-" + Integer.toString(i));
    	                layers.get(i).setPickEnabled(this.appFrame.pickCheck.isSelected());
    	            }
    	
    	            SwingUtilities.invokeLater(new Runnable()
    	            {
    	                public void run()
    	                {
    	                    for (Layer layer : layers)
    	                    {
    	                        insertBeforePlacenames(appFrame.getWwd(), layer);
    	                        appFrame.layers.add(layer);
    	                    }
    	
    	                    appFrame.layerPanel.update(appFrame.getWwd());
    	                }
    	            });
    	        }
    	        catch (Exception e)
    	        {
    	            e.printStackTrace();
    	        }
    	        finally
    	        {
    	            SwingUtilities.invokeLater(new Runnable()
    	            {
    	                public void run()
    	                {
    	                    ((Component) appFrame.getWwd()).setCursor(Cursor.getDefaultCursor());
    	                }
    	            });
    	        }
    	    }
    	
    	    protected List<Layer> makeShapefileLayers()
    	    {
    	        if (OpenStreetMapShapefileLoader.isOSMPlacesSource(this.source))
    	        {
    	            Layer layer = OpenStreetMapShapefileLoader.makeLayerFromOSMPlacesSource(source);
    	            List<Layer> layers = new ArrayList<Layer>();
    	            layers.add(layer);
    	            return layers;
    	        }
    	        else
    	        {
    	            ShapefileLoader loader = new ShapefileLoader();
    	            return loader.createLayersFromSource(this.source);
    	        }
    	    }
    	
    	    protected String makeDisplayName(Object source)
    	    {
    	        String name = WWIO.getSourcePath(source);
    	        if (name != null)
    	            name = WWIO.getFilename(name);
    	        if (name == null)
    	            name = "Shapefile";
    	
    	        return name;
    	    }
    	}
    	
        protected AppPanel createAppPanel(Dimension canvasSize, boolean includeStatusBar)
        {
            return new AppPanel(canvasSize, includeStatusBar);
        }

        public Dimension getCanvasSize()
        {
            return canvasSize;
        }

        public AppPanel getWwjPanel()
        {
            return wwjPanel;
        }

        public WorldWindow getWwd()
        {
            return this.wwjPanel.getWwd();
        }

        public StatusBar getStatusBar()
        {
            return this.wwjPanel.getStatusBar();
        }

        public LayerPanel getLayerPanel()
        {
            return layerPanel;
        }

        public StatisticsPanel getStatsPanel()
        {
            return statsPanel;
        }

        public void setToolTipController(ToolTipController controller)
        {
            if (this.wwjPanel.toolTipController != null)
                this.wwjPanel.toolTipController.dispose();

            this.wwjPanel.toolTipController = controller;
        }

        public void setHighlightController(HighlightController controller)
        {
            if (this.wwjPanel.highlightController != null)
                this.wwjPanel.highlightController.dispose();

            this.wwjPanel.highlightController = controller;
        }
    }

    public static void insertBeforeCompass(WorldWindow wwd, Layer layer)
    {
        // Insert the layer into the layer list just before the compass.
        int compassPosition = 0;
        LayerList layers = wwd.getModel().getLayers();
        for (Layer l : layers)
        {
            if (l instanceof CompassLayer)
                compassPosition = layers.indexOf(l);
        }
        layers.add(compassPosition, layer);
    }

    public static void insertBeforePlacenames(WorldWindow wwd, Layer layer)
    {
        // Insert the layer into the layer list just before the placenames.
        int compassPosition = 0;
        LayerList layers = wwd.getModel().getLayers();
        for (Layer l : layers)
        {
            if (l instanceof PlaceNameLayer)
                compassPosition = layers.indexOf(l);
        }
        layers.add(compassPosition, layer);
    }

    public static void insertAfterPlacenames(WorldWindow wwd, Layer layer)
    {
        // Insert the layer into the layer list just after the placenames.
        int compassPosition = 0;
        LayerList layers = wwd.getModel().getLayers();
        for (Layer l : layers)
        {
            if (l instanceof PlaceNameLayer)
                compassPosition = layers.indexOf(l);
        }
        layers.add(compassPosition + 1, layer);
    }

    public static void insertBeforeLayerName(WorldWindow wwd, Layer layer, String targetName)
    {
        // Insert the layer into the layer list just before the target layer.
        int targetPosition = 0;
        LayerList layers = wwd.getModel().getLayers();
        for (Layer l : layers)
        {
            if (l.getName().indexOf(targetName) != -1)
            {
                targetPosition = layers.indexOf(l);
                break;
            }
        }
        layers.add(targetPosition, layer);
    }

    static
    {
        System.setProperty("java.net.useSystemProxies", "true");
        if (Configuration.isMacOS())
        {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "World Wind Application");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
            System.setProperty("apple.awt.brushMetalLook", "true");
        }
        else if (Configuration.isWindowsOS())
        {
            System.setProperty("sun.awt.noerasebackground", "true"); // prevents flashing during window resizing
        }
    }

    public static AppFrame start(String appName, Class appFrameClass)
    {
        if (Configuration.isMacOS() && appName != null)
        {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", appName);
        }

        try
        {
            final AppFrame frame = (AppFrame) appFrameClass.newInstance();
            frame.setTitle(appName);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            java.awt.EventQueue.invokeLater(new Runnable()
            {
                public void run()
                {
                    frame.setVisible(true);
                }
            });

            return frame;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
