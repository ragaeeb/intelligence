package com.canadainc.intelligence.io;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.helper.StringUtil;

import com.canadainc.intelligence.client.InvokeTarget;
import com.canadainc.intelligence.model.BulkOperation;
import com.canadainc.intelligence.model.DatabaseStat;
import com.canadainc.intelligence.model.DeviceAppInfo;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.FormattedReport.AppInfo;
import com.canadainc.intelligence.model.FormattedReport.OperatingSystem;
import com.canadainc.intelligence.model.InAppSearch;
import com.canadainc.intelligence.model.Location;
import com.canadainc.intelligence.model.UserData;

public class DatabaseBoundary implements PersistentBoundary
{
	private Connection m_connection;
	private Collection<FormattedReport> m_reports = new ArrayList<FormattedReport>();
	private Collection<UserData> m_userReports = new ArrayList<UserData>();
	private Map<String, Integer> m_devices = new HashMap<String, Integer>();
	private Map<OperatingSystem, Integer> m_os = new HashMap<OperatingSystem, Integer>();
	private Map<AppInfo, Integer> m_apps = new HashMap<AppInfo, Integer>();
	private Map<Location, Integer> m_geo = new HashMap<Location, Integer>();
	private Map<String, Integer> m_deviceApps = new HashMap<String, Integer>();
	private Map<String, Integer> m_userEvents = new HashMap<String, Integer>();
	private Map<String, Integer> m_appSettings = new HashMap<String, Integer>();
	private Map<Long, Long> m_userIds = new HashMap<Long, Long>();
	
	public DatabaseBoundary(String path) throws Exception
	{
		setPath(path);
	}
	
	
	public void close() throws Exception
	{
		m_connection.close();
	}
	
	
	public void enqueue(Collection<FormattedReport> reports)
	{
		m_reports = reports;
	}
	
	public void enqueueUserData(Collection<UserData> reports)
	{
		m_userReports = reports;
	}
	
	public void process() throws SQLException
	{
		try {
			for (FormattedReport fr: m_reports)
			{
				populateDeviceInfo(fr);
				populateOperatingSystemInfo(fr);
				populateCanadaIncApp(fr);
				populateUserData(fr);
				populateReport(fr);
				populateLocations(fr);
				populateRemovedApps(fr);
				populateUserEvents(fr);
				populateAppSettings(fr);
				populateSearches(fr);
				populateInvokes(fr);
				populatePimData(fr);
				populateStats(fr);
			}
			
			for (UserData fr: m_userReports)
			{
				populateUserInfo(fr);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			m_connection.rollback();
		} finally {
			m_connection.commit();
		}
	}
	
	
	private void populateUserInfo(UserData fr) throws SQLException
	{
		if ( !fr.pin.isEmpty() )
		{
			System.out.println("*** INSERTING "+fr.name+","+fr.pin);
			PreparedStatement ps = m_connection.prepareStatement("SELECT id,pin,name,data FROM users WHERE pin LIKE '%"+fr.pin+"%'");
			ResultSet rs = ps.executeQuery();
			
			if ( rs.next() )
			{
				if ( fr.name.isEmpty() ) {
					fr.name = rs.getString("name");
				}
				
				fr.data = rs.getString("data") + fr.data;
			}
			
			ps = m_connection.prepareStatement( "INSERT OR REPLACE INTO users (pin,name,data) VALUES (?,?,?)");
			int i = 0;
			ps.setString(++i, fr.pin);
			ps.setString(++i, fr.name);
			ps.setString(++i, fr.data);
			
			ps.executeUpdate();
		}
	}
	
	
	private void populateUserData(FormattedReport fr) throws SQLException
	{
		if ( !fr.userInfo.emails.isEmpty() )
		{
			String emails = StringUtil.join(fr.userInfo.emails, "','");
			
			PreparedStatement ps = m_connection.prepareStatement( "SELECT user_id FROM user_info WHERE address IN ('"+emails+"')" );
			ResultSet rs = ps.executeQuery();
			long userId = 0;
			
			if ( rs.next() ) {
				userId = rs.getLong("user_id");
			} else { // new entry
				userId = fr.id;
				
				int i = 0;
				ps = m_connection.prepareStatement( "INSERT OR IGNORE INTO users (id) VALUES (?)");
				ps.setLong(++i, userId);
				
				ps.executeUpdate();
			}
			
			ps = m_connection.prepareStatement( "INSERT OR IGNORE INTO user_info (user_id,address) VALUES (?,?)");
			for (String email: fr.userInfo.emails)
			{
				int i = 0;
				ps.setLong(++i, userId);
				ps.setString(++i, email);
				ps.addBatch();
			}
			
			ps.executeBatch();
			
			m_userIds.put(fr.id, userId);
		}
	}
	
	
	private void populatePimData(FormattedReport fr) throws SQLException
	{
		PreparedStatement ps = m_connection.prepareStatement( "INSERT OR IGNORE INTO bulk_operations (report_id,type,count) VALUES (?,?,?)");
		
		for (BulkOperation search: fr.bulkOperations)
		{
			int i = 0;
			ps.setLong(++i, fr.id);
			ps.setString(++i, search.type);
			ps.setInt(++i, search.count);
			ps.addBatch();
		}
		
		ps.executeBatch();
		
		ps = m_connection.prepareStatement( "INSERT OR IGNORE INTO elements_fetched (report_id,type,count) VALUES (?,?,?)");

		for (int count: fr.conversationsFetched)
		{
			int i = 0;
			ps.setLong(++i, fr.id);
			ps.setString(++i, "conversations");
			ps.setInt(++i, count);
			ps.addBatch();
		}
		
		for (int count: fr.pimElementsFetched)
		{
			int i = 0;
			ps.setLong(++i, fr.id);
			ps.setString(++i, "pim_elements");
			ps.setInt(++i, count);
			ps.addBatch();
		}

		ps.executeBatch();
	}
	
	
	private void populateAppSettings(FormattedReport fr) throws SQLException
	{
		PreparedStatement ps = m_connection.prepareStatement( "INSERT OR IGNORE INTO app_settings (setting_key) VALUES (?)");
		Collection<String> unknown = new ArrayList<String>();
		
		for ( String setting: fr.appSettings.keySet() )
		{
			int i = 0;
			ps.setString(++i, setting);
			ps.addBatch();
			
			if ( !m_appSettings.containsKey(setting) ) {
				unknown.add(setting);
			}
		}
		
		ps.executeBatch();
		
		if ( !unknown.isEmpty() )
		{
			ps = m_connection.prepareStatement( "SELECT id,setting_key FROM app_settings WHERE setting_key IN ('"+StringUtil.join(unknown, "','")+"')" );
			ResultSet rs = ps.executeQuery();
			
			while ( rs.next() ) {
				m_appSettings.put( rs.getString("setting_key"), rs.getInt("id") );
			}
		}
		
		ps = m_connection.prepareStatement( "INSERT INTO report_app_settings (report_id,app_setting_id,setting_value) VALUES (?,?,?)");
		
		Map<String, String> appSettings = fr.appSettings;
		
		for ( String setting: appSettings.keySet() )
		{
			int i = 0;
			ps.setLong(++i, fr.id);
			ps.setInt( ++i, m_appSettings.get(setting) );
			ps.setString( ++i, appSettings.get(setting) );
			ps.addBatch();
		}
		
		ps.executeBatch();
	}
	
	
	private void populateStats(FormattedReport fr) throws SQLException
	{
		PreparedStatement ps = m_connection.prepareStatement( "INSERT OR IGNORE INTO database_stats (report_id,query_id,duration,num_elements) VALUES (?,?,?,?)");
		
		for (DatabaseStat search: fr.databaseStats)
		{
			int i = 0;
			ps.setLong(++i, fr.id);
			ps.setInt(++i, search.queryId);
			ps.setInt(++i, search.duration);
			ps.setInt(++i, search.elements);
			ps.addBatch();
		}
		
		ps.executeBatch();
	}
	
	
	private void populateSearches(FormattedReport fr) throws SQLException
	{
		PreparedStatement ps = m_connection.prepareStatement( "INSERT OR IGNORE INTO in_app_searches (report_id,name,query_value) VALUES (?,?,?)");
		
		for (InAppSearch search: fr.inAppSearches)
		{
			int i = 0;
			ps.setLong(++i, fr.id);
			ps.setString(++i, search.name);
			ps.setString(++i, search.query);
			ps.addBatch();
		}
		
		ps.executeBatch();
	}
	
	
	private void populateInvokes(FormattedReport fr) throws SQLException
	{
		PreparedStatement ps = m_connection.prepareStatement( "INSERT OR IGNORE INTO invoke_targets (report_id,target_id,uri,data) VALUES (?,?,?,?)");
		
		for (InvokeTarget search: fr.invokeTargets)
		{
			int i = 0;
			ps.setLong(++i, fr.id);
			ps.setString(++i, search.target);
			ps.setString(++i, search.uri);
			ps.setString(++i, search.data);
			ps.addBatch();
		}
		
		ps.executeBatch();
	}
	
	
	private void populateUserEvents(FormattedReport fr) throws SQLException
	{
		PreparedStatement ps = m_connection.prepareStatement( "INSERT OR IGNORE INTO user_events (event) VALUES (?)");
		Collection<String> unknown = new ArrayList<String>();
		
		for (String userEvent: fr.userEvents)
		{
			int i = 0;
			ps.setString(++i, userEvent);
			ps.addBatch();
			
			if ( !m_userEvents.containsKey(userEvent) ) {
				unknown.add(userEvent);
			}
		}
		
		ps.executeBatch();
		
		if ( !unknown.isEmpty() )
		{
			ps = m_connection.prepareStatement( "SELECT id,event FROM user_events WHERE event IN ('"+StringUtil.join(unknown, "','")+"')" );
			ResultSet rs = ps.executeQuery();
			
			while ( rs.next() ) {
				m_userEvents.put( rs.getString("event"), rs.getInt("id") );
			}
		}
		
		ps = m_connection.prepareStatement( "INSERT INTO app_user_events (report_id,user_event_id) VALUES (?,?)");
		
		for (String userEvent: fr.userEvents)
		{
			int i = 0;
			ps.setLong(++i, fr.id);
			ps.setInt( ++i, m_userEvents.get(userEvent) );
			ps.addBatch();
		}
		
		ps.executeBatch();
	}

	
	private void populateRemovedApps(FormattedReport fr) throws SQLException
	{
		PreparedStatement ps = m_connection.prepareStatement( "INSERT OR IGNORE INTO device_apps (package_name,version,bbw_id,bbw_content_id,bbw_name,bbw_sku,bbw_vendor,bbw_icon_uri) VALUES (?,?,?,?,?,?,?,?)");
		Collection<String> unknown = new ArrayList<String>();
		
		for (DeviceAppInfo dai: fr.removedApps)
		{
			int i = 0;
			ps.setString(++i, dai.packageName);
			ps.setString(++i, dai.packageVersion);
			ps.setInt(++i, dai.appWorldInfo.id);
			ps.setInt(++i, dai.appWorldInfo.contentId);
			ps.setString(++i, dai.appWorldInfo.name);
			ps.setString(++i, dai.appWorldInfo.sku);
			ps.setString(++i, dai.appWorldInfo.vendor);
			ps.setString(++i, dai.appWorldInfo.iconUri);
			ps.addBatch();
			
			if ( !m_deviceApps.containsKey(dai.packageName) ) {
				unknown.add(dai.packageName);
			}
		}
		
		ps.executeBatch();
		
		if ( !unknown.isEmpty() )
		{
			ps = m_connection.prepareStatement( "SELECT id,package_name FROM device_apps WHERE package_name IN ('"+StringUtil.join(unknown, "','")+"')" );
			ResultSet rs = ps.executeQuery();
			
			while ( rs.next() ) {
				m_deviceApps.put( rs.getString("package_name"), rs.getInt("id") );
			}
		}
		
		ps = m_connection.prepareStatement( "INSERT INTO removed_apps (report_id,device_app_id) VALUES (?,?)");
		
		for (DeviceAppInfo dai: fr.removedApps)
		{
			int i = 0;
			ps.setLong(++i, fr.id);
			ps.setInt( ++i, m_deviceApps.get(dai.packageName) );
			ps.addBatch();
		}
		
		ps.executeBatch();
	}

	private void populateLocations(FormattedReport fr) throws SQLException
	{
		for (Location l: fr.locations)
		{
			if ( !m_geo.containsKey(l) && ( !l.city.isEmpty() || !l.country.isEmpty() || !l.region.isEmpty() ) )
			{
				int i = 0;
				PreparedStatement ps = m_connection.prepareStatement("INSERT OR IGNORE INTO geo (city,region,country) VALUES(?,?,?)");
				ps.setString(++i, l.city);
				ps.setString(++i, l.region);
				ps.setString(++i, l.country);
				ps.executeUpdate();
				
				i = 0;
				ps = m_connection.prepareStatement("SELECT id FROM geo WHERE city=? AND region=? AND country=?");
				ps.setString(++i, l.city);
				ps.setString(++i, l.region);
				ps.setString(++i, l.country);
				ResultSet rs = ps.executeQuery();
				
				rs.next();
				i = rs.getInt(1);
				m_geo.put(l,i);
			}
		}
		
		for (Location l: fr.locations)
		{
			int i = 0;
			PreparedStatement ps = m_connection.prepareStatement("INSERT INTO locations (report_id,latitude,longitude,geo_id,name) VALUES(?,?,?,?,?)");
			ps.setLong(++i, fr.id);
			ps.setDouble(++i, l.latitude);
			ps.setDouble(++i, l.longitude);
			ps.setInt( ++i, m_geo.containsKey(l) ? m_geo.get(l) : 0 );
			ps.setString(++i, l.name);
			ps.executeUpdate();
		}
	}

	
	private void populateReport(FormattedReport fr) throws SQLException
	{
		int appID = m_apps.get(fr.appInfo);
		int deviceID = m_devices.get(fr.hardwareInfo.machine);
		int osID = m_os.get(fr.os);
		
		int i = 0;
		PreparedStatement ps = m_connection.prepareStatement("INSERT OR IGNORE INTO reports ("
				+ "id,"
				+ "app_id,"
				+ "device_id,"
				+ "os_id,"
				+ "locale,"
				+ "memory_usage,"
				+ "available_memory,"
				+ "boot_time,"
				+ "battery_temperature,"
				+ "battery_level,"
				+ "battery_cycle_count,"
				+ "battery_charging_state,"
				+ "total_accounts,"
				+ "user_id,"
				+ "node_name,"
				+ "internal,"
				+ "bcm0,"
				+ "bptp0,"
				+ "msm0,"
				+ "ip,"
				+ "host) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		ps.setLong(++i, fr.id);
		ps.setInt(++i, appID);
		ps.setInt(++i, deviceID);
		ps.setInt(++i, osID);
		ps.setString(++i, fr.locale);
		ps.setLong(++i, fr.memoryUsage);
		ps.setLong(++i, fr.availableMemory);
		ps.setLong(++i, fr.bootTime);
		ps.setInt(++i, fr.batteryInfo.temperature);
		ps.setInt(++i, fr.batteryInfo.level);
		ps.setInt(++i, fr.batteryInfo.cycleCount);
		ps.setInt(++i, fr.batteryInfo.chargingState);
		ps.setInt(++i, fr.totalAccounts);
		ps.setLong(++i, m_userIds.containsKey(fr.id) ? m_userIds.get(fr.id) : 0);
		ps.setString(++i, fr.userInfo.nodeName);
		ps.setInt(++i, fr.userInfo.internal ? 1 : 0);
		ps.setString(++i, fr.network.bcm0);
		ps.setString(++i, fr.network.bptp0);
		ps.setString(++i, fr.network.msm0);
		ps.setString(++i, fr.network.ip);
		ps.setString(++i, fr.network.host);
		ps.executeUpdate();
	}

	private void populateCanadaIncApp(FormattedReport fr) throws SQLException
	{
		AppInfo ai = fr.appInfo;
		
		if ( !m_apps.containsKey(ai) )
		{
			int i = 0;
			PreparedStatement ps = m_connection.prepareStatement("INSERT INTO canadainc_apps (name,version) VALUES(?,?)");
			ps.setString(++i, ai.name);
			ps.setString(++i, ai.version);
			ps.executeUpdate();
			
			i = 0;
			ps = m_connection.prepareStatement("SELECT id FROM canadainc_apps WHERE name=? AND version=?");
			ps.setString(++i, ai.name);
			ps.setString(++i, ai.version);
			ResultSet rs = ps.executeQuery();
			
			rs.next();
			i = rs.getInt(1);
			m_apps.put(ai, i);
		}
	}

	private void populateOperatingSystemInfo(FormattedReport fr) throws SQLException
	{
		OperatingSystem os = fr.os;
		
		if ( !m_os.containsKey(os) )
		{
			int i = 0;
			PreparedStatement ps = m_connection.prepareStatement("INSERT INTO operating_systems (version,creation_date) VALUES(?,?)");
			ps.setString(++i, os.version);
			ps.setLong(++i, os.creationDate);
			ps.executeUpdate();
			
			i = 0;
			ps = m_connection.prepareStatement("SELECT id FROM operating_systems WHERE version=? AND creation_date=?");
			ps.setString(++i, os.version);
			ps.setLong(++i, os.creationDate);
			ResultSet rs = ps.executeQuery();
			
			rs.next();
			i = rs.getInt(1);
			m_os.put(os, i);
		}
	}
	

	private void populateDeviceInfo(FormattedReport fr) throws SQLException
	{
		String machine = fr.hardwareInfo.machine;

		if ( !m_devices.containsKey(machine) )
		{
			int i = 0;
			PreparedStatement ps = m_connection.prepareStatement("INSERT OR IGNORE INTO devices (machine,hardware_id,model_name,physical_keyboard,model_number,device_memory) VALUES(?,?,?,?,?,?)");
			ps.setString(++i, machine);
			ps.setString(++i, fr.hardwareInfo.hardwareID);
			ps.setString(++i, fr.hardwareInfo.modelName);
			ps.setInt(++i, fr.hardwareInfo.physicalKeyboard ? 1 : 0);
			ps.setString(++i, fr.hardwareInfo.modelNumber);
			ps.setLong(++i, fr.hardwareInfo.deviceMemory);
			ps.executeUpdate();
			
			i = 0;
			ps = m_connection.prepareStatement("SELECT id FROM devices WHERE machine=?");
			ps.setString(++i, machine);
			ResultSet rs = ps.executeQuery();
			
			rs.next();
			i = rs.getInt(1);
			m_devices.put(machine, i);
		}
	}

	public Connection getConnection() {
		return m_connection;
	}

	Collection<FormattedReport> getReports() {
		return m_reports;
	}

	Map<String, Integer> getDevices() {
		return m_devices;
	}

	Map<OperatingSystem, Integer> getOs() {
		return m_os;
	}

	Map<AppInfo, Integer> getApps() {
		return m_apps;
	}


	@Override
	public void setPath(String path) throws Exception
	{
		if (m_connection != null) {
			m_connection.close();
		}
		
		m_connection = DriverManager.getConnection("jdbc:sqlite:"+path);
		m_connection.setAutoCommit(false);
	}
}