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

package org.ejbca.core.ejb.approval;

import java.util.Collection;
import java.util.List;

import javax.ejb.Local;

import org.cesecore.authentication.tokens.AuthenticationToken;
import org.ejbca.core.model.approval.Approval;
import org.ejbca.core.model.approval.ApprovalRequest;
import org.ejbca.core.model.approval.profile.ApprovalProfile;

/**
 * Local interface for ApprovalSession.
 * 
 * @version $Id$
 */
@Local
public interface ApprovalSessionLocal extends ApprovalSession {

    /**
     * Send all work-flow related approval notifications given the last added Approval to the approvalsPerformed.
     * 
     * @param authenticationToken the current administrator that triggered the approval action
     * @param approvalRequest the approval request
     * @param approvalProfile the profile determining the approval work flow
     * @param approvalsPerformed a list of completed approvals so far with the current approval last
     * @param expired should be set to true if the notification is due to expiration of the approval request
     */
    void sendApprovalNotifications(AuthenticationToken authenticationToken, ApprovalRequest approvalRequest, ApprovalProfile approvalProfile,
            List<Approval> approvalsPerformed, boolean expired);

	/**
	 * Encode a Collection of Approval and set it in ApprovalData object.
	 * @param approvals cannot be null.
	 */
	void setApprovals(ApprovalData approvalData, Collection<Approval> approvals);
	
	ApprovalData findNonExpiredApprovalDataLocal(int approvalId);
	
	/**
	 * Returns a list of all approval requests whose status is WAITING_FOR_APPROVAL only (aka. not including status APPROVED).
	 * This method is called mainly when upgrading older approval requests to EJBCA 6.6.0 or later.
	 * 
	 * @return A list of all approval requests whose status is WAITING_FOR_APPROVAL
	 */
	List<ApprovalData> findWaitingForApprovalApprovalDataLocal();
	
    /**
     * Method that takes an approvalId (hash) and returns the request's ID (the value in the "Id" column in the "ApprovalData" table in the database)
     * 
     * @param approvalId the request hash (approvalId in the database)
     * @return the ID of the approval request or 0 if it does not exist
     */
    int getIdFromApprovalId(int approvalId);

	 /**
      * Updates the approval request field for an approval. Doesn't do any authorization checks.
      * 
      * @param approvalDataId the ID of an approvalData object (not the approvalID hash)
      * @param approvalRequest the updated approval request
      */
     void updateApprovalRequest(final int approvalDataId, final ApprovalRequest approvalRequest);
}
