package com.canadainc.intelligence;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.canadainc.intelligence.client.*;
import com.canadainc.intelligence.io.DatabaseBoundaryTest;
import com.canadainc.intelligence.io.ReportCollectorTest;
import com.canadainc.intelligence.model.ReportAnalyzerTest;

@RunWith(Suite.class)
@SuiteClasses({ AutoBlockConsumerTest.class, GoldenRetrieverConsumerTest.class,
		OCTConsumerTest.class, QuranConsumerTest.class,
		SafeBrowseConsumerTest.class, SalatConsumerTest.class,
		SunnahConsumerTest.class,
		DatabaseBoundaryTest.class, ReportCollectorTest.class, ReportAnalyzerTest.class })
public class AllTests
{
}