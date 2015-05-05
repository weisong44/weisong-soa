package com.weisong.soa.agent.impl;

import static com.weisong.soa.agent.util.MgmtAgentUtil.key;
import static com.weisong.soa.agent.util.MgmtAgentUtil.tag;
import lombok.Setter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedOperation;

import com.weisong.common.ReadablePropertyPlaceholderConfigurer;
import com.weisong.soa.core.mgmt.ModuleReport;
import com.weisong.soa.util.HostUtil;
import com.weisong.soa.util.JsonUtil;

public class MainModule extends BaseModule {

    @Value("${rmi.port:1099}") protected int rmiPort;
    @Value("${app.domain:undefined}") @Setter protected String appDomain;
    @Value("${app.name:undefined}") @Setter protected String appName;
    @Value("${app.type:undefined}") @Setter protected String appType;
    @Value("${app.version:undefined}") @Setter protected String appVersion;
    @Value("${app.port:-1}") @Setter protected String appPort;
    @Value("${app.description:undefined}") @Setter protected String appDescription;
    
    @Autowired private ReadablePropertyPlaceholderConfigurer propertiesProvider;
    
    private long startTime = System.currentTimeMillis(); 
    
    public MainModule() {
    	super(null);
    }

	public String getDomain() {
		return appDomain;
	}
    
	@Override
	public String getType() {
		return appType;
	}

	@Override
	public String getDescription() {
		return appDescription;
	}

	@Override
	public String getName() {
		return appName;
	}
	
	@Override
	public String getPath() {
		return String.format("domain=%s/host=%s/%s=%s", 
				getDomain(), HostUtil.getHostIpAddress(), getType(), getName());
	}

	@Override
	public ModuleReport createReport() {
		ModuleReport report = super.createReport();
		report.addProperty("jmxPort", rmiPort);
		
		String[] tags = new String[] {
			tag("address", HostUtil.getHostIpAddress())
		};

		// Uptime
		long uptime = System.currentTimeMillis() - startTime;
		report.addMetric(key("uptime", tags), uptime);
		
		return report;
	}
	
	@ManagedOperation
	public String getSystemProperties() {
		return JsonUtil.toJsonString(System.getProperties());
	}
	
	@ManagedOperation
	public String getEnvironmentVariables() {
		return JsonUtil.toJsonString(System.getenv());
	}
	
	@ManagedOperation
	public String getProperties() {
		return JsonUtil.toJsonString(propertiesProvider.getProperties());
	}
	
}