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


import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.beacon.FileSizeRepresentation;
import ca.nrc.cadc.vos.*;

import ca.nrc.cadc.vos.client.ClientTransfer;
import org.easymock.IAnswer;
import org.json.JSONObject;
import org.restlet.Response;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;

import javax.security.auth.Subject;
import javax.servlet.ServletContext;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.resource.ResourceException;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;


public class FolderItemServerResourceTest
        extends AbstractServerResourceTest<FolderItemServerResource>
{
    @Test
    public void create() throws Exception
    {
        final VOSURI folderURI = new VOSURI(URI.create(
                StorageItemServerResource.VOSPACE_NODE_URI_PREFIX
                + "/my/node"));
        final ContainerNode containerNode = new ContainerNode(folderURI);

        expect(mockServletContext.getContextPath()).andReturn("/teststorage")
                .once();

        replay(mockServletContext);

        testSubject = new FolderItemServerResource(mockVOSpaceClient)
        {
            @Override
            VOSURI getCurrentItemURI()
            {
                return folderURI;
            }

            /**
             * Returns the handled response.
             *
             * @return The handled response.
             */
            @Override
            public Response getResponse()
            {
                return mockResponse;
            }

            @Override
            ServletContext getServletContext()
            {
                return mockServletContext;
            }

            @Override
            RegistryClient getRegistryClient()
            {
                return mockRegistryClient;
            }

            @Override
            <T> T executeSecurely(PrivilegedExceptionAction<T> runnable)
                    throws IOException
            {
                try
                {
                    return runnable.run();
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };

        expect(mockVOSpaceClient.createNode(containerNode, false))
                .andReturn(containerNode).once();

        mockResponse.setStatus(Status.SUCCESS_CREATED);
        expectLastCall().once();

        replay(mockVOSpaceClient, mockResponse);

        testSubject.create();

        verify(mockVOSpaceClient, mockResponse, mockServletContext);
    }

    @Test
    public void retrieveNormalQuota() throws Exception
    {
        long folderSize = 123456789L;
        long quota = 9876543210L;
        String expectedRemainingSize = new FileSizeRepresentation()
                .getSizeHumanReadable(quota - folderSize);
        NodeProperty prop = new NodeProperty(VOS.PROPERTY_URI_CONTENTLENGTH, Long
                .toString(folderSize));
        this.retrieveQuota(quota, expectedRemainingSize, prop);
    }

    @Test
    public void retrieveNotEnoughQuota() throws Exception
    {
        long folderSize = 9876543210L;
        long quota = 123456789L;
        String expectedRemainingSize = new FileSizeRepresentation()
                .getSizeHumanReadable(0);
        NodeProperty prop = new NodeProperty(VOS.PROPERTY_URI_CONTENTLENGTH, Long
                .toString(folderSize));
        this.retrieveQuota(quota, expectedRemainingSize, prop);
    }

    private void retrieveQuota(long quota, final String expectedRemainingSize,
                               final NodeProperty folderSizeNodeProp)
            throws Exception
    {
        String expectedQuota = new FileSizeRepresentation()
                .getSizeHumanReadable(quota);

        final VOSURI folderURI = new VOSURI(URI.create(
                StorageItemServerResource.VOSPACE_NODE_URI_PREFIX
                + "/my/node"));
        List<NodeProperty> properties = new ArrayList<>();
        properties.add(folderSizeNodeProp);
        NodeProperty prop = new NodeProperty(VOS.PROPERTY_URI_QUOTA, Long
                .toString(quota));
        properties.add(prop);

        final ContainerNode containerNode = new ContainerNode(folderURI, properties);

        expect(mockServletContext.getContextPath()).andReturn("/teststorage")
                .once();

        replay(mockServletContext);

        testSubject = new FolderItemServerResource()
        {
            @Override
            VOSURI getCurrentItemURI()
            {
                return folderURI;
            }

            /**
             * Returns the handled response.
             *
             * @return The handled response.
             */
            @Override
            public Response getResponse()
            {
                return mockResponse;
            }

            @Override
            ServletContext getServletContext()
            {
                return mockServletContext;
            }

            @SuppressWarnings("unchecked")
            @Override
            <T extends Node> T getNode(final VOSURI folderURI, final VOS.Detail detail)
                    throws ResourceException
            {
                return (T) containerNode;
            }
        };

        Representation jsonRep = testSubject.retrieveQuota();
        StringWriter swriter = new StringWriter();
        jsonRep.write(swriter);
        String[] kvps = swriter.getBuffer().toString().split(",");
        assertTrue("Should only contain two properties", kvps.length == 2);
        for (String kvp : kvps)
        {
            String[] kv = kvp.split(":");
            String key = extract(kv[0]);
            String value = extract(kv[1]);
            switch (key)
            {
                case "size":
                    Assert.assertEquals("Remainng size is incorrect", expectedRemainingSize, value);
                    break;
                case "quota":
                    Assert.assertEquals("Quota is incorrect", expectedQuota, value);
                    break;
                default:
                    fail("Incorrect property");
                    break;
            }
        }

    }

    private String extract(final String text)
    {
        int beginIndex = text.indexOf('"');
        int endIndex = text.lastIndexOf('"');
        return text.substring(beginIndex + 1, endIndex);
    }


    @Test
    public void moveItemsToFolder() throws Exception
    {
        final String srcNodeName = "/my/source_node";
        final String destNodeName = "/my/dest_node";

        final VOSURI destination = new VOSURI(URI.create(
                StorageItemServerResource.VOSPACE_NODE_URI_PREFIX
                + destNodeName));

        final ContainerNode mockDestinationNode = createMock(ContainerNode.class);

        expect(mockDestinationNode.getUri()).andReturn(destination)
                .once();
        replay(mockDestinationNode);


        final Transfer mockTransfer = createMock(Transfer.class);
        replay(mockTransfer);


        // Need mock ClientTransfer object as well.
        final ClientTransfer mockClientTransfer = createMock(ClientTransfer.class);

        // Override runTransfer & setMonitor methods
        mockClientTransfer.setMonitor(false);
        expectLastCall().andAnswer(new IAnswer<Void>()
        {
            @Override
            public Void answer() throws Throwable
            {
                return null;
            }
        });

        mockClientTransfer.runTransfer();
        expectLastCall().andAnswer(new IAnswer<Void>()
        {
            @Override
            public Void answer() throws Throwable
            {
                return null;
            }
        });


        // Set up return code in response
        mockResponse.setStatus(Status.SUCCESS_OK);
        expectLastCall().once();

        replay(mockResponse, mockClientTransfer);

        expect(mockVOSpaceClient.createTransfer(mockTransfer))
                .andReturn(mockClientTransfer).once();
        replay(mockVOSpaceClient);


        expect(mockServletContext.getContextPath()).andReturn("/teststorage")
                .once();
        replay(mockServletContext);


        testSubject = new FolderItemServerResource(mockVOSpaceClient)
        {
            @Override
            ServletContext getServletContext()
            {
                return mockServletContext;
            }

            @Override
            RegistryClient getRegistryClient()
            {
                return mockRegistryClient;
            }

            @Override
            Subject generateVOSpaceUser() throws IOException
            {
                return new Subject();
            }

            @Override
            public Response getResponse()
            {
                return mockResponse;
            }

            @Override
            VOSURI getCurrentItemURI()
            {
                return destination;
            }

            @SuppressWarnings("unchecked")
            @Override
            <T extends Node> T getNode(VOSURI folderURI, VOS.Detail detail)
                    throws ResourceException
            {
                return (T) mockDestinationNode;
            }

            @Override
            Transfer getTransfer(VOSURI source, VOSURI destination)
            {
                return mockTransfer;
            }
        };


        final JSONObject sourceJSON = new JSONObject("{\"destNode\":\"" + destNodeName + "\","
                                                     + "\"srcNodes\":\"" + srcNodeName + "\"}");

        final JsonRepresentation payload = new JsonRepresentation(sourceJSON);

        testSubject.moveToFolder(payload);

        verify(mockVOSpaceClient, mockResponse, mockServletContext,
               mockClientTransfer, mockDestinationNode, mockTransfer);

    }
}
