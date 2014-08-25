package utils;

import java.util.List;

public class PermuteWithProbability {
	public List<AdBidder> permute;
	public double probability;
	public PermuteWithProbability(List<AdBidder> permute, double probability) {
		super();
		this.permute = permute;
		this.probability = probability;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (AdBidder a : permute) {
			sb.append(a.toString() + "/");
		}
		sb.append("probability: " + probability);
		return sb.toString();
	}
}
