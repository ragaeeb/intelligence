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
import com.canadainc.intelligence.model.Report;

public class QuranConsumerTest {

	@Test
	public void testConsume()
	{
	}

	@Test
	public void testConsumeSettingQuranBookmark() throws IOException
	{
		QuranConsumer instance = new QuranConsumer();
		Map<String,Consumer> consumers = new HashMap<String,Consumer>();
		consumers.put("Quran10", instance);

		Report r = ReportCollector.extractReport( new File("res/quran10_bookmark/1399902067127") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		ra.analyze();

		Collection<Bookmark> stops = instance.getBookmarks();
		assertEquals( 1, stops.size() );
		assertTrue( stops.contains( new Bookmark(2,255) ) );
	}
}