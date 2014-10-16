package com.canadainc.intelligence.client;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.canadainc.common.io.DatabaseUtils;
import com.canadainc.intelligence.controller.ReportAnalyzer;
import com.canadainc.intelligence.io.ReportCollector;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.Report;

public class SunnahConsumerTest
{
	private void reset(Consumer c) throws Exception
	{
		String[] tables = {"sunnah10_visited_books", "sunnah10_homescreen", "sunnah10_visited_narrations", "sunnah10_visited_narrations", "sunnah10_visited_tafsir", "bookmarks", "bookmarked_tafsir"};
		
		Class.forName("org.sqlite.JDBC"); // load the sqlite-JDBC driver using the current class loader
		c.setPath("res/sunnah.db");
		
		DatabaseUtils.reset( c.getConnection(), tables );
	}
	
	
	@Test
	public void testConsume() throws Exception
	{
		Map<String,String> consumers = new HashMap<String,String>();
		consumers.put("Sunnah10", "com.canadainc.intelligence.client.SunnahConsumer");

		Report r = ReportCollector.extractReport( new File("res/sunnah10/1408723806275") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		FormattedReport fr = ra.analyze();
		
		assertEquals( 1, fr.invokeTargets.size() );
		assertEquals( "com.canadainc.Sunnah10.previewer", fr.invokeTargets.get(0).target );
		assertEquals( "Bukhari #1)", fr.invokeTargets.get(0).data );
		assertEquals( 30, fr.inAppSearches.size() );
		
		SunnahConsumer instance = (SunnahConsumer)ra.getConsumer();
		assertEquals( 1, instance.getHomescreens().size() );
		assertEquals( 1300005, instance.getHomescreens().get(0).id );
		assertTrue( instance.getHomescreens().get(0).isTafsir );
		assertEquals( "Loll", instance.getHomescreens().get(0).name );
		
		assertEquals( 3, instance.getVisitedBooks().size() );
		assertEquals( 2, instance.getVisitedBooks().get(0).bookID );
		assertEquals( "abudawud", instance.getVisitedBooks().get(0).collection );
		assertEquals( 4, instance.getVisitedBooks().get(2).bookID );
		assertEquals( "malik", instance.getVisitedBooks().get(2).collection );
		
		assertEquals( 12, instance.getVisitedNarrationIds().size() );
		assertEquals( 723600, instance.getVisitedNarrationIds().get(0).intValue() );
		assertEquals( 945990, instance.getVisitedNarrationIds().get(11).intValue() );
		
		assertEquals( 1, instance.getVisitedTafsirIds().size() );
		assertEquals( 1405485481933L, instance.getVisitedTafsirIds().get(0).longValue() );
		
		reset(instance);
		instance.save(fr);
		PreparedStatement ps = instance.getConnection().prepareStatement("SELECT * FROM sunnah10_visited_books");
		ResultSet rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1408723806275L, rs.getLong("report_id") );
		assertEquals( 2, rs.getInt("book_id") );
		assertEquals( "abudawud", rs.getString("collection") );
		advance(rs,2);
		assertEquals( 1408723806275L, rs.getLong("report_id") );
		assertEquals( 4, rs.getInt("book_id") );
		assertEquals( "malik", rs.getString("collection") );
		assertTrue( !rs.next() );
		
		ps = instance.getConnection().prepareStatement("SELECT * FROM sunnah10_homescreen");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1408723806275L, rs.getLong("report_id") );
		assertEquals( 1300005, rs.getInt("id") );
		assertEquals( "Loll", rs.getString("name") );
		assertEquals( 1, rs.getInt("isTafsir") );
		assertTrue( !rs.next() );
		
		ps = instance.getConnection().prepareStatement("SELECT * FROM sunnah10_visited_narrations");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1408723806275L, rs.getLong("report_id") );
		assertEquals( 0, rs.getInt("hadith_id") );
		assertEquals( "bukhari", rs.getString("collection") );
		assertEquals( "1", rs.getString("hadith_number") );
		advance(rs,12);
		assertEquals( 1408723806275L, rs.getLong("report_id") );
		assertEquals( 945990, rs.getInt("hadith_id") );
		assertNull( rs.getString("collection") );
		assertNull( rs.getString("hadith_number") );
		assertTrue( !rs.next() );
		
		ps = instance.getConnection().prepareStatement("SELECT * FROM sunnah10_visited_tafsir");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1408723806275L, rs.getLong("report_id") );
		assertEquals( 1405485481933L, rs.getLong("tafsir_id") );
		assertTrue( !rs.next() );
		
		ps = instance.getConnection().prepareStatement("SELECT * FROM bookmarks");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1408723806275L, rs.getLong("report_id") );
		assertEquals( 1, rs.getInt("aid") );
		assertEquals( 12009, rs.getLong("timestamp") );
		assertEquals( "xyz", rs.getString("tag") );
		assertTrue( !rs.next() );
		
		ps = instance.getConnection().prepareStatement("SELECT * FROM bookmarked_tafsir");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1408723806275L, rs.getLong("report_id") );
		assertEquals( 1, rs.getInt("tid") );
		assertEquals( 123455, rs.getLong("timestamp") );
		assertEquals( "sdfs", rs.getString("tag") );
		assertTrue( !rs.next() );
		
		instance.getConnection().close();
	}


	@Test
	public void testConsumeSetting() throws IOException
	{
		Map<String,String> consumers = new HashMap<String,String>();
		consumers.put("Sunnah10", "com.canadainc.intelligence.client.SunnahConsumer");

		Report r = ReportCollector.extractReport( new File("res/sunnah10/1408723806275") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		FormattedReport fr = ra.analyze();
		
		assertEquals( 4, fr.appSettings.size() );
	}
	
	
	private void advance(ResultSet rs, int n) throws SQLException
	{
		for (int i = 0; i < n; i++) {
			assertTrue( rs.next() );
		}
	}
}