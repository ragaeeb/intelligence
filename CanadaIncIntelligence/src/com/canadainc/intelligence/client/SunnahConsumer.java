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

public class SunnahConsumer implements Consumer
{
	private static Collection<String> EXCLUDED_SETTINGS = new HashSet<String>();
	private static final Pattern invokeRegex = Pattern.compile("invoked\\s+\"bb.action[^\n]+", Pattern.CASE_INSENSITIVE);
	private List<Integer> visitedNarrationIds = new ArrayList<Integer>();
	private List<Long> visitedTafsirIds = new ArrayList<Long>();
	private List<SunnahShortcut> m_homescreens = new ArrayList<SunnahShortcut>();
	public List<SunnahShortcut> getHomescreens() {
		return m_homescreens;
	}

	public List<Long> getVisitedTafsirIds() {
		return visitedTafsirIds;
	}

	public List<Integer> getVisitedNarrationIds() {
		return visitedNarrationIds;
	}

	public void setVisitedNarrationIds(List<Integer> visitedNarrationIds) {
		this.visitedNarrationIds = visitedNarrationIds;
	}

	private List<SunnahHadith> visitedNarrationCollections = new ArrayList<SunnahHadith>();
	private List<SunnahBook> visitedBooks = new ArrayList<SunnahBook>();

	public List<SunnahBook> getVisitedBooks() {
		return visitedBooks;
	}

	static {
		EXCLUDED_SETTINGS.add("alFurqanAdvertised");
		EXCLUDED_SETTINGS.add("dbVersion");
		EXCLUDED_SETTINGS.add("lastUpdateCheck");
	}

	public SunnahConsumer()
	{
	}


	@Override
	public void consume(Report r, FormattedReport fr)
	{
		for (String log: r.logs)
		{
			List<String> result = TextUtils.getValues("fetchHadith", log);
			for (String query: result)
			{
				String[] tokens = query.split(" ");

				if (tokens.length > 1) { // fetch hadith by collection name
					SunnahHadith s = new SunnahHadith(tokens[0], tokens[1]);
					visitedNarrationCollections.add(s);
				} else { // fetch hadith by ID
					visitedNarrationIds.add( Integer.parseInt(tokens[0]) );
				}
			}

			result = TextUtils.getValues("searchQuery", log);
			for (String query: result)
			{
				int index = query.lastIndexOf(" ");				
				String shortNarrations = query.substring(index+1).trim();
				query = query.substring(0, index).trim();

				index = query.lastIndexOf(" ");				
				String andMode = query.substring(index+1).trim();
				query = query.substring(0, index).trim();

				index = query.indexOf(" QMap(");
				String searchTerm = query.substring(1, index-1);
				query = query.substring(index).trim();

				int endPos = TextUtils.findClosingBracket( query, query.indexOf("(") );
				String exclusions = query.substring(0, endPos+1);
				exclusions = exclusions.replaceAll("\\s+", "");
				List<String> excludedCollections = TextUtils.extractQuotedValues(exclusions, "(\"", "\",");

				query = query.substring(endPos+1).trim();
				ArrayList<String> additional = TextUtils.extractQuotedStringValues(query);
				additional.add(0, searchTerm);
				
				String name;
				
				if ( searchTerm.matches("[a-z]+(40)?:\\d{1,4}") ) {
					name = "shortcut";
				} else if ( searchTerm.matches("[a-w]{1}\\d{1,4}") ) {
					name = "turbo";
				} else if ( searchTerm.matches("[a-w]{1}\\d{1,2}\\.\\d{1,3}") ) {
					name = "book_turbo";
				} else {
					name = "standard";
				}
				
				InAppSearch ias = new InAppSearch( name, additional.toString() );
				
				if ( !excludedCollections.isEmpty() ) {
					ias.query += ";"+excludedCollections.toString();
				}
				
				fr.inAppSearches.add(ias);
			}

			result = TextUtils.getValues("fetchTafsirContent", log);
			for (String query: result) {
				visitedTafsirIds.add( Long.parseLong(query) );
			}

			result = TextUtils.getValues("fetchBook", log);
			for (String query: result)
			{
				String[] tokens = query.split(" ");

				SunnahBook s = new SunnahBook( tokens[0], Integer.parseInt( tokens[1] ) );
				visitedBooks.add(s);
			}

			Matcher m = invokeRegex.matcher(log);
			while ( m.find() )
			{
				String match = log.substring( m.start(), m.end() );
				String[] tokens = match.split(" (?=(([^'\"]*['\"]){2})*[^'\"]*$)");

				InvokeTarget it = new InvokeTarget( TextUtils.removeQuotes(tokens[2]) );
				it.uri = TextUtils.removeQuotes(tokens[5]);
				it.data = TextUtils.removeQuotes(tokens[6]);
				fr.invokeTargets.add(it);
			}
			
			result = TextUtils.getValues("addToHomeScreen", log);
			for (String query: result)
			{
				String[] tokens = query.split(" (?=(([^'\"]*['\"]){2})*[^'\"]*$)");
				SunnahShortcut s = new SunnahShortcut( TextUtils.removeQuotes(tokens[1]), Long.parseLong(tokens[0]), tokens[2].equals("true") );
				m_homescreens.add(s);
			}
		}
	}

	@Override
	public String consumeSetting(String key, String value, FormattedReport fr)
	{
		if ( EXCLUDED_SETTINGS.contains(key) ) {
			return null;
		} else {
			return value;
		}
	}
	
	
	public class SunnahShortcut
	{
		public String name = new String();
		public long id;
		public SunnahShortcut(String name, long id, boolean isTafsir) {
			super();
			this.name = name;
			this.id = id;
			this.isTafsir = isTafsir;
		}
		boolean isTafsir;
	}
	
	
	public class SunnahBook
	{
		public String collection;
		public int bookID;

		public SunnahBook(String collection, int bookID) {
			super();
			this.collection = collection;
			this.bookID = bookID;
		}
	}
}