package com.canadainc.intelligence.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.canadainc.intelligence.client.QuranConsumer.QuranPlaylist;
import com.canadainc.intelligence.controller.ReportAnalyzer;
import com.canadainc.intelligence.io.ReportCollector;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.Report;

public class QuranConsumerTest
{
	@Test
	public void testConsume() throws IOException
	{
		QuranConsumer instance = new QuranConsumer();
		Map<String,Consumer> consumers = new HashMap<String,Consumer>();
		consumers.put("Quran10", instance);

		Report r = ReportCollector.extractReport( new File("res/quran10/1399550993703") );
		ReportAnalyzer ra = new ReportAnalyzer();
		ra.setReport(r);
		ra.setConsumers(consumers);
		ra.analyze();
		
		List<QuranPlaylist> playlists = instance.getPlaylists();
		assertEquals( 8, playlists.size() );
		assertEquals( 4, playlists.get(0).chapter );
		assertEquals( 1, playlists.get(0).fromVerse );
		assertEquals( 176, playlists.get(0).toVerse );
		assertEquals( 110, playlists.get(7).chapter );
		assertEquals( 1, playlists.get(7).fromVerse );
		assertEquals( 3, playlists.get(7).toVerse );
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
		FormattedReport fr = ra.analyze();

		Collection<QuranBookmark> bookmarks = instance.getBookmarks();
		assertEquals( 1, bookmarks.size() );
		assertTrue( bookmarks.contains( new QuranBookmark(2,255) ) );
		
		assertEquals( 1, fr.inAppSearches.size() );
		assertEquals( "search_chapter", fr.inAppSearches.get(0).name );
		assertEquals( "Yus", fr.inAppSearches.get(0).query );
		
		assertEquals( 3, instance.getChaptersVisited().size() );
		assertEquals( 12, instance.getChaptersVisited().get(0).intValue() );
		assertEquals( 3, instance.getChaptersVisited().get(2).intValue() );
		
		assertEquals( 1, instance.getTafsirInterested().size() );
		assertEquals( 12, instance.getTafsirInterested().get(0).intValue() );
		
		assertEquals( 1, instance.getTafsirVisited().size() );
		assertEquals( 201, instance.getTafsirVisited().get(0).intValue() );
		
		assertEquals( 1, instance.getHomescreens().size() );
		assertEquals( 1, instance.getHomescreens().get(0).chapter );
		assertEquals( 1, instance.getHomescreens().get(0).verse );
		assertEquals( "First one", instance.getHomescreens().get(0).name );
	}
}