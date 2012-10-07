/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */
package org.wwscc.dataentry;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wwscc.dataentry.ResultsPane.RandomThoughtsModel;
import org.wwscc.storage.Entrant;
import org.wwscc.storage.EventResult;
import org.wwscc.storage.SqliteDatabase;

/**
 * @author bwilson
 */
public class AnnouncerModelTests 
{
    @Before
    public void setUp()
    {
    }
    
    @After
    public void tearDown()
    {
    }
    
    @Test
	public void testRandomThoughts() throws IOException, URISyntaxException
    {
		int carid = 1;
		SqliteDatabase d = new SqliteDatabase(new File(getClass().getResource("randomthoughts.db").toURI()));
		d.setCurrentEvent(d.getEvents().get(0));
		d.setCurrentCourse(1);
		Entrant e = d.loadEntrant(carid, true);
		String classcode = e.getClassCode();
		
		List<EventResult> erlist = d.getResultsForClass(classcode);

		RandomThoughtsModel model = new RandomThoughtsModel();
		model.setData(erlist, e, true);
		Assert.assertEquals("none", model.getValueAt(2, 1));
    }
}
