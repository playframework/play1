package utils;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import play.templates.JavaExtensions;

public class AdSpaceTest {
	@Test
	public void testAllPosibilities() {
		AdSpace space = initSpace();
		List<PermuteWithProbability> posi = space.getProbabilities();
		double pro = 0;
		for (PermuteWithProbability p: posi) {
			pro += p.probability;
//			System.out.println(p.toString());
		}
		System.out.println("total of pro: " + pro);
		assertTrue(Math.abs(1 - pro) < 0.00001);
	}

	@Test
	public void testNewBidder() {
		AdSpace space = initSpace();
		AdBidder ab = new AdBidder("bran", 10, 1);
		double[] tt = space.getNewBidderChances(ab);
		double shownAtAll = 0;
		int s = 1;
		for (double t :tt) {
			System.out.println("slot " + s++  + ": " + t);
			shownAtAll += t;
		}
		System.out.println("Chance of total exposure: " + shownAtAll);
	}

	@Test
	public void testNewBidderFast() {
		AdSpace space = initSpace();
		AdBidder ab = new AdBidder("bran", 10, 1);
		List<Double> tt = space.getNewBidderChancesFast(ab);
		double shownAtAll = 0;
		
		int s = 1;
		for (double t :tt) {
			System.out.println("slot " + s++  + ": " + t);
			shownAtAll += t;
		}
		System.out.println("Chance of total exposure: " + shownAtAll);
	}

	@Test
	public void testNewBidder3() {
		AdSpace space = initSpace();
		AdBidder ab = new AdBidder("bran", 10, 1);
		List<Double> tt = space.getNewBidderChances3(ab);
		double shownAtAll = 0;
		
		int s = 1;
		for (double t :tt) {
			System.out.println("slot " + s++  + ": " + t);
			shownAtAll += t;
		}
		System.out.println("Chance of total exposure: " + shownAtAll);
	}

	@Test
	public void testSub() {
		List<String> list = new ArrayList<String>() {
			{
				add("1");
				add("2");
				add("3");
				add("4");
				add("5");
				add("6");
				add("7");
				add("8");
				add("9");
				add("10");
				add("11");
				add("12");
				add("13");
				add("14");
				add("15");
			}
		};
		
		Set<List<String>> sub = AdSpace.subsequences(list, 3);
		BigInteger combinationsCount = PermutationGenerator.getCombinationsCount(list.size(), 3);
		int parseInt = Integer.parseInt(combinationsCount.toString());
		System.out.println("total count: " + parseInt);
		assertEquals(parseInt, sub.size());
//		Iterator<List<String>> it = sub.iterator();
//		while (it.hasNext()) {
//			List<String> l = it.next();
//			System.out.println(l.toString());
//		}
	}
	
	private AdSpace initSpace() {
		int totalSlots = 5; // 6 or more will become very slow!
		List<AdBidder> bidders = new ArrayList<AdBidder>();
		bidders.add(new AdBidder("ad1",1, 1));
		bidders.add(new AdBidder("ad2",2, 1));
		bidders.add(new AdBidder("ad3",3, 1));
		bidders.add(new AdBidder("ad4",4, 1));
		bidders.add(new AdBidder("ad5",5, 1));
		bidders.add(new AdBidder("ad6",6, 1));
		bidders.add(new AdBidder("ad7",7, 1));
		bidders.add(new AdBidder("ad8",8, 1));
		bidders.add(new AdBidder("ad9",9, 1));
		bidders.add(new AdBidder("ad10",10, 1));
		bidders.add(new AdBidder("ad11", 11, 1));
		bidders.add(new AdBidder("ad12", 12, 1));
		bidders.add(new AdBidder("ad13", 13, 1));
		bidders.add(new AdBidder("ad14", 14, 1));
		bidders.add(new AdBidder("ad15", 15, 1));
		AdSpace space = new AdSpace(totalSlots, bidders);
		return space;
	}

	private AdSpace initSpaceWithBiddersLessThanSplots() {
		int totalSlots = 4;
		List<AdBidder> bidders = new ArrayList<AdBidder>();
		bidders.add(new AdBidder("ad1",1, 1));
		bidders.add(new AdBidder("ad5", 5, 1));
		AdSpace space = new AdSpace(totalSlots, bidders);
		return space;
	}
	
	@Test
	public void testSpaceWithFewBidders() {
		AdSpace sp = initSpaceWithBiddersLessThanSplots();
		List<PermuteWithProbability> probabilities = sp.getProbabilities();
		double pro = 0;
		for (PermuteWithProbability p: probabilities) {
			pro += p.probability;
			System.out.println(p.toString());
		}
		System.out.println("total of pro: " + pro);
		assertTrue(Math.abs(1 - pro) < 0.00001);
		
		AdBidder adBidder = new AdBidder("ad2",2, 1);
		double[] chances = sp.getNewBidderChances(adBidder);
		int s = 0;
		double shownAtAll = 0;
		for (double t :chances) {
			System.out.println("slot " + s++  + ": " + t);
			shownAtAll += t;
		}
		System.out.println("Chance of any exposure: " + shownAtAll);
	}
	
	public static void main(String[] args) {
		long t = System.currentTimeMillis();
		new AdSpaceTest().testNewBidderFast();
		t = System.currentTimeMillis()  - t;
		System.out.println("took/ms: " + t);
	}
	
	@Test
	public void testEscape() {
		String str = "<div>\"hello\"</div>";
		String esc = JavaExtensions.escapeJavaScript(str);
		System.out.println(esc);
		
	}
	
	
}
