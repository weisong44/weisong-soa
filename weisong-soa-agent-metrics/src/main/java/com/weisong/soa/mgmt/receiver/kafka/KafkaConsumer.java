package com.weisong.soa.mgmt.receiver.kafka;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;

public class KafkaConsumer {

	static public interface Listener {
		void onMessage(String message);
	}
	
	final private String clientId = "consumer-" + new Random().nextInt(1000000);
	final private int concurrency = 3;

	private String connectString;
	private String topic;
	private String consumerGroup;

	private ConsumerConnector consumerConnector;
	
    private ExecutorService executor;
        
	public KafkaConsumer(String host, int port, String topic,
			String consumerGroup) {
		this.connectString = host + ":" + port;
		this.topic = topic;
		this.consumerGroup = consumerGroup;
	    this.executor = Executors.newFixedThreadPool(concurrency);
	    
	    init();
	}
	
	private void init() {
		// Create the connector
		Properties properties = new Properties();
		properties.put("zookeeper.connect", connectString);
		properties.put("client.id", clientId);
		properties.put("group.id", consumerGroup);
        //properties.put("zookeeper.session.timeout.ms", "500");
        //properties.put("zookeeper.sync.time.ms", "250");
        //properties.put("auto.commit.interval.ms", "1000");
		consumerConnector = Consumer.createJavaConsumerConnector(
				new ConsumerConfig(properties));
	}

	@SuppressWarnings("rawtypes")
	public void start(final Listener listener) {
		
		// Create the message stream
        Map<String, Integer> topicCount = new HashMap<>();
        topicCount.put(topic, concurrency);
        Map<String, List<KafkaStream<byte[], byte[]>>> consumerStreams = 
        		consumerConnector.createMessageStreams(topicCount);
        List<KafkaStream<byte[], byte[]>> streams = consumerStreams.get(topic);

        // Start consuming
        for (final KafkaStream stream : streams) {
            executor.submit(new Runnable() {
				@SuppressWarnings("unchecked")
				@Override public void run() {
			        ConsumerIterator<byte[], byte[]> it = stream.iterator();
			        while (it.hasNext()) {
			            String message = new String(it.next().message());
			            listener.onMessage(message);
			        }
				}
            });
        }

        try { // without this wait the subsequent shutdown happens immediately before any messages are delivered
            Thread.sleep(3000);
        } catch (InterruptedException ie) {

        }
	}

	public void shutdown() {
        if (consumerConnector != null) {
            consumerConnector.shutdown();
            consumerConnector = null;
        }
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }				
	}
}
