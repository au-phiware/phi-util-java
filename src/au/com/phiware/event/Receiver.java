package au.com.phiware.event;

/**
 * A simple receiver of over whatever you need. Think {@link com.google.common.eventbus.EventBus}!
 * @author Corin Lawson <corin@phiware.com.au>
 */
public interface Receiver {
	public void post(Object event);
}
