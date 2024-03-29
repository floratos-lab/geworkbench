package org.geworkbench.builtin.projects.history;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geworkbench.bison.datastructure.biocollections.DSDataSet;
import org.geworkbench.bison.datastructure.properties.DSExtendable;
import org.geworkbench.engine.config.VisualPlugin;
import org.geworkbench.engine.management.AcceptTypes;
import org.geworkbench.engine.management.Subscribe;


/**
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: First Genetic Trust Inc.</p>
 * @author First Genetic Trust
 * @version $Id$
 */

/**
 * This application component is responsible for displaying the history of
 * modifications (editing, normalization, filtering, etc) that a microarray
 * set has undergone.
 */
@AcceptTypes({DSDataSet.class}) public class HistoryPanel implements VisualPlugin {
	static private Log log = LogFactory.getLog(HistoryPanel.class);
    /**
     * Text to display when there are no user comments entered.
     */
    private final String DEFAULT_MESSAGE = "";

    protected String datasetHistory = DEFAULT_MESSAGE;
    private BorderLayout borderLayout1 = new BorderLayout();
    protected JScrollPane jScrollPane1 = new JScrollPane();
    protected JTextArea historyTextArea = new JTextArea(datasetHistory);
    protected JPanel historyPanel = new JPanel();

    public String getName() {
        return "History Pane";
    }

    public HistoryPanel() {
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void jbInit() throws Exception {
        historyPanel.setLayout(borderLayout1);
        historyTextArea.setText(datasetHistory);
        historyTextArea.setLineWrap(true);
        historyTextArea.setWrapStyleWord(true);
        historyTextArea.setEditable(false);
        historyPanel.add(jScrollPane1, BorderLayout.CENTER);
        jScrollPane1.getViewport().add(historyTextArea, null);
    }

    public Component getComponent() {
        return historyPanel;
    }

    /**
     * Application listener for receiving events that modify the currently
     * selected microarray set.
     *
     * @param e
     */
	@Subscribe
	public void receive(org.geworkbench.events.ProjectEvent e, Object source) {
		DSDataSet<?> maSet = e.getDataSet();
		if (maSet != null) {
			datasetHistory = DEFAULT_MESSAGE;

			Object[] values = maSet.getValuesForName(HISTORY);
			if (values != null && values.length > 0) {
				datasetHistory = (String) values[0];
				if (datasetHistory.trim().equals(""))
					datasetHistory = DEFAULT_MESSAGE;
			}

			historyTextArea.setText(datasetHistory);
			historyTextArea.setCaretPosition(0); // For long text.
		} else {
			log.debug("dataSet is null (root node)");
		}
	}

    /**
     * UPdate data history for pattern discovery.
     * @param e
     * @param source
     */
    @Subscribe
	public void receive(org.geworkbench.events.HistoryEvent e, Object source) {
    	DSDataSet<?> maSet = e.getDataSet();
		if (maSet != null) {
			datasetHistory = DEFAULT_MESSAGE;

			Object[] values = maSet.getValuesForName(HISTORY);
			if (values != null && values.length > 0) {
				datasetHistory = (String) values[0];
				if (datasetHistory.trim().equals(""))
					datasetHistory = DEFAULT_MESSAGE;
			}
			historyTextArea.setText(datasetHistory);
			historyTextArea.setCaretPosition(0);
		}
	}

    /**
	 * Used as the "name" in the name-value pair that keeps track of the history
	 * of changes that a given dataset is being submitted to.
	 */
	public static final String HISTORY = "History";

	// TODO this name probably is not really used 
	public static final String HISTORYDETAIL = "HistoryDetail";

    public static void addHistoryDetail(DSExtendable objectWithHistory,
			String detail) {
		objectWithHistory.clearName(HISTORYDETAIL);
		objectWithHistory.addNameValuePair(HISTORYDETAIL, detail);
	}

	public static void addToHistory(DSExtendable objectWithHistory,
			String newHistory) {

		Object[] prevHistory = objectWithHistory.getValuesForName(HISTORY);
		if (prevHistory != null) {
			objectWithHistory.clearName(HISTORY);
		}
		objectWithHistory.addNameValuePair(HISTORY, (prevHistory == null ? ""
				: (String) prevHistory[0])
				+ newHistory + "\n");
	}
	
	public static void addBeforeToHistory(DSExtendable objectWithHistory,
			String newHistory) {

		Object[] prevHistory = objectWithHistory.getValuesForName(HISTORY);
		if (prevHistory != null) {
			objectWithHistory.clearName(HISTORY);
		}
		objectWithHistory.addNameValuePair(HISTORY, newHistory + "\n" + (prevHistory == null ? ""
				: (String) prevHistory[0]));
	}
	
	public static String getHistory(DSExtendable objectWithHistory) {

		Object[] prevHistory = objectWithHistory.getValuesForName(HISTORY);
		 if (prevHistory != null)
			  return (String) prevHistory[0];
		 else
			 return null;
				 
	}
	
	public static boolean hasHistory(DSExtendable objectWithHistory)
	{
		String historyString = getHistory(objectWithHistory);
		if (historyString == null || historyString.trim().equals(""))
			return false;
		else
		    return true;
	}
	
}

