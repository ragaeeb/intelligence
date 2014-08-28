package com.canadainc.intelligence.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.canadainc.intelligence.controller.ReportAnalyzer;
import com.canadainc.intelligence.model.Report;

public class AutoBlockConsumer implements Consumer
{
	private final Pattern insertRegex = Pattern.compile("INSERT OR REPLACE INTO inbound_blacklist \\(address\\) VALUES[(?),]+", Pattern.CASE_INSENSITIVE);
	private final Pattern invokeRegex = Pattern.compile("invoked\\s+QUrl[^\n]+", Pattern.CASE_INSENSITIVE);
	
	public AutoBlockConsumer()
	{
	}

	@Override
	public void consume(Report r)
	{
		Collection<Integer> conversations = new ArrayList<Integer>();
		Collection<Long> accounts = new ArrayList<Long>();
		Collection<Integer> fetched = new ArrayList<Integer>();
		Collection<Integer> insertBulk = new ArrayList<Integer>();
		Collection<String> invokeTargets = new ArrayList<String>();
		
		for (String log: r.logs)
		{
			List<String> result = ReportAnalyzer.getValues("processAllConversations ==== TOTAL", log);

			for (String s: result) {
				conversations.add( Integer.parseInt(s) );
			}
			
			result = ReportAnalyzer.getValues("MessageImporter::run()", log);

			for (String s: result)
			{
				long accountId = Long.parseLong( s.substring( 0, s.indexOf(" ") ) );
				accounts.add(accountId);
			}
			
			result = ReportAnalyzer.getValues("getResult Elements generated:", log);

			for (String s: result) {
				fetched.add( Integer.parseInt(s) );
			}
			
			Matcher m = insertRegex.matcher(log);
			
			while ( m.find() )
			{
				String lastToken = log.substring( m.start(), m.end() );
				lastToken = lastToken.substring( lastToken.lastIndexOf(" ")+1 );
				int count = lastToken.length() - lastToken.replace("?", "").length();
				insertBulk.add(count);
			}
			
			m = invokeRegex.matcher(log);
			
			while ( m.find() )
			{
				String[] tokens = log.substring( m.start(), m.end() ).split(" ");
				String target = tokens[tokens.length-1].substring(1);
				target = target.substring( 0, target.length()-1 );
				invokeTargets.add(target);
			}
		}
	}

	@Override
	public String consumeSetting(String key, String value)
	{
		if ( key.equals("accountId") ) {
			return null;
		}
		
		return value;
	}
}