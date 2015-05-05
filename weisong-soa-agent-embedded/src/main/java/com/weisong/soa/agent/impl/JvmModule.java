package com.weisong.soa.agent.impl;

import static com.weisong.soa.agent.util.MgmtAgentUtil.key;
import static com.weisong.soa.agent.util.MgmtAgentUtil.tag;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.CachedThreadStatesGaugeSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.weisong.soa.agent.Module;
import com.weisong.soa.core.mgmt.ModuleReport;
import com.weisong.soa.util.HostUtil;

public class JvmModule extends BaseModule {
	
	private Map<String, Metric> map = new HashMap<>();
	
	public JvmModule(Module parent) {
		super(parent);
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		map.put("buffer-pool", new BufferPoolMetricSet(server));
		map.put("thraed-state", new CachedThreadStatesGaugeSet(60, TimeUnit.SECONDS));
		map.put("classloader", new ClassLoadingGaugeSet());
		map.put("file-descriptor", new FileDescriptorRatioGauge());
		map.put("garbage-collection", new GarbageCollectorMetricSet());
		map.put("memory", new MemoryUsageGaugeSet());
	}
	
	@Override
	public String getName() {
		return "jvm-metrics";
	}

	@Override
	public String getType() {
		return "jvm-metrics";
	}

	@Override
	public String getDescription() {
		return "The JVM metrics";
	}

	@Override
	public ModuleReport createReport() {
		ModuleReport report = super.createReport();
		String addrTag = tag("address", HostUtil.getHostIpAddress());
		for(Map.Entry<String, Metric> e : map.entrySet()) {
			Metric value = e.getValue();
			if(value instanceof MetricSet) {
				String groupTag = tag("group", e.getKey());
				for(Map.Entry<String, Metric> e2 : ((MetricSet) value).getMetrics().entrySet()) {
					String name = e2.getKey();
					Metric metric = e2.getValue();
					if(metric instanceof Gauge) {
						handleGauge(report, name, metric, addrTag, groupTag);
					}
					else {
						System.out.println("Failed to include " + metric.getClass().getSimpleName());
					}
				}
			}
			else if(value instanceof Gauge) {
				handleGauge(report, e.getKey(), value, addrTag);
			}
			else {
				String name = e.getKey();
				System.out.println("Failed to include " + key(name, addrTag));
			}
			
		}
		return report;
	}
	
	private void handleGauge(ModuleReport report, String name, Object gauge, String ... tags) {
		Object value = ((Gauge<?>) gauge).getValue();
		if(value instanceof Number) {
			report.addMetric(key(name, tags), (Number) value); 
		}
		else if("deadlocks".equals(name) == false) {
			System.out.println("Failed to include " + key(name, tags));
		}
	}
}
