/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2007) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/
package org.geworkbench.components.gpmodule;

import java.awt.BorderLayout;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;

import org.genepattern.webservice.TaskInfo;
import org.geworkbench.analysis.AbstractSaveableParameterPanel;
import org.geworkbench.bison.model.analysis.ParameterPanel;

/**
 * @author: Marc-Danie Nazaire
 * @version $Id$
 */
public abstract class GPAnalysisPanel extends AbstractSaveableParameterPanel {

	private static final long serialVersionUID = 8988606419954213367L;
	
	private JTabbedPane gpTabbedPane;
    private GPConfigPanel gpConfigPanel;
    private GPHelpPanel gpHelpPanel;
    protected ParameterPanel parameterPanel;
    
    public GPAnalysisPanel(ParameterPanel panel, String label)
    {
        gpTabbedPane = new JTabbedPane();       
        gpHelpPanel = new GPHelpPanel(label, getParamDescriptionFile(), getDescriptionFile());
        parameterPanel = panel;
        parameterPanel.setLayout(new BorderLayout());
        gpConfigPanel = new GPConfigPanel();
        gpConfigPanel.setPreferredSize(getPreferredSize());
        gpConfigPanel.setMinimumSize(getPreferredSize());
        gpConfigPanel.setMaximumSize(getPreferredSize());
    }

    protected abstract void initParameterPanel();
    protected abstract URL getDescriptionFile();
    protected abstract URL getParamDescriptionFile();


    protected void init()
    {
        setLayout(new BorderLayout());
        initParameterPanel();

        gpTabbedPane.addTab("Parameters", parameterPanel);
        gpTabbedPane.addTab("GenePattern Server Settings", gpConfigPanel);
        gpTabbedPane.addTab("Help", gpHelpPanel);

        add(gpTabbedPane, BorderLayout.PAGE_START);
    }

    public String getPassword()
    {
        return gpConfigPanel.getPassword();
    }

    protected JLabel getGPLogo()
    {
        java.net.URL imageURL = GPAnalysisPanel.class.getResource("images/gp-logo.gif");
        ImageIcon image = new ImageIcon(imageURL);

        JLabel label = new JLabel();
        label.setIcon(image);

        return label;
    }

    public TaskInfo getModuleInfo()
    {
        return gpConfigPanel.getTaskInfo();            
    }
}
