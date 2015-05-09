package com.weisong.soa.proxy.engine;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.weisong.soa.proxy.RequestContext;
import com.weisong.soa.proxy.connection.ConnectionManager;
import com.weisong.soa.proxy.mgmt.ProxyCallModule;
import com.weisong.soa.proxy.util.ProxyUtil;
import com.weisong.soa.service.ServiceConst;
import com.weisong.soa.service.ServiceDescriptor;
import com.weisong.soa.util.GenericUtil;

public class ProxyEngine {

	final private Logger logger = LoggerFactory.getLogger(getClass());

	// Shutdown flag
	private boolean shutdown;
	
	@Autowired @Getter
	private ConnectionManager connMgr;
	
	// Scheduling executor
	private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);
	
	// Request map
	private Map<String, RequestContext> requestContextMap = new ConcurrentHashMap<>();

	@Autowired private ProxyCallModule proxyCallModule;
	
	public ProxyEngine() {
		Runtime.getRuntime().addShutdownHook(new ShutdownHook());
	}
	
	@PostConstruct
	public void start() {
		connMgr.start();
	}

	private class ShutdownHook extends Thread {
		public void run() {
			
			shutdown = true;
			
			System.out.println(String.format(
					"[%s] Shutdown initiated", 
					ProxyUtil.df.format(new Date()), requestContextMap.size()));
			
			long waitTime = 5000;
			long shutdownTime = System.currentTimeMillis() + waitTime;
			while(requestContextMap.isEmpty() == false) {
				
				System.out.println(String.format(
						"[%s] Waiting for %d pending requests to complete ...", 
						ProxyUtil.df.format(new Date()), requestContextMap.size()));
				
				if(System.currentTimeMillis() > shutdownTime) {
					break;
				}
				
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			int size = requestContextMap.size();
			if(size <= 0) {
				System.out.println(String.format(
					"[%s] Completed all pending requests, shutdown gracefully!", 
					ProxyUtil.df.format(new Date())));
			}
			else {
				System.out.println(String.format(
					"[%s] Forcefully shutdown after %d ms, dropping %d pending requests!", 
					ProxyUtil.df.format(new Date()), waitTime, size));
			}

			connMgr.stop();
			
		}
	}
	
	static public class TimeoutTask implements Runnable {

		final private Logger logger = LoggerFactory.getLogger(getClass().getName());

		private RequestContext ctx;
		
		public TimeoutTask(RequestContext ctx) {
			this.ctx = ctx;
		}
		
		@Override
		public void run() {
			if(ctx.reqeustContextMap.remove(ctx.getId()) != null) {
				ctx.targetCompletionTime = System.nanoTime();
				logger.debug(String.format("TimerTask removed context %s", ctx.getId()));
				ProxyUtil.sendErrorAndLogAccess(ctx, HttpResponseStatus.GATEWAY_TIMEOUT, "Timed out");
			}
		}
		
	}
	
	@PostConstruct
	public void init() {
		ProxyUtil.proxyCallModule = proxyCallModule;
	}
	
	public void connectedToClient(Channel channel) {
		if(shutdown) {
			logger.info(String.format("Shutting down, close connection from %s", 
					ProxyUtil.getRemoteConnString(channel)));
			channel.close();
		}
		else {
			String connString = ProxyUtil.getRemoteConnString(channel);
			logger.debug(String.format("Client connected: %s", connString));
		}
	}
	
	public void disconnectedFromClient(Channel channel) {
		String connString = ProxyUtil.getRemoteConnString(channel);
		logger.debug(String.format("Client disconnected: %s", connString));
	}
	
	public void receivedMessageFromClient(Channel clientChannel, DefaultFullHttpRequest request) {
		
		FullHttpRequest copiedRequest = request.copy();
		
		RequestContext ctx = null;
		String requestId = copiedRequest.headers().get(ServiceConst.HEADER_REQUEST_ID);
		try {
			logger.debug("Received request from client: " + requestId);
			if(shutdown) {
				ProxyUtil.sendErrorAndLogAccess(clientChannel, null, System.nanoTime(), 0L,
						requestId, copiedRequest.getUri(), copiedRequest.getMethod().name(),
						HttpResponseStatus.SERVICE_UNAVAILABLE, 
						"Proxy shutting down");
				return;
			}
			
			String domain = copiedRequest.headers().get(ServiceConst.HEADER_DOMAIN);
			String service = copiedRequest.headers().get(ServiceConst.HEADER_SERVICE_NAME);
			String version = copiedRequest.headers().get(ServiceConst.HEADER_SERVICE_VERSION);
			if(GenericUtil.isAnyEmpty(requestId, domain, service, version)) {
				ProxyUtil.sendErrorAndLogAccess(clientChannel, null, System.nanoTime(), 0L,
						requestId, copiedRequest.getUri(), copiedRequest.getMethod().name(),
						HttpResponseStatus.BAD_REQUEST, "Head incomplete");
				return;
			}

			ServiceDescriptor desc = new ServiceDescriptor(domain, service, version);
			ctx = new RequestContext(clientChannel, null, requestId, copiedRequest,  
					desc, 1000, requestContextMap);

			Channel serverChannel = connMgr.getConnection(ctx);
			if(serverChannel != null) {
				ctx.serverChannel = serverChannel;
				requestContextMap.put(ctx.getId(), ctx);
				ProxyUtil.setUserData(copiedRequest, ProxyUtil.getRemoteConnString(clientChannel));
				ctx.targetStartTime = System.nanoTime();
				ProxyUtil.sendMessage(serverChannel, copiedRequest);
				ctx.timeoutTaskFuture = executor.schedule(
						new TimeoutTask(ctx), ctx.timeout, TimeUnit.MILLISECONDS);
				logger.debug(String.format("Scheduled timeout task at %s", 
						new Date(System.currentTimeMillis() + ctx.timeout)));
				logger.debug("Forwarded request to server: " + requestId);
			}
			else {
				ctx.targetCompletionTime = System.nanoTime();
				ProxyUtil.sendErrorAndLogAccess(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE, 
						"Service not available");
			}
		} 
		catch (Throwable t) {
			ctx.targetCompletionTime = System.nanoTime();
			String connString = ProxyUtil.getRemoteConnString(clientChannel);
			logger.error(String.format("Failed to process request from %s: %s", connString, t.getMessage()));
			ProxyUtil.sendErrorAndLogAccess(clientChannel, null, System.nanoTime(), 0L, 
					requestId, copiedRequest.getUri(), copiedRequest.getMethod().name(), 
					HttpResponseStatus.INTERNAL_SERVER_ERROR, t.getMessage());
			if(ctx != null && ctx.timeoutTaskFuture != null) {
				ctx.timeoutTaskFuture.cancel(true);
				logger.debug(String.format("Cancelled timeout task"));
			}
		}
	}
	
	public void disconnectedFromServer(Channel channel) {
		connMgr.disconnected(channel);
	}
	
	public void receivedMessageFromServer(Channel serverChannel, DefaultFullHttpResponse response) {
		String requestId = null;
		RequestContext ctx = null;
		try {
			requestId = response.headers().get(ServiceConst.HEADER_REQUEST_ID);
			logger.debug("Received resposne from server: " + requestId);
			
			String clientConnId = ProxyUtil.getUserData(response);
			if(requestId == null || clientConnId == null) {
				logger.warn(String.format("Response from server %s contains "
						+ "neither x-request-id nor x-user-data, response dropped",
						ProxyUtil.getRemoteConnString(serverChannel)));
				return;
			}
			
			String ctxId = RequestContext.getContextId(clientConnId, serverChannel, requestId);
			ctx = requestContextMap.remove(ctxId);
			if(ctx == null) {
				logger.debug(String.format("Failed to find matching request for response %s from %s",
					requestId, ProxyUtil.getRemoteConnString(serverChannel)));
				return;
			}
			
			ctx.targetCompletionTime = System.nanoTime();
			
			FullHttpResponse r = response.copy();
			r.headers().remove(ServiceConst.HEADER_USER_DATA);
			
			ProxyUtil.sendMessage(ctx.clientChannel, r);
			ProxyUtil.printAccessLog(ctx, r.getStatus(), "Successfully proxied");
			logger.debug("Forwarded response to client: " + requestId);
		}
		catch (Throwable t) {
			if(ctx != null) {
				ctx.targetCompletionTime = System.nanoTime();
				ProxyUtil.sendErrorAndLogAccess(ctx,  
						HttpResponseStatus.INTERNAL_SERVER_ERROR, t.getMessage());
			}
		}
		finally {
			if(ctx != null && ctx.timeoutTaskFuture != null) {
				ctx.timeoutTaskFuture.cancel(true);
				logger.debug(String.format("Cancelled timeout task: %s", ctx.requestId));
			}
		}
	}
}
