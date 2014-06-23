package com.canadainc.intelligence.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormattedReport
{
	public AppInfo appInfo = new AppInfo();
	public Map<String,String> appSettings = new HashMap<String,String>();
	public List<DatabaseStat> databaseStats = new ArrayList<DatabaseStat>();
	public String locale = new String();
	public List<Location> locations = new ArrayList<Location>();
	public OperatingSystem os = new OperatingSystem();
	public List<String> userEvents = new ArrayList<String>();
	public UserInfo userInfo = new UserInfo();
}