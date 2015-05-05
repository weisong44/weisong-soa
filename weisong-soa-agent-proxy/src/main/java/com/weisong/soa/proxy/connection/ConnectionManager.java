package com.weisong.soa.proxy.connection;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;

import org.springframework.beans.factory.annotation.Autowired;

import com.weisong.soa.core.zk.config.ZkPropertyChangeRegistry;
import com.weisong.soa.core.zk.service.ZkServiceHandlerReadOnly;
import com.weisong.soa.proxy.RequestContext;
import com.weisong.soa.proxy.engine.ProxyEngine;
import com.weisong.soa.proxy.engine.ProxyNettyHandler;

public class ConnectionManager implements ConnectionPool.Listener {
	
	@Autowired private ProxyEngine engine;
	
	@Autowired private ZkPropertyChangeRegistry propsChangeRegistry;
	@Autowired private ZkServiceHandlerReadOnly zkHandler;
	
	// The client side network stack
	@Getter private Bootstrap bootstrap;
    private EventLoopGroup eventLoop;

    // address:port -> serviceConnectionManager
    private Map<String, ServiceConnectionManager> serviceConnMgrMap = new ConcurrentHashMap<>();
    
    private Map<Channel, ConnectionPool> channelToPoolMap = new ConcurrentHashMap<>();
    
	public ConnectionManager() {
        this.eventLoop = new NioEventLoopGroup(10);
        this.bootstrap = new Bootstrap();
	}

	public void start() {
		this.bootstrap.group(eventLoop).channel(NioSocketChannel.class)
			.option(ChannelOption.TCP_NODELAY, true)
			.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast(
						//new LoggingHandler(LogLevel.DEBUG),
						new HttpClientCodec(),
						new HttpObjectAggregator(12800),
						new ProxyNettyHandler.ServerSide(engine));
				}});
	}
	
	public void stop() {
		eventLoop.shutdownGracefully();
	}
	
	public Channel getConnection(RequestContext ctx) 
			throws Exception {
		ServiceConnectionManager mgr = null;
		String key = getKey(ctx);
		synchronized (serviceConnMgrMap) {
			mgr = serviceConnMgrMap.get(key);
			if(mgr == null) {
				mgr = new ServiceConnectionManager(this, ctx.desc, propsChangeRegistry, engine, zkHandler);
				serviceConnMgrMap.put(key, mgr);
			}
		}
		return mgr.getConnection(ctx);
	}
	
	@Override
	public void connected(Channel channel, ConnectionPool pool) {
		channelToPoolMap.put(channel, pool);
	}

	@Override
	public void disconnected(Channel c, ConnectionPool pool) {
		// Ignore
	}
	
	public void disconnected(Channel channel) {
		ConnectionPool pool = channelToPoolMap.remove(channel);
		if(pool != null) {
			pool.connectionClosed(channel);
		}
	}
	
	private String getKey(RequestContext ctx) {
		return String.format("%s:%s:%s", ctx.desc.getDomain(), 
				ctx.desc.getService(), ctx.desc.getVersion());
	}
}
