package org.geworkbench.components.skyline;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geworkbench.analysis.AbstractAnalysis;
import org.geworkbench.bison.datastructure.biocollections.DSDataSet;
import org.geworkbench.bison.datastructure.biocollections.views.DSMicroarraySetView;
import org.geworkbench.bison.datastructure.bioobjects.markers.DSGeneMarker;
import org.geworkbench.bison.datastructure.bioobjects.microarray.DSMicroarray;
import org.geworkbench.bison.model.analysis.AlgorithmExecutionResults;
import org.geworkbench.bison.model.analysis.ParamValidationResults;
import org.geworkbench.bison.model.analysis.ProteinStructureAnalysis;

/**
 * Run SkyLine analysis on grid service on web1 Replaces all values less (or
 * more) than a user designated Threshold X with the value X.
 * 
 * @author mw2518
 * @author zji
 * @version $Id: SkyLineAnalysis.java,v 1.8 2009-09-10 16:40:26 chiangy Exp $
 * 
 */
public class SkyLineAnalysis extends AbstractAnalysis implements
		ProteinStructureAnalysis {
	private static final long serialVersionUID = 5531166361344848544L;
	Log log = LogFactory.getLog(SkyLineAnalysis.class);

	private final String analysisName = "SkyLine";
	
	protected static final int MINIMUM = 0;
	protected static final int MAXIMUM = 1;
	protected static final int IGNORE = 0;
	protected static final int REPLACE = 1;

	private	SkyLineConfigPanel slp;

	public SkyLineAnalysis() {
		slp = new SkyLineConfigPanel();
		setDefaultPanel(slp);
	}

	public int getAnalysisType() {
		return SKYLINE_TYPE;
	}

	/** implements org.geworkbench.bison.model.analysis.Analysis.execute */
	public AlgorithmExecutionResults execute(Object input) {
		// inform the user that only remote service is available
		return new AlgorithmExecutionResults(
				false,
				"SkyLine does not have a local algorithm service.  Please choose \"Grid\" on the Services tab.",
				null);
	}

	@Override
	public ParamValidationResults validInputData(
			DSMicroarraySetView<DSGeneMarker, DSMicroarray> maSetView,
			DSDataSet<?> refMASet) {
		// TODO Auto-generated method stub
		return new ParamValidationResults(true, null);
	}

}
