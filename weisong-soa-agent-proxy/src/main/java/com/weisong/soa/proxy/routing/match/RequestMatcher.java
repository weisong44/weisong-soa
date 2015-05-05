package com.weisong.soa.proxy.routing.match;

import com.weisong.soa.proxy.RequestContext;

public interface RequestMatcher {
	boolean match(RequestContext ctx);
}
