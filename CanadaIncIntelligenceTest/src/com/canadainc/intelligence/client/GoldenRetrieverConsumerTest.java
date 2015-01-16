package com.canadainc.intelligence.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.canadainc.common.io.DatabaseUtils;
import com.canadainc.intelligence.controller.ReportAnalyzer;
import com.canadainc.intelligence.io.ReportCollector;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.Report;

public class GoldenRetrieverConsumerTest
{
	private void reset(Consumer c) throws Exception
	{
		String[] tables = {"logs"};
		
		Class.forName("org.sqlite.JDBC"); // load the sqlite-JDBC driver using the current class loader
		c.setPath("res/goldenretriever.db");
		
		DatabaseUtils.reset( c.getConnection(), tables );
	}
	
	
	@Test
	public void testConsume() throws Exception
	{
		Map<String,String> consumers = new HashMap<String,String>();
		consumers.put("Golden Retriever", "com.canadainc.intelligence.client.GoldenRetrieverConsumer");

		Report r = ReportCollector.extractReport( new File("res/golden/1400700013412") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		FormattedReport fr = ra.analyze();
		
		GoldenRetrieverConsumer instance = (GoldenRetrieverConsumer)ra.getConsumer();
		reset(instance);
		instance.save(fr);
		
		PreparedStatement ps = instance.getConnection().prepareStatement("SELECT * FROM logs");
		ResultSet rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1400700013412L, rs.getLong("report_id") );
		assertEquals( 1, rs.getInt("command") );
		assertEquals( 1397981544369L, rs.getLong("timestamp") );
		assertEquals( "Battery Level: 78, Temperature: 26 degrees Celsius", rs.getString("reply") );
		
		for (int i = 0; i < 22; i++) {
			assertTrue( rs.next() );
		}
		
		assertTrue( !rs.next() );
		
		instance.getConnection().close();
	}

	@Test
	public void testConsumeSetting() throws IOException
	{
		Map<String,String> consumers = new HashMap<String,String>();
		consumers.put("Golden Retriever", "com.canadainc.intelligence.client.GoldenRetrieverConsumer");

		Report r = ReportCollector.extractReport( new File("res/golden/1400700013412") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		FormattedReport fr = ra.analyze();
		
		assertEquals( 4, fr.userInfo.emails.size() );
		assertEquals( "kitc8@ukr.net", fr.userInfo.emails.get(0) );
		assertEquals( "kytsyuk@ukr.net", fr.userInfo.emails.get(3) );
		
		assertEquals( 4, fr.inAppSearches.size() );
		assertEquals( "command", fr.inAppSearches.get(0).name );
		assertEquals( "battey", fr.inAppSearches.get(0).query );
		assertEquals( "contact Qassim Abdul", fr.inAppSearches.get(3).query );
	}
}
