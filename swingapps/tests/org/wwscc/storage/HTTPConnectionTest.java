package org.wwscc.storage;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;

public class HTTPConnectionTest
{

	@Test
	public void test() throws IOException, URISyntaxException
	{
		RemoteHTTPConnection c = new RemoteHTTPConnection("scorekeeper.wwscc.org");
		c.downloadDatabase(new File("junk.db"), "ww2013", false);
	}

}
