package com.canadainc.intelligence.model;

import java.text.*;
import java.util.*;
import java.util.regex.*;

import com.canadainc.intelligence.client.Consumer;

public class ReportAnalyzer
{
	private Report m_report;
	private FormattedReport m_result;
	private static final Map<String, Pattern> PATTERNS = new HashMap<String, Pattern>();
	private static final DateFormat OS_CREATION_FORMAT = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ssz"); // OS Creation: 2014/02/09-15:22:47EST
	private static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
	private static Collection<String> EXCLUDED_SETTINGS = new HashSet<String>();
	private Map<String,Consumer> m_consumers = new HashMap<String,Consumer>();
	private Consumer m_consumer;
	
	static {
		EXCLUDED_SETTINGS.add("logUI");
		EXCLUDED_SETTINGS.add("purchasesRefreshed");
		EXCLUDED_SETTINGS.add("stopLogging");
		EXCLUDED_SETTINGS.add("logCard");
		EXCLUDED_SETTINGS.add("accountId");
		EXCLUDED_SETTINGS.add("promoted");
		EXCLUDED_SETTINGS.add("init");
	}

	public void setConsumers(Map<String,Consumer> consumers) {
		m_consumers = consumers;
	}

	public Report getReport() {
		return m_report;
	}

	public void setReport(Report report) {
		m_report = report;
	}

	public ReportAnalyzer()
	{
	}


	private static List<String> getValues(String key, String text, boolean sanitize)
	{
		if ( !PATTERNS.containsKey(key) )
		{
			String toCompile = sanitize ? key.replaceAll("(?=[]\\[+&|!(){}^\"~*?:\\\\-])", "\\\\") + " [^\n]+" : key;
			PATTERNS.put( key, Pattern.compile(toCompile, Pattern.CASE_INSENSITIVE) );
		}

		Matcher matcher = PATTERNS.get(key).matcher(text);
		List<String> results = new ArrayList<String>();

		while ( matcher.find() ) {
			results.add( text.substring( matcher.start()+key.length(), matcher.end() ).trim() );
		}

		return results;
	}
	
	
	private static List<String> getValues(String key, String text) {
		return getValues(key, text, true);
	}


	private void analyzeDeviceInfo()
	{
		String deviceInfo = m_report.deviceInfo;
		List<String> result = getValues("[app] =>", deviceInfo);
		
		if ( result.isEmpty() ) {
			result = getValues("applicationName:", deviceInfo);
		}
		
		if ( !result.isEmpty() ) {
			m_result.appInfo.name = result.get(0);
			m_consumer = m_consumers.get(m_result.appInfo.name);
		}

		try {
			result = getValues("OS Creation:", deviceInfo);
			
			if ( result.isEmpty() ) {
				result = getValues("OSCreation:", deviceInfo);
			}
			
			m_result.os.creationDate = OS_CREATION_FORMAT.parse( result.get(0) ).getTime();
		} catch (ParseException ex) {
			ex.printStackTrace();
		}
		
		result = getValues("os:", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.os.version = result.get(0);
		}
		
		result = getValues("memoryUsedByCurrentProcess:", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.appInfo.memoryUsage = Long.parseLong( result.get(0) );
		}

		m_result.userInfo.machine = getValues("Machine:", deviceInfo).get(0);
		m_result.userInfo.nodeName = getValues("NodeName:", deviceInfo).get(0);
	}


	private void analyzeLogs()
	{
		List<String> result;
		
		for (String log: m_report.logs)
		{
			result = getValues("Locale file name:", log);
			
			if ( !result.isEmpty() )
			{
				String locale = result.get(0);
				locale = locale.substring( 1, locale.length()-1 );
				locale = locale.substring( locale.indexOf("_")+1 );
				m_result.locale = locale;
			}
			
			result = getValues("onLoadAsyncResultData", log);
			DatabaseStat stat = null;
			for (String timing: result) // we traverse the timings because in case of error, there would not be a matching result list
			{
				String[] tokens = timing.split("\\s+");
				
				if ( tokens[0].equals("Result") ) {
					stat.elements = Integer.parseInt(tokens[2]);
					m_result.databaseStats.add(stat);
				} else {
					int index = Arrays.binarySearch(tokens, "took");

					if (index > -1)
					{
						stat = new DatabaseStat();
						stat.queryId = Integer.parseInt(tokens[index-1]);
						stat.duration = Integer.parseInt(tokens[index+1]);
					}
				}
			}

			List<String> latitudes = getValues("positionUpdated latitude", log);
			List<String> longitudes = getValues("positionUpdated longitude", log);
			for (int i = 0; i < latitudes.size(); i++)
			{
				Location l = new Location();
				l.latitude = Double.parseDouble( latitudes.get(i) );
				l.longitude = Double.parseDouble( longitudes.get(i) );
				m_result.locations.add(l);
			}
			
			result = getValues("UserEvent:", log);
			for (String event: result)
			{
				int lastSpaceIndex = event.lastIndexOf(" ");
				
				if (lastSpaceIndex == -1) {
					m_result.userEvents.add(event);
				} else {
					String rest = event.substring(lastSpaceIndex+1);
					
					if ( rest.matches("-?\\d+(\\.\\d+)?") ) {
						m_result.userEvents.add( event.substring(0, lastSpaceIndex) );
					} else {
						m_result.userEvents.add(event);
					}
				}
			}
			
			result = getValues("CURRENT ACCOUNT ID! >>>", log);
			for (String account: result)
			{
				String[] tokens = account.split(" (?=(([^'\"]*['\"]){2})*[^'\"]*$)");
				
				if (tokens.length > 3) {
					String addressField = tokens[3];
					addressField = addressField.substring( 1, addressField.length()-1 ); // without quotes
					
					if ( VALID_EMAIL_ADDRESS_REGEX.matcher(addressField).find() ) {
						m_result.userInfo.emails.add(addressField);
					}
				}
			}
			
			List<String> settings = getValues("getValueFor:", log);
			for (String setting: settings)
			{
				int separatorIndex = setting.indexOf(" ");
				String key = setting.substring(1, separatorIndex-1); // start quote and end quote
				String value = setting.substring(separatorIndex).trim();
				lookupSetting(key, value);
			}
		}
	}
	
	
	private void analyzeSettings()
	{
		String[] settings = m_report.settings.split("\n");
		
		for (String setting: settings)
		{
			int index = setting.indexOf("=");
			
			if (index > -1)
			{
				String key = setting.substring(0, index);
				String value = setting.substring(index+1);

				if ( !EXCLUDED_SETTINGS.contains(key) && !key.startsWith("tutorial") && !value.startsWith("@Variant") )
				{
					lookupSetting(key, value);
				} else if ( value.equals("init") ) {
					m_result.appInfo.installTimestamp = Long.parseLong(value);
				}
			}
		}
	}
	
	
	private void lookupSetting(String key, String value)
	{
		if (m_consumer != null)
		{
			value = m_consumer.consumeSetting(key, value);
			
			if (value != null) {
				m_result.appSettings.put(key, value);
			} // else reject
		} else { // accept everything
			m_result.appSettings.put(key, value);
		}
	}


	public FormattedReport analyze()
	{
		m_result = new FormattedReport();

		if ( !m_report.deviceInfo.isEmpty() ) {
			analyzeDeviceInfo();
		}

		if ( !m_report.logs.isEmpty() ) {
			analyzeLogs();
		}
		
		if ( !m_report.settings.isEmpty() ) {
			analyzeSettings();
		}

		return m_result;
	}
}