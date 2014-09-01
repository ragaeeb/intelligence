package com.canadainc.intelligence.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.canadainc.common.text.TextUtils;
import com.canadainc.intelligence.client.SunnahConsumer.SunnahShortcut;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.InAppSearch;
import com.canadainc.intelligence.model.Report;

public class OCTConsumer implements Consumer
{
	private static final String STOP_CODE_PATTERN = "stop_code\", QVariant(int, ";
	private static Collection<String> EXCLUDED_SETTINGS = new HashSet<String>();
	private List<OCTShortcut> m_homescreens = new ArrayList<OCTShortcut>();
	public List<OCTShortcut> getHomescreens() {
		return m_homescreens;
	}

	static {
		EXCLUDED_SETTINGS.add("dbVersion");
		EXCLUDED_SETTINGS.add("expiryDate");
		EXCLUDED_SETTINGS.add("hideAgreement");
		EXCLUDED_SETTINGS.add("lastUpdate");
	}
	
	public OCTConsumer()
	{
	}

	@Override
	public void consume(Report r, FormattedReport fr)
	{
		for (String log: r.logs)
		{
			List<String> result = TextUtils.getValues("performStopsQuery", log);
			
			for (String query: result)
			{
				query = query.substring( 1, query.length()-1 ); // without quotes
				
				InAppSearch ias = new InAppSearch("performStopsQuery", query);
				fr.inAppSearches.add(ias);
			}
			
			result = TextUtils.getValues("performRoutesQuery", log);
			
			for (String query: result)
			{
				String[] tokens = query.split(" ");
				query = tokens[0].substring( 1, tokens[0].length()-1 ); // without quotes

				InAppSearch ias = new InAppSearch("performRoutesQuery", query);
				fr.inAppSearches.add(ias);
			}
			
			result = TextUtils.getValues("performTripTimesQuery", log);
			
			for (String query: result)
			{
				String[] tokens = query.split(" ");
				query = tokens[0].substring( 1, tokens[0].length()-1 ); // without quotes
				
				InAppSearch ias = new InAppSearch("performTripTimesQuery", query);
				fr.inAppSearches.add(ias);
			}
			
			result = TextUtils.getValues("addToHomeScreen Adding shortcut", log);
			for (String query: result)
			{
				int start = query.indexOf(STOP_CODE_PATTERN);
				int end = query.indexOf(")", start);
				
				String stopCode = query.substring( start+STOP_CODE_PATTERN.length(), end );
				String[] tokens = query.split(" (?=(([^'\"]*['\"]){2})*[^'\"]*$)");
				String name = TextUtils.removeQuotes( tokens[tokens.length-1] );
				OCTShortcut s = new OCTShortcut( name, Integer.parseInt(stopCode) );
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
	
	public class OCTShortcut
	{
		public String name;
		public int stopCode;
		public OCTShortcut(String name, int stopCode) {
			super();
			this.name = name;
			this.stopCode = stopCode;
		}
	}
}