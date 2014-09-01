package com.canadainc.intelligence.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.canadainc.intelligence.client.InvokeTarget;

public class FormattedReport
{
	/** The app this report is being generated for. */
	public AppInfo appInfo = new AppInfo();
	
	public Map<String,String> appSettings = new HashMap<String,String>();
	public long availableMemory;
	public BatteryInfo batteryInfo = new BatteryInfo();
	public long bootTime;
	public List<DatabaseStat> databaseStats = new ArrayList<DatabaseStat>();
	public HardwareInfo hardwareInfo = new HardwareInfo();
	public long installTimestamp;
	public String locale = new String();
	public List<Location> locations = new ArrayList<Location>();
	public long memoryUsage;
	public int totalAccounts;
	public OperatingSystem os = new OperatingSystem();
	public List<DeviceAppInfo> removedApps = new ArrayList<DeviceAppInfo>();
	public List<String> userEvents = new ArrayList<String>();
	public UserInfo userInfo = new UserInfo();
	
	/** AccountsDropDown selection IDs. */
	public List<Integer> accountsSelected = new ArrayList<Integer>();
	
	public List<BulkOperation> bulkOperations = new ArrayList<BulkOperation>();
	
	/** The number of conversations fetched at a time. */
	public List<Integer> conversationsFetched = new ArrayList<Integer>();
	public List<Integer> elementsFetched = new ArrayList<Integer>();
	public List<InAppSearch> inAppSearches = new ArrayList<InAppSearch>();
	public List<InvokeTarget> invokeTargets = new ArrayList<InvokeTarget>();
	
	
	public class AppInfo
	{
		public String name;
		public String version;
		public String path;
	}
	
	
	public class BatteryInfo
	{
		public int chargingState;
		public int cycleCount;
		public int level;
		public int temperature;
	}
	
	
	public class HardwareInfo
	{
		public long deviceMemory;
		public String machine = new String();
		public String hardwareID = new String();
		public String modelName = new String();
		public boolean physicalKeyboard;
		public String modelNumber = new String();
	}
	
	
	public class NetworkInfo
	{
		public String bcm0 = new String();
		public String bptp0 = new String();
		public String msm0 = new String();
		public String ip = new String();
		public String host = new String();
	}
	
	
	public class OperatingSystem
	{
		public String version = new String();
		public long creationDate;
	}
	
	
	public class UserInfo
	{
		public List<String> emails = new ArrayList<String>();
		public String imei = new String();
		public String meid = new String();
		public String pin = new String();
		public String nodeName = new String();
		public NetworkInfo network = new NetworkInfo();
		public boolean internal = false;
	}
}