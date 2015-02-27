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

package org.ejbca.core.ejb.ca.publisher;

import java.beans.XMLDecoder;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;
import org.cesecore.audit.enums.EventStatus;
import org.cesecore.audit.log.SecurityEventsLoggerSessionLocal;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.authorization.control.AccessControlSessionLocal;
import org.cesecore.authorization.control.StandardRules;
import org.cesecore.certificates.certificate.Base64CertData;
import org.cesecore.certificates.certificate.CertificateConstants;
import org.cesecore.certificates.certificate.CertificateData;
import org.cesecore.certificates.certificate.CertificateDataWrapper;
import org.cesecore.certificates.endentity.ExtendedInformation;
import org.cesecore.config.CesecoreConfiguration;
import org.cesecore.jndi.JndiConstants;
import org.cesecore.util.Base64GetHashMap;
import org.cesecore.util.CertTools;
import org.cesecore.util.ProfileID;
import org.ejbca.core.ejb.audit.enums.EjbcaEventTypes;
import org.ejbca.core.ejb.audit.enums.EjbcaModuleTypes;
import org.ejbca.core.ejb.audit.enums.EjbcaServiceTypes;
import org.ejbca.core.model.InternalEjbcaResources;
import org.ejbca.core.model.authorization.AccessRulesConstants;
import org.ejbca.core.model.ca.publisher.ActiveDirectoryPublisher;
import org.ejbca.core.model.ca.publisher.BasePublisher;
import org.ejbca.core.model.ca.publisher.CustomPublisherContainer;
import org.ejbca.core.model.ca.publisher.LdapPublisher;
import org.ejbca.core.model.ca.publisher.LdapSearchPublisher;
import org.ejbca.core.model.ca.publisher.PublisherConnectionException;
import org.ejbca.core.model.ca.publisher.PublisherConst;
import org.ejbca.core.model.ca.publisher.PublisherDoesntExistsException;
import org.ejbca.core.model.ca.publisher.PublisherException;
import org.ejbca.core.model.ca.publisher.PublisherExistsException;
import org.ejbca.core.model.ca.publisher.PublisherQueueVolatileInformation;
import org.ejbca.core.model.ca.publisher.ValidationAuthorityPublisher;

/**
 * Handles management of Publishers.
 * 
 * @version $Id$
 */
@Stateless(mappedName = JndiConstants.APP_JNDI_PREFIX + "PublisherSessionRemote")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class PublisherSessionBean implements PublisherSessionLocal, PublisherSessionRemote {

    private static final Logger log = Logger.getLogger(PublisherSessionBean.class);

    /** Internal localization of logs and errors */
    private static final InternalEjbcaResources intres = InternalEjbcaResources.getInstance();

    @PersistenceContext(unitName = "ejbca")
    private EntityManager entityManager;

    @EJB
    private AccessControlSessionLocal authorizationSession;
    @EJB
    private PublisherQueueSessionLocal publisherQueueSession;
    @EJB
    private SecurityEventsLoggerSessionLocal auditSession;

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void flushPublisherCache() {
        PublisherCache.INSTANCE.flush();
        if (log.isDebugEnabled()) {
            log.debug("Flushed Publisher cache.");
        }
    }

    @Override
    public boolean storeCertificate(AuthenticationToken admin, Collection<Integer> publisherids, CertificateDataWrapper certWrapper, String username,
            String password, String userDN, String cafp, int status, int type, long revocationDate, int revocationReason, String tag,
            int certificateProfileId, long lastUpdate, ExtendedInformation extendedinformation) throws AuthorizationDeniedException {
        final Certificate incert = certWrapper.getCertificate();
        final int caid = CertTools.getIssuerDN(incert).hashCode();
        if (!authorizationSession.isAuthorized(admin, StandardRules.CAACCESS.resource() + caid)) {
            final String msg = intres.getLocalizedMessage("caadmin.notauthorizedtoca", admin.toString(), caid);
            throw new AuthorizationDeniedException(msg);
        }
        if (publisherids == null) {
            return true;
        }
        boolean returnval = true;
        final List<BasePublisher> publishersToTryDirect = new ArrayList<BasePublisher>();
        final List<BasePublisher> publishersToQueuePending = new ArrayList<BasePublisher>();
        final List<BasePublisher> publishersToQueueSuccess = new ArrayList<BasePublisher>();
        for (final Integer id : publisherids) {
            BasePublisher publ = getPublisherInternal(id, true);
            if (publ != null) {
                // If the publisher will not publish the certificate, break out directly and do not call the publisher or queue the certificate
                if (publ.willPublishCertificate(status, revocationReason)) {
                    if (publ.getOnlyUseQueue()) {
                        if (publ.getUseQueueForCertificates()) {
                            publishersToQueuePending.add(publ);
                            // Publishing to the queue directly is not considered a successful write to the publisher (since we don't know that it will be)
                            returnval = false;
                        } else {
                            // NOOP: This publisher is configured to only write to the queue, but not for certificates
                        }
                    } else {
                        publishersToTryDirect.add(publ);
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Not storing or queuing certificate for Publisher with id " + id + " because publisher will not publish it.");
                    }
                }
            } else {
                String msg = intres.getLocalizedMessage("publisher.nopublisher", id);
                log.info(msg);
                returnval = false;
            }
        }
        final String fingerprint = CertTools.getFingerprintAsString(incert);
        final List<Object> publisherResults = publisherQueueSession.storeCertificateNonTransactionalInternal(publishersToTryDirect, admin, certWrapper,
                username, password, userDN, cafp, status, type, revocationDate, revocationReason, tag, certificateProfileId, lastUpdate,
                extendedinformation);
        final String certSerno = CertTools.getSerialNumberAsString(incert);
        for (int i = 0; i < publishersToTryDirect.size(); i++) {
            final Object publisherResult = publisherResults.get(i);
            final BasePublisher publ = publishersToTryDirect.get(i);
            final int id = publ.getPublisherId();
            final String name = getPublisherName(id);
            if (!(publisherResult instanceof PublisherException)) {
                final String msg = intres.getLocalizedMessage("publisher.store", CertTools.getSubjectDN(incert), name);
                final Map<String, Object> details = new LinkedHashMap<String, Object>();
                details.put("msg", msg);
                auditSession.log(EjbcaEventTypes.PUBLISHER_STORE_CERTIFICATE, EventStatus.SUCCESS, EjbcaModuleTypes.PUBLISHER,
                        EjbcaServiceTypes.EJBCA, admin.toString(), null, username, certSerno, details);
                if (publ.getKeepPublishedInQueue() && publ.getUseQueueForCertificates()) {
                    publishersToQueueSuccess.add(publ);
                }
            } else {
                final String msg = intres.getLocalizedMessage("publisher.errorstore", name, fingerprint);
                final Map<String, Object> details = new LinkedHashMap<String, Object>();
                details.put("msg", msg);
                details.put("error", ((PublisherException) publisherResult).getMessage());
                auditSession.log(EjbcaEventTypes.PUBLISHER_STORE_CERTIFICATE, EventStatus.FAILURE, EjbcaModuleTypes.PUBLISHER,
                        EjbcaServiceTypes.EJBCA, admin.toString(), null, username, certSerno, details);
                if (publ.getUseQueueForCertificates()) {
                    publishersToQueuePending.add(publ);
                }
                returnval = false;
            }
        }
        addQueueData(publishersToQueueSuccess, username, password, extendedinformation, userDN, incert, status, PublisherConst.STATUS_SUCCESS);
        addQueueData(publishersToQueuePending, username, password, extendedinformation, userDN, incert, status, PublisherConst.STATUS_PENDING);
        return returnval;
    }

    @Override
    public boolean storeCertificate(AuthenticationToken admin, Collection<Integer> publisherids, Certificate certificate, String username,
            String password, String userDN, String cafp, int status, int type, long revocationDate, int revocationReason, String tag,
            int certificateProfileId, long lastUpdate, ExtendedInformation extendedinformation) throws AuthorizationDeniedException {
        String fingerprint = CertTools.getFingerprintAsString(certificate);
        final Base64CertData base64CertData;
        if (CesecoreConfiguration.useBase64CertTable()) {
            base64CertData = Base64CertData.findByFingerprint(entityManager, fingerprint);
        } else {
            base64CertData = null;
        }
        return storeCertificate(admin, publisherids,
                new CertificateDataWrapper(certificate, CertificateData.findByFingerprint(entityManager, fingerprint), base64CertData), username,
                password, userDN, cafp, status, type, revocationDate, revocationReason, tag, certificateProfileId, lastUpdate, extendedinformation);
    }
    
    
    private void addQueueData(final List<BasePublisher> publishersToQueue, final String username, final String password,
            final ExtendedInformation extendedInformation, final String userDN, final Certificate incert, final int status, final int publisherStatus) {
        for (final BasePublisher publ : publishersToQueue) {
            final int id = publ.getPublisherId();
            final String name = getPublisherName(id);
            if (log.isDebugEnabled()) {
                log.debug("KeepPublishedInQueue: " + publ.getKeepPublishedInQueue());
                log.debug("UseQueueForCertificates: " + publ.getUseQueueForCertificates());
            }
            // Write to the publisher queue either for audit reasons or to be able try again
            PublisherQueueVolatileInformation pqvd = new PublisherQueueVolatileInformation();
            pqvd.setUsername(username);
            pqvd.setPassword(password);
            pqvd.setExtendedInformation(extendedInformation);
            pqvd.setUserDN(userDN);
            String fp = CertTools.getFingerprintAsString(incert);
            try {
                publisherQueueSession.addQueueData(id, PublisherConst.PUBLISH_TYPE_CERT, fp, pqvd, publisherStatus);
                final String msg = intres.getLocalizedMessage("publisher.storequeue", name, fp, status);
                log.info(msg);
            } catch (CreateException e) {
                final String msg = intres.getLocalizedMessage("publisher.errorstorequeue", name, fp, status);
                log.info(msg, e);
            }
        }
    }

    @Override
    public void revokeCertificate(AuthenticationToken admin, Collection<Integer> publisherids, CertificateDataWrapper certificateWrapper, String username, String userDN,
            String cafp, int type, int reason, long revocationDate, String tag, int certificateProfileId, long lastUpdate)
            throws AuthorizationDeniedException {
        storeCertificate(admin, publisherids, certificateWrapper, username, null, userDN, cafp, CertificateConstants.CERT_REVOKED, type, revocationDate, reason,
                tag, certificateProfileId, lastUpdate, null);
    }

    @Override
    public boolean storeCRL(AuthenticationToken admin, Collection<Integer> publisherids, byte[] incrl, String cafp, int number, String issuerDn)
            throws AuthorizationDeniedException {
        if (log.isTraceEnabled()) {
            log.trace(">storeCRL");
        }
        int caid = CertTools.stringToBCDNString(issuerDn).hashCode();
        if (!authorizationSession.isAuthorized(admin, StandardRules.CAACCESS.resource() + caid)) {
            final String msg = intres.getLocalizedMessage("caadmin.notauthorizedtoca", admin.toString(), caid);
            throw new AuthorizationDeniedException(msg);
        }

        boolean returnval = true;
        for (Integer id : publisherids) {
            int publishStatus = PublisherConst.STATUS_PENDING;
            final BasePublisher publ = getPublisherInternal(id, true);
            if (publ != null) {
                final String name = getPublisherName(id);
                // If it should be published directly
                if (!publ.getOnlyUseQueue()) {
                    try {
                        try {
                            if (publisherQueueSession.storeCRLNonTransactional(publ, admin, incrl, cafp, number, issuerDn)) {
                                publishStatus = PublisherConst.STATUS_SUCCESS;
                            }
                        } catch (EJBException e) {
                            final Throwable t = e.getCause();
                            if (t instanceof PublisherException) {
                                throw (PublisherException) t;
                            } else {
                                throw e;
                            }
                        }
                        final String msg = intres.getLocalizedMessage("publisher.store", "CRL", name);
                        final Map<String, Object> details = new LinkedHashMap<String, Object>();
                        details.put("msg", msg);
                        auditSession.log(EjbcaEventTypes.PUBLISHER_STORE_CRL, EventStatus.SUCCESS, EjbcaModuleTypes.PUBLISHER,
                                EjbcaServiceTypes.EJBCA, admin.toString(), null, null, null, details);
                    } catch (PublisherException pe) {
                        final String msg = intres.getLocalizedMessage("publisher.errorstore", name, "CRL");
                        final Map<String, Object> details = new LinkedHashMap<String, Object>();
                        details.put("msg", msg);
                        details.put("error", pe.getMessage());
                        auditSession.log(EjbcaEventTypes.PUBLISHER_STORE_CRL, EventStatus.FAILURE, EjbcaModuleTypes.PUBLISHER,
                                EjbcaServiceTypes.EJBCA, admin.toString(), null, null, null, details);
                    }
                }
                if (publishStatus != PublisherConst.STATUS_SUCCESS) {
                    returnval = false;
                }
                if (log.isDebugEnabled()) {
                    log.debug("KeepPublishedInQueue: " + publ.getKeepPublishedInQueue());
                    log.debug("UseQueueForCRLs: " + publ.getUseQueueForCRLs());
                }
                if ((publishStatus != PublisherConst.STATUS_SUCCESS || publ.getKeepPublishedInQueue()) && publ.getUseQueueForCRLs()) {
                    // Write to the publisher queue either for audit reasons or
                    // to be able try again
                    final PublisherQueueVolatileInformation pqvd = new PublisherQueueVolatileInformation();
                    pqvd.setUserDN(issuerDn);
                    String fp = CertTools.getFingerprintAsString(incrl);
                    try {
                        publisherQueueSession.addQueueData(id.intValue(), PublisherConst.PUBLISH_TYPE_CRL, fp, pqvd, PublisherConst.STATUS_PENDING);
                        String msg = intres.getLocalizedMessage("publisher.storequeue", name, fp, "CRL");
                        log.info(msg);
                    } catch (CreateException e) {
                        String msg = intres.getLocalizedMessage("publisher.errorstorequeue", name, fp, "CRL");
                        log.info(msg, e);
                    }
                }
            } else {
                String msg = intres.getLocalizedMessage("publisher.nopublisher", id);
                log.info(msg);
                returnval = false;
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("<storeCRL");
        }
        return returnval;
    }

    @Override
    public void testConnection(int publisherid) throws PublisherConnectionException { // NOPMD: this is not a JUnit test
        if (log.isTraceEnabled()) {
            log.trace(">testConnection(id: " + publisherid + ")");
        }
        PublisherData pdl = PublisherData.findById(entityManager, Integer.valueOf(publisherid));
        if (pdl != null) {
            String name = pdl.getName();
            try {
                getPublisher(pdl).testConnection();
                String msg = intres.getLocalizedMessage("publisher.testedpublisher", name);
                log.info(msg);
            } catch (PublisherConnectionException pe) {
                String msg = intres.getLocalizedMessage("publisher.errortestpublisher", name);
                log.info(msg);
                throw new PublisherConnectionException(pe.getMessage());
            }
        } else {
            String msg = intres.getLocalizedMessage("publisher.nopublisher", Integer.valueOf(publisherid));
            log.info(msg);
        }
        if (log.isTraceEnabled()) {
            log.trace("<testConnection(id: " + publisherid + ")");
        }
    }

    @Override
    public int addPublisher(AuthenticationToken admin, String name, BasePublisher publisher) throws PublisherExistsException,
            AuthorizationDeniedException {
        if (log.isTraceEnabled()) {
            log.trace(">addPublisher(name: " + name + ")");
        }
        int id = findFreePublisherId();
        addPublisher(admin, id, name, publisher);
        if (log.isTraceEnabled()) {
            log.trace("<addPublisher()");
        }
        return id;
    }

    @Override
    public void addPublisher(AuthenticationToken admin, int id, String name, BasePublisher publisher) throws PublisherExistsException,
            AuthorizationDeniedException {
        if (log.isTraceEnabled()) {
            log.trace(">addPublisher(name: " + name + ", id: " + id + ")");
        }
        addPublisherInternal(admin, id, name, publisher);
        final String msg = intres.getLocalizedMessage("publisher.addedpublisher", name);
        final Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", msg);
        auditSession.log(EjbcaEventTypes.PUBLISHER_CREATION, EventStatus.SUCCESS, EjbcaModuleTypes.PUBLISHER, EjbcaServiceTypes.EJBCA,
                admin.toString(), null, null, null, details);
        if (log.isTraceEnabled()) {
            log.trace("<addPublisher()");
        }
    }
    
    @Override
    public void addPublisherFromData(AuthenticationToken admin, int id, String name, Map<?, ?> data) throws PublisherExistsException, AuthorizationDeniedException {
        final BasePublisher publisher = constructPublisher(((Integer) (data.get(BasePublisher.TYPE))).intValue());
        publisher.setPublisherId(id);
        publisher.setName(name);
        publisher.loadData(data);
        addPublisher(admin, id, name, publisher);
    }

    private void addPublisherInternal(AuthenticationToken admin, int id, String name, BasePublisher publisher) throws AuthorizationDeniedException,
            PublisherExistsException {
        authorizedToEditPublishers(admin);
        if (PublisherData.findByName(entityManager, name) == null) {
            if (PublisherData.findById(entityManager, Integer.valueOf(id)) == null) {
                entityManager.persist(new PublisherData(Integer.valueOf(id), name, publisher));
            } else {
                final String msg = intres.getLocalizedMessage("publisher.erroraddpublisher", id);
                log.info(msg);
                throw new PublisherExistsException();
            }
        } else {
            final String msg = intres.getLocalizedMessage("publisher.erroraddpublisher", name);
            log.info(msg);
            throw new PublisherExistsException();
        }
    }

    @Override
    public void changePublisher(AuthenticationToken admin, String name, BasePublisher publisher) throws AuthorizationDeniedException {
        if (log.isTraceEnabled()) {
            log.trace(">changePublisher(name: " + name + ")");
        }
        authorizedToEditPublishers(admin);

        PublisherData htp = PublisherData.findByName(entityManager, name);
        if (htp != null) {
            final Map<Object, Object> diff = getPublisher(htp).diff(publisher);
            htp.setPublisher(publisher);
            // Since loading a Publisher is quite complex, we simple purge the cache here
            PublisherCache.INSTANCE.removeEntry(htp.getId());
            final String msg = intres.getLocalizedMessage("publisher.changedpublisher", name);
            final Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("msg", msg);
            for (Map.Entry<Object, Object> entry : diff.entrySet()) {
                details.put(entry.getKey().toString(), entry.getValue().toString());
            }
            auditSession.log(EjbcaEventTypes.PUBLISHER_CHANGE, EventStatus.SUCCESS, EjbcaModuleTypes.PUBLISHER, EjbcaServiceTypes.EJBCA,
                    admin.toString(), null, null, null, details);
        } else {
            String msg = intres.getLocalizedMessage("publisher.errorchangepublisher", name);
            log.info(msg);
        }
        if (log.isTraceEnabled()) {
            log.trace("<changePublisher()");
        }
    }

    @Override
    public void clonePublisher(AuthenticationToken admin, String oldname, String newname) throws PublisherDoesntExistsException,
            AuthorizationDeniedException, PublisherExistsException {
        if (log.isTraceEnabled()) {
            log.trace(">clonePublisher(name: " + oldname + ")");
        }
        BasePublisher publisherdata = null;
        PublisherData htp = PublisherData.findByName(entityManager, oldname);
        if (htp == null) {
            throw new PublisherDoesntExistsException("Could not find publisher " + oldname);
        }
        try {
            publisherdata = (BasePublisher) getPublisher(htp).clone();
            addPublisherInternal(admin, findFreePublisherId(), newname, publisherdata);
            final String msg = intres.getLocalizedMessage("publisher.clonedpublisher", newname, oldname);
            final Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("msg", msg);
            auditSession.log(EjbcaEventTypes.PUBLISHER_CREATION, EventStatus.SUCCESS, EjbcaModuleTypes.PUBLISHER, EjbcaServiceTypes.EJBCA,
                    admin.toString(), null, null, null, details);
        } catch (PublisherExistsException f) {
            final String msg = intres.getLocalizedMessage("publisher.errorclonepublisher", newname, oldname);
            log.info(msg);
            throw f;
        } catch (CloneNotSupportedException e) {
            // Severe error, should never happen
            throw new EJBException(e);
        }
        if (log.isTraceEnabled()) {
            log.trace("<clonePublisher()");
        }
    }

    @Override
    public void removePublisher(AuthenticationToken admin, String name) throws AuthorizationDeniedException {
        if (log.isTraceEnabled()) {
            log.trace(">removePublisher(name: " + name + ")");
        }
        authorizedToEditPublishers(admin);
        try {
            PublisherData htp = PublisherData.findByName(entityManager, name);
            if (htp == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Trying to remove a publisher that does not exist: " + name);
                }
            } else {
                entityManager.remove(htp);
                // Purge the cache here
                PublisherCache.INSTANCE.removeEntry(htp.getId());
                final String msg = intres.getLocalizedMessage("publisher.removedpublisher", name);
                final Map<String, Object> details = new LinkedHashMap<String, Object>();
                details.put("msg", msg);
                auditSession.log(EjbcaEventTypes.PUBLISHER_REMOVAL, EventStatus.SUCCESS, EjbcaModuleTypes.PUBLISHER, EjbcaServiceTypes.EJBCA,
                        admin.toString(), null, null, null, details);
            }
        } catch (Exception e) {
            String msg = intres.getLocalizedMessage("publisher.errorremovepublisher", name);
            log.info(msg, e);
        }
        if (log.isTraceEnabled()) {
            log.trace("<removePublisher()");
        }
    }

    @Override
    public void renamePublisher(AuthenticationToken admin, String oldname, String newname) throws PublisherExistsException,
            AuthorizationDeniedException {
        if (log.isTraceEnabled()) {
            log.trace(">renamePublisher(from " + oldname + " to " + newname + ")");
        }
        authorizedToEditPublishers(admin);
        boolean success = false;
        if (PublisherData.findByName(entityManager, newname) == null) {
            PublisherData htp = PublisherData.findByName(entityManager, oldname);
            if (htp != null) {
                htp.setName(newname);
                success = true;
                // Since loading a Publisher is quite complex, we simple purge the cache here
                PublisherCache.INSTANCE.removeEntry(htp.getId());
            }
        }
        if (success) {
            String msg = intres.getLocalizedMessage("publisher.renamedpublisher", oldname, newname);
            final Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("msg", msg);
            auditSession.log(EjbcaEventTypes.PUBLISHER_RENAME, EventStatus.SUCCESS, EjbcaModuleTypes.PUBLISHER, EjbcaServiceTypes.EJBCA,
                    admin.toString(), null, null, null, details);
        } else {
            String msg = intres.getLocalizedMessage("publisher.errorrenamepublisher", oldname, newname);
            log.info(msg);
            throw new PublisherExistsException();
        }
        if (log.isTraceEnabled()) {
            log.trace("<renamePublisher()");
        }
    }

    @Override
    public Map<Integer, BasePublisher> getAllPublishers() {
        Map<Integer, BasePublisher> returnval = new HashMap<Integer, BasePublisher>();
        for(PublisherData publisher : PublisherData.findAll(entityManager)) {
            returnval.put(publisher.getId(), getPublisher(publisher));
        }
        return returnval;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public HashMap<Integer, String> getPublisherIdToNameMap() {
        HashMap<Integer,String> returnval = new HashMap<Integer,String>();
        for(PublisherData publisher : PublisherData.findAll(entityManager)) {
            returnval.put(publisher.getId(), publisher.getName());
        }
        return returnval;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public BasePublisher getPublisher(AuthenticationToken authenticationToken, String name) throws AuthorizationDeniedException {
        authorizedToEditPublishers(authenticationToken);
        return getPublisherInternal(name, true);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public BasePublisher getPublisher(AuthenticationToken authenticationToken, int id) throws AuthorizationDeniedException {
        authorizedToEditPublishers(authenticationToken);
        return getPublisherInternal(id, true);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public int getPublisherUpdateCount(int publisherid) {
        int returnval = 0;
        PublisherData pd = PublisherData.findById(entityManager, Integer.valueOf(publisherid));
        if (pd != null) {
            returnval = pd.getUpdateCounter();
        }
        return returnval;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public int getPublisherId(String name) {
        // Get publisher to ensure it is in the cache, or read
        final BasePublisher pub = getPublisherInternal(name, true);
        final int ret = (pub != null) ? pub.getPublisherId() : 0;
        return ret;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public String getPublisherName(int id) {
        if (log.isTraceEnabled()) {
            log.trace(">getPublisherName(id: " + id + ")");
        }
        // Get publisher to ensure it is in the cache, or read
        final BasePublisher pub = getPublisherInternal(id, true);
        final String ret = (pub != null) ? pub.getName() : null;
        if (log.isTraceEnabled()) {
            log.trace("<getPublisherName(): " + ret);
        }
        return ret;
    }
    
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public Map<?, ?> getPublisherData(AuthenticationToken admin, int id) throws AuthorizationDeniedException, PublisherDoesntExistsException {
        if (log.isTraceEnabled()) {
            log.trace(">getPublisherData(id: " + id + ")");
        }
        
        final BasePublisher pub = getPublisherInternal(id,true);
        if (pub == null) {
            throw new PublisherDoesntExistsException("Publisher with id "+id+" doesn't exist");
        }
        authorizedToEditPublishers(admin);
        return (Map<?, ?>)pub.saveData();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public String testAllConnections() {
        if (log.isTraceEnabled()) {
            log.trace(">testAllConnections");
        }
        String returnval = "";
        Iterator<PublisherData> i = PublisherData.findAll(entityManager).iterator();
        while (i.hasNext()) {
            PublisherData pdl = i.next();
            String name = pdl.getName();
            try {
                getPublisher(pdl).testConnection();
            } catch (PublisherConnectionException pe) {
                String msg = intres.getLocalizedMessage("publisher.errortestpublisher", name);
                log.info(msg);
                returnval += "\n" + msg;
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("<testAllConnections");
        }
        return returnval;
    }

    private int findFreePublisherId() {
        final ProfileID.DB db = new ProfileID.DB() {
            @Override
            public boolean isFree(int i) {
                return PublisherData.findById(PublisherSessionBean.this.entityManager, i) == null;
            }
        };
        return ProfileID.getNotUsedID(db);
    }
    
    
    private BasePublisher getPublisherInternal(final String name, boolean fromCache) {
        if (log.isTraceEnabled()) {
            log.trace(">getPublisherInternal: " + name);
        }
        int id = PublisherCache.INSTANCE.getNameToIdMap().get(name);
        return getPublisherInternal(id, fromCache);
    }

    /**
     * Internal method for getting Publisher, to avoid code duplication. Tries to find the Publisher even if the id is wrong due to CA certificate DN not being
     * the same as CA DN. Uses PublisherCache directly if configured to do so.
     * 
     * Note! No authorization checks performed in this internal method
     * 
     * @param id
     *            numerical id of Publisher that we search for
     * @param fromCache if we should use the cache or return a new, decoupled, instance from the database, to be used when you need
     *             a completely distinct object, for edit, and not a shared cached instance.
     * @return BasePublisher value object or null if it does not exist
     */
    private BasePublisher getPublisherInternal(int id, boolean fromCache) {
        if (log.isTraceEnabled()) {
            log.trace(">getPublisherInternal: " + id);
        }
        Integer idValue = Integer.valueOf(id);
        BasePublisher returnval = null;
        // If we should read from cache, and we have an id to use in the cache, and the cache does not need to be updated
        if (fromCache && idValue != null && !PublisherCache.INSTANCE.shouldCheckForUpdates(idValue)) {
            // Get from cache (or null)
            returnval = PublisherCache.INSTANCE.getEntry(idValue);
        }

        // if we selected to not read from cache, or if the cache did not contain this entry
        if (returnval == null) {
            if (log.isDebugEnabled()) {
                log.debug("Publisher with ID " + idValue + " will be checked for updates.");
            }
            // We need to read from database because we specified to not get from cache or we don't have anything in the cache
            final PublisherData pd = PublisherData.findById(entityManager, idValue);
            
            if (pd != null) {
                returnval = getPublisher(pd);
                final int digest = pd.getProtectString(0).hashCode();
                // The cache compares the database data with what is in the cache
                // If database is different from cache, replace it in the cache
                PublisherCache.INSTANCE.updateWith(pd.getId(), digest, pd.getName(), returnval);
            } else {
                // Ensure that it is removed from cache if it exists
                if (idValue != null) {
                    PublisherCache.INSTANCE.removeEntry(idValue);
                }
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("<getPublisherInternal: " + id + " : " + (returnval == null ? "null" : "not null"));
        }
        return returnval;
    }

    /** @return the publisher data and updates it if necessary. */
    private BasePublisher getPublisher(PublisherData pData) {
        BasePublisher publisher = pData.getCachedPublisher();
        if (publisher == null) {
            XMLDecoder decoder;
            try {
                decoder = new XMLDecoder(new ByteArrayInputStream(pData.getData().getBytes("UTF8")));
            } catch (UnsupportedEncodingException e) {
                throw new EJBException(e);
            }
            HashMap<?, ?> h = (HashMap<?, ?>) decoder.readObject();
            decoder.close();
            // Handle Base64 encoded string values
            HashMap<?, ?> data = new Base64GetHashMap(h);

            publisher = constructPublisher(((Integer) (data.get(BasePublisher.TYPE))).intValue());
            publisher.setPublisherId(pData.getId());
            publisher.setName(pData.getName());
            publisher.loadData(data);
        }
        return publisher;
    }
    
    private BasePublisher constructPublisher(final int publisherType) {
        switch (publisherType) {
        case PublisherConst.TYPE_LDAPPUBLISHER:
            return new LdapPublisher();
        case PublisherConst.TYPE_LDAPSEARCHPUBLISHER:
            return new LdapSearchPublisher();
        case PublisherConst.TYPE_ADPUBLISHER:
            return new ActiveDirectoryPublisher();
        case PublisherConst.TYPE_CUSTOMPUBLISHERCONTAINER:
            return new CustomPublisherContainer();
        case PublisherConst.TYPE_VAPUBLISHER:
            return new ValidationAuthorityPublisher();
        default:
            throw new IllegalStateException("Invalid or unimplemented publisher type "+publisherType);
        }
    }

    private void authorizedToEditPublishers(AuthenticationToken admin) throws AuthorizationDeniedException {
        // We need to check that admin also have rights to edit publishers
        if (!authorizationSession.isAuthorized(admin, AccessRulesConstants.REGULAR_EDITPUBLISHER)) {
            final String msg = intres.getLocalizedMessage("store.editpublishernotauthorized", admin.toString());
            throw new AuthorizationDeniedException(msg);
        }
    }

}
