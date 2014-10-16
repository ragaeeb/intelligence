package com.canadainc.intelligence.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.canadainc.common.io.IOUtils;
import com.canadainc.intelligence.model.UserData;

public class UserCollector implements DataCollector
{
	private Collection<String> m_folders;
	private static final String REPORT_REGEX = "[a-zA-Z\\s]+\\([0-9a-fA-F]+\\)\\.txt";

	@Override
	public void setFolders(Collection<String> folders)
	{
		m_folders = folders;
	}
	
	
	public static UserData extractReport(File f) throws IOException
	{
		UserData r = new UserData();

		String name = f.getName();
		name = name.substring( 0, name.lastIndexOf(".") );
		
		int index = name.lastIndexOf("(");
		r.pin = name.substring( index+1, name.lastIndexOf(")") ).trim();
		r.name = name.substring(0, index).trim();
		r.data = IOUtils.readFileUtf8(f).trim();
		
		return r;
	}
	
	
	public List<UserData> run() throws IOException
	{
		List<UserData> reports = new ArrayList<UserData>();

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
		return name.matches(REPORT_REGEX);
	}
}