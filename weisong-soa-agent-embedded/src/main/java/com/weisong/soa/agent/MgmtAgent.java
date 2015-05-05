package com.weisong.soa.agent;

import com.weisong.soa.core.mgmt.ModuleReport;

public interface MgmtAgent {
	void setReportingInterval(int interval);
	void register(Module module);
	void sendReport(ModuleReport report);
}
