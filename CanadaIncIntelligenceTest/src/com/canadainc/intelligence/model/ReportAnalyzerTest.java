package com.canadainc.intelligence.model;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.canadainc.common.io.IOUtils;
import com.canadainc.intelligence.client.Consumer;
import com.canadainc.intelligence.io.ReportCollector;

public class ReportAnalyzerTest
{
	@Test
	public void testAnalyzeSingleReport() throws IOException
	{
		Consumer c = new Consumer()
		{
			@Override
			public String consumeSetting(String key, String value) {
				return key.equals("bookmarks") ? value : null;
			}

			@Override
			public void consume(Report r) {}
		};
		Map<String,Consumer> consumers = new HashMap<String,Consumer>();
		consumers.put("oct10", c);
		
		Report r = ReportCollector.extractReport( new File("res/single_report/1399751585701.txt") );
		ReportAnalyzer instance = new ReportAnalyzer();
		instance.setReport(r);
		instance.setConsumers(consumers);

		FormattedReport fr = instance.analyze();
		assertEquals("oct10", fr.appInfo.name);
		assertEquals("Sun Feb 09 15:22:47 EST 2014", new Date(fr.os.creationDate).toString() );
		assertEquals("MSM8960_V3.2.1.1_F_R070_Rev:19", fr.userInfo.machine);
		assertEquals("BLACKBERRY-DD21", fr.userInfo.nodeName);
		assertEquals("en_US", fr.locale);

		List<DatabaseStat> stats = fr.databaseStats;
		assertEquals( 1, stats.size() );
		assertEquals( 4, stats.get(0).queryId );
		assertEquals( 81, stats.get(0).duration );
		assertEquals( 1, stats.get(0).elements );

		List<Location> locations = fr.locations;
		assertEquals( 1, locations.size() );
		assertEquals( 45.4114, locations.get(0).latitude, 0.01 );
		assertEquals( -75.6901, locations.get(0).longitude, 0.01 );
		
		assertEquals( 1, fr.appSettings.size() );
		assertTrue( fr.appSettings.containsKey("bookmarks") );
		String value = fr.appSettings.get("bookmarks");
		assertTrue( value.startsWith("QVariant(QVariantList, (QVariant(QVariantMap, QMap((\"name\"") && value.endsWith("CATHERINE / O'CONNOR\") ) )  ) ) )  ) )  )") );
	}
	
	
	@Test
	public void testAnalyzeSingleFolder() throws IOException
	{
		Report r = ReportCollector.extractReport( new File("res/single_folder/1402536925638") );
		ReportAnalyzer instance = new ReportAnalyzer();
		instance.setReport(r);

		FormattedReport fr = instance.analyze();
		assertEquals("OCT", fr.appInfo.name);
		assertEquals("Wed Apr 30 21:54:52 EDT 2014", new Date(fr.os.creationDate).toString() );
		assertEquals("10.2.1.3175", fr.os.version);
		assertEquals("MSM8960_V3.2.1.1_N_R085_Rev:16", fr.userInfo.machine);
		assertEquals("BLACKBERRY-60D3", fr.userInfo.nodeName);
		assertTrue( fr.locale.isEmpty() );
		assertEquals( 161619968, fr.appInfo.memoryUsage );
		
		List<DatabaseStat> stats = fr.databaseStats;
		assertEquals( 5, stats.size() );
		assertEquals( 2, stats.get(0).queryId );
		assertEquals( 49, stats.get(0).duration );
		assertEquals( 1, stats.get(0).elements );
		assertEquals( 1, stats.get( stats.size()-1 ).queryId );
		assertEquals( 68, stats.get( stats.size()-1 ).duration );
		assertEquals( 3, stats.get( stats.size()-1 ).elements );
		
		assertEquals( 6, fr.appSettings.size() );
		assertEquals( "201404061716", fr.appSettings.get("dbVersion") );
		assertEquals( "0.004", fr.appSettings.get("radius") );
		
		assertEquals( 5, fr.userEvents.size() );
		assertEquals( "StopTriggered", fr.userEvents.get(0) );
		assertEquals( "RoutePickerSheet Cancel", fr.userEvents.get( fr.userEvents.size()-1 ) );
	}
	

	@Test
	public void testAnalyzeAutoBlock() throws IOException
	{
		Report r = ReportCollector.extractReport( new File("res/auto_block/1403455028364") );
		ReportAnalyzer instance = new ReportAnalyzer();
		instance.setReport(r);

		FormattedReport fr = instance.analyze();
		assertEquals( 3, fr.userInfo.emails.size() );
		assertEquals( "kcs@isbginc.com", fr.userInfo.emails.get(0) );
		assertEquals( "kkobrin3@gmail.com", fr.userInfo.emails.get(1) );
		assertEquals( "lfkk@isbginc.com", fr.userInfo.emails.get(2) );

		assertEquals("AutoBlock", fr.appInfo.name);
		assertEquals("Thu Feb 20 15:03:41 EST 2014", new Date(fr.os.creationDate).toString() );
		assertEquals("10.2.1.2174", fr.os.version);
		assertEquals("MSM8960_V3.2.1.1_N_R084_Rev:12", fr.userInfo.machine);
		assertEquals("BLACKBERRY-10DA", fr.userInfo.nodeName);
		assertEquals("en_US", fr.locale);
		assertEquals( 151990272, fr.appInfo.memoryUsage );
		
		assertEquals( 7, fr.appSettings.size() );
		assertEquals( "3", fr.appSettings.get("keywordThreshold") );
		assertEquals( "1", fr.appSettings.get("whitelistContacts") );
		
		List<DatabaseStat> stats = fr.databaseStats;
		assertEquals( 23, stats.size() );
		assertEquals( 1, stats.get(0).queryId ); // this is from card.log
		assertEquals( 14, stats.get(0).duration );
		assertEquals( 0, stats.get(0).elements );
		assertEquals( 7, stats.get( stats.size()-1 ).queryId ); // this is from the ui.log
		assertEquals( 53, stats.get( stats.size()-1 ).duration );
		assertEquals( 0, stats.get( stats.size()-1 ).elements );
		
		assertEquals( 13, fr.userEvents.size() );
		assertEquals( "MultiBlock", fr.userEvents.get(0) );
		assertEquals( "MultiBlock", fr.userEvents.get( fr.userEvents.size()-1 ) );
	}
	
	
	@Test
	public void testAnalyzeQuran() throws IOException
	{
		Report r = ReportCollector.extractReport( new File("res/quran10/1399550993703") );
		ReportAnalyzer instance = new ReportAnalyzer();
		instance.setReport(r);

		FormattedReport fr = instance.analyze();
		assertEquals("Quran10", fr.appInfo.name);
		assertEquals("Sun Feb 09 15:22:47 EST 2014", new Date(fr.os.creationDate).toString() );
		assertEquals("10.2.1.2141", fr.os.version);
		assertEquals("MSM8960_V3.2.1.1_F_R085_Rev:16", fr.userInfo.machine);
		assertEquals("BLACKBERRY-Q10", fr.userInfo.nodeName);
		assertTrue( fr.locale.isEmpty() );
		assertEquals( 212058112, fr.appInfo.memoryUsage );
		
		assertEquals( 17, fr.appSettings.size() );
		assertEquals( "1", fr.appSettings.get("alFurqanAdvertised") );
		assertEquals( "1", fr.appSettings.get("v3.5") );
		
		List<DatabaseStat> stats = fr.databaseStats;
		assertEquals( 47, stats.size() );
		assertEquals( 1, stats.get(0).queryId ); // this is from card.log
		assertEquals( 21, stats.get(0).duration );
		assertEquals( 114, stats.get(0).elements );
		assertEquals( 47, stats.get( stats.size()-1 ).queryId ); // this is from the ui.log
		assertEquals( 38, stats.get( stats.size()-1 ).duration );
		assertEquals( 1, stats.get( stats.size()-1 ).elements );
		
		assertEquals( 63, fr.userEvents.size() );
		assertEquals( "SurahTriggered", fr.userEvents.get(0) );
		assertEquals( "SubmitLogs", fr.userEvents.get( fr.userEvents.size()-1 ) );
	}
}