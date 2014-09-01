package com.canadainc.intelligence.client;

import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.Report;

public interface Consumer
{
	public void consume(Report r, FormattedReport result);
	
	public String consumeSetting(String key, String value, FormattedReport fr);
}