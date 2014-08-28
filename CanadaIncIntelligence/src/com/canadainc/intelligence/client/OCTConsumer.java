package com.canadainc.intelligence.client;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.canadainc.intelligence.controller.ReportAnalyzer;
import com.canadainc.intelligence.model.Report;

public class OCTConsumer implements Consumer
{
	private final Pattern stopNumbers = Pattern.compile("\\d{4}:", Pattern.CASE_INSENSITIVE);
	private Collection<String> m_stops = new HashSet<String>();
	
	public Collection<String> getStops() {
		return m_stops;
	}

	public OCTConsumer()
	{
	}

	@Override
	public void consume(Report r)
	{
		Collection<String> stopQueries = new HashSet<String>();
		Collection<String> routeQueries = new HashSet<String>();
		Collection<String> tripQueries = new HashSet<String>();
		
		for (String log: r.logs)
		{
			List<String> result = ReportAnalyzer.getValues("performStopsQuery", log);
			
			for (String query: result)
			{
				query = query.substring( 1, query.length()-1 ); // without quotes
				stopQueries.add(query);
			}
			
			result = ReportAnalyzer.getValues("performRoutesQuery", log);
			
			for (String query: result)
			{
				String[] tokens = query.split(" ");
				query = tokens[0].substring( 1, tokens[0].length()-1 ); // without quotes
				routeQueries.add(query);
			}
			
			result = ReportAnalyzer.getValues("performTripTimesQuery", log);
			
			for (String query: result)
			{
				String[] tokens = query.split(" ");
				query = tokens[0].substring( 1, tokens[0].length()-1 ); // without quotes
				tripQueries.add(query);
			}
		}
	}

	@Override
	public String consumeSetting(String key, String value)
	{
		if ( key.equals("bookmarks") )
		{
			Matcher m = stopNumbers.matcher(value);
			
			while ( m.find() ) {
				m_stops.add( value.substring( m.start(), m.end()-1 ) );
			}

			return null;
		} else if ( key.equals("expiryDate") ) {
			return null;
		}
		
		else {
			return value;
		}
	}
}