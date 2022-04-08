#
#
# Dockerfile for building the Docker image of thr R2D Access Server
#
# The interopEHRate project: www.interopehrate.eu 
#
#

#
# Debian + JVM 8 + Tomcat 9
#
FROM tomcat:9.0.60-jre8-openjdk

#LABEL version="0.9.Test"

#
# Remove Tomcat default web apps
#
RUN rm -rf /usr/local/tomcat/webapps/*

#
# Copy the customised server.xml to Tomcat conf folder
#
COPY ./tomcat/server.xml /usr/local/tomcat/conf/server.xml
COPY ./tomcat/keystore.p12 /usr/local/tomcat/conf/keystore.p12

#
# Expose only the https port
#
EXPOSE 8443/tcp

#
# Copy the R2DA WAR file to be deployed as web app
#
COPY ./target/iehr-docker.war /usr/local/tomcat/webapps/iehr.war

#
# Copy the EHR-MW WAR file to be deployed as web app
#
COPY ./target/ehr-docker.war /usr/local/tomcat/webapps/ehr.war
