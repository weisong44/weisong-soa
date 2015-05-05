package com.weisong.soa.core.zk.bootstrap;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.weisong.common.javaconfig.CommonPropertyJavaConfig;
import com.weisong.soa.core.zk.DefaultZkClient;
import com.weisong.soa.core.zk.ZkClient;

public class PopulateBootstrapNodeMain {
	
	static private void printUsageAndExit() {
		StringBuffer sb = new StringBuffer();
		sb.append("Usage:\n");
		sb.append("    populate-bootstrap-node <node-data-file>\n");
		sb.append("Options:\n");
		sb.append("    node-data-file   The file that contains data\n");
		sb.append("                     to be populated in the bootstrap\n");
		sb.append("                     Zookeeper node\n");
		System.out.println(sb.toString());
		System.exit(0);
	}
	
	static public void main(String[] args) {
		
		if(args.length != 1) {
			printUsageAndExit();
		}
		
		String fname = args[0];

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
			JavaConfig.class);
		PropHolder props = ctx.getBean(PropHolder.class);
		
		System.out.println("Data file: " + fname);
		System.out.println("Zookeeper: " + props.bootstrapConnStr);
		System.out.println("Node path: " + props.bootstrapNodeData);
		
		try {
			String data = "";
			try(BufferedReader reader = new BufferedReader(new FileReader(fname))) {
				String line = null;
				while((line = reader.readLine()) != null) {
					data += line + "\n";
				}
				System.out.println(String.format("Read %d bytes", data.length()));
			}
			
			try(ZkClient zkClient = DefaultZkClient.create(props.bootstrapConnStr)) {
				// Create
				zkClient.create(props.bootstrapNodeData, data, CreateMode.PERSISTENT);
				System.out.println("Successfully created");
				// Verify
				String curData = zkClient.get(props.bootstrapNodeData);
				if(data.equals(curData)) {
					System.out.println("Successfully verified");
				}
				else {
					System.err.println("Verification failed!");
					System.err.println("");
					System.err.println("Data read from file:");
					System.err.println("===");
					System.err.println(data);
					System.err.println("===");
					System.err.println("Data read from ZK node:");
					System.err.println("===");
					System.err.println(curData);
					System.err.println("===");
				}
			}
		} 
		catch (FileNotFoundException e) {
			System.err.println(String.format("File not found: %s", fname));
		} 
		catch (IOException e) {
			System.err.println(String.format("Failed to read file %s: %s", 
					fname, e.getMessage()));
		} 
		catch (Exception e) {
			System.err.println(String.format("Failed to populate ZK node: %s", 
					fname, e.getMessage()));
		}
	}
	
	static public class PropHolder {
		@Value("${bootstrap.zk.connstr:localhost:2181}")
		private String bootstrapConnStr;
		@Value("${bootstrap.node.root:/bootstrap}")
		private String bootstrapNodeRoot;
		@Value("${bootstrap.node.data:/bootstrap/data}")
		private String bootstrapNodeData;
	}
	
	@Configuration
	@Import({
		CommonPropertyJavaConfig.class
	})
	static public class JavaConfig {
		@Bean
		public PropHolder getPropHolder() {
			return new PropHolder();
		}
	}
}
