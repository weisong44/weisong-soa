package com.weisong.soa.util;

public class ModulePathUtil {

	static public String getDomainName(String path) {
		return path.split("/")[0].split("=")[1];
	}
	
	static public String getAppName(String path) {
		return path.split("/")[2].split("=")[1];
	}

}
