package com.behindthemirrors.minecraft.sRPG;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
//import java.sql.Statement;
import java.util.HashMap;
import java.util.ArrayList;
import java.sql.PreparedStatement;

import org.bukkit.entity.Player;
import com.avaje.ebeaninternal.server.lib.sql.DataSourceException;

public class Database {
    private Connection conn;
	String dbTablePrefix;
	String dbPass;
	String dbUser;
	String dbName;
	String dbPort;
	String dbServer;
	Boolean mySQLenabled;
    
    public Database() {
        // Load the driver instance
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
            throw new DataSourceException("Failed to initialize JDBC driver");
        }
        
        // make the connection
    }
    
    public boolean connect() {
    	getConnection();
    	return (conn != null);
    }
    
	//Create the DB structure
    public void createStructure(){
    	String prefix = "CREATE TABLE IF NOT EXISTS `" + dbTablePrefix;
    	Write(prefix + "global` (`pk` int(10) UNSIGNED NOT NULL PRIMARY KEY, `version` varchar(40) NOT NULL) ENGINE=MyISAM DEFAULT CHARSET=latin1;");
        Write(prefix +  "users` (`user_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "`user` varchar(40) NOT NULL UNIQUE KEY," +
                "`xp` int(32) NOT NULL DEFAULT 0," +
                "`hp` int(32) NOT NULL DEFAULT 0," +
                "`class` varchar(40) NOT NULL," +
                "`locale` varchar(40) NOT NULL" +
                ") ENGINE=MyISAM DEFAULT CHARSET=latin1;");
        String sql = prefix + "skillpoints` (`user_id` int(10) UNSIGNED NOT NULL PRIMARY KEY";
        for (String name : Settings.SKILLS) {
        	sql += ",`" + name + "` int(10) unsigned NOT NULL DEFAULT 0";
        }
        sql += ") ENGINE=MyISAM DEFAULT CHARSET=latin1;";
        Write(sql);
        sql = prefix + "chargedata` (`user_id` int(10) UNSIGNED NOT NULL PRIMARY KEY";
        for (String type : Settings.TOOLS) {
        	sql += ",`" + type + "_charges` int(10) unsigned NOT NULL DEFAULT 0";
        	sql += ",`" + type + "_chargeprogress` int(10) unsigned NOT NULL DEFAULT 0";
        }
        sql += ") ENGINE=MyISAM DEFAULT CHARSET=latin1;";
        Write(sql);
        
        
        
    }
    
    public void updateDatabase(String version) {
    	// here be version-dependent database update routines
    	Integer id = SRPG.database.GetInt("SELECT pk FROM " + dbTablePrefix + "global WHERE pk = '1';");
		if (id == 0) {
			Write("INSERT INTO " + dbTablePrefix + "global (pk,version) VALUES ('1',\"" + version + "\");");
		}
        String db_version = new String(Read("SELECT version FROM " + dbTablePrefix + "global WHERE pk = '1';").get(1).get(0));
        if (!version.equalsIgnoreCase(db_version)) {
        	SRPG.output("Version changed from "+db_version+" to "+version+", updating database structure if necessary");
        }
        // change database according to version differences
        if (db_version.equalsIgnoreCase("0.5alpha1") || db_version.equalsIgnoreCase("0.5alpha2")) {
        	SRPG.output("updating user table");
        	Write("ALTER TABLE " + dbTablePrefix + "users ADD COLUMN class VARCHAR(40) NOT NULL DEFAULT \"adventurer\" AFTER user , ADD COLUMN hp INT(10) NOT NULL DEFAULT '0'  AFTER locale ;");
        }
        // recreate structure in case any tables were deleted during update process
        if (!version.equalsIgnoreCase(db_version)) {
        	createStructure();
        }
        // update version in database to current version
        Write("UPDATE " + dbTablePrefix + "global SET version = \"" + version + "\" WHERE pk = '1'");
    }
    
    // check if its closed
    private void getConnection() {
    	try {
            conn = DriverManager.getConnection("jdbc:mysql://" + dbServer + ":" + dbPort + "/" + dbName + "?user=" + dbUser + "&password=" + dbPass);            
            
            SRPG.output("Connection success");
        } catch (SQLException ex) {
        	SRPG.output("Connection to MySQL failed. Check status of MySQL server");
        	SRPG.output("SQLException: " + ex.getMessage());
        	SRPG.output("SQLState: " + ex.getSQLState());
        	SRPG.output("VendorError: " + ex.getErrorCode());
        }
    }
    
    private void reconnect()
    {
    	SRPG.output("Reconnecting to MySQL...");
    	getConnection();
        
        try {
            if(conn.isValid(5)){
                SRPG.profileManager.clear();
                
                for(Player player : SRPG.plugin.getServer().getOnlinePlayers())
                {
                    SRPG.profileManager.add(player);
                }
            }
        } catch (SQLException e) {
            // ignore exception
        }
    }
    // write query
    public boolean Write(String sql) 
    {
        /*
         * Double check connection to MySQL
         */
        try 
        {
            if(!conn.isValid(5))
            {
            reconnect();
            }
        } catch (SQLException e) 
        {
            e.printStackTrace();
        }
            
        try 
            {
                  PreparedStatement stmt = null;
                  stmt = this.conn.prepareStatement(sql);
                  stmt.executeUpdate();
                  return true;
            } catch(SQLException ex) {
            	SRPG.output("SQLException: " + ex.getMessage());
            	SRPG.output("SQLState: " + ex.getSQLState());
            	SRPG.output("VendorError: " + ex.getErrorCode());
                return false;
            }
     }
    
    // Get Int
    // only return first row / first field
    public Integer GetInt(String sql) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Integer result = 0;
        
        /*
         * Double check connection to MySQL
         */
        try 
        {
            if(!conn.isValid(5))
            {
            reconnect();
            }
        } catch (SQLException e) 
        {
            e.printStackTrace();
        }
        try {
            stmt = this.conn.prepareStatement(sql);
            if (stmt.executeQuery() != null) {
                stmt.executeQuery();
                rs = stmt.getResultSet();
                if(rs.next()){
                    result = rs.getInt(1);
                }
                else { result = 0; }
            }
        } 
        catch (SQLException ex) {
        	SRPG.output("SQLException: " + ex.getMessage());
        	SRPG.output("SQLState: " + ex.getSQLState());
        	SRPG.output("VendorError: " + ex.getErrorCode());
        }        
        return result;
    }
    
    // read query
    public HashMap<Integer, ArrayList<String>> Read(String sql) {
        /*
         * Double check connection to MySQL
         */
        try 
        {
            if(!conn.isValid(5))
            {
            reconnect();
            }
        } catch (SQLException e) 
        {
            e.printStackTrace();
        }
        
          PreparedStatement stmt = null;
        ResultSet rs = null;
        HashMap<Integer, ArrayList<String>> Rows = new HashMap<Integer, ArrayList<String>>();
        
        try {
            stmt = this.conn.prepareStatement(sql);
            if (stmt.executeQuery() != null) {
                stmt.executeQuery();
                rs = stmt.getResultSet();
                while (rs.next()) {
                    ArrayList<String> Col = new ArrayList<String>();
                    for(int i=1;i<=rs.getMetaData().getColumnCount();i++) {                        
                        Col.add(rs.getString(i));
                    }
                    Rows.put(rs.getRow(),Col);
                }
            }        
        }
        catch (SQLException ex) {
        	SRPG.output("SQLException: " + ex.getMessage());
        	SRPG.output("SQLState: " + ex.getSQLState());
        	SRPG.output("VendorError: " + ex.getErrorCode());
        }
        
        // release dataset
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException sqlEx) { } // ignore
            rs = null;
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException sqlEx) { } // ignore
            stmt = null;
        }

        return Rows;
    }
    
}