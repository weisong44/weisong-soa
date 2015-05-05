package com.weisong.soa.proxy.routing.config;

import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;

import com.weisong.soa.proxy.RequestContext;

@NoArgsConstructor
public class RRoutingConfig extends BaseRoutingConfig {
	
	@Getter private List<RTargetGroup> targetGroups	= new LinkedList<>();
	@Getter private List<RRoute> routes	= new LinkedList<>();
	
    public RTargetGroup getTargetGroup(String name) {
    	for(RTargetGroup tg : targetGroups) {
    		if(name.equals(tg.getName())) {
    			return tg;
    		}
    	}
    	return null;
    }
    
    public List<RTarget> getAllTargets() {
        List<RTarget> targets = new LinkedList<>();
    	for(RRoute r : routes) {
    		for(RRoute.ForwardTo fw : r.getForwardToList()) {
    			targets.addAll(fw.getTargetGroup().getTargets());
    		}
    	}
    	return targets;
    }
    
	@Override
	protected void createProc() {
		proc = new Proc();
	}
	
	public class Proc extends BaseRoutingProc {

		@Override
		public void start() {
			for(RRoute r : routes) {
				r.getProc().start();
			}
		}

		@Override
		public void stop() {
			for(RRoute r : routes) {
				r.getProc().stop();
			}
		}

		@Override
		public RTarget selectTarget(RequestContext ctx) {
			for(RRoute r : routes) {
				RTarget target = r.getProc().selectTarget(ctx);
				if(target != null) {
					return target;
				}
			}
			return null;
		}

		@Override
		public boolean isAvailable() {
			return true;
		}
	}
}
