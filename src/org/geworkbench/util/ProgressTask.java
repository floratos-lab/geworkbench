package org.geworkbench.util;

import javax.swing.JPanel;
import javax.swing.SwingWorker;

/**
 * An abstract SwingWorker subclass that contains a progress item.
 * 
 * Its subclass(eg. SubProgressTask) should call ProgressTask(pbtype, message) in its constructor,
 * and call progressDialog.removeTask(this) in done(),
 * and may call pb.setMessage() in process() if applicable.
 * 
 * Once a subProgressTask is created, use progressDialog.executeTask(subProgressTask) to start it.
 * $Id$
 */

public abstract class ProgressTask<T, V> extends SwingWorker<T, V> {
	
	final private ProgressItem pb;
	
	/**
	 * Create and show ProgressItem 
	 * add progress listener if progress bar is bounded
	 * @param pbtype: progress bar type: ProgressItem.INDETERMINATE_TYPE/BOUNDED_TYPE
	 * @param message: initial message for the progress bar
	 */
	public ProgressTask(int pbtype, String message){
		pb = new ProgressItem(pbtype, this);
		pb.setMessage(message);
		pb.start();
		
		if (pbtype == ProgressItem.BOUNDED_TYPE)
			addPropertyChangeListener( pb );
	}

	public void setMessage(String message) {
		pb.setMessage(message);
	}
	
	public void addToPanel(final JPanel container) {
		container.add(pb);
	}

	public void removeFromPanel(final JPanel container) {
		container.remove(pb);
	}
}