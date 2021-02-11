/**
 * 
 */
package org.geworkbench.components.ttest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.commons.math3.util.FastMath;
import org.geworkbench.components.ttest.data.TTestInput;
import org.geworkbench.components.ttest.data.TTestOutput;

/**
 * @author zji
 * 
 */
public class TTest {

	final double[][] caseArray; // [rowCount][caseCount]
	final double[][] controlArray; // [rowCount][controlCount]

	final int rowCount;
	final int caseCount;
	final int controlCount;

	// options. please some of them are supposed to be ignored depending on
	// other ones
	// approximately in the order that affects the algorithm flow more
	final int significanceMethod;
	final boolean byPermutation;
	final double alpha;
	final boolean useWelch; // only affect the calculation of DF. thus orthogonal to other parameters
	final boolean useAllCombinations;
	final int numberCombinations;

	boolean isLogNormalized; // only affect folder change
	volatile boolean cancelled;
	
	public TTest(TTestInput input) {
		rowCount = input.getRowCount();
		caseCount = input.getCaseCount();
		controlCount = input.getControlCount();
		
		caseArray = input.getCaseArray();
		controlArray = input.getControlArray();
		
		// options (control parameters)
		significanceMethod = input.getSignificanceMethod();
		byPermutation = input.isByPermutation();
		useWelch = input.isUseWelch();
		useAllCombinations = input.isUseAllCombinations();
		numberCombinations = input.getNumberCombinations();
		alpha = input.getAlpha();

		isLogNormalized = input.isLogNormalized(); // only affect folder change
	}
	
	public TTestOutput execute() throws TTestException {
		cancelled = false;

		if (caseArray.length != rowCount || caseArray[0].length != caseCount
				|| controlArray.length != rowCount
				|| controlArray[0].length != controlCount) {
			throw new TTestInvalidInputException("Array dimensions do not match");
		}

		// the fields in result
		final double[] tValue = new double[rowCount];
		final double[] pValue;
		final double[] foldChange;
		final int significanceCount;
		final int[] significanceIndex;
		
		List<Integer> significanceIndexList = new ArrayList<Integer>();

		for (int i = 0; i < rowCount; i++) {
			tValue[i] = TestUtils.t(caseArray[i], controlArray[i]);
		}
		
		switch (significanceMethod) {
		case SignificanceMethod.JUST_ALPHA:
			pValue = getPValue(tValue);
			if(pValue==null) return null; // cancelled
			for (int i = 0; i < rowCount; i++) {
				if (pValue[i] <= alpha) {
					significanceIndexList.add(i);
				}
			}
			break;
		case SignificanceMethod.STD_BONFERRONI:
			pValue = getPValue(tValue);
			if(pValue==null) return null; // cancelled
			for (int i = 0; i < rowCount; i++) {
				if (pValue[i] <= alpha / rowCount) {
					significanceIndexList.add(i);
				}
			}
			break;
		case SignificanceMethod.ADJ_BONFERRONI:
			pValue = getPValue(tValue);
			if(pValue==null) return null; // cancelled
			NaturalRanking ranking = new NaturalRanking(NaNStrategy.MINIMAL,
					TiesStrategy.SEQUENTIAL);
			double[] absoluateTValue = new double[tValue.length];
			for (int i = 0; i < tValue.length; i++) {
				absoluateTValue[i] = Math.abs(tValue[i]);
			}
			double[] ranks = ranking.rank(absoluateTValue);
			int[] rank2Index = new int[ranks.length];
			for (int i = 0; i < rank2Index.length; i++) {
				rank2Index[(int) (ranks[i] - 1)] = i;
			}

			int n = rowCount;
			for (int rank = rowCount - 1; rank >= 0; rank--) {
				int i = rank2Index[rank];

				if (pValue[i] <= alpha / n) {
					significanceIndexList.add(i);
				}
				if (rank > 0
						&& absoluateTValue[i] > absoluateTValue[rank2Index[rank - 1]]) {
					n--;
				}
			}
			break;
		case SignificanceMethod.MIN_P:
			pValue = getPValueWithMinPMethod(tValue);
			for (int i = 0; i < rowCount; i++) {
				if(pValue[i] < alpha) {
					significanceIndexList.add(i);
				}
			}
			break;
		case SignificanceMethod.MAX_T:
			pValue = getPValueWithMaxTMethod(tValue);
			for (int i = 0; i < rowCount; i++) {
				if(pValue[i] < alpha) {
					significanceIndexList.add(i);
				}
			}
			break;
		default:
			throw new TTestException("Not implemented significance method");
		}

		// same for all options
		significanceCount = significanceIndexList.size();
		significanceIndex = new int[significanceCount];
		for(int i=0; i<significanceCount; i++) significanceIndex[i] = significanceIndexList.get(i);
		
		foldChange = calculateFoldChange(significanceIndex);

		return new TTestOutput(rowCount, tValue, pValue, foldChange,
				significanceCount, significanceIndex);
	}

	// common part among three different ways to decide significance
	private double[] getPValue(double[] tValue) throws TTestException {
		double[] pValue = new double[rowCount];
		for (int i = 0; i < rowCount; i++) {
			if(cancelled) return null;
			
			if (byPermutation) {
				double[] sampleTValue = null;
				if (useAllCombinations) {
					sampleTValue = calculateTValueForAllPermutations(i);
				} else {
					sampleTValue = calculateTValueForRandomPermutations(i);
				}
				pValue[i] = calculateProbability(sampleTValue, tValue[i]);

			} else {

				try {
					if (useWelch) {
						pValue[i] = TestUtils.tTest(caseArray[i],
								controlArray[i]);
					} else {
						pValue[i] = TestUtils.homoscedasticTTest(caseArray[i],
								controlArray[i]);
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					throw new TTestException(e.getMessage());
				}
			}
		}
		return pValue;
	}
	
	private double[] getPValueWithMaxTMethod(double[] tValue) throws TTestException {
		// rank tValue's absolute value
		double[] absolute = new double[tValue.length];
		for(int i=0; i<absolute.length; i++) {
			absolute[i] = Math.abs(tValue[i]);
		}
		NaturalRanking ranking = new NaturalRanking(NaNStrategy.MINIMAL,
				TiesStrategy.SEQUENTIAL);
		double[] ranks = ranking.rank(absolute);
		int[] reversedRank2index = new int[ranks.length];
		for(int i=0; i<ranks.length; i++) {
			reversedRank2index[ranks.length-(int)(ranks[i])] = i;
		}
		
		// get max-T
		double[][] maxTValue = null;
		if(useAllCombinations) {
			long longSampleSize = CombinatoricsUtils.binomialCoefficient(caseCount+controlCount, caseCount);
			if(longSampleSize>Integer.MAX_VALUE) {
				throw new TTestException("The parameter setting requires array size too large (" + longSampleSize + ").");
			}
			int sampleSize = (int) longSampleSize;
			maxTValue = new double[rowCount][sampleSize];
			int i = reversedRank2index[rowCount-1];
			maxTValue[rowCount-1] = calculateTValueForAllPermutations(i);
			for(int row=rowCount-2; row>=0; row--) {
				i = reversedRank2index[row];
				double[] newValue = calculateTValueForAllPermutations(i);
				for(int j=0; j<sampleSize; j++) {
					maxTValue[row][j] = Math.max(maxTValue[row+1][j], newValue[j]);
				}
			}

		} else {
			int sampleSize = numberCombinations;
			maxTValue = new double[rowCount][sampleSize];
			int i = reversedRank2index[rowCount-1];
			maxTValue[rowCount-1] = calculateTValueForRandomPermutations(i);
			for(int row=rowCount-2; row>=0; row--) {
				i = reversedRank2index[row];
				double[] newValue = calculateTValueForRandomPermutations(i);
				for(int j=0; j<sampleSize; j++) {
					maxTValue[row][j] = Math.max(maxTValue[row+1][j], newValue[j]);
				}
			}
		}

		// FIXME I think sample size should not be the same as numberCombinations in case of 'all permutations'.
		// keep this way to be consistent with previous geWorkbench code
		int sampleSize = numberCombinations;
		
		double[] pValue = new double[rowCount]; // adjusted p-value
		for (int rank = 0; rank < rowCount; rank++) {
			int pCounter = 0;
			for (int j = 0; j < sampleSize; j++) {
				if (Math.abs(maxTValue[rank][j]) >= absolute[reversedRank2index[rank]]) {
					pCounter++;
				}
			}
			pValue[reversedRank2index[rank]] = (double) pCounter / (double) sampleSize;
		}
		for (int rank = 1; rank < rowCount; rank++) {
			int i = reversedRank2index[rank];
			int i_1 = reversedRank2index[rank-1];
			pValue[i] = Math.max(pValue[i],	pValue[i_1]);
		}
		return pValue;
	}
	
	private double[] getPValueWithMinPMethod(double[] tValue) throws TTestException {
		double[][] sampleTValue = null;
		if(useAllCombinations) {
			long longSampleSize = CombinatoricsUtils.binomialCoefficient(caseCount+controlCount, caseCount);
			if(longSampleSize>Integer.MAX_VALUE) {
				throw new TTestException("The parameter setting requires array size too large (" + longSampleSize + ").");
			}
			int sampleSize = (int) longSampleSize;
			sampleTValue = new double[rowCount][sampleSize];
			for(int i=0; i<rowCount; i++) {
				sampleTValue[i] = calculateTValueForAllPermutations(i);
			}

		} else {
			int sampleSize = numberCombinations;
			sampleTValue = new double[rowCount][sampleSize];
			for(int i=0; i<rowCount; i++) {
				sampleTValue[i] = calculateTValueForRandomPermutations(i);
			}
		}

		// FIXME I think sample size should not be the same as numberCombinations in case of 'all permutations'.
		// keep this way to be consistent with previous geWorkbench code
		int sampleSize = numberCombinations;

		double[] rawPValue = new double[rowCount];
		// I think raw p-value may be calculated based on t-test. 
		// the current choice is based on the previous geWorkbench code
		for(int i=0; i<rowCount; i++) {
			// this is necessary only because the odd logic in the old code
			double[] x = new double[sampleSize];
			for (int j = 0; j < sampleSize; j++) {
				x[j] = sampleTValue[i][j];
			}
//			rawPValue[i] = calculatePValue(sampleTValue[i], tValue[i]);
			// not sure doing one-side make better sense, but this is what the previous geWorkbench implementation does
			rawPValue[i] = calculateOneSideProbability(x, tValue[i]);
		}

		NaturalRanking ranking = new NaturalRanking(NaNStrategy.MINIMAL,
				TiesStrategy.SEQUENTIAL);
		double[] ranks = ranking.rank(rawPValue);
		int[] rawPValueRank2Index = new int[ranks.length];
		for(int i=0; i<rawPValueRank2Index.length; i++) {
			rawPValueRank2Index[(int)(ranks[i]-1)] = i;
		}

		double[][] q = new double[rowCount+1][sampleSize];
		for (int j = 0; j < sampleSize; j++) {
			q[rowCount][j] = 1.0d;
		}

		// loop starting with the largest raw p-value to smaller ones
		double[] sortedAdjPValues = new double[rowCount];
		for (int rank = rowCount-1; rank >=0; rank--) {
			int rowIndex = rawPValueRank2Index[rank];

			// this should be the correct one
//			double[] sampleTValuesInOneRow = sampleTValue[rowIndex];
			// this is the odd one following the old code
			double[] sampleTValuesInOneRow = new double[sampleSize];
			for(int j=0; j<sampleSize; j++)sampleTValuesInOneRow[j] = sampleTValue[rowIndex][j];
			
			double[] tRanks = ranking.rank(sampleTValuesInOneRow);
			int[] tRank2Index = new int[tRanks.length];
			for(int i=0; i<tRank2Index.length; i++) {
				tRank2Index[(int)(tRanks[i]-1)] = i;
			}
			double[] pValueFromOrderStats = getPValsFromOrderStats(sampleTValuesInOneRow);
			
			int adjPCounter = 0;
			for (int j = 0; j < sampleSize; j++) {
				q[rank][j] = Math.min(q[rank + 1][j], pValueFromOrderStats[tRank2Index[j]]);
				if (q[rank][j] <= rawPValue[rowIndex]) {
					adjPCounter++;
				}
			}

			sortedAdjPValues[rank] = (double) adjPCounter / (double) sampleSize;
		}
		
		for (int i = 1; i < sortedAdjPValues.length; i++) {
			sortedAdjPValues[i] = Math.max(sortedAdjPValues[i - 1],
					sortedAdjPValues[i]);
		}

		double[] pValue = new double[rowCount]; // adjusted p-value
		for (int i = 0; i < sortedAdjPValues.length; i++) {
			pValue[i] = sortedAdjPValues[rawPValueRank2Index[i]];
		}
		return pValue;
	}

	private static double[] getPValsFromOrderStats(double[] sampleTValuesInOneRow) {
		int sampleSize = sampleTValuesInOneRow.length;
		double[] x = new double[sampleSize];
		
		Arrays.sort(sampleTValuesInOneRow);
		x[sampleSize-1] = 1.;
		for(int i=sampleSize-2; i>=0; i--) {
			if(sampleTValuesInOneRow[i]<sampleTValuesInOneRow[i+1]) {
				x[i] = (i+1.)/sampleSize;
			} else {
				x[i] = x[i+1];
			}
		}
		return x;
	}

	// use random permutations
	private double[] calculateTValueForRandomPermutations(int row) {
		double[] sampleTValue = new double[numberCombinations];
		
		final int total = caseCount + controlCount;
		Random random = new Random();

		for (int i = 0; i < numberCombinations; i++) {
			List<Integer> candidates = new ArrayList<Integer>();
			for (int j = 0; j < total; j++)
				candidates.add(j);

			double[] a = new double[caseCount];
			for (int j = 0; j < caseCount; j++) {
				int index = random.nextInt( candidates.size() );
				int next = candidates.remove(index);
				if (next < caseCount) {
					a[j] = caseArray[row][next];
				} else {
					a[j] = controlArray[row][next - caseCount];
				}
			}
			double[] b = new double[controlCount];
			for (int j = 0; j < controlCount; j++) {
				int next = candidates.get(j);
				if (next < caseCount) {
					b[j] = caseArray[row][next];
				} else {
					b[j] = controlArray[row][next - caseCount];
				}
			}
			sampleTValue[i] = TestUtils.t(a, b);
		}
		return sampleTValue;
	}

	// use all permutations
	private double[] calculateTValueForAllPermutations(int row) throws TTestException {
		long longSampleSize = CombinatoricsUtils.binomialCoefficient(caseCount+controlCount, caseCount);
		if(longSampleSize>Integer.MAX_VALUE) {
			throw new TTestException("The parameter setting requires array size too large (" + longSampleSize + ").");
		}
		int sampleSize = (int) longSampleSize;
		double[] sampleTValue = new double[sampleSize];
		final int total = caseCount + controlCount;
		
		int totalCombinations = 0;
		int[] groupAIndex = new int[caseCount];
		for (int i = 0; i < groupAIndex.length; i++) {
			groupAIndex[i] = -1;
		}
		while (Combinations.enumerateCombinations(total,
				caseCount, groupAIndex)) {
			
			boolean[] inA = new boolean[total];
			for (int i = 0; i < total; i++) {
				inA[i] = false;
			}
			
			double[] a = new double[caseCount];
			for (int i = 0; i < groupAIndex.length; i++) {
				int next = groupAIndex[i];
				inA[next] = true;
				if (next < caseCount) {
					a[i] = caseArray[row][next];
				} else {
					a[i] = controlArray[row][next - caseCount];
				}
			}
			double[] b = new double[controlCount];
			int bIndex = 0;
			for (int i = 0; i < total; i++) {
				if (!inA[i]) {
					if (i < caseCount) {
						b[bIndex] = caseArray[row][i];
					} else {
						b[bIndex] = controlArray[row][i - caseCount];
					}
					bIndex++;
				}
			}
			sampleTValue[totalCombinations] = TestUtils.t(a, b);
			totalCombinations++;
		}
		return sampleTValue;
	}
	
	// calculate probability based on pseudo-samples' t-values
	private static double calculateProbability(final double[] sampleTValue, final double t) {
		
		int count = 0;
		for (int i = 0; i < sampleTValue.length; i++) {
			if(Math.abs(t) < Math.abs(sampleTValue[i])) {
				count++;
			}
		}
		return ((double)count)/sampleTValue.length;

	}

	// calculate one-side probability based on pseudo-samples' t-values
	// note: the convention about the equality case is different from in calculateProbability
	private static double calculateOneSideProbability(final double[] sampleTValue, final double t) {
		
		int count = 0;
		for (int i = 0; i < sampleTValue.length; i++) {
			if(t <= sampleTValue[i]) {
				count++;
			}
		}
		return ((double)count)/sampleTValue.length;

	}
	
	/** 
	 * Calculate fold change. If the mean is negative for a row, its fold change value is set as NaN.
	 * 
	 * @param significanceIndex
	 * @return
	 */
	private double[] calculateFoldChange(final int[] significanceIndex) {
		double[] foldChange = new double[rowCount];

		for (int i = 0; i < significanceIndex.length; i++) {

			int index = significanceIndex[i];
			double caseMean = StatUtils.mean(caseArray[index]);
			double controlMean = StatUtils.mean(controlArray[index]);

			if (isLogNormalized) {
				foldChange[index] = caseMean - controlMean;
			} else if (caseMean <= 0 || controlMean <= 0) {
				foldChange[index] = Double.NaN;
			} else {
				foldChange[index] = FastMath.log(2.0, caseMean / controlMean);
			}
		}

		return foldChange;
	}
}

class TTestInvalidInputException extends TTestException {

	private static final long serialVersionUID = 7991028425171144064L;

	public TTestInvalidInputException(String string) {
		super(string);
	}

}
