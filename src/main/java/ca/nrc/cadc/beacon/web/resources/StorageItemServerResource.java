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

import ca.nrc.cadc.beacon.web.StorageItemFactory;
import ca.nrc.cadc.beacon.web.URIExtractor;
import ca.nrc.cadc.beacon.web.restlet.VOSpaceApplication;
import ca.nrc.cadc.beacon.web.view.StorageItem;
import ca.nrc.cadc.util.StringUtil;
import ca.nrc.cadc.vos.*;
import ca.nrc.cadc.vos.client.ClientRecursiveSetNode;
import ca.nrc.cadc.vos.client.VOSpaceClient;
import org.json.JSONObject;
import org.restlet.Context;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;


import javax.security.auth.Subject;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Set;


public class StorageItemServerResource extends SecureServerResource
{
    static final String VOSPACE_NODE_URI_PREFIX = "vos://cadc.nrc.ca!vospace";
    static final String IVO_GMS_PROPERTY_PREFIX = "ivo://cadc.nrc.ca/gms#";


    // Page size for the initial page display.
    private static final int DEFAULT_DISPLAY_PAGE_SIZE = 35;
    private static final URIExtractor URI_EXTRACTOR = new URIExtractor();

    StorageItemFactory storageItemFactory;
    VOSpaceClient voSpaceClient;


    /**
     * Empty constructor needed for Restlet to manage it.  Needs to be public.
     */
    public StorageItemServerResource()
    {
    }

    /**
     * Complete constructor for testing.
     *
     * @param voSpaceClient The VOSpace Client to use.
     */
    StorageItemServerResource(final VOSpaceClient voSpaceClient)
    {
        initialize(voSpaceClient);
    }


    /**
     * Set-up method.  This ensures there is a context first before pulling
     * out some necessary objects for further work.
     * <p>
     * Tester
     */
    @Override
    protected void doInit() throws ResourceException
    {
        super.doInit();
        final Context context = getContext();
        initialize(((VOSpaceClient) context.getAttributes().get(
                VOSpaceApplication.VOSPACE_CLIENT_KEY)));
    }

    private void initialize(
            final VOSpaceClient voSpaceClient)
    {
        try
        {
            this.storageItemFactory =
                    new StorageItemFactory(URI_EXTRACTOR, getRegistryClient(),
                                           (getServletContext() == null)
                                           ? VOSpaceApplication.DEFAULT_CONTEXT_PATH
                                           : getServletContext()
                                                   .getContextPath());
        }
        catch (MalformedURLException e)
        {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
        }

        this.voSpaceClient = voSpaceClient;
    }


    String getCurrentPath()
    {
        final String pathInRequest = getRequestAttribute("path");
        return "/" + ((pathInRequest == null) ? "" : pathInRequest);
    }

    VOSURI getCurrentItemURI()
    {
        return toURI(getCurrentPath());
    }

    private <T extends Node> T getCurrentNode() throws NodeNotFoundException, IOException
    {
        return getCurrentNode(VOS.Detail.max);
    }

    final <T extends Node> T getCurrentNode(final VOS.Detail detail) throws NodeNotFoundException, IOException
    {
        return getNode(getCurrentItemURI(), detail);
    }

    final <T extends Node> T getCurrentNode(final VOS.Detail detail, final int limit)
            throws NodeNotFoundException, IOException
    {
        return getNode(getCurrentItemURI(), detail, limit);
    }

    /**
     * @param uri URI to look up.
     * @param <T> Type to translate to.
     * @return Translated Node to StorageItem.
     * @throws IOException For any problems.
     */
    @SuppressWarnings("unchecked")
    final <T extends StorageItem> T getStorageItem(final URI uri)
            throws IOException
    {
        try
        {
            return (T) storageItemFactory.translate(getNode(new VOSURI(uri),
                                                            VOS.Detail.max));
        }
        catch (ResourceException re)
        {
            if (re.getCause() instanceof NodeNotFoundException)
            {
                throw new FileNotFoundException(re.getMessage());
            }
            else
            {
                throw new ResourceException(re);
            }

        }

    }

    <T extends Node> T getNode(final VOSURI folderURI, final VOS.Detail detail, final int limit)
            throws ResourceException
    {
        final String query = "limit=" + limit + ((detail == null)
                ? ""
                : "&detail="
                + detail.name());

        try
        {
            return executeSecurely(new PrivilegedExceptionAction<T>()
            {
                /**
                 * Performs the computation.  This method will be called by
                 * {@code AccessController.doPrivileged} after enabling privileges.
                 *
                 * @return a class-dependent value that may represent the results of the
                 * computation.  Each class that implements
                 * {@code PrivilegedExceptionAction} should document what
                 * (if anything) this value represents.
                 * @throws Exception an exceptional condition has occurred.  Each class
                 *                   that implements {@code PrivilegedExceptionAction} should
                 *                   document the exceptions that its run method can throw.
                 * @see AccessController#doPrivileged(PrivilegedExceptionAction)
                 * @see AccessController#doPrivileged(PrivilegedExceptionAction, AccessControlContext)
                 */
                @Override
                @SuppressWarnings("unchecked")
                public T run() throws Exception
                {
                    return (T) voSpaceClient.getNode(folderURI.getPath(), query);
                }
            });
        }
        catch (Exception e)
        {
            throw new ResourceException(e);
        }
    }

    <T extends Node> T getNode(final VOSURI folderURI, final VOS.Detail detail)
            throws ResourceException
    {
        final int pageSize;

        if (detail == null)
        {
            pageSize = -1;
        }
        else if ((detail == VOS.Detail.max) || (detail == VOS.Detail.raw))
        {
            pageSize = DEFAULT_DISPLAY_PAGE_SIZE;
        }
        else
        {
            pageSize = 0;
        }

        return getNode(folderURI, detail, pageSize);
    }

    VOSURI toURI(final String path)
    {
        try
        {
            return new VOSURI(new URI(VOSPACE_NODE_URI_PREFIX + path));
        }
        catch (URISyntaxException e)
        {
            throw new ResourceException(new IllegalArgumentException("Invalid name: " + path));
        }
    }

    void setInheritedPermissions(final VOSURI newNodeURI) throws Exception
    {
        final ContainerNode parentNode = getCurrentNode();
        final Node newNode = getNode(newNodeURI, null);
        final List<NodeProperty> newNodeProperties = newNode.getProperties();

        // Clean slate.
        newNodeProperties.remove(new NodeProperty(VOS.PROPERTY_URI_GROUPREAD, ""));
        newNodeProperties.remove(new NodeProperty(VOS.PROPERTY_URI_GROUPWRITE, ""));
        newNodeProperties.remove(new NodeProperty(VOS.PROPERTY_URI_ISPUBLIC, ""));

        final String parentReadGroupURIValue = parentNode.getPropertyValue(VOS.PROPERTY_URI_GROUPREAD);
        if (StringUtil.hasText(parentReadGroupURIValue))
        {
            newNodeProperties.add(new NodeProperty(VOS.PROPERTY_URI_GROUPREAD, parentReadGroupURIValue));
        }

        final String parentWriteGroupURIValue = parentNode.getPropertyValue(VOS.PROPERTY_URI_GROUPWRITE);

        if (StringUtil.hasText(parentWriteGroupURIValue))
        {
            newNodeProperties.add(new NodeProperty(VOS.PROPERTY_URI_GROUPWRITE, parentWriteGroupURIValue));
        }

        final String isPublicValue = parentNode.getPropertyValue(VOS.PROPERTY_URI_ISPUBLIC);
        if (StringUtil.hasText(isPublicValue))
        {
            newNodeProperties.add(new NodeProperty(VOS.PROPERTY_URI_ISPUBLIC, isPublicValue));
        }

        setNodeSecure(newNode);
    }

    /**
     * Resolve this link Node's target to its final destination.  This method
     * will follow the target of the provided LinkNode, and continue to do so
     * until an external URL is found, or Node that is not a Link Node.
     * <p>
     * Finally, this method will redirect to the appropriate endpoint.
     *
     * @throws Exception For any parsing errors.
     */
    void resolveLink() throws Exception
    {
        final LinkNode linkNode = getCurrentNode(null);
        final URI resolvedURI = resolveLink(linkNode);
        getResponse().redirectTemporary(resolvedURI.toString());
    }

    /**
     * Resolve the given LinkNode's target URI and return it.
     *
     * @param linkNode The LinkNode to resolve.
     * @return URI of the target.
     * @throws NodeNotFoundException If the target is not found.
     */
    private URI resolveLink(final LinkNode linkNode) throws NodeNotFoundException, IOException
    {
        final URI endPoint;
        final URI targetURI = linkNode.getTarget();

        // Should ALWAYS be true for a LinkNode!
        if (targetURI == null)
        {
            throw new IllegalArgumentException("**BUG**: LinkNode has a null target!");
        }
        else
        {
            try
            {
                final VOSURI vosURI = new VOSURI(targetURI);
                final Node targetNode = getNode(vosURI, null);

                if (targetNode == null)
                {
                    throw new NodeNotFoundException("No target found or broken link for node: " + linkNode.getName());
                }
                else
                {
                    if (targetNode instanceof LinkNode)
                    {
                        endPoint = resolveLink((LinkNode) targetNode);
                    }
                    else
                    {
                        final StorageItem storageItem = storageItemFactory.translate(targetNode);
                        endPoint = URI.create(storageItem.getTargetURL());
                    }
                }
            }
            catch (IllegalArgumentException e)
            {
                // Not a VOSpace URI, so return this URI.
                return targetURI;
            }
        }

        return endPoint;
    }

    /**
     * Perform the HTTPS command to recursively set permissions for a node.
     * Returns when job is complete, OR a maximum of (15) seconds has elapsed.
     * If timeout has been reached, job will continue to run until is cancelled.
     *
     * @param newNode The Node whose permissions are to be recursively set
     */
    private void setNodeRecursiveSecure(final Node newNode) throws IOException
    {
        try
        {
            Subject.doAs(generateVOSpaceUser(), new PrivilegedExceptionAction<Void>()
            {
                @Override
                public Void run() throws Exception
                {
                    final ClientRecursiveSetNode rj = voSpaceClient.setNodeRecursive(newNode);

                    // Fire & forget is 'false'. 'true' will mean the run job does not return until it's finished.
                    rj.setMonitor(false);
                    rj.run();

                    return null;
                }
            });
        }
        catch (PrivilegedActionException pae)
        {
            throw new IOException(pae.getException());
        }
    }


    /**
     * Perform the HTTPS command.
     *
     * @param newNode The newly created Node.
     */
    private void setNodeSecure(final Node newNode) throws Exception
    {
        executeSecurely(new PrivilegedExceptionAction<Void>()
        {
            @Override
            public Void run() throws Exception
            {
                voSpaceClient.setNode(newNode);
                return null;
            }
        });
    }


    void createLink(final URI target) throws Exception
    {
        createNode(toLinkNode(target));
    }

    private LinkNode toLinkNode(final URI target)
    {
        final VOSURI linkNodeURI = toURI(getCurrentPath());
        return new LinkNode(linkNodeURI, target);
    }

    void createFolder() throws Exception
    {
        createNode(toContainerNode());
    }

    private ContainerNode toContainerNode()
    {
        return new ContainerNode(getCurrentItemURI());
    }

    void createNode(final Node newNode) throws Exception
    {
        executeSecurely(new PrivilegedExceptionAction<Void>()
        {
            @Override
            public Void run() throws Exception
            {
                voSpaceClient.createNode(newNode, false);
                return null;
            }
        });
    }

    @Delete
    public void deleteNode() throws Exception
    {
        executeSecurely(new PrivilegedExceptionAction<Void>()
        {
            @Override
            public Void run() throws Exception
            {
                voSpaceClient.deleteNode(getCurrentPath());
                return null;
            }
        });
    }

    <T> T executeSecurely(final PrivilegedExceptionAction<T> runnable) throws Exception
    {
        try
        {
            return Subject.doAs(generateVOSpaceUser(), runnable);
        }
        catch (PrivilegedActionException e)
        {
            throw e.getException();
        }
    }

    private void setNodeProperty(final List<NodeProperty> nodeProperties, final String propertyName,
                                 final String propertyValue)
    {
        nodeProperties.remove(new NodeProperty(propertyName, ""));

        if (!StringUtil.hasLength(propertyValue))
        {
            final NodeProperty np = new NodeProperty(propertyName, "");

            np.setMarkedForDeletion(true);
            nodeProperties.add(np);
        }
        else
        {
            nodeProperties.add(new NodeProperty(propertyName, propertyValue));
        }
    }

    @Post("json")
    public void update(final JsonRepresentation payload) throws Exception
    {
        final JSONObject jsonObject = payload.getJsonObject();

        // limit=0, detail=min so should only get the current node
        final Node currentNode = getCurrentNode(VOS.Detail.properties);
        final List<NodeProperty> nodeProperties = currentNode.getProperties();
        final Set<String> keySet = jsonObject.keySet();

        if (keySet.contains("publicPermission"))
        {
            final String parameterValue = Boolean.toString(jsonObject.get("publicPermission").equals("on"));
            final NodeProperty np = currentNode.findProperty(VOS.PROPERTY_URI_ISPUBLIC);

            if ((np != null) && !np.getPropertyValue().equals(parameterValue))
            {
                setNodeProperty(nodeProperties, VOS.PROPERTY_URI_ISPUBLIC, parameterValue);
            }
        }


        if (keySet.contains("readGroup"))
        {
            final String parameterValue =
                    StringUtil.hasLength((String) jsonObject.get("readGroup"))
                    ? IVO_GMS_PROPERTY_PREFIX + jsonObject.get("readGroup")
                    : "";

            final NodeProperty np = currentNode.findProperty(VOS.PROPERTY_URI_GROUPREAD);
            if (((np != null) && !np.getPropertyValue().equals(parameterValue))
                || ((np == null) && StringUtil.hasLength(parameterValue)))
            {
                setNodeProperty(nodeProperties, VOS.PROPERTY_URI_GROUPREAD, parameterValue);
            }
        }

        if (keySet.contains("writeGroup"))
        {
            final String parameterValue =
                    StringUtil.hasLength((String) jsonObject.get("writeGroup"))
                    ? IVO_GMS_PROPERTY_PREFIX + jsonObject.get("writeGroup")
                    : "";

            final NodeProperty np = currentNode.findProperty(VOS.PROPERTY_URI_GROUPWRITE);
            if (((np != null) && !np.getPropertyValue().equals(parameterValue))
                || ((np == null) && StringUtil.hasLength(parameterValue)))
            {
                setNodeProperty(nodeProperties, VOS.PROPERTY_URI_GROUPWRITE, parameterValue);
            }
        }

        // Recursively set permissions if requested
        if (jsonObject.keySet().contains("recursive"))
        {
            if (jsonObject.get("recursive").toString().equals("on"))
            {
                setNodeRecursiveSecure(currentNode);
                getResponse().setStatus(Status.SUCCESS_ACCEPTED);
            }
        }
        else
        {
            // Update the node properties
            setNodeSecure(currentNode);
            getResponse().setStatus(Status.SUCCESS_OK);
        }
    }
}
