package com.canadainc.intelligence.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import com.canadainc.intelligence.model.UserData;

public class UserCollectorTest
{
	@Test
	public void testRun()
	{
		UserCollector instance = new UserCollector();
		Collection<String> folders = new HashSet<String>();
		folders.add("res/canadainc");
		instance.setFolders(folders);
		
		try {
			List<UserData> reports = instance.run();
			assertEquals( 2, reports.size() );
			
			assertEquals( "Ndaman", reports.get(0).name );
			assertEquals( "24f444c2", reports.get(0).pin );
			assertTrue( !reports.get(0).data.isEmpty() );
			
			assertEquals( "Rial Arizal", reports.get(1).name );
			assertEquals( "2b39693f", reports.get(1).pin );
			assertTrue( !reports.get(1).data.isEmpty() );
		} catch (IOException e) {
			e.printStackTrace();
			fail("Failed!");
		}
	}
}