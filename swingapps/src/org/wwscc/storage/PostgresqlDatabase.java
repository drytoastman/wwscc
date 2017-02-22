package org.wwscc.storage;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.postgresql.util.PGobject;

public class PostgresqlDatabase extends SQLDataInterface 
{
	private static final Logger log = Logger.getLogger(PostgresqlDatabase.class.getCanonicalName());
	private static final List<String> ignore = Arrays.asList(new String[] {"information_schema", "pg_catalog", "public"});
	
	private Connection conn;
	private Map<ResultSet, PreparedStatement> leftovers;
	
	public PostgresqlDatabase()
	{
		conn = null;
		leftovers = new HashMap<ResultSet, PreparedStatement>();
	}

	static public List<String> getSeriesList()
	{
	    List<String> ret = new ArrayList<String>();
		try
		{
			String url = "jdbc:postgresql://127.0.0.1/scorekeeper";
			Properties props = new Properties();
			props.setProperty("user", "scorekeeper");
			props.setProperty("password", "scorkeeeper");
			//props.setProperty("ssl","true");
			Connection sconn = DriverManager.getConnection(url, props);

			DatabaseMetaData meta = sconn.getMetaData();
		    ResultSet rs = meta.getSchemas();
		    while (rs.next()) {
		    	String s = rs.getString("TABLE_SCHEM");
		    	if (!ignore.contains(s))
		    		ret.add(s);
		    }
		    rs.close();
		    sconn.close();
		}
		catch (SQLException sqle)
		{
			logError("getSeriesList", sqle);
		}
		
		return ret;
	}
    
	@Override
	public void open(String series, String password)
	{
		try
		{
			if ((conn != null) && (!conn.isClosed()))
				close();
			
			String url = "jdbc:postgresql://127.0.0.1/scorekeeper";
			Properties props = new Properties();
			props.setProperty("user", series);
			props.setProperty("password", password);
			//props.setProperty("ssl","true");
			conn = DriverManager.getConnection(url, props);
		} 
		catch (SQLException sqle)
		{
			log.severe(String.format("Error opening %s: %s", series, sqle));			
		}
	}
	
	@Override
	public void close() 
	{
		try {
			if (conn != null)
				conn.close();
		} catch (SQLException sqle) {
			log.warning("closing error: " + sqle);
		}
	}

	@Override
	public void start() throws SQLException 
	{
		conn.setAutoCommit(false);
	}

	@Override
	public void commit() throws SQLException 
	{
		conn.setAutoCommit(true);
	}

	@Override
	public void rollback() {
		try {
			conn.rollback();
		} catch (SQLException sqle) {
			log.warning("Database rollback failed.  You should probably restart the application.");
		}
	}

	void bindParam(PreparedStatement p, List<Object> args) throws SQLException
	{
		if (args == null)
			return;
		
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
	
	@Override
	public Object executeUpdate(String sql, List<Object> args) throws SQLException 
	{
		Object ret = null;
		PreparedStatement p = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		bindParam(p, args);
		p.executeUpdate();
		
		ResultSet gen = p.getGeneratedKeys();
		if (gen.next())
			ret = gen.getObject(1);
		
		gen.close();
		p.close();
		return ret;
	}

	@Override
	public void executeGroupUpdate(String sql, List<List<Object>> args) throws SQLException 
	{
		PreparedStatement p = conn.prepareStatement(sql);
		for (List<Object> l : args) {
			bindParam(p, l);
			p.executeUpdate();
		}
		p.close();
	}


	@Override 
	public ResultSet executeSelect(String sql, List<Object> args) throws SQLException
	{
		PreparedStatement p = conn.prepareStatement(sql);
		if (args != null)
			bindParam(p, args);
		ResultSet s = p.executeQuery();
		synchronized(leftovers) {
			leftovers.put(s,  p);
		}
		return s;
	}

	
	@Override
	public void closeLeftOvers()
	{
		synchronized (leftovers)
		{
			for (ResultSet s : leftovers.keySet())
			{
				try {
					s.close();
					leftovers.get(s).close();
				} catch (SQLException sqle) {
					log.info("Error closing leftover statements: " + sqle);
				}
			}
			
			leftovers.clear();
		}
	}


	@Override
	public <T> List<T> executeSelect(String sql, List<Object> args, Constructor<T> objc) throws SQLException 
	{
		try
		{
			List<T> result = new ArrayList<T>();
			PreparedStatement p = conn.prepareStatement(sql);
			if (args != null)
				bindParam(p, args);
			ResultSet s = p.executeQuery();
			while (s.next()) {
				result.add(objc.newInstance(s));
			}
			s.close();
			p.close();
			return result;
		} 
		catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			throw new SQLException(e);
		}
	}
}
