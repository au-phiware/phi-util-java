package au.com.phiware.util.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public interface CloseableBlockingQueue<E> extends BlockingQueue<E> {
	/** Returns <tt>true</tt> if this queue is closed, <tt>false</tt> otherwise. */
	public boolean isClosed();

	/**
	 * Closes this queue; elements cannot be added to a closed queue.
	 * This method blocks until the queue is closed.
	 */
	public void close() throws InterruptedException;
	
	/**
	 * Attempts to close this queue, returns immediately if closure is prevented.
	 */
	public boolean tryClose();
	public boolean tryClose(long time, TimeUnit unit) throws InterruptedException;
	
	/**
	 * Ensures that this queue will not be closed
	 * until the current thread calls {@link #permitClose()}.
	 */
	public void preventClose();
	
	/**
	 * Must be called after a call to {@link #preventClose()} by the same thread. 
	 */
	public void permitClose();
}
