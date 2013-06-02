package au.com.phiware.event;

/**
 * Posts events to a {@link Receiver}.
 * @author Corin Lawson <corin@phiware.com.au>
 */
public interface Emitter {
    public Receiver getEventReceiver();
	public void setEventReceiver(Receiver eventReceiver);
}
