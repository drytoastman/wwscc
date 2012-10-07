/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */
package org.wwscc.dataentry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wwscc.dataentry.ResultsPane.RandomThoughtsModel;
import org.wwscc.storage.Database;
import org.wwscc.storage.Entrant;
import org.wwscc.storage.EventResult;
import org.wwscc.storage.Run;
import org.wwscc.storage.SqliteDatabase;

/**
 * @author bwilson
 */
public class AnnouncerModelTests 
{
	File working;
	RandomThoughtsModel model;
	String classcode;
	List<Entrant> entrants;
	
    @Before
    public void setUp() throws IOException, URISyntaxException
    {
		File orig = new File(getClass().getResource("randomthoughts.db").toURI());
		working = new File("_test.db");
		
		FileChannel source = null;
		FileChannel destination = null;
		try {
			source = new FileInputStream(orig).getChannel();
			destination = new FileOutputStream(working).getChannel();
			destination.transferFrom(source, 0, source.size());
		} finally {
			if(source != null) source.close();
			if(destination != null) destination.close();
		}
		
		Database.d = new SqliteDatabase(working);
		Database.d.setCurrentEvent(Database.d.getEvents().get(0));
		Database.d.setCurrentCourse(1);
		
		model = new RandomThoughtsModel();
		classcode = "PRO1";
		
		entrants = new ArrayList<Entrant>();
		entrants.add(null);
		entrants.add(Database.d.loadEntrant(1, true));
		entrants.add(Database.d.loadEntrant(2, true));	
		entrants.add(Database.d.loadEntrant(3, true));
    }
    
    @After
    public void tearDown()
    {
		Database.d.close();
		working.delete();
    }
    
	private void applyRun(int carid, Run r, int run)
	{
		Entrant e = entrants.get(carid);
		if (r != null)
			e.setRun(r, run);
		List<EventResult> erlist = Database.d.getResultsForClass(classcode);
		model.setData(erlist, e, true);		
	}
	
    @Test
	public void testRandomThoughts() throws IOException, URISyntaxException
    {
		applyRun(1, null, 2);
		Assert.assertEquals("none", model.getValueAt(ResultsPane.MOVE, 1));
		applyRun(2, new Run(54.100), 2);
		Assert.assertEquals("3 to 1", model.getValueAt(ResultsPane.MOVE, 1));
		applyRun(3, new Run(54.800), 2);
		Assert.assertEquals("0.142", model.getValueAt(ResultsPane.FIRST, 1));
		Assert.assertEquals("0.142", model.getValueAt(ResultsPane.NEXT, 1));
    }
}
