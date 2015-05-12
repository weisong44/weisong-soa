package com.weisong.soa.proxy.routing.config;

import io.netty.channel.Channel;

import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.weisong.soa.proxy.RequestContext;
import com.weisong.soa.proxy.routing.config.RRoutingConfig.Proc;

@NoArgsConstructor
public class RRoutingConfig extends BaseRoutingConfig<Proc> {
	
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
    
    @JsonIgnore
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
	
	static public class SelectedTarget {
		@Getter @Setter private RRoute route;
		@Getter @Setter private RTarget target;
		@Getter @Setter private Channel channel;
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
			throw new RuntimeException("Not supported");
		}

		public SelectedTarget select(RequestContext ctx) {
			for(RRoute r : routes) {
				SelectedTarget result = new SelectedTarget();
				result.setRoute(r);
				RTarget target = r.getProc().selectTarget(ctx);
				if(target != null) {
					result.setTarget(target);
				}
				return result;
			}
			return null;
		}

		@Override
		public boolean isAvailable() {
			return true;
		}
	}
}
