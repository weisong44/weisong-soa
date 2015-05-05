package com.weisong.soa.util;

public class GenericUtil {
	
	static public boolean isEmpty(String s) {
		return s == null || s.isEmpty();
	}
	
	static public boolean isAnyEmpty(String ... array) {
		for(String s : array) {
			if(isEmpty(s)) {
				return true;
			}
		}
		return false;
	}
	
	static public boolean isAllEmpty(String ... array) {
		for(String s : array) {
			if(isEmpty(s) == false) {
				return false;
			}
		}
		return true;
	}
	
}
