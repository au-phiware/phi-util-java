package au.com.phiware.util.concurrent;

import java.util.concurrent.BlockingQueue;

/**
 *
 * @author Corin Lawson <corin@phiware.com.au>
 */
public interface PausableBlockingQueue<E> extends BlockingQueue<E> {
	public Continue getContinue();
	public void setContinue(Continue cont);
}
