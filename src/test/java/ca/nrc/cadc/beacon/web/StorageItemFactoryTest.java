
/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2019.                            (c) 2019.
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

package ca.nrc.cadc.beacon.web;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.beacon.AbstractUnitTest;
import ca.nrc.cadc.beacon.web.restlet.StorageApplication;
import ca.nrc.cadc.beacon.web.view.StorageItem;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.vos.DataNode;
import ca.nrc.cadc.vos.VOS;
import ca.nrc.cadc.vos.VOSURI;

import org.junit.Test;
import org.junit.Assert;
import org.mockito.*;

import java.net.URI;
import java.net.URL;


public class StorageItemFactoryTest extends AbstractUnitTest<StorageItemFactory> {
    @Test
    public void translate() throws Exception {
        final URIExtractor uriExtractor = new URIExtractor();
        final RegistryClient mockRegistryClient = Mockito.mock(RegistryClient.class);
        final DataNode mockDataNode = Mockito.mock(DataNode.class);
        final String contextPath = "/warehouse";
        final VOSURI vosuri = new VOSURI(URI.create("vos://cadc.nrc.ca~vault/myroot/path/file.txt"));
        final String writeGroupURIs = "ivo://cadc.nrc.ca/gms/mygroups?GROUP1";
        final String readGroupURIs = "ivo://cadc.nrc.ca/gms/mygroups?GROUP2 ivo://cadc.nrc.ca/gms/mygroups?GROUP3";
        final URL serviceURL = new URL("https://www.site.com/myservice");

        Mockito.when(mockDataNode.getPropertyValue(VOS.PROPERTY_URI_GROUPWRITE)).thenReturn(writeGroupURIs);
        Mockito.when(mockDataNode.getPropertyValue(VOS.PROPERTY_URI_GROUPREAD)).thenReturn(readGroupURIs);
        Mockito.when(mockDataNode.getPropertyValue(VOS.PROPERTY_URI_CONTENTLENGTH)).thenReturn("88000");
        Mockito.when(mockDataNode.getPropertyValue(VOS.PROPERTY_URI_READABLE)).thenReturn("true");

        Mockito.when(mockDataNode.getUri()).thenReturn(vosuri);

        Mockito.when(mockRegistryClient.getServiceURL(URI.create(StorageApplication.DEFAULT_FILES_META_SERVICE_SERVICE_ID),
                                                      URI.create(StorageApplication.DEFAULT_FILES_META_SERVICE_STANDARD_ID),
                                                      AuthMethod.COOKIE)).thenReturn(serviceURL);

        testSubject = new StorageItemFactory(uriExtractor, mockRegistryClient, contextPath,
                                             URI.create(StorageApplication.DEFAULT_FILES_META_SERVICE_SERVICE_ID),
                                             URI.create(StorageApplication.DEFAULT_FILES_META_SERVICE_STANDARD_ID),
                            "vault");

        final StorageItem storageItemResult = testSubject.translate(mockDataNode);
        Assert.assertEquals("Wrong target URL.",
                            "https://www.site.com/myservice/vault/myroot/path/file.txt",
                            storageItemResult.getTargetURL());

        final String readGroupNames = "GROUP2 GROUP3";
        Assert.assertEquals("Wrong Read groups.", readGroupNames, storageItemResult.getReadGroupNames());

        final String writeGroupNames = "GROUP1";
        Assert.assertEquals("Wrong Write groups.", writeGroupNames, storageItemResult.getWriteGroupNames());

        Assert.assertEquals("Wrong size in bytes.", 88000L, storageItemResult.getSizeInBytes());
        Assert.assertEquals("Wrong size in human readable.", "85.94 KB",
                            storageItemResult.getSizeHumanReadable());
    }

    @Test
    public void translateTargetFallback() throws Exception {
        final URIExtractor uriExtractor = new URIExtractor();
        final RegistryClient mockRegistryClient = Mockito.mock(RegistryClient.class);
        final DataNode mockDataNode = Mockito.mock(DataNode.class);
        final String contextPath = "/warehouse";
        final VOSURI vosuri = new VOSURI(URI.create("vos://cadc.nrc.ca~vault/myroot/path/file.txt"));
        final String writeGroupURIs = "ivo://cadc.nrc.ca/gms/mygroups?GROUP1";
        final String readGroupURIs = "ivo://cadc.nrc.ca/gms/mygroups?GROUP2 ivo://cadc.nrc.ca/gms/mygroups?GROUP3";
        final URL serviceURL = new URL("https://www.oldsite.com/oldservice");

        Mockito.when(mockDataNode.getPropertyValue(VOS.PROPERTY_URI_GROUPWRITE)).thenReturn(writeGroupURIs);
        Mockito.when(mockDataNode.getPropertyValue(VOS.PROPERTY_URI_GROUPREAD)).thenReturn(readGroupURIs);
        Mockito.when(mockDataNode.getPropertyValue(VOS.PROPERTY_URI_CONTENTLENGTH)).thenReturn("88000");
        Mockito.when(mockDataNode.getPropertyValue(VOS.PROPERTY_URI_READABLE)).thenReturn("true");

        Mockito.when(mockDataNode.getUri()).thenReturn(vosuri);

        Mockito.when(mockRegistryClient.getServiceURL(URI.create(StorageApplication.DEFAULT_FILES_META_SERVICE_SERVICE_ID),
                                                      URI.create(StorageApplication.DEFAULT_FILES_META_SERVICE_STANDARD_ID),
                                                      AuthMethod.COOKIE)).thenThrow(new IllegalArgumentException());

        Mockito.when(mockRegistryClient.getServiceURL(vosuri.getServiceURI(), Standards.VOSPACE_SYNC_21,
                                                      AuthMethod.ANON)).thenReturn(serviceURL);

        testSubject = new StorageItemFactory(uriExtractor, mockRegistryClient, contextPath,
                                             URI.create(StorageApplication.DEFAULT_FILES_META_SERVICE_SERVICE_ID),
                                             URI.create(StorageApplication.DEFAULT_FILES_META_SERVICE_STANDARD_ID),
                            "vault");

        final StorageItem storageItemResult = testSubject.translate(mockDataNode);
        Assert.assertEquals("Wrong target URL.",
                            "https://www.oldsite.com/oldservice?target=vos%3A%2F%2Fcadc.nrc" +
                                ".ca%7Evault%2Fmyroot%2Fpath%2Ffile" +
                                ".txt&direction=pullFromVoSpace&protocol=ivo%3A%2F%2Fivoa" +
                                ".net%2Fvospace%2Fcore%23httpget/vault/myroot/path/file.txt",
                            storageItemResult.getTargetURL());

        final String readGroupNames = "GROUP2 GROUP3";
        Assert.assertEquals("Wrong Read groups.", readGroupNames, storageItemResult.getReadGroupNames());

        final String writeGroupNames = "GROUP1";
        Assert.assertEquals("Wrong Write groups.", writeGroupNames, storageItemResult.getWriteGroupNames());

        Assert.assertEquals("Wrong size in bytes.", 88000L, storageItemResult.getSizeInBytes());
        Assert.assertEquals("Wrong size in human readable.", "85.94 KB",
                            storageItemResult.getSizeHumanReadable());
    }

    @Test
    public void translateNoGroups() throws Exception {
        final URIExtractor uriExtractor = new URIExtractor();
        final RegistryClient mockRegistryClient = Mockito.mock(RegistryClient.class);
        final DataNode mockDataNode = Mockito.mock(DataNode.class);
        final String contextPath = "/warehouse";
        final VOSURI vosuri = new VOSURI(URI.create("vos://cadc.nrc.ca~vault/myroot/path/file.txt"));
        final URL serviceURL = new URL("https://www.oldsite.com/oldservice");

        Mockito.when(mockDataNode.getPropertyValue(VOS.PROPERTY_URI_GROUPWRITE)).thenReturn("");
        Mockito.when(mockDataNode.getPropertyValue(VOS.PROPERTY_URI_GROUPREAD)).thenReturn("");
        Mockito.when(mockDataNode.getPropertyValue(VOS.PROPERTY_URI_CONTENTLENGTH)).thenReturn("88000");
        Mockito.when(mockDataNode.getPropertyValue(VOS.PROPERTY_URI_READABLE)).thenReturn("true");

        Mockito.when(mockDataNode.getUri()).thenReturn(vosuri);

        Mockito.when(mockRegistryClient.getServiceURL(URI.create(StorageApplication.DEFAULT_FILES_META_SERVICE_SERVICE_ID),
                                                      URI.create(StorageApplication.DEFAULT_FILES_META_SERVICE_STANDARD_ID),
                                                      AuthMethod.COOKIE)).thenThrow(new IllegalArgumentException());

        Mockito.when(mockRegistryClient.getServiceURL(vosuri.getServiceURI(), Standards.VOSPACE_SYNC_21,
                                                      AuthMethod.ANON)).thenReturn(serviceURL);

        testSubject = new StorageItemFactory(uriExtractor, mockRegistryClient, contextPath,
                                             URI.create(StorageApplication.DEFAULT_FILES_META_SERVICE_SERVICE_ID),
                                             URI.create(StorageApplication.DEFAULT_FILES_META_SERVICE_STANDARD_ID),
                            "vault");

        final StorageItem storageItemResult = testSubject.translate(mockDataNode);
        Assert.assertEquals("Wrong target URL.",
                            "https://www.oldsite.com/oldservice?target=vos%3A%2F%2Fcadc.nrc" +
                                ".ca%7Evault%2Fmyroot%2Fpath%2Ffile" +
                                ".txt&direction=pullFromVoSpace&protocol=ivo%3A%2F%2Fivoa" +
                                ".net%2Fvospace%2Fcore%23httpget/vault/myroot/path/file.txt",
                            storageItemResult.getTargetURL());

        Assert.assertEquals("Wrong Read groups.", "", storageItemResult.getReadGroupNames());
        Assert.assertEquals("Wrong Write groups.", "", storageItemResult.getWriteGroupNames());

        Assert.assertEquals("Wrong size in bytes.", 88000L, storageItemResult.getSizeInBytes());
        Assert.assertEquals("Wrong size in human readable.", "85.94 KB",
                            storageItemResult.getSizeHumanReadable());
    }
}
