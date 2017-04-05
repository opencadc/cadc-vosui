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

package ca.nrc.cadc.beacon.web.restlet;

import ca.nrc.cadc.auth.SSOCookieCredential;
import ca.nrc.cadc.net.HttpDownload;
import ca.nrc.cadc.net.InputStreamWrapper;
import ca.nrc.cadc.util.FileUtil;
import org.restlet.data.MediaType;

import javax.security.auth.Subject;
import java.io.*;
import java.net.URL;


public abstract class JNLPRepresentation
        extends AbstractAuthOutputRepresentation
{
    private final SSOCookieCredential cookieCredential;
    private final String codeBase;


    /**
     * Constructor.
     */
    public JNLPRepresentation(final String codeBase,
                              final SSOCookieCredential cookieCredential)
    {
        super(MediaType.APPLICATION_JNLP);

        this.codeBase = codeBase;
        this.cookieCredential = cookieCredential;
    }

    /**
     * Write out a line of the JNLP file.  Useful to filter downstream.
     * @param line              The line to write.
     * @param outputStream            The OutputStream to write to.
     * @throws IOException      Any writing errors.
     */
    abstract void writeLine(final String line, final OutputStream outputStream)
            throws IOException;

    /**
     * Writes the representation to a byte stream. This method is ensured to
     * write the full content for each invocation unless it is a transient
     * representation, in which case an exception is thrown.<br>
     * <br>
     * Note that the class implementing this method shouldn't flush or close the
     * given {@link OutputStream} after writing to it as this will be handled by
     * the Restlet connectors automatically.
     *
     * @param outputStream The output stream.
     * @throws IOException  Any errors writing to the stream.
     */
    @Override
    public void write(final OutputStream outputStream) throws IOException
    {
        final String file = "VOSpaceUploadLauncher.jnlp";
        final InputStream launcherFileStream =
                getClass().getClassLoader().getResourceAsStream(file);
        final Reader reader = new InputStreamReader(launcherFileStream);
        final BufferedReader bufferedReader = new BufferedReader(reader);

        String line;

        while ((line = bufferedReader.readLine()) != null)
        {
            // Remove the href as it causes issues...
            line = line.replace("$$codebase$$", codeBase);
            if (cookieCredential != null)
            {
                line = line.replace("$$ssocookie$$", cookieCredential
                        .getSsoCookieValue().replaceAll("&", "&amp;"));
            }
//            line = line.replace("$$ssocookiedomain$$", cookieCredential.getDomain());

            writeLine(line, outputStream);
        }

        bufferedReader.close();
        outputStream.flush();
    }
}
