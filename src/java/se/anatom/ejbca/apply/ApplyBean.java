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
 
/*
 * ApplyBean.java
 *
 * Created on den 3 nov 2002, 12:06
 */
package se.anatom.ejbca.apply;

import se.anatom.ejbca.SecConst;
import se.anatom.ejbca.ca.store.ICertificateStoreSessionHome;
import se.anatom.ejbca.ca.store.ICertificateStoreSessionRemote;
import se.anatom.ejbca.common.UserDataVO;
import se.anatom.ejbca.log.Admin;
import se.anatom.ejbca.ra.IUserAdminSessionHome;
import se.anatom.ejbca.ra.IUserAdminSessionRemote;

import java.security.cert.X509Certificate;

import javax.ejb.FinderException;

import javax.naming.*;

import javax.servlet.http.HttpServletRequest;


/**
 * A class used as an interface between Apply jsp pages and ejbca functions.
 *
 * @author Philip Vendil
 * @version $Id: ApplyBean.java,v 1.9 2005-04-21 15:14:41 herrvendil Exp $
 */
public class ApplyBean {
    /**
     * Creates a new instance of CaInterfaceBean
     */
    public ApplyBean() {
    }

    // Public methods
    public void initialize(HttpServletRequest request)
        throws Exception {
        if (!initialized) {
            if (request.getAttribute("javax.servlet.request.X509Certificate") != null) {
                administrator = new Admin(((X509Certificate[]) request.getAttribute(
                            "javax.servlet.request.X509Certificate"))[0]);
            } else {
                administrator = new Admin(Admin.TYPE_PUBLIC_WEB_USER, request.getRemoteAddr());
            }

            InitialContext jndicontext = new InitialContext();
            Object obj1 = jndicontext.lookup("UserAdminSession");
            useradminhome = (IUserAdminSessionHome) javax.rmi.PortableRemoteObject.narrow(obj1,
                    IUserAdminSessionHome.class);
            obj1 = jndicontext.lookup("CertificateStoreSession");
            certificatesessionhome = (ICertificateStoreSessionHome) javax.rmi.PortableRemoteObject.narrow(obj1,
                    ICertificateStoreSessionHome.class);
            initialized = true;
        }
    }

    /**
     * Method that returns a users tokentype defined in SecConst, if 0 is returned user couldn't be
     * found i database.
     *
     * @param username the user whose tokentype should be returned
     *
     * @return tokentype as defined in SecConst
     *
     * @see se.anatom.ejbca.SecConst
     */
    public int getTokenType(String username) throws Exception {
        int returnval = 0;
        IUserAdminSessionRemote useradminsession = useradminhome.create();

		if(!username.equals(this.username) || this.useradmindata == null){        
		  try {
			this.useradmindata = useradminsession.findUser(administrator, username);
		  } catch (FinderException fe) {
		  }
		}
		
        if (useradmindata != null) {
            returnval = useradmindata.getTokenType();
        }
		this.username = username;
		
        return returnval;
    }

	/**
	 * Method that returns a users tokentype defined in SecConst, if 0 is returned user couldn't be
	 * found i database.
	 *
	 * @param username the user whose tokentype should be returned
	 *
	 * @return caid of user.
	 *
	 * @see se.anatom.ejbca.SecConst
	 */
	public int getCAId(String username) throws Exception {
		int returnval = 0;		
		IUserAdminSessionRemote useradminsession = useradminhome.create();

		if(!username.equals(this.username) || this.useradmindata == null){        
		  try {
			this.useradmindata = useradminsession.findUser(administrator, username);
		  } catch (FinderException fe) {
		  }
		}
		
		if (useradmindata != null) {
			returnval = useradmindata.getCAId();
		}
		this.username = username;
		
		return returnval;
	}


    /**
     * Method that returns a bitlengths available for the user. Returns null if user couldn't be
     * found in database.
     *
     * @param username user whose bit lengts are requested.
     *
     * @return array of available bit lengths
     */
    public int[] availableBitLengths(String username) throws Exception {
        int[] returnval = null;        
        IUserAdminSessionRemote useradminsession = useradminhome.create();

        if(!username.equals(this.username) || this.useradmindata == null){        
          try {
            this.useradmindata = useradminsession.findUser(administrator, username);
          } catch (FinderException fe) {
          }
        }  

        if (useradmindata != null) {
            ICertificateStoreSessionRemote certstoresession = certificatesessionhome.create();
            int certprofile = useradmindata.getCertificateProfileId();

            if (certprofile != SecConst.PROFILE_NO_PROFILE) {
                returnval = certstoresession.getCertificateProfile(administrator, certprofile)
                                            .getAvailableBitLengths();
            }
        }
        this.username = username;

        return returnval;
    }

    // Private methods
    // Private fields
    private IUserAdminSessionHome useradminhome;
    private ICertificateStoreSessionHome certificatesessionhome;
    private boolean initialized;
    private Admin administrator;
    private String username = "";
    private UserDataVO useradmindata = null;
}
