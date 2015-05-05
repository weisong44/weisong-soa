package com.weisong.soa.proxy.engine;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyNettyHandler {
	static public class ClientSide extends SimpleChannelInboundHandler<DefaultFullHttpRequest> {

		final private Logger logger = LoggerFactory.getLogger(getClass().getName());
	    
	    private ProxyEngine engine;

	    public ClientSide(ProxyEngine engine) {
	    	this.engine = engine;
	    }
	    
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			engine.connectedToClient(ctx.channel());
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
	        ctx.close();
	        engine.disconnectedFromClient(ctx.channel());
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, DefaultFullHttpRequest msg)
				throws Exception {
			engine.receivedMessageFromClient(ctx.channel(), msg);
		}
		
	    @Override
	    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
	        logger.warn(String.format("Exception from upstream: %s, %s", 
		    		ctx.channel().remoteAddress(), cause.getMessage()));
	        ctx.close();
	        engine.disconnectedFromClient(ctx.channel());
	    }
	}
	
	static public class ServerSide extends SimpleChannelInboundHandler<DefaultFullHttpResponse> {

		final private Logger logger = LoggerFactory.getLogger(getClass().getName());
	    
	    private ProxyEngine engine;
	    
	    public ServerSide(ProxyEngine engine) {
	    	this.engine = engine;
	    }

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
	        ctx.close();
	        engine.disconnectedFromServer(ctx.channel());
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, DefaultFullHttpResponse msg)
				throws Exception {
			engine.receivedMessageFromServer(ctx.channel(), msg);
		}

	    @Override
	    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
	    	cause.printStackTrace();
	        logger.warn(String.format("Exception from downstream: %s, %s", 
		    		ctx.channel().remoteAddress(), cause.getMessage()));
	        ctx.close();
	        engine.disconnectedFromServer(ctx.channel());
	    }
	}
}
