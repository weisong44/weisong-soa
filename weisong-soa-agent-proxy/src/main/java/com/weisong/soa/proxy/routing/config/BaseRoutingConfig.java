package com.weisong.soa.proxy.routing.config;

import lombok.Getter;

import org.codehaus.jackson.annotate.JsonIgnore;

abstract public class BaseRoutingConfig<T extends BaseRoutingProc> {
	
	@Getter @JsonIgnore protected T proc;
	
	abstract protected void createProc();
	
	public BaseRoutingConfig() {
		createProc();
	}
}
