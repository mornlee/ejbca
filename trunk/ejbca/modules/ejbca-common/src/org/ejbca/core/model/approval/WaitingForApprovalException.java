/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.core.model.approval;

import javax.xml.ws.WebFault;

import org.cesecore.ErrorCode;
import org.ejbca.core.EjbcaException;

/**
 * Exception thrown from actions that stop to wait for approvals
 * 
 * @version $Id$
 */
@WebFault
public class WaitingForApprovalException extends EjbcaException {

	private static final long serialVersionUID = 6808192333114783496L;
    private int approvalId = 0;

    /**
     * Constructor used to create exception with an errorCode. Calls the same default constructor
     * in the base class <code>EjbcaException</code>.
     *
     * @param errorCode defines the cause of the exception.
     */
    public WaitingForApprovalException(ErrorCode errorCode) {
        super(errorCode);
    }

    public WaitingForApprovalException(String message, Throwable cause) {
		super(message, cause);
	}

	public WaitingForApprovalException(String message) {
		super(message);
	}
	
	public WaitingForApprovalException(String message, int approvalId) {
		super(message);
		this.approvalId = approvalId;
	}
	
	public int getApprovalId(){
		return approvalId;
	}
	
	public void setApprovalId(int approvalId){
		this.approvalId = approvalId;
	}
	
	@Override
    public final Object getNonSensitiveClone() {
	    WaitingForApprovalException nonSensitiveClone = (WaitingForApprovalException)super.getNonSensitiveClone();
	    nonSensitiveClone.setApprovalId(getApprovalId());
	    return nonSensitiveClone;
	}

}
