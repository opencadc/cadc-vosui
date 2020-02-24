FROM opencadc/tomcat:8.5-jdk11-slim

COPY LocalAuthority.properties /root/config/
COPY build/libs/*.war webapps/

EXPOSE 5555
