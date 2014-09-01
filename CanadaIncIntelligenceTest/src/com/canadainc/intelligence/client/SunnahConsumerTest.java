package com.canadainc.intelligence.client;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.canadainc.intelligence.controller.ReportAnalyzer;
import com.canadainc.intelligence.io.ReportCollector;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.Report;

public class SunnahConsumerTest
{
	@Test
	public void testConsume() throws IOException
	{
		SunnahConsumer instance = new SunnahConsumer();
		Map<String,Consumer> consumers = new HashMap<String,Consumer>();
		consumers.put("Sunnah10", instance);

		Report r = ReportCollector.extractReport( new File("res/sunnah10/1408723806275") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		FormattedReport fr = ra.analyze();
		
		assertEquals( 1, fr.invokeTargets.size() );
		assertEquals( "com.canadainc.Sunnah10.previewer", fr.invokeTargets.get(0).target );
		assertEquals( "Bukhari #1)", fr.invokeTargets.get(0).data );
		assertEquals( 30, fr.inAppSearches.size() );
		
		assertEquals( 1, instance.getHomescreens().size() );
		assertEquals( 1300005, instance.getHomescreens().get(0).id );
		assertTrue( instance.getHomescreens().get(0).isTafsir );
		assertEquals( "Loll", instance.getHomescreens().get(0).name );
	}


	@Test
	public void testConsumeSetting() throws IOException
	{
		SunnahConsumer instance = new SunnahConsumer();
		Map<String,Consumer> consumers = new HashMap<String,Consumer>();
		consumers.put("Sunnah10", instance);

		Report r = ReportCollector.extractReport( new File("res/sunnah10/1408723806275") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		FormattedReport fr = ra.analyze();
		
		assertEquals( 4, fr.appSettings.size() );
	}
}