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
package org.geworkbench.components.gpmodule.classification;

import java.awt.BorderLayout;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.geworkbench.bison.model.analysis.ParameterPanel;
import org.geworkbench.components.gpmodule.GPConfigPanel;
import org.geworkbench.components.gpmodule.GPHelpPanel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;

/**
 * @author: Marc-Danie Nazaire
 * @version $Id$
 */
public abstract class GPTrainingPanel extends AbstractTrainingPanel
{
	private static final long serialVersionUID = -7779793692399022977L;
	
	private JTabbedPane gpTabbedPane;
    private GPConfigPanel gpConfigPanel;
    private GPHelpPanel gpHelpPanel;
    protected ParameterPanel parameterPanel;

    public GPTrainingPanel(String label)
    {
        gpTabbedPane = new JTabbedPane();
        gpConfigPanel = new GPConfigPanel();
        gpConfigPanel.setPreferredSize(getPreferredSize());
        gpConfigPanel.setMinimumSize(getPreferredSize());
        gpConfigPanel.setMaximumSize(getPreferredSize());
        gpHelpPanel = new GPHelpPanel(label, getParamDescriptFile(), getSummaryFile());
    }

    protected abstract JPanel getParameterPanel();
    protected abstract URL getSummaryFile();
    protected abstract URL getParamDescriptFile();


    protected void init()
    {
        setLayout(new BorderLayout());
    }

    protected JLabel getGPLogo()
    {
        java.net.URL imageURL = GPTrainingPanel.class.getResource("images/gp-logo.gif");
        ImageIcon image = new ImageIcon(imageURL);

        JLabel label = new JLabel();
        label.setIcon(image);

        return label;
    }
    
    // FIXME it is a design problem to allow this method to be invoked from both EDT and non-EDT.
    // It is actually invoked both ways.
    // For now, let's keep it safe first.
    protected void addParameters(final DefaultFormBuilder builder)
    {
    	if(SwingUtilities.isEventDispatchThread()) {
    		addParameterFromEDT(builder);
    	} else {
    		try {
				SwingUtilities.invokeAndWait(new Runnable() {

					@Override
					public void run() {
						addParameterFromEDT(builder);
					}
					
				});
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
    	}
    }
    
    private void addParameterFromEDT(DefaultFormBuilder builder) {
        gpTabbedPane.removeAll();

        JPanel parameterPanel = getParameterPanel();
        gpTabbedPane.addTab("Parameters", parameterPanel);
        gpTabbedPane.addTab("GenePattern Server Settings", gpConfigPanel);
        gpTabbedPane.addTab("Help", gpHelpPanel);

        CellConstraints cc = new CellConstraints();
        builder.append("");
        
        builder.add(gpTabbedPane, cc.xywh(1, builder.getRow(), 7, 1));
    }

    public String getPassword()
    {
        return gpConfigPanel.getPassword();
    }

    public GPConfigPanel getConfigPanel()
    {
        return gpConfigPanel;
    }
}