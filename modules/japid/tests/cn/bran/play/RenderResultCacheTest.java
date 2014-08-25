package cn.bran.play;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import cn.bran.japid.template.RenderResult;

public class RenderResultCacheTest {
	private static final String KEY1 = "key1";

	@Test
	public void testSimpleExpiration() throws ShouldRefreshException {
		RenderResultCache.setAltCache(new AltCacheSimpleImpl());
		RenderResult rr = new RenderResult(null, null, 0);
		RenderResultCache.set(KEY1, rr, "2s");
		RenderResult rrr;
		rrr = RenderResultCache.get(KEY1);
		assertNotNull(rrr);
		waitfor(3000);
		rrr = RenderResultCache.get(KEY1);
		assertNull(rrr);
	}

	@Test
	public void testIgnoreCacheSetting() throws ShouldRefreshException {
		assertFalse(RenderResultCache.shouldIgnoreCache());
		RenderResultCache.setIgnoreCache(true);
		assertTrue(RenderResultCache.shouldIgnoreCache());
		final AtomicBoolean b = new AtomicBoolean(false);
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
					b.set(!RenderResultCache.shouldIgnoreCache());
			}
		});
		t.start();

		waitfor(120);
		assertTrue(b.get());
		RenderResultCache.setIgnoreCache(false);
	}

	@Test
	public void testReadThruWithThread() throws ShouldRefreshException {
		assertFalse(RenderResultCache.shouldIgnoreCache());
		RenderResultCache.setAltCache(new AltCacheSimpleImpl());
		RenderResult rr = new RenderResult(null, null, 0);
		RenderResultCache.set(KEY1, rr, "4s");
		RenderResultCache.setIgnoreCache(true);
		RenderResult rrr = RenderResultCache.get(KEY1);
		assertNull(rrr);

		final AtomicBoolean b = new AtomicBoolean(false);

		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					RenderResult rrr = RenderResultCache.get(KEY1);
					b.set(rrr != null);
				} catch (ShouldRefreshException e) {
					fail();
				}
				
			}
		});
		t.start();
		
		waitfor(100);
		assertTrue(b.get());
		RenderResultCache.setIgnoreCache(false);
	}

	@Test
	public void testExpirationSoon() {
		RenderResultCache.setAltCache(new AltCacheSimpleImpl());
		RenderResult rr = new RenderResult(null, null, 0);
		RenderResultCache.set(KEY1, rr, "2s");
		RenderResult rrr;
		try {
			rrr = RenderResultCache.get(KEY1);
			assertNotNull(rrr);
		} catch (ShouldRefreshException e1) {
			throw new RuntimeException(e1);
		}

		waitfor(1500);
		try {
			rrr = RenderResultCache.get(KEY1);
		} catch (ShouldRefreshException e) {
			assertNotNull(e.renderResult);
		}

		final AtomicBoolean b = new AtomicBoolean(false);
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					RenderResult rrr = RenderResultCache.get(KEY1);
					b.set(rrr != null);
				} catch (ShouldRefreshException e) {
					fail();
				}
				
			}
		});
		t.start();
		waitfor(100);
		assertTrue(b.get());

		// the second time in refreshing zone should get the item
		try {
			rrr = RenderResultCache.get(KEY1);
			assertNotNull(rrr);
		} catch (ShouldRefreshException e) {
			fail("should not get this");
		}

	}

	@Test
	public void test11sconds() {
		RenderResultCache.setAltCache(new AltCacheSimpleImpl());
		RenderResult rr = new RenderResult(null, null, 0);
		RenderResultCache.set(KEY1, rr, "11s");
		RenderResult rrr;
		try {
			rrr = RenderResultCache.get(KEY1);
			assertNotNull(rrr);
		} catch (ShouldRefreshException e1) {
			throw new RuntimeException(e1);
		}

		System.out.println("let wait for 9 secs");
		waitfor(9000);

		try {
			rrr = RenderResultCache.get(KEY1);
			assertNotNull(rrr);
		} catch (ShouldRefreshException e) {
			fail("should be safe");
		}

		waitfor(1000);
		// the second time in refreshing zone should get the item
		try {
			rrr = RenderResultCache.get(KEY1);
			fail("should alert expiration");
		} catch (ShouldRefreshException e) {
			try {
				rrr = RenderResultCache.get(KEY1);
				assertNotNull(rrr);
			} catch (ShouldRefreshException e1) {
				fail("should alert expiration ONCE");
			}
		}

	}

	/**
	 * @param i
	 * 
	 */
	private void waitfor(int i) {
		try {
			Thread.sleep(i); // 
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
