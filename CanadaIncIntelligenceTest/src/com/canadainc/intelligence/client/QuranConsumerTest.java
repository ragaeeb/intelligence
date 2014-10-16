package com.canadainc.intelligence.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.canadainc.common.io.DatabaseUtils;
import com.canadainc.intelligence.client.QuranConsumer.QuranPlaylist;
import com.canadainc.intelligence.controller.ReportAnalyzer;
import com.canadainc.intelligence.io.ReportCollector;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.Report;

public class QuranConsumerTest
{
	private void reset(Consumer c) throws Exception
	{
		String[] tables = {"quran10_bookmarks", "quran10_playlists", "quran10_tafsir_visited", "quran10_tafsir_interest", "quran10_homescreen", "quran10_chapters_visited"};

		Class.forName("org.sqlite.JDBC"); // load the sqlite-JDBC driver using the current class loader
		c.setPath("res/quran.db");

		DatabaseUtils.reset( c.getConnection(), tables );
	}


	@Test
	public void testConsume() throws Exception
	{
		Map<String,String> consumers = new HashMap<String,String>();
		consumers.put("Quran10", "com.canadainc.intelligence.client.QuranConsumer");

		Report r = ReportCollector.extractReport( new File("res/quran10/1399550993703") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		FormattedReport fr = ra.analyze();

		QuranConsumer c = (QuranConsumer)ra.getConsumer();
		List<QuranPlaylist> playlists = c.getPlaylists();
		assertEquals( 8, playlists.size() );
		assertEquals( 4, playlists.get(0).fromChapter );
		assertEquals( 1, playlists.get(0).fromVerse );
		assertEquals( 176, playlists.get(0).toVerse );
		assertEquals( 110, playlists.get(7).fromChapter );
		assertEquals( 1, playlists.get(7).fromVerse );
		assertEquals( 3, playlists.get(7).toVerse );

		reset(c);
		c.save(fr);
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT * FROM quran10_playlists");
		ResultSet rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1399550993703L, rs.getLong("report_id") );
		assertEquals( 4, rs.getInt("surah_id_start") );
		assertEquals( 1, rs.getInt("verse_id_start") );
		assertEquals( 4, rs.getInt("surah_id_end") );
		assertEquals( 176, rs.getInt("verse_id_end") );

		advance(rs, 7);
		assertEquals( 1399550993703L, rs.getLong("report_id") );
		assertEquals( 110, rs.getInt("surah_id_start") );
		assertEquals( 1, rs.getInt("verse_id_start") );
		assertEquals( 110, rs.getInt("surah_id_end") );
		assertEquals( 3, rs.getInt("verse_id_end") );
		assertTrue( !rs.next() );

		c.getConnection().close();
	}


	@Test
	public void testConsumeSettingQuranBookmark() throws Exception
	{
		Map<String,String> consumers = new HashMap<String,String>();
		consumers.put("Quran10", "com.canadainc.intelligence.client.QuranConsumer");

		Report r = ReportCollector.extractReport( new File("res/quran10_bookmark/1399902067127") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		FormattedReport fr = ra.analyze();

		QuranConsumer c = (QuranConsumer)ra.getConsumer();
		Collection<QuranBookmark> bookmarks = c.getBookmarks();
		assertEquals( 1, bookmarks.size() );
		assertTrue( bookmarks.contains( new QuranBookmark(2,255) ) );

		assertEquals( 1, fr.inAppSearches.size() );
		assertEquals( "search_chapter", fr.inAppSearches.get(0).name );
		assertEquals( "Yus", fr.inAppSearches.get(0).query );

		assertEquals( 3, c.getChaptersVisited().size() );
		assertEquals( 12, c.getChaptersVisited().get(0).intValue() );
		assertEquals( 3, c.getChaptersVisited().get(2).intValue() );

		assertEquals( 1, c.getTafsirInterested().size() );
		assertEquals( 12, c.getTafsirInterested().get(0).intValue() );

		assertEquals( 1, c.getTafsirVisited().size() );
		assertEquals( 201, c.getTafsirVisited().get(0).intValue() );

		assertEquals( 1, c.getHomescreens().size() );
		assertEquals( 1, c.getHomescreens().get(0).chapter );
		assertEquals( 1, c.getHomescreens().get(0).verse );
		assertEquals( "First one", c.getHomescreens().get(0).name );

		reset(c);
		c.save(fr);
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT * FROM quran10_bookmarks");
		ResultSet rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1399902067127L, rs.getLong("report_id") );
		assertEquals( 2, rs.getInt("surah_id") );
		assertEquals( 255, rs.getInt("verse_id") );
		assertEquals( "", rs.getString("name") );

		assertTrue( !rs.next() );

		ps = c.getConnection().prepareStatement("SELECT * FROM quran10_chapters_visited");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1399902067127L, rs.getLong("report_id") );
		assertEquals( 12, rs.getInt("chapter_id") );
		advance(rs, 2);
		assertEquals( 1399902067127L, rs.getLong("report_id") );
		assertEquals( 3, rs.getInt("chapter_id") );
		assertTrue( !rs.next() );

		ps = c.getConnection().prepareStatement("SELECT * FROM quran10_tafsir_visited");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1399902067127L, rs.getLong("report_id") );
		assertEquals( 201, rs.getInt("tafsir_id") );
		assertTrue( !rs.next() );

		ps = c.getConnection().prepareStatement("SELECT * FROM quran10_tafsir_interest");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1399902067127L, rs.getLong("report_id") );
		assertEquals( 12, rs.getInt("tafsir_id") );
		assertTrue( !rs.next() );

		ps = c.getConnection().prepareStatement("SELECT * FROM quran10_homescreen");
		rs = ps.executeQuery();
		assertTrue( rs.next() );
		assertEquals( 1399902067127L, rs.getLong("report_id") );
		assertEquals( 1, rs.getInt("surah_id") );
		assertEquals( 1, rs.getInt("verse_id") );
		assertEquals( "First one", rs.getString("name") );
		assertTrue( !rs.next() );

		c.getConnection().close();
	}


	private void advance(ResultSet rs, int n) throws SQLException
	{
		for (int i = 0; i < n; i++) {
			assertTrue( rs.next() );
		}
	}
}