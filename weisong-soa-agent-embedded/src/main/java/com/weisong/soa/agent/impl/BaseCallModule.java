package com.weisong.soa.agent.impl;

import static com.weisong.soa.agent.util.MgmtAgentUtil.key;
import static com.weisong.soa.agent.util.MgmtAgentUtil.tag;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.weisong.soa.agent.Module;
import com.weisong.soa.core.mgmt.ModuleReport;

abstract public class BaseCallModule extends BaseModule {

	static private class UriStats {
		private String address;
		private int port;
		private String uri;
		private String method;
		private int status;
		private long time;
	}

	static private class UriMetrics {
		// Metrics
		private Map<Integer, AtomicInteger> statusCounterMap = new ConcurrentHashMap<>();
		private Meter requestPerSecond;
		private Histogram responseTimeHistogram;
		
		private UriMetrics(MetricRegistry registry, UriStats stats) {
			String[] tags = new String[] {
				tag("address", stats.address), 
				tag("port", stats.port),
				tag("uri", stats.uri),
				tag("method", stats.method)
			};
			responseTimeHistogram = registry.histogram(key("response-time", tags)); 
			requestPerSecond = registry.meter(key("request", tags));
		}
		
		private void update(UriStats stats) {
			responseTimeHistogram.update(stats.time);
			requestPerSecond.mark();
			AtomicInteger counter = statusCounterMap.get(stats.status);
			if(counter == null) {
				counter = new AtomicInteger();
				statusCounterMap.put(stats.status, counter);
			}
			counter.incrementAndGet();
		}
	}
	
	private class Worker extends Thread {
		public void run() {
			setName("spark-metric-processor");
			while(true) {
				try {
					UriStats stats = queue.take();
					String key = String.format("%s|%d|%s|%s",  
						stats.address, stats.port, stats.uri, stats.method);
					UriMetrics metrics = metricsMap.get(key);
					if(metrics == null) {
						metrics = new UriMetrics(registry, stats);
						metricsMap.put(key, metrics);
					}
					metrics.update(stats);
				} catch (Throwable t) {
					t.printStackTrace(System.err);
				}
			}
		}
	}
	
	private BlockingQueue<UriStats> queue = new LinkedBlockingQueue<>();
	private MetricRegistry registry = new MetricRegistry();
	
	// uri => stats
	private Map<String, UriMetrics> metricsMap = new ConcurrentHashMap<>();
	
	public BaseCallModule(Module parent) {
		super(parent);
		new Worker().start();
	}

	@Override
	public ModuleReport createReport() {
		ModuleReport report = super.createReport();
		for(Map.Entry<String, UriMetrics> e : metricsMap.entrySet()) {
			String[] tokens = e.getKey().split("\\|");
			String[] tags = new String[] {
				tag("address", tokens[0]),
				tag("uri", tokens[2]),
				tag("method", tokens[3])
			};
			UriMetrics metrics = e.getValue();
			// Response time
			Snapshot snapshot = metrics.responseTimeHistogram.getSnapshot();
			report.addMetric(key("response-time-min",  tags), millis(snapshot.getMin()));
			report.addMetric(key("response-time-mean", tags), millis(snapshot.getMean()));
			report.addMetric(key("response-time-max",  tags), millis(snapshot.getMax()));
			report.addMetric(key("response-time-95",   tags), millis(snapshot.get95thPercentile()));
			report.addMetric(key("response-time-99",   tags), millis(snapshot.get99thPercentile()));
			// Status
			report.addMetric(key("request-per-second", tags), metrics.requestPerSecond.getMeanRate());
			for(Map.Entry<Integer, AtomicInteger> e2 : metrics.statusCounterMap.entrySet()) {
				int status = e2.getKey();
				AtomicInteger counter = e2.getValue();
				String[] statusTags = new String[] {
					tags[0], tags[1], tags[2], tag("status", status) 	
				};
				report.addMetric(key("request-count", statusTags), counter.intValue());
				counter.set(0);
			}
		}
		return report;
	}
	
	/**
	 * @param time in nanosecond
	 */
	public void requestCompleted(String address, int port, String uri, String method, int status, long time) {
		UriStats stats = new UriStats();
		stats.address = address;
		stats.port = port;
		stats.uri = uri;
		stats.method = method;
		stats.status = status;
		stats.time = time;
		queue.offer(stats);
	}

	private float millis(long time) {
		return 1.0f * time / 1000000;
	}

	private float millis(double time) {
		return (float) time / 1000000;
	}

}
