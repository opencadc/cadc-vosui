package ca.nrc.cadc.web;

import ca.nrc.cadc.auth.*;
import ca.nrc.cadc.net.NetUtil;
import ca.nrc.cadc.util.StringUtil;
import org.apache.log4j.Logger;
import org.restlet.Request;
import org.restlet.data.Cookie;
import org.restlet.util.Series;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Principal Extractor implementation using a Restlet Request.
 * * Created by hjeeves on 2017-01-11.
 */
public class RestletPrincipalExtractor implements PrincipalExtractor
{
    private static final Logger log =
            Logger.getLogger(RestletPrincipalExtractor.class);

    private final Request request;
    private boolean initialized = false;

    private SSOCookieCredential cookieCredential;
    private Principal cookiePrincipal; // principal extracted from cookie

    /**
     * Hidden no-arg constructor for testing.
     */
    RestletPrincipalExtractor()
    {
        this.request = null;
    }

    /**
     * Create this extractor from the given Restlet Request.
     *
     * @param req The Restlet Request.
     */
    public RestletPrincipalExtractor(final Request req)
    {
        this.request = req;
    }

    private void init()
    {
        if (!initialized)
        {
            final Series<Cookie> requestCookies = getRequest().getCookies();
            final Series<Cookie> cookies = new Series<>(Cookie.class);

            if (requestCookies != null)
            {
                cookies.addAll(requestCookies);
            }

            for (final Cookie ssoCookie : cookies)
            {
                if (SSOCookieManager.DEFAULT_SSO_COOKIE_NAME.equals(
                        ssoCookie.getName())
                    && StringUtil.hasText(ssoCookie.getValue()))
                {
                    final SSOCookieManager ssoCookieManager =
                            new SSOCookieManager();

                    try
                    {
                        cookiePrincipal = ssoCookieManager.parse(
                                ssoCookie.getValue()).getUser();
                        cookieCredential = new
                                SSOCookieCredential(ssoCookie.getValue(),
                                                    NetUtil.getDomainName(
                                                            getRequest()
                                                                    .getResourceRef()
                                                                    .toUrl()));
                    }
                    catch (IOException | InvalidDelegationTokenException e)
                    {
                        log.info("Cannot use SSO Cookie. Reason: "
                                 + e.getMessage());
                    }

                }
            }
        }

        initialized = true;
    }

    @Override
    public X509CertificateChain getCertificateChain()
    {
        return null;
    }

    @Override
    public Set<Principal> getPrincipals()
    {
        init();

        final Set<Principal> principals = new HashSet<>();

        addHTTPPrincipal(principals);

        return principals;
    }

    @Override
    public DelegationToken getDelegationToken()
    {
        return null;
    }

    /**
     * Add the HTTP Principal, if it exists.
     */
    private void addHTTPPrincipal(final Set<Principal> principals)
    {
        init();

        final String httpUser = getAuthenticatedUsername();

        // only add one HttpPrincipal, precedence order
        if (StringUtil.hasText(httpUser)) // user from HTTP AUTH
        {
            principals.add(new HttpPrincipal(httpUser));
        }

        if (cookiePrincipal != null) // user from cookie
        {
            principals.add(cookiePrincipal);
        }
    }


    /**
     * Obtain the Username submitted with the Request.
     *
     * @return String username, or null if none found.
     */
    private String getAuthenticatedUsername()
    {
        final String username;

        if (!getRequest().getClientInfo().getPrincipals().isEmpty())
        {
            // Put in to support Safari not injecting a Challenge Response.
            // Grab the first principal's name as the username.
            // update: this is *always* right and works with realms; the previous
            // call to getRequest().getChallengeResponse().getIdentifier() would
            // return whatever username the caller provided in a non-authenticating call
            username = getRequest().getClientInfo().getPrincipals().get(0)
                    .getName();
        }
        else
        {
            username = null;
        }

        return username;
    }


    public Request getRequest()
    {
        return request;
    }

    /**
     * Create and return a SSOCookieCredential from the request
     *
     * @return
     */
    @Override
    public List<SSOCookieCredential> getSSOCookieCredentials() {
        init();
        return Collections.singletonList(cookieCredential);
    }
}
