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
import com.canadainc.intelligence.model.InAppSearch;
import com.canadainc.intelligence.model.Report;

public class AutoBlockConsumerTest
{
	@Test
	public void testConsume() throws IOException
	{
		AutoBlockConsumer instance = new AutoBlockConsumer();
		Map<String,Consumer> consumers = new HashMap<String,Consumer>();
		consumers.put("AutoBlock", instance);

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
		AutoBlockConsumer instance = new AutoBlockConsumer();
		Map<String,Consumer> consumers = new HashMap<String,Consumer>();
		consumers.put("AutoBlock", instance);

		Report r = ReportCollector.extractReport( new File("res/auto_block/1403455028364") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		ra.analyze();
		
		assertNull( instance.consumeSetting("accountId", "1345666", null) );
		assertEquals( "1403361639000", instance.consumeSetting("autoblock_junk", "QVariant(QDateTime, QDateTime(\"Sat Jun 21 10:40:39 2014\") )", null) );
	}
	
	
	@Test
	public void testConsumeBulkKeywords() throws IOException
	{
		AutoBlockConsumer instance = new AutoBlockConsumer();
		Map<String,Consumer> consumers = new HashMap<String,Consumer>();
		consumers.put("AutoBlock", instance);

		Report r = ReportCollector.extractReport( new File("res/auto_block/1406619880273") );
		ReportAnalyzer ra = new ReportAnalyzer();
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
	}
}