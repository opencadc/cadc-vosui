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

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.beacon.web.*;
import ca.nrc.cadc.beacon.web.restlet.JSONRepresentation;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.util.StringUtil;
import ca.nrc.cadc.vos.*;
import ca.nrc.cadc.vos.client.ClientTransfer;
import ca.nrc.cadc.vos.client.VOSpaceClient;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONException;
import org.json.JSONWriter;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;


public class FileItemServerResource extends StorageItemServerResource
{
    private static final int BUFFER_SIZE = 8192;
    private static final String UPLOAD_FILE_KEY = "upload";

    private final UploadVerifier uploadVerifier;
    private final FileValidator fileValidator;


    FileItemServerResource(final VOSpaceClient voSpaceClient, final UploadVerifier uploadVerifier,
                           final FileValidator fileValidator)
    {
        super(voSpaceClient);
        this.uploadVerifier = uploadVerifier;
        this.fileValidator = fileValidator;
    }

    public FileItemServerResource()
    {
        this.uploadVerifier = new UploadVerifier();
        this.fileValidator = new RegexFileValidator();
    }


    @Post
    @Put
    public void accept(final Representation payload) throws Exception
    {
        if ((payload != null) && MediaType.MULTIPART_FORM_DATA.equals(payload.getMediaType(), true))
        {
            // The Apache FileUpload project parses HTTP requests which
            // conform to RFC 1867, "Form-based File Upload in HTML". That
            // is, if an HTTP request is submitted using the POST method,
            // and with a content type of "multipart/form-data", then
            // FileUpload can parse that request, and get all uploaded files
            // as FileItem.

            // Obtain the file upload Representation as an iterator.
            final ServletFileUpload upload = parseRepresentation();
            final FileItemIterator fileItemIterator = upload.getItemIterator(ServletUtils.getRequest(getRequest()));

            if (!fileItemIterator.hasNext())
            {
                // Some problem occurs, sent back a simple line of text.
                uploadError(Status.CLIENT_ERROR_BAD_REQUEST,
                            "Unable to upload corrupted or incompatible data.");
            }
            else
            {
                upload(fileItemIterator);
            }
        }
        else
        {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            getResponse().setEntity("Nothing to upload or invalid data.", MediaType.TEXT_PLAIN);
        }
    }

    protected void upload(final FileItemIterator fileItemIterator) throws Exception
    {
        boolean inheritParentPermissions = false;
        VOSURI newNodeURI = null;

        try
        {
            while (fileItemIterator.hasNext())
            {
                final FileItemStream nextFileItemStream = fileItemIterator.next();

                if (nextFileItemStream.getFieldName().startsWith(UPLOAD_FILE_KEY))
                {
                    newNodeURI = upload(nextFileItemStream);
                }
                else if (nextFileItemStream.getFieldName().equals("inheritPermissionsCheckBox"))
                {
                    inheritParentPermissions = true;
                }
            }
        }
        catch (FileUploadException e)
        {
            throw new IOException(e);
        }

        if (inheritParentPermissions)
        {
            setInheritedPermissions(newNodeURI);
        }
    }

    /**
     * Perform the actual upload.
     *
     * @param fileItemStream The upload file item stream.
     * @return The URI to the new node.
     * @throws IOException If anything goes wrong.
     */
    VOSURI upload(final FileItemStream fileItemStream) throws Exception
    {
        final String filename = fileItemStream.getName();

        if (fileValidator.validateFileName(filename))
        {
            final String path = getCurrentItemURI().getPath() + "/" + filename;
            final DataNode dataNode = new DataNode(toURI(path));

            // WebRT 19564: Add content type to the response of uploaded items.
            final List<NodeProperty> properties = new ArrayList<>();

            properties.add(new NodeProperty(VOS.PROPERTY_URI_TYPE, fileItemStream.getContentType()));
            properties.add(new NodeProperty(VOS.PROPERTY_URI_CONTENTLENGTH,
                                            Long.toString(getRequest().getEntity().getSize())));

            dataNode.setProperties(properties);

            try (final InputStream inputStream = fileItemStream.openStream())
            {
                upload(inputStream, dataNode);
            }

            return dataNode.getUri();
        }
        else
        {
            throw new ResourceException(new IllegalArgumentException(
                    String.format("Invalid file name: %s -- File name must match %s.", filename,
                                  fileValidator.getRule())));
        }
    }

    /**
     * Do the secure upload.
     *
     * @param inputStream The InputStream to pull from.
     * @param dataNode    The DataNode to upload to.
     */
    protected void upload(final InputStream inputStream, final DataNode dataNode) throws Exception
    {
        final UploadOutputStreamWrapper outputStreamWrapper =
                new UploadOutputStreamWrapperImpl(inputStream, BUFFER_SIZE);

        try
        {
            // Due to a bug in VOSpace that returns a 400 while checking
            // for an existing Node, we will work around it by checking manually
            // rather than looking for a NodeNotFoundException as expected, and
            // return the 409 code, while maintaining backward compatibility with the catch below.
            // jenkinsd 2016.07.25
            getNode(dataNode.getUri(), VOS.Detail.min);
        }
        catch (ResourceException e)
        {
            if (e.getCause() instanceof NodeNotFoundException)
            {
                createNode(dataNode);
            }
            else
            {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getCause());
            }
        }

        try
        {
            executeSecurely(new PrivilegedExceptionAction<Object>()
            {
                @Override
                public Object run() throws Exception
                {
                    upload(outputStreamWrapper, dataNode);
                    return null;
                }
            });
        }
        catch (Exception e)
        {
            final String message;

            if ((e.getCause() != null) && StringUtil.hasText(e.getCause().getMessage()))
            {
                message = e.getCause().getMessage();
            }
            else if (StringUtil.hasText(e.getMessage()))
            {
                message = e.getMessage();
            }
            else
            {
                message = "Error during upload.";
            }

            uploadError(Status.SERVER_ERROR_INTERNAL, message);
        }
    }

    /**
     * Abstract away the Transfer stuff.  It's cumbersome.
     *
     * @param outputStreamWrapper The OutputStream wrapper.
     * @param dataNode            The node to upload.
     * @throws Exception To capture transfer and upload failures.
     */
    void upload(final UploadOutputStreamWrapper outputStreamWrapper, final DataNode dataNode) throws Exception
    {
        final RegistryClient registryClient = new RegistryClient();
        final URL baseURL = registryClient
                .getServiceURL(dataNode.getUri().getServiceURI(), Standards.VOSPACE_TRANSFERS_20, AuthMethod.COOKIE);
        final List<Protocol> protocols = new ArrayList<>();
        protocols.add(new Protocol(VOS.PROTOCOL_HTTP_PUT, baseURL.toString(), null));
        final Transfer transfer = new Transfer(dataNode.getUri().getURI(), Direction.pushToVoSpace,
                                               new View(URI.create(VOS.VIEW_DEFAULT)), protocols);

        transfer.version = VOS.VOSPACE_21;

        final ClientTransfer ct = voSpaceClient.createTransfer(transfer);
        ct.setOutputStreamWrapper(outputStreamWrapper);

        ct.runTransfer();

        final Node uploadedNode = getNode(dataNode.getUri(), VOS.Detail.properties);
        uploadVerifier.verifyByteCount(outputStreamWrapper.getByteCount(), uploadedNode);
        uploadVerifier.verifyMD5(outputStreamWrapper.getCalculatedMD5(), uploadedNode);

        uploadSuccess();
    }

    /**
     * Parse the representation into a Map for easier access to Form elements.
     *
     * @return Map of field names to File Items, or empty Map.
     * Never null.
     * @throws Exception If the Upload could not be parsed.
     */
    private ServletFileUpload parseRepresentation() throws Exception
    {
        // 1/ Create a factory for disk-based file items
        final DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(1000240);

        // Create a new file upload handler.
        return createFileUpload(factory);
    }

    /**
     * External method of obtaining a Restlet File Upload.
     *
     * @param factory Factory used to create the upload.
     * @return RestletFileUpload instance.
     */
    private ServletFileUpload createFileUpload(final DiskFileItemFactory factory)
    {
        return new ServletFileUpload(factory);
    }


    private void uploadError(final Status status, final String message)
    {
        writeResponse(status,
                      new JSONRepresentation()
                      {
                          @Override
                          public void write(final JSONWriter jsonWriter)
                                  throws JSONException
                          {
                              jsonWriter.object().key("error").value(message).endObject();
                          }
                      });
    }

    private void uploadSuccess()
    {
        writeResponse(Status.SUCCESS_CREATED,
                      new JSONRepresentation()
                      {
                          @Override
                          public void write(final JSONWriter jsonWriter)
                                  throws JSONException
                          {
                              jsonWriter.object().key("code").value(0).endObject();
                          }
                      });
    }
}
