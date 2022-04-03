FROM hseeberger/scala-sbt:11.0.14.1_1.6.2_2.12.15 AS builder
WORKDIR /opt/table_api
COPY build.sbt build.sbt
COPY project project
COPY src src
RUN sbt package

FROM apache/flink:1.13.6-scala_2.12-java11
COPY --from=builder /opt/table_api/target/scala-2.12/table_api*.jar /opt/flink/usrlib/table_api.jar
COPY flink-conf.yaml /opt/flink/conf/flink-conf.yaml
RUN wget -P /opt/flink/lib/ \
    https://repo.maven.apache.org/maven2/org/apache/flink/flink-sql-connector-kafka_2.12/1.13.6/flink-sql-connector-kafka_2.12-1.13.6.jar \
    https://repo.maven.apache.org/maven2/org/apache/flink/flink-connector-jdbc_2.12/1.13.6/flink-connector-jdbc_2.12-1.13.6.jar \
    https://repo.maven.apache.org/maven2/org/apache/flink/flink-csv/1.13.6/flink-csv-1.13.6.jar \
    https://repo.maven.apache.org/maven2/mysql/mysql-connector-java/8.0.19/mysql-connector-java-8.0.19.jar \
