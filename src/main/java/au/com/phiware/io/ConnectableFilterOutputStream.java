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
package au.com.phiware.io;

import java.io.FilterOutputStream;
import java.io.OutputStream;

/**
 * A {@link FilterOutputStream} that allows the contained output stream
 * to be set post-construction.
 *  
 * @author Corin Lawson <corin@phiware.com.au>
 *
 */
public interface ConnectableFilterOutputStream {
	/**
	 * Upon successful return, the specified output stream shall become
	 * the underling output stream of this (yep, you guessed it) output
	 * stream.
	 *   
	 * @param output stream to be contained in this output stream. 
	 */
	public void connect(OutputStream output);
}
