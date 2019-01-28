/*
 ************************************************************************
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 *
 * (c) 2014.                         (c) 2014.
 * National Research Council            Conseil national de recherches
 * Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 * All rights reserved                  Tous droits reserves
 *
 * NRC disclaims any warranties         Le CNRC denie toute garantie
 * expressed, implied, or statu-        enoncee, implicite ou legale,
 * tory, of any kind with respect       de quelque nature que se soit,
 * to the software, including           concernant le logiciel, y com-
 * without limitation any war-          pris sans restriction toute
 * ranty of merchantability or          garantie de valeur marchande
 * fitness for a particular pur-        ou de pertinence pour un usage
 * pose.  NRC shall not be liable       particulier.  Le CNRC ne
 * in any event for any damages,        pourra en aucun cas etre tenu
 * whether direct or indirect,          responsable de tout dommage,
 * special or general, consequen-       direct ou indirect, particul-
 * tial or incidental, arising          ier ou general, accessoire ou
 * from the use of the software.        fortuit, resultant de l'utili-
 *                                      sation du logiciel.
 *
 *
 * @author jenkinsd
 * 15/05/14 - 1:19 PM
 *
 *
 *
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */

package ca.nrc.cadc;


import ca.nrc.cadc.web.selenium.AbstractWebApplicationIntegrationTest;
import org.apache.commons.lang3.RandomStringUtils;


public class AbstractBrowserTest extends AbstractWebApplicationIntegrationTest {
    private static final char[] SEED_CHARS;

    static {
        final StringBuilder chars = new StringBuilder(128);

        for (char c = 'a'; c <= 'z'; c++) {
            chars.append(c);
        }

        for (char c = 'A'; c <= 'Z'; c++) {
            chars.append(c);
        }

        for (char c = '0'; c <= '9'; c++) {
            chars.append(c);
        }

        chars.append("_-()=+!,:@*$.");

        SEED_CHARS = chars.toString().toCharArray();
    }

    public AbstractBrowserTest() throws Exception {
        super();
    }


    /**
     * Generate an ASCII string, replacing the '\' and '+' characters with
     * underscores to keep them URL friendly.
     *
     * @return An ASCII string of 16.
     */
    protected String generateAlphaNumeric() {
        return generateAlphaNumeric(16);
    }

    /**
     * Generate an ASCII string, replacing the '\' and '+' characters with
     * underscores to keep them URL friendly.
     *
     * @param length The desired length of the generated string.
     * @return An ASCII string of the given length.
     */
    protected String generateAlphaNumeric(final int length) {
        return RandomStringUtils.random(length, 0, SEED_CHARS.length, false,
                                        false, SEED_CHARS);
    }
}
