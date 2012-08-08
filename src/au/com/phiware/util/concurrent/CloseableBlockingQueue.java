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

/**
 * A {@link BlockingQueue} that may be closed.
 *
 * The operations of a closeable queue have the following behaviours:
 * <p>As with the <tt>BlockingQueue</tt>, methods come in four forms, with
 *
 * one throws an exception, the second returns a special value (either
 * <tt>null</tt> or <tt>false</tt>, depending on the operation), the third
 * blocks the current thread indefinitely until the operation can succeed,
 * and the fourth blocks for only a given maximum time limit before giving
 * up.  These methods are summarized in the following table:
 *
 * <p>
 * <table BORDER CELLPADDING=3 CELLSPACING=1>
 *  <tr>
 *    <td></td>
 *    <td ALIGN=CENTER><em>Throws exception</em></td>
 *    <td ALIGN=CENTER><em>Special value</em></td>
 *    <td ALIGN=CENTER><em>Blocks</em></td>
 *    <td ALIGN=CENTER><em>Times out</em></td>
 *  </tr>
 *  <tr>
 *    <td rowspan="2"><b>Insert</b></td>
 *    <td>{@link #add add(e)}</td>
 *    <td>{@link #offer offer(e)}</td>
 *    <td>{@link #put put(e)}</td>
 *    <td>{@link #offer(Object, long, TimeUnit) offer(e, time, unit)}</td>
 *  </tr>
 *  <tr>
 *    <td colspan="2">Behaves like a full <tt>Queue</tt>*.</td>
 *    <td>Throws {@link QueueClosedException} if and when closed.</td>
 *    <td>Returns <tt>false</tt> if and when closed*.</td>
 *  </tr>
 *  <tr>
 *    <td rowspan="2"><b>Remove</b></td>
 *    <td>{@link #remove remove()}</td>
 *    <td>{@link #poll poll()}</td>
 *    <td>{@link #take take()}</td>
 *    <td>{@link #poll(long, TimeUnit) poll(time, unit)}</td>
 *  </tr>
 *  <tr>
 *    <td colspan="2">Behaves like an empty <tt>Queue</tt>*.</td>
 *    <td>Throws {@link QueueClosedException} if and when closed.</td>
 *    <td>Returns <tt>null</tt> if and when closed*.</td>
 *  </tr>
 *  <tr>
 *    <td><b>Examine</b></td>
 *    <td>{@link #element element()}</td>
 *    <td>{@link #peek peek()}</td>
 *    <td><em>not applicable</em></td>
 *    <td><em>not applicable</em></td>
 *  </tr>
 * </table>
 *
 * *It is caller's responsibility to test {@link #isClosed()} where {@link QueueClosedException} is not thrown.
 *
 * Implementations of {@link #drainTo(java.util.Collection)} may now also block.
 *
 * Additionally, this interface provides the ability to prevent a queue from closing.
 * If a thread has sent the a queue the {@link #preventClose()} message, any thread
 * calling {@link #close()} will block until a matching call to {@link #permitClose()}
 * has occurred.
 *
 * @author Corin Lawson <corin@phiware.com.au>
 * @param <E> the type of elements held in this collection
 *
 */
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
