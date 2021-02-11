package org.geworkbench.components.masterregulator;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListModel;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math.stat.correlation.SpearmansCorrelation;
import org.geworkbench.analysis.AbstractSaveableParameterPanel;
import org.geworkbench.bison.annotation.CSAnnotationContextManager;
import org.geworkbench.bison.annotation.DSAnnotationContext;
import org.geworkbench.bison.annotation.DSAnnotationContextManager;
import org.geworkbench.bison.datastructure.biocollections.AdjacencyMatrix;
import org.geworkbench.bison.datastructure.biocollections.AdjacencyMatrix.NodeType;
import org.geworkbench.bison.datastructure.biocollections.AdjacencyMatrixDataSet;
import org.geworkbench.bison.datastructure.biocollections.DSAncillaryDataSet;
import org.geworkbench.bison.datastructure.biocollections.DSDataSet;
import org.geworkbench.bison.datastructure.biocollections.microarrays.DSMicroarraySet;
import org.geworkbench.bison.datastructure.bioobjects.DSBioObject;
import org.geworkbench.bison.datastructure.bioobjects.markers.DSGeneMarker;
import org.geworkbench.bison.datastructure.bioobjects.microarray.DSMicroarray;
import org.geworkbench.bison.datastructure.complex.panels.DSPanel;
import org.geworkbench.builtin.projects.DataSetNode;
import org.geworkbench.builtin.projects.DataSetSubNode;
import org.geworkbench.builtin.projects.ProjectPanel;
import org.geworkbench.builtin.projects.ProjectSelection;
import org.geworkbench.events.listeners.ParameterActionListener;
import org.geworkbench.parsers.AdjacencyMatrixFileFormat;
import org.geworkbench.parsers.InputFileFormatException;
import org.geworkbench.util.FilePathnameUtils;
import org.geworkbench.util.Util;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.ArrayListModel;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Parameter Panel used for Master Regulator Analysis
 * 
 * @author yc2480
 * @version $Id$
 */
public final class MARINaPanel extends AbstractSaveableParameterPanel {
	private static final long serialVersionUID = -6160058089960168299L;

	private static final float PValueThresholdDefault = 0.01f;
	// private static final String TFGeneListDefault =
	// ("AFFX-HUMGAPDH/M33197_3_at, AFFX-HUMGAPDH/M33197_5_at,
	// AFFX-HUMGAPDH/M33197_M_at, AFFX-HUMRGE/M10098_3_at,
	// AFFX-HUMRGE/M10098_M_at");

	private Log log = LogFactory.getLog(this.getClass());
	private ArrayListModel<String> adjModel; 

	private JTextField pValueTextField = null;

	private JTextField networkTextField = null;

	private AdjacencyMatrixDataSet adjMatrix = null;
	private DSMicroarraySet maSet = null;

	private JComboBox networkMatrix = createNetworkMatrixComboBox();

	private JButton loadNetworkButton = new JButton("Load");

	private JComboBox networkFrom = null;

	private JTextField mintg = new JTextField("20");  // number of targets minimumto run GSEA
	private JTextField minsp = new JTextField("7");  //minimum number of samples for label shuffling
	private JTextField nperm = new JTextField("1000"); //number of permutations
	private JTextField tail = new JTextField("2");   //tail: If the Spearman's correlation value is not known, use tail = 1. Otherwise tail = 2. 2 for GSEA2, 1 for GSEA
	private JTextField pvshadow = new JTextField("0.01"); //Significance threshold for shadow analysis
	private JTextField pvsynergy = new JTextField("0.01"); //Significance threshold for synergy analysis
	private JTextField resultid = new JTextField("mra0001"); //mra result id for retrieving prior result
	private JCheckBox priorBox = new JCheckBox("Retrieve prior result with ID: ");
	private static final String lastDirConf = FilePathnameUtils.getUserSettingDirectoryPath()
					+ "masterregulator" + FilePathnameUtils.FILE_SEPARATOR + "lastDir.conf";
	boolean allpos = true;
	private int correlationCol = 3;

	public MARINaPanel() {
		networkTextField = new JTextField();
		networkTextField.setEditable(false);
		FormLayout layout = new FormLayout(
				"left:max(100dlu;pref), 10dlu, 100dlu, 10dlu, "
						+ "100dlu, 10dlu, 100dlu, 10dlu, 100dlu", "");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		builder.appendSeparator("Network");
		builder.append("Load Network");
		networkFrom = createNetworkFromComboBox();
		networkFrom.setSelectedIndex(1); // preselect "From File"
		// JComboBox networkMatrix = createNetworkMatrixComboBox();
		builder.append(networkFrom);
		networkMatrix.setEnabled(false);
		builder.append(networkMatrix);

		builder.append(networkTextField);

		// JButton loadNetworkButton=new JButton("Load");
		loadNetworkButton.addActionListener(new LoadNetworkButtonListener());
		builder.append(loadNetworkButton);
		builder.nextLine();

		builder.appendSeparator("Enrichment Threshold");
		builder.append("GSEA p-value ");
		if (pValueTextField == null)
			pValueTextField = new JTextField();
		pValueTextField.setText(Float.toString(PValueThresholdDefault));
		builder.append(pValueTextField);
		builder.nextLine();

		JTabbedPane jTabbedPane1 = new JTabbedPane();
		jTabbedPane1.addTab("Main", builder.getPanel());

		layout = new FormLayout(
				"left:max(100dlu;pref), 10dlu, 100dlu, 10dlu, "
						+ "100dlu, 10dlu, 100dlu, 10dlu, 100dlu", "");

		builder = new DefaultFormBuilder(new FormLayout(
				"left:max(60dlu;pref), 10dlu, 100dlu, 80dlu, "
				+ "60dlu, 10dlu, 100dlu", ""));
		builder.append("Minimum number of Targets", mintg);
		builder.append("GSEA Tail", tail);
		builder.append("Minimum number of Samples", minsp);
		builder.append("Shadow P-value", pvshadow);
		builder.append("Number of GSEA Permutations", nperm);
		builder.append("Synergy P-value", pvsynergy);
		builder.nextLine();

		builder.append(priorBox, resultid);
		resultid.setEnabled(false);
		priorBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (priorBox.isSelected()) {
					mintg.setEnabled(false);
					minsp.setEnabled(false);
					nperm.setEnabled(false);
					tail.setEnabled(false);
					pvshadow.setEnabled(false);
					pvsynergy.setEnabled(false);
					pValueTextField.setEnabled(false);
					networkFrom.setEnabled(false);
					networkMatrix.setEnabled(false);
					networkTextField.setEnabled(false);
					loadNetworkButton.setEnabled(false);
					resultid.setEnabled(true);
				} else {
					mintg.setEnabled(true);
					minsp.setEnabled(true);
					nperm.setEnabled(true);
					tail.setEnabled(true);
					pvshadow.setEnabled(true);
					pvsynergy.setEnabled(true);
					pValueTextField.setEnabled(true);
					networkFrom.setEnabled(true);
					if (networkFrom.getSelectedIndex()==0)
						networkMatrix.setEnabled(true);
					else{
						networkTextField.setEnabled(true);
						loadNetworkButton.setEnabled(true);
					}
					resultid.setEnabled(false);
				}
				parameterActionListener.actionPerformed(null);
			}
		});
		jTabbedPane1.addTab("MARINa parameters", null, builder.getPanel(), "The parameters on this tab apply to the remote MARINa grid service only.");
		this.add(jTabbedPane1, BorderLayout.CENTER);

		parameterActionListener = new ParameterActionListener(
				this);

		networkTextField.addActionListener(parameterActionListener);
		networkFrom.addActionListener(parameterActionListener);
		networkMatrix.addActionListener(parameterActionListener);

		pValueTextField.addActionListener(parameterActionListener);
		mintg.addActionListener(parameterActionListener);
		minsp.addActionListener(parameterActionListener);
		nperm.addActionListener(parameterActionListener);
		tail.addActionListener(parameterActionListener);
		pvshadow.addActionListener(parameterActionListener);
		pvsynergy.addActionListener(parameterActionListener);
		resultid.addActionListener(parameterActionListener);
		priorBox.addActionListener(parameterActionListener);

		networkTextField.addFocusListener(parameterActionListener);
		networkFrom.addFocusListener(parameterActionListener);
		networkMatrix.addFocusListener(parameterActionListener);
		loadNetworkButton.addFocusListener(parameterActionListener);
		pValueTextField.addFocusListener(parameterActionListener);
		mintg.addFocusListener(parameterActionListener);
		minsp.addFocusListener(parameterActionListener);
		nperm.addFocusListener(parameterActionListener);
		tail.addFocusListener(parameterActionListener);
		pvshadow.addFocusListener(parameterActionListener);
		pvsynergy.addFocusListener(parameterActionListener);
		resultid.addFocusListener(parameterActionListener);
	}
	private ParameterActionListener parameterActionListener;

	private class LoadNetworkButtonListener implements
			java.awt.event.ActionListener {

		@Override
		public void actionPerformed(java.awt.event.ActionEvent e) {
			if (e.getActionCommand().equals("Load")) {
				if (maSet == null) {
					DSDataSet<? extends DSBioObject> currentDataset = ProjectPanel
							.getInstance().getDataSet();
					if (currentDataset instanceof DSMicroarraySet) {
						maSet = (DSMicroarraySet) currentDataset;
					} else {
						JOptionPane
								.showMessageDialog(
										MARINaPanel.this,
										"The current dataset is not a proper microarray set.",
										"Dataset error",
										JOptionPane.ERROR_MESSAGE);
						return;
					}
				}

				JFileChooser chooser = new JFileChooser();
				String lastDir = null;
				if ((lastDir = getLastDir()) != null) {
					chooser.setCurrentDirectory(new File(lastDir));
				}
				chooser.setFileFilter(new AdjacencyMatrixFileFormat()
						.getFileFilter());
				chooser.showOpenDialog(MARINaPanel.this);
				if (chooser.getSelectedFile() != null) {
					File selectedFile = chooser.getSelectedFile();
					String adjMatrixFileStr = selectedFile.getPath();
					networkTextField.setText(adjMatrixFileStr);
					networkFilename = selectedFile.getName();
					saveLastDir(selectedFile.getParent());

					if (!openDialog())
						return;

					// no need to generate adjmatrix for 5col network file
					// because 5col network format is used only by grid mra as a
					// file
					if (!selectedFormat.equals(marina5colformat)) {
						try {
							AdjacencyMatrix matrix = AdjacencyMatrixDataSet
									.parseAdjacencyMatrix(adjMatrixFileStr,
											maSet, interactionTypeMap,
											selectedFormat,
											selectedRepresentedBy, isRestrict);

							adjMatrix = new AdjacencyMatrixDataSet(matrix, 0,
									adjMatrixFileStr, adjMatrixFileStr, maSet);
						} catch (InputFileFormatException e1) {
							log.error(e1.getMessage());
							e1.printStackTrace();
						}
					} else {
						adjMatrix = null;
					}
				} else {
					// user canceled
				}
			}
		}
	}

	private JComboBox createNetworkFromComboBox() {
		ArrayListModel<String> networkFromModel = new ArrayListModel<String>();
		networkFromModel.add("From Workspace");
		networkFromModel.add("From File");
		NetworkFromListener networkFromListener = new NetworkFromListener();
		SelectionInList<String> selectionInList = new SelectionInList<String>(
				(ListModel) networkFromModel);
		selectionInList.addPropertyChangeListener(networkFromListener);
		return BasicComponentFactory.createComboBox(selectionInList);
	}

	private class NetworkFromListener implements PropertyChangeListener {
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName() == "value")
				if (evt.getNewValue() == "From Workspace") {
					networkMatrix.setEnabled(true);
					loadNetworkButton.setEnabled(false);
					networkTextField.setEnabled(false);
					
					updateNetworkList();
				} else if (evt.getNewValue() == "From File") {
					networkMatrix.setEnabled(false);
					loadNetworkButton.setEnabled(true);
					networkTextField.setEnabled(true);
					// active load button
					// show file name loaded
				}
		}
	}

	private JComboBox createNetworkMatrixComboBox() {
		adjModel = new ArrayListModel<String>();
		// we'll generate network list in addAdjMatrixToCombobox()
		AdjListener adjListener = new AdjListener();
		SelectionInList<String> selectionInList = new SelectionInList<String>(
				(ListModel) adjModel);
		selectionInList.addPropertyChangeListener(adjListener);
		return BasicComponentFactory.createComboBox(selectionInList);
	}

	private class AdjListener implements PropertyChangeListener {

		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName() == "value")
				log.info("User select adj matrix: " + evt.getNewValue());
			for (Iterator<AdjacencyMatrixDataSet> iterator = adjacencymatrixDataSets
					.iterator(); iterator.hasNext();) {
				AdjacencyMatrixDataSet adjMatrixDataSet = (AdjacencyMatrixDataSet) iterator
						.next();
				if (adjMatrixDataSet.getDataSetName().equals(evt.getNewValue())) {
					adjMatrix = adjMatrixDataSet;
				}
			}
		}
	}

	// after user selected adjMatrix in the panel, you can use this method to
	// get the adjMatrix user selected.
	private AdjacencyMatrixDataSet getAdjMatrixDataSet() {
		if (!networkMatrix.isEnabled() && networkTextField.getText().length()==0) return null;
		return adjMatrix;
	}

	public double getPValue() {
		try {
			return Double.valueOf(pValueTextField.getText());
		} catch (NumberFormatException nfe) {
			return -1;
		}
	}

	public void setPValue(double d) {
		pValueTextField.setText(Double.toString(d));
	}

	ArrayList<AdjacencyMatrixDataSet> adjacencymatrixDataSets = new ArrayList<AdjacencyMatrixDataSet>();

	public String getSelectedAdjMatrix()
	{		 
		   return (String)networkMatrix.getSelectedItem();
	}
	
	public void setSelectedAdjMatrix(String datasetName)
	{		 
		networkMatrix.getModel().setSelectedItem(datasetName);
	}
	
	
	public void addAdjMatrixToCombobox(AdjacencyMatrixDataSet adjDataSet) {
		adjacencymatrixDataSets.add(adjDataSet);
		adjModel.add(adjDataSet.getDataSetName());		 
		
	}

	public void clearAdjMatrixCombobox() {
		adjacencymatrixDataSets.clear();
		adjModel.clear();
        networkTextField.setText(null);
	}

	public void removeAdjMatrixToCombobox(AdjacencyMatrixDataSet adjDataSet) {
		try {
			adjacencymatrixDataSets.remove(adjDataSet);
			int i = adjModel.indexOf(adjDataSet.getDataSetName());
			if(i>=0)adjModel.remove(i);
		} catch (Exception ex) {
			log.error(ex.getMessage());
		}

	}

	public void renameAdjMatrixToCombobox(AdjacencyMatrixDataSet adjDataSet,
			String oldName, String newName) {
		for (AdjacencyMatrixDataSet adjSet : adjacencymatrixDataSets) {
			if (adjSet == adjDataSet)
				adjSet.setLabel(newName);
		}
		adjModel.remove(oldName);
		adjModel.add(newName);
	}

	public void setMicroarraySet(DSMicroarraySet maSet) {
		this.maSet = maSet;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geworkbench.analysis.AbstractSaveableParameterPanel#setParameters(java.util.Map)
	 *      Set inputed parameters to GUI.
	 */
	public void setParameters(Map<Serializable, Serializable> parameters) {
		if (parameters == null)
			return; // FIXME: this is a quick patch for 0001691, should fix it
					// correctly.
		if (getStopNotifyAnalysisPanelTemporaryFlag() == true)
			return;
		stopNotifyAnalysisPanelTemporary(true);

		if (parameters.get("networkFrom") != null
				&& !parameters.get("networkFrom").toString().trim().equals(""))
			networkFrom.setSelectedIndex((Integer) parameters
					.get("networkFrom"));

     	if (parameters.get("networkMatrix") != null )
	        networkMatrix.setSelectedItem(parameters.get("networkMatrix"));
		
		String networkText = parameters.get("networkField")==null?null:parameters.get("networkField").toString();		 
		if (maSet != null && networkTextField.isEnabled()
				&& networkText != null && !networkText.trim().equals("")) {
			networkTextField.setText(networkText);
			networkFilename = new File(networkText).getName();
			if (!is5colnetwork(networkText, 10)){
				try {
					adjMatrix = new AdjacencyMatrixDataSet(
							0, networkText, networkText, maSet, networkText);
				} catch (InputFileFormatException e) {
					log.error(e.getMessage());
					e.printStackTrace();
				}
			}
		}

		if (parameters.get("GSEA p-value") != null) {
			double d = (Double) parameters.get("GSEA p-value");
			//if (d >= 0 && d <= 1)
			   setPValue(d);
			//else
			//   setPValue(0.01);
		}

		if (parameters.get("mintg") != null)
			setMintg((Integer)parameters.get("mintg"));
		if (parameters.get("minsp") != null)
			setMinsp((Integer)parameters.get("minsp"));
		if (parameters.get("nperm") != null)
			setNperm((Integer)parameters.get("nperm"));
		if (parameters.get("tail") != null)
			setTail((Integer)parameters.get("tail"));
		if (parameters.get("pvshadow") != null)
			setPVshadow((Double)parameters.get("pvshadow"));
		if (parameters.get("pvsynergy") != null)
			setPVsynergy((Double)parameters.get("pvsynergy"));
		if (parameters.get("resultid") != null)
			setResultid((String)parameters.get("resultid"));
		if (parameters.get("priorid") != null)
			setPriorid((Boolean)parameters.get("priorid"));

		stopNotifyAnalysisPanelTemporary(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geworkbench.analysis.AbstractSaveableParameterPanel#getParameters()
	 */
	public Map<Serializable, Serializable> getParameters() {
		Map<Serializable, Serializable> answer = new HashMap<Serializable, Serializable>();
		if (networkFrom.isEnabled())
			answer.put("networkFrom", networkFrom.getSelectedIndex());	
		if (networkMatrix.isEnabled() && networkMatrix.getSelectedItem() != null)
		   answer.put("networkMatrix", (String)networkMatrix.getSelectedItem());
		if (networkTextField.isEnabled())
			answer.put("networkField", networkTextField.getText());
    	
		//if (getPValue() > 1 || getPValue() < 0)
		//	answer.put("Fisher's Exact P Value", 0.01);
		//else
			answer.put("GSEA p-value", getPValue());

		if (mintg.isEnabled())     answer.put("mintg", getMintg());
		if (minsp.isEnabled())     answer.put("minsp", getMinsp());
		if (nperm.isEnabled())     answer.put("nperm", getNperm());
		if (tail.isEnabled())      answer.put("tail", getTail());
		if (pvshadow.isEnabled())  answer.put("pvshadow", getPVshadow());
		if (pvsynergy.isEnabled()) answer.put("pvsynergy", getPVsynergy());
		if (resultid.isEnabled())  answer.put("resultid", getResultid());
		answer.put("priorid", getPriorid());

		return answer;
	}

	@Override
	public void fillDefaultValues(Map<Serializable, Serializable> parameters) {
		// TODO Auto-generated method stub

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

	/**
	 * Test if the network is in 5-column format, and if all correlation cols are positive.
	 * @param fname    network file name
	 * @param numrows  test format in the first numrows; if numrows <= 0, test whole file.
	 * @return if the network is in 5-column format
	 */
	private boolean is5colnetwork(String fname, int numrows){
		if (!new File(fname).exists())
			return false;
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(fname));
			allpos = true;
			String line = null; int i = 0;
			while( (line = br.readLine()) != null && 
					(numrows <= 0 || i++ < numrows)) {
				String[] toks = line.split("\t");
				if (toks.length != 5 || !isDouble(toks[2]) 
						|| !isDouble(toks[3]) || !isDouble(toks[4]))
					return false;
				if (allpos && Double.valueOf(toks[correlationCol]) < 0)
					allpos = false;
			}
			log.info("This is a 5-column network");
			return true;
		}catch(IOException e){
			e.printStackTrace();
			return false;
		}finally{
			try{ 
				if (br!=null) br.close(); 
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	
	private boolean use5colnetwork(){
		return !networkMatrix.isEnabled() && selectedFormat.equals(marina5colformat);
	}

	private boolean isDouble(String s){
		try{
			Double.parseDouble(s);
			return true;
		}catch(NumberFormatException e){
			return false;
		}
	}

	public int getMintg() {
		try {
			return Integer.valueOf(mintg.getText());
		} catch (NumberFormatException e) {
			return -1;
		}
	}
	public void setMintg(int p){
		mintg.setText(Integer.toString(p));
	}
	public int getMinsp() {
		try {
			return Integer.valueOf(minsp.getText());
		} catch (NumberFormatException e) {
			return -1;
		}
	}
	public void setMinsp(int p){
		minsp.setText(Integer.toString(p));
	}
	public int getNperm() {
		try {
			return Integer.valueOf(nperm.getText());
		} catch (NumberFormatException e) {
			return -1;
		}
	}
	public void setNperm(int p){
		nperm.setText(Integer.toString(p));
	}
	public int getTail() {
		try {
			return Integer.valueOf(tail.getText());
		} catch (NumberFormatException e) {
			return -1;
		}
	}
	public void setTail(int p){
		tail.setText(Integer.toString(p));
	}
	public double getPVshadow() {
		try {
			return Double.valueOf(pvshadow.getText());
		} catch (NumberFormatException nfe) {
			return -1;
		}
	}
	public void setPVshadow(double d) {
		pvshadow.setText(Double.toString(d));
	}
	public double getPVsynergy() {
		try {
			return Double.valueOf(pvsynergy.getText());
		} catch (NumberFormatException nfe) {
			return -1;
		}
	}
	public void setPVsynergy(double d) {
		pvsynergy.setText(Double.toString(d));
	}
	public String getResultid(){
		if (!priorBox.isSelected()) return null;
		return resultid.getText().toLowerCase();
	}
	public void setResultid(String id){
		resultid.setText(id);
	}
	public boolean getPriorid(){
		return priorBox.isSelected();
	}
	public void setPriorid(boolean i){
		priorBox.setSelected(i);
	}

	private String networkFilename = "";
	public String getNetworkFilename(){
		if (!networkTextField.isEnabled()) return "adjMatrix5col.txt";
		return networkFilename;
	}

	/*get zipped network file in byte[]*/
	public byte[] getNetwork(){
		if (!networkFrom.isEnabled()) return null;
		if (use5colnetwork())
			return getNetworkFromFile();
		else return getNetworkFromAdjMatrix();
	}
	
	private byte[] getNetworkFromFile(){
		String fname = networkTextField.getText();
		if (!is5colnetwork(fname, 0))
			return null;

		int blocksize = 4096;
		FileInputStream in = null;
		GZIPOutputStream zipout = null;
		try {
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			zipout = new GZIPOutputStream(bo);
			byte[] buffer = new byte[blocksize];

			in = new FileInputStream(fname);
			int length;
			while ((length = in.read(buffer, 0, blocksize)) != -1)
				zipout.write(buffer, 0, length);
			zipout.close();
			return bo.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				if (in!=null)     in.close();
				if (zipout!=null) zipout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private byte[] getNetworkFromAdjMatrix(){
		AdjacencyMatrixDataSet amSet = getAdjMatrixDataSet();
		if (amSet==null) return null;
		AdjacencyMatrix matrix  = amSet.getMatrix();
		if (matrix==null) return null;
		boolean goodNetwork = false;
		allpos = true;
		GZIPOutputStream zipout = null;
		
		DSMicroarraySet microarraySet = (DSMicroarraySet) amSet.getParentDataSet();
		try{
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			zipout = new GZIPOutputStream(bo);

			for (AdjacencyMatrix.Node node1 : matrix.getNodes()) {
				DSGeneMarker marker1 = getMarkerInNode(node1, matrix, microarraySet);
				if (marker1 != null && marker1.getLabel() != null) {
					StringBuilder builder = new StringBuilder();
					for (AdjacencyMatrix.Edge edge : matrix.getEdges(node1)) {
						DSGeneMarker marker2 = getMarkerInNode(edge.node2, matrix, microarraySet);
						if (marker2 != null && marker2.getLabel() != null) {
							double rho = 1, pvalue = 0;
							double[] v1 = maSet.getRow(marker1);
							double[] v2 = maSet.getRow(marker2);
							if (v1 != null && v1.length > 0 && v2 != null && v2.length > 0){
								double[][] arrayData = new double[][]{v1, v2};
								RealMatrix rm = new SpearmansCorrelation().computeCorrelationMatrix(transpose(arrayData));
								if (rm.getColumnDimension() > 1)  rho = rm.getEntry(0, 1);
								if (allpos && rho < 0)  allpos = false;
								try{
									pvalue = new PearsonsCorrelation(rm, v1.length).getCorrelationPValues().getEntry(0, 1);
								}catch(Exception e){
									e.printStackTrace();
								}
							}
							builder.append(marker1.getLabel() + "\t");
							builder.append(marker2.getLabel() + "\t"
									+ edge.info.value +"\t"  // Mutual information
									+ rho+ "\t"   // Spearman's correlation = 1
									+ pvalue +"\n"); // P-value for Spearman's correlation = 0
						}
					}
					if (!goodNetwork && builder.length() > 0) goodNetwork = true;
					zipout.write(builder.toString().getBytes());
				}
			}
			zipout.close();
			if (!goodNetwork) return null;
			return bo.toByteArray();
		}catch(IOException e){
			e.printStackTrace();
			return null;
		}finally{
			if (zipout!=null) {
				try{
					zipout.close();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}

	private double[][] transpose(double[][] in){
		if (in==null || in.length==0 || in[0].length==0)
			return null;
		int row = in.length;
		int col = in[0].length;
		double[][] out = new double[col][row];
		for(int i=0; i<row; i++)
			for (int j=0; j<col; j++)
				out[j][i] = in[i][j];
		return out;
	}

	private DSGeneMarker getMarkerInNode(AdjacencyMatrix.Node node, AdjacencyMatrix matrix, DSMicroarraySet microarraySet){
		if (node == null || matrix == null) return null;
		DSGeneMarker marker = null;
		if (node.type == NodeType.MARKER) 
			marker = node.getMarker();
		else 
			marker = microarraySet.getMarkers().get(node.stringId);
		return marker;
	}
	 

	HashSet<String> getIxClass(String contextClass){
		DSAnnotationContextManager manager = CSAnnotationContextManager.getInstance();
		if (maSet == null && ProjectPanel.getInstance().getDataSet() instanceof DSMicroarraySet)
			maSet = (DSMicroarraySet)ProjectPanel.getInstance().getDataSet();
		DSAnnotationContext<DSMicroarray> context = manager.getCurrentContext(maSet);
		String[] groups = context.getLabelsForClass(contextClass);
		HashSet<String> hash = new HashSet<String>();
		for (String group : groups){
			if (context.isLabelActive(group)){
				DSPanel<DSMicroarray> panel = context.getItemsWithLabel(group);
				int size = panel.size();
				for (int i = 0; i < size; i++)
					hash.add(panel.get(i).getLabel());
			}
		}
		return hash;
	}

	private String[] representedByList;
	private String selectedRepresentedBy = AdjacencyMatrixDataSet.PROBESET_ID;
	private HashMap<String, String> interactionTypeMap = null;
	private boolean isRestrict = true;
	private boolean isCancel = false;
	private String selectedFormat = AdjacencyMatrixDataSet.ADJ_FORMART;
	private String marina5colformat = "marina 5-column format";

	private class LoadInteractionNetworkPanel extends JPanel {

		static final long serialVersionUID = -1855255412334333328L;

		final JDialog parent;

		private JComboBox formatJcb;
		private JComboBox presentJcb;

		public LoadInteractionNetworkPanel(JDialog parent) {

			setLayout(new BorderLayout());
			this.parent = parent;
			init();

		}

		private void init() {

			JPanel panel1 = new JPanel(new GridLayout(3, 2));
			JPanel panel3 = new JPanel(new GridLayout(0, 3));
			JLabel label1 = new JLabel("File Format:    ");

			formatJcb = new JComboBox();
			formatJcb.addItem(AdjacencyMatrixDataSet.ADJ_FORMART);
			formatJcb.addItem(AdjacencyMatrixDataSet.SIF_FORMART);
			formatJcb.addItem(marina5colformat);
			JLabel label2 = new JLabel("Node Represented By:   ");

			representedByList = new String[4];
			representedByList[0] = AdjacencyMatrixDataSet.PROBESET_ID;
			representedByList[1] = AdjacencyMatrixDataSet.GENE_NAME;
			representedByList[2] = AdjacencyMatrixDataSet.ENTREZ_ID;
			representedByList[3] = AdjacencyMatrixDataSet.OTHER;
			presentJcb = new JComboBox(representedByList);

			JButton continueButton = new JButton("Continue");
			JButton cancelButton = new JButton("Cancel");
			formatJcb.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent evt) {
					if (formatJcb.getSelectedItem().toString().equals(
							AdjacencyMatrixDataSet.ADJ_FORMART)) {
						representedByList = new String[4];
						representedByList[0] = AdjacencyMatrixDataSet.PROBESET_ID;
						representedByList[1] = AdjacencyMatrixDataSet.GENE_NAME;
						representedByList[2] = AdjacencyMatrixDataSet.ENTREZ_ID;
						representedByList[3] = AdjacencyMatrixDataSet.OTHER;
						presentJcb.setModel(new DefaultComboBoxModel(
								representedByList));
					} else if (formatJcb.getSelectedItem().toString().equals(
							marina5colformat)) {
						representedByList = new String[1];
						representedByList[0] = AdjacencyMatrixDataSet.PROBESET_ID;
						presentJcb.setModel(new DefaultComboBoxModel(
								representedByList));
					} else {
						representedByList = new String[3];
						representedByList[0] = AdjacencyMatrixDataSet.GENE_NAME;
						representedByList[1] = AdjacencyMatrixDataSet.ENTREZ_ID;
						representedByList[2] = AdjacencyMatrixDataSet.OTHER;
						presentJcb.setModel(new DefaultComboBoxModel(
								representedByList));
					}
				}
			});

			if (networkFilename.toLowerCase().endsWith(".sif"))
				formatJcb.setSelectedItem(AdjacencyMatrixDataSet.SIF_FORMART);
			else if (networkFilename.toLowerCase().contains("5col"))
				formatJcb.setSelectedItem(marina5colformat);
			else
				formatJcb.setSelectedItem(AdjacencyMatrixDataSet.ADJ_FORMART);
			continueButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					continueButtonActionPerformed();
					parent.dispose();
					isCancel = false;
				}
			});
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					parent.dispose();
					isCancel = true;
				}
			});

			panel1.add(label1);
			panel1.add(formatJcb);

			panel1.add(label2);
			panel1.add(presentJcb);

			panel3.add(cancelButton);
			panel3.add(new JLabel("  "));
			panel3.add(continueButton);
			
			this.add(panel1, BorderLayout.CENTER);
			this.add(panel3, BorderLayout.SOUTH);
			parent.getRootPane().setDefaultButton(continueButton);
		}

		private void continueButtonActionPerformed() {
			selectedFormat = formatJcb.getSelectedItem().toString();
			selectedRepresentedBy = presentJcb.getSelectedItem().toString();
		}

	}
	private boolean openDialog(){
		JDialog loadDialog = new JDialog();

		loadDialog.addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent e) {
				isCancel = true;
			}
		});

		isCancel = false;
		loadDialog.setTitle("Load Interaction Network");
		LoadInteractionNetworkPanel loadPanel = new LoadInteractionNetworkPanel(
				loadDialog);

		loadDialog.add(loadPanel);
		loadDialog.setModal(true);
		loadDialog.pack();
		Util.centerWindow(loadDialog);
		loadDialog.setVisible(true);

		if (isCancel)
			return false;

		if ((selectedFormat
				.equalsIgnoreCase(AdjacencyMatrixDataSet.SIF_FORMART) && !networkFilename
				.toLowerCase().endsWith(".sif"))
				|| (networkFilename.toLowerCase().endsWith(".sif") && !selectedFormat
						.equalsIgnoreCase(AdjacencyMatrixDataSet.SIF_FORMART))
				||(selectedFormat.equals(marina5colformat) && !is5colnetwork(networkTextField.getText(), 10))){
			JOptionPane.showMessageDialog(null,  "The network format selected does not match that of the file.",
					"Format Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		if (selectedFormat
				.equalsIgnoreCase(AdjacencyMatrixDataSet.SIF_FORMART)) {
			interactionTypeMap = new org.geworkbench.parsers.AdjacencyMatrixFileFormat().getInteractionTypeMap();
		}
		return true;
	}
	
	
	@Override
	public String getDataSetHistory() {			 
		StringBuffer histStr = new StringBuffer("Generated with MRA run with parameters:\n\n");		 
		
		if (getPriorid())
		{
			 histStr .append( "[PARA] : Retrieve prior result with ID:  ")
				.append(getResultid())
						.append( "\n\n\n" );			
		}else
		{
			AdjacencyMatrixDataSet set = getAdjMatrixDataSet();
			String setname = (set != null) ? set.getDataSetName() : this.getNetworkFilename();
		    histStr .append( "[PARA] Load Network: " + setname  ).append("\n");
	  	    histStr .append( "[PARA] GSEA p-value : " + getPValue() ).append("\n");		
		    histStr .append( "[PARA] Minimum number of Targets : " + mintg.getText() ).append("\n");
		    histStr .append( "[PARA] GSEA Tail : " + tail.getText() ).append("\n");
		    histStr .append( "[PARA] Minimum number of Samples : " + minsp.getText() ).append("\n");
		    histStr .append( "[PARA] Shadow P-value : " + pvshadow.getText() ).append("\n");
		    histStr .append( "[PARA] Number of GSEA Permutations : " + nperm.getText() ).append("\n");
		    histStr .append( "[PARA] Synergy P-value: " )
				.append( pvsynergy.getText() )
						.append( "\n\n\n" );
		}
		
		return histStr.toString();
	}
	
	String validateNetwork(){
		 if (use5colnetwork()) {
			 if (!is5colnetwork(networkTextField.getText(), 10)) return "Network file format error";
			 BufferedReader br = null;
				try{
					br = new BufferedReader(new FileReader(networkTextField.getText()));
					String line = null;
					while( (line = br.readLine()) != null) {
						String[] toks = line.split("\t");
						if (maSet.getMarkers().get(toks[0])!=null &&
								maSet.getMarkers().get(toks[1])!=null)
							return "Valid";
					}
					return "No matching markers";
				}catch(IOException e){
					e.printStackTrace();
					return "Network file IO exception";
				}finally{
					try{ 
						if (br!=null) br.close(); 
					}catch(IOException e){
						e.printStackTrace();
					}
				} 
		 } else {
			 AdjacencyMatrixDataSet amSet = getAdjMatrixDataSet();
			 if (amSet==null) return "Network (Adjacency Matrix) has not been loaded yet.";
			 AdjacencyMatrix matrix  = amSet.getMatrix();
			 if (matrix==null) return "Network (Adjacency Matrix) has not been loaded yet.";

			 DSMicroarraySet microarraySet = (DSMicroarraySet) amSet.getParentDataSet();
			 for (AdjacencyMatrix.Node node1 : matrix.getNodes()) {
				DSGeneMarker marker1 = getMarkerInNode(node1, matrix, microarraySet);
				 if (marker1 != null && marker1.getLabel() != null) {
					 for (AdjacencyMatrix.Edge edge : matrix.getEdges(node1)) {
						 DSGeneMarker marker2 = getMarkerInNode(edge.node2, matrix, microarraySet);
						 if (marker2 != null && marker2.getLabel() != null) return "Valid";
					 }
				 }
			 }
		 }
		 return "No matching markers";
	}

	private void updateNetworkList() {
		ProjectSelection selection = ProjectPanel.getInstance().getSelection();
		DataSetNode dNode = selection.getSelectedDataSetNode();
		if (dNode == null) {
			return;
		}

		String currentTargetSet = this.getSelectedAdjMatrix();
		this.clearAdjMatrixCombobox();
		Enumeration<?> children = dNode.children();
		while (children.hasMoreElements()) {
			Object obj = children.nextElement();
			if (obj instanceof DataSetSubNode) {
				DSAncillaryDataSet<?> ads = ((DataSetSubNode) obj)._aDataSet;
				if (ads instanceof AdjacencyMatrixDataSet) {
					this.addAdjMatrixToCombobox((AdjacencyMatrixDataSet) ads);
					if (currentTargetSet != null
							&& StringUtils.equals(ads.getDataSetName(),
									currentTargetSet.trim())) {
						this.setSelectedAdjMatrix(ads.getDataSetName());
					}
				}
			}
		}
	}
}
