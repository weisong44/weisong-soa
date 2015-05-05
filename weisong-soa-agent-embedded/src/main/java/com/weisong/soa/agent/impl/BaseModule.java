package com.weisong.soa.agent.impl;

import java.util.Date;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.weisong.soa.agent.MgmtAgent;
import com.weisong.soa.agent.Module;
import com.weisong.soa.core.mgmt.ModuleReport;
import com.weisong.soa.util.DateUtil;
import com.weisong.soa.util.HostUtil;
import com.weisong.soa.util.JsonUtil;

@ManagedResource
abstract public class BaseModule implements Module {

	@Value("${monitor.agent.reporting.interval:-1}")
	protected long interval;
	
	@Autowired protected MgmtAgent agent;
	
	protected Module parent;
	protected String path;
	protected String mBeanPath;
	
	public BaseModule(Module parent) {
		this.parent = parent;
		if(parent == null && this instanceof MainModule == false) {
			throw new RuntimeException("Parent can not be null!");
		}
	}
	
	@PostConstruct
	protected void register() throws Exception {
		agent.register(this);
	}
	
	@Override
	public String getPath() {
		if(path == null) {
			path = String.format("%s/%s=%s", parent.getPath(), getType(), getName());
		}
		return path;
	}

	@Override
	public String getMBeanPath() {
		if(mBeanPath == null) {
			String[] tokens = getPath().split("/");
			if(tokens.length < 3 ||
					tokens[0].startsWith("domain=") == false || 
					tokens[1].startsWith("host=") == false) {
				throw new RuntimeException("Incompatible path format!");
			}
			mBeanPath = tokens[0].split("=")[1] + ":";
			for(int i = 2; i < tokens.length; i++) {
				if(mBeanPath.endsWith(":") == false) {
					mBeanPath += ",";
				}
				mBeanPath += tokens[i];
			}
		}
		return mBeanPath;
	}

	@ManagedOperation
	public String createJsonReport() {
		return JsonUtil.toJsonString(createReport());
	}
	
	@ManagedOperation
	public void sendReport() {
		agent.sendReport(createReport());
	}
	
	@Override
	public ModuleReport createReport() {
		ModuleReport report = new ModuleReport(getType(), getName());
		report.setPath(getPath());
		report.setHostname(HostUtil.getHostname());
		report.setIpAddr(HostUtil.getHostIpAddress());
		report.setTimestamp(DateUtil.format(new Date()));
		report.setInterval(interval * 1000);
		return report;
	}

}
