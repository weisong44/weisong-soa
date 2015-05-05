package com.weisong.soa.proxy;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import com.weisong.soa.proxy.util.ProxyUtil;
import com.weisong.soa.service.ServiceDescriptor;


public class RequestContext {
	
	public long startTime = System.nanoTime();
	public long targetStartTime = System.nanoTime();
	public long targetCompletionTime = System.nanoTime();
	
	public long timeout = 100L;
	
	public String requestId;
	public FullHttpRequest request;
	public ServiceDescriptor desc; 
	
	public Channel clientChannel;
	public Channel serverChannel;
	
	public ScheduledFuture<?> timeoutTaskFuture;
	public Map<String, RequestContext> reqeustContextMap;

	static public String getContextId(Channel clientChannel, Channel serverChannel, String requestId) {
		String clientConnId = ProxyUtil.getRemoteConnString(clientChannel); 
		return getContextId(clientConnId, serverChannel, requestId);
	}

	static public String getContextId(String clientConnId, Channel serverChannel, String requestId) {
		StringBuffer sb = new StringBuffer();
		sb.append(clientConnId).append(" -> ")
		  .append(ProxyUtil.getRemoteConnString(serverChannel)).append(": ")
		  .append(requestId);
		return sb.toString();
	}
	
	public RequestContext(Channel clientChannel, Channel serverChannel, 
			String requestId, FullHttpRequest request, ServiceDescriptor desc,
			long timeout, Map<String, RequestContext> reqeustContextMap) {
		this.clientChannel = clientChannel;
		this.serverChannel = serverChannel;
		this.requestId = requestId;
		this.request = request;
		this.desc = desc;
		this.reqeustContextMap = reqeustContextMap;
		this.timeout = timeout;
	}

	public String getId() {
		return getContextId(clientChannel, serverChannel, requestId);
	}
}
