package com.canadainc.intelligence.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.canadainc.common.io.DatabaseUtils;
import com.canadainc.intelligence.controller.ReportAnalyzer;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.UserData;
import com.maxmind.geoip.LookupService;

public class DatabaseBoundaryTest
{
	private DatabaseBoundary m_db;

	@Before
	public void setUp() throws Exception
	{
		Class.forName("org.sqlite.JDBC"); // load the sqlite-JDBC driver using the current class loader
		
		String[] tables = {"devices", "operating_systems", "canadainc_apps", "reports", "geo", "locations", "removed_apps", "device_apps", "user_events", "app_user_events", "app_settings", "report_app_settings", "in_app_searches", "invoke_targets", "elements_fetched", "bulk_operations", "users", "user_info", "database_stats"};
		
		m_db = new DatabaseBoundary("res/analytics.db");
		
		DatabaseUtils.reset( m_db.getConnection(), tables );
	}

	
	@After
	public void tearDown() throws Exception
	{
		m_db.close();
	}
	

	@Test
	public void testProcessSunnah10() throws SQLException, IOException
	{
		Map<String,String> consumers = new HashMap<String,String>();
		consumers.put("Sunnah10", "com.canadainc.intelligence.client.SunnahConsumer");
		
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport( ReportCollector.extractReport( new File("res/sunnah10/1408723806275") ) );
		ra.setLookupService( new LookupService("res/GeoLiteCity.dat", LookupService.GEOIP_MEMORY_CACHE) );
		ra.setConsumers(consumers);
		FormattedReport fr = ra.analyze();
		
		Collection<FormattedReport> reports = new ArrayList<FormattedReport>();
		reports.add(fr);
		
		UserCollector instance = new UserCollector();
		Collection<String> folders = new HashSet<String>();
		folders.add("res/canadainc");
		instance.setFolders(folders);
		Collection<UserData> users = instance.run();
		
		m_db.enqueue(reports);
		m_db.enqueueUserData(users);
		m_db.process();
		
		PreparedStatement ps = m_db.getConnection().prepareStatement("SELECT * FROM devices");
		ResultSet rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( "MSM8960AB_CS_PVS2_ARISTO_Rev:11", rs.getString("machine") );
		assertEquals( 2147483648L, rs.getLong("device_memory") );

		ps = m_db.getConnection().prepareStatement("SELECT * FROM operating_systems");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( "10.3.0.1166", rs.getString("version") );
		assertEquals( 1408550408000L, rs.getLong("creation_date") );
		
		ps = m_db.getConnection().prepareStatement("SELECT * FROM canadainc_apps");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( "Sunnah10", rs.getString("name") );
		assertEquals( "2.8.0.0", rs.getString("version") );
		
		// start validate report
		ps = m_db.getConnection().prepareStatement("SELECT * FROM reports");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1408723806275L, rs.getLong("id") );
		assertEquals( 1, rs.getInt("app_id") );
		assertEquals( 1, rs.getInt("device_id") );
		assertEquals( 1, rs.getInt("os_id") );
		assertEquals( "", rs.getString("locale") );
		assertEquals( 242221056L, rs.getLong("memory_usage") );
		assertEquals( 692178944L, rs.getLong("available_memory") );
		assertEquals( 1408652317000L, rs.getLong("boot_time") );
		assertEquals( 31, rs.getInt("battery_temperature") );
		assertEquals( 24, rs.getInt("battery_level") );
		assertEquals( 295, rs.getInt("battery_cycle_count") );
		assertEquals( 3, rs.getInt("battery_charging_state") );
		assertEquals( 0, rs.getInt("total_accounts") );
		assertEquals( "Ragaeeb7D", rs.getString("node_name") );
		assertEquals( 1, rs.getInt("internal") );
		assertEquals( "FE80:3B::96EB:CDFF:FE91:9C2C", rs.getString("bptp0") );
		assertEquals( "10.87.1.15", rs.getString("msm0") );
		assertEquals( "10.231.180.108", rs.getString("bcm0") );
		assertEquals( "208.65.73.39", rs.getString("ip") );
		assertEquals( "c73-039.rim.net", rs.getString("host") );
		assertTrue( !rs.next() );
		
		ps = m_db.getConnection().prepareStatement("SELECT * FROM geo");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1, rs.getLong("id") );
		assertEquals( "Mississauga", rs.getString("city") );
		assertEquals( "ON", rs.getString("region") );
		assertEquals( "Canada", rs.getString("country") );
		
		ps = m_db.getConnection().prepareStatement("SELECT * FROM locations");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1408723806275L, rs.getLong("report_id") );
		assertEquals( 43.6, rs.getDouble("latitude"), 3 );
		assertEquals( -79.65, rs.getDouble("longitude"), 3 );
		assertEquals( 1, rs.getInt("geo_id") );
		assertEquals( "", rs.getString("name") );
		// validate report
		
		// validate removed apps
		ps = m_db.getConnection().prepareStatement("SELECT * FROM device_apps");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( "DialogTest.testRel_DialogTest_2443ae1a", rs.getString("package_name") );
		assertEquals( "10.3.0.17", rs.getString("version") );
		assertEquals( 0, rs.getInt("bbw_id") );
		assertTrue( rs.next() );
		assertTrue( rs.next() );
		assertEquals( "Stocks.BB10.gYABgMQXznH4wRvvvN74EmaRiM0", rs.getString("package_name") );
		assertEquals( "1.4.1.1", rs.getString("version") );
		assertEquals( 25656264, rs.getInt("bbw_id") );
		assertEquals( "Stocks for BlackBerry 10", rs.getString("bbw_name") );

		advance(rs, 92);
		
		assertEquals( "test.gftsuite.debug.testRel_suite_debug58f24224", rs.getString("package_name") );
		assertEquals( "19.102.1.7", rs.getString("version") );
		assertEquals( 0, rs.getInt("bbw_id") );
		
		ps = m_db.getConnection().prepareStatement("SELECT * FROM removed_apps");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1, rs.getInt("device_app_id") );
		assertEquals( 1408723806275L, rs.getLong("report_id") );
		
		advance(rs, 94);
		
		assertEquals( 95, rs.getInt("device_app_id") );
		assertEquals( 1408723806275L, rs.getLong("report_id") );
		assertTrue( !rs.next() );
		// end validate removed apps
		
		// validate user events
		ps = m_db.getConnection().prepareStatement("SELECT * FROM user_events");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( "HadithOptionSelected", rs.getString("event") );
		advance(rs, 20);
		assertEquals( "UnlinkNarrationsFromTafsirTriggered", rs.getString("event") );
		
		ps = m_db.getConnection().prepareStatement("SELECT * FROM app_user_events");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1408723806275L, rs.getLong("report_id") );
		assertEquals( 1, rs.getInt("user_event_id") );
		advance(rs, 145);
		assertEquals( 1, rs.getInt("user_event_id") );
		assertEquals( 1408723806275L, rs.getLong("report_id") );
		assertTrue( !rs.next() );
		// end validate user events

		// validate app settings
		ps = m_db.getConnection().prepareStatement("SELECT * FROM app_settings");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( "tafsirSize", rs.getString("setting_key") );
		advance(rs, 3);
		assertEquals( "shortThreshold", rs.getString("setting_key") );
		
		ps = m_db.getConnection().prepareStatement("SELECT * FROM report_app_settings");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1408723806275L, rs.getLong("report_id") );
		assertEquals( 1, rs.getInt("app_setting_id") );
		advance(rs, 3);
		assertEquals( 4, rs.getInt("app_setting_id") );
		assertEquals( 1408723806275L, rs.getLong("report_id") );
		assertEquals( "200", rs.getString("setting_value") );
		assertTrue( !rs.next() );
		// end validate app settings

		// validate in app searches
		ps = m_db.getConnection().prepareStatement("SELECT * FROM in_app_searches");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1408723806275L, rs.getLong("report_id") );
		assertEquals( "turbo_and", rs.getString("name") );
		assertEquals( "[m1856]", rs.getString("query_value") );
		advance(rs, 28);
		
		assertTrue( rs.next() );
		assertEquals( 1408723806275L, rs.getLong("report_id") );
		assertEquals( "standard_and", rs.getString("name") );
		assertEquals( "[seventy, sects]", rs.getString("query_value") );
		
		assertTrue( !rs.next() );
		// end validate in app seaches

		ps = m_db.getConnection().prepareStatement("SELECT * FROM invoke_targets");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1408723806275L, rs.getLong("report_id") );
		assertEquals( "com.canadainc.Sunnah10.previewer", rs.getString("target_id") );
		assertEquals( "", rs.getString("uri") );
		assertEquals( "Bukhari #1)", rs.getString("data") );
		assertTrue( !rs.next() );
		
		ps = m_db.getConnection().prepareStatement("SELECT * FROM users");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( "24f444c2", rs.getString("pin") );
		assertEquals( "Ndaman", rs.getString("name") );
		assertTrue( !rs.getString("data").isEmpty() );
		assertTrue( rs.next() );
		assertEquals( "2b39693f", rs.getString("pin") );
		assertEquals( "Rial Arizal", rs.getString("name") );
		assertTrue( !rs.getString("data").isEmpty() );
		assertTrue( !rs.next() );
	}
	
	
	@Test
	public void testProcessSalat10() throws SQLException, IOException
	{
		Map<String,String> consumers = new HashMap<String,String>();
		consumers.put("Salat10", "com.canadainc.intelligence.client.SalatConsumer");
		
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport( ReportCollector.extractReport( new File("res/salat10/1401895045955") ) );
		ra.setConsumers(consumers);
		FormattedReport fr = ra.analyze();
		
		Collection<FormattedReport> reports = new ArrayList<FormattedReport>();
		reports.add(fr);
		
		m_db.enqueue(reports);
		m_db.process();
		
		PreparedStatement ps = m_db.getConnection().prepareStatement("SELECT * FROM locations");
		ResultSet rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1401895045955L, rs.getLong("report_id") );
		assertEquals( 3.1544292, rs.getDouble("latitude"), 3 );
		assertEquals( 101.7151017, rs.getDouble("longitude"), 3 );
		assertEquals( 1, rs.getInt("geo_id") );
		assertEquals( "Kuala Lumpur, Kuala Lumpur Federal Territory, MYS", rs.getString("name") );
		
		ps = m_db.getConnection().prepareStatement("SELECT * FROM geo");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( "Kuala Lumpur", rs.getString("city") );
		assertEquals( "", rs.getString("region") );
		assertEquals( "MYS", rs.getString("country") );
	}
	
	
	@Test
	public void testProcessAutoBlock() throws SQLException, IOException
	{
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport( ReportCollector.extractReport( new File("res/auto_block/1403455028364") ) );
		FormattedReport fr = ra.analyze();
		
		Collection<FormattedReport> reports = new ArrayList<FormattedReport>();
		reports.add(fr);
		
		m_db.enqueue(reports);
		m_db.process();
		
		PreparedStatement ps = m_db.getConnection().prepareStatement("SELECT * FROM elements_fetched");
		ResultSet rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1403455028364L, rs.getLong("report_id") );
		assertEquals( "conversations", rs.getString("type") );
		assertEquals( 300, rs.getInt("count") );
		
		advance(rs, 13);
		assertEquals( 1403455028364L, rs.getLong("report_id") );
		assertEquals( "pim_elements", rs.getString("type") );
		assertEquals( 27, rs.getInt("count") );
		
		assertTrue( !rs.next() );
	}
	
	
	
	@Test
	public void testProcessGolden() throws SQLException, IOException
	{
		Map<String,String> consumers = new HashMap<String,String>();
		consumers.put("Golden Retriever", "com.canadainc.intelligence.client.GoldenRetrieverConsumer");
		
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setConsumers(consumers);
		ra.setReport( ReportCollector.extractReport( new File("res/golden/1400700013412") ) );
		
		Collection<FormattedReport> reports = new ArrayList<FormattedReport>();
		reports.add( ra.analyze() );
		
		m_db.enqueue(reports);
		m_db.process();
		
		PreparedStatement ps = m_db.getConnection().prepareStatement("SELECT * FROM users");
		ResultSet rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1400700013412L, rs.getLong("id") );
		assertTrue( !rs.next() );
		
		ps = m_db.getConnection().prepareStatement("SELECT * FROM user_info");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1400700013412L, rs.getLong("user_id") );
		assertEquals( "kitc8@ukr.net", rs.getString("address") );
		advance(rs, 3);
		assertEquals( 1400700013412L, rs.getLong("user_id") );
		assertEquals( "kytsyuk@ukr.net", rs.getString("address") );
		assertTrue( !rs.next() );
		
		ps = m_db.getConnection().prepareStatement("SELECT * FROM database_stats");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1400700013412L, rs.getLong("report_id") );
		assertEquals( 3, rs.getInt("query_id") );
		assertEquals( 21, rs.getInt("duration") );
		assertEquals( 0, rs.getInt("num_elements") );
		advance(rs,4);
		assertEquals( 1400700013412L, rs.getLong("report_id") );
		assertEquals( 1, rs.getInt("query_id") );
		assertEquals( 41, rs.getInt("duration") );
		assertEquals( 23, rs.getInt("num_elements") );
	}
	
	
	private void advance(ResultSet rs, int n) throws SQLException
	{
		for (int i = 0; i < n; i++) {
			assertTrue( rs.next() );
		}
	}
}