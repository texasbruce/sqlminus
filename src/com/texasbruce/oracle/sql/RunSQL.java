package com.texasbruce.oracle.sql;

import java.io.*;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;

import com.texasbruce.oracle.util.SQLConnMgr;

import java.util.Properties;

public class RunSQL {
    private static RunSQL instance = new RunSQL();

    protected RunSQL() {
    }
    
    private static boolean isLogConfig = Logger.getRootLogger().getAllAppenders().hasMoreElements();
    private static Logger logger = isLogConfig ? Logger.getLogger(RunSQL.class.getName()) : null;
    public static void log (Object o) {
    	log (o, null);
    }
    public static void log (Object o, Throwable e) {
    	if (isLogConfig) {
    		logger.info (o, e);
    	}
    	else {
    		System.out.println("[" + new java.util.Date().toString() + "] " + o);
    		if (e != null) {
                System.out.println(e.getMessage() + " - " + e.getClass().getName());
                for (Object st : e.getStackTrace()) {
                    System.out.println("\t" + st);
                }
    		}
    	}
    }

    public static RunSQL getInstance() {
        return instance;
    }

	public static void main(String args[])
    {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String url = args[0];
			String user = args[1];
			String password = args[2];
			String sql = args[3];
			boolean update = args.length > 4 && "1".equals(args[4]);
			boolean logResultsOnly = Boolean.parseBoolean(System.getProperty("LOGRESULTSONLY", "true"));
			
			conn = SQLConnMgr.getConnectionFromDriver(url, user, password);
			ps = conn.prepareStatement(sql);
			if(!logResultsOnly) log("STMT = " + sql);
			if(!logResultsOnly) log("update = " + update);
			if (update) {
				//20150213 ZZ only try twice
//				while (true) {
//				for (int i = 0; i < 2; i++) {
					try {
						int affectedRows = ps.executeUpdate();
						if(!logResultsOnly) log("affected rows = " + affectedRows);
//						break;
					}
					catch (Exception e) {
						log("", e);
//						e.printStackTrace();
					}
//				}
				try {
					conn.commit();
				}
				catch (Exception e) {
					log("", e);
				}
			}
			else {
				rs = ps.executeQuery();
				int columnCount = ps.getMetaData().getColumnCount();
				
				String output = "";
				for (int i = 1; i <= columnCount; i++) {
					output += ps.getMetaData().getColumnName(i) + "\t";
				}
				output += "\n";
				while (rs.next()) {
					for (int i = 1; i <= columnCount; i++) {
						Object o = rs.getObject(i);
						if (o instanceof oracle.sql.Datum && o.getClass().getName().contains("TIMESTAMP")) {
							o = rs.getTimestamp(i);
						} 
						else if (o instanceof oracle.sql.Datum && o.getClass().getName().contains("CLOB")) {
							o = rs.getString(i);
						}
						output += o + "\t";
					}
					output += "\n";
				}
				log("Query result:\n" + output);
			}
		}
		catch (Exception e) {
			log("", e);
		}
		finally {
			SQLConnMgr.closeResultSet(rs);
			SQLConnMgr.closeStatement(ps);
			SQLConnMgr.closeConnection(conn);
		}
		//20150219 ZZ
//		System.exit(0);
		Runtime.getRuntime().halt(0);
    }

}