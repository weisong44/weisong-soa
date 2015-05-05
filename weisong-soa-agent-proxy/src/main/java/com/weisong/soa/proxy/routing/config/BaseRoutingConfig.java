package com.weisong.soa.proxy.routing.config;

import lombok.Getter;

import org.codehaus.jackson.annotate.JsonIgnore;

abstract public class BaseRoutingConfig {
	
	@Getter @JsonIgnore protected BaseRoutingProc proc;
	
	abstract protected void createProc();
	
	public BaseRoutingConfig() {
		createProc();
	}
}
