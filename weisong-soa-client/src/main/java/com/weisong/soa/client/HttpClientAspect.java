package com.weisong.soa.client;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;

@Aspect
public class HttpClientAspect {
	
	@Autowired private HttpClientModule module;
	
	@Around("execution(* org.apache.http.client.HttpClient.execute(..))")
	public Object recordInvocationMetrics(ProceedingJoinPoint pjp) throws Throwable {

		String address = null;
		int port = -1;
		String uri = null;
		String method = null;
		int status = -1;
		long startTime = System.nanoTime();
				
		for(Object a : pjp.getArgs()) {
			if(a instanceof HttpUriRequest) {
				HttpUriRequest uriRequest = (HttpUriRequest) a;
				address = uriRequest.getURI().getHost();
				port = uriRequest.getURI().getPort();
				uri = uriRequest.getURI().getPath();
				method = uriRequest.getMethod();
			}
		}
		
		try {
			Object result = pjp.proceed();
			if(result instanceof HttpResponse) {
				HttpResponse response = (HttpResponse) result;
				status = response.getStatusLine().getStatusCode();
			}
			else {
				status = 200;
			}
			return result;
		} catch (Throwable t) {
			status = 600;
			throw t;
		}
		finally {
			if(address == null || port < 0 || uri == null || method == null || status < 0) {
				throw new RuntimeException("TODO: AOP not yet supported!");
			}
			long time = System.nanoTime() - startTime;
			module.requestCompleted(address, port, uri, method, status, time);
		}
		
	}
}
