package org.geworkbench.components.sam;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.awt.Font;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent; 
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.geworkbench.bison.datastructure.biocollections.DSDataSet;
import org.geworkbench.bison.datastructure.biocollections.microarrays.DSMicroarraySet;
import org.geworkbench.bison.datastructure.biocollections.views.DSMicroarraySetView;
import org.geworkbench.bison.datastructure.bioobjects.SamResultData;
import org.geworkbench.bison.datastructure.bioobjects.markers.DSGeneMarker;
import org.geworkbench.bison.datastructure.bioobjects.markers.annotationparser.AnnotationParser;
import org.geworkbench.bison.datastructure.bioobjects.microarray.DSMicroarray;
import org.geworkbench.bison.datastructure.complex.panels.CSAnnotPanel;
import org.geworkbench.bison.datastructure.complex.panels.DSAnnotatedPanel;
import org.geworkbench.builtin.projects.SaveFileFilterFactory;
import org.geworkbench.builtin.projects.SaveFileFilterFactory.CustomFileFilter;
import org.geworkbench.engine.config.VisualPlugin;
import org.geworkbench.engine.management.AcceptTypes;
import org.geworkbench.engine.management.Publish;
import org.geworkbench.engine.management.Subscribe;
import org.geworkbench.events.ProjectEvent;
import org.geworkbench.events.SubpanelChangedEvent;
import org.geworkbench.util.FilePathnameUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * 
 * @author zm2165 
 * @version $Id$
 */


@AcceptTypes({ SamResultData.class })
public class SAMViewer extends JPanel implements VisualPlugin{

	private static final long serialVersionUID = -4629489912300556101L;
	private final float DELTA_INIT=2.4f;
	
	private static final String lastDirConf = FilePathnameUtils.getUserSettingDirectoryPath()
	+ "sam" + FilePathnameUtils.FILE_SEPARATOR + "lastDir.conf";
	
	private float delta=DELTA_INIT;
	private float deltaInc=0.3f;
	private float deltaMax=5.0f;
	private float fdrValue=0.05f;
	private int sigTotal=0;
	private int sigOver=0;
	private int sigUnder=0;	
	private int geneNo=0;
	
	private float[] dd;
	private float[] dbar;
	private static float[] pvalue;	//FIXME why static
	private static float[] fold;
	private float[] fdr;
	private DSMicroarraySetView<DSGeneMarker, DSMicroarray> dataView;
	private static DSMicroarraySet maSet;
		
	SortPair[] sortPair;
	
	private JSlider deltaSlider;
	private JLabel deltaLabel;
	private JLabel fdrLabel;
	private JFreeChart chart;
	private JPanel sliderPane;
	private JLabel sigOverUnderLabel;	
	private JLabel sigTotalLabel;
	private JLabel sigExpectLabel;
	private JButton jAddBttn;
	private static final String DELTA_HEAD="Delta = ";
	private static final String FDR_HEAD="FDR  = ";
	private static final String SIG_TOTAL_HEAD="Significant (total) = ";
	private static final String SIG_OVERUNDER_HEAD="Significant (over/under) = ";
	private static final String SIG_EXPECT_HEAD="Significant (expected) = ";
	
	ArrayList<Dot> dotList=new ArrayList<Dot>();
	List<Dot> overList;
	List<Dot> underList;
	List<Dot> totalList;	
	
	private static class TableModel extends AbstractTableModel {
		
		private static final long serialVersionUID = -747015190283539771L;

		private static final int COLUMN_COUNT =5;

		private static final String[] columnNames = new String[] { "Probeset Id",
				"Gene Symbol", "P-value", "Fold x", "Annotation" };

		List<Dot> list = null;

		public TableModel() {
			list = new ArrayList<Dot>();
		}

		@Override
		public String getColumnName(int col) {
			return columnNames[col];
		}

		@Override
		public int getColumnCount() {
			return COLUMN_COUNT;
		}

		@Override
		public int getRowCount() {
			if(list==null) return 0;
			return list.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Dot d = list.get(rowIndex);
			switch (columnIndex) {
			case 0:
				return d.getMarker().getLabel();
			case 1:
				return d.getMarker().getGeneName();
			case 2:
				return pvalue[d.getGeneRowNo()];
			case 3:
				return fold[d.getGeneRowNo()];
			case 4:
				String s="";
				String markerLabel=d.getMarker().getLabel();				
				try{					
					String[] geneTitles = AnnotationParser.getInfo(maSet,
							markerLabel, AnnotationParser.DESCRIPTION);
					s=geneTitles[0];					
				}
				catch(Exception e){					
					//e.printStackTrace();
				}
				return s;
				
			}
			return 0;
		}

		void setValues(List<Dot> list) {
			this.list = list;
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Class getColumnClass(int column) {
	        Class returnValue;
	        if ((column >= 0) && (column < getColumnCount())) {
	          returnValue = getValueAt(0, column).getClass();
	        } else {
	          returnValue = Object.class;
	        }
	        return returnValue;
	      }
	}	
	
	private TableModel totalTableModel = new TableModel();
	private TableModel overExpTableModel = new TableModel();
	private TableModel underExpTableModel = new TableModel();
	
	public SAMViewer() {
		
		JTable totalTable = new JTable(totalTableModel);
		totalTable.setAutoCreateRowSorter(true);	
		Enumeration<TableColumn> totColumns = totalTable.getColumnModel().getColumns();
		while (totColumns.hasMoreElements()) {
			totColumns.nextElement().setCellRenderer(new CellRenderer());
		}
		
		
		JTable overExpTable = new JTable(overExpTableModel);
		overExpTable.setAutoCreateRowSorter(true);
		Enumeration<TableColumn> overColumns = overExpTable.getColumnModel().getColumns();
		while (overColumns.hasMoreElements()) {
			overColumns.nextElement().setCellRenderer(new CellRenderer());
		}
		
		
		JTable underExpTable = new JTable(underExpTableModel);
		underExpTable.setAutoCreateRowSorter(true);
		Enumeration<TableColumn> underColumns = underExpTable.getColumnModel().getColumns();
		while (underColumns.hasMoreElements()) {
			underColumns.nextElement().setCellRenderer(new CellRenderer());
		}
		
		JSplitPane splitPaneTop;		
		JPanel upperLeftPane=new JPanel();
		JPanel upperRightPane=new JPanel();
		
		splitPaneTop = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				upperLeftPane, upperRightPane);
		upperRightPane.setLayout(new BorderLayout());
		splitPaneTop.setOneTouchExpandable(true);
		splitPaneTop.setDividerLocation(750);
		
		JSplitPane splitPane;		
		//JPanel upperPane=new JPanel();
		JPanel lowerPane=new JPanel();
		lowerPane.setLayout(new BorderLayout());		
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				splitPaneTop, lowerPane);
		
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(450);
		splitPane.setAutoscrolls(true);
		
		setLayout(new BorderLayout());
		add(splitPane, BorderLayout.CENTER);
		
		JTabbedPane tabbedPane=new JTabbedPane();
		JPanel totalPane=new JPanel();
		JPanel overExPane=new JPanel();
		JPanel underExPane=new JPanel();
		
		totalPane.setLayout(new BorderLayout());
		totalPane.add(new JScrollPane(totalTable), BorderLayout.CENTER);		
		JPanel totalPaneWithSave=new JPanel();
		totalPaneWithSave.setLayout(new BorderLayout());
		totalPaneWithSave.add(totalPane, BorderLayout.CENTER);
		JButton saveTotalButton=new JButton();
		saveTotalButton.setText("Export Table");
		JPanel bottom1=new JPanel();
		bottom1.setLayout(new GridLayout(0,5));
		bottom1.add(new JLabel());
		bottom1.add(new JLabel());
		bottom1.add(saveTotalButton);
		totalPaneWithSave.add(bottom1, BorderLayout.SOUTH);		
		tabbedPane.addTab("Total",totalPaneWithSave);
		
		
		overExPane.setLayout(new BorderLayout());
		overExPane.add(new JScrollPane(overExpTable), BorderLayout.CENTER);		
		JPanel overExPaneWithSave=new JPanel();
		overExPaneWithSave.setLayout(new BorderLayout());
		overExPaneWithSave.add(overExPane, BorderLayout.CENTER);
		JButton saveExButton=new JButton();
		saveExButton.setText("Export Table");
		JPanel bottom2=new JPanel();
		bottom2.setLayout(new GridLayout(0,5));
		bottom2.add(new JLabel());
		bottom2.add(new JLabel());
		bottom2.add(saveExButton);
		overExPaneWithSave.add(bottom2, BorderLayout.SOUTH);		
		tabbedPane.addTab("OverExpressed",overExPaneWithSave);
		
		underExPane.setLayout(new BorderLayout());
		underExPane.add(new JScrollPane(underExpTable), BorderLayout.CENTER);		
		JPanel underExPaneWithSave=new JPanel();
		underExPaneWithSave.setLayout(new BorderLayout());
		underExPaneWithSave.add(underExPane, BorderLayout.CENTER);
		JButton saveUnderButton=new JButton();
		saveUnderButton.setText("Export Table");
		JPanel bottom3=new JPanel();
		bottom3.setLayout(new GridLayout(0,5));
		bottom3.add(new JLabel());
		bottom3.add(new JLabel());
		bottom3.add(saveUnderButton);
		underExPaneWithSave.add(bottom3, BorderLayout.SOUTH);		
		tabbedPane.addTab("UnderExpressed",underExPaneWithSave);
		lowerPane.add(tabbedPane,BorderLayout.CENTER);
		
		saveTotalButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				saveTable(totalList);
			}

		});

		saveExButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				saveTable(overList);
			}
		});

		saveUnderButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {				
				saveTable(underList);
			}
		});
		
		
		
		createSlider();
		sliderPane=new JPanel();
		//sliderPane.setLayout(new BoxLayout(sliderPane, BoxLayout.LINE_AXIS));
		sliderPane.setLayout(new BorderLayout());
		sliderPane.add(deltaSlider,BorderLayout.CENTER);
		//sliderPane.setPreferredSize(new Dimension(200, 80));		
			
		JPanel sumPane=new JPanel();
		upperRightPane.add(sumPane,BorderLayout.NORTH);		
		sumPane.setLayout(new BoxLayout(sumPane, BoxLayout.Y_AXIS));
		sumPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,40));
		sumPane.add(sliderPane);		
		deltaLabel=new JLabel();
		deltaLabel.setText(DELTA_HEAD+delta);		
		sumPane.add(deltaLabel);
		sumPane.add(new JLabel(" "));
		fdrLabel=new JLabel();
		fdrLabel.setText(FDR_HEAD+fdrValue);
		sumPane.add(fdrLabel);
		sumPane.add(new JLabel(" "));
		sigTotalLabel=new JLabel();
		sigTotalLabel.setText(SIG_TOTAL_HEAD+Integer.toString(sigTotal));
		sumPane.add(sigTotalLabel);
		sumPane.add(new JLabel(" "));
		sigOverUnderLabel=new JLabel();
		sigOverUnderLabel.setText(SIG_OVERUNDER_HEAD+sigOver+"/"+sigUnder);
		sumPane.add(sigOverUnderLabel);
		sumPane.add(new JLabel(" "));
		sigExpectLabel=new JLabel();
		sigExpectLabel.setText(SIG_EXPECT_HEAD+0);
		sumPane.add(sigExpectLabel);
		sumPane.add(new JLabel(" "));
		jAddBttn = new JButton();
        jAddBttn.setText("Add to Set");
		sumPane.add(jAddBttn);
		
		jAddBttn.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent arg0) {				
			
				DSAnnotatedPanel<DSGeneMarker, Float> panelOver= new CSAnnotPanel<DSGeneMarker, Float>(
            			"SAM genes(overexpressed)");
				for(int i=0;i<overList.size();i++){
					panelOver.add(overList.get(i).getMarker(), new Float(i));					
				}
				publishSubpanelChangedEvent(new SubpanelChangedEvent<DSGeneMarker>(
            			DSGeneMarker.class, panelOver,
            			SubpanelChangedEvent.NEW));
								
				//FIXME:only 2 marker sets show up, reasons unknown
				DSAnnotatedPanel<DSGeneMarker, Float> panelTotal = new CSAnnotPanel<DSGeneMarker, Float>(
	        			"SAM genes(all)");
				for(int i=0;i<totalList.size();i++){
					panelTotal.add(totalList.get(i).getMarker(), new Float(i));					
				}
				publishSubpanelChangedEvent(new SubpanelChangedEvent<DSGeneMarker>(
	        			DSGeneMarker.class, panelTotal,
	        			SubpanelChangedEvent.NEW));				
				
				
				DSAnnotatedPanel<DSGeneMarker, Float> panelUnder= new CSAnnotPanel<DSGeneMarker, Float>(
            			"SAM genes(underexpressed)");
				for(int i=0;i<underList.size();i++){
					panelUnder.add(underList.get(i).getMarker(), new Float(i));					
				}
				publishSubpanelChangedEvent(new SubpanelChangedEvent<DSGeneMarker>(
            			DSGeneMarker.class, panelUnder,
            			SubpanelChangedEvent.NEW));				
				
			}
		});
		
		
		//set up graph
		XYDataset dataset = createSampleDataset(delta);
		ChartPanel chartPanel=drawChartPanel(dataset);
		upperLeftPane.add(chartPanel,BorderLayout.CENTER);
		
	}
	
	private void createSlider(){
		int sliderMax= (int) (deltaMax/deltaInc*10.0/10);
		int sliderInit=sliderMax/2;
		deltaSlider = new JSlider(JSlider.HORIZONTAL,
		                                      1, sliderMax, sliderInit);		
		deltaSlider.setMajorTickSpacing(10);
		deltaSlider.setMinorTickSpacing(1);
		deltaSlider.setPaintTicks(true);
		deltaSlider.setPaintLabels(true);
		
		Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		labelTable.put( new Integer( 1 ), new JLabel(""+deltaInc) );
		labelTable.put( new Integer( sliderMax ), new JLabel(""+deltaInc*sliderMax) );
		deltaSlider.setLabelTable( labelTable );

		deltaSlider.setPaintLabels(true);
		
		
		deltaSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                deltaSlider_stateChanged(e);
            }
        });		
		
	}
	
	private ChartPanel drawChartPanel(XYDataset ds){				
		// Generate the graph
		chart= ChartFactory.createXYLineChart(
			"SAM Plot", // Title
			"average null t-statistic, from permutations", // x-axis Label
			"actual t-statistic, from data", // y-axis Label
			ds, // Dataset
			PlotOrientation.VERTICAL, // Plot Orientation
			false, // Show Legend or not
			true, // Use tooltips
			false // Configure chart to generate URLs?
		);
		XYPlot plot = (XYPlot) chart.getPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, true);
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesPaint(0, Color.black);
        renderer.setSeriesStroke(
                0, (Stroke) new BasicStroke(
                    2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1.0f, new float[] {2.0f, 6.0f}, 0.0f
                )
            );        
       
        renderer.setSeriesLinesVisible(1, true);
        renderer.setSeriesShapesVisible(1, false);
        renderer.setSeriesPaint(1, Color.black);
        renderer.setSeriesStroke(
                1, (Stroke) new BasicStroke(
                    2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1.0f, new float[] {2.0f, 6.0f}, 0.0f
                )
            );
        
        renderer.setSeriesLinesVisible(2, true);
        renderer.setSeriesShapesVisible(2, false);
        renderer.setSeriesPaint(2, Color.black);        
       
        int pointsize=5;
        double o = pointsize/2.0*(-1);
        renderer.setSeriesLinesVisible(3, false);
        renderer.setSeriesShapesVisible(3, true);
        renderer.setSeriesPaint(3, Color.red);
        renderer.setSeriesShape(3,new Ellipse2D.Double(o,o,pointsize,pointsize));
        
        renderer.setSeriesLinesVisible(4, false);
        renderer.setSeriesShapesVisible(4, true);
        renderer.setSeriesPaint(4, Color.blue);
        renderer.setSeriesShape(4,new Ellipse2D.Double(o,o,pointsize,pointsize));
        
        renderer.setSeriesLinesVisible(5, false);
        renderer.setSeriesShapesVisible(5, true);
        renderer.setSeriesPaint(5, Color.black);
        renderer.setSeriesShape(5,new Ellipse2D.Double(o,o,pointsize,pointsize));
        
        
        plot.setRenderer(renderer);				
		ChartPanel cPanel = new ChartPanel(chart);
		cPanel.setPreferredSize(new java.awt.Dimension(700, 400));
		
		return cPanel;
	}
	
	

	private XYDataset createSampleDataset(float deltaValue) {
	 
		 XYSeriesCollection dataset = new XYSeriesCollection();
	
		if(dbar!=null){
	        XYSeries seriesUpperDotLine = new XYSeries("Series 1");
	        seriesUpperDotLine.add(dbar[0], dbar[0]+deltaValue);        
	        seriesUpperDotLine.add(dbar[geneNo-1], dbar[geneNo-1]+deltaValue);
	        XYSeries seriesLowerDotLine = new XYSeries("Series 2");
	        seriesLowerDotLine.add(dbar[0], dbar[0]-deltaValue);        
	        seriesLowerDotLine.add(dbar[geneNo-1], dbar[geneNo-1]-deltaValue);        
	        XYSeries seriesMiddleLine = new XYSeries("Series 3");
	        seriesMiddleLine.add(dbar[0], dbar[0]);        
	        seriesMiddleLine.add(dbar[geneNo-1], dbar[geneNo-1]);
	        XYSeries seriesOverExpDot = new XYSeries("Series 4");	       
	        XYSeries seriesUnderExpDot = new XYSeries("Series 5");	       
	        XYSeries seriesMiddleDot = new XYSeries("Series 6");	       
	        overList=new ArrayList<Dot>();
	        underList=new ArrayList<Dot>();
	        totalList=new ArrayList<Dot>();
	        for(Dot d:dotList){
	        	if(d.getY()>(d.getX()+deltaValue)){
	        		seriesOverExpDot.add(d.getX(),d.getY());
	        		d.setOverExpressed(true);
	        		d.setUnderExpressed(false);
	        		overList.add(d);
	        		totalList.add(d);
	        	}
	        	else if(d.getY()<(d.getX()-deltaValue)){
	        		seriesUnderExpDot.add(d.getX(),d.getY());
	        		d.setOverExpressed(false);
	        		d.setUnderExpressed(true);
	        		underList.add(d);
	        		totalList.add(d);
	        	}
	        	else{
	        		seriesMiddleDot.add(d.getX(),d.getY());
	        		d.setOverExpressed(false);
	        		d.setUnderExpressed(false);        		
	        	}
	        		
	        }
	        
	        if(totalList!=null)
	        	sigTotal=totalList.size();
	        if(underList!=null)
	        	sigUnder=underList.size();
	        if(overList!=null)
	        	sigOver=overList.size();
	        sigTotalLabel.setText(SIG_TOTAL_HEAD+Integer.toString(sigTotal));
	        sigOverUnderLabel.setText(SIG_OVERUNDER_HEAD+sigOver+"/"+sigUnder);
	        
	        //System.out.println("************OVER EXPRESSED "+overExpressed);
	      	totalTableModel.setValues(totalList);
			totalTableModel.fireTableDataChanged();
			overExpTableModel.setValues(overList);
			overExpTableModel.fireTableDataChanged();
			underExpTableModel.setValues(underList);
			underExpTableModel.fireTableDataChanged();        
	       
	        dataset.addSeries(seriesUpperDotLine);
	        dataset.addSeries(seriesLowerDotLine);
	        dataset.addSeries(seriesMiddleLine);
	        dataset.addSeries(seriesOverExpDot);
	        dataset.addSeries(seriesUnderExpDot);
	        dataset.addSeries(seriesMiddleDot);
		}
        return dataset;
    }
	

	private SamResultData samResult = null;

	@Subscribe
	public void receive(ProjectEvent event, Object source) {
		DSDataSet<?> resultDataSet = event.getDataSet();
		if (resultDataSet instanceof SamResultData) {			
			
			samResult = (SamResultData) resultDataSet;
			//maSet=samResult.getMaSet();
			dataView=samResult.getData();
			deltaInc=samResult.getDeltaInc();
			deltaMax=samResult.getDeltaMax();			
			
			sliderPane.removeAll();
			createSlider();
			sliderPane.add(deltaSlider);
			
			dd=samResult.getD();
			dbar=samResult.getDbar();
			pvalue=samResult.getPvalue();
			fold=samResult.getFold();
			fdr=samResult.getFdr();			
			geneNo=dd.length;
			
			sortPair=new SortPair[dd.length];
			for(int i=0;i<dd.length;i++){
				sortPair[i]=new SortPair(dd[i],i);
			}
			
			Arrays.sort(sortPair);			
			
			dotList=new ArrayList<Dot>();
			for(int i=0;i<dd.length;i++){
				DSGeneMarker gmarker=dataView.markers().get(i);
				dotList.add(new Dot(dbar[i],sortPair[i].getValue(),sortPair[i].getOriginalIndex(), gmarker));
			}
			XYDataset dataset = createSampleDataset(delta);
		    ((XYPlot)chart.getPlot()).setDataset(dataset);
		    
		    updateSum();
			
			totalTableModel.fireTableDataChanged();			
			overExpTableModel.fireTableDataChanged();			
			underExpTableModel.fireTableDataChanged();			
		}
		else if (resultDataSet instanceof DSMicroarraySet){
			maSet = (DSMicroarraySet)resultDataSet;
		}	
		
	}	
	
	private void deltaSlider_stateChanged(ChangeEvent e) {
        updateSum();
        
        XYDataset dataset = createSampleDataset(delta);
        ((XYPlot)chart.getPlot()).setDataset(dataset);
    }
	
	private void updateSum(){
		delta  = (float) deltaSlider.getValue()*deltaInc;
        deltaLabel.setText(DELTA_HEAD+((int)(delta*100))/100.0);
        fdrLabel.setText(FDR_HEAD+((int)(fdr[deltaSlider.getValue()-1]*10000))/10000.0);
        sigExpectLabel.setText(SIG_EXPECT_HEAD+((int) (fdr[deltaSlider.getValue()-1]*sigTotal*100))/100.0);
       
	}
	
	private class Dot{
		private float x;
		private float y;
		private int geneRowNo;
		DSGeneMarker marker;
		
		public Dot(float x, float y, int geneRowNo, DSGeneMarker marker){
			this.x=x;
			this.y=y;
			this.geneRowNo=geneRowNo;
			this.marker=marker;
		}
		
		public float getX(){
			return x;
		}
		
		public float getY(){
			return y;
		}
		
		public int getGeneRowNo(){
			return geneRowNo;
		}
		
		public DSGeneMarker getMarker(){
			return marker;
		}
		
		public void setOverExpressed(boolean b){
		}
		
		public void setUnderExpressed(boolean b){
		}
		
	}
	
	class SortPair implements Comparable<SortPair>
	{
	  private int originalIndex;
	  private float value;

	  public SortPair(float value, int originalIndex)
	  {
	    this.value = value;
	    this.originalIndex = originalIndex;
	  }
	  
	  @Override public int compareTo(SortPair o)
	  {
	    return Float.compare(value, o.getValue());
	  }

	  public int getOriginalIndex()
	  {
	    return originalIndex;
	  }

	  public float getValue()
	  {
	    return value;
	  }

	}
	
	
	@Publish
	public org.geworkbench.events.SubpanelChangedEvent<?> publishSubpanelChangedEvent(
			org.geworkbench.events.SubpanelChangedEvent<?> event) {
		return event;
	}	
	
	@Override
	public Component getComponent() {
		return this;
	}
	
	private void saveTable(List<Dot> list){

		JFileChooser fc = new JFileChooser();
		CustomFileFilter filter = SaveFileFilterFactory.createCsvFileFilter();	
		fc.setFileFilter(filter);
		fc.setAcceptAllFileFilterUsed(false);
		String lastDir = null;
		if ((lastDir = getLastDir()) != null) {
			fc.setCurrentDirectory(new File(lastDir));
		}	 
		 
		String exportFile = null;			
		if (JFileChooser.APPROVE_OPTION == fc
				.showSaveDialog(null)) {
			exportFile = fc.getSelectedFile().getPath();				
			if (!filter.accept(new File(exportFile))) {
				exportFile += "." + filter.getExtension();
			}
		
		
		} else {
			return;
		}


		saveLastDir(fc.getSelectedFile().getParent());		
	 
		
		if (new File(exportFile).exists()) {
			int n = JOptionPane.showConfirmDialog(
					null,
					"The file exists, are you sure to overwrite?",
					"Overwrite?", JOptionPane.YES_NO_OPTION);
			if (n == JOptionPane.NO_OPTION || n == JOptionPane.CLOSED_OPTION) {
				JOptionPane.showMessageDialog(null, "Save cancelled.");
				return;
			}
		}
		 
			String str = "";

			str += "ProbeSet Id,Gene Sysmbol,P-value,Fold x,Anotation";
			for(Dot d: list){
				String s="";
				String markerLabel=d.getMarker().getLabel();				
				try{					
					String[] geneTitles = AnnotationParser.getInfo(maSet,
							markerLabel, AnnotationParser.DESCRIPTION);
					s=geneTitles[0];					
				}
				catch(Exception e){					
					//e.printStackTrace();
				}
				
				str += "\n" + d.getMarker().getLabel()+","+d.getMarker().getGeneName()+","+pvalue[d.getGeneRowNo()]
						+","+fold[d.getGeneRowNo()]+",\""+ s+"\"";
				
				
			}
			
			PrintWriter out = null;
			try {
				out = new PrintWriter(exportFile);
				out.print(str);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} finally {
				if ( out != null)
				out.close();
			}
			
		 
	
	}	 
	
	private String getLastDir(){
		String dir = null;
		try {
			File file = new File(lastDirConf);
			if (file.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(file));
				dir = br.readLine();
				br.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return dir;
	}
	
	private void saveLastDir(String dir){
		//save as last used dir
		try {
			BufferedWriter br = new BufferedWriter(new FileWriter(lastDirConf));
			br.write(dir);
			br.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
private class CellRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = -2697909778548788305L;
		
		private JPanel colorPanel = new JPanel();
		private JLabel label;
		private JTextArea textArea;

		/**
		 * Renders basic data input types JLabel, Color,
		 */
		public Component getTableCellRendererComponent(JTable jTable,
				Object obj, boolean param, boolean param3, int row, int col) {
			if (col == 0) {
				Component c = super.getTableCellRendererComponent(jTable, obj,
						param, param3, row, col);
				c.setBackground(Color.lightGray);
				((JLabel) c).setHorizontalAlignment(JLabel.LEFT);
				((JLabel) c).setBorder(BorderFactory.createRaisedBevelBorder());
				return c;
			} else if (obj instanceof Color) {
				colorPanel.setBackground((Color) obj);
				return colorPanel;
			} else if (obj instanceof JLabel) {
				label = (JLabel) obj;
				label.setOpaque(true);
				label.setFont(new Font("Arial", Font.PLAIN, 12));
				label.setBackground(new Color(225, 0, 0));
				label.setForeground(Color.black);
				label.setHorizontalAlignment(JLabel.CENTER);
				if (jTable.isRowSelected(row))
					label.setBackground(jTable.getSelectionBackground());
				return label;
			} else if (obj instanceof JTextArea) {
				textArea = (JTextArea) obj;
				if (jTable.isRowSelected(row))
					textArea.setBackground(jTable.getSelectionBackground());
				return textArea;
			}
			Component c = super.getTableCellRendererComponent(jTable, obj,
					param, param3, row, col);
			((JLabel) c).setHorizontalAlignment(JLabel.RIGHT);

			return c;
		}
		
		/*
		 * (non-Javadoc)
		 * @see javax.swing.table.DefaultTableCellRenderer#setValue(java.lang.Object)
		 */
		public void setValue(Object value) {
			if ((value != null) && (value instanceof Number)) {
				if (((Number) value).doubleValue() < 0.1)
					value = String.format("%.2E", value);
				else
					value = String.format("%.2f", value);
			}
			super.setValue(value);
		}
	}
}
