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

import ca.nrc.cadc.beacon.UploadVerificationFailedException;
import ca.nrc.cadc.util.StringUtil;
import ca.nrc.cadc.vos.Node;
import ca.nrc.cadc.vos.NodeProperty;
import ca.nrc.cadc.vos.VOS;


public class UploadVerifier
{
    /**
     * Verify that each byte is accounted for on the server side.
     *
     * @param byteCount The count of bytes.
     * @param node      The node to verify.
     */
    public void verifyByteCount(final long byteCount, final Node node)
            throws UploadVerificationFailedException
    {
        if (byteCount < 0)
        {
            throw new IllegalArgumentException(
                    "The given byte count cannot be a negative value.");
        }
        else if (node == null)
        {
            throw new IllegalArgumentException(
                    "The given Node cannot be null.");
        }

        final NodeProperty contentLengthProperty =
                node.findProperty(VOS.PROPERTY_URI_CONTENTLENGTH);
        final long contentLength = contentLengthProperty == null
                                   ? 0L
                                   : Long.parseLong(
                contentLengthProperty.getPropertyValue());

        if (byteCount != contentLength)
        {
            throw new UploadVerificationFailedException("** ERROR ** - Upload did not succeed: "
                                                        + String.format("File length counted [%d] does not "
                                                                        + "match what the service said it "
                                                                        + "should be [%d]", byteCount,
                                                                        contentLength));
        }
    }

    /**
     * Verify the given MD5.
     * <p>
     * Note that the given MD5 will be converted to a Hex string, and then
     * the string will be compared to what the returned Node provided.
     *
     * @param calculatedMD5 The byte array of the calculated MD5.
     * @param node          The node to verify againts.
     */
    public void verifyMD5(final byte[] calculatedMD5, final Node node)
            throws UploadVerificationFailedException
    {
        if (calculatedMD5 == null)
        {
            throw new IllegalArgumentException(
                    "The calculated MD5 cannot be null.");
        }
        else if (node == null)
        {
            throw new IllegalArgumentException(
                    "The given Node cannot be null.");
        }

        final NodeProperty MD5Property =
                node.findProperty(VOS.PROPERTY_URI_CONTENTMD5);
        final String serverMD5String = MD5Property == null
                                       ? null
                                       : MD5Property.getPropertyValue();

        if (!StringUtil.hasLength(serverMD5String))
        {
            throw new UploadVerificationFailedException("** ERROR YOUR UPLOAD DID NOT SUCCEED ** "
                                                        + "MD5 checksum was not produced by "
                                                        + "service!  This was not expected, please "
                                                        + "contact canfarhelp@nrc-cnrc.gc.ca for "
                                                        + "assistance.");
        }
        else
        {
            if (!ca.nrc.cadc.util.HexUtil.toHex(calculatedMD5).equals(
                    serverMD5String))
            {
                throw new UploadVerificationFailedException(
                        "** ERROR ** - Upload did not succeed: "
                        + "MD5 checksum failed.");
            }
        }
    }
}
