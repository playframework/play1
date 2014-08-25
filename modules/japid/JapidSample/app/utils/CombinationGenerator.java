package utils;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * this one generate all combinations and permutations of a specific length out of a bigger pool
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 *
 */
public class CombinationGenerator {
	Set<Object> total;
	
	
	public static void main(String[] args) {
		String[] at = new String[] {"a", "b", "c", "d"};
		HashSet t = new HashSet(Arrays.asList(at));
		new CombinationGenerator(t).enumerateAllHands(Integer.parseInt("3"));
	}

	public CombinationGenerator(Set total) {
		super();
		this.total = total;
	}

	private void enumerateAllHands(int n) {
		if (n > total.size()) {
			throw new IllegalArgumentException();
		}
		Object[] cards = new Object[n];
		Set cardsUsed = new HashSet();
		enumerate(cards, 0, cardsUsed);
	}

	private void enumerate(Object[] cards, int from, Set cardsUsed) {
		if (from == cards.length) {
			emit(cards);
		} else {
			Iterator<Object> it = total.iterator();
			while (it.hasNext()) {
				Object o = it.hasNext();

//			for (int i = 0; i < total; i++) {
				if (!cardsUsed.contains(o)) {
					cards[from] = o;
					cardsUsed.add(o);
					enumerate(cards, from + 1, cardsUsed);
					cardsUsed.remove(o);
				}
			}
		}
	}

	private static void emit(Object[] cards) {
		System.out.println(Arrays.toString(cards));
	}
	

}
