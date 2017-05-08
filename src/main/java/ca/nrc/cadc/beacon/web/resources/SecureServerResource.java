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

import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.beacon.web.restlet.VOSpaceApplication;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.web.RestletPrincipalExtractor;
import ca.nrc.cadc.web.SubjectGenerator;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import javax.security.auth.Subject;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.Map;


class SecureServerResource extends ServerResource
{
    private final SubjectGenerator subjectGenerator;

    @Override
    protected void doInit() throws ResourceException
    {

    }

    @SuppressWarnings("unchecked")
    <T> T getRequestAttribute(final String attributeName)
    {
        return (T) getRequestAttributes().get(attributeName);
    }

    @SuppressWarnings("unchecked")
    <T> T getContextAttribute(final String attributeName)
    {
        return (T) getContext().getAttributes().get(attributeName);
    }

    SecureServerResource()
    {
        this(new SubjectGenerator());
    }

    SecureServerResource(final SubjectGenerator subjectGenerator)
    {
        this.subjectGenerator = subjectGenerator;
    }


    Subject getCurrentUser()
    {
        final Request request = getRequest();
        return AuthenticationUtil.getSubject(new RestletPrincipalExtractor(request));
    }

    RegistryClient getRegistryClient()
    {
        return (RegistryClient) getContext().getAttributes().get(VOSpaceApplication.REGISTRY_CLIENT_KEY);
    }

    Subject generateVOSpaceUser() throws IOException
    {
        return subjectGenerator.generate(new RestletPrincipalExtractor(getRequest()));
    }

    ServletContext getServletContext()
    {
        final Map<String, Object> attributes = getApplication().getContext().getAttributes();
        return (ServletContext) attributes.get(VOSpaceApplication.SERVLET_CONTEXT_ATTRIBUTE_KEY);
    }

    /**
     * Set a default context path when this is not running in a servlet
     * container.
     *
     * @return      String path.
     */
    String getContextPath()
    {
        return (getServletContext() == null)
               ? VOSpaceApplication.DEFAULT_CONTEXT_PATH : getServletContext().getContextPath();
    }

    protected String getPath()
    {
        return getRequest().getResourceRef().getPath();
    }

    /**
     * Write out the given status and representation body to the response.
     * @param status                The Status to set.
     * @param representation        The representation used for the body of the response.
     */
    void writeResponse(final Status status, final Representation representation)
    {
        final Response response = getResponse();

        response.setStatus(status);
        response.setEntity(representation);
    }
}
