FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn --batch-mode verify dependency:copy-dependencies -DoutputDirectory=/app/dependencies

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/classes ./classes
COPY --from=build /app/dependencies ./lib
ENV BIND_HOST=0.0.0.0 PORT=10000 DATABASE_PATH=/var/data/payroll.db
RUN mkdir -p /var/data
EXPOSE 10000
CMD ["java", "-cp", "classes:lib/*", "payroll.Main", "--dashboard"]
