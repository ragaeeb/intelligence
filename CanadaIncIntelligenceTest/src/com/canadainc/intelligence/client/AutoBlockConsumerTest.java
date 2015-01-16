package com.canadainc.intelligence.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.canadainc.common.io.DatabaseUtils;
import com.canadainc.intelligence.controller.ReportAnalyzer;
import com.canadainc.intelligence.io.ReportCollector;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.Location;
import com.canadainc.intelligence.model.Report;
import com.maxmind.geoip.LookupService;

public class AutoBlockConsumerTest
{
	private void reset(Consumer c) throws Exception
	{
		String[] tables = {"inbound_blacklist", "inbound_keywords", "logs", "outbound_blacklist"};

		Class.forName("org.sqlite.JDBC"); // load the sqlite-JDBC driver using the current class loader
		c.setPath("res/autoblock.db");

		DatabaseUtils.reset( c.getConnection(), tables );
	}
	
	@Test
	public void testConsume() throws IOException
	{
		Map<String,String> consumers = new HashMap<String,String>();
		consumers.put("AutoBlock", "com.canadainc.intelligence.client.AutoBlockConsumer");

		Report r = ReportCollector.extractReport( new File("res/auto_block/1403455028364") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		FormattedReport fr = ra.analyze();
		
		assertEquals( 1, fr.invokeTargets.size() );
		assertEquals( "com.canadainc.AutoBlock.reply", fr.invokeTargets.get(0).target );
		
		assertEquals( 3, fr.bulkOperations.size() );
		assertEquals( "insert_inbound_blacklist", fr.bulkOperations.get(0).type );
		assertEquals( 2, fr.bulkOperations.get(0).count );
		assertEquals( "insert_inbound_blacklist", fr.bulkOperations.get(1).type );
		assertEquals( 2, fr.bulkOperations.get(1).count );
		assertEquals( "insert_inbound_blacklist", fr.bulkOperations.get(2).type );
		assertEquals( 2, fr.bulkOperations.get(2).count );
	}

	@Test
	public void testConsumeSetting() throws IOException
	{
		Map<String,String> consumers = new HashMap<String,String>();
		consumers.put("AutoBlock", "com.canadainc.intelligence.client.AutoBlockConsumer");

		Report r = ReportCollector.extractReport( new File("res/auto_block/1403455028364") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		ra.analyze();
		
		Consumer instance = ra.getConsumer();
		assertNull( instance.consumeSetting("accountId", "1345666", null) );
		assertEquals( "1403361639000", instance.consumeSetting("autoblock_junk", "QVariant(QDateTime, QDateTime(\"Sat Jun 21 10:40:39 2014\") )", null) );
	}
	
	
	@Test
	public void testConsumeBulkKeywords() throws Exception
	{
		Map<String,String> consumers = new HashMap<String,String>();
		consumers.put("AutoBlock", "com.canadainc.intelligence.client.AutoBlockConsumer");

		Report r = ReportCollector.extractReport( new File("res/auto_block/1406619880273") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setLookupService( new LookupService("res/GeoLiteCity.dat", LookupService.GEOIP_MEMORY_CACHE) );
		ra.setReport(r);
		ra.setConsumers(consumers);
		FormattedReport fr = ra.analyze();
		
		assertEquals( 2, fr.bulkOperations.size() );
		assertEquals( 1294, fr.bulkOperations.get(1).count );
		assertEquals( 3, fr.inAppSearches.size() );
		
		assertEquals( "search_logs", fr.inAppSearches.get(0).name );
		assertEquals( "gold", fr.inAppSearches.get(0).query );
		assertEquals( "search_inbound_blacklist", fr.inAppSearches.get(1).name );
		assertEquals( "fire", fr.inAppSearches.get(1).query );
		assertEquals( "search_inbound_keywords", fr.inAppSearches.get(2).name );
		assertEquals( "fisj food", fr.inAppSearches.get(2).query );
		
		assertEquals( 1, fr.locations.size() );
		Location l = fr.locations.get(0);
		assertEquals( "Paris", l.city );
		assertEquals( "France", l.country );
		assertEquals( 48.8666, l.latitude, 3 );
		assertEquals( 2.333, l.longitude, 3 );
		assertEquals( "", l.name );
		assertEquals( "A8", l.region );
		
		AutoBlockConsumer sbc = (AutoBlockConsumer)ra.getConsumer();
		reset(sbc);
		
		sbc.save(fr);
		PreparedStatement ps = sbc.getConnection().prepareStatement("SELECT * FROM inbound_blacklist");
		ResultSet rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1406619880273L, rs.getLong("report_id") );
		assertEquals( "info@myios-cloud-mail.com", rs.getString("address") );
		assertEquals( 0, rs.getInt("count") );
		advance(rs, 2722);
		assertEquals( 1406619880273L, rs.getLong("report_id") );
		assertEquals( "admin@readyon.biz", rs.getString("address") );
		assertEquals( 0, rs.getInt("count") );
		assertTrue( !rs.next() );
		
		ps = sbc.getConnection().prepareStatement("SELECT * FROM inbound_keywords");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1406619880273L, rs.getLong("report_id") );
		assertEquals( "agrable", rs.getString("term") );
		assertEquals( 0, rs.getInt("count") );
		advance(rs, 2730);
		assertEquals( 1406619880273L, rs.getLong("report_id") );
		assertEquals( "ѕolutіon", rs.getString("term") );
		assertEquals( 0, rs.getInt("count") );
		assertTrue( !rs.next() );
		
		ps = sbc.getConnection().prepareStatement("SELECT * FROM logs");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1406619880273L, rs.getLong("report_id") );
		assertEquals( "smtp@email.planet-plan.fr", rs.getString("address") );
		assertTrue( rs.getString("message").startsWith("Bient&ocirc") );
		assertEquals( 1402572077799L, rs.getLong("timestamp") );
		advance(rs, 658);
		assertEquals( 1406619880273L, rs.getLong("report_id") );
		assertEquals( "noreply@opencamp.fr", rs.getString("address") );
		assertTrue( rs.getString("message").trim().startsWith(" Cliquez") );
		assertEquals( 1406615387656L, rs.getLong("timestamp") );
		assertTrue( !rs.next() );
		
		ps = sbc.getConnection().prepareStatement("SELECT * FROM outbound_blacklist");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1406619880273L, rs.getLong("report_id") );
		assertEquals( "something@hotmail.com", rs.getString("address") );
		assertEquals( 5, rs.getInt("count") );
		assertTrue( !rs.next() );
	}
	
	
	private void advance(ResultSet rs, int n) throws SQLException
	{
		for (int i = 0; i < n; i++) {
			assertTrue( rs.next() );
		}
	}
}