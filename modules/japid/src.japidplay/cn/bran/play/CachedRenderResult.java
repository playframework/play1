package cn.bran.play;

import java.io.Serializable;

import cn.bran.japid.template.RenderResult;

/**
 * bind a RenderResult with a cache status
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 *
 */
public class CachedRenderResult implements Serializable{
	public CachedItemStatus status;
	public RenderResult rr;
	public CachedRenderResult(CachedItemStatus status, RenderResult rr) {
		super();
		this.status = status;
		this.rr = rr;
	}
	public boolean isExpired() {
		return status.isExpired();
	}
	
}
