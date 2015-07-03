package org.wwscc.registration.changeviewer;

import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

import org.wwscc.storage.Change;
import org.wwscc.storage.DataInterface;
import org.wwscc.storage.ProgressInterface;

class MergeProcess extends SwingWorker<Void,Void> implements ProgressInterface
{
	int counter = 0;
	ProgressMonitor monitor;
	ChangeViewer source;
	DataInterface dest;
	List<Change> changes;

	
	public MergeProcess(ChangeViewer source, List<Change> changes, DataInterface dest)
	{
		monitor = new ProgressMonitor(source, "Merge Progress", "connecting ...", 0, changes.size());
		monitor.setMillisToDecideToPopup(10);
		monitor.setMillisToPopup(10);
		this.source = source;
		this.changes = changes;
		this.dest = dest;
	}
	
	@Override
	public void step(){
		monitor.setProgress(++counter);
		monitor.setNote("Merging change " + counter);
	}
	
	@Override
	protected Void doInBackground() throws Exception
	{
		try {
			dest.mergeChanges(changes, this);
		} finally {
			monitor.close();
		}
		return null;
	}
	
	@Override
	public void done() 
	{
		JOptionPane.showMessageDialog(source, String.format("Merged %d changes", counter));
		source.archive();
	}
}