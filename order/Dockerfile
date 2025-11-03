FROM infotechsoft/maven:3.9.6-openjdk-17 AS dependcies

# Set the working directory inside the container
WORKDIR /opt/app

# Copy Maven wrapper files
COPY .mvn/ .mvn

# Copy Maven wrapper script and the project's POM file
COPY mvnw ./
COPY order/parent-pom.xml ./pom.xml

# project parent copy
COPY common/pom.xml ./common/pom.xml
COPY order/pom.xml ./order/pom.xml

# Download dependencies using Maven
RUN mvn -B -e org.apache.maven.plugins:maven-dependency-plugin:3.1.2:go-offline -DexcludeArtifactIds=common

FROM infotechsoft/maven:3.9.6-openjdk-17 AS builder

WORKDIR /opt/app
COPY --from=dependcies /root/.m2 /root/.m2
COPY --from=dependcies /opt/app/ /opt/app

# Copy the application source code
COPY common/src/main ./common/src/main
COPY order/src/main ./order/src/main

# Build the application
RUN ./mvnw clean install

#RUN mvn -B -e clean install -DskipTests
#RUN mvn -pl order -am clean install -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /opt/app
COPY --from=builder /opt/app/order/target/*.jar /opt/app/order.jar

ENTRYPOINT ["java", "-jar", "/opt/app/order.jar"]