package com.canadainc.intelligence.controller;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.canadainc.intelligence.client.Consumer;
import com.canadainc.intelligence.model.AppWorldInfo;
import com.canadainc.intelligence.model.DatabaseStat;
import com.canadainc.intelligence.model.DeviceAppInfo;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.Location;
import com.canadainc.intelligence.model.Report;

public class ReportAnalyzer
{
	private Report m_report;
	private FormattedReport m_result;
	private static final Map<String, Pattern> PATTERNS = new HashMap<String, Pattern>();
	private static final DateFormat OS_CREATION_FORMAT = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ssz"); // OS Creation: 2014/02/09-15:22:47EST
	private static final DateFormat BOOT_TIME_FORMAT = new SimpleDateFormat("MMM dd HH:mm:ss z yyyy"); // Aug 21 16:18:37 EDT 2014
	private static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
	private static Collection<String> EXCLUDED_SETTINGS = new HashSet<String>();
	private static final String INSTALL_DATA = "dat::";
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
		EXCLUDED_SETTINGS.add("analytics_collected");
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


	public static List<String> getValues(String key, String text, boolean sanitize)
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
	
	
	public static List<String> getEqualValue(String key, String text)
	{
		if ( !PATTERNS.containsKey(key) ) {
			PATTERNS.put( key, Pattern.compile( key+"=[^\n]+", Pattern.CASE_INSENSITIVE) );
		}
		
		Matcher matcher = PATTERNS.get(key).matcher(text);
		List<String> results = new ArrayList<String>();

		while ( matcher.find() )
		{
			String match = text.substring( matcher.start()+key.length(), matcher.end() ).trim();
			String[] tokens = match.split("=");
			
			results.add( tokens[1] );
		}

		return results;
	}
	
	
	public static List<String> getValues(String key, String text) {
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
			m_result.memoryUsage = Long.parseLong( result.get(0) );
		}
		
		result = getValues("availableDeviceMemory:", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.availableMemory = Long.parseLong( result.get(0) );
		}
		
		result = getValues("BatteryChargingState:", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.batteryInfo.chargingState = Integer.parseInt( result.get(0) );
		}
		
		result = getValues("BatteryCycleCount:", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.batteryInfo.cycleCount = Integer.parseInt( result.get(0) );
		}
		
		result = getValues("BatteryLevel:", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.batteryInfo.level = Integer.parseInt( result.get(0) );
		}
		
		result = getValues("BatteryTemperature:", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.batteryInfo.temperature = Integer.parseInt( result.get(0) );
		}
		
		result = getValues("InternalDevice:", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.userInfo.internal = result.get(0).equals("1");
		}
		
		result = getEqualValue("bcm0", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.userInfo.network.bcm0 = result.get(0);
		}
		
		result = getEqualValue("bptp0", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.userInfo.network.bptp0 = result.get(0);
		}
		
		result = getEqualValue("msm0", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.userInfo.network.msm0 = result.get(0);
		}

		m_result.hardwareInfo.machine = getValues("Machine:", deviceInfo).get(0);
		m_result.hardwareInfo.modelName = getValues("ModelName:", deviceInfo).get(0);
		m_result.hardwareInfo.modelNumber = getValues("ModelNumber:", deviceInfo).get(0);
		m_result.hardwareInfo.physicalKeyboard = getValues("PhysicalKeyboard:", deviceInfo).get(0).equals("1");
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
			
			result = getValues("run AccountImporter::run()", log);
			for (String total: result) {
				m_result.totalAccounts += Integer.parseInt(total);
			}
			
			result = getValues("processAllConversations ==== TOTAL", log);
			for (String total: result)
			{
				int n = Integer.parseInt(total);
				m_result.conversationsFetched.add(n);
			}
			
			result = getValues("getResult Elements generated:", log);
			for (String total: result)
			{
				int n = Integer.parseInt(total);
				m_result.elementsFetched.add(n);
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
					m_result.installTimestamp = Long.parseLong(value);
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
	
	
	private void analyzeBootTime()
	{
		try {
			m_result.bootTime = BOOT_TIME_FORMAT.parse( m_report.bootTime ).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	
	private void analyzeIpData()
	{
		String[] data = m_report.ipData.split("\n");
		m_result.userInfo.network.ip = data[0].split("=")[1].trim();
		m_result.userInfo.network.host = data[1].split("=")[1].trim();
	}

	
	private void analyzeRemovedApps()
	{
		String[] data = m_report.removedApps.split("\n");
		data = Arrays.copyOfRange(data, 1, data.length);
		
		for (String app: data)
		{
			DeviceAppInfo dai = new DeviceAppInfo();
			int bbWorldStart = app.indexOf(INSTALL_DATA);
			
			if (bbWorldStart >= 0)
			{
				String appWorldData = app.substring(bbWorldStart);
				app = app.substring(0, bbWorldStart-1);
				int start = appWorldData.indexOf("{");
				int end = appWorldData.lastIndexOf("}");
				String awData = appWorldData.substring(start+1, end).trim();
				String regex = "\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*$)";
				awData = awData.replaceAll(regex, "");
				String[] awMetaData = awData.split(",");
				
				AppWorldInfo awi = new AppWorldInfo();
				
				for (String attribute: awMetaData)
				{
					String[] attributes = attribute.split(":(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
					String key = removeQuotes(attributes[0]);
					String value = removeQuotes(attributes[1]);
					
					if ( key.equals("contentID") ) {
						awi.contentId = Integer.parseInt(value);
					} else if ( key.equals("iconID") ) {
						awi.iconUri = value;
					} else if ( key.equals("id") ) {
						awi.id = Integer.parseInt(value);
					} else if ( key.equals("sku") ) {
						awi.sku = value;
					} else if ( key.equals("vendor") ) {
						awi.vendor = value;
					} else if ( key.equals("name") ) {
						awi.name = value;
					}
				}
				
				dai.appWorldInfo = awi;
			}

			String[] tokens = app.split(",");

			dai.packageName = tokens[0].split("::")[0];
			dai.packageVersion = tokens[1];
			m_result.removedApps.add(dai);
		}
	}
	
	
	private static String removeQuotes(String input) {
		return input.substring(1, input.length()-1);
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
		
		if ( !m_report.bootTime.isEmpty() ) {
			analyzeBootTime();
		}
		
		if ( !m_report.ipData.isEmpty() ) {
			analyzeIpData();
		}
		
		if ( !m_report.removedApps.isEmpty() ) {
			analyzeRemovedApps();
		}
		
		if (m_consumer != null) {
			m_consumer.consume(m_report);
		}

		return m_result;
	}
}