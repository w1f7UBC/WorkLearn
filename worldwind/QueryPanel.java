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
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;

import DataBase.Query;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwindx.examples.FlatWorldPanel;


public class QueryPanel extends JPanel{

	private JList<String> querySelector;
	private int mode=0;

	public QueryPanel(WorldWindow wwd) {
		super(new BorderLayout(10, 10));
        this.add(this.makePanel(), BorderLayout.CENTER);
        this.add(new FlatWorldPanel(wwd), BorderLayout.SOUTH);
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

        JPanel radioButtonPanel = new JPanel(new GridLayout(0, 2, 0, 0));
        JRadioButton noneRadioButton = new JRadioButton("None");
        noneRadioButton.setSelected(true);
        noneRadioButton.addActionListener(new ActionListener()
        {
            @Override
			public void actionPerformed(ActionEvent event)
            {
            	mode=0;
            }
        });
        radioButtonPanel.add(noneRadioButton);
        JRadioButton pointRadioButton = new JRadioButton("Point");
        pointRadioButton.addActionListener(new ActionListener()
        {
            @Override
			public void actionPerformed(ActionEvent event)
            {
            	mode=1;
            }
        });
        radioButtonPanel.add(pointRadioButton);
        ButtonGroup group = new ButtonGroup();
        group.add(noneRadioButton);
        group.add(pointRadioButton);

        selectorPanel.add(listScroller, BorderLayout.CENTER);
        selectorPanel.add(buttonPanel, BorderLayout.WEST);
        selectorPanel.add(radioButtonPanel, BorderLayout.SOUTH);
        selectorPanel.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(5, 9, 9, 9), new TitledBorder("Query Selector")));
        selectorPanel.setToolTipText("Set query target");
        return selectorPanel;
    }

	public int getMode(){
		return mode;
	}

	public Query getQueryObject(){
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
}
