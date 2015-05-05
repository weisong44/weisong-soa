package com.weisong.soa.core.mgmt;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

public class ModuleReport {
	
	@Getter @Setter private String ipAddr; 
	@Getter @Setter private String hostname;
	@Getter @Setter private String path;
	@Getter @Setter private String timestamp;
	@Getter @Setter private Long interval;

	@Getter private String type;
	@Getter private String name;

	@Getter private Map<String, Object> properties = new HashMap<>(5);
	@Getter private Map<String, Number> metrics = new HashMap<>(5);
	@Getter private List<ModuleEvent> events = new LinkedList<>();

	protected ModuleReport() {
	}

	public ModuleReport(String type, String name) {
		this.type = type;
		this.name = name;
	}

	public ModuleReport addProperty(String key, Object value) {
		properties.put(key, value);
		return this;
	}

	public ModuleReport addMetric(String name, Number value) {
		metrics.put(name, value);
		return this;
	}

	public ModuleEvent addEvent(String type) {
		ModuleEvent event = new ModuleEvent(type);
		events.add(event);
		return event;
	}
	
}
