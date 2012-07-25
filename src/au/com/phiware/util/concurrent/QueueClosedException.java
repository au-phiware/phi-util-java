package au.com.phiware.util.concurrent;

public class QueueClosedException extends IllegalStateException {

	private static final long serialVersionUID = -8393303638950186454L;

	public QueueClosedException() {
		super();
	}

	public QueueClosedException(String s) {
		super(s);
	}

	public QueueClosedException(Throwable cause) {
		super(cause);
	}

	public QueueClosedException(String message, Throwable cause) {
		super(message, cause);
	}

}
