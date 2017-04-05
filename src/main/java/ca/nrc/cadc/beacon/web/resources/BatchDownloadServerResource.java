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


import ca.nrc.cadc.beacon.web.restlet.DownloadJNLPRepresentation;
import ca.nrc.cadc.beacon.web.restlet.ZIPFileRepresentation;
import ca.nrc.cadc.dlm.DownloadDescriptor;
import ca.nrc.cadc.dlm.ManifestReader;
import ca.nrc.cadc.vos.client.VOSpaceClient;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.WriterRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import javax.security.auth.Subject;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;


public class BatchDownloadServerResource extends StorageItemServerResource
{
    private static final String URI_PARAMETER_KEY = "uri";
    private static final String METHOD_PARAMETER_KEY = "method";
    private static final String HTML_FILE_NAME = "/download_links.html";
    private static final String REPLACE_TOKEN = "%%LIST%%";


    /**
     * Represent the various download methods.
     */
    enum DownloadMethod
    {
        URL_LIST("URL List"), HTML_LIST("HTML List"), ZIP_FILE("ZIP File"),
        DOWNLOAD_MANAGER("Java Webstart");

        final String requestPropertyValue;

        DownloadMethod(String requestPropertyValue)
        {
            this.requestPropertyValue = requestPropertyValue;
        }


        static DownloadMethod valueFromRequest(
                final String requestPropertyValue)
        {
            for (final DownloadMethod downloadMethod : values())
            {
                if (downloadMethod.requestPropertyValue.equals(
                        requestPropertyValue))
                {
                    return downloadMethod;
                }
            }

            throw new IllegalArgumentException("No such Download Method: "
                                               + requestPropertyValue);
        }
    }


    /**
     * Empty constructor needed for Restlet to manage it.  Needs to be public.
     */
    public BatchDownloadServerResource()
    {
    }

    /**
     * Complete constructor for testing.
     *
     * @param voSpaceClient  The VOSpace Client to use.
     */
    BatchDownloadServerResource(final VOSpaceClient voSpaceClient)
    {
        super(voSpaceClient);
    }


    @Get
    public Representation represent() throws Exception
    {
        final String downloadMethodValue = getQueryValue(METHOD_PARAMETER_KEY);
        final String[] uriValues = getQuery().getValuesArray(URI_PARAMETER_KEY);

        return handleDownload(downloadMethodValue, uriValues);
    }


    @Post
    public void accept(final Representation payload) throws Exception
    {
        final Form form = new Form(payload);
        final String downloadMethodValue =
                form.getFirstValue(METHOD_PARAMETER_KEY);
        final String[] uriValues = form.getValuesArray(URI_PARAMETER_KEY);

        getResponse().setEntity(handleDownload(downloadMethodValue, uriValues));
        getResponse().setStatus(Status.SUCCESS_OK);
    }

    Representation handleDownload(final String downloadMethodValue,
                                  final String[] uriValues)
            throws Exception
    {
        final URI[] uris = toURIArray(uriValues);

        final DownloadMethod downloadMethod =
                DownloadMethod.valueFromRequest(downloadMethodValue);

        final Subject currentUser = getCurrentUser();
        final Representation representation;
        final Writer manifestStringWriter = new StringWriter();
        for (final URI uri : uris)
        {
            getManifest(uri.getPath(), manifestStringWriter);
        }

        final ManifestReader manifestReader = new ManifestReader();


        switch (downloadMethod)
        {
            case ZIP_FILE:
            {
                representation =
                        new ZIPFileRepresentation(currentUser,
                                                  manifestReader.read(
                                                          manifestStringWriter
                                                                  .toString()));

                break;
            }

            case HTML_LIST:
            {
                representation = new WriterRepresentation(
                        MediaType.TEXT_HTML)
                {
                    @Override
                    public void write(final Writer writer) throws IOException
                    {
                        final BufferedReader bufferedReader =
                                loadTemplateHTML();
                        String line;

                        while ((line = bufferedReader.readLine()) != null)
                        {
                            String nextWriteLine;

                            if (line.trim().startsWith(REPLACE_TOKEN))
                            {
                                nextWriteLine = line.replace(REPLACE_TOKEN, "");
                                for (final Iterator<DownloadDescriptor> downloadDescriptorIterator =
                                     manifestReader.read(
                                        manifestStringWriter.toString());
                                     downloadDescriptorIterator.hasNext();)
                                {
                                    final URL downloadURL =
                                            downloadDescriptorIterator.next().url;
                                    final String downloadURLString =
                                            downloadURL.toString().trim();
                                    nextWriteLine +=
                                            "  <p><a href=\"" + downloadURLString
                                            + "\">" + downloadURLString
                                            + "</a></p>\n";
                                }
                            }
                            else
                            {
                                nextWriteLine = line + "\n";
                            }

                            writer.write(nextWriteLine);
                        }
                    }
                };

                break;
            }

            case URL_LIST:
            {
                representation = new WriterRepresentation(
                        MediaType.TEXT_URI_LIST)
                {
                    @Override
                    public void write(final Writer writer) throws IOException
                    {
                        writer.write(manifestStringWriter.toString());
                    }
                };

                break;
            }

            case DOWNLOAD_MANAGER:
            {
                representation =
                        new DownloadJNLPRepresentation(getCodebase("/batch-download"),
                                                       getCurrentSSOCookie(),
                                                       manifestReader.read(
                                                       manifestStringWriter
                                                               .toString()));

                break;
            }

            default:
            {
                throw new UnsupportedOperationException("Unsupported download: "
                                                        + downloadMethod);
            }
        }

        return representation;
    }

    BufferedReader loadTemplateHTML()
    {
        final InputStream inputStream =
                getClass().getResourceAsStream(HTML_FILE_NAME);
        final Reader inputStreamReader = new InputStreamReader(inputStream);
        return new BufferedReader(inputStreamReader);
    }

    private URI[] toURIArray(final String[] uriValues)
    {
        final URI[] uris = new URI[uriValues.length];

        for (int i = 0; i < uriValues.length; i++)
        {
            uris[i] = URI.create(uriValues[i]);
        }

        return uris;
    }
}
