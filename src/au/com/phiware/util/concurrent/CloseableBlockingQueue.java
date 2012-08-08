/*
 *  QueueClosedException.java
 *  Copyright (C) 2012  Corin Lawson
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
