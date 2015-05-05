package com.weisong.soa.proxy.routing.match;

import com.weisong.soa.proxy.RequestContext;

public class DefaultMatcher implements RequestMatcher {

	@Override
	public boolean match(RequestContext ctx) {
		return true;
	}

}
