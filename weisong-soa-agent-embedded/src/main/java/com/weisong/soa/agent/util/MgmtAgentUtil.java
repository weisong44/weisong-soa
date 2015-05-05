package com.weisong.soa.agent.util;

public class MgmtAgentUtil {
	
	static public String tag(String name, Object value) {
		return new StringBuffer()
			.append('[')
			.append(name).append('=').append(value)
			.append(']')
			.toString();
	}
	
	static public String key(String metric, String ... tags) {
		StringBuffer sb = new StringBuffer();
		for(String tag : tags) {
			sb.append(tag);
		}
		return String.format("%s%s", metric, sb.toString());
	}

}
