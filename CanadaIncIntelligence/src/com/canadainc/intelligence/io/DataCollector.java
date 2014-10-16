package com.canadainc.intelligence.io;

import java.io.FileFilter;
import java.util.Collection;

public interface DataCollector extends FileFilter
{
	public abstract void setFolders(Collection<String> folders);
}