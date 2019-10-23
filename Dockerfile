# Alpine Linux with OpenJDK JRE
FROM openjdk:8-jre-alpine
# copy JAR into image
COPY target/*.jar /opt/app.jar
# run application with this command line
CMD ["/usr/bin/java", "-jar", "-Dspring.profiles.active=production", "/opt/app.jar"]
EXPOSE 7000