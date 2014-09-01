package com.canadainc.intelligence.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.canadainc.intelligence.controller.ReportAnalyzer;
import com.canadainc.intelligence.io.ReportCollector;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.Report;

public class OCTConsumerTest
{
	@Test
	public void testConsume()
	{
	}


	@Test
	public void testConsumeSettingSingleReport() throws IOException
	{
		OCTConsumer instance = new OCTConsumer();
		Map<String,Consumer> consumers = new HashMap<String,Consumer>();
		consumers.put("oct10", instance);

		Report r = ReportCollector.extractReport( new File("res/single_report/1399751585701.txt") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		ra.analyze();
	}


	@Test
	public void testConsumeSettingOCT10() throws IOException
	{
		OCTConsumer instance = new OCTConsumer();
		Map<String,Consumer> consumers = new HashMap<String,Consumer>();
		consumers.put("OCT", instance);

		Report r = ReportCollector.extractReport( new File("res/oct10/1401241983499") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		FormattedReport result = ra.analyze();
		assertEquals( 4, result.appSettings.size() );
		assertEquals( 1, result.inAppSearches.size() );
		assertEquals( "performStopsQuery", result.inAppSearches.get(0).name );
		assertEquals( "WELLINGTON / O'CONNOR", result.inAppSearches.get(0).query );
		
		assertEquals( 1, instance.getHomescreens().size() );
		assertEquals( 7196, instance.getHomescreens().get(0).stopCode );
		assertEquals( "7196:WALKLEY / JASPER (8)", instance.getHomescreens().get(0).name );
	}
}