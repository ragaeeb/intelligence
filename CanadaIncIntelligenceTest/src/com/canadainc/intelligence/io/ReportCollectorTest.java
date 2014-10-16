package com.canadainc.intelligence.io;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

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
			List<Report> reports = m_instance.run();
			assertEquals( 2, reports.size() );
			
			Report r = reports.get(0);
			assertEquals( Long.parseLong("1403455028364"), r.timestamp );
			assertTrue( !r.deviceInfo.isEmpty() );
			assertEquals( 2, r.logs.size() );
			assertTrue( !r.settings.isEmpty() );
			assertEquals( 1, r.assets.size() );
			assertEquals( "res/auto_block/1403455028364/database.db", r.assets.get(0) );
			
			r = reports.get(1);
			assertEquals( Long.parseLong("1406619880273"), r.timestamp );
			assertTrue( !r.deviceInfo.isEmpty() );
			assertEquals( 1, r.logs.size() );
			assertTrue( !r.settings.isEmpty() );
			assertTrue( !r.ipData.isEmpty() );
			assertEquals( 1, r.assets.size() );
			assertEquals( "res/auto_block/1406619880273/database.db", r.assets.get(0) );
		} catch (IOException e) {
			e.printStackTrace();
			fail("Failed!");
		}
	}
	
	
	@Test
	public void testRunMultiFolder() throws IOException
	{
		m_instance = new ReportCollector();
		Collection<String> folders = new HashSet<String>();
		folders.add("res/multi_folder");
		m_instance.setFolders(folders);
		
		Collection<Report> reports = m_instance.run();
	}
	
	
	@Test
	public void testRunSunnah10Folder()
	{
		m_instance = new ReportCollector();
		Collection<String> folders = new HashSet<String>();
		folders.add("res/sunnah10");
		m_instance.setFolders(folders);
		
		try {
			Collection<Report> reports = m_instance.run();
			assertEquals( 1, reports.size() );
			
			for (Report r: reports) {
				assertEquals( Long.parseLong("1408723806275"), r.timestamp );
				assertTrue( !r.deviceInfo.isEmpty() );
				assertEquals( 2, r.logs.size() );
				assertTrue( !r.settings.isEmpty() );
				assertTrue( !r.bootTime.isEmpty() );
				assertTrue( !r.ipData.isEmpty() );
				assertTrue( !r.removedApps.isEmpty() );
				assertEquals( 1, r.assets.size() );
				assertEquals( "res/sunnah10/1408723806275/bookmarks.db", r.assets.get(0) );
			}
		} catch (IOException e) {
			e.printStackTrace();
			fail("Failed!");
		}
	}
}