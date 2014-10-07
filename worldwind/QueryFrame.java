package worldwind;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;


import com.sandwell.JavaSimulation3D.GUIFrame;

import DataBase.Query;

import gov.nasa.worldwindx.examples.FlatWorldPanel;
import gov.nasa.worldwindx.examples.LayerPanel;

public class QueryFrame extends JPanel {
	public static JFrame HostFrame=null;
	private static JList<String> querySelector;
	private static int mode=0;
	private static JSlider slider;
	private static int sliderValue=10;
	private QueryFrame() {
		super(new BorderLayout(0, 0));
		if (WorldWindFrame.AppFrame==null){
			WorldWindFrame.initialize();
		}
        this.add(this.makePanel(), BorderLayout.CENTER);
        this.add(new FlatWorldPanel(WorldWindFrame.AppFrame.getWwd()), BorderLayout.SOUTH);
        JPanel controlPanel = new JPanel(new BorderLayout(0, 0));
        LayerPanel layerPanel = new LayerPanel(WorldWindFrame.AppFrame.getWwd());
        controlPanel.add(layerPanel, BorderLayout.WEST);
        controlPanel.add(this, BorderLayout.CENTER);
        HostFrame=new JFrame();
        HostFrame.add(controlPanel);
        HostFrame.pack();
        HostFrame.setLocation(GUIFrame.COL4_START, GUIFrame.LOWER_START);
        HostFrame.setSize(GUIFrame.COL4_WIDTH, GUIFrame.LOWER_HEIGHT);
      	HostFrame.setTitle("WorldView Controller");
       	HostFrame.setIconImage(GUIFrame.getWindowIcon());
        HostFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

	private JPanel makePanel()
    {
        JPanel selectorPanel=new JPanel(new BorderLayout());
        final DefaultListModel<String> selection=new DefaultListModel<String>();
        querySelector=new JList<String>(selection);
        JScrollPane listScroller = new JScrollPane(querySelector);
        JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 0, 0));

        JButton refresh = new JButton("Refresh Queriables");
        refresh.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
            	ArrayList<Query> list = Query.getAll();
            	Iterator<Query> iterator = list.iterator();
            	selection.clear();
            	selection.addElement("None");
            	while(iterator.hasNext()){
            		selection.addElement(iterator.next().getName());
            	}
            	//more unrelated code
            	//Query.deleteAllResultFrames();
            	//unrelated code but i need it for later
            	/*
            	BasicOrbitView test;
            	test = (BasicOrbitView) WorldWindFrame.AppFrame.getWwd().getView();
            	
            	Angle test1 = Angle.fromDegrees(30);
            	Angle test2 = Angle.fromDegrees(60);
            	test.addHeadingPitchAnimator(test.getHeading(), test1, test.getPitch(), test2);
            	*/
            }
        });
        buttonPanel.add(refresh);

        JButton queryArea = new JButton("Plot queriables if exist");
        queryArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
            	if (querySelector.getSelectedValue()!=null){
            		if (querySelector.getSelectedValue()!="none"){
            			getQueryObject().executeArea(true, new DefinedShapeAttributes());
            		}
            	}
            }
        });
        buttonPanel.add(queryArea);
       
        	
        JPanel comboBoxPanel = new JPanel(new GridLayout(0, 2, 0, 0));
        
        
        slider = new JSlider(JSlider.HORIZONTAL, 0, 50, 25);
        slider.setMinorTickSpacing(2);
        slider.setMajorTickSpacing(10);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setLabelTable(slider.createStandardLabels(10));
        slider.setVisible(false);
       
     
        /*
         * DROP DOWN
         */
         String[] menu_options = {"None","Point","Radius(km)","Closest Point"};
         JComboBox comboBox = new JComboBox();   
         
         int count = 0;
         for(int i = 0; i < menu_options.length; i++)
        	   comboBox.addItem(menu_options[count++]);
         comboBox.addActionListener(new ActionListener() {
        	   public void actionPerformed(ActionEvent e) {        	     
        	       setMode(((JComboBox)e.getSource()).getSelectedIndex());  
        	       if(((JComboBox)e.getSource()).getSelectedIndex()==2)
        	    	   slider.setVisible(true);
        	       else
        	    	   slider.setVisible(false);
        	   }
        	 });
         comboBoxPanel.add(comboBox);
         comboBoxPanel.add(slider);
         
        selectorPanel.add(listScroller, BorderLayout.CENTER);
        selectorPanel.add(buttonPanel, BorderLayout.WEST);
        selectorPanel.add(comboBoxPanel, BorderLayout.SOUTH);
        selectorPanel.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(5, 9, 9, 9), new TitledBorder("Query Selector")));
        selectorPanel.setToolTipText("Set query target");
  
  
        return selectorPanel;
    }
	
	public static void setSliderValue(int value){
		slider.setValue(value);
	}
	
	public static int getSliderValue()
	{
		return slider.getValue();
	}
	
	public static void setMode(int setMode){
		mode=setMode;
	}
	
	public static int getMode(){
		return mode;
	}

	public static Query getQueryObject(){
		if (querySelector.getSelectedValue()=="None"){
			return null;
		}
		Iterator<Query> iterator = Query.getAll().iterator();
		while(iterator.hasNext()){
			Query target=iterator.next();
			if (target.getName()==querySelector.getSelectedValue()){
				return target;
			}
		}
		return null;
	}

	public static void initialize(){
		if (HostFrame==null){
			new QueryFrame();
		}
	}

	public static void setControlVisible(final boolean visibility){
    	if (HostFrame==null && visibility==true){
    		QueryFrame.initialize();
    	}
    	java.awt.EventQueue.invokeLater(new Runnable()
        {
            @Override
			public void run()
            {
            	QueryFrame.HostFrame.setVisible(visibility);
            }
        });
    }
}
