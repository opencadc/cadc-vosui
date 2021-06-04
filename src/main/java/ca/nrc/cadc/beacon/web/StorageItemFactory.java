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

package ca.nrc.cadc.beacon.web;

import org.opencadc.gms.GroupURI;
import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.beacon.web.view.*;
import ca.nrc.cadc.date.DateUtil;
import ca.nrc.cadc.net.NetUtil;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.util.StringUtil;
import ca.nrc.cadc.vos.*;

import javax.security.auth.Subject;
import java.net.URI;
import java.security.Principal;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;


public class StorageItemFactory {
    private static final Logger log = Logger.getLogger(StorageItemFactory.class);

    private final URIExtractor uriExtractor;
    private final RegistryClient registryClient;
    private final String contextPath;
    private final URI filesMetaServiceID;
    private final URI filesMetaServiceStandardID;
    private final String vospaceServiceName;


    public StorageItemFactory(final URIExtractor uriExtractor, final RegistryClient registryClient,
                              final String contextPath, final URI filesMetaServiceID,
                              final URI filesMetaServiceStandardID,
                              final String vospaceServiceName) {
        this.uriExtractor = uriExtractor;
        this.registryClient = registryClient;
        this.contextPath = contextPath;
        this.filesMetaServiceID = filesMetaServiceID;
        this.filesMetaServiceStandardID = filesMetaServiceStandardID;
        this.vospaceServiceName = vospaceServiceName;
    }

    private String getTarget(final DataNode dataNode) {
        // This is used for the /files endpoint, for viewing files in the browser
        // (Downloading is a different mechanism)
        final VOSURI dataNodeURI = dataNode.getUri();

        return String.format("%s/%s%s", lookupMetaServiceURLString(dataNodeURI), this.vospaceServiceName,
                             dataNodeURI.getPath());
    }

    private String lookupMetaServiceURLString(final VOSURI nodeURI) {

        try {
            return registryClient.getServiceURL(filesMetaServiceID, filesMetaServiceStandardID,
                                                AuthMethod.COOKIE).toExternalForm();
        } catch (IllegalArgumentException iae) {
            // Revert back to old Service URL if the new meta service is not supported.
            //
            final String query = "?target=" + NetUtil.encode(nodeURI.toString())
                + "&direction=" + Direction.pullFromVoSpaceValue
                + "&protocol=" + NetUtil.encode(VOS.PROTOCOL_HTTP_GET);

            return (registryClient.getServiceURL(nodeURI.getServiceURI(), Standards.VOSPACE_SYNC_21,
                                                 AuthMethod.ANON).toExternalForm() + query);
        }
    }

    private String getTarget(final ContainerNode containerNode) {
        final String target;
        if (StringUtil.hasLength(this.vospaceServiceName)) {
            target = contextPath + (contextPath.endsWith("/") ? "" : "/") + this.vospaceServiceName + "/list"
                + containerNode.getUri().getPath();
        } else {
            target = contextPath + (contextPath.endsWith("/") ? "" : "/") + "list" + containerNode.getUri().getPath();
        }
        return target;
    }

    private String getTarget(final LinkNode linkNode) {
        final String target;
        if (StringUtil.hasLength(this.vospaceServiceName)) {
            target = contextPath + (contextPath.endsWith("/") ? "" : "/") + this.vospaceServiceName + "/link"
                + linkNode.getUri().getPath();
        } else {
            target = contextPath + (contextPath.endsWith("/") ? "" : "/") + "link" + linkNode.getUri().getPath();
        }
        return target;
    }

    /**
     * Parse this node's last modified date.
     *
     * @param node The Node whose date to parse.
     * @return The Date parsed, or null if it cannot be parsed.
     */
    private Date parseDate(final Node node) {
        final String dateProperty = node.getPropertyValue(VOS.PROPERTY_URI_DATE);

        if (dateProperty == null) {
            return null;
        } else {
            try {
                return DateUtil.getDateFormat(DateUtil.IVOA_DATE_FORMAT, DateUtil.UTC).parse(dateProperty);
            } catch (ParseException e) {
                // Date cannot be parsed for some reason.
                return null;
            }
        }
    }


    /**
     * Translate the given node into a view object (StorageItem) instance.
     *
     * @param node The VOSpace Node instance.
     * @return StorageItem instance, never null.
     */
    public StorageItem translate(final Node node) {
        final StorageItem nextItem;
        final VOSURI nodeURI = node.getUri();
        final boolean isRoot = nodeURI.isRoot();
        final Date lastModifiedDate = isRoot ? null : parseDate(node);

        final boolean publicFlag = Boolean.parseBoolean(node.getPropertyValue(VOS.PROPERTY_URI_ISPUBLIC));
        final String lockedFlagValue = node.getPropertyValue(VOS.PROPERTY_URI_ISLOCKED);
        final boolean lockedFlag = StringUtil.hasText(lockedFlagValue) && Boolean.parseBoolean(lockedFlagValue);

        final String writeGroupValues = node.getPropertyValue(VOS.PROPERTY_URI_GROUPWRITE);
        GroupURI[] writeGroupURIs = null;
        try {
            writeGroupURIs = uriExtractor.extract(writeGroupValues);
        } catch (Exception iae) {
            log.warn("Unable to extract group, skipping...: " + writeGroupValues, iae);
        }

        final String readGroupValues = node.getPropertyValue(VOS.PROPERTY_URI_GROUPREAD);
        GroupURI[] readGroupURIs = null;
        try {
            readGroupURIs = uriExtractor.extract(readGroupValues);
        } catch (Exception iae) {
            log.warn("Unable to extract group, skipping...: " + readGroupValues, iae);
        }

        final String readableFlagValue = node.getPropertyValue(VOS.PROPERTY_URI_READABLE);
        final boolean readableFlag = StringUtil.hasLength(readableFlagValue) && Boolean.parseBoolean(readableFlagValue);

        final Subject currentAuthenticatedUser = AuthenticationUtil.getCurrentSubject();
        final Set<Principal> principals = (currentAuthenticatedUser == null)
            ? new HashSet<>() : currentAuthenticatedUser.getPrincipals();
        final boolean writableFlag;
        final VOSURI parentURI = node.getUri().getParentURI();

        if (((parentURI != null) && parentURI.isRoot()) && (!principals.isEmpty())) {
            writableFlag = true;
        } else {
            final String writableFlagValue = node.getPropertyValue(VOS.PROPERTY_URI_WRITABLE);
            writableFlag = StringUtil.hasLength(writableFlagValue) && Boolean.parseBoolean(writableFlagValue);
        }

        final String owner = node.getPropertyValue(VOS.PROPERTY_URI_CREATOR);

        final String totalChildCountValue = node.getPropertyValue("ivo://ivoa.net/vospace/core#childCount");
        final int totalChildCount = StringUtil.hasLength(totalChildCountValue) ? Integer.parseInt(totalChildCountValue)
            : -1;

        if (node instanceof ContainerNode) {
            final ContainerNode containerNode = (ContainerNode) node;
            final String sizeInString = containerNode.getPropertyValue(VOS.PROPERTY_URI_CONTENTLENGTH);
            final long sizeInBytes = sizeInString == null ? -1L : Long.parseLong(sizeInString);
            nextItem = new FolderItem(nodeURI, sizeInBytes, lastModifiedDate, publicFlag, lockedFlag, writeGroupURIs,
                                      readGroupURIs, owner, readableFlag, writableFlag, totalChildCount,
                                      getTarget(containerNode));
        } else if (node instanceof LinkNode) {
            nextItem = new LinkItem(nodeURI, -1L, lastModifiedDate, publicFlag, lockedFlag, writeGroupURIs,
                                    readGroupURIs, owner, readableFlag, writableFlag, getTarget((LinkNode) node));
        } else {
            final long sizeInBytes = Long.parseLong(node.getPropertyValue(VOS.PROPERTY_URI_CONTENTLENGTH));

            nextItem = new FileItem(nodeURI, sizeInBytes, lastModifiedDate, publicFlag, lockedFlag, writeGroupURIs,
                                    readGroupURIs, owner, readableFlag, writableFlag, getTarget((DataNode) node));
        }

        return nextItem;
    }

    public FolderItem getFolderItemView(final ContainerNode containerNode) {
        return (FolderItem) translate(containerNode);
    }
}
