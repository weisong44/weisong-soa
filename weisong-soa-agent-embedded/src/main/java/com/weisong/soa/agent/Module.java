package com.weisong.soa.agent;

import org.springframework.jmx.export.annotation.ManagedAttribute;

import com.weisong.soa.core.mgmt.ModuleReport;


public interface Module {
	@ManagedAttribute String getName();
	@ManagedAttribute String getType();
	@ManagedAttribute String getDescription();
	@ManagedAttribute String getPath();
	String getMBeanPath();
	ModuleReport createReport();
}
