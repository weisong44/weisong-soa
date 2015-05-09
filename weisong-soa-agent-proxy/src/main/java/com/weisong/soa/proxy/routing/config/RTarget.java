package com.weisong.soa.proxy.routing.config;

import io.netty.channel.Channel;

import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Getter;
import lombok.Setter;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.weisong.soa.proxy.RequestContext;
import com.weisong.soa.proxy.connection.ConnectionPool;

public class RTarget extends BaseRoutingConfig {
	
	final private Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	@Getter @JsonIgnore private RTargetGroup.Proc parentProc;
	@Getter @Setter private String target;
	@Getter @Setter private float weight = 1f;
	
	public RTarget(RTargetGroup targetGroup) {
		this.parentProc = (RTargetGroup.Proc) targetGroup.getProc();
	}
	
	@Override
	protected void createProc() {
		proc = new Proc();
	}
	
	public class Proc extends BaseRoutingProc implements ConnectionPool.Listener {
		
		private AtomicBoolean available = new AtomicBoolean();

		@Override
		public RTarget selectTarget(RequestContext ctx) {
			return RTarget.this;
		}

		@Override
		public void connected(Channel c, ConnectionPool pool) {
			updateAvailablity(pool);
		}

		@Override
		public void disconnected(Channel c, ConnectionPool pool) {
			updateAvailablity(pool);
		}
		
		private void updateAvailablity(ConnectionPool pool) {
			boolean oldAvailable = available.get();
			available.set(pool.getSize() > 0);
			if(oldAvailable != available.get()) {
				if(available.get()) {
					logger.info(String.format("Target '%s' becomes available", target));
				}
				else {
					logger.info(String.format("Target '%s' becomes unavailable", target));
				}
				parentProc.targetStateChanged(available.get());
			}
		}

		@Override
		public boolean isAvailable() {
			return available.get();
		}
	}
}
