package com.canadainc.intelligence.client;

import static org.junit.Assert.*;

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
import com.canadainc.intelligence.model.Report;

public class SafeBrowseConsumerTest
{
	private void reset(Consumer c) throws Exception
	{
		String[] tables = {"safe_browse_homescreen", "controlled", "keywords", "passive"};

		Class.forName("org.sqlite.JDBC"); // load the sqlite-JDBC driver using the current class loader
		c.setPath("res/safebrowse.db");

		DatabaseUtils.reset( c.getConnection(), tables );
	}

	@Test
	public void testConsume() throws Exception
	{
		Map<String,String> consumers = new HashMap<String,String>();
		consumers.put("Safe Browse", "com.canadainc.intelligence.client.SafeBrowseConsumer");

		Report r = ReportCollector.extractReport( new File("res/safe_browse/1409582750351") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		FormattedReport fr = ra.analyze();

		assertEquals( "com.canadainc.SafeBrowse.shortcut", fr.invokeTargets.get(0).target );
		assertEquals( "safebrowse:url::http://www.purple.com", fr.invokeTargets.get(0).uri );

		SafeBrowseConsumer sbc = (SafeBrowseConsumer)ra.getConsumer();
		assertEquals( 1, sbc.getHomescreens().size() );
		assertEquals( "Purple", sbc.getHomescreens().get(0).name );
		assertEquals( "http://www.purple.com/", sbc.getHomescreens().get(0).uri );

		reset(sbc);
		sbc.save(fr);
		PreparedStatement ps = sbc.getConnection().prepareStatement("SELECT * FROM safe_browse_homescreen");
		ResultSet rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1409582750351L, rs.getLong("report_id") );
		assertEquals( "Purple", rs.getString("name") );
		assertEquals( "http://www.purple.com/", rs.getString("uri") );
		assertTrue( !rs.next() );

		ps = sbc.getConnection().prepareStatement("SELECT * FROM controlled");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1409582750351L, rs.getLong("report_id") );
		assertEquals( "hotmail.com", rs.getString("uri") );
		advance(rs,2);
		assertEquals( 1409582750351L, rs.getLong("report_id") );
		assertEquals( "mozilla.org", rs.getString("uri") );
		assertTrue( !rs.next() );
		
		ps = sbc.getConnection().prepareStatement("SELECT * FROM passive");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1409582750351L, rs.getLong("report_id") );
		assertEquals( "youtube.com", rs.getString("uri") );
		assertTrue( rs.next() );
		assertEquals( 1409582750351L, rs.getLong("report_id") );
		assertEquals( "twitter.com", rs.getString("uri") );
		assertTrue( !rs.next() );
		
		ps = sbc.getConnection().prepareStatement("SELECT * FROM keywords");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1409582750351L, rs.getLong("report_id") );
		assertEquals( "casyrate", rs.getString("term") );
		assertTrue( rs.next() );
		assertEquals( 1409582750351L, rs.getLong("report_id") );
		assertEquals( "migrate", rs.getString("term") );
		assertTrue( !rs.next() );

		sbc.getConnection().close();
	}

	@Test
	public void testConsumeSetting() throws IOException
	{
		Map<String,String> consumers = new HashMap<String,String>();
		consumers.put("Safe Browse", "com.canadainc.intelligence.client.SafeBrowseConsumer");

		Report r = ReportCollector.extractReport( new File("res/safe_browse/1409582750351") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		FormattedReport fr = ra.analyze();

		assertEquals( 3, fr.appSettings.size() );
	}


	private void advance(ResultSet rs, int n) throws SQLException
	{
		for (int i = 0; i < n; i++) {
			assertTrue( rs.next() );
		}
	}
}