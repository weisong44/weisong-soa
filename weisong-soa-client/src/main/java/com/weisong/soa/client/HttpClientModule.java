package com.weisong.soa.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import com.weisong.soa.agent.Module;
import com.weisong.soa.agent.impl.BaseCallModule;
import com.weisong.soa.agent.impl.MainModule;

public class HttpClientModule extends BaseCallModule {

	public HttpClientModule(Module parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "client-call-stats";
	}

	@Override
	public String getType() {
		return "http-client-call-stats";
	}

	@Override
	public String getDescription() {
		return "Apache HTTP client (instrumented)";
	}

	@Configuration
	@EnableAspectJAutoProxy
	static public class JavaConfig {
		
		@Autowired private MainModule mainModule;
		
		@Bean public HttpRequestFactory httpRequestFactory() {
			return new HttpRequestFactory();
		}
		@Bean public HttpClientModule restfulClientModule() {
			return new HttpClientModule(mainModule);
		}
		@Bean public HttpClientAspect httpClientAspect() {
			return new HttpClientAspect();
		}
	}
}
