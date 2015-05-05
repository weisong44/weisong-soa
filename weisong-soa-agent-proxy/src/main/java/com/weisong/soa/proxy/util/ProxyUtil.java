package com.weisong.soa.proxy.util;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.weisong.soa.proxy.RequestContext;
import com.weisong.soa.proxy.mgmt.ProxyCallModule;
import com.weisong.soa.service.ServiceConst;

public class ProxyUtil {
	
	final static public String ACCESS_LOGGER = "proxy.access.logger";
	
	final static private Logger logger = LoggerFactory.getLogger(ProxyUtil.class);
	final static private Logger accessLogger = LoggerFactory.getLogger(ACCESS_LOGGER);
    
	final static public SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss.SSS z");
	
    final static private Random random = new Random();
    
    static public ProxyCallModule proxyCallModule;
	
	static public String getConnString(SocketAddress address) {
		String addrStr = address.toString();
		if(addrStr.contains("/")) {
			addrStr = addrStr.substring(addrStr.indexOf("/") + 1);
		}
		return addrStr;
	}
	
	static public String getLocalConnString(Channel channel) {
		return getConnString(channel.localAddress());
	}
	
	static public String getRemoteConnString(Channel channel) {
		return getConnString(channel.remoteAddress());
	}
	
	static public void sendErrorAndLogAccess(RequestContext ctx,  
			HttpResponseStatus status, String errMsg) {
		float targetTime = 1.0f * (ctx.targetCompletionTime - ctx.targetStartTime) / 1000000;
		sendErrorAndLogAccess(ctx.clientChannel, ctx.serverChannel, ctx.startTime, targetTime, 
				ctx.requestId, ctx.request.getUri(), ctx.request.getMethod().name(), 
				status, errMsg);
	}

	static public void sendErrorAndLogAccess(
			Channel clientChannel, Channel serverChannel, long startTime, 
			float targetTime, String requestId, String uri, 
			String method, HttpResponseStatus status, String errMsg) {
		sendError(clientChannel, requestId, status, errMsg);
		printAccessLog(clientChannel, serverChannel, startTime, targetTime, 
				requestId, uri, method, status, errMsg);
	}
	
	static public void sendError(Channel channel, String requestId, HttpResponseStatus status, String errMsg) {
		if(requestId == null) {
			requestId = "N/A";
		}
		if(errMsg == null) {
			errMsg = "Unknown error";
		}
		DefaultFullHttpResponse error = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
		error.headers().set(ServiceConst.HEADER_REQUEST_ID, requestId);
		error.headers().set(ServiceConst.HEADER_ERROR_MESSAGE, errMsg);
		sendMessage(channel, error);
	}

	static public void sendMessage(Channel channel, HttpMessage msg) {
		try {
			channel.writeAndFlush(msg);
		} catch (Exception e) {
			logger.error(String.format("Failed to send %s to %s", 
				msg.getClass().getSimpleName(), getRemoteConnString(channel)));
		}
	}
	
	static public String createRandomString(int length) {
		byte[] bytes = new byte[length];
		for(int i = 0; i < length; i++) {
			bytes[i] = (byte) (32 + random.nextInt(90));
		}
		return new String(bytes);
	}
	
	static public void setUserData(HttpMessage msg, String data) {
		msg.headers().set(ServiceConst.HEADER_USER_DATA, data);
	}

	static public void printAccessLog(RequestContext ctx, 
			HttpResponseStatus status, String msg) {
		float targetTime = 1.0f * (ctx.targetCompletionTime - ctx.targetStartTime) / 1000000;
		String uri = ctx.request != null ?
				ctx.request.getUri()
			  :	"unknown";
		String method = ctx.request != null ?
				ctx.request.getMethod().name()
			  :	"unknown";
		printAccessLog(ctx.clientChannel, ctx.serverChannel, ctx.startTime, targetTime,
			ctx.requestId, uri, method, status, msg);
	}
	
	static public void printAccessLog(Channel clientChannel, Channel serverChannel, 
			long startTime, float targetTime, String requestId, String uri, String method, 
			HttpResponseStatus status, String msg) {
		String serverConnString = serverChannel == null ? 
				"unknown:-1" : ProxyUtil.getRemoteConnString(serverChannel);
		long proxyTimeNano = System.nanoTime() - startTime;
		float proxyTime = 1.0f * proxyTimeNano / 1000000;
		String message = String.format("[%s][%3d] %s => [P] => %s %s %.2fms %.2fms %s",
			df.format(new Date()), status.code(),
			ProxyUtil.getRemoteConnString(clientChannel), 
			serverConnString, 
			requestId, proxyTime, targetTime, msg);
		accessLogger.info(message);
		
		if(proxyCallModule != null) {
			String tokens[] = serverConnString.split(":");
			String serverAddress = tokens[0]; 
			int serverPort = Integer.valueOf(tokens[1]); 
			proxyCallModule.requestCompleted(serverAddress, serverPort, uri, method, 
					status.code(), proxyTimeNano);
		}
	}
	
	static public String getUserData(HttpMessage msg) {
		return msg.headers().get(ServiceConst.HEADER_USER_DATA);
	}
}
