package com.weisong.soa.proxy.load.balancing;

import org.junit.Assert;
import org.junit.Test;

public class TestWeightedRandomStrategy {
	
	@Test
	public void testEvenDistribution() {
		doTestRR(new float[] { 1, 1, 1, 1 }, 0.001f, 0.001f);
		doTestRR(new float[] { 1, 1, 1, 2 }, 0.001f, 0.001f);
		doTestRR(new float[] { 1, 1, 1, 3 }, 0.002f, 0.001f);
		doTestRR(new float[] { 100, 200, 300, 400 }, 0.004f, 0.001f);
	}
	
	/**
	 * 
	 * @param maxSameRatio how many consecutive same values as a ratio 
	 *        of total operations
	 * @param distributionError how big is the error of distribution 
	 *        as a ratio of total operations
	 */
	public void doTestRR(float[] weights, float maxSameRatio, float maxDistributionError) {
		
		int n = 10000;
		
		WeightedRandomStrategy stragegy = new WeightedRandomStrategy(weights);

		int last = -1;
		int sameCount = 0, maxSameCount = 0;
		int[] counts = new int[weights.length];
		for(int i = 0; i < n; i++) {
			int index = stragegy.next();
			counts[index]++;
			if(last == index) {
				++sameCount;
			}
			else {
				maxSameCount = Math.max(sameCount, maxSameCount);
				last = index;
				sameCount = 0;
			}
		}
		
		// Randomness
		Assert.assertTrue(1f * maxSameCount / n < maxSameRatio); // 0.1%
		
		// Weight distribution
		float sum = 0f;
		for(int i = 0; i < weights.length; i++) {
			sum += weights[i];
		}
		
		for(int i = 0; i < weights.length; i++) {
			float r1 = weights[i] / sum;
			float r2 = 1f * counts[i] / n;
			Assert.assertTrue(Math.abs(r1/r2 - 1) < maxDistributionError);
		}
		
	}
}
