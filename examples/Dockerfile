FROM opencadc/tomcat:8.5-alpine

COPY LocalAuthority.properties /root/config/
COPY build/libs/*.war webapps/

EXPOSE 5555
