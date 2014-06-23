package com.canadainc.intelligence.client;

import com.canadainc.intelligence.model.Report;

public interface Consumer
{
	public void consume(Report r);
	
	public String consumeSetting(String key, String value);
}