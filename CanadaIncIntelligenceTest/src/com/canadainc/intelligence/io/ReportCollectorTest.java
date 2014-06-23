package com.canadainc.intelligence.io;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import com.canadainc.intelligence.model.Report;

public class ReportCollectorTest
{
	private ReportCollector m_instance;

	@Test
	public void testRunSingleReport()
	{
		m_instance = new ReportCollector();
		Collection<String> folders = new HashSet<String>();
		folders.add("res/single_report");
		m_instance.setFolders(folders);
		
		try {
			Collection<Report> reports = m_instance.run();
			assertEquals( 1, reports.size() );
			
			for (Report r: reports) {
				assertEquals( Long.parseLong("1399751585701"), r.timestamp );
				assertTrue( !r.deviceInfo.isEmpty() );
				assertTrue( !r.logs.isEmpty() );
				assertTrue( r.settings.isEmpty() );
			}
		} catch (IOException e) {
			e.printStackTrace();
			fail("Failed!");
		}
	}
	
	
	@Test
	public void testRunSingleFolder()
	{
		m_instance = new ReportCollector();
		Collection<String> folders = new HashSet<String>();
		folders.add("res/single_folder");
		m_instance.setFolders(folders);
		
		try {
			Collection<Report> reports = m_instance.run();
			assertEquals( 1, reports.size() );
			
			for (Report r: reports) {
				assertEquals( Long.parseLong("1402536925638"), r.timestamp );
				assertTrue( !r.deviceInfo.isEmpty() );
				assertTrue( !r.logs.isEmpty() );
				assertTrue( !r.settings.isEmpty() );
			}
		} catch (IOException e) {
			e.printStackTrace();
			fail("Failed!");
		}
	}
	
	
	@Test
	public void testRunAutoBlockFolder()
	{
		m_instance = new ReportCollector();
		Collection<String> folders = new HashSet<String>();
		folders.add("res/auto_block");
		m_instance.setFolders(folders);
		
		try {
			Collection<Report> reports = m_instance.run();
			assertEquals( 1, reports.size() );
			
			for (Report r: reports) {
				assertEquals( Long.parseLong("1403455028364"), r.timestamp );
				assertTrue( !r.deviceInfo.isEmpty() );
				assertEquals( 2, r.logs.size() );
				assertTrue( !r.settings.isEmpty() );
				assertEquals( 1, r.assets.size() );
				assertEquals( "res/auto_block/1403455028364/database.db", r.assets.get(0) );
			}
		} catch (IOException e) {
			e.printStackTrace();
			fail("Failed!");
		}
	}
}