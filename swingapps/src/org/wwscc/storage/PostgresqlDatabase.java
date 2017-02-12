package org.wwscc.storage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.postgresql.util.PGobject;

public class PostgresqlDatabase extends SQLDataInterface {

	private static final Logger log = Logger.getLogger(PostgresqlDatabase.class.getCanonicalName());
	private Connection conn;
	
	public PostgresqlDatabase() throws SQLException
	{
		String url = "jdbc:postgresql://localhost/scorekeeper";
		Properties props = new Properties();
		props.setProperty("user","ww2017");
		props.setProperty("password","ww2017");
		//props.setProperty("ssl","true");
		conn = DriverManager.getConnection(url, props);
	}
	
	@Override
	public void start() throws IOException {
		try {
			conn.setAutoCommit(false);
		} catch (SQLException sqle) {
			throw new IOException(sqle);
		}
	}

	@Override
	public void commit() throws IOException {
		try {
			conn.setAutoCommit(true);
		} catch (SQLException sqle) {
			throw new IOException(sqle);
		}
	}

	@Override
	public void rollback() {
		try {
			conn.rollback();
		} catch (SQLException sqle) {
			log.warning("rollback failed");
		}
	}

	@Override
	public int lastInsertId() throws IOException {
		throw new IOException("Remove last insert ids");
	}


	void bindParam(PreparedStatement p, List<Object> args) throws SQLException
	{
		for (int ii = 0; ii < args.size(); ii++)
		{
			Object v = args.get(ii);
			if (v == null) {
				p.setNull(ii+1, java.sql.Types.NULL);
			} else if (v instanceof Integer) {
				p.setInt(ii+1, (Integer)v);
			} else if (v instanceof Long) {
				p.setLong(ii+1, (Long)v);
			} else if (v instanceof Double) {
				p.setDouble(ii+1, (Double)v);
			} else if (v instanceof String) {
				p.setString(ii+1, (String)v);
			} else if (v instanceof Boolean) {
				p.setBoolean(ii+1, (Boolean)v);
			} else if (v instanceof UUID) {
				p.setObject(ii+1, v);
			} else if (v instanceof JSONObject) {
				PGobject pgo = new PGobject();
				pgo.setType("json");
				pgo.setValue(((JSONObject)v).toJSONString());
				p.setObject(ii+1, pgo);
			} else {
				throw new SQLException("unexpected param type: " + v.getClass());
			}
		}
	}
	
	
	ResultRow loadRow(ResultSet rs) throws SQLException
	{
		ResultRow row = new ResultRow();
		ResultSetMetaData meta = rs.getMetaData();
		
		for (int ii = 1; ii <= meta.getColumnCount(); ii++)
		{
			int type = meta.getColumnType(ii);
			String name = meta.getColumnName(ii);

			if (type == Types.INTEGER) {
				row.put(name, rs.getInt(ii));
			} else if (type == Types.DOUBLE) {
				row.put(name, rs.getDouble(ii));
			} else if ((type == Types.BOOLEAN) || (type == Types.BIT)) {
				row.put(name, rs.getBoolean(ii));
			} else if (type == Types.VARCHAR) {
				row.put(name, rs.getString(ii));
			} else if (type == Types.BLOB) {
				row.put(name, rs.getBlob(ii));
			} else if (type == Types.OTHER) {
				row.put(name, rs.getObject(ii));
			} else if (type == Types.DATE) {
				row.put(name, rs.getDate(ii));
			} else if (type == Types.TIMESTAMP) {
				row.put(name, rs.getTimestamp(ii));
			} else {
				throw new SQLException("unexpected result type: " + type);
			}
		}
		
		return row;
	}

	
	
	@Override
	public void executeUpdate(String sql, List<Object> args) throws IOException {
		try {
			PreparedStatement p = conn.prepareStatement(sql);
			bindParam(p, args);
			p.executeUpdate();
			p.close();
		} catch (SQLException sqle) {
			throw new IOException(sqle);
		}
	}

	@Override
	public void executeGroupUpdate(String sql, List<List<Object>> args) throws IOException {
		try {
			PreparedStatement p = conn.prepareStatement(sql);
			for (List<Object> l : args) {
				bindParam(p, l);
				p.executeUpdate();
			}
			p.close();
		} catch (SQLException sqle) {
			throw new IOException(sqle);
		}
	}

	/*
	@Override
	public <T> List<T> executeSelect(String key, List<Object> args, Constructor<T> objc) throws SQLException 
	{
		try
		{
			List<T> result = new ArrayList<T>();
			PreparedStatement p = conn.prepareStatement(SQLMap.get(key));
			if (args != null)
				bindParam(p, args);
			ResultSet s = p.executeQuery();
			while (s.next()) {
				result.add(objc.newInstance(s));
			}
			p.close();
			return result;
		} 
		catch (Exception e)
		{
			throw new SQLException(e);
		}
	}
	*/
	
	@Override
	public ResultData executeSelect(String key, List<Object> args) throws IOException {
		try {
			PreparedStatement p = conn.prepareStatement(SQLMap.get(key));
			if (args != null)
				bindParam(p, args);
			ResultSet s = p.executeQuery();
			ResultData d = new ResultData();
			while (s.next()) {
				d.add(loadRow(s));
			}
			p.close();
			return d;
		} catch (SQLException sqle) {
			throw new IOException(sqle);
		}
	}

	@Override
	protected ResultData getCarAttributesImpl(String attr) throws IOException {
		// TODO Auto-generated method stub
		throw new IOException("do this");
	}

	@Override
	public void close() {
		try {
			conn.close();
		} catch (SQLException sqle) {
			log.warning("closing error: " + sqle);
		}
	}

}
