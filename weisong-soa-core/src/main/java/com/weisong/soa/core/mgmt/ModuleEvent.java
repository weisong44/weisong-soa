package com.weisong.soa.core.mgmt;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

public class ModuleEvent {
	@Getter private String type;
	@Getter private Map<String, Object> properties = new HashMap<>();
	
	protected ModuleEvent() {
	}
	
	public ModuleEvent(String type) {
		this.type = type;
	}
	
	public ModuleEvent addProperty(String name, Object value) {
		properties.put(name, value);
		return this;
	}
}
