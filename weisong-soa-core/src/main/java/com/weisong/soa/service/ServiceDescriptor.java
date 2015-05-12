package com.weisong.soa.service;

import lombok.Data;

@Data
public class ServiceDescriptor {
	
	private String domain;
	private String service;
	private String version;
	
	public ServiceDescriptor(String domain, String service, String version) {
		this.domain = domain;
		this.service = service;
		this.version = version;
	}
}
