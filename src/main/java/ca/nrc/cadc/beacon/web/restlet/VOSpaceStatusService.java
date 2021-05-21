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

package ca.nrc.cadc.beacon.web.restlet;


import ca.nrc.cadc.auth.NotAuthenticatedException;
import ca.nrc.cadc.beacon.web.view.FreeMarkerConfiguration;
import ca.nrc.cadc.net.ResourceAlreadyExistsException;
import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.util.StringUtil;
import ca.nrc.cadc.vos.*;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ResourceException;
import org.restlet.service.StatusService;

import java.io.FileNotFoundException;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.Map;


/**
 * Translate Exceptions into HTTP Statuses.
 */
public class VOSpaceStatusService extends StatusService {

    @Override
    public Representation toRepresentation(final Status status, final Request request, final Response response) {
        if (status.getCode() == Status.CLIENT_ERROR_BAD_REQUEST.getCode()) {
            response.setStatus(status);
            return new StringRepresentation(status.getReasonPhrase(), MediaType.TEXT_PLAIN);
        } else {
            final Map<String, Object> dataModel = new HashMap<>();
            final Context curContext = getContext();

            final String pathInRequest = (String) request.getAttributes().get("path");
            final String requestedResource = "/" + ((pathInRequest == null) ? "" : pathInRequest);

            dataModel.put("errorMessage", status.toString());

            // requestedFolder in login.ftl will allow login to this node
            // in case this is a permissions issue
            dataModel.put("requestedFolder", requestedResource);

            // Add the current VOSpace service name so that navigation links can be rendered correctly
            StorageApplication sa = (StorageApplication) StorageApplication.getCurrent();
            String vospaceSvcName = sa.getVospaceServiceConfigMgr().currentServiceName;
            if (StringUtil.hasLength(vospaceSvcName)) {
                dataModel.put("vospaceSvcPath", vospaceSvcName + "/");
            } else {
                dataModel.put("vospaceSvcPath", "/");
            }

            return new TemplateRepresentation("error.ftl",
                                              (FreeMarkerConfiguration) curContext.getAttributes()
                                                                                  .get(StorageApplication.FREEMARKER_CONFIG_KEY),
                                              dataModel, MediaType.TEXT_HTML);
        }
    }


    /**
     * Returns a status for a given exception or error.
     *
     * @param throwable The exception or error caught.
     * @param request   The request handled.
     * @param response  The response updated.
     * @return The representation of the given status.
     */
    @Override
    public Status toStatus(final Throwable throwable, final Request request, final Response response) {
        final Status status;

        if ((throwable instanceof IllegalStateException) && throwable.getCause() != null) {
            status = toStatus(throwable.getCause(), request, response);
        } else if (throwable instanceof ResourceException) {
            final Throwable cause = throwable.getCause();
            status = (cause == null)
                     ? super.toStatus(throwable, request, response) : toStatus(cause, request, response);
        } else if (throwable instanceof IllegalArgumentException) {
            // Prune out any CR or LF that might come from web services, or they'll
            // get converted to a helpful message about how they need to be removed
            // further up the chain in the restlet code.
            String thrownMessage = throwable.getMessage().replace("\n", "");
            status = new Status(Status.CLIENT_ERROR_BAD_REQUEST.getCode(), thrownMessage, thrownMessage);
        } else if ((throwable instanceof FileNotFoundException) || (throwable instanceof NodeNotFoundException)
                   || (throwable instanceof ResourceNotFoundException)) {
            status = Status.CLIENT_ERROR_NOT_FOUND;
        } else if (throwable instanceof AccessControlException) {
            status = Status.CLIENT_ERROR_FORBIDDEN;
        } else if (throwable instanceof NotAuthenticatedException) {
            status = Status.CLIENT_ERROR_UNAUTHORIZED;
        } else if (throwable instanceof ResourceAlreadyExistsException
                || throwable instanceof NodeAlreadyExistsException) {
            status = Status.CLIENT_ERROR_CONFLICT;
        } else if (StringUtil.hasText(throwable.getMessage())) {
            final String message = throwable.getMessage();

            status = (message.contains("(409)"))
                     ? Status.CLIENT_ERROR_CONFLICT : super.toStatus(throwable, request, response);
        } else {
            status = super.toStatus(throwable, request, response);
        }

        return status;
    }

}
