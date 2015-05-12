package com.weisong.soa.proxy.command;

import static com.weisong.soa.proxy.degrade.CircuitBreaker.State.closed;
import static com.weisong.soa.proxy.degrade.CircuitBreaker.State.halfOpen;
import static com.weisong.soa.proxy.degrade.CircuitBreaker.State.open;
import junit.framework.Assert;
import lombok.Setter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weisong.soa.proxy.degrade.CircuitBreaker;
import com.weisong.soa.proxy.degrade.CircuitBreaker.Def;
import com.weisong.soa.proxy.load.balancing.WeightedRandomStrategy;

public class TestCircuitBreaker {
	
	private boolean debug = true;
	
	private long startTime;
	private CircuitBreaker cb;
	private StateMonitor monitor;
	
	@Before
	public void setup() {
		log("=======================");
		CircuitBreaker.Def def = new Def();
		cb = new CircuitBreaker(def, 100);
		startTime = System.currentTimeMillis();
		monitor = new StateMonitor();
		monitor.start();
	}
	
	@After
	public void cleanup() {
		cb.stop();
		monitor.setShutdown(true);
	}
	
	private void log(String s) {
		if(debug) {
			System.out.println(s);
		}
	}
	
	@Test
	public void testStayClose() throws InterruptedException {
		testState(new float[] { 1.0f, 1.0f, 1.0f, 1.0f }, 300, closed);
		testState(new float[] { 2.0f, 2.1f, 1.1f, 1.0f }, 300, closed);
	}

	@Test
	public void testClosedToOpen() throws InterruptedException {
		testState(new float[] { 1.0f, 1.0f, 1.1f, 1.0f }, 300, open);
		testState(new float[] { 1.0f, 1.0f, 1.0f, 1.1f }, 300, open);
	}

	@Test
	public void testHalfOpen1() throws InterruptedException {
		testState(new float[] { 1.0f, 1.0f, 1.1f, 1.0f }, 2000, open);
		waitUntilHalfOpen();
		testState(new float[] { 1.0f, 1.0f, 1.0f, 1.9f }, 300,  open);
		waitUntilHalfOpen();
		testState(new float[] { 1.0f, 1.0f, 1.0f, 0.9f }, 300,  closed);
	}

	@Test
	public void testHalfOpen2() throws InterruptedException {
		testState(new float[] { 1.0f, 1.0f, 1.1f, 1.0f }, 1000, open);
		waitUntilHalfOpen();
		testState(new float[] { 1.0f, 1.0f, 1.0f, 0.9f }, 300,  closed);
	}

	private void waitUntilHalfOpen() throws InterruptedException {
		long time = System.currentTimeMillis() + 20000;
		while(System.currentTimeMillis() < time) {
			if(cb.getState() == halfOpen) {
				return;
			}
			Thread.sleep(10);
		}
		Assert.fail("Circuit breaker state didn't change to half-open");
	}
	
	private void testState(float[] ratios, long duration, CircuitBreaker.State expected) 
			throws InterruptedException {
		log(String.format("%6d ms -------------", getDelta()));
		DataFeeder dp = new DataFeeder(cb, ratios, duration);
		dp.start();
		dp.join();
		Assert.assertEquals(expected, cb.getState());
		log(String.format("%6d ms     >%s<", getDelta(), expected));
	}
	
	private long getDelta() {
		return System.currentTimeMillis() - startTime;
	}
	
	private class StateMonitor extends Thread {
		
		@Setter private boolean shutdown;
		
		private CircuitBreaker.State state = null;
		
		public void run() {
			while(!shutdown) {
				CircuitBreaker.State curState = cb.getState();
				if(state == null || state != curState) {
					state = curState;
					long delta = System.currentTimeMillis() - startTime;
					log(String.format("%6d ms      %s", delta, cb.getState()));
				}
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	static private class DataFeeder extends Thread {
		
		private long duration;
		private CircuitBreaker cb;
		private WeightedRandomStrategy gen;
		
		public DataFeeder(CircuitBreaker cb, float[] ratios, long duration) {
			this.duration = duration;
			this.cb = cb;
			this.gen = new WeightedRandomStrategy(ratios);
		}
		
		public void run() {
			long time = System.currentTimeMillis() + duration;
			while(true) {
				if(System.currentTimeMillis() > time) {
					break;
				}
				cb.update(gen.next());
			}
		}
	}
}
