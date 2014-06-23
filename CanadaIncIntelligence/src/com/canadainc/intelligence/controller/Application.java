package com.canadainc.intelligence.controller;

import java.beans.DefaultPersistenceDelegate;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.canadainc.intelligence.io.ReportCollector;

public class Application
{
	private Application()
	{
	}
	

	public static void main(String[] args)
	{
		try {
			Properties prop = new Properties();
			FileInputStream in = new FileInputStream("config.properties");
			prop.load(in);
			in.close();
			
			System.out.println( prop.getProperty("database") );
			
			if (true) {
				return;
			}
			new ReportCollector().run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}