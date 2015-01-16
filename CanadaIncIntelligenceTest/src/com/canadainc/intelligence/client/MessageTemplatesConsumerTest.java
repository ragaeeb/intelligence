package com.canadainc.intelligence.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.canadainc.common.io.DatabaseUtils;
import com.canadainc.intelligence.controller.ReportAnalyzer;
import com.canadainc.intelligence.io.ReportCollector;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.Report;

public class MessageTemplatesConsumerTest
{
	private void reset(Consumer c) throws Exception
	{
		String[] tables = {"message_templates"};

		Class.forName("org.sqlite.JDBC"); // load the sqlite-JDBC driver using the current class loader
		c.setPath("res/messagetemplates.db");

		DatabaseUtils.reset( c.getConnection(), tables );
	}
	
	
	@Before
	public void setUp() throws Exception
	{
	}

	@Test
	public void testConsume()
	{
	}

	@Test
	public void testConsumeSetting() throws Exception
	{
		Map<String,String> consumers = new HashMap<String,String>();
		consumers.put("Message Templates", "com.canadainc.intelligence.client.MessageTemplatesConsumer");

		Report r = ReportCollector.extractReport( new File("res/message_templates/1420389410952") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		FormattedReport fr = ra.analyze();
		
		MessageTemplatesConsumer c = (MessageTemplatesConsumer)ra.getConsumer();
		Map<String, String> templates = c.getTemplates();
		assertEquals( 3, templates.size() );
		assertEquals( "$5416r", templates.get( "Huis reset" ) );
		
		reset(c);
		
		c.save(fr);
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT * FROM message_templates");
		ResultSet rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1420389410952L, rs.getLong("report_id") );
		assertEquals( "Huis af", rs.getString("name") );
		assertEquals( "$5416A", rs.getString("message") );

		advance( rs, 2 );
	}
	
	private void advance(ResultSet rs, int n) throws SQLException
	{
		for (int i = 0; i < n; i++) {
			assertTrue( rs.next() );
		}
	}
}