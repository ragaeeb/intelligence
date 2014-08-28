package com.canadainc.intelligence.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Report
{
	public String deviceInfo = new String();
	public Collection<String> logs = new ArrayList<String>();
	public String settings = new String();
	public long timestamp;
	public List<String> assets = new ArrayList<String>();
	public String ipData = new String();
	public String bootTime = new String();
	public String removedApps = new String();
}