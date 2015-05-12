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
import com.weisong.soa.proxy.degrade.CircuitBreaker;
import com.weisong.soa.proxy.routing.config.RTarget.Proc;

public class RTarget extends BaseRoutingConfig<Proc> {
	
	final private Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	@Getter @JsonIgnore private RTargetGroup.Proc parentProc;
	@Getter @Setter private String connStr;
	@Getter @Setter private float weight = 1f;
	@Getter @Setter private RCircuitBreaker cbDef;
	
	public RTarget(RTargetGroup targetGroup) {
		this.parentProc = (RTargetGroup.Proc) targetGroup.getProc();
	}
	
	@JsonIgnore
	public boolean isCircuitBreakerEnabled() {
		return cbDef != null;
	}
 	
	@Override
	protected void createProc() {
		proc = new Proc();
	}
	
	public class Proc extends BaseRoutingProc 
			implements ConnectionPool.Listener, CircuitBreaker.Listener {
		
		@Getter private CircuitBreaker circuitBreaker;
		private AtomicBoolean connPoolAvailable = new AtomicBoolean();
		private boolean curAvailable;

		@Override
		public void start() {
			if(isCircuitBreakerEnabled()) {
				circuitBreaker = new CircuitBreaker(cbDef);
				circuitBreaker.setListener(this);
			}
		}

		@Override
		public void stop() {
			if(circuitBreaker != null) {
				circuitBreaker.setListener(null);
				circuitBreaker = null;
			}
		}

		@Override
		public RTarget selectTarget(RequestContext ctx) {
			return RTarget.this;
		}

		@Override
		public void connected(Channel c, ConnectionPool pool) {
			updateConnPoolAvailablity(pool);
		}

		@Override
		public void disconnected(Channel c, ConnectionPool pool) {
			updateConnPoolAvailablity(pool);
		}
		
		private void updateConnPoolAvailablity(ConnectionPool pool) {
			boolean oldConnPoolAvailable = connPoolAvailable.get();
			connPoolAvailable.set(pool.getSize() > 0);
			if(oldConnPoolAvailable != connPoolAvailable.get()) {
				if(connPoolAvailable.get()) {
					logger.info(String.format("Target '%s' connetion pool becomes available", connStr));
				}
				else {
					logger.info(String.format("Target '%s' connetion pool becomes unavailable", connStr));
				}
			}
			updateCurAvailability();
		}

		@Override
		public void circuitBreakerStateChanged(CircuitBreaker cb) {
			logger.info(String.format("Circuit breaker on target '%s' is now '%s'", 
					connStr, circuitBreaker.getState()));
			updateCurAvailability();
		}
		
		private void updateCurAvailability() {
			boolean newAvailable = isAvailable();
			if(curAvailable != newAvailable) {
				curAvailable = newAvailable;
				if(curAvailable) {
					logger.info(String.format("Target '%s' becomes available", connStr));
				}
				else {
					logger.info(String.format("Target '%s' becomes unavailable", connStr));
				}
				parentProc.targetStateChanged(curAvailable);
			}
		}

		@Override
		public boolean isAvailable() {
			if(isCircuitBreakerEnabled() && circuitBreaker.allowRequest() == false) {
				return false;
			}
			return connPoolAvailable.get();
		}
	}
}
