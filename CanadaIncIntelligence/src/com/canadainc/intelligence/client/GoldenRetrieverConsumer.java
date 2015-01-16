package com.canadainc.intelligence.client;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.canadainc.common.text.TextUtils;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.InAppSearch;
import com.canadainc.intelligence.model.Report;

public class GoldenRetrieverConsumer implements Consumer
{
	private static Collection<String> EXCLUDED_SETTINGS = new HashSet<String>();
	private static Pattern EMAIL_REGEX = Pattern.compile("([\\w\\-]([\\.\\w])+[\\w]+@([\\w\\-]+\\.)+[A-Za-z]{2,4})");
	private String m_databasePath;
	private Connection m_connection;

	static {
		EXCLUDED_SETTINGS.add("password");
		EXCLUDED_SETTINGS.add("subjectMoveTutorial");
		EXCLUDED_SETTINGS.add("subjectTutorial");
	}
	
	public GoldenRetrieverConsumer()
	{
	}

	@Override
	public void consume(Report r, FormattedReport fr)
	{
		for (String log: r.logs)
		{
			List<String> result = TextUtils.getValues("run RUNNING", log);
			for (String command: result)
			{
				command = command.substring( 1, command.length()-1 ).trim(); // without quotes
				String[] tokens = command.split(", (?=(([^'\"]*['\"]){2})*[^'\"]*$)");
				
				for (int i = tokens.length-1; i >= 0; i--) {
					tokens[i] = TextUtils.removeQuotes( tokens[i].trim() );
				}
				
				command = StringUtils.join(tokens, " ");
				
				InAppSearch ias = new InAppSearch("command", command);
				fr.inAppSearches.add(ias);
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
		} else if ( key.equals("whitelist") ) {
			Matcher m = EMAIL_REGEX.matcher(value);
			
			while ( m.find() )
			{
				String current = m.group(1);
				fr.userInfo.emails.add(current);
			}
			
			return null;
		} else {
			return value;
		}
	}

	@Override
	public void save(FormattedReport fr)
	{
		if (m_databasePath != null)
		{
			try {
				m_connection.setAutoCommit(true);
				PreparedStatement ps = m_connection.prepareStatement("ATTACH DATABASE '"+m_databasePath+"' AS 'source'");
				ps.execute();

				ps = m_connection.prepareStatement("INSERT OR IGNORE INTO logs SELECT "+fr.id+",command,reply,timestamp FROM source.logs");
				ps.execute();

				ps = m_connection.prepareStatement("DETACH DATABASE source");
				ps.execute();
				m_connection.setAutoCommit(false);
			} catch (SQLException ex) {
				ex.printStackTrace();
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