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


import ca.nrc.cadc.beacon.web.restlet.JSONRepresentation;
import ca.nrc.cadc.beacon.web.restlet.VOSpaceApplication;
import ca.nrc.cadc.vos.Node;
import ca.nrc.cadc.vos.NodeNotFoundException;
import ca.nrc.cadc.vos.VOS;
import ca.nrc.cadc.vos.VOSURI;
import ca.nrc.cadc.vos.client.VOSpaceClient;
import org.json.JSONException;
import org.json.JSONWriter;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
//import sun.jvm.hotspot.debugger.cdbg.AccessControl;

import java.io.FileNotFoundException;
import java.security.AccessControlException;

public class NodeServerResource extends StorageItemServerResource
{

    @Get("json")
    public void checkItemAccess() throws ResourceException
    {
//        try {

            Node requestedNode = getNode(getCurrentItemURI(), VOS.Detail.max, 0);

            // if it gets to here without throwing an error, check the writable property
            // If it's there AND it's true, return the read & write group property values
            // If it's there AND it's false, return a 403
            // if it's not there, return a 403

            // Node properties are in a known order, as in StorageItemCSVWriter.java
            final String writeGroupStr = requestedNode.getPropertyValue(VOS.PROPERTY_URI_WRITABLE);
            final String readGroupStr = requestedNode.getPropertyValue(VOS.PROPERTY_URI_READABLE);

            // needs to check if the current user owns this item.
            // there may be a write group and the person isn't in it.
            // TODO: I don't think this is right...
            // Should this just be passed back and the UI can check it?
            if (writeGroupStr == null)
            {
                throw new ResourceException(new AccessControlException("User can't write to node."));
            }
            else
            {
                writeResponse(Status.SUCCESS_OK,
                        new JSONRepresentation()
                        {
                            @Override
                            public void write(final JSONWriter jsonWriter)
                                    throws JSONException
                            {
                                jsonWriter.object().key("writeGroup").value(writeGroupStr).key("readGroup").value(readGroupStr)
                                        .endObject();
                            }
                        });

            }

//        }
//        catch (ResourceException re)
//        {
//            if (re.getCause() instanceof AccessControlException)
//            {
//
//            }
//            else
//            {
//                throw new ResourceException(re);
//            }
//
//////        catch (Exception e)
////        {
////            throw new IllegalAccessException(re.getMessage());
//        }

    }

}
