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

package org.ejbca.core.ejb.hardtoken;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.ejbca.core.model.SecConst;
import org.cesecore.authentication.tokens.AlwaysAllowLocalAuthenticationToken;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.cesecore.util.CryptoProviderTools;
import org.ejbca.core.model.hardtoken.HardTokenProfileExistsException;
import org.ejbca.core.model.hardtoken.profiles.EnhancedEIDProfile;
import org.ejbca.core.model.hardtoken.profiles.HardTokenProfile;
import org.ejbca.core.model.hardtoken.profiles.SwedishEIDProfile;
import org.ejbca.core.model.hardtoken.profiles.TurkishEIDProfile;
import org.ejbca.util.InterfaceCache;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the hard token profile entity bean.
 * 
 * @version $Id$
 */
public class HardTokenProfileTest {
    private static Logger log = Logger.getLogger(HardTokenProfileTest.class);

    private HardTokenSessionRemote hardTokenSession = InterfaceCache.getHardTokenSession();

    private static int SVGFILESIZE = 512 * 1024; // 1/2 Mega char

    private static final AuthenticationToken admin = new AlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("SYSTEMTEST"));

    @Before
    public void setUp() throws Exception {
        CryptoProviderTools.installBCProvider();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test01AddHardTokenProfile() throws HardTokenProfileExistsException {

        final SwedishEIDProfile swedishProfileOrg = new SwedishEIDProfile();
        final EnhancedEIDProfile enhancedProfileOrg = new EnhancedEIDProfile();
        final TurkishEIDProfile turkishProfileOrg = new TurkishEIDProfile();

        final String svgdata = createSVGData();
        swedishProfileOrg.setPINEnvelopeData(svgdata);
        enhancedProfileOrg.setIsKeyRecoverable(EnhancedEIDProfile.CERTUSAGE_ENC, true);
        swedishProfileOrg.setCertificateProfileId(SwedishEIDProfile.CERTUSAGE_SIGN, SecConst.CERTPROFILE_NO_PROFILE);

        this.hardTokenSession.addHardTokenProfile(admin, "SWETEST", swedishProfileOrg);
        this.hardTokenSession.addHardTokenProfile(admin, "ENHTEST", enhancedProfileOrg);
        this.hardTokenSession.addHardTokenProfile(admin, "TURTEST", turkishProfileOrg);

        final Collection<Integer> authorizedHardTokenIds = this.hardTokenSession.getAuthorizedHardTokenProfileIds(admin);

        final SwedishEIDProfile swedishProfile = (SwedishEIDProfile) this.hardTokenSession.getHardTokenProfile(admin, "SWETEST");
        final EnhancedEIDProfile enhancedProfile = (EnhancedEIDProfile) this.hardTokenSession.getHardTokenProfile(admin, "ENHTEST");
        final TurkishEIDProfile turkishProfile = (TurkishEIDProfile) this.hardTokenSession.getHardTokenProfile(admin, "TURTEST");

        final String svgdata2 = swedishProfile.getPINEnvelopeData();

        assertTrue(  "Profile not authorized", authorizedHardTokenIds.contains( new Integer(this.hardTokenSession.getHardTokenProfileId(admin, "SWETEST")) )  );
        assertTrue("Saving certificate profile failed", swedishProfile.getCertificateProfileId(SwedishEIDProfile.CERTUSAGE_SIGN)==SecConst.CERTPROFILE_NO_PROFILE);
        assertTrue("Saving SVG Data failed", svgdata.equals(svgdata2));
        assertTrue("Saving Hard Token Profile failed", enhancedProfile.getIsKeyRecoverable(EnhancedEIDProfile.CERTUSAGE_ENC));
        assertTrue("Saving Turkish Hard Token Profile failed", turkishProfile!=null);
    }

    @Test
    public void test02RenameHardTokenProfile() throws Exception {
        log.trace(">test02RenameHardTokenProfile()");

        boolean ret = false;
        try {
            hardTokenSession.renameHardTokenProfile(admin, "SWETEST", "SWETEST2");
            ret = true;
        } catch (HardTokenProfileExistsException pee) {
            log.debug("", pee);
        }
        assertTrue("Renaming Hard Token Profile failed", ret);

        log.trace("<test02RenameHardTokenProfile()");
    }

    @Test
    public void test03CloneHardTokenProfile() throws Exception {
        log.trace(">test03CloneHardTokenProfile()");

        boolean ret = false;
        try {
            hardTokenSession.cloneHardTokenProfile(admin, "SWETEST2", "SWETEST");
            ret = true;
        } catch (HardTokenProfileExistsException pee) {
            log.debug("", pee);
        }
        assertTrue("Cloning Hard Token Profile failed", ret);

        log.trace("<test03CloneHardTokenProfile()");
    }

    @Test
    public void test04EditHardTokenProfile() throws Exception {
        log.trace(">test04EditHardTokenProfile()");
        boolean ret = false;
        HardTokenProfile profile = hardTokenSession.getHardTokenProfile(admin, "ENHTEST");
        profile.setHardTokenSNPrefix("11111");
        hardTokenSession.changeHardTokenProfile(admin, "ENHTEST", profile);
        ret = true;
        assertTrue("Editing HardTokenProfile failed", ret);
        log.trace("<test04EditHardTokenProfile()");
    }

    @Test
    public void test05removeHardTokenProfiles() throws Exception {
        log.trace(">test05removeHardTokenProfiles()");
        boolean ret = false;
        try {
            // Remove all except ENHTEST
            hardTokenSession.removeHardTokenProfile(admin, "SWETEST");
            hardTokenSession.removeHardTokenProfile(admin, "SWETEST2");
            hardTokenSession.removeHardTokenProfile(admin, "ENHTEST");
            hardTokenSession.removeHardTokenProfile(admin, "TURTEST");
            ret = true;
        } catch (Exception pee) {
            log.debug("", pee);
        }
        assertTrue("Removing Hard Token Profile failed", ret);
        log.trace("<test05removeHardTokenProfiles()");
    }

    private String createSVGData() {
        char[] chararray = new char[SVGFILESIZE];
        Arrays.fill(chararray, 'a');

        return new String(chararray);
    }

}
