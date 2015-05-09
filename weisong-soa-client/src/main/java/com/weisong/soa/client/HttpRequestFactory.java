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
import com.weisong.soa.service.ServiceDescriptor;

public class HttpRequestFactory {
	
	final static private String urlPrefix = "http://localhost:" + ProxyConst.PROXY_PORT;
	
	private int requestIdSeed = getInitialRequestId();
	
	public HttpGet createHttpGet(ServiceDescriptor desc, String uri) 
			throws URISyntaxException {
		return create(HttpGet.class, desc, uri);
	}
	
	public HttpPost createHttpPost(ServiceDescriptor desc, String uri) 
			throws URISyntaxException {
		return create(HttpPost.class, desc, uri);
	}
	
	public HttpDelete createHttpDelete(ServiceDescriptor desc, String uri) 
			throws URISyntaxException {
		return create(HttpDelete.class, desc, uri);
	}
	
	public HttpPatch createHttpPatch(ServiceDescriptor desc, String uri) 
			throws URISyntaxException {
		return create(HttpPatch.class, desc, uri);
	}
	
	public HttpPut createHttpPut(ServiceDescriptor desc, String uri) 
			throws URISyntaxException {
		return create(HttpPut.class, desc, uri);
	}
	
	public HttpHead createHttpHead(ServiceDescriptor desc, String uri) 
			throws URISyntaxException {
		return create(HttpHead.class, desc, uri);
	}
	
	public HttpOptions createHttpOptions(ServiceDescriptor desc, String uri) 
			throws URISyntaxException {
		return create(HttpOptions.class, desc, uri);
	}
	
	public HttpTrace createHttpTrace(ServiceDescriptor desc, String uri) 
			throws URISyntaxException {
		return create(HttpTrace.class, desc, uri);
	}
	
	private <T extends HttpRequestBase> T create(Class<T> clazz, ServiceDescriptor desc, String uri) 
			throws URISyntaxException {
		try {
			T request = clazz.newInstance();
			populateUri(request, uri);
			populateHeader(request, desc);
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
	
	private void populateHeader(HttpUriRequest request, ServiceDescriptor desc) {
		request.setHeader(ServiceConst.HEADER_DOMAIN, desc.getDomain());
		request.setHeader(ServiceConst.HEADER_SERVICE_NAME, desc.getService());
		request.setHeader(ServiceConst.HEADER_SERVICE_VERSION, desc.getVersion());
		generateRequestId(request);
	}
}
