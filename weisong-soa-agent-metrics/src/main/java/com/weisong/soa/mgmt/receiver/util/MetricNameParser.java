package com.weisong.soa.mgmt.receiver.util;

import java.util.HashMap;
import java.util.Map;

public class MetricNameParser {
	public class Result {
		public String name;
		public Map<String, String> tags = new HashMap<>();
	}
	
	/**
	 * request[address=192.168.1.96][port=4567][method=GET][status=600]
	 */
	public Result parse(String s) throws Exception {
		Result result = new Result();
		int i1 = 0, i2 = 0;
		// Metric name
		i2 = nextTagBegin(s, i1);
		if(i2 < 0) {
			result.name = s;
			return result;
		}
		else {
			result.name = s.substring(i1, i2);
		}
		
		while(i2 < s.length()) {
			i1 = nextTagBegin(s, i2) + 1;
			if(i1 < 0) {
				return result;
			}
			i2 = nextTagEnd(s, i1);
			if(i2 < 0) {
				throw new Exception("Parsing error");
			}
			String[] tokens = s.substring(i1, i2).split("=");
			result.tags.put(tokens[0], tokens[1]);
		}
		throw new Exception("Parsing error");
	}
	
	private int nextChar(String s, int pos, char c) {
		int i = pos;
		while(i < s.length()) {
			if(s.charAt(i) == c) {
				return i;
			}
			++i;
		}
		return Integer.MIN_VALUE;
	}
	
	private int nextTagBegin(String s, int pos) {
		return nextChar(s, pos, '[');
	}
	
	private int nextTagEnd(String s, int pos) {
		return nextChar(s, pos, ']');
	}
}
