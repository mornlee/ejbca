/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

package org.ejbca.core.protocol.ws;

import javax.xml.ws.WebFault;

/**
 * Thrown when a string is not a valid date
 * @version $Id: DateNotValidException.java 14932 2012-06-14 21:53:07Z primelars $
 *
 */
@WebFault
public class DateNotValidException extends Exception {

	private static final long serialVersionUID = -4557881537494914234L;

	/**
	 * @param message with more information what is wrong
	 */
	public DateNotValidException(String m) {
		super(m);
	}
}
