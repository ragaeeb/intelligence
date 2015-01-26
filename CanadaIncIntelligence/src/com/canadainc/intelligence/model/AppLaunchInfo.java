package com.canadainc.intelligence.model;

public class AppLaunchInfo
{
	public String name;
	public LaunchType type;
	public double launcherSendStat;
	public double processCreatedStat;
	public double windowPostedStat;
	public double fullyVisibleStat;
	
	public enum LaunchType {
		App,
		Card,
		PooledApp,
		PooledCard
	}
}