/**
 * 
 */
package cn.bran.play;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

public class CachedItemStatus implements Serializable{
	private static final int MIN_ALERT_ADVANCE = 1000;
	private long timein;
	private long ttl;
	private long safeBefore;
	//
	private AtomicBoolean isRefreshing  = new AtomicBoolean(false);
	private boolean expireSoon;

	public CachedItemStatus(long timein, long ttl) {
		super();
		this.timein = timein;
		this.ttl = ttl;
		long unsafeZone = (long) (ttl * (1 - RenderResultCache.SAFE_TIME_ZONE));
		if (unsafeZone < MIN_ALERT_ADVANCE) {
			// make a minimum 1s alert advance
			safeBefore = timein + ttl - MIN_ALERT_ADVANCE;
		}
		else {
			safeBefore = timein + ttl - unsafeZone;
		}
			
	}

	public CachedItemStatus(long ttl) {
		this(System.currentTimeMillis(), ttl);
	}

	boolean expireSoon() {
		return this.expireSoon ? true : (this.expireSoon = System.currentTimeMillis() > safeBefore);
	}

	public boolean isRefreshing() {
		return isRefreshing.get();
	}

	public void setIsRefreshing() {
		isRefreshing.set(true);
	}

	/**
	 * this one mutate the internal state of expiration flag if it will expire
	 * soon.
	 * 
	 * @return
	 */
	public boolean shouldRefresh() {
		if (expireSoon())
			return isRefreshing.compareAndSet(false, true);
		else
			return false;
	}

	public boolean isExpired() {
		return System.currentTimeMillis() > timein + ttl;
	}
}