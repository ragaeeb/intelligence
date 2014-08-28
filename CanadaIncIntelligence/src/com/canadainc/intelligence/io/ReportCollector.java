package com.canadainc.intelligence.io;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import com.canadainc.common.io.IOUtils;
import com.canadainc.intelligence.model.Report;

public class ReportCollector
{
	private Collection<String> m_folders;

	public ReportCollector()
	{
	}


	public void setFolders(Collection<String> folders) {
		m_folders = folders;
	}

	
	public static Report extractReport(File f) throws IOException
	{
		Report r = new Report();

		String name = f.getName();
		int lastDot = name.lastIndexOf(".");
		
		if (lastDot > -1) {
			name = name.substring( 0, name.lastIndexOf(".") );
		}

		r.timestamp = Long.parseLong(name);

		if ( f.isDirectory() )
		{
			File[] assets = f.listFiles();

			for (File asset: assets)
			{
				name = asset.getName();

				if ( name.endsWith(".conf") ) {
					r.settings = IOUtils.readFileUtf8(asset).trim();
				} else if ( name.endsWith(".log") ) {
					r.logs.add( IOUtils.readFileUtf8(asset).trim() );
				} else if ( name.equals("deviceInfo.txt") ) {
					r.deviceInfo = IOUtils.readFileUtf8(asset).trim();
				} else if ( name.equals("boottime.txt") ) {
					r.bootTime = IOUtils.readFileUtf8(asset).trim();
				} else if ( name.equals("ip.txt") ) {
					r.ipData = IOUtils.readFileUtf8(asset).trim();
				} else if ( name.equals("removedapps") ) {
					r.removedApps = IOUtils.readFileUtf8(asset).trim();
				} else {
					r.assets.add( asset.getPath() );
				}
			}

		} else { // legacy text file
			String content = IOUtils.readFileUtf8(f);
			int logIndex = content.indexOf("[uilog]");

			if (logIndex > 0)
			{
				r.deviceInfo = content.substring(0, logIndex).trim();
				r.logs.add( content.substring( logIndex, content.length() ).trim() );
			}
		}
		
		return r;
	}
	

	public Collection<Report> run() throws IOException
	{
		Collection<Report> reports = new ArrayList<Report>();

		for (String folderPath: m_folders)
		{
			File folder = new File(folderPath);
			File[] listOfFiles = folder.listFiles( new ReportFilter() );

			for (File f: listOfFiles) {
				reports.add( extractReport(f) );
			}
		}
		
		return reports;
	}


	private class ReportFilter implements FileFilter
	{
		private static final String REPORT_REGEX = "^[0-9]*$";

		@Override
		public boolean accept(File path)
		{
			String name = path.getName();

			if ( path.isDirectory() ) {
				return name.matches(REPORT_REGEX);
			} else {
				int periodIndex = name.lastIndexOf(".");

				if (periodIndex > 0) {
					String fileName = name.substring(0, periodIndex);
					return fileName.matches(REPORT_REGEX) && name.endsWith(".txt");
				}
			}

			return false;
		}
	}
}