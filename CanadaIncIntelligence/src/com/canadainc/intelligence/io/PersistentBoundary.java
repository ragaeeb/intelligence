package com.canadainc.intelligence.io;

import java.sql.Connection;

public interface PersistentBoundary
{
	public void close() throws Exception;
	public void setPath(String path) throws Exception;
	public Connection getConnection();
}
