package com.canadainc.intelligence.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.canadainc.intelligence.model.Report;

public class SalatConsumer implements Consumer
{
	private final Pattern surahRegex = Pattern.compile("surah_id\", QVariant\\(int,\\s+\\d{1,3}", Pattern.CASE_INSENSITIVE);
	private final Pattern verseRegex = Pattern.compile("verse_id\"\\s+,\\s+QVariant\\(int,\\s+\\d{1,3}", Pattern.CASE_INSENSITIVE);
	private Collection<Bookmark> m_bookmarks = new HashSet<Bookmark>();
	
	public Collection<Bookmark> getBookmarks() {
		return m_bookmarks;
	}

	public SalatConsumer()
	{
	}

	@Override
	public void consume(Report r)
	{
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
	public String consumeSetting(String key, String value)
	{
		if ( key.equals("bookmarks") )
		{
			List<Integer> chapters = new ArrayList<Integer>();
			List<Integer> verses = new ArrayList<Integer>();
			
			populate(chapters, surahRegex, value);
			populate(verses, verseRegex, value);
			
			if ( chapters.size() != verses.size() ) {
				throw new IllegalArgumentException("Mismatched bookmarks: "+chapters.size()+", "+verses.size() );
			}
			
			for (int i = 0; i < chapters.size(); i++)
			{
				Bookmark b = new Bookmark( chapters.get(i), verses.get(i) );
				m_bookmarks.add(b);
			}

			return null;
		}
		
		else {
			return value;
		}
	}
}