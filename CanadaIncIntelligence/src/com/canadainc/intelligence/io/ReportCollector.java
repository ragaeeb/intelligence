package com.canadainc.intelligence.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.canadainc.common.io.IOUtils;
import com.canadainc.intelligence.model.Report;

public class ReportCollector implements DataCollector
{
	private static final String REPORT_REGEX = "^[0-9]*$";
	private static final String FILE_SEPARATOR = System.getProperty("file.separator");

	private Collection<String> m_folders;
	private boolean m_deleteOriginal;

	public ReportCollector(boolean deleteOriginal)
	{
		m_deleteOriginal = deleteOriginal;
	}
	
	
	public ReportCollector()
	{
		this(true);
	}


	/* (non-Javadoc)
	 * @see com.canadainc.intelligence.io.DataCollector#setFolders(java.util.Collection)
	 */
	@Override
	public void setFolders(Collection<String> folders) {
		m_folders = folders;
	}

	
	public static Report extractReport(File f) throws IOException {
		return extractReport(f, true);
	}
	

	public static Report extractReport(File f, boolean deleteOriginal) throws IOException
	{
		Report r = new Report();

		String original = f.getName();
		String name = original;
		int lastDot = name.lastIndexOf(".");

		if (lastDot > -1) {
			name = name.substring(0, lastDot);
		}

		r.timestamp = Long.parseLong(name);

		if ( f.isDirectory() )
		{
			processFolder(f,r);
		} else if ( original.endsWith(".zip") ) {
			String target = f.getParent()+FILE_SEPARATOR+name;
			unzip( f.getPath(), target );
			
			if (deleteOriginal) {
				f.deleteOnExit();
			}
			
			processFolder( new File(target), r );
		} else if ( original.endsWith(".txt") ) { // legacy text file
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


	private static void processFolder(File f, Report r) throws IOException
	{
		String name;
		File[] assets = f.listFiles();

		for (File asset: assets)
		{
			if ( asset.length() > 0 )
			{
				name = asset.getName();

				if ( name.equals("flags.conf") ) {
					r.flags = IOUtils.readFileUtf8(asset).trim();
				} else if ( name.endsWith(".conf") ) {
					r.settings = IOUtils.readFileUtf8(asset).trim();
				} else if ( name.endsWith(".log") || name.equals("slog2.txt") ) {
					r.logs.add( IOUtils.readFileUtf8(asset).trim() );
				} else if ( name.equals("deviceInfo.txt") ) {
					r.deviceInfo = IOUtils.readFileUtf8(asset).trim();
				} else if ( name.equals("boottime.txt") ) {
					r.bootTime = IOUtils.readFileUtf8(asset).trim();
				} else if ( name.equals("ip.txt") ) {
					r.ipData = IOUtils.readFileUtf8(asset).trim();
				} else if ( name.startsWith("removedapps") ) {
					r.removedApps = IOUtils.readFileUtf8(asset).trim();
				} else if ( name.equals("app_launch_data.txt") ) {
					r.appLaunchData = IOUtils.readFileUtf8(asset).trim();
				} else if ( name.equals("notes.txt") ) {
					r.notes = IOUtils.readFileUtf8(asset).trim();
				} else if ( name.equals("progress_manager.txt") ) {
					r.progressManager = IOUtils.readFileUtf8(asset).trim();
				} else {
					r.assets.add( asset.getPath() );
				}
			}
		}
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
					reports.add( extractReport(f, m_deleteOriginal) );
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
				return fileName.matches(REPORT_REGEX) && ( name.endsWith(".txt") || name.endsWith(".zip") );
			}
		}

		return false;
	}


	private static void unzip(String zipFile, String outputFolder){

		byte[] buffer = new byte[1024];

		try{

			//create output directory is not exists
			File folder = new File(".");
			if(!folder.exists()){
				folder.mkdir();
			}

			//get the zip file content
			ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
			//get the zipped file list entry
			ZipEntry ze = zis.getNextEntry();

			while(ze!=null){

				String fileName = ze.getName();
				File newFile = new File(outputFolder + File.separator + fileName);

				//create all non exists folders
				//else you will hit FileNotFoundException for compressed folder
				new File(newFile.getParent()).mkdirs();

				FileOutputStream fos = new FileOutputStream(newFile);             

				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}

				fos.close();   
				ze = zis.getNextEntry();
			}

			zis.closeEntry();
			zis.close();

		}catch(IOException ex){
			ex.printStackTrace(); 
		}
	}    
}