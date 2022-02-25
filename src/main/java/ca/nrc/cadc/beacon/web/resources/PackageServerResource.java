/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2020.                            (c) 2020.
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



import ca.nrc.cadc.beacon.web.restlet.JSONRepresentation;

import ca.nrc.cadc.beacon.web.restlet.PackageRepresentation;
import ca.nrc.cadc.io.ByteLimitExceededException;
import ca.nrc.cadc.io.MultiBufferIO;
import ca.nrc.cadc.io.ReadException;
import ca.nrc.cadc.io.WriteException;
import ca.nrc.cadc.net.HttpGet;
import ca.nrc.cadc.net.ResourceAlreadyExistsException;
import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.net.TransientException;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.vos.*;
import ca.nrc.cadc.vos.VOS.Detail;
import ca.nrc.cadc.vos.client.ClientTransfer;
import ca.nrc.cadc.vos.client.VOSClientUtil;
import ca.nrc.cadc.vos.client.VOSpaceClient;



import ca.nrc.cadc.uws.Job;
import ca.nrc.cadc.uws.JobReader;
import ca.nrc.cadc.uws.Parameter;
import ca.nrc.cadc.vos.Direction;
import ca.nrc.cadc.vos.Protocol;
import ca.nrc.cadc.vos.Transfer;
import ca.nrc.cadc.vos.TransferParsingException;
import ca.nrc.cadc.vos.TransferReader;
import ca.nrc.cadc.vos.VOS;
import ca.nrc.cadc.vos.VOSURI;
import ca.nrc.cadc.vos.View;

import freemarker.template.utility.StringUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;


import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.security.auth.Subject;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;


public class PackageServerResource extends StorageItemServerResource {
    private static Logger log = Logger.getLogger(PackageServerResource.class);

    /**
     * Empty constructor needed for Restlet to manage it.
     */
    public PackageServerResource() {
    }

    PackageServerResource(final VOSpaceClient voSpaceClient) {
        super(voSpaceClient);
    }

    @Get("json")
    public Representation notSupported() throws Exception {
        return new JSONRepresentation() {
            @Override
            public void write(final JSONWriter jsonWriter)
                throws JSONException {
                jsonWriter.object()
                    .key("msg").value("GET not supported.")
                    .endObject();
            }
        };
    }

    @Post("json")
    public Representation getPackage(final JsonRepresentation payload) throws Exception {
        final JSONObject jsonObject = payload.getJsonObject();
        log.debug("getPackage input: " + jsonObject);

        List<URI> targetList = new ArrayList<>();

        final Set<String> keySet = jsonObject.keySet();

        String responseFormat;
        if (keySet.contains("responseformat")) {
            responseFormat = jsonObject.getString("responseformat");
        } else {
            // default response format
            responseFormat = "application/zip";
        }

        if (!keySet.contains("target")) {
            return new JSONRepresentation() {
                @Override
                public void write(final JSONWriter jsonWriter)
                    throws JSONException {
                    jsonWriter.object()
                        .key("msg").value("no targets found.")
                        .endObject();
                }
            };
        } else {
            final String targetStr = (String) jsonObject.get("target");
            final String[] targets = targetStr.split(",");

            // iterate over each srcNode & call clientTransfer
            for (final String target : targets) {
//                URI targetURI = new URI(getVospaceNodeUriPrefix() + target);
                // URIs are being passed in from the UI
                URI targetURI = new URI(target);
                log.debug("adding URI to transfer target list: " + targetURI.toString());
                targetList.add(targetURI);
            }

            // Get all targets from the json sent in
            // make a list of URIs
            // get the mime type (response format) passed in
            // make the client transfer object
            // use VOSpaceClient to send the request
            // (use code similar to vault integration tests here)

            // Create the Transfer.
            Transfer transfer = new Transfer(Direction.pullFromVoSpace);
            transfer.getTargets().addAll(targetList);

            List<Protocol> protocols = new ArrayList<Protocol>();
            protocols.add(new Protocol(VOS.PROTOCOL_HTTP_GET));
            protocols.add(new Protocol(VOS.PROTOCOL_HTTPS_GET));
            transfer.getProtocols().addAll(protocols);

            // Add package view for tar file
            View packageView = new View(new URI(Standards.PKG_10.toString()));
            packageView.getParameters().add(new View.Parameter(new URI(VOS.PROPERTY_URI_FORMAT), responseFormat));
            transfer.setView(packageView);

            transfer.version = VOS.VOSPACE_21;

            final ClientTransfer ct = voSpaceClient.createTransfer(transfer);
            URL packageURL = new URL(ct.getTransfer().getProtocols().get(0).getEndpoint());


            String endpoint = packageURL.toString();
            if (endpoint != "") {
                // Need a PackageRepresentation class
//                getResponse().setEntity(new PackageRepresentation(MediaType.APPLICATION_ZIP, packageURL));
                getResponse().setStatus(Status.SUCCESS_OK);
//                return new PackageRepresentation(MediaType.APPLICATION_ZIP, packageURL);
                return new JSONRepresentation() {
                    @Override
                    public void write(final JSONWriter jsonWriter)
                        throws JSONException {
                        jsonWriter.object()
                            .key("endpoint").value(endpoint)
                            .endObject();
                    }
                };

            } else {
                return new JSONRepresentation() {
                    @Override
                    public void write(final JSONWriter jsonWriter)
                        throws JSONException {
                        jsonWriter.object()
                            .key("msg").value("no package generated.")
                            .endObject();
                    }
                };
            }
        }
//        return null;

    }


}
