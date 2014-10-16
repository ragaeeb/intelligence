package com.canadainc.intelligence.client;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.canadainc.common.text.TextUtils;
import com.canadainc.intelligence.client.QuranConsumer.QuranPlaylist;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.InAppSearch;
import com.canadainc.intelligence.model.Report;

public class SunnahConsumer implements Consumer
{
	private static final String EMAIL_ADDRESS_SUFFIX = ";expected_value";
	private static final String EMAIL_ADDRESS_PREFIX = "email_address:";


	public class SunnahBook
	{
		public int bookID;
		public String collection;

		public SunnahBook(String collection, int bookID) {
			super();
			this.collection = collection;
			this.bookID = bookID;
		}
	}
	public class SunnahShortcut
	{
		public long id;
		public String name = new String();
		boolean isTafsir;
		public SunnahShortcut(String name, long id, boolean isTafsir) {
			super();
			this.name = name;
			this.id = id;
			this.isTafsir = isTafsir;
		}
	}
	private static Collection<String> EXCLUDED_SETTINGS = new HashSet<String>();
	private static final Pattern invokeRegex = Pattern.compile("invoked\\s+\"bb.action[^\n]+", Pattern.CASE_INSENSITIVE);
	static {
		EXCLUDED_SETTINGS.add("alFurqanAdvertised");
		EXCLUDED_SETTINGS.add("dbVersion");
		EXCLUDED_SETTINGS.add("lastUpdateCheck");
	}
	private Connection m_connection;
	private List<SunnahShortcut> m_homescreens = new ArrayList<SunnahShortcut>();

	private List<SunnahBook> visitedBooks = new ArrayList<SunnahBook>();

	private List<SunnahHadith> visitedNarrationCollections = new ArrayList<SunnahHadith>();

	private List<Integer> visitedNarrationIds = new ArrayList<Integer>();

	private List<Long> visitedTafsirIds = new ArrayList<Long>();
	private String m_databasePath;

	public SunnahConsumer()
	{
	}

	@Override
	public void close() throws SQLException
	{
		m_connection.close();
	}

	@Override
	public void consume(Report r, FormattedReport fr)
	{
		if ( r.deviceInfo.contains("Notes: mistake_type") ) // mistake submission
		{
			int start = r.deviceInfo.indexOf(EMAIL_ADDRESS_PREFIX);
			int end = r.deviceInfo.indexOf(EMAIL_ADDRESS_SUFFIX);
			
			String email = r.deviceInfo.substring( start+EMAIL_ADDRESS_PREFIX.length(), end);
			fr.userInfo.emails.add(email);
		}
		
		for (String log: r.logs)
		{
			List<String> result = TextUtils.getValues("fetchHadith", log);
			for (String query: result)
			{
				String[] tokens = query.split(" ");

				if (tokens.length > 1) { // fetch hadith by collection name
					SunnahHadith s = new SunnahHadith( TextUtils.removeQuotes(tokens[0]), TextUtils.removeQuotes(tokens[1]) );
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
				
				if ( andMode.equals("true") ) {
					name += "_and";
				} else {
					name += "_or";
				}
				
				if ( shortNarrations.equals("true") ) {
					name += "_short";
				}
				
				InAppSearch ias = new InAppSearch( name, additional.toString() );
				
				if ( !excludedCollections.isEmpty() ) {
					ias.query += ";"+excludedCollections.toString();
				}
				
				fr.inAppSearches.add(ias);
			}

			result = TextUtils.getValues("fetchTafsirContent", log);
			for (String query: result)
			{
				if ( query.contains(" ") ) {
					String[] tokens = query.split(" ");
					query = tokens[tokens.length-1];
				}
				
				visitedTafsirIds.add( Long.parseLong(query) );
			}

			result = TextUtils.getValues("fetchBook", log);
			for (String query: result)
			{
				String[] tokens = query.split(" ");

				SunnahBook s = new SunnahBook( TextUtils.removeQuotes(tokens[0]), Integer.parseInt( tokens[1] ) );
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
		
		for (String asset: r.assets)
		{
			if ( asset.endsWith("bookmarks.db") )
			{
				m_databasePath = asset;
				break;
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


	@Override
	public Connection getConnection()
	{
		return m_connection;
	}

	public List<SunnahShortcut> getHomescreens() {
		return m_homescreens;
	}
	
	
	public List<SunnahBook> getVisitedBooks() {
		return visitedBooks;
	}
	
	
	public List<Integer> getVisitedNarrationIds() {
		return visitedNarrationIds;
	}


	public List<Long> getVisitedTafsirIds() {
		return visitedTafsirIds;
	}

	@Override
	public void save(FormattedReport fr)
	{
		try {
			PreparedStatement ps;
			
			if ( !visitedBooks.isEmpty() )
			{
				ps = m_connection.prepareStatement( "INSERT OR IGNORE INTO sunnah10_visited_books (report_id,book_id,collection) VALUES (?,?,?)");
				for (SunnahBook qb: visitedBooks)
				{
					int i = 0;
					ps.setLong(++i, fr.id);
					ps.setInt(++i, qb.bookID);
					ps.setString(++i, qb.collection);
					
					ps.executeUpdate();
				}
			}
			
			if ( !m_homescreens.isEmpty() )
			{
				ps = m_connection.prepareStatement( "INSERT OR IGNORE INTO sunnah10_homescreen (report_id,id,name,isTafsir) VALUES (?,?,?,?)");
				for (SunnahShortcut qb: m_homescreens)
				{
					int i = 0;
					ps.setLong(++i, fr.id);
					ps.setLong(++i, qb.id);
					ps.setString(++i, qb.name);
					ps.setInt(++i, qb.isTafsir ? 1 : 0);
					ps.executeUpdate();
				}
			}

			if ( !visitedNarrationCollections.isEmpty() )
			{
				ps = m_connection.prepareStatement( "INSERT OR IGNORE INTO sunnah10_visited_narrations (report_id, collection, hadith_number) VALUES (?,?,?)");
				for (SunnahHadith qb: visitedNarrationCollections)
				{
					int i = 0;
					ps.setLong(++i, fr.id);
					ps.setString(++i, qb.collection);
					ps.setString(++i, qb.hadithNumber);
					
					ps.executeUpdate();
				}
			}
			
			if ( !visitedNarrationIds.isEmpty() )
			{
				ps = m_connection.prepareStatement( "INSERT OR IGNORE INTO sunnah10_visited_narrations (report_id, hadith_id) VALUES (?,?)");
				for (Integer qb: visitedNarrationIds)
				{
					int i = 0;
					ps.setLong(++i, fr.id);
					ps.setInt(++i, qb);
					ps.executeUpdate();
				}
			}
			
			if ( !visitedTafsirIds.isEmpty() )
			{
				ps = m_connection.prepareStatement( "INSERT OR IGNORE INTO sunnah10_visited_tafsir (report_id,tafsir_id) VALUES (?,?)");
				for (Long qb: visitedTafsirIds)
				{
					int i = 0;
					ps.setLong(++i, fr.id);
					ps.setLong(++i, qb);
					ps.executeUpdate();
				}
			}
			
			if (m_databasePath != null)
			{
				m_connection.setAutoCommit(true);
				ps = m_connection.prepareStatement("ATTACH DATABASE '"+m_databasePath+"' AS 'source'");
				ps.execute();

				ps = m_connection.prepareStatement("INSERT OR IGNORE INTO bookmarks SELECT "+fr.id+",aid,tag,timestamp FROM source.bookmarks");
				ps.execute();

				ps = m_connection.prepareStatement("INSERT OR IGNORE INTO bookmarked_tafsir SELECT "+fr.id+",tid,tag,timestamp FROM source.bookmarked_tafsir");
				ps.execute();

				ps = m_connection.prepareStatement("DETACH DATABASE source");
				ps.execute();
				m_connection.setAutoCommit(false);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			try {
				m_connection.rollback();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} finally {
			try {
				m_connection.commit();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	@Override
	public void setPath(String path) throws Exception
	{
		if (m_connection != null) {
			m_connection.close();
		}
		
		m_connection = DriverManager.getConnection("jdbc:sqlite:"+path);
		m_connection.setAutoCommit(false);
	}
}