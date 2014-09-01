package com.canadainc.intelligence.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.canadainc.common.text.TextUtils;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.InAppSearch;
import com.canadainc.intelligence.model.Report;

public class QuranConsumer implements Consumer
{
	private static Collection<String> EXCLUDED_SETTINGS = new HashSet<String>();
	private final Pattern surahRegex = Pattern.compile("surah_id\", QVariant\\(int,\\s+\\d{1,3}");
	private final Pattern verseRegex = Pattern.compile("verse_id\"\\s+,\\s+QVariant\\(int,\\s+\\d{1,3}");
	private final Pattern searchSurahNameRegex = Pattern.compile("QString, \"SELECT surah_id,arabic_name[^\n]+");
	private final Pattern openSurahRegex = Pattern.compile("QString, \"SELECT arabic_uthmani.text as arabic,arabic_uthmani.verse_id[^\n]+");
	private final Pattern openSurahRegexTransliteration = Pattern.compile("QString, \"SELECT transliteration.text as arabic,transliteration.verse_id[^\n]+");
	private final Pattern tafsirInterestRegex = Pattern.compile("QString, \"SELECT id,description,verse_id,explainer[^\n]+");
	private final Pattern tafsirVisitedRegex = Pattern.compile("QString, \"SELECT \\* from tafsir_[^\n]+");
	private Collection<QuranBookmark> m_bookmarks = new HashSet<QuranBookmark>();
	private List<QuranBookmark> m_homescreens = new ArrayList<QuranBookmark>();
	public List<QuranBookmark> getHomescreens() {
		return m_homescreens;
	}

	private List<QuranPlaylist> m_playlists = new ArrayList<QuranPlaylist>();
	private List<Integer> m_chaptersVisited = new ArrayList<Integer>();
	private List<Integer> m_tafsirInterested = new ArrayList<Integer>();
	private List<Integer> m_tafsirVisited = new ArrayList<Integer>();

	public List<Integer> getTafsirVisited() {
		return m_tafsirVisited;
	}

	public List<Integer> getTafsirInterested() {
		return m_tafsirInterested;
	}

	public List<Integer> getChaptersVisited() {
		return m_chaptersVisited;
	}

	public List<QuranPlaylist> getPlaylists() {
		return m_playlists;
	}

	static {
		EXCLUDED_SETTINGS.add("alFurqanAdvertised");
		EXCLUDED_SETTINGS.add("firstTime");
		EXCLUDED_SETTINGS.add("hideAgreement");
		EXCLUDED_SETTINGS.add("tafsirTutorialCount");
	}

	public Collection<QuranBookmark> getBookmarks() {
		return m_bookmarks;
	}

	public QuranConsumer()
	{
	}

	@Override
	public void consume(Report r, FormattedReport fr)
	{
		for (String log: r.logs)
		{
			List<String> result = TextUtils.getValues("downloadAndPlay", log);

			for (String playlist: result)
			{
				String[] tokens = playlist.split(" ");

				QuranPlaylist qp = new QuranPlaylist( Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]) );
				m_playlists.add(qp);
			}
			
			Matcher m = searchSurahNameRegex.matcher(log);
			while ( m.find() )
			{
				String inner = log.substring( m.start(), m.end() );
				int start = inner.indexOf("'%");
				int end = inner.indexOf("%'", start);
				
				if (start >= 0 && end >= 0)
				{
					inner = inner.substring(start+2, end);
					fr.inAppSearches.add( new InAppSearch("search_chapter", inner) );
				}
			}
			
			m = openSurahRegex.matcher(log);
			while ( m.find() )
			{
				String inner = log.substring( m.start(), m.end() );
				int start = inner.lastIndexOf("=");
				int end = inner.indexOf("\"", start);

				if (start >= 0 && end >= 0)
				{
					inner = inner.substring(start+1, end).trim();
					m_chaptersVisited.add( Integer.parseInt(inner) );
				}
			}
			
			m = openSurahRegexTransliteration.matcher(log);
			while ( m.find() )
			{
				String inner = log.substring( m.start(), m.end() );
				int start = inner.lastIndexOf("=");
				int end = inner.indexOf("\"", start);

				if (start >= 0 && end >= 0)
				{
					inner = inner.substring(start+1, end).trim();
					m_chaptersVisited.add( Integer.parseInt(inner) );
				}
			}
			
			m = tafsirInterestRegex.matcher(log);
			while ( m.find() )
			{
				String inner = log.substring( m.start(), m.end() );
				int start = inner.lastIndexOf("=");
				int end = inner.indexOf(" ", start);

				if (start >= 0 && end >= 0)
				{
					inner = inner.substring(start+1, end).trim();
					m_tafsirInterested.add( Integer.parseInt(inner) );
				}
			}
			
			m = tafsirVisitedRegex.matcher(log);
			while ( m.find() )
			{
				String inner = log.substring( m.start(), m.end() );
				int start = inner.lastIndexOf("=");
				int end = inner.indexOf("\"", start);

				if (start >= 0 && end >= 0)
				{
					inner = inner.substring(start+1, end).trim();
					m_tafsirVisited.add( Integer.parseInt(inner) );
				}
			}
			
			result = TextUtils.getValues("addToHomeScreen Adding shortcut", log);

			for (String shortcut: result)
			{
				String[] tokens = shortcut.split(" (?=(([^'\"]*['\"]){2})*[^'\"]*$)");
				
				QuranBookmark qb = new QuranBookmark( Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]) );
				qb.name = TextUtils.removeQuotes(tokens[2]);
				m_homescreens.add(qb);
			}
		}
	}

	private static void populate(List<Integer> l, Pattern regex, String value)
	{
		Matcher m = regex.matcher(value);
		while ( m.find() )
		{
			String result = value.substring( m.start(), m.end() );
			result = result.substring( result.lastIndexOf(" ")+1 );
			l.add( Integer.parseInt(result) );
		}
	}

	@Override
	public String consumeSetting(String key, String value, FormattedReport fr)
	{
		if ( EXCLUDED_SETTINGS.contains(key) ) {
			return null;
		} else if ( key.equals("bookmarks") ) {
			List<Integer> chapters = new ArrayList<Integer>();
			List<Integer> verses = new ArrayList<Integer>();

			populate(chapters, surahRegex, value);
			populate(verses, verseRegex, value);

			if ( chapters.size() != verses.size() ) {
				throw new IllegalArgumentException("Mismatched bookmarks: "+chapters.size()+", "+verses.size() );
			}

			for (int i = 0; i < chapters.size(); i++)
			{
				QuranBookmark b = new QuranBookmark( chapters.get(i), verses.get(i) );
				m_bookmarks.add(b);
			}

			return null;
		}

		else {
			return value;
		}
	}
	
	
	public class QuranPlaylist
	{
		public int chapter;
		public int fromVerse;
		public int toVerse;

		public QuranPlaylist(int chapter, int fromVerse, int toVerse) {
			super();
			this.chapter = chapter;
			this.fromVerse = fromVerse;
			this.toVerse = toVerse;
		}
	}

}