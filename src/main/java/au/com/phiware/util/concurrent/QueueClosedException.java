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

/**
 * Thrown when an operation cannot be performed because a {@link CloseableBlockingQueue} is closed.
 *
 * @author Corin Lawson <corin@phiware.com.au>
 *
 */
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
