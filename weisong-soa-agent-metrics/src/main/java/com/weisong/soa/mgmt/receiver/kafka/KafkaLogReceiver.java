package com.weisong.soa.mgmt.receiver.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.weisong.common.javaconfig.CommonCombinedJavaConfig;
import com.weisong.soa.core.mgmt.ModuleReport;
import com.weisong.soa.mgmt.receiver.LogWriter;
import com.weisong.soa.mgmt.receiver.kafka.KafkaConsumer.Listener;
import com.weisong.soa.util.JsonUtil;

public class KafkaLogReceiver {

	@Configuration
	@Import({
	    CommonCombinedJavaConfig.class
	})
	public class JavaConfig {

		@Autowired private LogWriter writer;
		
		@Bean 
		KafkaLogReceiver getKafkaLogReceiver() {
			String kafkaHost = System.getProperty("kafka.host");
			KafkaLogReceiver receiver = new KafkaLogReceiver(kafkaHost, writer);
			receiver.start();
			return receiver;
		}
	}
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private String kafkaHost;
	private LogWriter writer;
	
	public KafkaLogReceiver(String kafkaHost, LogWriter writer) {
		this.kafkaHost = kafkaHost;
	}

	public void start() {
		KafkaConsumer consumer = new KafkaConsumer(kafkaHost, 2181, "logs", "log-receivers");
		consumer.start(new Listener() {
			@Override public void onMessage(String logMessage) {
				ModuleReport report = JsonUtil.toObject(logMessage, ModuleReport.class);
				if(report != null) {
					try {
						writer.write(report);
					}
					catch (Throwable e) {
						logger.warn(String.format("Failed to write %s, %s", 
							report.getPath(), e.getMessage()));
					}
				}
				else {
					logger.info("Failed to write message: " + logMessage);
				}
			}
		});
	}
	
}
