package com.canadainc.intelligence.client;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.canadainc.common.text.TextUtils;
import com.canadainc.intelligence.client.QuranConsumer.QuranPlaylist;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.Report;

public class MessageTemplatesConsumer implements Consumer
{
	private Connection m_connection;
	private Map<String, String> m_templates = new HashMap<String, String>();
	
	@Override
	public void close() throws Exception
	{
		m_connection.close();
	}

	@Override
	public void consume(Report r, FormattedReport result)
	{
	}

	@Override
	public String consumeSetting(String key, String value, FormattedReport fr)
	{
		if ( key.equals("templates") )
		{
			List<String> all = TextUtils.extractQuotedStringValues(value);
			
			for (int i = 0; i+1 < all.size(); i+=2) {
				m_templates.put( all.get(i+1), all.get(i) );
			}
			
			return null;
		}
		
		return value;
	}

	@Override
	public Connection getConnection()
	{
		return m_connection;
	}

	Map<String, String> getTemplates()
	{
		return m_templates;
	}

	@Override
	public void save(FormattedReport fr)
	{
		try {
			PreparedStatement ps;
			
			if ( !m_templates.isEmpty() )
			{
				ps = m_connection.prepareStatement( "INSERT OR IGNORE INTO message_templates (report_id,name,message) VALUES (?,?,?)");
				for ( String key: m_templates.keySet() )
				{
					int i = 0;
					ps.setLong(++i, fr.id);
					ps.setString(++i, key);
					ps.setString( ++i, m_templates.get(key) );
					ps.executeUpdate();
				}
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