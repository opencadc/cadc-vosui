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


import ca.nrc.cadc.beacon.FileSizeRepresentation;
import ca.nrc.cadc.beacon.web.restlet.JSONRepresentation;
import ca.nrc.cadc.util.Log4jInit;
import ca.nrc.cadc.vos.*;
import ca.nrc.cadc.vos.VOS.Detail;
import ca.nrc.cadc.vos.client.ClientTransfer;
import ca.nrc.cadc.vos.client.VOSClientUtil;
import ca.nrc.cadc.vos.client.VOSpaceClient;

import java.io.IOException;
import java.net.URI;
import java.security.*;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

import javax.security.auth.Subject;


public class FolderItemServerResource extends StorageItemServerResource {
    private static Logger log = Logger.getLogger(FolderItemServerResource.class);
    static {
        Log4jInit.setLevel("ca.nrc.cadc", Level.DEBUG);
    }
    /**
     * Empty constructor needed for Restlet to manage it.
     */
    public FolderItemServerResource() {
    }

    FolderItemServerResource(final VOSpaceClient voSpaceClient) {
        super(voSpaceClient);
    }


    @Put
    public void create() throws Exception {
        createFolder();
        getResponse().setStatus(Status.SUCCESS_CREATED);
    }

    @Get("json")
    public Representation retrieveQuota() throws Exception {
        final FileSizeRepresentation fileSizeRepresentation = new FileSizeRepresentation();
        final Node node = getCurrentNode(Detail.properties);
        final long folderSize = getPropertyValue(node, VOS.PROPERTY_URI_CONTENTLENGTH);
        final long quota = getPropertyValue(node, VOS.PROPERTY_URI_QUOTA);
        final String quotaString = new FileSizeRepresentation()
            .getSizeHumanReadable(quota);
        final String remainingSizeString =
            fileSizeRepresentation.getSizeHumanReadable(
                ((quota - folderSize) > 0) ? (quota - folderSize) : 0);

        return new JSONRepresentation() {
            @Override
            public void write(final JSONWriter jsonWriter)
                throws JSONException {
                jsonWriter.object()
                          .key("size").value(remainingSizeString)
                          .key("quota").value(quotaString)
                          .endObject();
            }
        };
    }

    private long getPropertyValue(final Node node, final String propertyURI) {
        final NodeProperty property = node.findProperty(propertyURI);
        return (property == null) ? 0L
            : Long.parseLong(property.getPropertyValue());
    }

    @Post("json")
    public void moveToFolder(final JsonRepresentation payload) throws Exception {
        final JSONObject jsonObject = payload.getJsonObject();
        log.debug("moveToFolder input: " + jsonObject);

        final ContainerNode currentNode = getCurrentNode(VOS.Detail.min);
        final Set<String> keySet = jsonObject.keySet();

        if (keySet.contains("srcNodes")) {
            final String srcNodeStr = (String) jsonObject.get("srcNodes");
            final String[] srcNodes = srcNodeStr.split(",");

            // iterate over each srcNode & call clientTransfer
            for (final String srcNode : srcNodes) {
                final VOSURI srcURI = new VOSURI(URI.create(getVospaceNodeUriPrefix() + srcNode));
                final VOSURI destURI = currentNode.getUri();
                log.debug("moving " + srcURI.toString() + " to " + destURI.toString());
                move(srcURI, destURI);
            }
            // move() will throw an exception if there is a problem
            getResponse().setStatus(Status.SUCCESS_OK);
        }
    }

    Transfer getTransfer(VOSURI source, VOSURI destination) {
        return new Transfer(source.getURI(), destination.getURI(), false);
    }

    private void move(final VOSURI source, final VOSURI destination) throws Exception
    {
        // According to ivoa.net VOSpace 2.1 spec, a move is handled using
        // a transfer. keepBytes = false. destination URI is the Direction.
        final Transfer transfer = getTransfer(source, destination);

        try {
            Subject.doAs(generateVOSpaceUser(),
                         (PrivilegedExceptionAction<Void>) () -> {
                             final ClientTransfer clientTransfer =
                                 voSpaceClient.createTransfer(
                                     transfer);
                             clientTransfer.setMonitor(true);
                             clientTransfer.runTransfer();
                            log.debug("transfer run complete");
                             // TODO: need to put something in to wait for the
                             // transfer job to finish
                             VOSClientUtil.checkTransferFailure(clientTransfer);
                            log.debug("no errors in transfer");
                             return null;
                         });
        } catch (PrivilegedActionException e) {
            log.debug("error in transfer.", e);
            throw e.getException();
        }
    }
}
