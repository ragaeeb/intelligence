package com.canadainc.intelligence.client;

import com.canadainc.intelligence.model.Report;

public class QuranConsumer implements Consumer
{
	public QuranConsumer()
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