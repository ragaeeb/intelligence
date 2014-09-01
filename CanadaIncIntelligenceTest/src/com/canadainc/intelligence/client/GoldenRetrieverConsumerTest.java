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

public class GoldenRetrieverConsumerTest
{
	@Test
	public void testConsume()
	{
	}

	@Test
	public void testConsumeSetting() throws IOException
	{
		GoldenRetrieverConsumer instance = new GoldenRetrieverConsumer();
		Map<String,Consumer> consumers = new HashMap<String,Consumer>();
		consumers.put("Golden Retriever", instance);

		Report r = ReportCollector.extractReport( new File("res/golden/1400700013412") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		FormattedReport fr = ra.analyze();
		
		assertEquals( 4, fr.userInfo.emails.size() );
		assertEquals( "kitc8@ukr.net", fr.userInfo.emails.get(0) );
		assertEquals( "kytsyuk@ukr.net", fr.userInfo.emails.get(3) );
	}
}
