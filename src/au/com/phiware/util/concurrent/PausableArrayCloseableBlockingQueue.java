package au.com.phiware.util.concurrent;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * A ArrayCloseableBlockingQueue that check ins with a Continue before returning from any method.
 * Note the side effect that offer, peek and tryClose could block indefinately.
 * If you care about this use the *Unpausably variants.
 * 
 * @author Corin Lawson <corin@phiware.com.au>
 */
public class PausableArrayCloseableBlockingQueue<E>
		extends ArrayCloseableBlockingQueue<E>
		implements PausableBlockingQueue<E> {
	private static final long serialVersionUID = 6118458880439936086L;
	private Continue cont;

	/**
	 * @param capacity
	 */
	public PausableArrayCloseableBlockingQueue(int capacity) {
		super(capacity);
	}

	/**
	 * @param capacity
	 * @param cont
	 */
	public PausableArrayCloseableBlockingQueue(int capacity, Continue cont) {
		super(capacity);
		setContinue(cont);
	}

	/**
	 * @param capacity
	 * @param cont
	 * @param fair
	 */
	public PausableArrayCloseableBlockingQueue(int capacity, Continue cont, boolean fair) {
		super(capacity, fair);
		setContinue(cont);
	}

	/**
	 * @param capacity
	 * @param cont
	 * @param fair
	 * @param c
	 */
	public PausableArrayCloseableBlockingQueue(int capacity, Continue cont, boolean fair,
			Collection<E> c) {
		super(capacity, fair, c);
		setContinue(cont);
	}

	@Override
	public Continue getContinue() {
		return cont;
	}

	@Override
	public void setContinue(Continue cont) {
		//FIXME: This will cause a blip of activity if cont is paused.
		if (this.cont != cont) {
			Continue oldContinue = this.cont;
			this.cont = cont;
			if (oldContinue != null)
				oldContinue.resume();
		}
	}
	
	private void checkIn() throws InterruptedException {
		if (cont != null)
			cont.checkIn();
	}

	private void checkInUninterruptibly() {
		if (cont != null)
			try {
				cont.checkIn();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
	}

	@Override
	public boolean add(E e) {
		try {
			return super.add(e);
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public boolean contains(Object o) {
		try {
			return super.contains(o);
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public int drainTo(Collection<? super E> receiver) {
		try {
			return super.drainTo(receiver);
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public int drainTo(Collection<? super E> receiver, int maxElements) {
		try {
			return super.drainTo(receiver, maxElements);
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public boolean offer(E e) {
		try {
			return super.offer(e);
		} finally {
			checkInUninterruptibly();
		}
	}

	public boolean offerUnpausably(E e) {
		return super.offer(e);
	}

	@Override
	public boolean offer(E e, long timeout, TimeUnit unit)
			throws InterruptedException {
		try {
			return super.offer(e, timeout, unit);
		} finally {
			checkIn();
		}
	}

	public boolean offerUnpausably(E e, long timeout, TimeUnit unit)
			throws InterruptedException {
		return super.offer(e, timeout, unit);
	}

	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		try {
			return super.poll(timeout, unit);
		} finally {
			checkIn();
		}
	}

	public E pollUnpausably(long timeout, TimeUnit unit) throws InterruptedException {
		return super.poll(timeout, unit);
	}

	@Override
	public void put(E e) throws InterruptedException {
		try {
			super.put(e);
		} finally {
			checkIn();
		}
	}

	@Override
	public int remainingCapacity() {
		try {
			return super.remainingCapacity();
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public boolean remove(Object o) {
		try {
			return super.remove(o);
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public E take() throws InterruptedException {
		try {
			return super.take();
		} finally {
			checkIn();
		}
	}

	@Override
	public E element() {
		try {
			return super.element();
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public E peek() {
		try {
			return super.peek();
		} finally {
			checkInUninterruptibly();
		}
	}

	public E peekUnpausably() {
		return super.peek();
	}
	
	@Override
	public E poll() {
		try {
			return super.poll();
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public E remove() {
		try {
			return super.remove();
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		try {
			return super.addAll(c);
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public void clear() {
		try {
			super.clear();
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		try {
			return super.containsAll(c);
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public boolean isEmpty() {
		try {
			return super.isEmpty();
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public Iterator<E> iterator() {
		try {
			return super.iterator();
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		try {
			return super.removeAll(c);
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		try {
			return super.retainAll(c);
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public int size() {
		try {
			return super.size();
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public Object[] toArray() {
		try {
			return super.toArray();
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public <T> T[] toArray(T[] a) {
		try {
			return super.toArray(a);
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public boolean isClosed() {
		try {
			return super.isClosed();
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public void close() throws InterruptedException {
		try {
			super.close();
		} finally {
			checkIn();
		}
	}

	@Override
	public boolean tryClose() {
		try {
			return super.tryClose();
		} finally {
			checkInUninterruptibly();
		}
	}

	public boolean tryCloseUnpausably() {
		return super.tryClose();
	}

	@Override
	public boolean tryClose(long time, TimeUnit unit)
			throws InterruptedException {
		try {
			return super.tryClose(time, unit);
		} finally {
			checkIn();
		}
	}

	public boolean tryCloseUnpausably(long time, TimeUnit unit)
			throws InterruptedException {
		return super.tryClose(time, unit);
	}
	
	@Override
	public void preventClose() {
		try {
			super.preventClose();
		} finally {
			checkInUninterruptibly();
		}
	}

	@Override
	public void permitClose() {
		try {
			super.permitClose();
		} finally {
			checkInUninterruptibly();
		}
	}
}
