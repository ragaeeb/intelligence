package com.canadainc.intelligence.model;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
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
import com.canadainc.intelligence.controller.ReportAnalyzer;
import com.canadainc.intelligence.io.ReportCollector;

public class ReportAnalyzerTest
{
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
		assertEquals("MSM8960_V3.2.1.1_N_R084_Rev:12", fr.hardwareInfo.machine);
		assertEquals("BLACKBERRY-10DA", fr.userInfo.nodeName);
		assertEquals("en_US", fr.locale);
		assertEquals( 151990272, fr.memoryUsage );
		
		assertEquals( 6, fr.appSettings.size() );
		assertEquals( "3", fr.appSettings.get("keywordThreshold") );
		assertEquals( "1", fr.appSettings.get("whitelistContacts") );
		
		assertEquals( 7, fr.conversationsFetched.size() );
		assertEquals( 300, fr.conversationsFetched.get(0).intValue() );
		assertEquals( 150, fr.conversationsFetched.get(6).intValue() );
		
		assertEquals( 7, fr.pimElementsFetched.size() );
		assertEquals( 14, fr.pimElementsFetched.get(0).intValue() );
		assertEquals( 0, fr.pimElementsFetched.get(5).intValue() );
		assertEquals( 27, fr.pimElementsFetched.get(6).intValue() );
		
		assertEquals(7, fr.totalAccounts);
		
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
	public void testAnalyzeAutoReply() throws IOException
	{
		Report r = ReportCollector.extractReport( new File("res/autoreply/1401448387928") );
		ReportAnalyzer instance = new ReportAnalyzer();
		instance.setReport(r);

		FormattedReport fr = instance.analyze();
		assertEquals("AutoReply", fr.appInfo.name);
		assertEquals("Sun Feb 09 15:22:47 EST 2014", new Date(fr.os.creationDate).toString() );
		assertEquals("10.2.1.2141", fr.os.version);
		assertEquals("OMAP4470_ES1.0_HS_London_Rev:09", fr.hardwareInfo.machine);
		assertEquals("PUTRASEGO-3A07", fr.userInfo.nodeName);
		assertEquals( "id_ID", fr.locale );
		assertEquals( "1", fr.appSettings.get("led") );
		assertEquals( 191209472, fr.memoryUsage );
		assertTrue( fr.userInfo.emails.isEmpty() );
		assertEquals( 13, fr.userEvents.size() );
		assertEquals( "SubmitLogs", fr.userEvents.get( fr.userEvents.size()-1 ) );
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
		assertEquals("MSM8960_V3.2.1.1_F_R085_Rev:16", fr.hardwareInfo.machine);
		assertEquals("BLACKBERRY-Q10", fr.userInfo.nodeName);
		assertTrue( fr.locale.isEmpty() );
		assertEquals( 212058112, fr.memoryUsage );
		
		assertEquals( 17, fr.appSettings.size() );
		assertEquals( "1", fr.appSettings.get("alFurqanAdvertised") );
		assertNull( fr.appSettings.get("v3.5") );
		
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
		assertEquals("MSM8960_V3.2.1.1_N_R085_Rev:16", fr.hardwareInfo.machine);
		assertEquals("BLACKBERRY-60D3", fr.userInfo.nodeName);
		assertTrue( fr.locale.isEmpty() );
		assertEquals( 161619968, fr.memoryUsage );
		
		List<DatabaseStat> stats = fr.databaseStats;
		assertEquals( 5, stats.size() );
		assertEquals( 2, stats.get(0).queryId );
		assertEquals( 49, stats.get(0).duration );
		assertEquals( 1, stats.get(0).elements );
		assertEquals( 1, stats.get( stats.size()-1 ).queryId );
		assertEquals( 68, stats.get( stats.size()-1 ).duration );
		assertEquals( 3, stats.get( stats.size()-1 ).elements );
		
		assertEquals( 5, fr.appSettings.size() );
		assertEquals( "201404061716", fr.appSettings.get("dbVersion") );
		assertEquals( "0.004", fr.appSettings.get("radius") );
		
		assertEquals( 5, fr.userEvents.size() );
		assertEquals( "StopTriggered", fr.userEvents.get(0) );
		assertEquals( "RoutePickerSheet Cancel", fr.userEvents.get( fr.userEvents.size()-1 ) );
	}
	
	
	@Test
	public void testAnalyzeSingleReport() throws IOException
	{
		Report r = ReportCollector.extractReport( new File("res/single_report/1399751585701.txt") );
		ReportAnalyzer instance = new ReportAnalyzer();
		instance.setReport(r);

		FormattedReport fr = instance.analyze();
		assertEquals("oct10", fr.appInfo.name);
		assertEquals("Sun Feb 09 15:22:47 EST 2014", new Date(fr.os.creationDate).toString() );
		assertEquals("MSM8960_V3.2.1.1_F_R070_Rev:19", fr.hardwareInfo.machine);
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
	}
	
	
	@Test
	public void testAnalyzeSunnah10() throws IOException
	{
		Report r = ReportCollector.extractReport( new File("res/sunnah10/1408723806275") );
		ReportAnalyzer instance = new ReportAnalyzer();
		instance.setReport(r);

		FormattedReport fr = instance.analyze();
		assertEquals("Sunnah10", fr.appInfo.name);
		assertEquals("10.3.0.1166", fr.os.version);
		assertEquals("MSM8960AB_CS_PVS2_ARISTO_Rev:11", fr.hardwareInfo.machine);
		assertEquals( "Z30", fr.hardwareInfo.modelName );
		assertEquals( "STA100-1", fr.hardwareInfo.modelNumber );
		assertFalse( fr.hardwareInfo.physicalKeyboard );
		assertEquals("Ragaeeb7D", fr.userInfo.nodeName);
		assertTrue( fr.locale.isEmpty() );
		assertEquals("Thu Aug 21 16:18:37 EDT 2014", new Date(fr.bootTime).toString() ); // Aug 21 16:18:37 EDT 2014
		assertTrue( fr.userInfo.internal );
		assertEquals( 3, fr.batteryInfo.chargingState );
		assertEquals( 295, fr.batteryInfo.cycleCount );
		assertEquals( 31, fr.batteryInfo.temperature );
		assertEquals( 692178944, fr.availableMemory );
		
		assertEquals( "10.231.180.108", fr.network.bcm0 );
		assertEquals( "FE80:3B::96EB:CDFF:FE91:9C2C", fr.network.bptp0 );
		assertEquals( "c73-039.rim.net", fr.network.host );
		assertEquals( "208.65.73.39", fr.network.ip );
		assertEquals( "10.87.1.15", fr.network.msm0 );
		
		assertEquals( 95, fr.removedApps.size() );
		DeviceAppInfo dai = fr.removedApps.get(0);
		assertEquals( "DialogTest.testRel_DialogTest_2443ae1a", dai.packageName );
		assertEquals( "10.3.0.17", dai.packageVersion );
		assertEquals(0, dai.appWorldInfo.id);
		
		dai = fr.removedApps.get(2);
		assertEquals( "Stocks.BB10.gYABgMQXznH4wRvvvN74EmaRiM0", dai.packageName );
		assertEquals( "1.4.1.1", dai.packageVersion );
		AppWorldInfo awi = dai.appWorldInfo;
		assertEquals(20081905, awi.contentId);
		assertEquals("http://download.appworld.blackberry.com/ClientAPI/image/15868036", awi.iconUri);
		assertEquals("Stocks for BlackBerry 10", awi.name);
		assertEquals("Stocks_bb10", awi.sku);
		assertEquals("Alex Garipian", awi.vendor);
		assertEquals(25656264, awi.id);
		
		dai = fr.removedApps.get(8);
		assertEquals( "com.foursquare.blackberry.gYABgBY3zYaCRi7CDRw5ChZRJ18", dai.packageName );
		assertEquals( "10.4.4.1720", dai.packageVersion );
		awi = dai.appWorldInfo;
		assertEquals(6921, awi.contentId);
		assertTrue( awi.iconUri.isEmpty() );
		assertTrue( awi.name.isEmpty() );
		assertTrue( awi.sku.isEmpty() );
		assertTrue( awi.vendor.isEmpty() );
		assertEquals(0, awi.id);
	}
}