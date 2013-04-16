package org.wwscc.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.ProgressMonitor;

/**
 * Provide progress bar for movement of data if it takes longer than about 100ms to complete. 
 */
public class MonitorProgressStream extends FilterOutputStream 
{
	int transferred = 0;
	ProgressMonitor monitor;
	public MonitorProgressStream(String title, OutputStream out, long max) { 
		super(out); 
		monitor = new ProgressMonitor(null, title, "Connecting...", 0, (int)max);
		monitor.setMillisToDecideToPopup(10);
		monitor.setMillisToPopup(10);
		monitor.setProgress(0);
	}
	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		out.write(b, off, len);
		transferred += len;
		monitor.setProgress(transferred);
	}
	@Override
	public void write(final int b) throws IOException {
		out.write(b);
		transferred++;
		monitor.setProgress(transferred);
	}
	@Override
	public void close() throws IOException {
		super.close();
		monitor.close();
	}
}
