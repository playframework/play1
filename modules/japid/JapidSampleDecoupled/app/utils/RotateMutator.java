package utils;

public class RotateMutator {
	int totCount;
	byte[] original;
	byte[] prefix;
	byte[] suffix;
	byte[][] allperms;
	private int total;
	
	 void circularPrint(byte[] str) {

		int len = str.length;
		byte[] mystr = new byte[len];

		int i, j;

		for (i = 0; i < len; i++) {
			for (j = 0; j < len; j++)
				mystr[j] = str[(i + j) % len];
			allperms[totCount++] = mystr;
			mystr = new byte[len];
		}
		return;
	}

	public RotateMutator(byte[] src) {
		this.original = src;
		prefix = new byte[src.length - 1];
		System.arraycopy(src, 0, prefix, 0, src.length - 1);
		suffix = new byte[1];
		suffix[0] = src[src.length - 1];
		this.total = (int) factorial(src.length);
		allperms = new byte[total][];
	}

	void permutation(byte prefix[], byte suffix[]) {
		int length = suffix.length;
		int len = prefix.length;
		int i = 0, j = 0;

		if (len > 0) {
			byte[] myprefix = new byte[len - 1];
			System.arraycopy(prefix, 0, myprefix, 0, len - 1);

			for (i = 0; i < length; i++) {
				byte[] mysuffix = new byte[length + 1];
				mysuffix[0] = prefix[len - 1];

				// rotate the current append and prepare append for next
				for (j = 0; j < length; j++) {
					mysuffix[j + 1] = suffix[(i + j) % length];
				}
				permutation(myprefix, mysuffix);
			}
		} else {
			// found permutation, now find other
			// permutations by rotating the string
			circularPrint(suffix);
		}
	}

	public byte[][] mutate() {
		permutation(prefix, suffix);
		if (total != totCount) {
			throw new RuntimeException("totally wrong");
		}
		return allperms;
	}
	
	public static void main(String[] agrs) {
		// anything longer than 10 elements will  blow out
		byte[] src = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
//		String src = "1234567890";
		RotateMutator rotateMutator = new RotateMutator(src);
		long t1 = System.currentTimeMillis();
		byte[][] mutate = rotateMutator.mutate();
		long t2 = System.currentTimeMillis();
		
//		for (char[] ch: mutate) {
//			System.out.println(ch);
//		}
		System.out.println(mutate.length + " mutants took(ms): " + (t2 - t1) );
	}
	
	static long factorial(int n) {
		long result = 1;
		for (int i =0 ; i < n; i++) {
			result *= (n - i);
		}
		return result;
	}
	
}
