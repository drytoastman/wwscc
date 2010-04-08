/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author bwilson
 */
public class FakeDatabase extends SQLDataInterface
{
	ResultData fakedata = new ResultData();
	public FakeDatabase()
	{
		host = "127.0.0.1";
		seriesName = "none";
		currentEvent = new Event();
		currentCourse = 1;
		currentRunGroup = 1;
		currentChallengeId = 1;
	}

	@Override
	public void close() {}
	@Override
	public void setCurrentEvent(Event e) {};
	@Override
	public void start() throws IOException {}
	@Override
	public void commit() throws IOException {}
	@Override
	public void rollback() {}
	@Override
	public int lastInsertId() throws IOException { return -1; }
	@Override
	public void executeUpdate(String sql, List<Object> args) throws IOException {}
	@Override
	public void executeGroupUpdate(String sql, List<List<Object>> args) throws IOException {}
	@Override
	public ResultData executeSelect(String sql, List<Object> args) throws IOException { return fakedata; }
	@Override
	protected ResultData getCarAttributesImpl(String attr) throws IOException { return fakedata; }
}

