package com.canadainc.intelligence.client;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.canadainc.intelligence.controller.ReportAnalyzer;
import com.canadainc.intelligence.io.ReportCollector;
import com.canadainc.intelligence.model.Report;

public class AutoBlockConsumerTest
{
	@Test
	public void testConsume()
	{
		
	}

	@Test
	public void testConsumeSetting() throws IOException
	{
		AutoBlockConsumer instance = new AutoBlockConsumer();
		Map<String,Consumer> consumers = new HashMap<String,Consumer>();
		consumers.put("AutoBlock", instance);

		Report r = ReportCollector.extractReport( new File("res/auto_block/1403455028364") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		ra.analyze();
		
		
	}
}