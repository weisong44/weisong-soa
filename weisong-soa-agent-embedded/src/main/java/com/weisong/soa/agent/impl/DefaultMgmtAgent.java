package com.weisong.soa.agent.impl;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import lombok.Setter;

import org.apache.flume.clients.log4jappender.LoadBalancingLog4jAppender;
import org.apache.log4j.Appender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.weisong.soa.agent.MgmtAgent;
import com.weisong.soa.agent.Module;
import com.weisong.soa.core.mgmt.ModuleReport;
import com.weisong.soa.util.JsonUtil;

public class DefaultMgmtAgent implements MgmtAgent {

	protected Logger logger = LoggerFactory.getLogger(getClass());
	protected Logger monitorLogger = LoggerFactory.getLogger("monitor");
	
	@Setter	@Value("${monitor.agent.reporting.interval:30}")
	private int reportingInterval; // in seconds
	
	@Value("${monitor.agent.flume.hosts}")
	private String flumeHostAndPorts;
	
	@Value("${monitor.agent.log.file}")
	private String logFileName;
	
	@Value("${app.name}")
	private String appName;
	
	private Set<Module> modules = new HashSet<>();
	
	@PostConstruct
	private void startReportingThread() {
		logger.debug("Adding monitoring log appender");
		addMonitoringLogAppender();
		logger.debug("Starting reporting thread");
		new ReportingThread().start();
	}
	
	@Override
	public void register(Module module) {
		for(Module m : modules) {
			if(m.getPath().equals(module.getPath())) {
				RuntimeException ex = new RuntimeException(String.format(
					"Module reporter with path %s of class %s is already registered", 
					m.getPath(), m.getClass().getName()));
				logger.error("Failed to register report", ex);
				throw ex;
			}
		}
		modules.add(module);
		logger.info(String.format("Registered %s [%s] [total=%d]", 
			module.getPath(), module.getClass().getSimpleName(), modules.size()));
	}

	private void doReport() {
		long startTime = System.nanoTime();
		for(Module m : modules) {
			try {
				ModuleReport report = m.createReport();
				sendReport(report);
			}
			catch (Throwable ex) {
				ex.printStackTrace(System.err);
				logger.warn(String.format("Failed to send report for %s", m.getPath()));
			}
		}
		float totalTime = 0.000001f * (System.nanoTime() - startTime);
		logger.debug(String.format("Reporting completed in %.2f ms for %d modules", 
				totalTime, modules.size()));
	}

	private String convertToJson(ModuleReport report) {
		String json = JsonUtil.toJsonString(report)
				.replace("\n", "")
				.replace("\r", "");
			while(json.contains("  ")) {
				json = json.replace("  ", " ");
			}
			return json;
	}
	
	@Override
	public void sendReport(ModuleReport report) {
		if(report != null) {
			monitorLogger.info(convertToJson(report)); 
		}
	}
	
	private Appender createMonitoringLogAppender() throws Exception {
		if(flumeHostAndPorts.startsWith("$") == false) {
			LoadBalancingLog4jAppender appender = new LoadBalancingLog4jAppender();
			appender.setHosts(flumeHostAndPorts);
			appender.setUnsafeMode(true);
			appender.activateOptions();
			return appender;
		}
		else if(logFileName.startsWith("$") == false) {
			RollingFileAppender appender = new RollingFileAppender(
					new PatternLayout("%m%n"), logFileName);
			appender.setMaxFileSize("10240KB");
			appender.setMaxBackupIndex(1);
			return appender;
		}
		else {
			throw new RuntimeException(
				"Neither monitor.agent.log.file nor monitor.agent.flume.hosts is defined");
		}
	}
	
	private void addMonitoringLogAppender() {
		
		if("undefined".equals(logFileName)) {
			logFileName = String.format("/tmp/%s.log", appName);
		}
		
		try {
			org.apache.log4j.Logger mLogger = org.apache.log4j.Logger.getLogger("monitor");
			Appender appender = createMonitoringLogAppender();
			mLogger.setAdditivity(false);
			mLogger.addAppender(appender);
		} 
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private class ReportingThread extends Thread {
		public void run() {
			setName("soa-mgmt-agent");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			while(true) {
				try {
					doReport();
					Thread.sleep(1000 * reportingInterval);
				} catch (Throwable ex) {
					ex.printStackTrace();
				}
			}
		}
	}
}