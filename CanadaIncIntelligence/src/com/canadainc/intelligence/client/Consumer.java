package com.canadainc.intelligence.client;

import com.canadainc.intelligence.io.PersistentBoundary;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.Report;

public interface Consumer extends PersistentBoundary
{
	public void consume(Report r, FormattedReport result);
	
	public String consumeSetting(String key, String value, FormattedReport fr);
	
	public void save(FormattedReport fr);
}