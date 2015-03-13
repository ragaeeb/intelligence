package com.canadainc.intelligence.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.canadainc.intelligence.client.Consumer;
import com.canadainc.intelligence.io.DatabaseBoundary;
import com.canadainc.intelligence.io.ReportCollector;
import com.canadainc.intelligence.io.UserCollector;
import com.canadainc.intelligence.model.FormattedReport;
import com.canadainc.intelligence.model.Report;
import com.canadainc.intelligence.model.UserData;
import com.maxmind.geoip.LookupService;

public class Application
{
	private LookupService m_ls;
	private ReportCollector m_collector;
	private UserCollector m_userCollector;
	private DatabaseBoundary m_db;
	private Map<String,String> m_consumers = new HashMap<String,String>();
	private Collection<String> m_excluded = new HashSet<String>();
	private List<Long> m_userInitiated = new ArrayList<Long>();

	private Application()
	{
	}
	
	
	public void display() throws Exception
	{
		m_db = new DatabaseBoundary("res/analytics.db");
		m_db.getOperatingSystems(0, null, null);
		m_db.getAppSettingValues("translation", "Quran10");
		m_db.getAppSettingValues("reciter", "Quran10");
	}
	
	
	public void collect() throws Exception
	{
		m_ls = new LookupService("res/geo/GeoLiteCity.dat", LookupService.GEOIP_MEMORY_CACHE);
		m_db = new DatabaseBoundary("res/analytics.db");

		m_collector = new ReportCollector();
		Collection<String> folders = new ArrayList<String>();
		folders.add("res/reports");
//		folders.add("res/autoblock");
//		folders.add("res/autoreply");
//		folders.add("res/exporter");
//		folders.add("res/golden_retriever");
//		folders.add("res/oct");
//		folders.add("res/oct10");
//		folders.add("res/quran10");
//		folders.add("res/salat10");
//		folders.add("res/sunnah10");
		m_collector.setFolders(folders);
		
		folders = new ArrayList<String>();
		//folders.add("res/canadainc");
		m_userCollector = new UserCollector();
		m_userCollector.setFolders(folders);

		m_consumers.put("AutoBlock", "com.canadainc.intelligence.client.AutoBlockConsumer");
		m_consumers.put("Golden Retriever", "com.canadainc.intelligence.client.GoldenRetrieverConsumer");
		m_consumers.put("OCT", "com.canadainc.intelligence.client.OCTConsumer");
		m_consumers.put("oct10", "com.canadainc.intelligence.client.OCTConsumer");
		m_consumers.put("Quran10", "com.canadainc.intelligence.client.QuranConsumer");
		m_consumers.put("Safe Browse", "com.canadainc.intelligence.client.SafeBrowseConsumer");
		m_consumers.put("Salat10", "com.canadainc.intelligence.client.SalatConsumer");
		m_consumers.put("Sunnah10", "com.canadainc.intelligence.client.SunnahConsumer");

		m_excluded.add("Ragaeeb7D");
		m_excluded.add("BLACKBERRY-D87D");
		
		process();
	}


	private void process() throws IOException
	{
		System.out.println("Starting...");
		List<Report> reports = m_collector.run();
		int n = reports.size();
		System.out.println("Collected "+n+" reports...");

		long start = System.currentTimeMillis();

		for (int i = 0; i < n; i++)
		{
			Report r = reports.get(i);
			//System.out.println( r.timestamp );

			if ( !r.deviceInfo.isEmpty() )
			{
				ReportAnalyzer ra = new ReportAnalyzer();
				ra.setLookupService(m_ls);
				ra.setConsumers(m_consumers);
				ra.setReport(r);

				FormattedReport fr = ra.analyze();

				if ( ra.isUserInitiated() ) {
					m_userInitiated.add(fr.id);
				}

				Collection<FormattedReport> formatted = new ArrayList<FormattedReport>();
				formatted.add(fr);

				if ( !m_excluded.contains(fr.userInfo.nodeName) )
				{
					m_db.enqueue(formatted);

					try {
						m_db.process();

						Consumer c = ra.getConsumer();

						if (c != null)
						{
							String value = m_consumers.get(fr.appInfo.name);
							value = value.substring( value.lastIndexOf(".")+1, value.lastIndexOf("Consumer") ).toLowerCase();

							c.setPath("res/"+value+".db");
							c.save(fr);
							c.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			if ( i == n/2 || i == n/3 ) {
				System.out.println(i+"/"+n+" reports processed...");
			}
		}
		
		Collection<UserData> users = m_userCollector.run();
		m_db.enqueueUserData(users);

		try {
			m_db.process();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		long end = System.currentTimeMillis();

		Collections.sort(m_userInitiated);

		System.out.println( m_userInitiated.toString() );
		System.out.println("Took "+(end-start)+" ms");
	}


	public static void main(String[] args)
	{
		try {
			Class.forName("org.sqlite.JDBC"); // load the sqlite-JDBC driver using the current class loader

			Application app = new Application();
			//app.collect();
			app.display();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}