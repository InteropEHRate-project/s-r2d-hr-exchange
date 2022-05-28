#
#
# Dockerfile for building the Docker image of the R2D Access Server
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
COPY ./tomcat/seven.token /usr/local/tomcat/seven.token
COPY ./tomcat/ten.token /usr/local/tomcat/ten.token

#
# Ports to expose
#
#EXPOSE 8443/tcp
EXPOSE 8080/tcp

#
# Copy the R2DA WAR file to be deployed as web app
#
#COPY ./target/r2da-chu.war /usr/local/tomcat/webapps/r2da.war
COPY ./target/r2da-ftgm.war /usr/local/tomcat/webapps/r2da.war

#
# Copy the EHR-MW WAR file to be deployed as web app
#
#COPY ./target/ehr-chu.war /usr/local/tomcat/webapps/ehr.war
COPY ./target/ehr-ftgm.war /usr/local/tomcat/webapps/ehr.war
