/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2016.                            (c) 2016.
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

package ca.nrc.cadc.beacon.web.resources;


import ca.nrc.cadc.accesscontrol.AccessControlClient;
import ca.nrc.cadc.accesscontrol.AccessControlUtil;
import ca.nrc.cadc.beacon.web.restlet.StorageApplication;
import ca.nrc.cadc.net.NetUtil;
import ca.nrc.cadc.util.StringUtil;
import org.restlet.data.Cookie;
import org.restlet.data.CookieSetting;
import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Post;

import java.security.AccessControlException;


public class AccessControlServerResource extends SecureServerResource
{
    @Post
    public void login(final Representation payload) throws Exception
    {
        final Form form = new Form(payload);
        final AccessControlClient accessControlClient =
                (AccessControlClient) getContext().getAttributes().get(
                    StorageApplication.ACCESS_CONTROL_CLIENT_KEY);

        final String username = form.getFirstValue("username");
        final String passwordString = form.getFirstValue("password");

        if (StringUtil.hasText(username) && StringUtil.hasText(passwordString))
        {
            final String cookieValue =
                    accessControlClient.login(username,
                                              passwordString.toCharArray());

            final CookieSetting cookieSetting =
                    new CookieSetting(0, "CADC_SSO",
                                      "\"" + cookieValue + "\"", "/",
                                      NetUtil.getDomainName(getRequest()
                                                                    .getResourceRef()
                                                                    .toUrl()),
                                      null, 60 * 60 * 24 * 2, false,
                                      false);

            getResponse().getCookieSettings().add(cookieSetting);

            final String contextPath = getContextPath();

            getResponse().redirectSeeOther(
                    contextPath
                    + (contextPath.endsWith("/") ? "" : "/") + "list"
                    + form.getFirstValue("redirectPath"));
        }
        else
        {
            throw new AccessControlException("Login denied");
        }
    }

    @Delete
    public void logout() throws Exception
    {
        for (final Cookie cookie : getRequest().getCookies())
        {
            if (cookie.getName().equals(AccessControlUtil.SSO_COOKIE_NAME))
            {
                getResponse().getCookieSettings().add(
                        new CookieSetting(0,
                                          AccessControlUtil.SSO_COOKIE_NAME,
                                          null, "/",
                                          NetUtil.getDomainName(getRequest()
                                                                        .getResourceRef()
                                                                        .toUrl()),
                                          null, 0,
                                          false, false));
            }
        }
    }


}
