package org.ejbca.core.ejb.log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.crypto.SecretKey;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.RemoveException;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.BaseSessionBean;
import org.ejbca.core.ejb.JNDINames;
import org.ejbca.core.ejb.ServiceLocator;
import org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionLocal;
import org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionLocalHome;
import org.ejbca.core.ejb.ca.sign.ISignSessionLocal;
import org.ejbca.core.ejb.ca.sign.ISignSessionLocalHome;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionLocal;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionLocalHome;
import org.ejbca.core.ejb.services.IServiceSessionLocal;
import org.ejbca.core.ejb.services.IServiceSessionLocalHome;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.core.model.ca.catoken.CATokenOfflineException;
import org.ejbca.core.model.ca.crl.RevokedCertInfo;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.log.IProtectedLogAction;
import org.ejbca.core.model.log.IProtectedLogExportHandler;
import org.ejbca.core.model.log.LogConstants;
import org.ejbca.core.model.log.LogEntry;
import org.ejbca.core.model.log.ProtectedLogActions;
import org.ejbca.core.model.log.ProtectedLogCMSExportHandler;
import org.ejbca.core.model.log.ProtectedLogDevice;
import org.ejbca.core.model.log.ProtectedLogEventIdentifier;
import org.ejbca.core.model.log.ProtectedLogEventRow;
import org.ejbca.core.model.log.ProtectedLogExportRow;
import org.ejbca.core.model.log.ProtectedLogExporter;
import org.ejbca.core.model.log.ProtectedLogToken;
import org.ejbca.core.model.log.ProtectedLogVerifier;
import org.ejbca.core.model.services.ServiceConfiguration;
import org.ejbca.core.model.services.workers.ProtectedLogExportWorker;
import org.ejbca.core.model.services.workers.ProtectedLogVerificationWorker;
import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;
import org.ejbca.util.JDBCUtil;

/**
 * The center of the Protected Log functionality. Most services used in this workflow are found here.
 *
 * @ejb.bean
 *   display-name="ProtectedLogSessionBean"
 *   name="ProtectedLogSession"
 *   jndi-name="ProtectedLogSession"
 *   local-jndi-name="ProtectedLogSessionLocal"
 *   view-type="both"
 *   type="Stateless"
 *   transaction-type="Container"
 *
 * @weblogic.enable-call-by-reference True
 *
 * @ejb.env-entry
 * name="DataSource"
 * type="java.lang.String"
 * value="${datasource.jndi-name-prefix}${datasource.jndi-name}"
 *
 * @ejb.env-entry
 *   description="Defines the JNDI name of the mail service used"
 *   name="MailJNDIName"
 *   type="java.lang.String"
 *   value="${mail.jndi-name}"
 *   
 * @ejb.ejb-external-ref
 *   description="The ProtectedLogData entity bean"
 *   view-type="local"
 *   ref-name="ejb/ProtectedLogDataLocal"
 *   type="Entity"
 *   home="org.ejbca.core.ejb.log.ProtectedLogDataLocalHome"
 *   business="org.ejbca.core.ejb.log.ProtectedLogDataLocal"
 *   link="ProtectedLogData"
 *   
 * @ejb.ejb-external-ref
 *   description="The ProtectedLogTokenData entity bean"
 *   view-type="local"
 *   ref-name="ejb/ProtectedLogTokenDataLocal"
 *   type="Entity"
 *   home="org.ejbca.core.ejb.log.ProtectedLogTokenDataLocalHome"
 *   business="org.ejbca.core.ejb.log.ProtectedLogTokenDataLocal"
 *   link="ProtectedLogTokenData"
 *   
 * @ejb.ejb-external-ref
 *   description="The ProtectedLogExportData entity bean"
 *   view-type="local"
 *   ref-name="ejb/ProtectedLogExportDataLocal"
 *   type="Entity"
 *   home="org.ejbca.core.ejb.log.ProtectedLogExportDataLocalHome"
 *   business="org.ejbca.core.ejb.log.ProtectedLogExportDataLocal"
 *   link="ProtectedLogExportData"
 *   
 * @ejb.ejb-external-ref description="The Sign Session Bean"
 *   view-type="local"
 *   ref-name="ejb/RSASignSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.ca.sign.ISignSessionLocalHome"
 *   business="org.ejbca.core.ejb.ca.sign.ISignSessionLocal"
 *   link="RSASignSession"
 *   
 * @ejb.ejb-external-ref
 *   description="The CA Admin Session"
 *   view-type="local"
 *   ref-name="ejb/CAAdminSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionLocalHome"
 *   business="org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionLocal"
 *   link="CAAdminSession"
 *   
 * @ejb.home
 *   extends="javax.ejb.EJBHome"
 *   local-extends="javax.ejb.EJBLocalHome"
 *   local-class="org.ejbca.core.ejb.log.IProtectedLogSessionLocalHome"
 *   remote-class="org.ejbca.core.ejb.log.IProtectedLogSessionHome"
 *
 * @ejb.interface
 *   extends="javax.ejb.EJBObject"
 *   local-extends="javax.ejb.EJBLocalObject"
 *   local-class="org.ejbca.core.ejb.log.IProtectedLogSessionLocal"
 *   remote-class="org.ejbca.core.ejb.log.IProtectedLogSessionRemote"
 *
 * @jboss.method-attributes
 *   pattern = "get*"
 *   read-only = "true"
 *   
 * @jboss.method-attributes
 *   pattern = "verify*"
 *   read-only = "true"
 *   
 * @jboss.method-attributes
 *   pattern = "find*"
 *   read-only = "true"
 *   
 * @jonas.bean
 *   ejb-name="ProtectedLogSession"
 *
 */
public class ProtectedLogSessionBean extends BaseSessionBean {

	private static final Logger log = Logger.getLogger(ProtectedLogSessionBean.class);

	private static Admin internalAdmin = new Admin(Admin.TYPE_INTERNALUSER);

	private ProtectedLogDataLocalHome protectedLogData = null;
	private ProtectedLogExportDataLocalHome protectedLogExportData = null;
	private ProtectedLogTokenDataLocalHome protectedLogTokenData = null;

	private ICAAdminSessionLocal caAdminSession = null;
	private ICertificateStoreSessionLocal certificateStoreSession = null;
	private ISignSessionLocal signSession = null;
	private IServiceSessionLocal serviceSession = null;
	
	
    public void ejbCreate() {
    }
    
    public void ejbRemove() {
    }

	private ProtectedLogDataLocalHome getProtectedLogData() {
		if (protectedLogData == null) {
			protectedLogData = (ProtectedLogDataLocalHome) ServiceLocator.getInstance().getLocalHome(ProtectedLogDataLocalHome.COMP_NAME);
		}
		return protectedLogData;
	}
	
	private ProtectedLogExportDataLocalHome getProtectedLogExportData() {
		if (protectedLogExportData == null) {
			protectedLogExportData = (ProtectedLogExportDataLocalHome) ServiceLocator.getInstance().getLocalHome(ProtectedLogExportDataLocalHome.COMP_NAME);
		}
		return protectedLogExportData;
	}
	
	private ProtectedLogTokenDataLocalHome getProtectedLogTokenData() {
		if (protectedLogTokenData == null) {
			protectedLogTokenData = (ProtectedLogTokenDataLocalHome) ServiceLocator.getInstance().getLocalHome(ProtectedLogTokenDataLocalHome.COMP_NAME);
		}
		return protectedLogTokenData;
	}
	
	private ICAAdminSessionLocal getCAAdminSession() {
		try {
			if (caAdminSession == null) {
				caAdminSession = ((ICAAdminSessionLocalHome) ServiceLocator.getInstance().getLocalHome(ICAAdminSessionLocalHome.COMP_NAME)).create();
			}
			return caAdminSession;
		} catch (Exception e) {
			log.error("", e);
			throw new EJBException(e);
		}
	}
	
	private ICertificateStoreSessionLocal getCertificateStoreSession() {
		try {
			if (certificateStoreSession == null) {
				certificateStoreSession = ((ICertificateStoreSessionLocalHome) ServiceLocator.getInstance().getLocalHome(ICertificateStoreSessionLocalHome.COMP_NAME)).create();
			}
			return certificateStoreSession;
		} catch (Exception e) {
			throw new EJBException(e);
		}
	}
	
	private ISignSessionLocal getSignSession() {
		try {
			if (signSession == null) {
				signSession = ((ISignSessionLocalHome) ServiceLocator.getInstance().getLocalHome(ISignSessionLocalHome.COMP_NAME)).create();
			}
			return signSession;
		} catch (Exception e) {
			throw new EJBException(e);
		}
	}
	
    private IServiceSessionLocal getServiceSession() {
        try{
            if(serviceSession == null){
            	serviceSession = ((IServiceSessionLocalHome) ServiceLocator.getInstance().getLocalHome(IServiceSessionLocalHome.COMP_NAME)).create();
            }
          } catch(Exception e){
              throw new EJBException(e);
          }
          return serviceSession;
    }

    /**
     * Persists a new token to the database.
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="RequiresNew"
     */
	public void addToken(ProtectedLogToken token) {
		try {
			int tokenType = token.getType();
			switch (tokenType) {
			case ProtectedLogToken.TYPE_CA:
				getProtectedLogTokenData().create(token.getIdentifier(), tokenType, token.getTokenCertificate(), String.valueOf(token.getCAId()));
				break;
			case ProtectedLogToken.TYPE_NONE:
				getProtectedLogTokenData().create(token.getIdentifier(), tokenType, token.getTokenCertificate(), String.valueOf(token.getCAId()));
				break;
			case ProtectedLogToken.TYPE_ASYM_KEY:
			case ProtectedLogToken.TYPE_SYM_KEY:
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(token.getTokenProtectionKey());
				oos.close();
				byte[] rawKeyData = encryptKeyData(baos.toByteArray(), token.getTokenCertificate());
				getProtectedLogTokenData().create(token.getIdentifier(), tokenType, token.getTokenCertificate(), new String(Base64.encode(rawKeyData, false)));
				break;
			}
		} catch (Exception e) {
			log.error("", e);
			throw new EJBException(e);
		}
	}
	
	private ProtectedLogToken protectedLogTokenCache = null;

    /**
     * Fetch a existing token from the database. Caches the last found token.
     * @return null if no token was found
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="Supports"
     */
	public ProtectedLogToken getToken(int tokenIdentifier) {
		if (protectedLogTokenCache != null && protectedLogTokenCache.getIdentifier() == tokenIdentifier) {
			return protectedLogTokenCache;
		}
		ProtectedLogToken protectedLogToken = null;
		try {
			ProtectedLogTokenDataLocal protectedLogTokenDataLocal = getProtectedLogTokenData().findByTokenIdentifier(tokenIdentifier);
			int tokenType = protectedLogTokenDataLocal.getTokenType();
			switch (tokenType) {
			case ProtectedLogToken.TYPE_CA:
				protectedLogToken = new ProtectedLogToken(new Integer(Integer.parseInt(protectedLogTokenDataLocal.getTokenReference())).intValue(), (X509Certificate) protectedLogTokenDataLocal.getTokenCertificate());
				break;
			case ProtectedLogToken.TYPE_NONE:
				protectedLogToken = new ProtectedLogToken();
				break;
			case ProtectedLogToken.TYPE_ASYM_KEY:
			case ProtectedLogToken.TYPE_SYM_KEY:
				byte[] rawKeyData = Base64.decode(protectedLogTokenDataLocal.getTokenReference().getBytes()); 
				ByteArrayInputStream bais = new ByteArrayInputStream(decryptKeyData(rawKeyData, (X509Certificate) protectedLogTokenDataLocal.getTokenCertificate()));
				ObjectInputStream ois = new ObjectInputStream(bais);
				Key key = (Key) ois.readObject();
				ois.close();
				if (key instanceof PrivateKey) {
					protectedLogToken = new ProtectedLogToken((PrivateKey) key, (X509Certificate) protectedLogTokenDataLocal.getTokenCertificate());
				} else {
					protectedLogToken = new ProtectedLogToken((SecretKey) key, (X509Certificate) protectedLogTokenDataLocal.getTokenCertificate());
				}
			}
		} catch (ObjectNotFoundException e) {
			log.error("getToken() could not locate the requested log protection token. This is normal when you use a new token.");
		} catch (Exception e) {
			log.error("", e);
		}
		protectedLogTokenCache = protectedLogToken;
		return protectedLogToken;
	}

	/**
	 * Encrypt key-data with the issuers certificate.
	 */
	private byte[] encryptKeyData(byte[] data, X509Certificate certificate) throws Exception {
		// Use issuing CA for encryption
		int caid = certificate.getIssuerDN().getName().hashCode();
		//int caid = CertTools.getIssuerDN(certificate).hashCode();
		return getCAAdminSession().encryptWithCA(caid, data);
	}

	/**
	 * Decrypt key-data with the issuers certificate.
	 */
	private byte[] decryptKeyData(byte[] data, X509Certificate certificate) throws Exception {
		// Use issuing CA for encryption
		int caid = certificate.getIssuerDN().getName().hashCode();
		//int caid = CertTools.getIssuerDN(certificate).hashCode();
		return getCAAdminSession().decryptWithCA(caid, data);
	}

    /**
     * Find and remove all the specified tokens.
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="RequiresNew"
     */
	public void removeTokens(Integer[] tokenIdentifiers) {
		for (int i=0; i<tokenIdentifiers.length; i++) {
			try {
				// Find token
				ProtectedLogTokenDataLocal protectedLogTokenDataLocal = getProtectedLogTokenData().findByTokenIdentifier(tokenIdentifiers[i].intValue());
				// Nuke token
				protectedLogTokenDataLocal.remove();
			} catch (FinderException e) {
				// Ignore, it's obviously gone..
			} catch (RemoveException e) {
				log.error("", e);
				throw new EJBException(e);
			}
		}
	}
	
    /**
     * Persists a new export to the database.
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="RequiresNew"
     */
	public void addExport(ProtectedLogExportRow protectedLogExportRow) {
		try {
			getProtectedLogExportData().create(
					protectedLogExportRow.getTimeOfExport(), protectedLogExportRow.getExportEndTime(), protectedLogExportRow.getExportStartTime(),
					protectedLogExportRow.getLogDataHash(), protectedLogExportRow.getPreviosExportHash(), protectedLogExportRow.getCurrentHashAlgorithm(),
					protectedLogExportRow.getSignatureCertificateAsByteArray(), protectedLogExportRow.getDeleted(), protectedLogExportRow.getSignature());
		} catch (CreateException e) {
			log.error("", e);
			throw new EJBException(e);
		}
	}

    /**
     * @return the newest export
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="Supports"
     */
	public ProtectedLogExportRow getLastExport() {
		ProtectedLogExportRow protectedLogExportRow = null;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
			String sql="SELECT * FROM ProtectedLogExportData ORDER BY exportEndTime DESC";
			ps = con.prepareStatement(sql);
			ps.setFetchSize(1);
			ps.setMaxRows(1);
			rs = ps.executeQuery();
			if (rs.next()) {
				protectedLogExportRow = new ProtectedLogExportRow(rs);
			}
		} catch (Exception e) {
			log.error("", e);
			throw new EJBException(e);
		} finally {
			JDBCUtil.close(con, ps, rs);
		}
		return protectedLogExportRow;
	}

    /**
     * @return the last signed export
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="Supports"
     */
	public ProtectedLogExportRow getLastSignedExport() {
		ProtectedLogExportRow protectedLogExportRow = null;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
			String sql="SELECT * FROM ProtectedLogExportData WHERE b64Signature IS NOT NULL ORDER BY exportEndTime DESC";
			ps = con.prepareStatement(sql);
			ps.setFetchSize(1);
			ps.setMaxRows(1);
			rs = ps.executeQuery();
			if (rs.next()) {
				protectedLogExportRow = new ProtectedLogExportRow(rs);
			}
		} catch (Exception e) {
			log.error("", e);
			throw new EJBException(e);
		} finally {
			JDBCUtil.close(con, ps, rs);
		}
		return protectedLogExportRow;
	}
	
    /**
     * Persist a new ProtectedLogEvent
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="RequiresNew"
     */
	public void addProtectedLogEventRow(ProtectedLogEventRow protectedLogEventRow) {
		try {
			getProtectedLogData().create(
					protectedLogEventRow.getAdminType(), protectedLogEventRow.getAdmindata(), protectedLogEventRow.getCaid(),
					protectedLogEventRow.getModule(), protectedLogEventRow.getEventTime(), protectedLogEventRow.getUsername(),
					protectedLogEventRow.getCertificateSerialNumber(), protectedLogEventRow.getCertificateIssuerDN(),
					protectedLogEventRow.getEventId(), protectedLogEventRow.getEventComment(),
					protectedLogEventRow.getEventIdentifier(), protectedLogEventRow.getNodeIP(),
					protectedLogEventRow.getLinkedInEventIdentifiers(), protectedLogEventRow.getLinkedInEventsHash(),
					protectedLogEventRow.getCurrentHashAlgorithm(), protectedLogEventRow.getProtectionKeyIdentifier(),
					protectedLogEventRow.getProtectionKeyAlgorithm(), protectedLogEventRow.getProtection());
		} catch (CreateException e) {
			log.error("", e);
			throw new EJBException(e);
		}
	}

    /**
     * @return the requested ProtectedLogRow or null if not found
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="Supports"
     */
	public ProtectedLogEventRow getProtectedLogEventRow(ProtectedLogEventIdentifier identifier) {
		ProtectedLogEventRow protectedLogEventRow = null;
		try {
			ProtectedLogDataLocal protectedLogDataLocal = getProtectedLogData().findByNodeGUIDandCounter(identifier.getNodeGUID(), identifier.getCounter());
			protectedLogEventRow = new ProtectedLogEventRow(protectedLogDataLocal);
		} catch (FinderException e) {
		}
		return protectedLogEventRow;
	}

	/**
	 * Find the newest event for all nodes, except the specified node.
	 * 
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="Supports"
	 */
	public ProtectedLogEventIdentifier[] findNewestProtectedLogEventsForAllOtherNodes(int nodeToExclude, long newerThan) {
		if (newerThan < 0) {
			newerThan = 0;
		}
		ProtectedLogEventRow[] protectedLogEventRows = findNewestProtectedLogEventsForAllOtherNodesInternal(nodeToExclude, newerThan);
		ProtectedLogEventIdentifier[] protectedLogEventIdentifiers = null;
		if (protectedLogEventRows != null) {
			protectedLogEventIdentifiers = new ProtectedLogEventIdentifier[protectedLogEventRows.length];
			for (int i=0; i<protectedLogEventRows.length; i++) {
				protectedLogEventIdentifiers[i] = protectedLogEventRows[i].getEventIdentifier(); 
			}
		}
		return protectedLogEventIdentifiers;
	}

	/**
	 * Find the newest ProtectedLogEvent for all nodes except one, that have an eventTime newer than the requested.
	 */
	private ProtectedLogEventRow[] findNewestProtectedLogEventsForAllOtherNodesInternal(int nodeToExclude, long newerThan) {
		 // TODO: Double check the algo on this one to make it more efficient
		ProtectedLogEventRow[] protectedLogEventRows = null;
		try {
			Collection protectedLogDataLocals = getProtectedLogData().findNewProtectedLogEvents(nodeToExclude, newerThan);
			ArrayList protectedLogEventRowArrayList = new ArrayList(); // <ProtectedLogEventRow>
			Iterator i = protectedLogDataLocals.iterator();
			while (i.hasNext()) {
				ProtectedLogDataLocal protectedLogDataLocal = (ProtectedLogDataLocal) i.next();
				boolean addProtectedLogEventRow = true;
				ProtectedLogEventRow protectedLogEventRowToRemove = null;
				Iterator j = protectedLogEventRowArrayList.iterator();
				while (j.hasNext()) {
					ProtectedLogEventRow protectedLogEventRow = (ProtectedLogEventRow) j.next();
					if (protectedLogDataLocal.getNodeGUID() == protectedLogEventRow.getEventIdentifier().getNodeGUID()) {
						if (protectedLogDataLocal.getEventTime() > protectedLogEventRow.getEventTime()) {
							// Replace if in array and newer (added later)
							protectedLogEventRowToRemove = protectedLogEventRow;
							break;
						} else {
							// Skip if in array and older
							addProtectedLogEventRow = false;
						}
					}
				}
				if (protectedLogEventRowToRemove != null) {
					protectedLogEventRowArrayList.remove(protectedLogEventRowToRemove);
					protectedLogEventRowToRemove = null;
				}
				if (addProtectedLogEventRow) {
					ProtectedLogEventRow newProtectedLogEventRow = new ProtectedLogEventRow(protectedLogDataLocal);
					protectedLogEventRowArrayList.add(newProtectedLogEventRow);
				}
				protectedLogEventRows = (ProtectedLogEventRow[]) protectedLogEventRowArrayList.toArray(new ProtectedLogEventRow[0]);
			}
			// Get newest for every one
		} catch (FinderException e) {
		}
		return protectedLogEventRows;
	}

    /**
     * @return the identifier of the newest protected ProtectedLogEvent
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="Supports"
     */
	public ProtectedLogEventIdentifier findNewestProtectedLogEventRow(int nodeGUID) {
		ProtectedLogEventIdentifier protectedLogEventIdentifier = null;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
			String sql="SELECT nodeGUID, counter FROM ProtectedLogData WHERE b64Protection IS NOT NULL AND nodeGUID=? ORDER BY eventTime DESC";
			ps = con.prepareStatement(sql);
			ps.setInt(1, nodeGUID);
			ps.setFetchSize(1);
			ps.setMaxRows(1);
			rs = ps.executeQuery();
			if (rs.next()) {
				protectedLogEventIdentifier = new ProtectedLogEventIdentifier(rs.getInt(1), rs.getInt(2));
			}
		} catch (Exception e) {
			log.error("", e);
			throw new EJBException(e);
		} finally {
			JDBCUtil.close(con, ps, rs);
		}
		return protectedLogEventIdentifier;
	}

    /**
     * @return the identifier of the newest ProtectedLogEvent, protected or unprotected
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="Supports"
     */
	public ProtectedLogEventIdentifier findNewestLogEventRow(int nodeGUID) {
		ProtectedLogEventIdentifier protectedLogEventIdentifier = null;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
			String sql="SELECT nodeGUID, counter FROM ProtectedLogData WHERE nodeGUID=? ORDER BY eventTime DESC";
			ps = con.prepareStatement(sql);
			ps.setInt(1, nodeGUID);
			ps.setFetchSize(1);
			ps.setMaxRows(1);
			rs = ps.executeQuery();
			if (rs.next()) {
				protectedLogEventIdentifier = new ProtectedLogEventIdentifier(rs.getInt(1), rs.getInt(2));
			}
		} catch (Exception e) {
			log.error("", e);
			throw new EJBException(e);
		} finally {
			JDBCUtil.close(con, ps, rs);
		}
		return protectedLogEventIdentifier;
	}
	
    /**
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="Supports"
     */
	 public ProtectedLogEventIdentifier findNewestProtectedLogEventRow() {
			return findNewestProtectedLogEventRow(true);
	 }

	    /**
	     * @param search for protected events if true or unprotected if not
	     * @return the identifier of the newest ProtectedLogEvent
	     * @ejb.interface-method view-type="both"
	     * @ejb.transaction type="Supports"
	     */
	 public ProtectedLogEventIdentifier findNewestProtectedLogEventRow(boolean isProtected) {
			ProtectedLogEventIdentifier protectedLogEventIdentifier = null;
			Connection con = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
				String sql="SELECT nodeGUID, counter FROM ProtectedLogData WHERE b64Protection IS "+(isProtected ? "NOT" : "")+" NULL ORDER BY eventTime DESC";
				ps = con.prepareStatement(sql);
				ps.setFetchSize(1);
				ps.setMaxRows(1);
				rs = ps.executeQuery();
				if (rs.next()) {
					protectedLogEventIdentifier = new ProtectedLogEventIdentifier(rs.getInt(1), rs.getInt(2));
				}
			} catch (Exception e) {
				log.error("", e);
				throw new EJBException(e);
			} finally {
				JDBCUtil.close(con, ps, rs);
			}
			return protectedLogEventIdentifier;
	 }

	 /**
	  * Find the oldest log-event, protected or unprotected
	  * @ejb.interface-method view-type="both"
	  * @ejb.transaction type="Supports"
	  */
	 public ProtectedLogEventIdentifier findOldestProtectedLogEventRow() {
		ProtectedLogEventIdentifier protectedLogEventIdentifier = null;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
			String sql="SELECT nodeGUID, counter FROM ProtectedLogData ORDER BY eventTime ASC";
			ps = con.prepareStatement(sql);
			ps.setFetchSize(1);
			ps.setMaxRows(1);
			rs = ps.executeQuery();
			if (rs.next()) {
				protectedLogEventIdentifier = new ProtectedLogEventIdentifier(rs.getInt(1), rs.getInt(2));
			}
		} catch (Exception e) {
			log.error("", e);
			throw new EJBException(e);
		} finally {
			JDBCUtil.close(con, ps, rs);
		}
		return protectedLogEventIdentifier;
	}

	 
	 /**
	  * Find the oldest protected log-event
	  * @ejb.interface-method view-type="both"
	  * @ejb.transaction type="Supports"
	  */
	 public ProtectedLogEventIdentifier findOldestSignedProtectedLogEventRow() {
		ProtectedLogEventIdentifier protectedLogEventIdentifier = null;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
			String sql="SELECT nodeGUID, counter FROM ProtectedLogData WHERE b64Protection IS NOT NULL ORDER BY eventTime ASC";
			ps = con.prepareStatement(sql);
			ps.setFetchSize(1);
			ps.setMaxRows(1);
			rs = ps.executeQuery();
			if (rs.next()) {
				protectedLogEventIdentifier = new ProtectedLogEventIdentifier(rs.getInt(1), rs.getInt(2));
			}
		} catch (Exception e) {
			log.error("", e);
			throw new EJBException(e);
		} finally {
			JDBCUtil.close(con, ps, rs);
		}
		return protectedLogEventIdentifier;
	}

	 /**
	  * @return all different nodeGUID that exist between the requested times
	  * @ejb.interface-method view-type="both"
	  * @ejb.transaction type="Supports"
	  */
	 public Integer[] getNodeGUIDs(long exportStartTime, long exportEndTime) {
		ArrayList nodes = new ArrayList();	// <Integer>
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
			String sql="SELECT nodeGUID FROM ProtectedLogData WHERE eventTime >= ? AND eventTime < ? GROUP BY nodeGUID";
			ps = con.prepareStatement(sql);
			ps.setLong(1, exportStartTime);
			ps.setLong(2, exportEndTime);
			rs = ps.executeQuery();
			while (rs.next()) {
				nodes.add(rs.getInt(1));
			}
		} catch (Exception e) {
			log.error("", e);
			throw new EJBException(e);
		} finally {
			JDBCUtil.close(con, ps, rs);
		}
		return (Integer[]) nodes.toArray(new Integer[0]);
	}

	 /**
	  * @return all different nodeGUID that exist
	  * @ejb.interface-method view-type="both"
	  * @ejb.transaction type="Supports"
	  */
	 public Integer[] getAllNodeGUIDs() {
		ArrayList nodes = new ArrayList();	// <Integer>
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
			String sql="SELECT nodeGUID FROM ProtectedLogData GROUP BY nodeGUID";
			ps = con.prepareStatement(sql);
			rs = ps.executeQuery();
			while (rs.next()) {
				nodes.add(rs.getInt(1));
			}
		} catch (Exception e) {
			log.error("", e);
			throw new EJBException(e);
		} finally {
			JDBCUtil.close(con, ps, rs);
		}
		return (Integer[]) nodes.toArray(new Integer[0]);
	}

	 /**
	  * Find all nodeGUIDs where all log events are unprotected.
	  * @ejb.interface-method view-type="both"
	  * @ejb.transaction type="Supports"
	  */
	 public Integer[] getFullyUnprotectedNodeGUIDs() {
		ArrayList nodes = new ArrayList();	// <Integer>
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
			String sql="SELECT nodeGUID FROM ProtectedLogData WHERE b64Protection IS NULL GROUP BY nodeGUID";
			ps = con.prepareStatement(sql);
			rs = ps.executeQuery();
			while (rs.next()) {
				nodes.add(rs.getInt(1));
			}
		} catch (Exception e) {
			log.error("", e);
			throw new EJBException(e);
		} finally {
			JDBCUtil.close(con, ps, rs);
		}
		return (Integer[]) nodes.toArray(new Integer[0]);
	}
	 
    /**
     * @return at most fetchSize legevents between the specified times, oldest first 
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="Supports"
     */
	public ProtectedLogEventRow[] findNextProtectedLogEventRows(long exportStartTime, long exportEndTime, int fetchSize) {
		ArrayList protectedLogEventRows = new ArrayList();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
			// This sql might loose an event between calls if two times are equal
			String sql="SELECT * FROM ProtectedLogData WHERE eventTime >= ? AND eventTime <= ? ORDER BY eventTime ASC";
			ps = con.prepareStatement(sql);
			ps.setLong(1, exportStartTime);
			ps.setLong(2, exportEndTime);
			ps.setFetchSize(fetchSize+1);
			ps.setMaxRows(fetchSize*2); // No more than this at the time..
			rs = ps.executeQuery();
			int count = 0;
			long lastTime = 0;
			while (rs.next()) {
				ProtectedLogEventRow protectedLogEventRow = new ProtectedLogEventRow(rs);
				count++;
				if (count > fetchSize && protectedLogEventRow.getEventTime() != lastTime) {
					break;
				}
				protectedLogEventRows.add(protectedLogEventRow);
				lastTime = protectedLogEventRow.getEventTime();
			}
		} catch (Exception e) {
			log.error("", e);
			throw new EJBException(e);
		} finally {
			JDBCUtil.close(con, ps, rs);
		}
		return (ProtectedLogEventRow[]) protectedLogEventRows.toArray(new ProtectedLogEventRow[0]);
	}
	
    /**
     * Deletes all log events until the reqeusted time
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="RequiresNew"
     */
	public void removeAllUntil(long exportEndTime) {
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
			String sql="DELETE FROM ProtectedLogData WHERE eventTime < ?";
			ps = con.prepareStatement(sql);
			ps.setLong(1, exportEndTime);
			ps.executeUpdate();
		} catch (Exception e) {
			log.error("", e);
			throw new EJBException(e);
		} finally {
			JDBCUtil.close(con, ps, null);
		}
	}

    /**
     * Roll back the export table to the last one with the delete-flag set. This will remove all the export if none has the delet-flag set.
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="Supports"
     */
	public boolean removeAllExports(boolean removeDeletedToo) {
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
			String sql="DELETE FROM ProtectedLogExportData"+(removeDeletedToo ? "":" WHERE deleted=0");
			ps = con.prepareStatement(sql);
			ps.executeUpdate();
		} catch (Exception e) {
			log.error("", e);
			return false;
		} finally {
			JDBCUtil.close(con, ps, null);
		}
		return true;
	}
	
    /**
     * Retrieve a list of token the has been used before, but not after the request time.
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="Supports"
     */
	public Integer[] findTokenIndentifiersUsedOnlyUntil(long exportEndTime) {
		ArrayList protectionKeyIdentifiers = new ArrayList();	//<Integer>
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
			String sql="SELECT protectionKeyIdentifier FROM ProtectedLogData WHERE eventTime < ? GROUP BY protectionKeyIdentifier";
			ps = con.prepareStatement(sql);
			ps.setLong(1, exportEndTime);
			rs = ps.executeQuery();
			while (rs.next()) {
				protectionKeyIdentifiers.add(new Integer(rs.getInt(1)));
			}
			sql="SELECT protectionKeyIdentifier FROM ProtectedLogData WHERE eventTime >= ? GROUP BY protectionKeyIdentifier";
			ps = con.prepareStatement(sql);
			ps.setLong(1, exportEndTime);
			rs = ps.executeQuery();
			while (rs.next()) {
				if (protectionKeyIdentifiers.contains(new Integer(rs.getInt(1)))) {
					protectionKeyIdentifiers.remove(new Integer(rs.getInt(1)));
				}
			}
		} catch (Exception e) {
			log.error("", e);
			throw new EJBException(e);
		} finally {
			JDBCUtil.close(con, ps, rs);
		}
		return (Integer[]) protectionKeyIdentifiers.toArray(new Integer[0]);
	}
	
    /**
     * Verifies that the certificate was valid at the time of signing and that the signature was made by the owner of this certificate.
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="Supports"
     */
	public boolean verifySignature(byte[] data, byte[] signature, X509Certificate certificate, long timeOfSigning) {
		boolean verified = false;
		if (signature == null || data == null) {
			return false;
		}
		if (!verifyCertificate(certificate, timeOfSigning)) {
			return false;
		}
		try {
			// Verify signature of data
			Signature signer = Signature.getInstance(certificate.getSigAlgName(), "BC");
			signer.initVerify(certificate.getPublicKey());
			signer.update(data);
			verified = signer.verify(signature);
		} catch (Exception e) {
			log.error("", e);
		}
		log.debug("<verifySignature exited with " + verified);
		return verified;
	}
	
	private X509Certificate certificateCache = null;
	
    /**
     * Verifies that the certificate was valid at the specified time
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="Supports"
     */
	public boolean verifyCertificate(X509Certificate certificate, long timeOfUse) {
		boolean verified = false;
		try {
			// Verify that this is a certificate signed by a known CA
			if (certificateCache != null && certificate != null && Arrays.equals(certificateCache.getEncoded(), certificate.getEncoded())) {
				// We checked the signature last time, so it's ok.
			} else {
				//int caid = CertTools.getIssuerDN(certificate).hashCode();
				int caid = certificate.getIssuerDN().getName().hashCode();
				CAInfo caInfo = getCAAdminSession().getCAInfo(new Admin(Admin.TYPE_INTERNALUSER), caid);
				CertTools.verify(certificate, caInfo.getCertificateChain());
			}
			// Verify that the certificate is valid
			certificate.checkValidity(new Date(timeOfUse));
			// Verify that the cert wasn't revoked
			RevokedCertInfo revinfo = getCertificateStoreSession().isRevoked(new Admin(certificate), CertTools.getIssuerDN(certificate), certificate.getSerialNumber());
			if (revinfo == null) {
				return false;	// Certificate missing
			} else if (revinfo.getReason() != RevokedCertInfo.NOT_REVOKED && revinfo.getRevocationDate().getTime() <= timeOfUse) {
				return false;	// Certificate was revoked
			}
			verified = true;
		} catch (Exception e) {
			log.error("",e);
		}
		log.debug("<verifyCertificate exited with " + verified);
		return verified;
	}
	
    /**
     * Reservers a slot in the export table.
     * ejb.interface-method view-type="both"
     * @ejb.transaction type="RequiresNew"
     */
	private ProtectedLogExportRow reserveExport(long atLeastThisOld) {
		ProtectedLogExportRow protectedLogExportRow = getLastSignedExport();
		ProtectedLogExportRow unProtectedLogExportRow = getLastExport();
		if (unProtectedLogExportRow != null) {
			if (!unProtectedLogExportRow.equals(protectedLogExportRow)) {
				log.info("Abstained from exporting log since another node is doing this..");
				return null;
			}
			if (!verifySignature(protectedLogExportRow.getAsByteArray(false), protectedLogExportRow.getSignature(),
					protectedLogExportRow.getSignatureCertificate(), protectedLogExportRow.getTimeOfExport())) {
				log.error("Last export is invalid!");
	        	return null;
			}
		}
		// exportStartTime (oldest) is 0 or last exportEndTime
		long exportStartTime = 0;
		if (protectedLogExportRow != null) {
			exportStartTime = protectedLogExportRow.getExportEndTime() + 1;
		}
		long now = new Date().getTime();
		long exportEndTime = new Date().getTime();
		if (exportEndTime > now - atLeastThisOld) {
			exportEndTime = now - atLeastThisOld;
		}
		// Make sure all events before exportEndTime (the newest) are protected
		Integer[] nodeGUIDs = getNodeGUIDs(exportStartTime, exportEndTime);
		for (int i=0; i<nodeGUIDs.length; i++) {
			//log.info("Found " + nodeGUIDs[i] + "..");
			ProtectedLogEventIdentifier newestProtectedLogEventIdentifier = findNewestProtectedLogEventRow(nodeGUIDs[i]);
			if (newestProtectedLogEventIdentifier == null) {
				log.error("No latest event for nodeGUID "+nodeGUIDs[i]);
				return null;
			}
			ProtectedLogEventRow newestProtectedLogEventRow = getProtectedLogEventRow(newestProtectedLogEventIdentifier);
			if (newestProtectedLogEventRow == null) {
				log.error("Could no fetch latest event for nodeGUID "+nodeGUIDs[i]);
				return null;
			}
			// If the latest signed event for a node isn't an stop-event we need to take the eventTime into account
			long currentTime = newestProtectedLogEventRow.getEventTime();
			if (newestProtectedLogEventRow.getEventId() != LogConstants.EVENT_SYSTEM_STOPPED_LOGGING && currentTime < exportEndTime) {
				exportEndTime = currentTime;
			}
			// Since this is now the newest event, we are going to use it's token or issuing CA-token. We want to know if it's has one.
			ProtectedLogToken plt = getToken(newestProtectedLogEventRow.getProtectionKeyIdentifier());
			if (plt == null) {
				log.error("No token in use for nodeGUID " + newestProtectedLogEventRow.getEventIdentifier().getNodeGUID() + " counter " + newestProtectedLogEventRow.getEventIdentifier().getNodeGUID()+".");
				return null;
			}
			// And that it is alright
			byte[] dummy = "testing if token is working".getBytes();
			if (plt.getType() == ProtectedLogToken.TYPE_CA && plt.protect(dummy) == null) {
				throw new EJBException("Token not available.");
			}
			if (plt.getType() != ProtectedLogToken.TYPE_CA) {
				// If it is a soft token we need to verify that is issuing CA is online.
				X509Certificate cert = plt.getTokenCertificate();
				int caId = cert.getIssuerDN().getName().hashCode();
				//int caId = CertTools.getIssuerDN(cert).hashCode();
				try {
					getSignSession().signData(dummy, caId, SecConst.CAKEYPURPOSE_CERTSIGN);
				} catch (Exception e) {
					throw new EJBException("Token not available.");
				}
			}
		}
		try {
			getProtectedLogExportData().create(0, exportEndTime, exportStartTime, null, null, null, null, false, null);
			return getLastExport();
		} catch (CreateException e) {
			log.error("", e);
			throw new EJBException(e);
		}
	}
	
    /**
     * Either completes the reserved export if success if true or removes it. 
     * ejb.interface-method view-type="both"
     * @ejb.transaction type="RequiresNew"
     */
	private void completeExport(ProtectedLogExportRow protectedLogExportRow, boolean success) {
		try {
			ProtectedLogExportDataLocal protectedLogExportDataLocal = getProtectedLogExportData().findByExportStartTime(protectedLogExportRow.getExportStartTime());
			if (success) {
				protectedLogExportDataLocal.setTimeOfExport(protectedLogExportRow.getTimeOfExport());
				protectedLogExportDataLocal.setLogDataHash(protectedLogExportRow.getLogDataHash());
				protectedLogExportDataLocal.setPreviosExportHash(protectedLogExportRow.getPreviosExportHash());
				protectedLogExportDataLocal.setCurrentHashAlgorithm(protectedLogExportRow.getCurrentHashAlgorithm());
				protectedLogExportDataLocal.setSignatureCertificate(protectedLogExportRow.getSignatureCertificateAsByteArray());
				protectedLogExportDataLocal.setDeleted(protectedLogExportRow.getDeleted());
				protectedLogExportDataLocal.setSignature(protectedLogExportRow.getSignature());
			} else {
				protectedLogExportDataLocal.remove();
			}
		} catch (Exception e) {
			log.error("", e);
			throw new EJBException(e);
		}
	}
	
    /**
     * Perform a query and convert to a Collection of LogEntry
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="Required"
     */
	public Collection performQuery(String sqlQuery) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// Construct SQL query.
			con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
			ps = con.prepareStatement(sqlQuery);
			//ps.setFetchDirection(ResultSet.FETCH_REVERSE);
			ps.setFetchSize(LogConstants.MAXIMUM_QUERY_ROWCOUNT + 1);
			// Execute query.
			rs = ps.executeQuery();
			// Assemble result.
			ArrayList returnval = new ArrayList();
			while (rs.next() && returnval.size() <= LogConstants.MAXIMUM_QUERY_ROWCOUNT) {
				// Use pk 0
				LogEntry data = new LogEntry(0, rs.getInt(2), rs.getString(3), rs.getInt(4), rs.getInt(5), new Date(rs.getLong(6)), rs.getString(7), 
						rs.getString(8), rs.getInt(9), rs.getString(10));
				// Verify each result
				String verified = "VERIFY_FAILED";
				if (verifyProtectedLogEventRow(new ProtectedLogEventRow(protectedLogData.findByPrimaryKey(rs.getString(1))))) {
					verified = "VERIFY_SUCCESS";
				}
				data.setVerifyResult(verified);
				returnval.add(data);
			}
			return returnval;
		} catch (Exception e) {
			throw new EJBException(e);
		} finally {
			JDBCUtil.close(con, ps, rs);
		}
	}
	
    /**
     * Recurses forward in time, verifying each hash of the previous event until a signature is reached which is verified.
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="Required"
     */
	public boolean verifyProtectedLogEventRow(ProtectedLogEventRow protectedLogEventRow) {
		// If signed - verify this PLER
		if (protectedLogEventRow.getProtection() != null) {
			ProtectedLogToken protectedLogToken = getToken(protectedLogEventRow.getProtectionKeyIdentifier());
			try {
				return protectedLogToken.verify(protectedLogEventRow.getAsByteArray(false), protectedLogEventRow.getProtection());
			} catch (Exception e) {
				log.debug("Could not verify.", e);
				return false;
			}
		} else {
			// Fetch next from this node
			ProtectedLogEventIdentifier nextProtectedLogEventIdentifier = new ProtectedLogEventIdentifier(
					protectedLogEventRow.getEventIdentifier().getNodeGUID(), protectedLogEventRow.getEventIdentifier().getCounter()+1);
			ProtectedLogEventRow nextProtectedLogEventRow = getProtectedLogEventRow(nextProtectedLogEventIdentifier);
			if (nextProtectedLogEventRow == null) {
				return false;
			}
			// Make sure that one links in all hashes properly
			ProtectedLogEventIdentifier[] linkedInEventIdentifiers = nextProtectedLogEventRow.getLinkedInEventIdentifiers();
			// Create a hash of the linked in nodes
			MessageDigest messageDigest = null;
			try {
				messageDigest = MessageDigest.getInstance(nextProtectedLogEventRow.getCurrentHashAlgorithm(), "BC");
			} catch (NoSuchAlgorithmException e) {
				throw new EJBException(e);
			} catch (NoSuchProviderException e) {
				throw new EJBException(e);
			}
			// Chain nodes with hash
			byte[] linkedInEventsHash = null;
			if (linkedInEventIdentifiers != null && linkedInEventIdentifiers.length != 0) {
				for (int i=0; i<linkedInEventIdentifiers.length; i++) {
					messageDigest.update(getProtectedLogEventRow(linkedInEventIdentifiers[i]).calculateHash());
					ProtectedLogEventRow tmpDebug = getProtectedLogEventRow(linkedInEventIdentifiers[i]);
					//log.info(" ("+linkedInEventIdentifiers[i].getNodeGUID()+"," + linkedInEventIdentifiers[i].getCounter()+") has hash " + tmpDebug.calculateHash()[0] + "...");
				}
				linkedInEventsHash = messageDigest.digest();
			}
			if (Arrays.equals(linkedInEventsHash, nextProtectedLogEventRow.getLinkedInEventsHash())) {
				// Recuse through the chain until a protected row is found.
				return verifyProtectedLogEventRow(nextProtectedLogEventRow);
			}
		}
		return false;
	}

    /**
     * Verify entire log
     * Verify that log hasn't been frozen for any node
     * Verify that each protect operation had a valid certificate and is not about to expire without a valid replacement
     * Verify that no nodes exists that haven't been processed
     * 
     * Starts at the specified event and traverses through the chain of linked in events, following one nodeGUID at
     * the time. The newest signature for each node is verifed and the link-in hashes for each event. The
     * verification continues node by node, until the oldest event is reached or the time where an verified exporting
     * delete was last made.
     *  
     * @param freezeThreshold longest allowed time to newest ProtectedLogEvent of any node.
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="Supports"
     */
	public ProtectedLogEventIdentifier verifyEntireLog(ProtectedLogActions protectedLogActions, long freezeThreshold) {
		ArrayList newestProtectedLogEventRows =new ArrayList();	//<ProtectedLogEventRow>
		ArrayList knownNodeGUIDs =new ArrayList();	//<Integer>
		ArrayList processedNodeGUIDs =new ArrayList();	// <Integer>
		long stopTime = 0;
		// The time right after last chunk was exported (if it was deleted too)
		long lastDeletingExportTime = 0;
		ArrayList lastExportProtectedLogIdentifier = new ArrayList(); // <ProtectedLogIdentifier>
		ProtectedLogExportRow protectedLogExportRow = getLastSignedExport();
		if (protectedLogExportRow != null && protectedLogExportRow.getDeleted()) {
			if (!verifySignature(protectedLogExportRow.getAsByteArray(false), protectedLogExportRow.getSignature(),
					protectedLogExportRow.getSignatureCertificate(), protectedLogExportRow.getTimeOfExport())) {
				protectedLogActions.takeActions(IProtectedLogAction.CAUSE_INVALID_EXPORT);
				return null;
			}
			lastDeletingExportTime = protectedLogExportRow.getExportEndTime();
			// Fetch the identifier for this/these last event
			try {
				Collection protectedLogDataLocals = getProtectedLogData().findProtectedLogEventsByTime(lastDeletingExportTime);
				Iterator i = protectedLogDataLocals.iterator();
				while (i.hasNext()) {
					ProtectedLogDataLocal protectedLogDataLocal = (ProtectedLogDataLocal) i.next();
					if (verifyProtectedLogEventRow(new ProtectedLogEventRow(protectedLogDataLocal))) {
						lastExportProtectedLogIdentifier.add(new ProtectedLogEventIdentifier(protectedLogDataLocal.getNodeGUID(), protectedLogDataLocal.getCounter()));
					}
				}
			} catch (FinderException e) {
				log.error("", e);
				throw new EJBException(e);
			}
		}
		// Find newest protected LogEventRow.
		ProtectedLogEventIdentifier newestProtectedLogEventIdentifier = findNewestProtectedLogEventRow();
		if (newestProtectedLogEventIdentifier == null) {
			log.error("Log is empty or unprotected.");
			protectedLogActions.takeActions(IProtectedLogAction.CAUSE_EMPTY_LOG);
			return null;
		}
		knownNodeGUIDs.add(newestProtectedLogEventIdentifier.getNodeGUID());
		ProtectedLogEventRow tmpPLER = getProtectedLogEventRow(newestProtectedLogEventIdentifier);
		if (tmpPLER == null) {
			protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MISSING_LOGROW);
			return newestProtectedLogEventIdentifier;
		}
		newestProtectedLogEventRows.add(tmpPLER);
		// Find the oldest LogEventRow and save this time as “stoptime”.
		ProtectedLogEventIdentifier oldestProtectedLogEventIdentifier = findOldestProtectedLogEventRow();
		ProtectedLogEventIdentifier oldestSignedProtectedLogEventIdentifier = findOldestSignedProtectedLogEventRow();
		stopTime = getProtectedLogEventRow(oldestProtectedLogEventIdentifier).getEventTime();
		
		// Keep track of all found nodes and their newest known LogEventRow. Also keep track of which nodes already has been verified.
		
		// While there still are unverified nodes left: verify the node-chain with the newest LogEventRow until “stoptime” or a the latest
		//  verified exportEndTime is reached. The later only applies if export is configured to remove the exported events.
		while (knownNodeGUIDs.size() > processedNodeGUIDs.size()) {
			// Pick newest know event of newestProtectedLogEventIdentifiers
			ProtectedLogEventRow nextProtectedLogEventRow = null;
			Iterator iterator = newestProtectedLogEventRows.iterator();
			while (iterator.hasNext()) {
				ProtectedLogEventRow i = (ProtectedLogEventRow) iterator.next();
				if ( !processedNodeGUIDs.contains(i.getEventIdentifier().getNodeGUID())
						&&  (nextProtectedLogEventRow == null || nextProtectedLogEventRow.getEventTime() < i.getEventTime()) ) {
					nextProtectedLogEventRow = i;
				}
			}
			int nextProtectedLogEventRowNodeGUID = nextProtectedLogEventRow.getEventIdentifier().getNodeGUID();
			processedNodeGUIDs.add(nextProtectedLogEventRowNodeGUID);
			//log.info("Processing node " + nextProtectedLogEventRowNodeGUID);

			// Verify that log hasn't been frozen for any node
			ProtectedLogEventIdentifier newestNodeProtectedLogEventIdentifier = findNewestProtectedLogEventRow(nextProtectedLogEventRowNodeGUID);
			ProtectedLogEventRow newestNodeProtectedLogEventRow = getProtectedLogEventRow(newestNodeProtectedLogEventIdentifier);
			if (newestNodeProtectedLogEventRow == null) {
				log.error("Could not retrieve ProtectedLogEvent nodeGUID="+newestNodeProtectedLogEventIdentifier.getNodeGUID()+
						" counter="+newestNodeProtectedLogEventIdentifier.getCounter());
				protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MISSING_LOGROW);
				return newestNodeProtectedLogEventIdentifier;
			}
			if (newestNodeProtectedLogEventRow.getEventId() != LogConstants.EVENT_SYSTEM_STOPPED_LOGGING 
					&& newestNodeProtectedLogEventRow.getEventTime() < new Date().getTime() - freezeThreshold ) {
				protectedLogActions.takeActions(IProtectedLogAction.CAUSE_FROZEN);
				return newestNodeProtectedLogEventIdentifier;
			}
			
			// while not reached stoptime
			boolean isTopSignatureVerified = false;
			boolean thoroughMode = false;
			while (nextProtectedLogEventRow != null && nextProtectedLogEventRow.getEventTime() >= stopTime && nextProtectedLogEventRow.getEventTime() > lastDeletingExportTime) {
				ProtectedLogEventIdentifier nextProtectedLogEventIdentifier = nextProtectedLogEventRow.getEventIdentifier();
				// Verify current signature (if first or in thorough mode)
				if (thoroughMode || !isTopSignatureVerified) {
					ProtectedLogToken currentToken = getToken(nextProtectedLogEventRow.getProtectionKeyIdentifier());
					if (currentToken == null) {
						protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MISSING_TOKEN);
						return nextProtectedLogEventIdentifier;
					}
					isTopSignatureVerified = true;
					if (!currentToken.verify(nextProtectedLogEventRow.getAsByteArray(false), nextProtectedLogEventRow.getProtection())) {
						protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MODIFIED_LOGROW);
						return nextProtectedLogEventIdentifier;
					}
					if (!verifyCertificate(currentToken.getTokenCertificate(), nextProtectedLogEventRow.getEventTime())) {
						protectedLogActions.takeActions(IProtectedLogAction.CAUSE_INVALID_TOKEN);
						return nextProtectedLogEventIdentifier;
					}
				}
				// Get linked in events
				ProtectedLogEventIdentifier[] linkedInEventIdentifiers = nextProtectedLogEventRow.getLinkedInEventIdentifiers();
				if (linkedInEventIdentifiers == null || linkedInEventIdentifiers.length == 0) {
					// This is ok if this a startevent..
					//|| !( nextProtectedLogEventIdentifier.equals(findOldestSignedProtectedLogEventRow()) || nextProtectedLogEventIdentifier.equals(findOldestProtectedLogEventRow()) )
					if (nextProtectedLogEventIdentifier.getCounter() != 0) {
						protectedLogActions.takeActions(IProtectedLogAction.CAUSE_INTERNAL_ERROR);
						return nextProtectedLogEventRow.getEventIdentifier();
					}
				}
				//  Verify hash for all events
				MessageDigest messageDigest = null;
				try {
					messageDigest = MessageDigest.getInstance(nextProtectedLogEventRow.getCurrentHashAlgorithm(), "BC");
				} catch (Exception e) {
					log.error("", e);
					throw new EJBException(e);
				}
				// Is the the current event nextProtectedLogEventRow the last non-delete-exported event?
				boolean isLastEvent = false;
				Iterator iterator3 = lastExportProtectedLogIdentifier.iterator();
				while (iterator3.hasNext()) {
					ProtectedLogEventIdentifier plei = (ProtectedLogEventIdentifier) iterator3.next();
					if (plei.equals(nextProtectedLogEventRow.getEventIdentifier())) {
						isLastEvent = true;
					}
				}
				//lastExportProtectedLogIdentifier.contains(nextProtectedLogEventRow.getEventIdentifier());
				byte[] linkedInEventsHash = null;
				if (!isLastEvent && linkedInEventIdentifiers != null && linkedInEventIdentifiers.length != 0) {
					// If one of the linked in identifiers points to a ProtectedLogEventRow where eventTime is the same as
					// the latest Export and that export is "deleted" and has a valid signature, we can't verify it's linked in hash..
					// If not every log-row is signed in a multi-node environment, this would lead to unverified log-rows.
					boolean isLinkingInLast = false;
					for (int i=0; i<linkedInEventIdentifiers.length; i++) {
						if (lastExportProtectedLogIdentifier.contains(linkedInEventIdentifiers[i])) {
							isLinkingInLast = true;
							break;
						}
					}
					if (!isLinkingInLast) {
						// Verify the hash of all the linked in events.
						for (int i=0; i<linkedInEventIdentifiers.length; i++) {
							ProtectedLogEventRow fetchedProtectedLogEventRow =getProtectedLogEventRow(linkedInEventIdentifiers[i]);
							if (fetchedProtectedLogEventRow == null) {
								protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MISSING_LOGROW);
								return linkedInEventIdentifiers[i];
							} else {
								messageDigest.update(fetchedProtectedLogEventRow.calculateHash());
							}
						}
						linkedInEventsHash = messageDigest.digest();
						if ((linkedInEventsHash != null || nextProtectedLogEventRow.getLinkedInEventsHash() != null) &&
								!Arrays.equals(linkedInEventsHash, nextProtectedLogEventRow.getLinkedInEventsHash())) {
							log.info("Linked in events hash has changed. (" + nextProtectedLogEventRow.getEventIdentifier().getNodeGUID() + "," + nextProtectedLogEventRow.getEventIdentifier().getCounter()+")");
							protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MODIFIED_LOGROW); 
							return nextProtectedLogEventRow.getEventIdentifier();
						}
					}
				}
				nextProtectedLogEventRow = null;
				if (!isLastEvent && linkedInEventIdentifiers != null && linkedInEventIdentifiers.length != 0) {
					// For each linked in, if any of them is newer then the one found in newestProtectedLogEventIdentifiers for each node → verify event signature and replace
					for (int l=0; l<linkedInEventIdentifiers.length; l++) {
						ProtectedLogEventIdentifier k = linkedInEventIdentifiers[l];
						ProtectedLogEventRow currentProtectedLogEventRow = getProtectedLogEventRow(k);
						if (currentProtectedLogEventRow == null) {
							protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MISSING_LOGROW);
							return currentProtectedLogEventRow.getEventIdentifier();
						}
						ProtectedLogEventRow toRemove = null;
						boolean knownNodeGUID = false;
						Iterator iterator2 = newestProtectedLogEventRows.iterator();
						while (iterator2.hasNext()) {
							ProtectedLogEventRow j = (ProtectedLogEventRow) iterator2.next();
							if (j.getEventIdentifier().getNodeGUID() == currentProtectedLogEventRow.getEventIdentifier().getNodeGUID()
									&& j.getEventTime() < currentProtectedLogEventRow.getEventTime()) {
								toRemove = j;
								break;
							}
							if (k.getNodeGUID() == j.getEventIdentifier().getNodeGUID()) {
								knownNodeGUID = true;
							}
						}
						//log.info("Current linked in GUID " + i.getNodeGUID() + " and counter " + i.getCounter());
						if (!knownNodeGUID) {
							//log.info("Found previously unknown node " + k.getNodeGUID());
							ProtectedLogToken currentToken = getToken(currentProtectedLogEventRow.getProtectionKeyIdentifier());
							if (!currentToken.verify(currentProtectedLogEventRow.getAsByteArray(false), currentProtectedLogEventRow.getProtection())) {
								protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MODIFIED_LOGROW);
								return currentProtectedLogEventRow.getEventIdentifier();
							}
							newestProtectedLogEventRows.add(currentProtectedLogEventRow);
							knownNodeGUIDs.add(k.getNodeGUID());
						} else if (toRemove != null) {
							ProtectedLogToken currentToken = getToken(currentProtectedLogEventRow.getProtectionKeyIdentifier());
							if (currentToken == null) {
								protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MISSING_TOKEN);
								return currentProtectedLogEventRow.getEventIdentifier();
							}
							if (!currentToken.verify(currentProtectedLogEventRow.getAsByteArray(false), currentProtectedLogEventRow.getProtection())) {
								protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MODIFIED_LOGROW);
								return currentProtectedLogEventRow.getEventIdentifier();
							}
							newestProtectedLogEventRows.remove(toRemove);
							newestProtectedLogEventRows.add(currentProtectedLogEventRow);
						}
						// Find next for this chain 
						if (nextProtectedLogEventRowNodeGUID == k.getNodeGUID()) {
							nextProtectedLogEventRow = currentProtectedLogEventRow;
						}
					}
				}
			}
		}
		//Verify that no nodes exists that hasn't been processed
		Integer[] everyExistingNodeGUID = getAllNodeGUIDs();
		for (int i=0; i<everyExistingNodeGUID.length; i++) {
			if (!processedNodeGUIDs.contains(everyExistingNodeGUID[i])) {
				protectedLogActions.takeActions(IProtectedLogAction.CAUSE_UNVERIFYABLE_CHAIN);
				return new ProtectedLogEventIdentifier(everyExistingNodeGUID[i], 0);
			}
		}

		// If something is wrong the failed verified ProtectedLogEventRowIdentifier is returned.
		return null;
	}
	
    /**
     * Fetches a known token from the database or creates a new one, depending on the configuration.
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="RequiresNew"
     */
	public ProtectedLogToken getProtectedLogToken(Properties properties) {
		ProtectedLogToken protectedLogToken = null;
		try {
			// Get ProtectedLogToken from configuration data
			String protectionTokenReferenceType = properties.getProperty(ProtectedLogDevice.CONFIG_TOKENREFTYPE, ProtectedLogDevice.CONFIG_TOKENREFTYPE_NONE);
			String protectionTokenReference = properties.getProperty(ProtectedLogDevice.CONFIG_TOKENREF, "AdminCA1");
			String protectionTokenKeyStoreAlias = properties.getProperty(ProtectedLogDevice.CONFIG_KEYSTOREALIAS, "defaultKey");
			String protectionTokenKeyStorePassword = properties.getProperty(ProtectedLogDevice.CONFIG_KEYSTOREPASSWORD, "foo123");
			X509Certificate protectedLogTokenCertificate = null;
			if ( ProtectedLogDevice.CONFIG_TOKENREFTYPE_CANAME.equalsIgnoreCase(protectionTokenReferenceType) ) {
				// Use a CA as token
				CAInfo caInfo = getCAAdminSession().getCAInfo(internalAdmin, protectionTokenReference);
				if (caInfo == null) {
					// Revert to the "none" token.
					log.error("Requested CA was not found. Reverting to \"none\".");
					protectedLogToken = new ProtectedLogToken();
				}
				protectedLogTokenCertificate = (X509Certificate) caInfo.getCertificateChain().iterator().next();
				protectedLogToken = new ProtectedLogToken(caInfo.getCAId(), protectedLogTokenCertificate);
			} else if (ProtectedLogDevice.CONFIG_TOKENREFTYPE_NONE.equalsIgnoreCase(protectionTokenReferenceType)) {
				// This is the default key used during startup. It can't sign or verify anything.
				protectedLogToken = new ProtectedLogToken();
			} else if (ProtectedLogDevice.CONFIG_TOKENREFTYPE_DATABASE.equalsIgnoreCase(protectionTokenReferenceType)) {
				// protectionTokenReference contains token id i database
				protectedLogToken = getToken(Integer.parseInt(protectionTokenReference, 10));
			} else {
				InputStream is = null;
				if (ProtectedLogDevice.CONFIG_TOKENREFTYPE_URI.equalsIgnoreCase(protectionTokenReferenceType)) {
					// Use a URI as token
					is = (new URI(protectionTokenReference)).toURL().openStream();
				} else if (ProtectedLogDevice.CONFIG_TOKENREFTYPE_CONFIG.equalsIgnoreCase(protectionTokenReferenceType)) {
					// protectionTokenReference contains b64encoded JKS
					is = new ByteArrayInputStream(Base64.decode(protectionTokenReference.getBytes()));
				}
				KeyStore keyStore = null;
				Key protectionKey = null;
				keyStore = KeyStore.getInstance("JKS");
				keyStore.load(is, protectionTokenKeyStorePassword.toCharArray());
				protectionKey = keyStore.getKey(protectionTokenKeyStoreAlias, protectionTokenKeyStorePassword.toCharArray());
				protectedLogTokenCertificate = (X509Certificate) keyStore.getCertificate(protectionTokenKeyStoreAlias);
				// Validate certificate here
				if (!verifyCertificate(protectedLogTokenCertificate, new Date().getTime())) {
					log.error("Invalid token certificate.");
					return null;
				}
				if (protectionKey instanceof PrivateKey) {
					protectedLogToken = new ProtectedLogToken((PrivateKey) protectionKey, protectedLogTokenCertificate);
				} else {
					// TODO: Verify custom extension of cert so we know the certoficate belongs to this symmetric key
					protectedLogToken = new ProtectedLogToken((SecretKey) protectionKey, protectedLogTokenCertificate);
				}
			}
			// Check if token already exists and add it to the list of used tokens otherwise
			int tokenIdentifier = protectedLogToken.getIdentifier();
			if (getToken(tokenIdentifier) == null) {
				addToken(protectedLogToken);
			}
		} catch (Exception e) {
			log.error("", e);
		}
		return protectedLogToken;
	}

    /**
     * Insert a new signed stop event for each unsigned node-chain in a "near future" and let the real node chain in these events..
     * 
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="RequiresNew"
     */
	public boolean signAllUnsignedChains(Properties properties, boolean signAll) {
		// Find last unsigned event for all nodes, sorted by time, oldest first
		Integer[] nodeGUIDs = null;
		if (signAll) {
			// Add protection to chains that have frozen
			Integer[] allNodeGUIDs = getNodeGUIDs(0, new Date().getTime());
			ArrayList nodeGUIDsArray = new ArrayList();
			long freezeTreshold = Long.parseLong(properties.getProperty(ProtectedLogVerifier.CONF_FREEZE_THRESHOLD, ProtectedLogVerifier.DEFAULT_FREEZE_THRESHOLD)) * 60 * 1000;
			for (int i=0; i<allNodeGUIDs.length; i++) {
				ProtectedLogEventRow protectedLogEventRow = getProtectedLogEventRow(findNewestLogEventRow(allNodeGUIDs[i]));
				if (protectedLogEventRow != null && protectedLogEventRow.getEventTime() < new Date().getTime() - freezeTreshold &&
						protectedLogEventRow.getEventId() != LogConstants.EVENT_SYSTEM_STOPPED_LOGGING) {
					nodeGUIDsArray.add(allNodeGUIDs[i]);
				}
			}
			nodeGUIDs = (Integer[]) nodeGUIDsArray.toArray(new Integer[0]);
		} else {
			// Add protected to chains that have no previous protection
			nodeGUIDs = getFullyUnprotectedNodeGUIDs();
		}
		// Get latest real signed event
		ProtectedLogEventRow newestProtectedLogEventRow = getProtectedLogEventRow(findNewestProtectedLogEventRow());
		List unsignedNodeGUIDs = Arrays.asList(nodeGUIDs);
		Iterator i = unsignedNodeGUIDs.iterator();
		while (i.hasNext()) {
			Integer nodeGUID = (Integer) i.next();
			if (nodeGUID.intValue() == newestProtectedLogEventRow.getEventIdentifier().getNodeGUID()) {
				continue;
			}
			// Find last event, signed or unsigned
			ProtectedLogEventIdentifier currentProtectedLogEventIdentifier = findNewestLogEventRow(nodeGUID);
			ProtectedLogEventRow currentProtectedLogEventRow = getProtectedLogEventRow(currentProtectedLogEventIdentifier);
			// Create a new event that links in this one
			ProtectedLogEventIdentifier[] linkedInEventIdentifiers = new ProtectedLogEventIdentifier[1];
			linkedInEventIdentifiers[0] = currentProtectedLogEventIdentifier;
			MessageDigest messageDigest;
			try {
				messageDigest = MessageDigest.getInstance(newestProtectedLogEventRow.getCurrentHashAlgorithm(), "BC");
			} catch (Exception e) {
				log.error("", e);
				return false;
			}
			messageDigest.update(currentProtectedLogEventRow.calculateHash());
			byte[] linkedInEventsHash = messageDigest.digest();
			ProtectedLogEventRow newProtectedLogEventRow = new ProtectedLogEventRow(0,null,0,0,(new Date().getTime()+10000), null, null, null,
					LogConstants.EVENT_SYSTEM_STOPPED_LOGGING, "Node-chain was accepted by CLI.",
					new ProtectedLogEventIdentifier(currentProtectedLogEventIdentifier.getNodeGUID(), currentProtectedLogEventIdentifier.getCounter()+1),
					null, linkedInEventIdentifiers, linkedInEventsHash, newestProtectedLogEventRow.getCurrentHashAlgorithm(),
					newestProtectedLogEventRow.getProtectionKeyIdentifier(), newestProtectedLogEventRow.getProtectionKeyAlgorithm(), null);
			// Sign new event
			newProtectedLogEventRow.setProtection(getProtectedLogToken(properties).protect(newProtectedLogEventRow.getAsByteArray(false)));
			// Persist event
			addProtectedLogEventRow(newProtectedLogEventRow);
			log.info("Accepted node-chain "+currentProtectedLogEventIdentifier.getNodeGUID()+".");
		}
		return true;
	}
	
    /**
     * Optionally exports and then deletes the entire log and export table.
     * Writes an export to the database with the deleted events' times
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="RequiresNew"
     */
	public boolean resetEntireLog(boolean export, Properties exportHandlerProperties) {
		// Disable services first.
		ServiceConfiguration serviceConfiguration = getServiceSession().getService(internalAdmin, ProtectedLogExportWorker.DEFAULT_SERVICE_NAME);
		if (serviceConfiguration != null) {
			serviceConfiguration.setActive(false);
			getServiceSession().changeService(internalAdmin, ProtectedLogExportWorker.DEFAULT_SERVICE_NAME, serviceConfiguration);
		}
		serviceConfiguration = getServiceSession().getService(internalAdmin, ProtectedLogVerificationWorker.DEFAULT_SERVICE_NAME);
		if (serviceConfiguration != null) {
			serviceConfiguration.setActive(false);
			getServiceSession().changeService(internalAdmin, ProtectedLogExportWorker.DEFAULT_SERVICE_NAME, serviceConfiguration);
		}
		// Wait for already running instances of the services to stop. Time-out after x minutes.
		ProtectedLogVerifier protectedLogVerifier = ProtectedLogVerifier.instance();
		ProtectedLogExporter protectedLogExporter = ProtectedLogExporter.instance();
		long waitedTime = 0;
		try {
			while ( (protectedLogVerifier.isRunning() || protectedLogExporter.isRunning()) && waitedTime < 60*60*1000) {	// 1h timeout
				Thread.sleep(1000);
				waitedTime += 1000;
				if (waitedTime % (60*1000) == 0) {
					log.debug("Waiting for services to stop.. Has waited "+(waitedTime/1000)+" seconds.");
				}
			}
			if ((protectedLogVerifier.isRunning() || protectedLogExporter.isRunning())) {
				return false;	// FAILURE
			}
		} catch (InterruptedException e) {
			log.error("", e);
		}
		try {
			IProtectedLogExportHandler protectedLogExportHandler = null;
			try {
				Class implClass = Class.forName(exportHandlerProperties.getProperty("exportservice.exporthandler", ProtectedLogCMSExportHandler.class.getName()).trim());
				protectedLogExportHandler =(IProtectedLogExportHandler) implClass.newInstance();
			} catch (Exception e1) {
				log.error("", e1);
				return false;
			}
			String currentHashAlgorithm = exportHandlerProperties.getProperty("exportservice.hashAlgorithm", "SHA-256");
			// Nuke export table
			if (!removeAllExports(true)) {
				return false;
			}
			if (!export) {
				return true;
			}
			// Do an export but don't validate anything and do not perform any actions
			// TODO: This is ripped from export.. Try to merge some functionality into new method.
			ProtectedLogExportRow reservedProtectedLogExportRow = reserveExport(0);
			boolean success = false;
			boolean deleteAfterExport = true;
			ProtectedLogEventRow lastProtectedLogEventRow = getProtectedLogEventRow(findNewestProtectedLogEventRow());
			long exportEndTime = lastProtectedLogEventRow.getEventTime();
			long exportStartTime = 0;
			try {
				protectedLogExportHandler.init(exportHandlerProperties, exportEndTime, exportStartTime, true); 
				ProtectedLogExportRow protectedLogExportRow = getLastSignedExport();
				// Process all LogEventRows in the timespan chronologically, oldest first
				// By sorting for newest first caching would be easier, but that would result in exported files with newest event first
				int fetchSize = 20;
				ProtectedLogEventRow[] protectedLogEventRows = new ProtectedLogEventRow[0];
				MessageDigest messageDigest = MessageDigest.getInstance(currentHashAlgorithm, "BC");
				ProtectedLogEventRow newesetExportedProtectedLogEventRow = null;
				long lastLoopTime = exportStartTime;
				do {
					protectedLogEventRows = findNextProtectedLogEventRows(lastLoopTime, exportEndTime, fetchSize);
					for (int i=0; i<protectedLogEventRows.length; i++) {
						// Extract data from LogEventRow and send to interface + digest
						messageDigest.digest(protectedLogEventRows[i].getLogDataAsByteArray());
						if (!protectedLogExportHandler.update(protectedLogEventRows[i].getAdminType(), protectedLogEventRows[i].getAdmindata(), protectedLogEventRows[i].getCaid(),
								protectedLogEventRows[i].getModule(), protectedLogEventRows[i].getEventTime(), protectedLogEventRows[i].getUsername(),
								protectedLogEventRows[i].getCertificateSerialNumber(), protectedLogEventRows[i].getCertificateIssuerDN(), protectedLogEventRows[i].getEventId(),
								protectedLogEventRows[i].getEventComment())) {
							log.error("Error in export handler during update.");
							return false;
						}
						newesetExportedProtectedLogEventRow = protectedLogEventRows[i];
					}
					if (newesetExportedProtectedLogEventRow != null) {
						lastLoopTime = newesetExportedProtectedLogEventRow.getEventTime() + 1;
					}
				} while(protectedLogEventRows.length > 0);
				// Calculate hash
				byte[] exportedHash = messageDigest.digest();
				byte[] lastExportedHash = null;
				if (protectedLogExportRow != null) {
					lastExportedHash = protectedLogExportRow.getLogDataHash();
				}
				if (newesetExportedProtectedLogEventRow != null) {
					// Final processing
					long timeOfExport = new Date().getTime();
					// Get token-cert
					X509Certificate certificate = getToken(newesetExportedProtectedLogEventRow.getProtectionKeyIdentifier()).getTokenCertificate();
					// if not CA-cert get the issuers cert
					int caId = certificate.getSubjectDN().getName().hashCode();
					//int caId = CertTools.getSubjectDN(certificate).hashCode();
					CAInfo caInfo = getCAAdminSession().getCAInfo(new Admin(Admin.TYPE_INTERNALUSER), caId);
					if (caInfo == null) {
						caId = certificate.getIssuerDN().getName().hashCode();
						//int caId = CertTools.getIssuerDN(certificate).hashCode();
						caInfo = getCAAdminSession().getCAInfo(new Admin(Admin.TYPE_INTERNALUSER), caId);
						if (caInfo == null) {
							log.error("No valid CA certificate to use for log-export.");
							protectedLogExportHandler.abort();
							return false;
						} else {
							certificate = (X509Certificate) caInfo.getCertificateChain().iterator().next();
						}
					}
					ProtectedLogExportRow newProtectedLogExportRow = new ProtectedLogExportRow(timeOfExport, exportEndTime, exportStartTime, exportedHash,
							lastExportedHash, currentHashAlgorithm, certificate, deleteAfterExport, null);
					byte[] signature = getSignSession().signData(newProtectedLogExportRow.getAsByteArray(false), caId, SecConst.CAKEYPURPOSE_CERTSIGN);
					newProtectedLogExportRow.setSignature(signature);
					// Send to interface
					if (!protectedLogExportHandler.done(currentHashAlgorithm, exportedHash, lastExportedHash)) {
						// Something went wrong here
						log.error("Error in export handler during finalization.");
						return false;
					}
					// Write to database
					reservedProtectedLogExportRow = newProtectedLogExportRow;
					success = true;
				} else {
					log.info("No events to export..?");
				}
			} catch (CATokenOfflineException e) {
				log.error("Cannot export since CA token is offline.");
				return false;
			} catch (Exception e) {
				log.error("", e);
				throw new EJBException(e);
			} finally {
				completeExport(reservedProtectedLogExportRow, success);
				if (success && deleteAfterExport) {
					// Nuke it
					Integer[] tokenIdentifiers = findTokenIndentifiersUsedOnlyUntil(exportEndTime);
					removeAllUntil(exportEndTime);
					removeTokens(tokenIdentifiers);
				}
			}
			return success;
		} finally {
			// Enable services again
			serviceConfiguration = getServiceSession().getService(internalAdmin, ProtectedLogExportWorker.DEFAULT_SERVICE_NAME);
			if (serviceConfiguration != null) {
				serviceConfiguration.setActive(true);
				getServiceSession().changeService(internalAdmin, ProtectedLogExportWorker.DEFAULT_SERVICE_NAME, serviceConfiguration);
			}
			serviceConfiguration = getServiceSession().getService(internalAdmin, ProtectedLogVerificationWorker.DEFAULT_SERVICE_NAME);
			if (serviceConfiguration != null) {
				serviceConfiguration.setActive(true);
				getServiceSession().changeService(internalAdmin, ProtectedLogExportWorker.DEFAULT_SERVICE_NAME, serviceConfiguration);
			}
			
		}
	}
	
    /**
     * Exports the log the the given export handler and stores a signed hash linking each export to the last one.
     * @ejb.interface-method view-type="both"
     * @ejb.transaction type="NotSupported"
     */
	public boolean exportLog(IProtectedLogExportHandler protectedLogExportHandler, Properties exportHandlerProperties,
			ProtectedLogActions protectedLogActions, String currentHashAlgorithm, boolean deleteAfterExport, long atLeastThisOld) {
		ProtectedLogExportRow reservedProtectedLogExportRow = reserveExport(atLeastThisOld);
		if (reservedProtectedLogExportRow == null) {
			return false;
		}
		log.info("Starting export proceedure.");
		long exportEndTime = reservedProtectedLogExportRow.getExportEndTime();
		long exportStartTime = reservedProtectedLogExportRow.getExportStartTime();
		boolean success = false;
		try {
			protectedLogExportHandler.init(exportHandlerProperties, exportEndTime, exportStartTime, false); 
			ProtectedLogExportRow protectedLogExportRow = getLastSignedExport();
			// Process all LogEventRows in the timespan chronologically, oldest first
			// By sorting for newest first caching would be easier, but that would result in exported files with newest event first
			int fetchSize = 1000;
			long rowCount = 0;
			ProtectedLogEventRow[] protectedLogEventRows = new ProtectedLogEventRow[0];
			MessageDigest messageDigest = MessageDigest.getInstance(currentHashAlgorithm, "BC");
			ProtectedLogEventRow newesetExportedProtectedLogEventRow = null;
			long lastLoopTime = exportStartTime;
			do {
				protectedLogEventRows = findNextProtectedLogEventRows(lastLoopTime, exportEndTime, fetchSize);
				rowCount += protectedLogEventRows.length;
				log.info("exportLog is processing another "+protectedLogEventRows.length+" rows. ("+rowCount+" rows in total so far.)");
				for (int i=0; i<protectedLogEventRows.length; i++) {
					// Verify current by verifying every step to the next protected log event row with valid protection (cache all steps if signature was valid)
					if (!verifyProtectedLogEventRow(protectedLogEventRows[i])) {
						log.error("Export failed. Could not verify node-chain.");
						protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MODIFIED_LOGROW);
						return false;
					}
					// Extract data from LogEventRow and send to interface + digest
					messageDigest.digest(protectedLogEventRows[i].getLogDataAsByteArray());
					if (!protectedLogExportHandler.update(protectedLogEventRows[i].getAdminType(), protectedLogEventRows[i].getAdmindata(), protectedLogEventRows[i].getCaid(),
							protectedLogEventRows[i].getModule(), protectedLogEventRows[i].getEventTime(), protectedLogEventRows[i].getUsername(),
							protectedLogEventRows[i].getCertificateSerialNumber(), protectedLogEventRows[i].getCertificateIssuerDN(), protectedLogEventRows[i].getEventId(),
							protectedLogEventRows[i].getEventComment())) {
						log.error("Error in export handler during update.");
						protectedLogActions.takeActions(IProtectedLogAction.CAUSE_INTERNAL_ERROR);
						return false;
					}
					newesetExportedProtectedLogEventRow = protectedLogEventRows[i];
				}
				if (newesetExportedProtectedLogEventRow != null) {
					lastLoopTime = newesetExportedProtectedLogEventRow.getEventTime() + 1;
				}
			} while(protectedLogEventRows.length >= fetchSize);
			// Calculate hash
			byte[] exportedHash = messageDigest.digest();
			byte[] lastExportedHash = null;
			if (protectedLogExportRow != null) {
				lastExportedHash = protectedLogExportRow.getLogDataHash();
			}
			if (newesetExportedProtectedLogEventRow != null) {
				// Final processing
				long timeOfExport = new Date().getTime();
				// Get token-cert
				X509Certificate certificate = getToken(newesetExportedProtectedLogEventRow.getProtectionKeyIdentifier()).getTokenCertificate();
				// if not CA-cert get the issuers cert
				int caId = certificate.getSubjectDN().getName().hashCode();
				//int caId = CertTools.getSubjectDN(certificate).hashCode();
				CAInfo caInfo = getCAAdminSession().getCAInfo(new Admin(Admin.TYPE_INTERNALUSER), caId);
				if (caInfo == null) {
					caId = certificate.getIssuerDN().getName().hashCode();
					//int caId = CertTools.getIssuerDN(certificate).hashCode();
					caInfo = getCAAdminSession().getCAInfo(new Admin(Admin.TYPE_INTERNALUSER), caId);
					if (caInfo == null) {
						log.error("No valid CA certificate to use for log-export.");
						protectedLogActions.takeActions(IProtectedLogAction.CAUSE_INVALID_TOKEN);
						protectedLogExportHandler.abort();
						return false;
					} else {
						certificate = (X509Certificate) caInfo.getCertificateChain().iterator().next();
					}
				}
				ProtectedLogExportRow newProtectedLogExportRow = new ProtectedLogExportRow(timeOfExport, exportEndTime, exportStartTime, exportedHash,
						lastExportedHash, currentHashAlgorithm, certificate, deleteAfterExport, null);
					byte[] signature = getSignSession().signData(newProtectedLogExportRow.getAsByteArray(false), caId, SecConst.CAKEYPURPOSE_CERTSIGN);
					newProtectedLogExportRow.setSignature(signature);
				// Send to interface
				if (!protectedLogExportHandler.done(currentHashAlgorithm, exportedHash, lastExportedHash)) {
					// Something went wrong here
					log.error("Error in export handler during finalization.");
					protectedLogActions.takeActions(IProtectedLogAction.CAUSE_INTERNAL_ERROR);
					return false;
				}
				// Write to database
				reservedProtectedLogExportRow = newProtectedLogExportRow;
				success = true;
			} else {
				log.info("No events to export..?");
			}
		} catch (CATokenOfflineException e) {
			log.error("Cannot export since CA token is offline.");
			protectedLogActions.takeActions(IProtectedLogAction.CAUSE_INTERNAL_ERROR);
			return false;
		} catch (Exception e) {
			log.error("", e);
			throw new EJBException(e);
		} finally {
			completeExport(reservedProtectedLogExportRow, success);
			if (success && deleteAfterExport) {
				// Nuke it
				Integer[] tokenIdentifiers = findTokenIndentifiersUsedOnlyUntil(exportEndTime);
				removeAllUntil(exportEndTime);
				removeTokens(tokenIdentifiers);
			}
		}
		return true;
	}
}
