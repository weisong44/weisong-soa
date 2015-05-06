package com.weisong.soa.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;

import com.weisong.soa.proxy.ProxyConst;
import com.weisong.soa.service.ServiceConst;

public class HttpRequestFactory {
	
	final static private String urlPrefix = "http://localhost:" + ProxyConst.PROXY_PORT;
	
	private int requestIdSeed = getInitialRequestId();
	
	public HttpGet createHttpGet(String domain, String serviceName, String version, String uri) 
			throws URISyntaxException {
		return create(HttpGet.class, domain, serviceName, version, uri);
	}
	
	public HttpPost createHttpPost(String domain, String serviceName, String version, String uri) 
			throws URISyntaxException {
		return create(HttpPost.class, domain, serviceName, version, uri);
	}
	
	public HttpDelete createHttpDelete(String domain, String serviceName, String version, String uri) 
			throws URISyntaxException {
		return create(HttpDelete.class, domain, serviceName, version, uri);
	}
	
	public HttpPatch createHttpPatch(String domain, String serviceName, String version, String uri) 
			throws URISyntaxException {
		return create(HttpPatch.class, domain, serviceName, version, uri);
	}
	
	public HttpPut createHttpPut(String domain, String serviceName, String version, String uri) 
			throws URISyntaxException {
		return create(HttpPut.class, domain, serviceName, version, uri);
	}
	
	public HttpHead createHttpHead(String domain, String serviceName, String version, String uri) 
			throws URISyntaxException {
		return create(HttpHead.class, domain, serviceName, version, uri);
	}
	
	public HttpOptions createHttpOptions(String domain, String serviceName, String version, String uri) 
			throws URISyntaxException {
		return create(HttpOptions.class, domain, serviceName, version, uri);
	}
	
	public HttpTrace createHttpTrace(String domain, String serviceName, String version, String uri) 
			throws URISyntaxException {
		return create(HttpTrace.class, domain, serviceName, version, uri);
	}
	
	private <T extends HttpRequestBase> T create(Class<T> clazz, String domain, String serviceName, String version, String uri) 
			throws URISyntaxException {
		try {
			T request = clazz.newInstance();
			populateUri(request, uri);
			populateHeader(request, domain, serviceName, version);
			return request;
		} 
		catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void populateUri(HttpRequestBase request, String uri) 
			throws URISyntaxException {
		URI url = new URI(urlPrefix + uri);
		request.setURI(url);
	}

	public void generateRequestId(HttpUriRequest request) {
		request.setHeader(ServiceConst.HEADER_REQUEST_ID, getNextRequestId());
	}
	
	private int getInitialRequestId() {
		return 1000000000 + new Random().nextInt(999999999);
	}
	
	public String getNextRequestId() {
		synchronized (HttpRequestFactory.class) {
			if(++requestIdSeed >= Integer.MAX_VALUE) {
				requestIdSeed = getInitialRequestId();
			}
			return String.valueOf(requestIdSeed);
		}
	}
	
	private void populateHeader(HttpUriRequest request, String domain, String service, String version) {
		request.setHeader(ServiceConst.HEADER_DOMAIN, domain);
		request.setHeader(ServiceConst.HEADER_SERVICE_NAME, service);
		request.setHeader(ServiceConst.HEADER_SERVICE_VERSION, version);
		generateRequestId(request);
	}
}
