package com.canadainc.intelligence.client;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.canadainc.common.text.TextUtils;
import com.canadainc.intelligence.model.BulkOperation;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.InAppSearch;
import com.canadainc.intelligence.model.Report;

public class AutoBlockConsumer implements Consumer
{
	private final Pattern insertRegex = Pattern.compile("Starting query \"INSERT OR REPLACE INTO inbound_blacklist \\(address\\) VALUES[(?),]+", Pattern.CASE_INSENSITIVE);
	private final Pattern insertRegex2 = Pattern.compile("executePrepared \"INSERT OR REPLACE INTO inbound_blacklist \\(address\\) VALUES[(?),]+", Pattern.CASE_INSENSITIVE);
	private final Pattern invokeRegex = Pattern.compile("invoked\\s+QUrl[^\n]+", Pattern.CASE_INSENSITIVE);
	private static final String QDATE_TIME_START = "QDateTime(\"";
	private static final DateFormat QDATE_TIME_FORMAT = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy"); // Sat Jun 21 10:40:39 2014
	private static final String UNION_SELECT_KEYWORD = "UNION SELECT ?";
	
	public AutoBlockConsumer()
	{
	}

	@Override
	public void consume(Report r, FormattedReport result)
	{
		for (String log: r.logs)
		{
			Collection<Pattern> patterns = new ArrayList<Pattern>(2);
			patterns.add(insertRegex);
			patterns.add(insertRegex2);
			
			for (Pattern p: patterns)
			{
				Matcher m = p.matcher(log);
				
				while ( m.find() )
				{
					String lastToken = log.substring( m.start(), m.end() );
					lastToken = lastToken.substring( lastToken.lastIndexOf(" ")+1 );
					int count = lastToken.length() - lastToken.replace("?", "").length();
					
					BulkOperation bulk = new BulkOperation();
					bulk.type = "insert_inbound_blacklist";
					bulk.count = count;
					result.bulkOperations.add(bulk);
				}
			}
			
			Matcher m = invokeRegex.matcher(log);
			
			while ( m.find() )
			{
				String[] tokens = log.substring( m.start(), m.end() ).split(" ");
				String target = tokens[tokens.length-1].substring(1);
				target = target.substring( 0, target.length()-1 );
				InvokeTarget it = new InvokeTarget(target);
				result.invokeTargets.add(it);
			}
			
			List<String> matches = TextUtils.getValues("Starting query \"INSERT OR REPLACE INTO inbound_keywords (term)", log);
			for (String match: matches)
			{
				int start = match.indexOf(UNION_SELECT_KEYWORD);
				int end = match.indexOf("\"", start);
				match = match.substring(start, end);
				
				BulkOperation bulk = new BulkOperation();
				bulk.type = "insert_inbound_keyword";
				bulk.count = match.split("\\?").length;
				result.bulkOperations.add(bulk);
			}
			
			matches = TextUtils.getValues("executePrepared \"SELECT address,message,timestamp FROM logs WHERE address LIKE", log);
			for (String match: matches)
			{
				List<String> searchTerms = TextUtils.extractQuotedStringValues(match);
				
				if ( !searchTerms.isEmpty() )
				{
					InAppSearch ias = new InAppSearch( "search_logs", searchTerms.get(0) );
					result.inAppSearches.add(ias);
				}
			}
			
			matches = TextUtils.getValues("executePrepared \"SELECT address,count FROM inbound_blacklist WHERE address LIKE", log);
			for (String match: matches)
			{
				List<String> searchTerms = TextUtils.extractQuotedStringValues(match);
				
				if ( !searchTerms.isEmpty() )
				{
					InAppSearch ias = new InAppSearch( "search_inbound_blacklist", searchTerms.get(0) );
					result.inAppSearches.add(ias);
				}
			}
			
			matches = TextUtils.getValues("executePrepared \"SELECT term,count FROM inbound_keywords WHERE term LIKE", log);
			for (String match: matches)
			{
				List<String> searchTerms = TextUtils.extractQuotedStringValues(match);
				
				if ( !searchTerms.isEmpty() )
				{
					InAppSearch ias = new InAppSearch( "search_inbound_keywords", searchTerms.get(0) );
					result.inAppSearches.add(ias);
				}
			}
		}
	}

	@Override
	public String consumeSetting(String key, String value, FormattedReport fr)
	{
		if ( key.equals("accountId") ) {
			return null;
		} else if ( key.equals("autoblock_junk") ) {
			int start = value.indexOf(QDATE_TIME_START)+QDATE_TIME_START.length();
			int end = value.indexOf("\")", start);
			value = value.substring(start, end);

			try {
				value = ""+QDATE_TIME_FORMAT.parse(value).getTime();
			} catch (ParseException e) {
				return null;
			}
		}
		
		return value;
	}
}