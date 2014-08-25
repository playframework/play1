package utils;

public class AdBidder {
	public String adId;
	/**
	 * pay per click, in cents.
	 */
	public int price;
	public int adQuality;
	private int bidPower;
	
	public AdBidder(String adId, int price, int adQuality) {
 		this.adId = adId;
		this.price = price;
		this.adQuality = adQuality;
		this.bidPower = price * adQuality;
	}
	public int getBidPower() {
		return bidPower;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Ad: " + adId + "/");
		sb.append(price + "/");
		sb.append(adQuality);
		return sb.toString();
	}
	
//	@Override
//	public int hashCode() {
//		return adId.hashCode();
//	}
//	@Override
//	public boolean equals(Object obj) {
//		if (obj instanceof AdBidder) {
//			return ((AdBidder)obj).adId.equals(adId);
//		}
//		return false;
//	}
	
}
