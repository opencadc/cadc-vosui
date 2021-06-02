/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2021.                            (c) 2021.
 *  Government of Canada                 Gouvernement du Canada
 *  National Research Council            Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 *  All rights reserved                  Tous droits réservés
 *
 *  NRC disclaims any warranties,        Le CNRC dénie toute garantie
 *  expressed, implied, or               énoncée, implicite ou légale,
 *  statutory, of any kind with          de quelque nature que ce
 *  respect to the software,             soit, concernant le logiciel,
 *  including without limitation         y compris sans restriction
 *  any warranty of merchantability      toute garantie de valeur
 *  or fitness for a particular          marchande ou de pertinence
 *  purpose. NRC shall not be            pour un usage particulier.
 *  liable in any event for any          Le CNRC ne pourra en aucun cas
 *  damages, whether direct or           être tenu responsable de tout
 *  indirect, special or general,        dommage, direct ou indirect,
 *  consequential or incidental,         particulier ou général,
 *  arising from the use of the          accessoire ou fortuit, résultant
 *  software.  Neither the name          de l'utilisation du logiciel. Ni
 *  of the National Research             le nom du Conseil National de
 *  Council of Canada nor the            Recherches du Canada ni les noms
 *  names of its contributors may        de ses  participants ne peuvent
 *  be used to endorse or promote        être utilisés pour approuver ou
 *  products derived from this           promouvoir les produits dérivés
 *  software without specific prior      de ce logiciel sans autorisation
 *  written permission.                  préalable et particulière
 *                                       par écrit.
 *
 *  This file is part of the             Ce fichier fait partie du projet
 *  OpenCADC project.                    OpenCADC.
 *
 *  OpenCADC is free software:           OpenCADC est un logiciel libre ;
 *  you can redistribute it and/or       vous pouvez le redistribuer ou le
 *  modify it under the terms of         modifier suivant les termes de
 *  the GNU Affero General Public        la “GNU Affero General Public
 *  License as published by the          License” telle que publiée
 *  Free Software Foundation,            par la Free Software Foundation
 *  either version 3 of the              : soit la version 3 de cette
 *  License, or (at your option)         licence, soit (à votre gré)
 *  any later version.                   toute version ultérieure.
 *
 *  OpenCADC is distributed in the       OpenCADC est distribué
 *  hope that it will be useful,         dans l’espoir qu’il vous
 *  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
 *  without even the implied             GARANTIE : sans même la garantie
 *  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
 *  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
 *  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
 *  General Public License for           Générale Publique GNU Affero
 *  more details.                        pour plus de détails.
 *
 *  You should have received             Vous devriez avoir reçu une
 *  a copy of the GNU Affero             copie de la Licence Générale
 *  General Public License along         Publique GNU Affero avec
 *  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
 *  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
 *                                       <http://www.gnu.org/licenses/>.
 *
 *
 ************************************************************************
 */
package ca.nrc.cadc.web;

import ca.nrc.cadc.auth.*;
import ca.nrc.cadc.net.NetUtil;
import ca.nrc.cadc.util.StringUtil;
import org.apache.log4j.Logger;
import org.restlet.Request;
import org.restlet.data.Cookie;
import org.restlet.util.Series;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

/**
 * Principal Extractor implementation using a Restlet Request.
 * * Created by hjeeves on 2017-01-11.
 */
public class RestletPrincipalExtractor implements PrincipalExtractor
{
    private static final Logger log =
            Logger.getLogger(RestletPrincipalExtractor.class);

    private final Request request;
    private boolean initialized = false;

    private SSOCookieCredential cookieCredential;

    /**
     * Hidden no-arg constructor for testing.
     */
    RestletPrincipalExtractor()
    {
        this.request = null;
    }

    /**
     * Create this extractor from the given Restlet Request.
     *
     * @param req The Restlet Request.
     */
    public RestletPrincipalExtractor(final Request req)
    {
        this.request = req;
    }

    private void init()
    {
        if (!initialized)
        {
            final Series<Cookie> requestCookies = getRequest().getCookies();
            final Series<Cookie> cookies = new Series<>(Cookie.class);

            if (requestCookies != null)
            {
                cookies.addAll(requestCookies);
            }

            for (final Cookie ssoCookie : cookies)
            {
                if (SSOCookieManager.DEFAULT_SSO_COOKIE_NAME.equals(
                        ssoCookie.getName())
                    && StringUtil.hasText(ssoCookie.getValue()))
                {
                    final SSOCookieManager ssoCookieManager =
                            new SSOCookieManager();

                    try
                    {
                        cookieCredential = new
                                SSOCookieCredential(ssoCookie.getValue(),
                                                    NetUtil.getDomainName(
                                                            getRequest()
                                                                    .getResourceRef()
                                                                    .toUrl()));
                    }
                    catch (IOException | InvalidSignedTokenException e)
                    {
                        log.info("Cannot use SSO Cookie. Reason: "
                                 + e.getMessage());
                    }

                }
            }
        }

        initialized = true;
    }

    @Override
    public X509CertificateChain getCertificateChain()
    {
        return null;
    }

    @Override
    public Set<Principal> getPrincipals()
    {
        init();

        final Set<Principal> principals = new HashSet<>();

        // For now, the UI only needs to deal with the cookie principal.
        addCookiePrincipal(principals);

        return principals;
    }

    private void addCookiePrincipal(final Set<Principal> principals)
    {
        init();

        if (cookieCredential != null)
        {
            principals.add(new CookiePrincipal(SSOCookieManager.DEFAULT_SSO_COOKIE_NAME,
                cookieCredential.getSsoCookieValue()));
        }
    }

    public Request getRequest()
    {
        return request;
    }
}
