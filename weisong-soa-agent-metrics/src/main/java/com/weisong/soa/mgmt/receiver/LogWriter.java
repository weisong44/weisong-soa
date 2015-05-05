package com.weisong.soa.mgmt.receiver;

import com.weisong.soa.core.mgmt.ModuleReport;


public interface LogWriter {
	void write(ModuleReport report) throws Exception;
}
