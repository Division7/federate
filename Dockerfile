FROM maven:3.9.15-eclipse-temurin-25-noble

WORKDIR /home/app

COPY . .

RUN mvn -Dkotlin.compiler.execution.strategy=in-process --no-transfer-progress -B -Pproduction -DskipTests -f /home/app/pom.xml clean package

ENTRYPOINT ["mvn", "spring-boot:run"]


