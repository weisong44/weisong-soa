package com.weisong.soa.agent.impl;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.weisong.soa.agent.MgmtAgent;
import com.weisong.soa.agent.Module;
import com.weisong.soa.agent.impl.DefaultMgmtAgentTest.JavaConfig;
import com.weisong.soa.core.mgmt.ModuleReport;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { JavaConfig.class })
public class DefaultMgmtAgentTest {

	static private boolean reportCreated = false;
	
	@Configuration
	@Import({ DefaultMgmtAgentJavaConfig.class })
	static class JavaConfig {
		@Autowired MainModule main;
		@Bean public MainModule mainModule() {
			return new MainModule();
		}
		@Bean public TestModule testModule() {
			return new TestModule(main);
		}
	}

	static public class TestModule extends BaseModule {
		
		public TestModule(Module parent) {
			super(parent);
		}

		@Override public String getType() {
			return "module";
		}
		
		@Override public String getName() {
			return "test";
		}

		@Override public String getDescription() {
			return "Test app";
		}

		@Override public ModuleReport createReport() {
			ModuleReport report = super.createReport();
			reportCreated = true;
			return report;
		}
	}
	
    @Autowired private MgmtAgent agent;
    @Autowired private TestModule testModule;
    
    @Test
    public void testCreateReport() throws Exception {
    	agent.setReportingInterval(1);
    	Thread.sleep(2000);
    	Assert.assertTrue(reportCreated);
    }
}
