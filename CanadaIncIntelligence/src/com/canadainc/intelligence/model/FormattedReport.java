package com.canadainc.intelligence.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.canadainc.intelligence.client.Consumer;
import com.canadainc.intelligence.client.InvokeTarget;

public class FormattedReport
{
	/** The app this report is being generated for. */
	public AppInfo appInfo = new AppInfo();
	public List<AppLaunchInfo> appLaunches = new ArrayList<AppLaunchInfo>();
	public Map<String,String> appSettings = new HashMap<String,String>();
	public long availableMemory;
	public BatteryInfo batteryInfo = new BatteryInfo();
	public long bootTime;
	public List<BulkOperation> bulkOperations = new ArrayList<BulkOperation>();
	public Consumer consumer;
	/** The number of conversations fetched at a time. */
	public List<Integer> conversationsFetched = new ArrayList<Integer>();
	public List<DatabaseStat> databaseStats = new ArrayList<DatabaseStat>();
	public HardwareInfo hardwareInfo = new HardwareInfo();
	public long id;
	public List<InAppSearch> inAppSearches = new ArrayList<InAppSearch>();
	public long installTimestamp;
	
	public List<InvokeTarget> invokeTargets = new ArrayList<InvokeTarget>();
	
	public String locale = new String();
	public List<Location> locations = new ArrayList<Location>();
	public long memoryUsage;
	public NetworkInfo network = new NetworkInfo();
	public OperatingSystem os = new OperatingSystem();
	
	public List<Integer> pimElementsFetched = new ArrayList<Integer>();
	
	
	public List<DeviceAppInfo> removedApps = new ArrayList<DeviceAppInfo>();
	public Map<String, DownloadedApp> downloadedApps = new HashMap<String, DownloadedApp>();
	
	public int totalAccounts;
	
	
	public List<String> userEvents = new ArrayList<String>();
	
	
	public UserInfo userInfo = new UserInfo();
	
	
	public FormattedReport(long id) {
		this.id = id;
	}
	
	public class AppInfo
	{
		public String name;
		public String version;
		
		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof AppInfo) {
				AppInfo os = (AppInfo)obj;
				return name.equals(os.name) && version.equals(os.version);
			}
			
			return false;
		}
		@Override
		public int hashCode() {
			if (name == null) {
				System.out.println("*** NAME");
			} else if (version == null) {
				System.out.println("*** VERSION");
			}
			
			return name.hashCode()+version.hashCode();
		}
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
		public String hardwareID = new String();
		public String machine = new String();
		public String modelName = new String();
		public String modelNumber = new String();
		public boolean physicalKeyboard;
	}
	public class NetworkInfo
	{
		public String bcm0 = new String();
		public String bptp0 = new String();
		public String host = new String();
		public String ip = new String();
		public String msm0 = new String();
		
		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof NetworkInfo) {
				NetworkInfo os = (NetworkInfo)obj;
				return os.bcm0.equals(bcm0);
			}
			
			return false;
		}

		@Override
		public int hashCode() {
			return bcm0.hashCode();
		}
	}
	public class OperatingSystem
	{
		public long creationDate;
		public String version = new String();

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof OperatingSystem) {
				OperatingSystem os = (OperatingSystem)obj;
				return os.creationDate == creationDate && os.version.equals(version);
			}
			
			return false;
		}

		@Override
		public int hashCode() {
			return version.hashCode()+(int)creationDate;
		}
	}
}