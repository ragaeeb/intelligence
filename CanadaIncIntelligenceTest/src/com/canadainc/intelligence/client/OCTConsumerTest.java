package com.canadainc.intelligence.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.canadainc.common.io.DatabaseUtils;
import com.canadainc.intelligence.controller.ReportAnalyzer;
import com.canadainc.intelligence.io.ReportCollector;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.Report;

public class OCTConsumerTest
{
	private void reset(Consumer c) throws Exception
	{
		String[] tables = {"oct_routes_accessed"};
		
		Class.forName("org.sqlite.JDBC"); // load the sqlite-JDBC driver using the current class loader
		c.setPath("res/oct.db");
		
		DatabaseUtils.reset( c.getConnection(), tables );
	}
	
	
	@Test
	public void testConsume()
	{
	}


	@Test
	public void testConsumeSettingSingleReport() throws IOException
	{
		Map<String,String> consumers = new HashMap<String,String>();
		consumers.put("oct10", "com.canadainc.intelligence.client.OCTConsumer");

		Report r = ReportCollector.extractReport( new File("res/single_report/1399751585701.txt") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		ra.analyze();
	}


	@Test
	public void testConsumeSettingOCT10() throws Exception
	{
		Map<String,String> consumers = new HashMap<String,String>();
		consumers.put("OCT", "com.canadainc.intelligence.client.OCTConsumer");

		Report r = ReportCollector.extractReport( new File("res/oct10/1401241983499") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		FormattedReport result = ra.analyze();
		OCTConsumer instance = (OCTConsumer)ra.getConsumer();
		
		assertEquals( 4, result.appSettings.size() );
		assertEquals( 1, result.inAppSearches.size() );
		assertEquals( "performStopsQuery", result.inAppSearches.get(0).name );
		assertEquals( "WELLINGTON / O'CONNOR", result.inAppSearches.get(0).query );
		
		assertEquals( 1, instance.getHomescreens().size() );
		assertEquals( 7196, instance.getHomescreens().get(0).stopCode );
		assertEquals( "7196:WALKLEY / JASPER (8)", instance.getHomescreens().get(0).name );
		
		reset(instance);
		instance.save(result);
		PreparedStatement ps = instance.getConnection().prepareStatement("SELECT * FROM oct_routes_accessed");
		ResultSet rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1401241983499L, rs.getLong("report_id") );
		assertEquals( 7196, rs.getInt("stop_code") );
		assertEquals( "7196:WALKLEY / JASPER (8)", rs.getString("name") );
		assertTrue( !rs.next() );
		
		instance.getConnection().close();
	}
}