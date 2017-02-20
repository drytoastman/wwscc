/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author bwilson
 */
public class FakeDatabase extends SQLDataInterface
{
	public FakeDatabase() {}

	@Override
	public void open(String series, String password) {}
	@Override
	public void close() {}
	@Override
	public void start() throws SQLException {}
	@Override
	public void commit() throws SQLException {}
	@Override
	public void rollback() {}
	@Override
	public int executeUpdate(String sql, List<Object> args) throws SQLException { return -1; }
	@Override
	public void executeGroupUpdate(String sql, List<List<Object>> args) throws SQLException {}
	@Override
	public ResultSet executeSelect(String sql, List<Object> args) throws SQLException { throw new SQLException("fake database"); }
	@Override
	public void closeLeftOvers() {}
	@Override
	public <T> List<T> executeSelect(String key, List<Object> args, Constructor<T> objc) throws SQLException { return new ArrayList<T>(); }
}

