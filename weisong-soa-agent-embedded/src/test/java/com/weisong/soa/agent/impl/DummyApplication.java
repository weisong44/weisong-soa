package com.weisong.soa.agent.impl;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.weisong.soa.core.mgmt.ModuleReport;

public class DummyApplication {
	
	@Configuration
	static public class JavaConfig {
		@Autowired 
		private MainModule main;
		
		@Bean public WaveReporter waveReporter() {
			return new WaveReporter(main);
		}
	}
	
	@ManagedResource
	static public class WaveReporter extends BaseModule {

		private double degree;
		
		public WaveReporter(MainModule main) {
			super(main);
			new Thread() {
				@Override public void run() {
					while(true) {
						if(agent != null) {
							ModuleReport report = createReport();
							report.addEvent("DummyAppEvent")
								  .addProperty("time", new Date());
							agent.sendReport(report);
						}
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}.start();;
		}
		
		@Override
		public String getType() {
			return "module";
		}
		
		@Override
		public String getName() {
			return "wave";
		}

		@Override
		public String getDescription() {
			return "A module that generates sin/cos waves";
		}

		@Override
		public ModuleReport createReport() {
			degree = (degree + 1) % 360.0;
			double value = 2.0 * Math.PI * degree / 360;
			return createReport()
				.addMetric("sine", Math.sin(value))
				.addMetric("cosine", Math.cos(value));
		}
	}

	@SuppressWarnings("resource")
	static public void main(String[] args) {
		new AnnotationConfigApplicationContext(DefaultMgmtAgentJavaConfig.class, JavaConfig.class);
	}
}
