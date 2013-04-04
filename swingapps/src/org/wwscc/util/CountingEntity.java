package org.wwscc.util;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;

/**
 * Wrap an HttpEntity so that we can attach a progress bar watcher to it
 */
public class CountingEntity extends HttpEntityWrapper
{
	String title;
	public CountingEntity(String monitorTitle, HttpEntity wrapped) { 
		super(wrapped);
		title = monitorTitle;
	}
	@Override
	public void writeTo(final OutputStream out) throws IOException {
		if (out instanceof MonitorProgressStream)
			wrappedEntity.writeTo(out);
		else
			wrappedEntity.writeTo(new MonitorProgressStream(title, out, wrappedEntity.getContentLength()));
	}
}