package com.weisong.soa.mgmt.receiver.opentsdb;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.weisong.soa.mgmt.receiver.kafka.KafkaLogReceiver;

public class OpenTsdbKafkaLogReceiverMain {
	
	@Configuration
	@Import({
	    KafkaLogReceiver.JavaConfig.class
	})
	static public class JavaConfig {
		@Bean 
		OpenTsdbLogWriter getOpenTsdbLogWriter() {
			String dbHost = System.getProperty("opentsdb.host");
			int dbPort = Integer.valueOf(System.getProperty("opentsdb.port"));
			OpenTsdbLogWriter writer = new OpenTsdbLogWriter(dbHost, dbPort);
			writer.start();
			return writer;
		}
	}
	
	static void printUsageAndExit() {
		System.out.println("Usage:\n"
			+ "    java " + OpenTsdbKafkaLogReceiverMain.class.getName() + " <kafka-host> <opentsdb-host>");
		System.exit(-1);
	}

	@SuppressWarnings("resource")
	static public void main(String[] args) {
		if(args.length != 2) {
			printUsageAndExit();
		}
		
		System.setProperty("kafka.host", args[0]);
		System.setProperty("opentsdb.host", args[1]);
		System.setProperty("opentsdb.port", "4242");
		
		new AnnotationConfigApplicationContext(JavaConfig.class);
	}
}
