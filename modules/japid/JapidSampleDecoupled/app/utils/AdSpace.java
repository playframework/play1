package utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdSpace {

	int totalSlots;
	public List<AdBidder> bidders;
	List<PermuteWithProbability> probabilities;

	// other attributes to add

	public List<PermuteWithProbability> getProbabilities() {
		if (probabilities == null) {
			probabilities = calcProbabilities(bidders, totalSlots);
		}
		return probabilities;
	}

	public AdSpace(int totalSlots, List<AdBidder> bidders) {
		super();
		this.totalSlots = totalSlots;
		this.bidders = bidders;
		// probabilities = calcProbabilities(bidders, totalSlots);
	}

	/**
	 * get all the probs of a new bidder being displaying in all the slots
	 * 
	 * @param newBidder
	 * @return
	 * @deprecated this method consumes lots of memory. Please use
	 *             getNewBidderChancesFast()
	 */
	public double[] getNewBidderChances(AdBidder newBidder) {
		List<AdBidder> withNew = new ArrayList<AdBidder>(bidders);
		withNew.add(newBidder);
		List<PermuteWithProbability> perms = calcProbabilities(withNew, totalSlots);
		double[] results = new double[totalSlots];

		for (PermuteWithProbability perm : perms) {
			List<AdBidder> list = perm.permute;
			int size = list.size();
			for (int i = 0; i < size; i++) {
				AdBidder ab = list.get(i);
				if (ab.equals(newBidder)) {
					results[i] += perm.probability;
				}
			}
		}
		return results;
	}

	/**
	 * Finds all non-null subsequences of a list. E.g.
	 * <code>subsequences([1, 2, 3])</code> would be: [[1, 2, 3], [1, 3], [2,
	 * 3], [1, 2], [1], [2], [3]]
	 * 
	 * @param items
	 *            the List of items
	 * @return the subsequences from items
	 */
	public static <T> Set<List<T>> subsequences(List<T> items) {
		Set<List<T>> ans = new HashSet<List<T>>();
		for (T h : items) {
			Set<List<T>> next = new HashSet<List<T>>();
			for (List<T> it : ans) {
				List<T> sublist = new ArrayList<T>(it);
				sublist.add(h);
				next.add(sublist);
			}
			next.addAll(ans);
			List<T> hlist = new ArrayList<T>();
			hlist.add(h);
			next.add(hlist);
			ans = next;
		}
		return ans;
	}

	/**
	 * get sub sequence of a specific length.
	 * 
	 * @param <T>
	 * @param items
	 * @param length
	 *            , the number of elements in the combination
	 * @return
	 */
	public static <T> Set<List<T>> subsequences(List<T> items, int length) {
		if (length > items.size())
			length = items.size();
		Set<List<T>> ans = new HashSet<List<T>>();
		for (T h : items) {
			Set<List<T>> next = new HashSet<List<T>>();
			for (List<T> it : ans) {
				if (it.size() < length) {
					List<T> sublist = new ArrayList<T>(it);
					sublist.add(h);
					next.add(sublist);
				}
			}
			next.addAll(ans);
			List<T> hlist = new ArrayList<T>();
			hlist.add(h);
			next.add(hlist);
			ans = next;
		}

		// removed too short ones
		Iterator<List<T>> it = ans.iterator();
		while (it.hasNext()) {
			List<T> o = it.next();
			if (o.size() != length)
				it.remove();
		}
		return ans;
	}

	/**
	 * very slow
	 * 
	 * @param candies
	 * @param totalSlots
	 * @return
	 * @deprecated memory unsafe.
	 */
	public static List<PermuteWithProbability> calcProbabilities(List<AdBidder> candies, int totalSlots) {
		Set<List<AdBidder>> permutes = new HashSet<List<AdBidder>>();
		long t = System.currentTimeMillis();
		// Set<List<AdBidder>> subsequences =
		// GroovyCollections.subsequences(candies, totalSlots);
		Set<List<AdBidder>> subsequences = subsequences(candies, totalSlots);
		System.out.println("subseq used(ms): " + (System.currentTimeMillis() - t));
		t = System.currentTimeMillis();

		Iterator<List<AdBidder>> it = subsequences.iterator();
		while (it.hasNext()) {
			List<AdBidder> lab = it.next();
			PermutationGenerator<AdBidder> permu = new PermutationGenerator<AdBidder>(lab);
			while (permu.hasNext()) {
				permutes.add(permu.next());
			}
		}

		System.out.println("calc all permutes used(ms): " + (System.currentTimeMillis() - t));

		System.out.println("total exposure combo: " + permutes.size());

		List<PermuteWithProbability> calc = new ArrayList<PermuteWithProbability>();

		it = permutes.iterator();
		while (it.hasNext()) {
			List<AdBidder> per = it.next();
			List<AdBidder> placed = new ArrayList<AdBidder>();
			double prob = 1;
			int persize = per.size();
			for (int i = 0; i < persize; i++) {
				AdBidder cur = per.get(i);
				Iterator<AdBidder> caniter = candies.iterator();
				int total = 0;
				while (caniter.hasNext()) {
					AdBidder can = caniter.next();
					if (!placed.contains(can))
						total += can.getBidPower();
				}
				prob *= (cur.getBidPower() + 0.0) / total;
				placed.add(cur);
			}
			calc.add(new PermuteWithProbability(per, prob));
		}

		return calc;
	}

	public List<Double> getNewBidderChancesFast(AdBidder newBidder) {
		return testNewBidderChancesFast(newBidder, this.bidders, totalSlots);
	}

	public List<Double> getNewBidderChances3(AdBidder newBidder) {
		return testNewBidderChances3(newBidder, this.bidders, totalSlots);
	}
	
	public static List<Double> testNewBidderChancesFast(AdBidder newBidder, List<AdBidder> candies, int totalSlots) {
		// Set<List<AdBidder>> permutes = new HashSet<List<AdBidder>>();
		List<AdBidder> allBidders = new ArrayList<AdBidder>(candies);
		allBidders.add(newBidder);

		long t = System.currentTimeMillis();
		// Set<List<AdBidder>> subsequences =
		// GroovyCollections.subsequences(candies, totalSlots);
		Set<List<AdBidder>> subsequences = subsequences(allBidders, totalSlots);
		System.out.println(subsequences.size() + " subseqs  used(ms): " + (System.currentTimeMillis() - t));
		t = System.currentTimeMillis();

		// each bidder with a list of chances in each open slot
		Map<AdBidder, List<Double>> bidderChances = new HashMap<AdBidder, List<Double>>();

		Iterator<List<AdBidder>> it = subsequences.iterator();
		BigInteger bi = new BigInteger("0");
		while (it.hasNext()) {
			List<AdBidder> lab = it.next();
			PermutationGenerator<AdBidder> permu = new PermutationGenerator<AdBidder>(lab);
			while (permu.hasNext()) {
				bi = bi.add(BigInteger.ONE);
				List<AdBidder> per = permu.next();
				List<AdBidder> placed = new ArrayList<AdBidder>();
				double prob = 1;
				int persize = per.size();
				for (int i = 0; i < persize; i++) {
					AdBidder cur = per.get(i);
					Iterator<AdBidder> caniter = allBidders.iterator();
					int total = 0;
					while (caniter.hasNext()) {
						AdBidder can = caniter.next();
						if (!placed.contains(can))
							total += can.getBidPower();
					}
					prob *= (cur.getBidPower() + 0.0) / total;
					placed.add(cur);
				}
				for (int i = 0; i < persize; i++) {
					AdBidder cur = per.get(i);
					List<Double> list = bidderChances.get(cur);
					if (list == null) {
						list = new ArrayList<Double>();
						for (int j = 0; j < persize; j++)
							list.add(new Double(0));
						bidderChances.put(cur, list);
					}
					Double double1 = list.get(i);
					if (double1 == null)
						double1 = 0d;
					list.set(i, double1 + prob);
				}
			}
		}

		System.out.println("total permuts: " + bi.toString());
		return bidderChances.get(newBidder);
	}

	public static List<Double> testNewBidderChances3(AdBidder newBidder, List<AdBidder> candies, int totalSlots) {
		// Set<List<AdBidder>> permutes = new HashSet<List<AdBidder>>();
		List<AdBidder> allBidders = new ArrayList<AdBidder>(candies);
		allBidders.add(newBidder);

		AdBidder[] bidderArray = new AdBidder[allBidders.size()];
		allBidders.toArray(bidderArray);
		
		Map<AdBidder, Byte> bidderIndexMap = new HashMap<AdBidder, Byte>();
		for (byte i = 0; i < allBidders.size(); i++) {
			AdBidder b = bidderArray[i];
			bidderIndexMap.put(b, i);
		}
		
		
		long t = System.currentTimeMillis();
		// Set<List<AdBidder>> subsequences =
		// GroovyCollections.subsequences(candies, totalSlots);
		Set<List<AdBidder>> subsequences = subsequences(allBidders, totalSlots);
		System.out.println(subsequences.size() + " subseqs  used(ms): " + (System.currentTimeMillis() - t));
		t = System.currentTimeMillis();

		// each bidder with a list of chances in each open slot
//		Map<AdBidder, List<Double>> bidderChances = new HashMap<AdBidder, List<Double>>();
		// dim 1: all bidders , dim 2 chances to appear in each slots
		double[][]  bidderChances = new double[allBidders.size()][];
		
		Iterator<List<AdBidder>> it = subsequences.iterator();
		long bi = 0;
		while (it.hasNext()) {
			List<AdBidder> lab = it.next();
			byte[] seqBytes = new byte[totalSlots];
			for (int k = 0; k < totalSlots; k++) {
				AdBidder der = lab.get(k);
				seqBytes[k] = bidderIndexMap.get(der);
			}
			
			t = System.currentTimeMillis();
			RotateMutator mu = new RotateMutator(seqBytes);
			byte[][] mutates = mu.mutate();

			for (byte[] mut: mutates) {
				bi++;
				Set<Integer> placed = new HashSet<Integer>();
				double prob = 1;
				for (int i = 0; i < totalSlots; i++) {
					byte b = mut[i];
					AdBidder cur = bidderArray[b];
					double total = 0;
					for (int l = 0; l < bidderArray.length; l++) {
						if (!placed.contains(l))
							total += bidderArray[l].getBidPower();
					}
					prob *= (cur.getBidPower() + 0.0) / total;
					placed.add((int)b);
				}
				// now we got the probability of one mutation
				for (int i = 0; i < totalSlots; i++) {
					byte b = mut[i]; // the id of AdBidder
					
//					AdBidder cur = per.get(i);
					double[] list = bidderChances[b];
					// this is the probs for one bidder in all slots
					if (list == null) {
						// init the chance array
						list = new double[totalSlots];
						bidderChances[b] = list;
					}
					
					list[i] = list[i] + prob;
				}
			}
		}

		System.out.println("total permuts: " + bi);
		double[] newBidderChancesInSlots = bidderChances[allBidders.size() - 1];
		List<Double> results = new java.util.ArrayList<Double>();
		for (double d: newBidderChancesInSlots) {
			results.add(d);
		}
		return results;
	}

	// public static double calcProbForElementInSlot(String ) {
	//
	// }
	/**
	 * the probability of pick up a value from a set of value candidates,
	 * assuming the value is in the set and all elements are integer strings
	 * 
	 * @param cur
	 * @param candiesLeft
	 * @return
	 */
	private static double getProb(AdBidder cur, HashSet<AdBidder> candiesLeft) {
		Iterator<AdBidder> i = candiesLeft.iterator();
		int total = 0;
		while (i.hasNext()) {
			AdBidder next = i.next();
			total += next.getBidPower();
		}
		return (cur.getBidPower() + 0.0) / total;
	}

	/**
	 * experiment permute
	 * 
	 * @param <T>
	 * @param list
	 * @param r
	 * @return
	 */
	public static <T> List<T> permute(List<T> list, int r) {
		int n = list.size();
		int f = fac(n);
		List<T> perm = new ArrayList<T>();
		list = new ArrayList<T>(list);
		for (list = new ArrayList<T>(list); n > 0; n--, r %= f) {
			f /= n;
			perm.add(list.remove(r / f));
		}
		return perm;
	}

	public static int fac(int n) {
		int f = 1;
		for (; n > 0; f *= n--)
			;
		return f;
	}
}
