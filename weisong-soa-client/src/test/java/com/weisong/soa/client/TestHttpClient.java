package com.weisong.soa.client;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.weisong.soa.agent.impl.DefaultMgmtAgentJavaConfigZk;
import com.weisong.soa.agent.impl.JvmModuleJavaConfig;
import com.weisong.soa.agent.impl.MainModule;
import com.weisong.soa.core.zk.ZkConst;
import com.weisong.soa.core.zk.config.ZkPropertyChangeRegistry;
import com.weisong.soa.core.zk.config.ZkPropertyChangeRegistry.Listener;

public class TestHttpClient {
	
	private int totalRequests = 200000;
	private int delayBetweenRequest = 0;
	
	private String domain = "test";
	private String service = "TestService";
	private String version = "0.0.1";

	@Autowired private HttpClient client;
	@Autowired private HttpRequestFactory factory;
	
	@Autowired private ZkPropertyChangeRegistry reg;
		
	@PostConstruct
	private void init() {
		reg.addPropertyChangeListener(new Listener() {
			@Override
			public void propertyChanged(Properties props) {
				System.out.println("Config updated:");
				{
					String s = props.getProperty("total.request");
					if(s != null) {
						int v = Integer.valueOf(s);
						System.out.println(String.format(
							"  totalRequests: %d -> %d", totalRequests, v));
						totalRequests = v;
					}
				} {
					String s = props.getProperty("delay.between.request");
					if(s != null) {
						int v = Integer.valueOf(s);
						System.out.println(String.format(
							"  delayBetweenRequest: %d -> %d", delayBetweenRequest, v));
						delayBetweenRequest = v;
					}
				}
			}
		});
	}

	private void doInvocation() throws Exception {
		
		final ResponseHandler<HttpResponse> responseHandler = new ResponseHandler<HttpResponse>() {
			public HttpResponse handleResponse(final HttpResponse response)
					throws ClientProtocolException, IOException {
				return response;
			}
        };
		
		List<HttpUriRequest> list = new LinkedList<>();
		for(int i = 0; i < 5; i++) {
			list.add(factory.createHttpGet(domain, service, version, "/hello" + i));
			list.add(factory.createHttpPost(domain, service, version, "/hello" + i));
			list.add(factory.createHttpPut(domain, service, version, "/hello" + i));
		}
		final HttpUriRequest[] requests = list.toArray(new HttpUriRequest[list.size()]);

		final AtomicInteger index = new AtomicInteger();
		final AtomicInteger count = new AtomicInteger();

		int workerCount = 5;
		Thread[] workers = new Thread[workerCount];
		for(int i = 0; i < workerCount; i++) {
			final int a = i + 1;
			workers[i] = new Thread() {
				public void run() {
					setName("" + a);
					while(count.intValue() < totalRequests) {
				        try {
				        	HttpUriRequest request = requests[index.intValue()];
				        	factory.regenerateRequestId(request);
							client.execute(request, responseHandler);
							Thread.sleep(delayBetweenRequest);
						} 
				        catch (Exception e) {
				        	e.printStackTrace();
						}
				        synchronized (TestHttpClient.class) {
					        index.set(index.incrementAndGet() % requests.length);
						}
				        count.incrementAndGet();
					}
				}
			};
		}
		
		for(Thread worker : workers) {
			worker.start();
		}
		
		for(Thread worker : workers) {
			worker.join();
		}
	}

	static public void main(String[] args) throws Exception {
		System.setProperty(ZkConst.ZK_CONN_STR, "localhost:2181");
		try(AnnotationConfigApplicationContext ctx = 
				new AnnotationConfigApplicationContext(JavaConfig.class)) {
			TestHttpClient client = ctx.getBean(TestHttpClient.class);
			client.doInvocation();
		}
	}
	
	@Configuration
	@Import({
	 	DefaultMgmtAgentJavaConfigZk.class
      ,	JvmModuleJavaConfig.class
	  ,	HttpClientModule.JavaConfig.class
	})
	static public class JavaConfig {
		@Bean public MainModule mainModule() {
			return new MainModule();
		}
		@Bean TestHttpClient testHttpClient() {
			return new TestHttpClient();
		}
	}
}
