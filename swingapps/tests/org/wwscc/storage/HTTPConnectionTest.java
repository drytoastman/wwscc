package org.wwscc.storage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.Test;

public class HTTPConnectionTest
{
	static Path file = Paths.get("junk.db");
	
	@AfterClass
	public static void tearDown() throws Exception 
	{
		Files.delete(file);
	}
	
	@Test
	public void test() throws IOException, URISyntaxException
	{
		RemoteHTTPConnection c = new RemoteHTTPConnection("scorekeeper.wwscc.org");
		c.downloadDatabase(file.toFile(), "ww2013", false);
	}
}
