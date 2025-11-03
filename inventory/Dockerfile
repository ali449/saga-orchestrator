FROM infotechsoft/maven:3.9.6-openjdk-17 AS dependcies

# Set the working directory inside the container
WORKDIR /opt/app

# Copy Maven wrapper files
COPY .mvn/ .mvn

# Copy Maven wrapper script and the project's POM file
COPY mvnw ./
COPY inventory/parent-pom.xml ./pom.xml

# project parent copy
COPY common/pom.xml ./common/pom.xml
COPY inventory/pom.xml ./inventory/pom.xml

# Download dependencies using Maven
RUN mvn -B -e org.apache.maven.plugins:maven-dependency-plugin:3.1.2:go-offline -DexcludeArtifactIds=common

FROM infotechsoft/maven:3.9.6-openjdk-17 AS builder

WORKDIR /opt/app
COPY --from=dependcies /root/.m2 /root/.m2
COPY --from=dependcies /opt/app/ /opt/app

# Copy the application source code
COPY common/src/main ./common/src/main
COPY inventory/src/main ./inventory/src/main

# Build the application
RUN ./mvnw clean install

#RUN mvn -B -e clean install -DskipTests
#RUN mvn -pl inventory -am clean install -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /opt/app
COPY --from=builder /opt/app/inventory/target/*.jar /opt/app/inventory.jar

ENTRYPOINT ["java", "-jar", "/opt/app/inventory.jar"]