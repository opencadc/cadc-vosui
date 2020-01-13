---
---

#### User Storage User Interface

By default, this uses the CANFAR (CADC) VOSpace located at:

<a rel="external" href="https://www.canfar.phys.uvic.ca/vault">https://www.canfar.phys.uvic.ca/vault</a>

A fully functional UI is deployed at:
<a rel="external" href="http://www.canfar.net/storage/list">http://www.canfar.net/storage/list/</a>

<a href="https://travis-ci.org/opencadc/cadc-vosui"><img src="https://travis-ci.org/opencadc/cadc-vosui.svg?branch=master" /></a>


### Building

Running:

`gradle clean build`

Will produce a `jar` file in the `build/libs` directory that can be used to run a local Java container using the `gradle run` command.


### Running

#### Environment

In order for Authorization and Authentication to work properly, the User Storage UI uses Domain Cookies to pass from the browser to the application.
The browser will always send a valid cookie to the server whose domain matches the cookie's, but the application will then make another request
to the VOSpace Web Service using the cookie.  The VOSpace Web Service can run on a different domain, however, which presents a problem for
cookie passing.

To get around this, please supply a property called `SSO_SERVERS` containing a space delimited list of trusted servers one of two ways:

  - As a System property (e.g. `-DSSO_SERVERS="<host 1> <host 2>" etc.`)
  - In a file located at `${user.home}/config/AccessControl.properties` (e.g. `cat SSO_SERVERS=<host 1> <host 2> > $HOME/config/AccessControl.properties`)

The hostnames included in this property are all of the servers involved in your setup (i.e. the web server, and the VOSpace Web Service host).

For an embedded Jetty container, you can just run:

`gradle run`

To produce a running embedded Jetty container running on port `8080`, with a debug port on `5555`.

Pass your own Registry settings into the `JAVA_OPTS` environment variable to use your own VOSpace service:

`gradle -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5555 -Djava.security.egd=file:/dev/./urandom -Djsse.enableSNIExtension=false -Dca.nrc.cadc.reg.client.RegistryClient.host=<your host for IVOA Registry host> run`

To specify the Service ID (often called Resource ID) of your services.  The User Storage Interface relies on two services:


 - VOSpace
 - Group Management Service (Access Control)

To specify the Service ID for each service, add the appropriate System property for those, too e.g.:

`... -Dorg.opencadc.vospace.service_id=ivo://<your domain>/<vospace service name> -Dorg.opencadc.gms.service_id=ivo://<your domain>/<gms service name> ...`


Or, from the `examples` directory, deploy the `war` file in `build/libs` into a Java container such as Tomcat.

Then, in your browser, look at <a href="http://localhost:8080/storage/list">http://localhost:8080/storage/list</a>.

#### Running with Docker

See the Docker repo here:

<a rel="external" href="https://hub.docker.com/r/opencadc/storage/">https://hub.docker.com/r/opencadc/storage/</a>

It uses the lightweight Tomcat 8.5 java container that was built using Alpine Linux found here:

<a href="https://hub.docker.com/_/tomcat/" rel="external">https://hub.docker.com/_/tomcat/</a>

To run it as-is and use the CANFAR VOSpace Service, use:

`docker run --name storage -d -p 8080:8080 -p 5555:5555 opencadc/storage`

Then, in your browser, look at <a href="http://localhost:8080/storage/list">http://localhost:8080/storage/list</a>.

##### Running with Docker for your environment

To run in your environment, switch to the examples directory and create your own Dockerfile:

```
# This is the Docker hub location for the User Storage User Interface (Project Beacon)
FROM opencadc/storage

# The JAVA_OPTS variable to pass to Tomcat.  Note the -Dca.nrc.cadc.reg.client.RegistryClient.host property.
ENV JAVA_OPTS "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5555 -Djava.security.egd=file:/dev/./urandom -Djsse.enableSNIExtension=false -Dca.nrc.cadc.reg.client.RegistryClient.host=<your host for IVOA Registry lookup>"
```

Also in the examples directory, run:

`docker build -t user_storage_ui .`

Then run it:

`docker run --name storage -d -p 8080:8080 -p 5555:5555 user_storage_ui`

Or mount your own built `war`:

`docker run --name storage -d -p 8080:8080 -p 5555:5555 -v $(pwd)/build/libs:/usr/local/tomcat/webapps user_storage_ui`

Then, in your browser, look at <a href="http://localhost:8080/storage/list">http://localhost:8080/storage/list</a>.
