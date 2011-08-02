package com.behindthemirrors.minecraft.sRPG;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.sql.PreparedStatement;

import org.bukkit.entity.Player;
import com.avaje.ebeaninternal.server.lib.sql.DataSourceException;

public class Database {
	
	public static boolean debug = false;
	
    private Connection connection;
	String tablePrefix;
	String pass;
	String user;
	String name;
	String port;
	String server;
	
	String uint;
	String autoinc;
	String engine;
    String text;
    String unique;
	
    public Database() {
        // Load the driver instance
        try {
            if (Settings.mySQLenabled) {
            	Class.forName("com.mysql.jdbc.Driver").newInstance();
            } else {
            	URLClassLoader classLoader = new URLClassLoader(new URL[]{new URL("jar:file:" + (new File("lib/sqlitejdbc-v056.jar")).getPath()+"!/")});
            	classLoader.loadClass("org.sqlite.JDBC").newInstance();
            }
            uint = Settings.mySQLenabled ? "int(10) UNSIGNED" : "INTEGER";
        	autoinc = Settings.mySQLenabled ? "AUTO_INCREMENT" : "AUTOINCREMENT";
        	engine = Settings.mySQLenabled ? "ENGINE=MyISAM DEFAULT CHARSET=latin1" : "";
        	text = Settings.mySQLenabled ? "VARCHAR(40)" : "TEXT";
        	unique = Settings.mySQLenabled ? "UNIQUE KEY" : "UNIQUE";
        } catch (Exception ex) {
            throw new DataSourceException("Failed to initialize JDBC driver");
        }
        //make the connection
    }
    
    public boolean connect() {
    	getConnection();
    	return (connection != null);
    }
    
	//Create the DB structure
    public void createStructure(){
    	String prefix = "CREATE TABLE IF NOT EXISTS `" + tablePrefix;
    	update(prefix + "global` (`pk` "+uint+" NOT NULL PRIMARY KEY, `version` "+text+" NOT NULL) "+engine+";");
        update(prefix +  "users` (`user_id` "+uint+" NOT NULL PRIMARY KEY "+autoinc+"," +
                "`user` "+text+" NOT NULL "+unique+"," +
                "`hp` "+uint+" NOT NULL DEFAULT 0," +
                "`charges` "+uint+" NOT NULL DEFAULT 0," +
                "`chargeprogress` "+uint+" NOT NULL DEFAULT 0," +
                "`currentjob` "+text+"," +
                "`locale` "+text+" NOT NULL" +
                ") "+engine+";");
        String sql = prefix + "jobxp` (`user_id` "+uint+" NOT NULL PRIMARY KEY";
        for (String name : Settings.jobsettings.getKeys("tree")) {
        	sql += ",`" + name + "` "+uint+" NOT NULL DEFAULT 0";
        }
        sql += ") "+engine+";";
        update(sql);
    }
    
    public void updateDatabase(String version) {
    	createStructure();
    	// here be version-dependent database update routines
    	Integer id = SRPG.database.getSingleIntValue("global","pk","pk",1);
		if (id == null) {
			update("INSERT INTO " + tablePrefix + "global (pk,version) VALUES (1,'" + version + "');");
		}
        String db_version = SRPG.database.getSingleStringValue("global","version","pk",1);
        if (!version.equalsIgnoreCase(db_version)) {
        	SRPG.output("Version changed from "+db_version+" to "+version+", updating database structure if necessary");
        }
        // change database according to version differences
        if (db_version.equalsIgnoreCase("0.5alpha1") || db_version.equalsIgnoreCase("0.5alpha2")) {
        	SRPG.output("updating user table");
        	createColumn("users","class",text+" NOT NULL DEFAULT 'adventurer' AFTER user");
        	createColumn("users","hp",uint+" NOT NULL DEFAULT 0 AFTER locale");
        	db_version = "0.5alpha3";
        }
        if (db_version.equalsIgnoreCase("0.5alpha3")) {
        	SRPG.output("changing lots of tables");
        	dropTable("chargedata");
        	dropTable("skillpoints");
        	update("ALTER TABLE " + tablePrefix + "users CHANGE COLUMN class currentjob "+text+";");
        	dropColumn("users", "xp");
        	ArrayList<String> columns = new ArrayList<String>();
        	columns.addAll(Arrays.asList(new String[] {"charges","chargeprogress"}));
        	createIntColumnsIfNotExist("users",columns);
        	for (ArrayList<String> row : query("SELECT user_id FROM "+tablePrefix+"users;")) {
        		insertSingleIntValue("jobxp", "user_id", Integer.parseInt(row.get(0)));
        	}
        }
        createStructure();
        // update version in database to current version
        setSingleStringValue("global", "version", version, "pk", 1);
        
        ArrayList<String> columns = new ArrayList<String>();
		columns.addAll(Settings.jobs.keySet());
		SRPG.database.createIntColumnsIfNotExist("jobxp", columns);
    }
    
    // check if its closed
    private void getConnection() {
    	try {
    		if (connection != null && !connection.isClosed()) {
    			connection.close();
    		}
            if (Settings.mySQLenabled) {
            	connection = DriverManager.getConnection("jdbc:mysql://" + server + ":" + port + "/" + name + "?user=" + user + "&password=" + pass);            
            } else {
            	connection = DriverManager.getConnection("jdbc:sqlite:plugins/srpg/srpg.db");
            }
            
            SRPG.output("Connection success");
        } catch (SQLException ex) {
        	SRPG.output("Connection to database failed. Check status of MySQL server or write permissions for SQLite .db");
        	SRPG.output("SQLException: " + ex.getMessage());
        	SRPG.output("SQLState: " + ex.getSQLState());
        	SRPG.output("VendorError: " + ex.getErrorCode());
        }
    }
    
    private boolean reconnect() {
        try {
	        if(!Settings.mySQLenabled || connection.isValid(5)) {
	            return true;
	        } else {
	        	SRPG.output("Reconnecting to MySQL...");
	        	getConnection();
	        	if(connection.isValid(5)) {
	        		SRPG.profileManager.clear();
		            
		            for(Player player : SRPG.plugin.getServer().getOnlinePlayers()) {
		                SRPG.profileManager.add(player);
		            }
		            return true;
	        	}
	        }
        } catch (SQLException e) {
        	e.printStackTrace();
        }
        return false;
    }
    // write query
    public boolean update(String sql) {
        // Double check connection to MySQL
        if (reconnect()) {
        	PreparedStatement ps = null; 
        	try {
                ps = connection.prepareStatement(sql);
                ps.executeUpdate();
                return true;
    		} catch(SQLException ex) {
				SRPG.output("SQLException: " + ex.getMessage());
				SRPG.output("SQLState: " + ex.getSQLState());
				SRPG.output("VendorError: " + ex.getErrorCode());
    		} finally {
    			closeQuietly(ps);
    		}
        }
        return false;
    }
    
    // key/value type overloads start
    public boolean setSingleStringValue(String table, String column, String value, String keyColumn, String key) {
    	return setSingleValueRaw(table, column, "'"+value+"'", keyColumn, "'"+key+"'");
    }
    
    public boolean setSingleStringValue(String table, String column, String value, String keyColumn, Integer key) {
    	return setSingleValueRaw(table, column, "'"+value+"'", keyColumn, ""+key);
    }
    
    public boolean setSingleIntValue(String table, String column, Integer value, String keyColumn, String key) {
    	return setSingleValueRaw(table, column, ""+value, keyColumn, "'"+key+"'");
    }
    
    public boolean setSingleIntValue(String table, String column, Integer value, String keyColumn, Integer key) {
    	return setSingleValueRaw(table, column, ""+value, keyColumn, ""+key);
    }
    // key/value type overloads end
    
    public boolean setSingleValueRaw(String table, String column, String value, String keyColumn, String key) {
    	String sql = "UPDATE "+tablePrefix+table+" SET "+column+" = "+value+" WHERE "+keyColumn+" = "+key+";";
    	if (debug) {
    		SRPG.output("setSingleValue: "+sql);
    	}
    	return update(sql);
    }
    
    public boolean setValuesRaw(String table, HashMap<String,String> map, String keyColumn, String key) {
    	String sql = "UPDATE "+tablePrefix+table+" SET ";
    	boolean first = true;
    	for (Map.Entry<String,String> entry : map.entrySet()) {
    		if (!first) {
    			sql += ",";
    			first = false;
    		}
    		sql += entry.getKey() + " = " + entry.getValue();
    	}
    	sql += "WHERE "+keyColumn+" = "+key+";";
    	if (debug) {
    		SRPG.output("setValues: "+sql);
    	}
    	return update(sql);
    }
    
    // value type overloads begin
    public boolean insertSingleStringValue(String table, String column, String value) {
    	return insertSingleValueRaw(table, column, "'"+value+"'");
    }
    public boolean insertSingleIntValue(String table, String column, Integer value) {
    	return insertSingleValueRaw(table, column, ""+value);
    }
    // value type overloads end
    public boolean insertSingleValueRaw(String table, String column, String value) {
    	String sql = "INSERT INTO "+tablePrefix+table+" ("+column+") VALUES ("+value+");";
    	if (debug) {
    		SRPG.output("insertSingleValue: "+sql);
    	}
    	return update(sql);
    }
    
    // value type overloads start
    public boolean insertStringValues(String table, HashMap<String,String> map) {
    	Iterator<Map.Entry<String,String>> iterator = map.entrySet().iterator();
    	while (iterator.hasNext()){
    		Map.Entry<String, String> entry = iterator.next();
    		entry.setValue("'"+entry.getValue()+"'");
    	}
    	return insertValuesRaw(table, map);
    }
    public boolean insertIntValues(String table, HashMap<String,Integer> map) {
    	HashMap<String,String> newmap = new HashMap<String, String>();
    	for (Map.Entry<String, Integer> entry : map.entrySet()) {
    		newmap.put(entry.getKey(), ""+entry.getValue());
    	}
    	return insertValuesRaw(table, newmap);
    }
    // value type overloads end
    public boolean insertValuesRaw(String table, HashMap<String,String> map) {
    	ArrayList<String> columns = new ArrayList<String>();
    	ArrayList<String> values = new ArrayList<String>();
    	columns.addAll(map.keySet());
    	values.addAll(map.values());
    	String sql = "INSERT INTO "+tablePrefix+table+" ("+Utility.join(columns, ",")+") VALUES ("+Utility.join(values, ",")+");";
    	if (debug) {
    		SRPG.output("insertValues: "+sql);
    	}
    	return update(sql);
    }
    
    public void dropTable(String table) {
    	update("DROP TABLE " + tablePrefix + table + ";");
    }
    
    public void dropColumn(String table, String column) {
    	update("ALTER TABLE "+tablePrefix+table+" DROP COLUMN " + column + ";");
    }
    
    public boolean createColumn(String table, String column, String format) {
    	return update("ALTER TABLE "+tablePrefix+table+" ADD "+column+" "+format+";");
    }
    
    // value type overloads start
    public boolean createStringColumnsIfNotExist(String table, ArrayList<String> columns) {
    	return createColumnsIfNotExist(table, columns, " VARCHAR(40) NOT NULL");
    }
    public boolean createIntColumnsIfNotExist(String table, ArrayList<String> columns) {
    	return createColumnsIfNotExist(table, columns, " INT(10) NOT NULL DEFAULT '0'");
    }
    // value type overloads end
    public boolean createColumnsIfNotExist(String table, ArrayList<String> columns, String format) {
    	columns.removeAll(getColumns(table));
    	if (columns.isEmpty()) {
    		return true;
    	}
    	ArrayList<String> formatted = new ArrayList<String>();
    	for (String entry : columns) {
    		formatted.add(entry+" "+format);
    	}
    	String sql = "ALTER TABLE "+tablePrefix+table+" ADD ("+Utility.join(formatted,",")+");";
    	return update(sql);
    }
    
    public ArrayList<String> getColumns(String table) {
    	try {
            if(Settings.mySQLenabled && !connection.isValid(5)) {
            	reconnect();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        ArrayList<String> columns = new ArrayList<String>();
        
        try {
        	ps = connection.prepareStatement("SELECT * FROM "+tablePrefix+table+";");
            rs = ps.executeQuery();
            for (int i = 1;i <= rs.getMetaData().getColumnCount();i++) {
            	columns.add(rs.getMetaData().getColumnName(i));
            }
        } catch (SQLException ex) {
        	SRPG.output("SQLException: " + ex.getMessage());
        	SRPG.output("SQLState: " + ex.getSQLState());
        	SRPG.output("VendorError: " + ex.getErrorCode());
        } finally {
        	closeQuietly(ps);
        	closeQuietly(rs);
        }
        return columns;
    }
    
    // key/value type overloads start
    public Integer getSingleIntValue(String table, String column, String keyColumn, String key) {
    	try {
    		return Integer.parseInt(getSingleValueRaw(table, column, keyColumn, "'"+key+"'"));
	    } catch (NumberFormatException ex) {
			return null;
		}
    }
    
    public Integer getSingleIntValue(String table, String column, String keyColumn, Integer key) {
    	try {
    		return Integer.parseInt(getSingleValueRaw(table, column, keyColumn, ""+key));
    	} catch (NumberFormatException ex) {
    		return null;
    	}
    }
    
    public String getSingleStringValue(String table, String column, String keyColumn, String key) {
    	return getSingleValueRaw(table, column, keyColumn, "'"+key+"'");
    }
    
    public String getSingleStringValue(String table, String column, String keyColumn, Integer key) {
    	return getSingleValueRaw(table, column, keyColumn, ""+key);
    }
    // key/value type overloads end
    public String getSingleValueRaw(String table, String column, String keyColumn, String key) {
    	String sql = "SELECT "+column+" FROM "+tablePrefix+table+" WHERE "+keyColumn+" = "+key+";";
    	try {
    		if (debug) {
        		SRPG.output("getSingleValue: "+sql);
        	}
    		return query(sql).get(0).get(0);
    	} catch (IndexOutOfBoundsException ex) {
    		return null;
    	}
    }
    
    // value type overloads start
    // key type overloads start
    public ArrayList<Integer> getSingleIntRow(String table, ArrayList<String> columns, String keyColumn, String key) {
    	return getSingleIntRowRaw(table, columns, keyColumn, "'"+key+"'");
    }
    
    public ArrayList<Integer> getSingleIntRow(String table, ArrayList<String> columns, String keyColumn, Integer key) {
    	return getSingleIntRowRaw(table, columns, keyColumn, ""+key);
    }
    // key type overloads end
    public ArrayList<Integer> getSingleIntRowRaw(String table, ArrayList<String> columns, String keyColumn, String key) {
    	ArrayList<Integer> list = new ArrayList<Integer>();
    	try {
	    	for (String entry : getSingleRowRaw(table, columns, keyColumn, key)) {
	    		list.add(Integer.parseInt(entry));
	    	}
    	} catch (NullPointerException ex) {
    		return null;
    	}
    	return list;
    }
    // value type overloads end
    
    // key type overloads start
    public ArrayList<String> getSingleStringRow(String table, ArrayList<String> columns, String keyColumn, String key) {
    	return getSingleRowRaw(table, columns, keyColumn, "'"+key+"'");
    }
    public ArrayList<String> getSingleStringRow(String table, ArrayList<String> columns, String keyColumn, Integer key) {
    	return getSingleRowRaw(table, columns, keyColumn, ""+key);
    }
    // key types overloads end
    public ArrayList<String> getSingleRowRaw(String table, ArrayList<String> columns, String keyColumn, String key) {
    	String sql = "SELECT "+Utility.join(columns, ",")+" FROM "+tablePrefix+table+" WHERE "+keyColumn+" = "+key+";";
    	if (debug) {
    		SRPG.output("getSingleRow: "+sql);
    	}
    	try { 
    		return query(sql).get(0);
    	} catch (IndexOutOfBoundsException ex) {
    		return null;
    	}
    }
    
    // read query
    public ArrayList<ArrayList<String>> query(String sql) {
        // check connection to MySQL
        try {
            if(Settings.mySQLenabled && !connection.isValid(5)) {
            	reconnect();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        PreparedStatement ps = null;
        ResultSet rs = null;
        ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
        
        try {
        	ps = connection.prepareStatement(sql);
            rs = ps.executeQuery();
            Integer cc = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                ArrayList<String> column = new ArrayList<String>();
                for(int i=1;i<=cc;i++) {                        
                	column.add(rs.getString(i));
                }
                rows.add(column);
            }
        } catch (SQLException ex) {
        	SRPG.output("SQLException: " + ex.getMessage());
        	SRPG.output("SQLState: " + ex.getSQLState());
        	SRPG.output("VendorError: " + ex.getErrorCode());
        } finally {
        	closeQuietly(ps);
        	closeQuietly(rs);
        }
        
        return rows;
    }
    
    void closeQuietly(PreparedStatement ps) {
    	if (ps != null) {
    		try {
    			ps.close();
    		} catch (SQLException e) {
    			// ignore
    		}
    	}
    }
    
    void closeQuietly(ResultSet rs) {
    	if (rs != null) {
    		try {
    			rs.close();
    		} catch (SQLException e) {
    			// ignore
    		}
    	}
    }
    
}