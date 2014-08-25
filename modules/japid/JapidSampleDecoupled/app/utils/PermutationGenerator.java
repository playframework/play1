/*
 * Copyright 2003-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;

/**
 * From Groovy: Systematically generate permutations.
 * 
 * Adapted from Java Code by Michael Gilleland (released with no restrictions)
 * using an algorithm described here: Kenneth H. Rosen, Discrete Mathematics and
 * Its Applications, 2nd edition (NY: McGraw-Hill, 1991), pp. 282-284
 */
public class PermutationGenerator<E> implements Iterator<List<E>> {
	private int[] a;
	private BigInteger numLeft;

	public BigInteger getNumLeft() {
		return numLeft;
	}

	private BigInteger total;
	private List<E> items;

	/**
	 * WARNING: Don't make n too large. Recall that the number of permutations
	 * is n! which can be very large, even when n is as small as 20 -- 20! =
	 * 2,432,902,008,176,640,000 and 21! is too big to fit into a Java long,
	 * which is why we use BigInteger instead.
	 * 
	 * @param items
	 *            the items to permute
	 */
	public PermutationGenerator(Collection<E> items) {
		this.items = new ArrayList<E>(items);
		int n = items.size();
		if (n < 1) {
			throw new IllegalArgumentException("At least one item required");
		}
		a = new int[n];
		total = getFactorial(n);
		reset();
	}

	public void reset() {
		for (int i = 0; i < a.length; i++) {
			a[i] = i;
		}
		numLeft = new BigInteger(total.toString());
	}

	public BigInteger getTotal() {
		return total;
	}

	public boolean hasNext() {
		return numLeft.compareTo(BigInteger.ZERO) == 1;
	}

	/**
	 * Compute factorial (TODO: expose this)
	 * 
	 * @param n
	 *            the input integer
	 * @return the factorial for n
	 */
	private static BigInteger getFactorial(int n) {
		BigInteger fact = BigInteger.ONE;
		for (int i = n; i > 1; i--) {
			fact = fact.multiply(new BigInteger(Integer.toString(i)));
		}
		return fact;
	}

	/**
	 * Generate next permutation (algorithm from Rosen p. 284)
	 * 
	 * @return the items permuted
	 */
	public List<E> next() {
		if (numLeft.equals(total)) {
			numLeft = numLeft.subtract(BigInteger.ONE);
			return items;
		}

		int temp;

		// Find largest index j with a[j] < a[j+1]
		int j = a.length - 2;
		while (a[j] > a[j + 1]) {
			j--;
		}

		// Find index k such that a[k] is smallest integer
		// greater than a[j] to the right of a[j]
		int k = a.length - 1;
		while (a[j] > a[k]) {
			k--;
		}

		// Interchange a[j] and a[k]
		temp = a[k];
		a[k] = a[j];
		a[j] = temp;

		// Put tail end of permutation after jth position in increasing order
		int r = a.length - 1;
		int s = j + 1;

		while (r > s) {
			temp = a[s];
			a[s] = a[r];
			a[r] = temp;
			r--;
			s++;
		}

		numLeft = numLeft.subtract(BigInteger.ONE);
		List<E> ans = new ArrayList<E>(a.length);
		for (int index : a) {
			ans.add(items.get(index));
		}
		return ans;
	}

	public void remove() {
		throw new UnsupportedOperationException("remove() not allowed for PermutationGenerator");
	}

	/**
	 * he total number of combinations of n objects taken k at a time, denoted
	 * nCk, is given by
	 * 
	 * n! / (k! * (n - k)!),
	 * 
	 * or n * (n-1) * (n-2)... (n - (k - 1)) / k!
	 * 
	 * @param n
	 *            totalSize
	 * @param k
	 *            comboSize
	 * @return
	 * @author bran
	 */
	public static BigInteger getCombinationsCount(int n, int k) {
		BigInteger bn = getFactorial(n, k);
		BigInteger bk = getFactorial(k);
		BigInteger divide = bn.divide(bk);
		return divide;
	}

	/**
	 * take any mutations of length k from n: formula: n!/(n-k)!
	 * @param n
	 * @param k
	 * @return
	 */
	public static BigInteger getPermuationCount(int n, int k) {
		BigInteger bn = getFactorial(n);
		BigInteger bk = getFactorial(n - k);
		BigInteger divide = bn.divide(bk);
		return divide;
	}
	
	/**
	 * calc   n(n-1)(n-2)... (n - (k - 1)) 
	 * @param n
	 * @param k
	 * @return
	 */
	private static BigInteger getFactorial(int n, int k) {
		if (k > n)
			return null;
		BigInteger fact = BigInteger.ONE;
		for (int i = n, j = 0; j < k; i--, j++) {
			fact = fact.multiply(new BigInteger(Integer.toString(i)));
		}
		return fact;
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#forEachRemaining(java.util.function.Consumer)
	 */
	@Override
	public void forEachRemaining(Consumer<? super List<E>> action) {
		// TODO Auto-generated method stub
		
	}

}
