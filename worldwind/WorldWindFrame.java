/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package worldwind;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.RenderingExceptionListener;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.exception.WWAbsentRequirementException;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.ViewControlsLayer;
import gov.nasa.worldwind.layers.ViewControlsSelectListener;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwindx.examples.ApplicationTemplate;
import gov.nasa.worldwindx.examples.LayerPanel;
import gov.nasa.worldwindx.examples.util.*;

import javax.swing.*;
import javax.swing.filechooser.*;

import com.sandwell.JavaSimulation3D.GUIFrame;

import DataBase.Query;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.ResultSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;

/**
 * Illustrates how to import ESRI Shapefiles into World Wind. This uses a <code>{@link ShapefileLoader}</code> to parse
 * a Shapefile's contents and convert each shape into an equivalent World Wind shape. This provides examples of
 * importing a Shapefile on the local hard drive and importing a Shapefile at a remote URL.
 *
 * @author Patrick Murris
 * @version $Id: Shapefiles.java 2109 2014-06-30 16:52:38Z tgaskins $
 */
public class WorldWindFrame extends ApplicationTemplate
{
	public static AppFrame AppFrame=null;
	private static JFrame ControlFrame=null;
	
    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
    	private QueryPanel queryPanel;
    	
        public AppFrame(){
            makeMenu(this);
            this.setLocation(GUIFrame.COL4_START, GUIFrame.TOP_START);
            this.setSize(GUIFrame.COL4_WIDTH, GUIFrame.VIEW_HEIGHT);
            this.setIconImage(GUIFrame.getWindowIcon());
            final WorldWindowGLCanvas canvas = (WorldWindowGLCanvas) this.getWwd();
            canvas.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                	Position position = canvas.getCurrentPosition();
                	if (e!=null){
                		if(e.getButton()==3){
                	        //if cursor mode is set to 1 and WorldWind actually returns a position
                			if (position!=null && queryPanel.getMode()==1){
                				Query query = queryPanel.getQueryObject();
                				if (query!=null){
                					String lat = position.latitude.toString().split("бу")[0];
                					String lon = position.longitude.toString().split("бу")[0];
                					String name = query.getName()+"("+lat+","+lon+")";
                					ResultSet resultset=query.execute(name, lat, lon, true);
                					query.printResultContent(name, resultset);
                				}
                				position=null;
                			}
                	    }
                	}
                }
             });
            AppFrame=this;
        }
        
        public void addShapefileLayer(Layer layer)
        {
            this.getWwd().getModel().getLayers().add(layer);
        }

        public void removeShapefileLayer(String layer) {
			LayerList layerList=this.getWwd().getModel().getLayers();
			if (layerList.getLayerByName(layer)!=null){
				layerList.remove(layerList.getLayerByName(layer));
			}
		}
        
        public void gotoLayer(Layer layer)
        {
            Sector sector = (Sector) layer.getValue(AVKey.SECTOR);
            if (sector != null)
            {
                ExampleUtil.goTo(this.getWwd(), sector);
            }
        }
        
        @Override
        protected void initialize(boolean includeStatusBar, boolean includeLayerPanel, boolean includeStatsPanel)
        {
            // Create the WorldWindow.
            this.wwjPanel = this.createAppPanel(this.getCanvasSize(), includeStatusBar);
            this.wwjPanel.setPreferredSize(getCanvasSize());

            // Put the pieces together.
            this.getContentPane().add(wwjPanel, BorderLayout.CENTER);
            ControlFrame=new JFrame();
            if (includeLayerPanel){
                this.controlPanel = new JPanel(new BorderLayout(10, 10));
                this.layerPanel = new LayerPanel(this.getWwd());
                this.controlPanel.add(this.layerPanel, BorderLayout.WEST);
                this.queryPanel = new QueryPanel(this.getWwd());
                this.controlPanel.add(this.queryPanel, BorderLayout.CENTER);
                ControlFrame.add(this.controlPanel);
                ControlFrame.pack();
                ControlFrame.setLocation(GUIFrame.COL4_START, GUIFrame.LOWER_START);
        		ControlFrame.setSize(GUIFrame.COL4_WIDTH, GUIFrame.LOWER_HEIGHT);
                ControlFrame.setTitle("WorldView Controller");
                ControlFrame.setIconImage(GUIFrame.getWindowIcon());
                ControlFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            }
            this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            if (includeStatsPanel || System.getProperty("gov.nasa.worldwind.showStatistics") != null)
            {
                this.statsPanel = new StatisticsPanel(this.wwjPanel.getWwd(), new Dimension(250, this.getCanvasSize().height));
                this.getContentPane().add(this.statsPanel, BorderLayout.EAST);
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
        }
    }
    
    public static class WorkerThread extends Thread
    {
        protected Object shpSource;
        protected AppFrame appFrame;

        public WorkerThread(Object shpSource, AppFrame appFrame)
        {
            this.shpSource = shpSource;
            this.appFrame = appFrame;
        }
        
        public void run()
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    appFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                }
            });

            try
            {
                Layer shpLayer = this.parse();

                // Set the shapefile layer's display name
                shpLayer.setName(formName(this.shpSource));

                // Schedule a task on the EDT to add the parsed shapefile layer to a layer
                final Layer finalSHPLayer = shpLayer;
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        appFrame.addShapefileLayer(finalSHPLayer);
                        appFrame.gotoLayer(finalSHPLayer);
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
                        appFrame.setCursor(null);
                    }
                });
            }
        }

        protected Layer parse()
        {
        	Layer layer = null;
            if (OpenStreetMapShapefileLoader.isOSMPlacesSource(this.shpSource))
            {
            	layer = OpenStreetMapShapefileLoader.makeLayerFromOSMPlacesSource(this.shpSource);
            	layer.setPickEnabled(false);
            	return layer;
            }
            else
            {
                DefinedShapeLoader loader = new DefinedShapeLoader();
                layer = loader.createLayerFromSource(this.shpSource);
                layer.setPickEnabled(false);
                return layer;
            }
        }
    }

    protected static String formName(Object source)
    {
        String name = WWIO.getSourcePath(source);
        if (name != null)
            name = WWIO.getFilename(name);
        if (name == null)
            name = "Shapefile";
        return name;
    }

    protected static void makeMenu(final AppFrame appFrame)
    {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Shapefile", "shp"));
        fileChooser.setFileFilter(fileChooser.getChoosableFileFilters()[1]);

        JMenuBar menuBar = new JMenuBar();
        appFrame.setJMenuBar(menuBar);
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenuItem openFileMenuItem = new JMenuItem(new AbstractAction("Open File...")
        {
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    int status = fileChooser.showOpenDialog(appFrame);
                    if (status == JFileChooser.APPROVE_OPTION)
                    {
                        for (File file : fileChooser.getSelectedFiles())
                        {
                            new WorkerThread(file, appFrame).start();
                        }
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        fileMenu.add(openFileMenuItem);

        JMenuItem openURLMenuItem = new JMenuItem(new AbstractAction("Open URL...")
        {
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    String status = JOptionPane.showInputDialog(appFrame, "URL");
                    if (!WWUtil.isEmpty(status))
                    {
                        new WorkerThread(status.trim(), appFrame).start();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        fileMenu.add(openURLMenuItem);
    }
    
    public static AppFrame startClosable(String appName, Class appFrameClass)
    {
        if (Configuration.isMacOS() && appName != null)
        {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", appName);
        }

        try
        {
            final AppFrame frame = (AppFrame) appFrameClass.newInstance();
            frame.setTitle(appName);
            return frame;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void initialize()
    {
    	   // Get the World Wind logger by name.
        Logger logger = Logger.getLogger("gov.nasa.worldwind");

        // Turn off logging to parent handlers of the World Wind handler.
        logger.setUseParentHandlers(false);

        // Create a console handler (defined below) that we use to write log messages.
        final ConsoleHandler handler = new MyHandler();

        // Enable all logging levels on both the logger and the handler.
        logger.setLevel(Level.OFF);
        handler.setLevel(Level.OFF);

        // Add our handler to the logger
        logger.addHandler(handler);
        startClosable("WorldViewer", AppFrame.class);
    }
    
    private static class MyHandler extends ConsoleHandler
    {
        public void publish(LogRecord logRecord)
        {
            // Just redirect the record to ConsoleHandler for printing.
            super.publish(logRecord);
        }
    }
    
    public static void setViewVisible(final boolean visibility){
    	if (AppFrame==null && visibility==true){
    		initialize();
    	}
    	java.awt.EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
            	AppFrame.setVisible(visibility);
            }
        });
    }
    
    public static void setControlVisible(final boolean visibility){
    	if (ControlFrame==null && visibility==true){
    		initialize();
    	}
    	java.awt.EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
            	ControlFrame.setVisible(visibility);
            }
        });
    }
}
