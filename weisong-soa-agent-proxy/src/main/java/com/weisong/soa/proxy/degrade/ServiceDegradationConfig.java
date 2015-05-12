package com.weisong.soa.proxy.degrade;

import lombok.Getter;
import lombok.Setter;

public class ServiceDegradationConfig {
	
	@Getter @Setter private boolean circuitBreakerEnabled;
	//private Map<String, Fallback> fallbackMap = new ConcurrentHashMap<>();
	
	
}
