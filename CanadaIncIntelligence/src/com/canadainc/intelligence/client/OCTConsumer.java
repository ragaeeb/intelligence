package com.canadainc.intelligence.client;

import com.canadainc.intelligence.model.Report;

public class OCTConsumer implements Consumer
{
	public OCTConsumer()
	{
	}

	@Override
	public void consume(Report r)
	{
	}

	@Override
	public String consumeSetting(String key, String value) {
		return value;
	}
}