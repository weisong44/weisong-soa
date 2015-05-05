package com.weisong.soa.proxy.routing.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

import com.weisong.soa.proxy.RequestContext;
import com.weisong.soa.proxy.load.balancing.LoadBalancingStrategy;
import com.weisong.soa.proxy.load.balancing.WeightedRandomStrategy;
import com.weisong.soa.proxy.routing.match.DefaultMatcher;
import com.weisong.soa.proxy.routing.match.RequestMatcher;

public class RRoute extends BaseRoutingConfig {

	static public class ForwardTo {
		@Getter @Setter private RTargetGroup targetGroup;
		@Getter @Setter private float weight = 1f;
	}

	final private Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	@Getter @Setter private String name;
	@Getter @Setter private String match;
	@Getter private List<ForwardTo> forwardToList = new ArrayList<>();
	
	@Override
	protected void createProc() {
		proc = new Proc();
	}
	
	public class Proc extends BaseRoutingProc implements RTargetGroup.ProcListener {
		
		@Getter private RequestMatcher matcher;
		@Getter private LoadBalancingStrategy loadBalancer;

		private AtomicInteger availableForwardToCount;
		
		public boolean match(RequestContext ctx) {
			return matcher.match(ctx);
		}
		
		@Override
		public RTarget selectTarget(RequestContext ctx) {
			if(match(ctx) == false) {
				return null;
			}
			while(isAvailable()) {
				int index = loadBalancer.next();
				ForwardTo fw = forwardToList.get(index);
				if(fw.targetGroup.getProc().isAvailable()) {
					return fw.targetGroup.getProc().selectTarget(ctx);
				}
			}
			return null;
		}
		
		@Override
		public boolean isAvailable() {
			return availableForwardToCount.intValue() > 0;
		}
		
		@Override
		public void start() {
			availableForwardToCount = new AtomicInteger();
			float[] weights = new float[forwardToList.size()];
			for(int i = 0; i < forwardToList.size(); i++) {
				ForwardTo fw = forwardToList.get(i);
				RTargetGroup.Proc tgProc = (RTargetGroup.Proc) fw.targetGroup.getProc(); 
				tgProc.addListener(this);
				weights[i] = fw.weight;
				fw.targetGroup.getProc().start();
			}
			loadBalancer = new WeightedRandomStrategy(weights);
			matcher = new DefaultMatcher();
			logger.info(String.format("Route '%s' started", name));
		}
		
		@Override
		public void stop() {
			availableForwardToCount = null;
			for(int i = 0; i < forwardToList.size(); i++) {
				ForwardTo fw = forwardToList.get(i);
				RTargetGroup.Proc tgProc = (RTargetGroup.Proc) fw.targetGroup.getProc(); 
				tgProc.removeListener(this);
				fw.targetGroup.getProc().stop();
			}
			loadBalancer = null;
			matcher = null;
			logger.info(String.format("Route '%s' stopped", name));
		}

		@Override
		public void targetGroupBecomesAvailable(RTargetGroup targetGroup) {
			availableForwardToCount.incrementAndGet();
			logChange();
		}

		@Override
		public void targetGroupBecomesUnavailable(RTargetGroup targetGroup) {
			availableForwardToCount.decrementAndGet();
			logChange();
		}
		
		private void logChange() {
			if(availableForwardToCount.intValue() > 0) {
				logger.info(String.format(
						"Route '%s' updated, %d target groups available", 
						name, availableForwardToCount.intValue()));
			}
			else {
				logger.info(String.format(
						"Route '%s' becomes unavailable", 
						name, availableForwardToCount.intValue()));
			}
		}
	}
	
}
