package com.canadainc.intelligence.controller;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.canadainc.common.text.TextUtils;
import com.canadainc.intelligence.client.Consumer;
import com.canadainc.intelligence.model.AppWorldInfo;
import com.canadainc.intelligence.model.DatabaseStat;
import com.canadainc.intelligence.model.DeviceAppInfo;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.Location;
import com.canadainc.intelligence.model.Report;
import com.maxmind.geoip.LookupService;

public class ReportAnalyzer
{
	private static final String EMAIL_ADDRESS_HEADER = "Email Address:";
	private static final String USER_DETAILS_HEADER = "Notes: UserEnteredReport: Name:";
	private Report m_report;
	private FormattedReport m_result;
	private static final DateFormat OS_CREATION_FORMAT = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ssz"); // OS Creation: 2014/02/09-15:22:47EST
	private static final DateFormat BOOT_TIME_FORMAT = new SimpleDateFormat("MMM dd HH:mm:ss z yyyy"); // Aug 21 16:18:37 EDT 2014
	public static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
	private static Collection<String> EXCLUDED_SETTINGS = new HashSet<String>();
	private static Collection<String> USER_EVENTS_TO_FIX = new HashSet<String>();
	private static final String INSTALL_DATA = "dat::";
	private LookupService m_ls;
	private Map<String,String> m_consumers = new HashMap<String,String>();
	private boolean m_userInitiated = false;
	
	boolean isUserInitiated()
	{
		return m_userInitiated;
	}


	static {
		EXCLUDED_SETTINGS.add("accountId");
		EXCLUDED_SETTINGS.add("adminMode");
		EXCLUDED_SETTINGS.add("alreadyReviewed");
		EXCLUDED_SETTINGS.add("analytics_collected");
		EXCLUDED_SETTINGS.add("hideAgreement");
		EXCLUDED_SETTINGS.add("init");
		EXCLUDED_SETTINGS.add("logCard");
		EXCLUDED_SETTINGS.add("logService");
		EXCLUDED_SETTINGS.add("logUI");
		EXCLUDED_SETTINGS.add("promoted");
		EXCLUDED_SETTINGS.add("purchasesRefreshed");
		EXCLUDED_SETTINGS.add("startLogging");
		EXCLUDED_SETTINGS.add("stopLogging");
		EXCLUDED_SETTINGS.add("unblockedSelf");
		
		USER_EVENTS_TO_FIX.add("Tab");
		USER_EVENTS_TO_FIX.add("Changed");
		USER_EVENTS_TO_FIX.add("Triggered");
		USER_EVENTS_TO_FIX.add("Page");
		USER_EVENTS_TO_FIX.add("Tapped");
		USER_EVENTS_TO_FIX.add("Selected");
	}

	public void setConsumers(Map<String,String> consumers) {
		m_consumers = consumers;
	}
	
	public void setLookupService(LookupService ls) {
		m_ls = ls;
	}

	public Report getReport() {
		return m_report;
	}

	public void setReport(Report report) {
		m_report = report;
	}
	
	public Consumer getConsumer() {
		return m_result.consumer;
	}

	public ReportAnalyzer()
	{
	}


	private void analyzeDeviceInfo()
	{
		String deviceInfo = m_report.deviceInfo;
		List<String> result = TextUtils.getValues("[app] =>", deviceInfo);
		
		if ( result.isEmpty() ) {
			result = TextUtils.getValues("applicationName:", deviceInfo);
		}
		
		if ( !result.isEmpty() )
		{
			m_result.appInfo.name = result.get(0);
			
			try {
				String consumer = m_consumers.get(m_result.appInfo.name);
				
				if (consumer != null)
				{
					Class<Consumer> c = (Class<Consumer>)Class.forName(consumer);
					m_result.consumer = (Consumer)c.newInstance();
				}
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		
		result = TextUtils.getValues("applicationVersion:", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.appInfo.version = result.get(0);
		}
		
		result = TextUtils.getValues("Notes:", deviceInfo);
		if ( !result.isEmpty() )
		{
			String notes = result.get(0);
			m_userInitiated = !notes.isEmpty() && !notes.equals("[canadainc_collect_analytics]");
		}
		
		if ( deviceInfo.startsWith(USER_DETAILS_HEADER) )
		{
			int startOfName = USER_DETAILS_HEADER.length()+1;
			int endOfName = deviceInfo.indexOf(EMAIL_ADDRESS_HEADER, startOfName+1);
			String name = deviceInfo.substring(startOfName, endOfName).trim();
			
			if ( !name.isEmpty() ) {
				m_result.userInfo.name = name;
			}
			
			int startOfEmail = deviceInfo.indexOf(EMAIL_ADDRESS_HEADER)+1;
			int endOfEmail = deviceInfo.indexOf("Summary of Bug:", startOfEmail+1);
			
			if (startOfEmail >= 0 && endOfEmail >= startOfEmail)
			{
				name = deviceInfo.substring( startOfEmail+EMAIL_ADDRESS_HEADER.length(), endOfEmail ).trim();
				
				if ( !name.isEmpty() && VALID_EMAIL_ADDRESS_REGEX.matcher(name).find() ) {
					m_result.userInfo.emails.add(name);
				}
			}
		}

		try {
			result = TextUtils.getValues("OS Creation:", deviceInfo);
			
			if ( result.isEmpty() ) {
				result = TextUtils.getValues("OSCreation:", deviceInfo);
			}
			
			m_result.os.creationDate = OS_CREATION_FORMAT.parse( result.get(0) ).getTime();
		} catch (ParseException ex) {
			ex.printStackTrace();
		}
		
		result = TextUtils.getValues("os:", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.os.version = result.get(0);
		}
		
		result = TextUtils.getValues("memoryUsedByCurrentProcess:", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.memoryUsage = Long.parseLong( result.get(0) );
		}
		
		result = TextUtils.getValues("totalDeviceMemory:", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.hardwareInfo.deviceMemory = Long.parseLong( result.get(0) );
		}
		
		result = TextUtils.getValues("HardwareID:", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.hardwareInfo.hardwareID = result.get(0);
		} else {
			result = TextUtils.getValues("Hardware ID:", deviceInfo);
			m_result.hardwareInfo.hardwareID = result.get(0);
		}
		
		result = TextUtils.getValues("availableDeviceMemory:", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.availableMemory = Long.parseLong( result.get(0) );
		}
		
		result = TextUtils.getValues("BatteryChargingState:", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.batteryInfo.chargingState = Integer.parseInt( result.get(0) );
		}
		
		result = TextUtils.getValues("BatteryCycleCount:", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.batteryInfo.cycleCount = Integer.parseInt( result.get(0) );
		}
		
		result = TextUtils.getValues("BatteryLevel:", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.batteryInfo.level = Integer.parseInt( result.get(0) );
		}
		
		result = TextUtils.getValues("BatteryTemperature:", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.batteryInfo.temperature = (int)Math.round( Double.parseDouble( result.get(0) ) );
		}
		
		result = TextUtils.getValues("InternalDevice:", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.userInfo.internal = result.get(0).equals("1");
		}
		
		result = TextUtils.getEqualValue("bcm0", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.network.bcm0 = result.get(0);
		}
		
		result = TextUtils.getEqualValue("bptp0", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.network.bptp0 = result.get(0);
		}
		
		result = TextUtils.getEqualValue("msm0", deviceInfo);
		if ( !result.isEmpty() ) {
			m_result.network.msm0 = result.get(0);
		}

		m_result.hardwareInfo.machine = TextUtils.getValues("Machine:", deviceInfo).get(0);
		m_result.hardwareInfo.modelName = TextUtils.getValues("ModelName:", deviceInfo).get(0);
		m_result.hardwareInfo.modelNumber = TextUtils.getValues("ModelNumber:", deviceInfo).get(0);
		m_result.hardwareInfo.physicalKeyboard = TextUtils.getValues("PhysicalKeyboard:", deviceInfo).get(0).equals("1");
		m_result.userInfo.nodeName = TextUtils.getValues("NodeName:", deviceInfo).get(0);
	}


	private void analyzeLogs()
	{
		List<String> result;
		
		for (String log: m_report.logs)
		{
			result = TextUtils.getValues("Locale file name:", log);
			
			if ( !result.isEmpty() )
			{
				String locale = result.get(0);
				locale = locale.substring( 1, locale.length()-1 );
				locale = locale.substring( locale.indexOf("_")+1 );
				m_result.locale = locale;
			}
			
			result = TextUtils.getValues("onLoadAsyncResultData", log);
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

			List<String> latitudes = TextUtils.getValues("positionUpdated latitude", log);
			List<String> longitudes = TextUtils.getValues("positionUpdated longitude", log);
			for (int i = 0; i < latitudes.size(); i++)
			{
				Location l = new Location();
				l.latitude = Double.parseDouble( latitudes.get(i) );
				l.longitude = Double.parseDouble( longitudes.get(i) );
				m_result.locations.add(l);
			}
			
			result = TextUtils.getValues("UserEvent:", log);
			for (String event: result)
			{
				String[] tokens = event.split(" ");
				
				if ( tokens.length > 1 && USER_EVENTS_TO_FIX.contains(tokens[1]) ) {
					tokens[0] += tokens[1];
				}
				
				m_result.userEvents.add(tokens[0]);
			}
			
			result = TextUtils.getValues("CURRENT ACCOUNT ID! >>>", log);
			result.addAll( TextUtils.getValues("AccountImporter.cpp 56", log) );
			
			for (String account: result)
			{
				String[] tokens = account.split(" (?=(([^'\"]*['\"]){2})*[^'\"]*$)");
				
				if (tokens.length > 3) {
					String addressField = tokens[3];
					parseEmail(addressField);
				}
			}
			
			result = TextUtils.getValues("AccountImporter.cpp 56", log);
			for (String account: result)
			{
				String[] tokens = account.split(" (?=(([^'\"]*['\"]){2})*[^'\"]*$)");
				
				if ( tokens.length > 5 && tokens[3].equals("\"imapemail\"") )
				{
					String addressField = tokens[tokens.length-1];
					parseEmail(addressField);
				}
			}
			
			result = TextUtils.getValues("199 \"pin\" \"PIN Messages\"", log);
			for (String account: result)
			{
				if ( !account.isEmpty() ) {
					m_result.userInfo.pin = TextUtils.removeQuotes(account);
				}
			}
			
			result = TextUtils.getValues("run AccountImporter::run()", log);
			for (String total: result) {
				m_result.totalAccounts += Integer.parseInt(total);
			}
			
			result = TextUtils.getValues("processAllConversations ==== TOTAL", log);
			for (String total: result)
			{
				int n = Integer.parseInt(total);
				m_result.conversationsFetched.add(n);
			}
			
			result = TextUtils.getValues("getResult Elements generated:", log);
			for (String total: result)
			{
				int n = Integer.parseInt(total);
				m_result.pimElementsFetched.add(n);
			}
			
			result = TextUtils.getValues("getValueFor:", log);
			for (String setting: result)
			{
				int separatorIndex = setting.indexOf(" ");
				String key = setting.substring(1, separatorIndex-1); // start quote and end quote
				String value = setting.substring(separatorIndex).trim();
				doLookup(key, value);
			}
			
			result = TextUtils.findMatch("getValueFor\\s+\"[^\n]+", log);
			for (String setting: result)
			{
				int indexOfFirstQuote = setting.indexOf("\"");
				int indexOfLastQuote = setting.indexOf("\"", indexOfFirstQuote+1);
				
				if (indexOfLastQuote > indexOfFirstQuote)
				{
					String key = setting.substring(indexOfFirstQuote+1, indexOfLastQuote);
					String value = setting.substring(indexOfLastQuote+1).trim();
					doLookup(key, value);
				}
			}
		}
	}

	private void parseEmail(String addressField)
	{
		addressField = TextUtils.removeQuotes(addressField); // without quotes
		
		if ( VALID_EMAIL_ADDRESS_REGEX.matcher(addressField).find() && !addressField.isEmpty() ) {
			m_result.userInfo.emails.add(addressField);
		}
	}
	
	
	private void doLookup(String key, String value)
	{
		if ( !EXCLUDED_SETTINGS.contains(key) && !key.startsWith("tutorial") && !value.startsWith("@Variant") && !key.matches("v\\d+.\\d+") && !m_result.appSettings.containsKey(key) ) // avoid lookups on UILogs when settings file already contains them
		{
			if (m_result.consumer != null)
			{
				value = m_result.consumer.consumeSetting(key, value, m_result);
				
				if (value != null)
				{
					if ( value.startsWith("QVariant(") ) { // sometimes happens for floats
						value = value.split(",")[1].trim();
						value = value.substring( 0, value.length()-1 );
					}
					
					m_result.appSettings.put(key, value);
				} // else reject
			} else { // accept everything
				m_result.appSettings.put(key, value);
			}
		} else if ( value.equals("init") ) {
			m_result.installTimestamp = Long.parseLong(value);
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
				
				if ( value.startsWith("\"") && value.endsWith("\"") ) { // certain settings that have commas get surrounded by quotes
					value = TextUtils.removeQuotes(value);
				}
				
				doLookup(key, value);
			}
		}
	}
	
	
	private void analyzeBootTime()
	{
		try {
			m_result.bootTime = BOOT_TIME_FORMAT.parse(m_report.bootTime).getTime();
		} catch (ParseException e) {
			//e.printStackTrace();
		}
	}
	
	
	private void analyzeIpData()
	{
		String[] data = m_report.ipData.split("\n");
		m_result.network.ip = data[0].split("=")[1].trim();
		m_result.network.host = data[1].split("=")[1].trim();
		
		if (m_ls != null)
		{
			com.maxmind.geoip.Location l = m_ls.getLocation(m_result.network.ip);
			
			if (l != null)
			{
				Location location = new Location();
				
				if (l.city != null) {
					location.city = l.city;
				}

				if (l.countryName != null) {
					location.country = l.countryName;
				}
				
				if ( l.region != null && !l.region.matches("\\d+") ) {
					location.region = l.region;
				}

				location.latitude = l.latitude;
				location.longitude = l.longitude;

				m_result.locations.add(location);
			}
		}
	}

	
	private void analyzeRemovedApps()
	{
		String[] data = m_report.removedApps.split("\n");
		data = Arrays.copyOfRange(data, 1, data.length);
		
		for (String app: data)
		{
			try {
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
						String key = TextUtils.removeQuotes(attributes[0]);
						String value = TextUtils.removeQuotes(attributes[1]);
						
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
			} catch (ArrayIndexOutOfBoundsException ex) {
				// ignore app
			}
		}
	}
	
	
	private void cleanUp()
	{
		m_result.userInfo.emails = new ArrayList<String>( new LinkedHashSet<String>(m_result.userInfo.emails) );
	}
	
	
	public FormattedReport analyze()
	{
		m_result = new FormattedReport(m_report.timestamp);

		if ( !m_report.deviceInfo.isEmpty() ) {
			analyzeDeviceInfo();
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

		if ( !m_report.logs.isEmpty() ) {
			analyzeLogs();
		}
		
		if (m_result.consumer != null) {
			m_result.consumer.consume(m_report, m_result);
		}
		
		cleanUp();

		return m_result;
	}
}