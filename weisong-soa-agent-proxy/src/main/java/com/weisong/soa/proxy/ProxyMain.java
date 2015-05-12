package com.weisong.soa.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import java.io.IOException;

import lombok.Setter;

import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;

import com.weisong.soa.agent.impl.DefaultMgmtAgentJavaConfigZk;
import com.weisong.soa.agent.impl.JvmModuleJavaConfig;
import com.weisong.soa.core.zk.service.ZkServiceHandlerImpl;
import com.weisong.soa.proxy.connection.ConnectionManager;
import com.weisong.soa.proxy.engine.ProxyEngine;
import com.weisong.soa.proxy.engine.ProxyNettyHandler;
import com.weisong.soa.proxy.engine.ServiceConfigManager;
import com.weisong.soa.proxy.mgmt.MainProxyModule;
import com.weisong.soa.proxy.mgmt.ProxyCallModule;
import com.weisong.soa.proxy.util.ProxyUtil;

/**
 * Echoes back any received data from a client.
 */
public class ProxyMain {

	@Value("${app.port:-1}")
	@Setter
	protected String appPort;
	@Value("${proxy.access.log:/tmp/proxy-access.log}")
	@Setter
	protected String accessLogFileName;

	@Autowired
	private ProxyEngine engine;

	private void createAccessLogger() throws IOException {
		// Create the appender
		RollingFileAppender appender = new RollingFileAppender(
				new PatternLayout("%m%n"), accessLogFileName);
		appender.setMaxFileSize("10240KB");
		appender.setMaxBackupIndex(1);
		// Create the logger
		Logger accessLogger = Logger.getLogger(ProxyUtil.ACCESS_LOGGER);
		accessLogger.addAppender(appender);
		accessLogger.setAdditivity(false);
	}

	public void start() throws Exception {

		createAccessLogger();

		// Configure the server.
		EventLoopGroup bossGroup = new NioEventLoopGroup(5);
		EventLoopGroup workerGroup = new NioEventLoopGroup(10);
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_KEEPALIVE, true)
				.option(ChannelOption.SO_BACKLOG, 100)
				// .handler(new LoggingHandler(LogLevel.DEBUG))
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch)
							throws Exception {
						ch.pipeline().addLast(
							// new LoggingHandler(LogLevel.DEBUG),
							new HttpServerCodec(),
							new HttpObjectAggregator(12800),
							new ProxyNettyHandler.ClientSide(engine));
					}
				});

			// Start the server.
			ChannelFuture f = b.bind(ProxyConst.PROXY_PORT).sync();

			// Wait until the server socket is closed.
			f.channel().closeFuture().sync();
		} finally {
			// Shut down all event loops to terminate all threads.
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	@Configuration
	@Import({ JvmModuleJavaConfig.class, DefaultMgmtAgentJavaConfigZk.class,
			ZkServiceHandlerImpl.JavaConfig.class })
	static public class JavaConfig {

		@Autowired
		MainProxyModule mainModule;

		@Bean
		MainProxyModule mainProxyModule() {
			return new MainProxyModule();
		}

		@Bean
		ProxyCallModule proxyCallModule() {
			return new ProxyCallModule(mainModule);
		}

		@Bean
		ProxyMain proxyMain() {
			return new ProxyMain();
		}

		@Bean
		ProxyEngine proxyEngine() {
			return new ProxyEngine();
		}

		@Bean
		ConnectionManager connectionManager() {
			return new ConnectionManager();
		}
		
		@Bean
		ServiceConfigManager serviceConfigManager() {
			return new ServiceConfigManager();
		}

	}

	public static void main(String[] args) throws Exception {
		System.setProperty("ZK_CONN_STR", "localhost:2181");
		try (GenericApplicationContext ctx = new AnnotationConfigApplicationContext(
				JavaConfig.class)) {
			ProxyMain main = ctx.getBean(ProxyMain.class);
			main.start();
		}
	}
}
