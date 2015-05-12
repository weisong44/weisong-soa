package com.weisong.soa.proxy.degrade;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import lombok.Getter;
import lombok.Setter;

import com.weisong.soa.proxy.routing.config.RCircuitBreaker;
import com.weisong.soa.proxy.util.SlidingWindowCounter;

public class CircuitBreaker {
	
	// Invocation results
	final static public int SUCCESSFUL = 0;
	final static public int ERROR_4XX  = 1;
	final static public int ERROR_5XX  = 2;
	final static public int TIMED_OUT  = 3;

	static public enum State {
		open, closed, halfOpen
	}
	
	abstract public class Arbitor {
		
		/** 
		 * Number of slots, each slot is an checkInterval. By default, it is 
		 *  the same as openIntervalRatio  
		 */
		protected int slidingWindowSize;
		protected SlidingWindowCounter<Integer> counter;
		
		abstract boolean shouldOpen();
		
		private Arbitor() {
			this.slidingWindowSize = openIntervalRatio;
			reset();
		}
		
		private void update(int invocationResult) {
			counter.incrementCount(invocationResult);
		}
		
		private void reset() {
			counter = new SlidingWindowCounter<>(slidingWindowSize);
		}
	}
	
	private class DefaultArbitor extends Arbitor {
		
		private float err4xxThreshold = Float.MAX_VALUE;
		@Getter @Setter private float err5xxThreshold = Float.MAX_VALUE;
		@Getter @Setter private float timedOutThreshold = Float.MAX_VALUE;
		@Getter @Setter private float errTotalThreshold = 0.5f;
		
		private DefaultArbitor(RCircuitBreaker def) {
			this.err4xxThreshold = def.getErr4xxThreshold();
			this.err5xxThreshold = def.getErr5xxThreshold();
			this.timedOutThreshold = def.getTimedOutThreshold();
			this.errTotalThreshold = def.getErrTotalThreshold();
		}
		
		@Override
		boolean shouldOpen() {
			Map<Integer, Long> statsMap = counter.getCountsThenAdvanceWindow();
			Long err4xx = statsMap.get(ERROR_4XX) != null ?
					statsMap.get(ERROR_4XX)  : 0L;
			Long err5xx = statsMap.get(ERROR_5XX) != null ?
					statsMap.get(ERROR_5XX)  : 0L;
			Long timedOut = statsMap.get(TIMED_OUT) != null ?
					statsMap.get(TIMED_OUT)  : 0L;
			Long successful = statsMap.get(SUCCESSFUL) != null ?
					statsMap.get(SUCCESSFUL) : 0L;
			float error = timedOut + err5xx;
			float total = timedOut + err4xx + err5xx + successful;
			
			if(total <= 0) {
				return false;
			}
			
			float err4xxRatio = err4xx / total;
			float err5xxRatio = err5xx / total;
			float timedOutRatio = timedOut / total;
			float totalErrorRatio = error / total;
			
			System.out.println(String.format(
					"4xx=%5.2f 5xx=%5.2f TimedOut=%5.2f Total=%5.2f Successful=%5.0f Total=%5.0f", 
					err4xxRatio, err5xxRatio, timedOutRatio, totalErrorRatio, error, total));
				
			return totalErrorRatio > errTotalThreshold
				|| err4xxRatio > err4xxThreshold
				|| err5xxRatio > err5xxThreshold
				|| timedOutRatio > timedOutThreshold;
		}
	}
	
	static private Timer timer = new Timer();

	private RCircuitBreaker def;
	
	// Intervals
	@Getter @Setter private int checkingInterval;
	@Getter @Setter private int openInterval;
	@Getter @Setter private int halfOpenInterval;
	
	// Ratios
	@Getter @Setter private int openIntervalRatio = 10;
	@Getter @Setter private int halfOpenIntervalRatio = 2;
	
	// Expiration times
	private Long openStateExpirationTime;
	private Long halfOpenStateExpirationTime;
	
	@Getter private State state = State.closed;
	
	private Arbitor arbitor;
	
	private CheckTask checkTask;
		
	private class CheckTask extends TimerTask {
		@Override 
		public void run() {
			switch(state) {
			case closed:
				if(arbitor.shouldOpen()) {
					setToOpen();
				}
				break;
			case halfOpen:
				if(stateExpired()) {
					if(arbitor.shouldOpen()) {
						setToOpen();
					}
					else {
						setToClosed();
					}
				}
				break;
			case open:
				if(stateExpired()) {
					setToHalfOpen();
				}
				break;
			default:
				throw new RuntimeException("Should not get here");
			}
		}
		
		private boolean stateExpired() {
			switch(state) {
			case halfOpen:
				return now() > halfOpenStateExpirationTime;
			case open:
				return now() > openStateExpirationTime;
			case closed:
			default:
				throw new RuntimeException("Should not get here");
			}
		}
		
		private long now() {
			return System.currentTimeMillis();
		}
		
		private void setToOpen() {
			state = State.open;
			halfOpenStateExpirationTime = null;
			openStateExpirationTime = now() + openInterval;
		}
		
		private void setToHalfOpen() {
			state = State.halfOpen;
			arbitor.reset();
			openStateExpirationTime = null;
			halfOpenStateExpirationTime = now() + halfOpenInterval;
		}
		
		private void setToClosed() {
			state = State.closed;
			openStateExpirationTime = null;
			halfOpenStateExpirationTime = null;
		}
	};
	
	public CircuitBreaker(RCircuitBreaker def) {
		this(def, 1000);
	}
	
	public CircuitBreaker(RCircuitBreaker def, int checkingInterval) {
		this(def, null, checkingInterval);
	}
	
	public CircuitBreaker(RCircuitBreaker def, Arbitor arbitor, int checkingInterval) {
		this.def = def;
		this.checkingInterval = checkingInterval;
		this.openInterval = openIntervalRatio * checkingInterval;
		this.halfOpenInterval = halfOpenIntervalRatio * checkingInterval;
		
		if(arbitor == null) {
			arbitor = new DefaultArbitor(def);
		}
		this.arbitor = arbitor;
		
		checkTask = new CheckTask();
		timer.schedule(checkTask, 0, checkingInterval);
	}

	public boolean allowRequest() {
		return state != State.open;
	}
	
	public void update(int invocationResult) {
		if(allowRequest()) {
			arbitor.update(invocationResult);
		}
	}
	
	public void stop() {
		checkTask.cancel();
	}
	
	public String getName() {
		return def.getName();
	}
}
