package com.weisong.soa.proxy.routing.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.weisong.soa.proxy.RequestContext;
import com.weisong.soa.proxy.load.balancing.LoadBalancingStrategy;
import com.weisong.soa.proxy.load.balancing.LoadBalancingType;
import com.weisong.soa.proxy.load.balancing.WeightedRandomStrategy;
import com.weisong.soa.proxy.load.balancing.WeightedRoundRobinStragegy;

@NoArgsConstructor
public class RTargetGroup extends BaseRoutingConfig {
	
	final private Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	@Getter @Setter private String name;
	@Getter @Setter private LoadBalancingType lbType = LoadBalancingType.random;
	@Getter @Setter private List<RTarget> targets = new ArrayList<>();

	public RTargetGroup(String name) {
		this.name = name;
	}

	@Override
	protected void createProc() {
		proc = new Proc();
	}
		
	static public interface ProcListener {
		void targetGroupBecomesAvailable(RTargetGroup targetGroup);
		void targetGroupBecomesUnavailable(RTargetGroup targetGroup);
	}
	
	public class Proc extends BaseRoutingProc {

		private Set<ProcListener> listeners = new HashSet<>();
		private LoadBalancingStrategy loadBalancer;
		private AtomicInteger availableTargets = new AtomicInteger();
		
		@Override
		public RTarget selectTarget(RequestContext ctx) {
			while(isAvailable()) {
				int index = loadBalancer.next();
				if(targets.get(index).getProc().isAvailable()) {
					return targets.get(index);
				}
			}
			return null;
		}
		
		public void targetStateChanged(boolean available) {
			if(available) {
				availableTargets.incrementAndGet();
				if(availableTargets.intValue() == 1) {
					logger.info(String.format(
							"Target group '%s' becomes available", name));
					for(ProcListener l : listeners) {
						l.targetGroupBecomesAvailable(RTargetGroup.this);
					}
				}
			}
			else {
				availableTargets.decrementAndGet();
				if(availableTargets.intValue() == 0) {
					logger.info(String.format(
							"Target group '%s' becomes unavailable", name));
					for(ProcListener l : listeners) {
						l.targetGroupBecomesUnavailable(RTargetGroup.this);
					}
				}
			}
		}
		
		@Override
		public boolean isAvailable() {
			return availableTargets.intValue() > 0;
		}
		
		@Override
		public void start() {
			float[] weights = new float[targets.size()];
			for(int i = 0; i < targets.size(); i++) {
				RTarget t = targets.get(i);
				weights[i] = t.getWeight();
				t.getProc().start();
			}
			switch (lbType) {
			case random:
				loadBalancer = new WeightedRandomStrategy(weights);
				break;
			case roundRobin:
				loadBalancer = new WeightedRoundRobinStragegy(weights);
				break;
			case leastActive:
			case hashing:
			default:
				throw new RuntimeException("Not implemented");
			}
			logger.info(String.format("Target group '%s' started", name));
		}

		@Override
		public void stop() {
			loadBalancer = null;
			for(int i = 0; i < targets.size(); i++) {
				RTarget t = targets.get(i);
				t.getProc().stop();
			}
			logger.info(String.format("Target group '%s' stopped", name));
		}
		
		public void addListener(ProcListener listener) {
			listeners.add(listener);
		}
		
		public void removeListener(ProcListener listener) {
			listeners.remove(listener);
		}
	}
}
