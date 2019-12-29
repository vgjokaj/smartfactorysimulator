FROM maven:3.5-jdk-8

# copy your source tree
COPY ./ /opt/project

WORKDIR /opt/project

# build for release
RUN mvn package

# set the startup command to run your binary
CMD ["java", "-jar", "./target/SmartFactorySimulator.jar", "/opt/project/config/config.json", "/opt/project/config/curveTemplates"]
