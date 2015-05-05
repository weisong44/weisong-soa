package com.weisong.soa.proxy.connection;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.weisong.soa.proxy.engine.ProxyEngine;

public class ConnectionPool {
	
	static public interface Listener {
		void connected(Channel c, ConnectionPool pool);
		void disconnected(Channel c, ConnectionPool pool);
	}
	
	final private Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	@Getter private String connStr;
	@Getter private String address;
	@Getter private int port;
	@Getter @Setter private int maxSize;
	@Getter @Setter private int checkingInterval = 1000; // in ms
	
	@Getter private boolean isRegistered;
	
	private ProxyEngine engine;
	private Timer timer = new Timer();
	private HousekeepingTask task = new HousekeepingTask();
	
	private int index;
	
	private List<Channel> connections = new ArrayList<>(5);
	
	private Set<Listener> listeners = new HashSet<>();
	
	public ConnectionPool(ProxyEngine engine, String connStr, int maxSize) {
		this.engine = engine;
		this.connStr = connStr;
		this.maxSize = maxSize;
		String[] tokens = connStr.split(":");
		address = tokens[0];
		port = Integer.valueOf(tokens[1]);
				
		int delay = new Random().nextInt(checkingInterval); 
		this.timer.schedule(task, delay, checkingInterval);
		
		logger.info(String.format("Connection pool for %s created", connStr)); 
	}
	
	public Channel getConnection() {
		synchronized (connections) {
			if(connections.isEmpty()) {
				return null;
			}
			index = ++index % connections.size();
			return connections.get(index);
		}
	}
	
	public void connectionClosed(Channel channel) {
		synchronized (connections) {
			// TODO: removal is O(N)
			if(connections.remove(channel)) {
				logger.info(String.format("Disconnected from server %s [%d]", 
						connStr, connections.size()));
				for(Listener l : listeners) {
					l.disconnected(channel, this);
				}
			}
		}
	}
	
	public void close() {
		task.shutdown = true;
		timer.cancel();
		synchronized (connections) {
			for(Channel c : connections) {
				c.close();
			}
			connections.clear();
		}
		logger.info(String.format("Connection pool for %s closed", connStr)); 
	}
	
	public int getSize() {
		return connections.size();
	}
	
	public void addListener(Listener listener) {
		listeners.add(listener);
	}
 	
	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}
 	
	public int getListeners() {
		return listeners.size();
	}
	
	public void setRegistered(boolean isRegistered) {
		if(this.isRegistered != isRegistered) {
			this.isRegistered = isRegistered;
			if(isRegistered) {
				logger.info(String.format(
					"Target %s is now registered, will connect", 
					connStr));
			}
			else {
				logger.info(String.format(
					"Target %s is now unregistered, will not connect", 
					connStr));
			}
		}
	}
 	
	private class HousekeepingTask extends TimerTask {
		private boolean shutdown = false;
		@Override
		public void run() {
			int delta = maxSize - connections.size();
			for(int i = 0; shutdown == false && i < delta; i++) {
				try {
					if(isRegistered == false) {
						break;
					}
					Bootstrap bootstrap = engine.getConnMgr().getBootstrap();
					ChannelFuture f = bootstrap.connect(address, port).sync();
					synchronized (connections) {
						connections.add(f.channel());
						for(Listener listener : listeners) {
							listener.connected(f.channel(), ConnectionPool.this);
						}
					}
					logger.info(String.format("Connected to server %s [%d]", connStr, connections.size()));
				}
				catch (Throwable t) {
					t.printStackTrace();
					logger.warn(String.format("Failed to connect to server %s, %s", 
							connStr, t.getMessage()));
					break;
				}
			}
		}
	}
	
}
