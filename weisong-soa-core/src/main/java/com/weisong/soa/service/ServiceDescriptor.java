package com.weisong.soa.service;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ServiceDescriptor {
	
	private String domain;
	private String service;
	private String version;
	
	public ServiceDescriptor(String domain, String service, String version) {
		this.domain = domain;
		this.service = service;
		this.version = version;
	}
	
	@Override
	public String toString() {
		return String.format("%s-%s-%s", domain, service, version);
	}
}
