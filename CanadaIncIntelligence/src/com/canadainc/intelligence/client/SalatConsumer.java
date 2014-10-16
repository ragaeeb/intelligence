package com.canadainc.intelligence.client;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.Location;
import com.canadainc.intelligence.model.Report;

public class SalatConsumer implements Consumer
{
	private static Collection<String> EXCLUDED_SETTINGS = new HashSet<String>();
	private static final Map<String, Pattern> PATTERNS = new HashMap<String, Pattern>();

	static {
		EXCLUDED_SETTINGS.add("advertisedSalafyInk");
		EXCLUDED_SETTINGS.add("alFurqanAdvertised");
		EXCLUDED_SETTINGS.add("altitude");
		EXCLUDED_SETTINGS.add("angles");
		EXCLUDED_SETTINGS.add("athanPicked");
		EXCLUDED_SETTINGS.add("athanPrompted");
		EXCLUDED_SETTINGS.add("donateNotice");
		EXCLUDED_SETTINGS.add("lastUpdate");
	}

	public SalatConsumer()
	{
	}
	
	
	private String extract(Map<String, String> settings, String key)
	{
		String value = settings.get(key);
		return value == null ? "" : value;
	}
	

	@Override
	public void consume(Report r, FormattedReport fr)
	{
		Map<String, String> settings = fr.appSettings;
		
		if ( settings.containsKey("city") || settings.containsKey("location") || settings.containsKey("country") || settings.containsKey("latitude") || settings.containsKey("longitude") )
		{
			String city = extract(settings, "city");
			String country = extract(settings, "country");
			String name = extract(settings, "location");
			String latitude = settings.get("latitude");
			String longitude = settings.get("longitude");
			
			Location l = new Location();
			l.city = city;
			l.country = country;
			l.name = name;

			if ( latitude != null && !latitude.isEmpty() && !latitude.equals("nan") ) {
				l.latitude = Double.parseDouble(latitude);
			}

			if ( longitude != null && !longitude.isEmpty() && !longitude.equals("nan") ) {
				l.longitude = Double.parseDouble(longitude);
			}
			
			settings.remove("city");
			settings.remove("country");
			settings.remove("location");
			settings.remove("latitude");
			settings.remove("longitude");
			
			fr.locations.add(l);
		}
	}
	
	
	private static final void process(String setting, String key, String value, FormattedReport fr)
	{
		if ( !PATTERNS.containsKey(key) )
		{
			String toCompile = key+"\"\\s*,\\s*QVariant\\(\\w+,\\s+[a-zA-Z_0-9-]+";
			PATTERNS.put( key, Pattern.compile(toCompile, Pattern.CASE_INSENSITIVE) );
		}
		
		Pattern p = PATTERNS.get(key);
		Matcher m = p.matcher(value);
		while ( m.find() )
		{
			String result = value.substring( m.start(), m.end() );
			result = result.split(", ")[2];
			
			if ( result.equals("false") ) {
				result = "0";
			} else if ( result.equals("true") ) {
				result = "1";
			}
			
			fr.appSettings.put(setting+"_"+key, result);
		}
	}

	@Override
	public String consumeSetting(String key, String value, FormattedReport fr)
	{
		if ( EXCLUDED_SETTINGS.contains(key) ) {
			return null;
		} else if ( key.equals("adjustments") ) {
			process(key, "asr", value, fr);
			process(key, "dhuhr", value, fr);
			process(key, "sunrise", value, fr);
			process(key, "fajr", value, fr);
			process(key, "isha", value, fr);
			process(key, "maghrib", value, fr);
			process(key, "halfNight", value, fr);
			process(key, "lastThirdNight", value, fr);

			return null;
		} else if ( key.equals("notifications") || key.equals("athaans") ) {
			process(key, "asr", value, fr);
			process(key, "dhuhr", value, fr);
			process(key, "sunrise", value, fr);
			process(key, "fajr", value, fr);
			process(key, "isha", value, fr);
			process(key, "maghrib", value, fr);
			process(key, "halfNight", value, fr);
			process(key, "lastThirdNight", value, fr);

			return null;
		} else if ( key.equals("profiles") ) {
			for (int i = 1; i <= 5; i++) {
				process(key, String.valueOf(i), value, fr );
			}

			return null;
		} else {
			return value;
		}
	}

	@Override
	public void save(FormattedReport fr)
	{
	}

	@Override
	public void setPath(String path) throws Exception
	{
	}
	
	
	@Override
	public void close() throws SQLException
	{
	}

	@Override
	public Connection getConnection()
	{
		return null;
	}
}