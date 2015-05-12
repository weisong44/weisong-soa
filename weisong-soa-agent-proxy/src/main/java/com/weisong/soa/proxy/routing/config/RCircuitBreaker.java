package com.weisong.soa.proxy.routing.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class RCircuitBreaker {
	@Getter @Setter private String name;
	@Getter @Setter private float err4xxThreshold = Float.MAX_VALUE;
	@Getter @Setter private float err5xxThreshold = Float.MAX_VALUE;
	@Getter @Setter private float timedOutThreshold = Float.MAX_VALUE;
	@Getter @Setter private float errTotalThreshold = 0.5f;
}
