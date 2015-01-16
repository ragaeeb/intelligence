package com.canadainc.intelligence.client;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.canadainc.intelligence.controller.ReportAnalyzer;
import com.canadainc.intelligence.io.ReportCollector;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.Location;
import com.canadainc.intelligence.model.Report;

public class SalatConsumerTest
{
	@Test
	public void testConsume()
	{
	}

	@Test
	public void testConsumeSetting() throws IOException
	{
		Map<String,String> consumers = new HashMap<String,String>();
		consumers.put("Salat10", "com.canadainc.intelligence.client.SalatConsumer");

		Report r = ReportCollector.extractReport( new File("res/salat10/1401895045955") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		FormattedReport fr = ra.analyze();
		
		assertEquals( 25, fr.appSettings.size() );
		assertEquals( "1", fr.appSettings.get("athaans_fajr") );
		assertEquals( "1", fr.appSettings.get("notifications_fajr") );
		assertEquals( "0", fr.appSettings.get("notifications_halfNight") );
		assertEquals( "-2", fr.appSettings.get("adjustments_fajr") );
		assertEquals( 1, fr.locations.size() );
		
		Location l = fr.locations.get(0);
		assertEquals( "Kuala Lumpur", l.city );
		assertEquals( "MYS", l.country );
		assertEquals( 3.1544292, l.latitude, 3 );
		assertEquals( 101.7151017, l.longitude, 3 );
		assertEquals( "Kuala Lumpur, Kuala Lumpur Federal Territory, MYS", l.name );
		assertEquals( "", l.region );
	}
	
	@Test
	public void testConsumeSettingProfiles() throws IOException
	{
		Map<String,String> consumers = new HashMap<String,String>();
		consumers.put("Salat10", "com.canadainc.intelligence.client.SalatConsumer");

		Report r = ReportCollector.extractReport( new File("res/salat10/1404042526979") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		FormattedReport fr = ra.analyze();
		
		assertEquals( 30, fr.appSettings.size() );
		assertEquals( "1", fr.appSettings.get("athaans_dhuhr") );
		assertEquals( "1", fr.appSettings.get("notifications_fajr") );
		assertEquals( "1", fr.appSettings.get("notifications_halfNight") );
		assertEquals( "0", fr.appSettings.get("adjustments_fajr") );
		assertEquals( "0", fr.appSettings.get("profiles_1") );
		assertEquals( "1", fr.appSettings.get("profiles_3") );
		
		assertEquals( 1, fr.locations.size() );
		Location l = fr.locations.get(0);
		assertEquals( "Mukomuko Utara", l.city );
		assertEquals( "Indonesia", l.country );
		assertEquals( -2.58255, l.latitude, 3 );
		assertEquals( 101.118, l.longitude, 3 );
		assertEquals( "Jalan Bengkulu - Muko-muko, Mukomuko Utara, Bengkulu, Indonesia", l.name );
		assertEquals( "", l.region );
	}
}