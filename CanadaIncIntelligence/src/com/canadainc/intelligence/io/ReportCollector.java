package com.canadainc.intelligence.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.canadainc.common.io.IOUtils;
import com.canadainc.intelligence.model.Report;

public class ReportCollector implements DataCollector
{
	private static final String REPORT_REGEX = "^[0-9]*$";
	
	private Collection<String> m_folders;

	public ReportCollector()
	{
	}


	/* (non-Javadoc)
	 * @see com.canadainc.intelligence.io.DataCollector#setFolders(java.util.Collection)
	 */
	@Override
	public void setFolders(Collection<String> folders) {
		m_folders = folders;
	}

	
	public static Report extractReport(File f) throws IOException
	{
		Report r = new Report();

		String name = f.getName();
		int lastDot = name.lastIndexOf(".");
		
		if (lastDot > -1) {
			name = name.substring(0, lastDot);
		}

		r.timestamp = Long.parseLong(name);

		if ( f.isDirectory() )
		{
			File[] assets = f.listFiles();

			for (File asset: assets)
			{
				if ( asset.length() > 0 )
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
	

	public List<Report> run() throws IOException
	{
		List<Report> reports = new ArrayList<Report>();

		for (String folderPath: m_folders)
		{
			File folder = new File(folderPath);
			File[] listOfFiles = folder.listFiles(this);

			if (listOfFiles != null)
			{
				for (File f: listOfFiles) {
					reports.add( extractReport(f) );
				}
			}
		}
		
		return reports;
	}
	
	
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