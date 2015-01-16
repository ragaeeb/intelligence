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
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.Report;

public class SafeBrowseConsumer implements Consumer
{
	private static Collection<String> EXCLUDED_SETTINGS = new HashSet<String>();
	private static final Pattern invokeRegex = Pattern.compile("invoked\\s+\"bb.action[^\n]+", Pattern.CASE_INSENSITIVE);
	private List<SafeBrowseShortcut> m_homescreens = new ArrayList<SafeBrowseShortcut>();
	private Connection m_connection;
	private String m_databasePath;
	public List<SafeBrowseShortcut> getHomescreens() {
		return m_homescreens;
	}

	static {
		EXCLUDED_SETTINGS.add("keywordsCreated");
		EXCLUDED_SETTINGS.add("password");
	}

	public SafeBrowseConsumer()
	{
	}

	@Override
	public void consume(Report r, FormattedReport fr)
	{
		for (String log: r.logs)
		{
			Matcher m = invokeRegex.matcher(log);
			while ( m.find() )
			{
				String match = log.substring( m.start(), m.end() );
				String[] tokens = match.split(" ");

				InvokeTarget it = new InvokeTarget( TextUtils.removeQuotes(tokens[2]) );
				it.uri = TextUtils.removeQuotes(tokens[5]);
				fr.invokeTargets.add(it);
			}

			List<String> result = TextUtils.getValues("addToHomeScreen", log);
			for (String query: result)
			{
				if ( !query.startsWith("Uri") && !query.startsWith("ASCII") )
				{
					String[] tokens = query.split(" (?=(([^'\"]*['\"]){2})*[^'\"]*$)");

					SafeBrowseShortcut sbs = new SafeBrowseShortcut( TextUtils.removeQuotes(tokens[0]), TextUtils.removeQuotes(tokens[3]) );
					m_homescreens.add(sbs);
				}
			}
		}

		for (String asset: r.assets)
		{
			if ( asset.endsWith("database.db") )
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


	public class SafeBrowseShortcut
	{
		public String name = new String();
		public String uri;

		public SafeBrowseShortcut(String name, String uri)
		{
			super();
			this.name = name;
			this.uri = uri;
		}
	}


	@Override
	public void save(FormattedReport fr)
	{
		try {
			PreparedStatement ps;

			if ( !m_homescreens.isEmpty() )
			{
				ps = m_connection.prepareStatement( "INSERT OR IGNORE INTO safe_browse_homescreen (report_id,uri,name) VALUES (?,?,?)");
				for (SafeBrowseShortcut qb: m_homescreens)
				{
					int i = 0;
					ps.setLong(++i, fr.id);
					ps.setString(++i, qb.uri);
					ps.setString(++i, qb.name);
					ps.executeUpdate();
				}
			}

			if (m_databasePath != null)
			{
				m_connection.setAutoCommit(true);
				ps = m_connection.prepareStatement("ATTACH DATABASE '"+m_databasePath+"' AS 'source'");
				ps.execute();

				ps = m_connection.prepareStatement("INSERT OR IGNORE INTO controlled SELECT "+fr.id+",uri FROM source.controlled");
				ps.execute();

				ps = m_connection.prepareStatement("INSERT OR IGNORE INTO keywords SELECT "+fr.id+",term FROM source.keywords");
				ps.execute();

				ps = m_connection.prepareStatement("INSERT OR IGNORE INTO passive SELECT "+fr.id+",uri FROM source.passive");
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


	@Override
	public void close() throws SQLException
	{
		m_connection.close();
	}

	@Override
	public Connection getConnection()
	{
		return m_connection;
	}
}