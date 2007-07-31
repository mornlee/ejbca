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

package org.ejbca.core.model.ra;

/**
 * Exception used when a request contains impossible combination of data.
 */
public class BadRequestException extends org.ejbca.core.EjbcaException {
    /**
     * Creates a new instance of BadRequestException
     *
     * @param message error message
     */
    public BadRequestException(String message) {
        super(message);
    }

}
