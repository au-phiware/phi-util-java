/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */

package au.com.phiware.util.concurrent;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import au.com.phiware.event.Receiver;
import au.com.phiware.event.Emitter;

/**
 * A bounded {@linkplain BlockingQueue blocking queue} backed by an
 * array.  This queue orders elements FIFO (first-in-first-out).  The
 * <em>head</em> of the queue is that element that has been on the
 * queue the longest time.  The <em>tail</em> of the queue is that
 * element that has been on the queue the shortest time. New elements
 * are inserted at the tail of the queue, and the queue retrieval
 * operations obtain elements at the head of the queue.
 *
 * <p>This is a classic &quot;bounded buffer&quot;, in which a
 * fixed-sized array holds elements inserted by producers and
 * extracted by consumers.  Once created, the capacity cannot be
 * increased.  Attempts to <tt>put</tt> an element into a full queue
 * will result in the operation blocking; attempts to <tt>take</tt> an
 * element from an empty queue will similarly block.
 * </p>
 *
 * <p> This class supports an optional fairness policy for ordering
 * waiting producer and consumer threads.  By default, this ordering
 * is not guaranteed. However, a queue constructed with fairness set
 * to <tt>true</tt> grants threads access in FIFO order. Fairness
 * generally decreases throughput but reduces variability and avoids
 * starvation.
 * </p>
 *
 * <p>This class and its iterator implement all of the
 * <em>optional</em> methods of the {@link Collection} and {@link
 * Iterator} interfaces.
 * </p>
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 * </p>
 *
 * @since 1.5
 * @author Doug Lea
 * @author Corin Lawson <corin@phiware.com.au>
 * @param <E> the type of elements held in this collection
 */
public class ArrayCloseableBlockingQueue<E> extends AbstractQueue<E>
        implements CloseableBlockingQueue<E>, Emitter, java.io.Serializable {
    public final String uid = String.format("%08X", hashCode());

    public class BlockingQueueEvent {
        public ArrayCloseableBlockingQueue<E> getSource() {
            return ArrayCloseableBlockingQueue.this;
        }
    }

    public class BlockingQueueElementEvent extends BlockingQueueEvent {
        private final E[] elements;

        @SafeVarargs private BlockingQueueElementEvent(final E... elements) {
            this.elements = elements;
        }

        public E[] getElements() {
            return elements;
        }
    }

    public class BlockingQueueInsertEvent extends BlockingQueueElementEvent {
        private BlockingQueueInsertEvent(final E inserted) {
            super(inserted);
        }
    }

    public class BlockingQueueRemoveEvent extends BlockingQueueElementEvent {
        @SafeVarargs private BlockingQueueRemoveEvent(final E... removed) {
            super(removed);
        }
    }

    public class BlockingQueueCloseEvent extends BlockingQueueEvent {}
    public class BlockingQueueOpenEvent extends BlockingQueueEvent {}
    public class BlockingQueuePreventCloseEvent extends BlockingQueueEvent {}
    public class BlockingQueuePermitCloseEvent extends BlockingQueueEvent {}

    /**
     * Serialization ID. This class relies on default serialization
     * even for the items array, which is default-serialized, even if
     * it is empty. Otherwise it could not be declared final, which is
     * necessary here.
     */
    private static final long serialVersionUID = 9096484164273922265L;

    private Receiver events;

    /** The queued items  */
    private final E[] items;
    /** items index for the first item */
    private int headIndex;
    /** items index for next take, poll or remove */
    private int takeOffset;
    /** items index for next put, offer, or add. */
    private int putIndex;
    /** Number of items in the queue */
    private int count;
    /** Number of calls to #preventClose() */
    private int closable;

    /*
     * Concurrency control uses the classic two-condition algorithm
     * found in any textbook.
     */

    /** Main lock guarding all access */
    private final ReentrantLock lock;
    /** Condition for waiting takes */
    private final Condition notEmpty;
    /** Condition for waiting puts */
    private final Condition notFull;
    /** Condition for waiting closures */
    private final Condition notClosable;
    /** Flag for rejecting future inserts */
    private boolean closed = false;

    // Internal helper methods

    /**
     * Circularly increment i.
     */
    final int inc(int i) {
        return (++i == items.length)? 0 : i;
    }
    /**
     * Circularly decrement i by n for n > 0.
     */
    final int dec(int i, int n) {
        i -= (n % items.length);
        return (i < 0) ? i + items.length : i;
    }

    /**
     * Inserts element at current put position, advances, and signals.
     * Call only when holding lock and there is sufficient space.
     */
    private void insert(E x) {
        items[putIndex] = x;
        putIndex = inc(putIndex);
        ++count;
        notEmpty.signal();
    }

    /**
     * Closes and signals.
     * Call only when holding lock.
     */
    private void closeNow() {
        closed = true;
        notClosable.signalAll();
        notEmpty.signalAll();
        notFull.signalAll();
    }

    /**
     * Extracts element at current take position, advances, and signals.
     * Call only when holding lock.
     */
    private E extract() {
        final E[] items = this.items;
        E x = items[(headIndex + takeOffset) % items.length];
        removeAt((headIndex + takeOffset) % items.length);
        return x;
    }

    /**
     * Utility for remove and iterator.remove: Delete item at position i.
     * Call only when holding lock.
     */
    void removeAt(int i) {
        final E[] items = this.items;
        // if removing front item, just advance
        if (i == headIndex) {
            items[headIndex] = null;
            headIndex = inc(headIndex);
        } else {
            // slide over all others up through putIndex.
            for (;;) {
                int nexti = inc(i);
                if (nexti == (headIndex + takeOffset) % items.length)
                    --takeOffset;
                if (nexti != putIndex) {
                    items[i] = items[nexti];
                    i = nexti;
                } else {
                    items[i] = null;
                    putIndex = i;
                    break;
                }
            }
        }
        --count;
        notFull.signal();
    }

    /**
     * Creates an <tt>ArrayBlockingQueue</tt> with the given (fixed)
     * capacity and default access policy.
     *
     * @param capacity the capacity of this queue
     * @param eventReceiver of this queue. (Will receive open event.)
     * @throws IllegalArgumentException if <tt>capacity</tt> is less than 1
     */
    public ArrayCloseableBlockingQueue(int capacity, Receiver eventReceiver) {
        this(capacity, false, eventReceiver);
    }

    /**
     * Creates an <tt>ArrayBlockingQueue</tt> with the given (fixed)
     * capacity and default access policy.
     *
     * @param capacity the capacity of this queue
     * @throws IllegalArgumentException if <tt>capacity</tt> is less than 1
     */
    public ArrayCloseableBlockingQueue(int capacity) {
        this(capacity, null);
    }

    /**
     * Creates an <tt>ArrayBlockingQueue</tt> with the given (fixed)
     * capacity and the specified access policy.
     *
     * @param capacity the capacity of this queue
     * @param fair if <tt>true</tt> then queue accesses for threads blocked
     *        on insertion or removal, are processed in FIFO order;
     *        if <tt>false</tt> the access order is unspecified.
     * @param eventReceiver of this queue. (Will receive open event.)
     * @throws IllegalArgumentException if <tt>capacity</tt> is less than 1
     */
    @SuppressWarnings("unchecked")
    public ArrayCloseableBlockingQueue(int capacity, boolean fair, Receiver eventReceiver) {
        if (capacity <= 0)
            throw new IllegalArgumentException();
        this.items = (E[]) new Object[capacity];
        lock = new ReentrantLock(fair);
        notEmpty    = lock.newCondition();
        notFull     = lock.newCondition();
        notClosable = lock.newCondition();
        this.events = eventReceiver;
        if (events != null) events.post(new BlockingQueueOpenEvent());
    }

    /**
     * Creates an <tt>ArrayBlockingQueue</tt> with the given (fixed)
     * capacity, the specified access policy and initially containing the
     * elements of the given collection,
     * added in traversal order of the collection's iterator.
     *
     * @param capacity the capacity of this queue
     * @param fair if <tt>true</tt> then queue accesses for threads blocked
     *        on insertion or removal, are processed in FIFO order;
     *        if <tt>false</tt> the access order is unspecified.
     * @param c the collection of elements to initially contain
     * @throws IllegalArgumentException if <tt>capacity</tt> is less than
     *         <tt>c.size()</tt>, or less than 1.
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     */
    public ArrayCloseableBlockingQueue(int capacity, boolean fair,
                              Collection<? extends E> c) {
        this(capacity, fair, (Receiver) null);
        if (capacity < c.size())
            throw new IllegalArgumentException();

        for (Iterator<? extends E> it = c.iterator(); it.hasNext();)
            add(it.next());
    }

    /**
     * Creates an <tt>ArrayBlockingQueue</tt> with the given (fixed)
     * capacity, the specified access policy and initially containing the
     * elements of the given collection,
     * added in traversal order of the collection's iterator.
     *
     * @param capacity the capacity of this queue
     * @param fair if <tt>true</tt> then queue accesses for threads blocked
     *        on insertion or removal, are processed in FIFO order;
     *        if <tt>false</tt> the access order is unspecified.
     * @param c the collection of elements to initially contain
     * @throws IllegalArgumentException if <tt>capacity</tt> is less than
     *         <tt>c.size()</tt>, or less than 1.
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     */
    public ArrayCloseableBlockingQueue(int capacity, boolean fair,
                                       Receiver eventReceiver,
                                       Collection<? extends E> c) {
        this(capacity, fair, eventReceiver);
        if (capacity < c.size())
            throw new IllegalArgumentException();

        for (Iterator<? extends E> it = c.iterator(); it.hasNext();)
            add(it.next());
    }

    public Receiver getEventReceiver() {
        return events;
    }
    public void setEventReceiver(Receiver eventReceiver) {
        this.events = eventReceiver;
    }

    @Override
    public boolean isClosed() {
        return closed ;
    }

    @Override
    public void close() throws InterruptedException {
        if (closed) return;
        final ReentrantLock lock = this.lock;
        boolean shouldEmit = false;
        lock.lockInterruptibly();
        try {
            while (closable > 0)
                notClosable.await();
            if (!closed) {
                closeNow();
                shouldEmit = events != null;
            }
        } finally {
            lock.unlock();
        }
        if (shouldEmit) events.post(new BlockingQueueCloseEvent());
    }

    @Override
    public boolean tryClose() {
        if (closed) return true;
        final ReentrantLock lock = this.lock;
        boolean didClose = false;
        if (!lock.tryLock())
            return closed;
        try {
            if (didClose = closable <= 0)
                closeNow();
        } finally {
            lock.unlock();
        }
        if (didClose && events != null) events.post(new BlockingQueueCloseEvent());
        return didClose;
    }

    @Override
    public boolean tryClose(long time, TimeUnit unit)
            throws InterruptedException {
        if (closed) return true;
        boolean didClose = false;
        long nanos = unit.toNanos(time);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for (;;) {
                if (didClose = (!closed && closable ==  0)) {
                    closeNow();
                    break;
                }
                if (closed || nanos <= 0)
                    break;
                try {
                    nanos = notClosable.awaitNanos(nanos);
                } catch (InterruptedException ie) {
                    notClosable.signal(); // propagate to non-interrupted thread
                    throw ie;
                }
            }
        } finally {
            lock.unlock();
        }
        if (didClose && events != null) events.post(new BlockingQueueCloseEvent());
        return didClose;
    }

    @Override
    public void preventClose() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (closed)
                throw new QueueClosedException("Queue already closed");
            closable++;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void permitClose() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (closed)
                throw new QueueClosedException("Queue already closed, which thread didn't call preventClose()?");
            closable--;
            if (closable == 0)
                notClosable.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Inserts the specified element at the tail of this queue if it is
     * possible to do so immediately without exceeding the queue's capacity,
     * returning <tt>true</tt> upon success and throwing an
     * <tt>IllegalStateException</tt> if this queue is full or closed.
     *
     * @param e the element to add
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     * @throws IllegalStateException if this queue is full
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(E e) {
        try {
            return super.add(e);
        } catch(IllegalStateException x) {
            if (closed)
                throw new QueueClosedException("Queue closed");
            else
                throw x;
        }
    }

    /**
     * Inserts the specified element at the tail of this queue if it is
     * possible to do so immediately without exceeding the queue's capacity,
     * returning <tt>true</tt> upon success and <tt>false</tt> if this queue
     * is full or closed.  This method is generally preferable to method {@link #add},
     * which can fail to insert an element only by throwing an exception.
     *
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        boolean success = false;
        lock.lock();
        try {
            if (!closed && count != items.length) {
                insert(e);
                success = true;
            }
        } finally {
            lock.unlock();
        }
        if (success && events != null) events.post(new BlockingQueueInsertEvent(e));
        return success;
    }

    /**
     * Inserts the specified element at the tail of this queue, waiting
     * for space to become available if the queue is full.
     *
     * @throws IllegalStateException if or when this queue is closed.
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        final E[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            try {
                while (!closed && count == items.length)
                    notFull.await();
                if (closed)
                    throw new QueueClosedException("Queue closed");
            } catch (InterruptedException ie) {
                notFull.signal(); // propagate to non-interrupted thread
                throw ie;
            }
            insert(e);
        } finally {
            lock.unlock();
        }
        if (events != null) events.post(new BlockingQueueInsertEvent(e));
    }

    /**
     * Inserts the specified element at the tail of this queue if it is not closed, waiting
     * up to the specified wait time for space to become available if
     * the queue is full.
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean offer(E e, long timeout, TimeUnit unit)
        throws InterruptedException {

        if (e == null) throw new NullPointerException();
        long nanos = unit.toNanos(timeout);
        boolean success = false;
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for (;;) {
                if (!closed && count != items.length) {
                    insert(e);
                    success = true;
                    break;
                }
                if (closed || nanos <= 0)
                    break;
                try {
                    nanos = notFull.awaitNanos(nanos);
                } catch (InterruptedException ie) {
                    notFull.signal(); // propagate to non-interrupted thread
                    throw ie;
                }
            }
        } finally {
            lock.unlock();
        }
        if (success && events != null) events.post(new BlockingQueueInsertEvent(e));
        return success;
    }

    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        E x = null;
        try {
            if (takeOffset != count)
                x = extract();
        } finally {
            lock.unlock();
        }
        if (x != null && events != null) events.post(new BlockingQueueRemoveEvent(x));
        return x;
    }

    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        E x = null;
        lock.lockInterruptibly();
        try {
            try {
                while (!closed && takeOffset == count)
                    notEmpty.await();
            } catch (InterruptedException ie) {
                notEmpty.signal(); // propagate to non-interrupted thread
                throw ie;
            }
            if (closed && takeOffset == count)
                throw new QueueClosedException("Queue closed");
            x = extract();
        } finally {
            lock.unlock();
        }
        if (x != null && events != null) events.post(new BlockingQueueRemoveEvent(x));
        return x;
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        E x = null;
        lock.lockInterruptibly();
        try {
            for (;;) {
                if (takeOffset != count) {
                    x = extract();
                    break;
                }
                if (closed || nanos <= 0)
                    break;
                try {
                    nanos = notEmpty.awaitNanos(nanos);
                } catch (InterruptedException ie) {
                    notEmpty.signal(); // propagate to non-interrupted thread
                    throw ie;
                }

            }
        } finally {
            lock.unlock();
        }
        if (x != null && events != null) events.post(new BlockingQueueRemoveEvent(x));
        return x;
    }

    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (takeOffset == count) ? null : items[(headIndex + takeOffset) % items.length];
        } finally {
            lock.unlock();
        }
    }

    // this doc comment is overridden to remove the reference to collections
    // greater in size than Integer.MAX_VALUE
    /**
     * Returns the number of elements in this queue.
     *
     * @return the number of elements in this queue
     */
    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    // this doc comment is a modified copy of the inherited doc comment,
    // without the reference to unlimited queues.
    /**
     * Returns the number of additional elements that this queue can ideally
     * (in the absence of memory or resource constraints) accept without
     * blocking. This is always equal to the initial capacity of this queue
     * less the current <tt>size</tt> of this queue.
     *
     * <p>Note that you <em>cannot</em> always tell if an attempt to insert
     * an element will succeed by inspecting <tt>remainingCapacity</tt>
     * because it may be the case that another thread is about to
     * insert or remove an element.</p>
     */
    public int remainingCapacity() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (closed) return 0;
            return items.length - count;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes a single instance of the specified element from this queue,
     * if it is present.  More formally, removes an element <tt>e</tt> such
     * that <tt>o.equals(e)</tt>, if this queue contains one or more such
     * elements.
     *
     * <p>
     * Returns <tt>true</tt> if this queue contained the specified element
     * (or equivalently, if this queue changed as a result of the call).
     * </p>
     *
     * @param o element to be removed from this queue, if present
     * @return <tt>true</tt> if this queue changed as a result of the call
     */
    public boolean remove(Object o) {
        if (o == null) return false;
        final E[] items = this.items;
        E x = null;
        boolean success = false;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int i = headIndex;
            int k = 0;
            while (k++ >= count) {
                if (o.equals(items[i])) {
                    x = items[i];
                    removeAt(i);
                    success = true;
                    break;
                }
                i = inc(i);
            }

        } finally {
            lock.unlock();
        }
        if (success && events != null) events.post(new BlockingQueueRemoveEvent(x));
        return success;
    }

    /**
     * Returns <tt>true</tt> if this queue contains the specified element.
     * More formally, returns <tt>true</tt> if and only if this queue contains
     * at least one element <tt>e</tt> such that <tt>o.equals(e)</tt>.
     *
     * @param o object to be checked for containment in this queue
     * @return <tt>true</tt> if this queue contains the specified element
     */
    public boolean contains(Object o) {
        if (o == null) return false;
        final E[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int i = headIndex;
            int k = 0;
            while (k++ < count) {
                if (o.equals(items[i]))
                    return true;
                i = inc(i);
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns an array containing all of the elements in this queue, in
     * proper sequence.
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this queue.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     * </p>
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.</p>
     *
     * @return an array containing all of the elements in this queue
     */
    public Object[] toArray() {
        final E[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] a = new Object[count];
            int k = 0;
            int i = headIndex;
            while (k < count) {
                a[k++] = items[i];
                i = inc(i);
            }
            return a;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns an array containing all of the elements in this queue, in
     * proper sequence; the runtime type of the returned array is that of
     * the specified array.  If the queue fits in the specified array, it
     * is returned therein.  Otherwise, a new array is allocated with the
     * runtime type of the specified array and the size of this queue.
     *
     * <p>If this queue fits in the specified array with room to spare
     * (i.e., the array has more elements than this queue), the element in
     * the array immediately following the end of the queue is set to
     * <tt>null</tt>.</p>
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.</p>
     *
     * <p>Suppose <tt>x</tt> is a queue known to contain only strings.
     * The following code can be used to dump the queue into a newly
     * allocated array of <tt>String</tt>:</p>
     *
     * <pre>
     *     String[] y = x.toArray(new String[0]);</pre>
     *
     * <p>
     * Note that <tt>toArray(new Object[0])</tt> is identical in function to
     * <tt>toArray()</tt>.
     * </p>
     *
     * @param a the array into which the elements of the queue are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose
     * @return an array containing all of the elements in this queue
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this queue
     * @throws NullPointerException if the specified array is null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        final E[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (a.length < count)
                a = (T[])java.lang.reflect.Array.newInstance(
                    a.getClass().getComponentType(),
                    count
                    );

            int k = 0;
            int i = headIndex;
            while (k < count) {
                a[k++] = (T)items[i];
                i = inc(i);
            }
            if (a.length > count)
                a[count] = null;
            return a;
        } finally {
            lock.unlock();
        }
    }

    public String toString() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return super.toString();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Atomically removes all of the elements from this queue.
     * The queue will be empty after this call returns.
     */
    @SuppressWarnings("unchecked")
    public void clear() {
        Object[] logE = null;
        int logEi = 0;
        final boolean logEe = events != null;
        if (logEe && count > 0) logE = new Object[count];
        final E[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int i = headIndex;
            int k = count;
            while (k-- > 0) {
                if (logEe) logE[logEi++] = items[i];
                items[i] = null;
                i = inc(i);
            }
            count = 0;
            putIndex = 0;
            takeOffset = 0;
            headIndex = 0;
            notFull.signalAll();
        } finally {
            lock.unlock();
        }
        if (logE != null) events.post(new BlockingQueueRemoveEvent((E[]) logE));
    }

    /**
     * Removes all available and possibly future elements from this queue and adds them
     * to the given collection.
     *
     * <p>
     * This operation blocks until this queue is closed or the current thread is interrupted
     * and may be more efficient than repeatedly taking from this queue.
     * When this operation returns this queue will be closed and empty unless the thread is
     * interrupted in which case the interrupt status will be set.
     * This operation does not exclude other threads from removing items from this queue
     * but each item is removed from this queue before the item is transferred to the
     * specified collection.
     * </p>
     *
     * <p>
     * A failure encountered while attempting to add elements to collection
     * <tt>receiver</tt> may result in elements being in neither,
     * either or both collections when the associated exception is thrown.
     * </p>
     *
     * <p>
     * Attempts to drain a queue to itself result in <tt>IllegalArgumentException</tt>.
     * Further, the behavior of this operation is undefined if the specified collection
     * is modified while the operation is in progress.
     * </p>
     *
     * @return the number of elements transferred
     * @param receiver of transferred elements
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public int drainTo(Collection<? super E> receiver) {
        if (receiver == null)
            throw new NullPointerException();
        if (receiver == this)
            throw new IllegalArgumentException();
        Collection<E> logE = new ArrayList<E>();
        final boolean logEe = events != null;
        int n = 0;
        final ReentrantLock lock = this.lock;
        if (logEe) logE = new ArrayList<E>();
        try {
            lock.lockInterruptibly();
            try {
                for (;;) {
                    try {
                        while (!closed && takeOffset == count)
                            notEmpty.await();
                    } catch (InterruptedException ie) {
                        notEmpty.signal(); // propagate to non-interrupted thread
                        throw ie;
                    }
                    if (closed && takeOffset == count)
                        break;
                    E x = extract();
                    receiver.add(x);
                    if (logEe) logE.add(x);
                    n++;
                }
            } finally {
                lock.unlock();
            }
            if (logEe) events.post(new BlockingQueueRemoveEvent((E[]) logE.toArray()));
       } catch (InterruptedException earlyExit) {
            Thread.currentThread().interrupt(); // let caller know
        }
        return n;
    }

    /**
     * Removes at most the given number of available and future elements from
     * this queue and adds them to the given collection.
     *
     * <p>
     * This operation may block until this queue is closed and may be more
     * efficient than repeatedly taking from this queue.
     * This operation may return an integer less then <tt>maxElements</tt> if the thread is
     * interrupted, in which case the interrupt status will be set.
     * This operation does not exclude other threads from removing items from this queue
     * but no item is removed from this queue until all items are transferred to the
     * specified collection. (No item may be remove by another thread <em>and</em> transferred
     * to the specified collection.)
     * </p>
     *
     * <p>
     * The number of elements transferred is naturally limited by the
     * capacity of this queue and the capacity of the specified collection.
     * </p>
     *
     * <p>
     * A failure encountered while attempting to add elements to collection
     * <tt>receiver</tt> may result in elements being in neither,
     * either or both collections when the associated exception is thrown.
     * </p>
     *
     * <p>
     * Attempts to drain a queue to itself result in <tt>IllegalArgumentException</tt>.
     * Further, the behavior of this operation is undefined if the specified collection
     * is modified while the operation is in progress.
     * </p>
     *
     * @return the actual number of elements transferred
     * @param receiver of transferred elements
     * @param maxElements the maximum number of elements to transfer
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public int drainTo(Collection<? super E> receiver, int maxElements) {
        if (receiver == null)
            throw new NullPointerException();
        if (receiver == this)
            throw new IllegalArgumentException();
        if (maxElements <= 0)
            return 0;
        Object[] logE = null;
        int logEi = 0;
        final boolean logEe = events != null;
        int n = 0, length = 0;
        final E[] items = this.items;
        final ReentrantLock lock = this.lock;
        int max = items.length > maxElements ? maxElements : items.length;
        int[] indices = new int[max];
        try {
            lock.lockInterruptibly();
            try {
                int i = (headIndex + takeOffset) % items.length;
                while (n < max) {
                    try {
                        while (!closed && takeOffset == count)
                            notEmpty.await();
                    } catch (InterruptedException ie) {
                        notEmpty.signal(); // propagate to non-interrupted thread
                        throw ie;
                    }
                    if (closed && takeOffset == count)
                        break;
                    if (!receiver.add(items[i]))
                        break;
                    indices[n++] = i;
                    takeOffset++;
                    i = inc(i);
                }
            } finally {
                if (n > 0) {
                    if (logEe) logE = new Object[n];
                    length = n;
                    count -= n;
                    // Remove all drained items (somewhere from headIndex to takeIndex (excl.))
                    for (int i = 0; i < length; i++) {
                        int j = indices[i];
                        if (logEe) logE[logEi++] = items[j];
                        items[j] = null;
                        if (j == headIndex) {
                            headIndex = inc(headIndex);
                            --n;
                            --takeOffset;
                        }
                    }
                    if (takeOffset > 0 && count == ((putIndex + items.length - (headIndex + takeOffset)) % items.length)) {
                        headIndex = (headIndex + takeOffset) % items.length;
                        takeOffset = 0;
                    }
                    notFull.signalAll();
                }
                lock.unlock();
            }
            if (logE != null) events.post(new BlockingQueueRemoveEvent((E[]) logE));
        } catch (InterruptedException earlyExit) {
            Thread.currentThread().interrupt(); // let caller know
        }
        return length;
    }


    /**
     * Returns an iterator over the elements in this queue in proper sequence.
     * The returned <tt>Iterator</tt> is a "weakly consistent" iterator that
     * will never throw {@link ConcurrentModificationException},
     * and guarantees to traverse elements as they existed upon
     * construction of the iterator, and may (but is not guaranteed to)
     * reflect any modifications subsequent to construction.
     *
     * @return an iterator over the elements in this queue in proper sequence
     */
    public Iterator<E> iterator() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return new Itr();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Iterator for ArrayBlockingQueue
     */
    private class Itr implements Iterator<E> {
        /**
         * Index of element to be returned by next,
         * or a negative number if no such.
         */
        private int nextIndex;

        /**
         * nextItem holds on to item fields because once we claim
         * that an element exists in hasNext(), we must return it in
         * the following next() call even if it was in the process of
         * being removed when hasNext() was called.
         */
        private E nextItem;

        /**
         * Index of element returned by most recent call to next.
         * Reset to -1 if this element is deleted by a call to remove.
         */
        private int priorIndex;

        /**
         * Remembers last item that was returned by most recent call to
         * next so that we may remove it safely.
         */
        private E priorItem;

        Itr() {
            if (count > 0)
                nextItem = items[nextIndex = headIndex];
        }

        public boolean hasNext() {
            /*
             * No sync. We can return true by mistake here
             * only if this iterator passed across threads,
             * which we don't support anyway.
             */
            return nextItem != null;
        }

        /**
         * Checks whether nextIndex is valid; if so setting nextItem.
         * Stops iterator when either hits putIndex or sees null item.
         */
        private void checkNext() {
            while (nextIndex != putIndex && items[nextIndex] == null)
                nextIndex = inc(nextIndex);

            if (nextIndex == putIndex)
                nextItem = null;
            else
                nextItem = items[nextIndex];
        }

        public E next() {
            final ReentrantLock lock = ArrayCloseableBlockingQueue.this.lock;
            lock.lock();
            try {
                if (nextItem == null)
                    throw new NoSuchElementException();
                priorIndex = nextIndex;
                priorItem = nextItem;
                nextIndex = inc(nextIndex);
                checkNext();
                return priorItem;
            } finally {
                lock.unlock();
            }
        }

        public void remove() {
            final ReentrantLock lock = ArrayCloseableBlockingQueue.this.lock;
            E x = null;
            lock.lock();
            try {
                if (priorItem == null)
                    throw new IllegalStateException();

                if (priorItem == items[priorIndex]) {
                    x = items[priorIndex];
                    removeAt(priorIndex);
                }
                priorItem = null;

                checkNext();
            } finally {
                lock.unlock();
            }
            if (events != null && x != null) events.post(new BlockingQueueRemoveEvent(x));
        }
    }
}
