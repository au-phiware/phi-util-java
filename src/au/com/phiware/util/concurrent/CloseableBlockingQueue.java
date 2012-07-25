package au.com.phiware.util.concurrent;

import java.util.concurrent.BlockingQueue;

public interface CloseableBlockingQueue<E> extends BlockingQueue<E> {
	/** Returns <tt>true</tt> if this queue is closed, <tt>false</tt> otherwise. */
	public boolean isClosed();

	/** Closes this queue; elements cannot be added to a closed queue. **/
	public void close();
}
