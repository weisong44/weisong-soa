package com.weisong.soa.mgmt.receiver.opentsdb;

import java.io.File;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.weisong.common.javaconfig.CommonCombinedJavaConfig;
import com.weisong.soa.core.mgmt.ModuleReport;
import com.weisong.soa.mgmt.receiver.LogWriter;
import com.weisong.soa.util.JsonUtil;

public class OpenTsdbLogFileReceiverMain {

	@Configuration
	@Import({ CommonCombinedJavaConfig.class })
	static public class JavaConfig {
		@Bean
		OpenTsdbLogWriter openTsdbLogWriter() {
			String dbHost = System.getProperty("opentsdb.host");
			int dbPort = Integer.valueOf(System.getProperty("opentsdb.port"));
			OpenTsdbLogWriter writer = new OpenTsdbLogWriter(dbHost, dbPort);
			writer.start();
			return writer;
		}
	}
	
	static void printUsageAndExit() {
		System.out.println("Usage:\n"
			+ "    java " + OpenTsdbLogFileReceiverMain.class.getName() 
			+ " <opentsdb-host> <log-file> [log-file] ...\n");
		System.exit(-1);
	}

	@SuppressWarnings("resource")
	static public void main(String[] args) throws Exception {
		if (args.length < 2) {
			printUsageAndExit();
		}

		String openTsdbHost = args[0];
		
		System.setProperty("opentsdb.host", openTsdbHost);
		System.setProperty("opentsdb.port", "4242");

		ApplicationContext ctx = new AnnotationConfigApplicationContext(JavaConfig.class);
		final LogWriter writer = ctx.getBean(LogWriter.class);

		for(int i = 1; i < args.length; i++) {
			String logFileName = args[i];
			final File f = new File(logFileName);
			Tailer.create(f, new TailerListenerAdapter() {
				@Override
			    public void fileNotFound() {
					System.out.println("Not found: " + f.getAbsolutePath());
			    }
				@Override
				public void handle(String line) {
					ModuleReport report = JsonUtil.toObject(line, ModuleReport.class);
					try {
						writer.write(report);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}
}
