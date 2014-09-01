package com.canadainc.intelligence.client;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.canadainc.intelligence.controller.ReportAnalyzer;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.InAppSearch;
import com.canadainc.intelligence.model.Report;

public class GoldenRetrieverConsumer implements Consumer
{
	private static Collection<String> EXCLUDED_SETTINGS = new HashSet<String>();
	private static Pattern EMAIL_REGEX = Pattern.compile("([\\w\\-]([\\.\\w])+[\\w]+@([\\w\\-]+\\.)+[A-Za-z]{2,4})");

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
}