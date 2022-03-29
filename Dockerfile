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

#
# Remove Tomcat default web apps
#
RUN rm -rf /usr/local/tomcat/webapps/*

#
# Copy the customised server.xml to Tomcat conf folder
#
COPY ./tomcat/server.xml /usr/local/tomcat/conf/server.xml

#
# Copy the R2DA WAR file to be deployed as web app
#
COPY ./target/iehr-docker.war /usr/local/tomcat/webapps/iehr.war

#
# Copy the EHR-MW WAR file to be deployed as web app
#
COPY ./target/ehr-docker.war /usr/local/tomcat/webapps/ehr.war

