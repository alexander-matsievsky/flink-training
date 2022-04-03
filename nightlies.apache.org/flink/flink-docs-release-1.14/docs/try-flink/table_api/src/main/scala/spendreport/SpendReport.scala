package spendreport

import org.apache.flink.table.api.{
  EnvironmentSettings,
  FieldExpression,
  LiteralIntExpression,
  Table,
  TableEnvironment,
  Tumble,
  WithOperations,
  call
}
import org.apache.flink.table.functions.ScalarFunction

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import scala.language.postfixOps

object SpendReport {
  @throws[Exception]
  def main(args: Array[String]): Unit = {
    val tEnv = TableEnvironment.create(EnvironmentSettings.newInstance.inStreamingMode.build)
    tEnv.executeSql(
      """
      create table transactions
      (
          account_id       bigint,
          amount           bigint,
          transaction_time timestamp(3),
          watermark for transaction_time as transaction_time - interval '5' second
      ) with (
          'connector' = 'kafka',
          'format' = 'csv',
          'properties.bootstrap.servers' = 'kafka:9092',
          'topic' = 'transactions'
      )
      """
    )
    tEnv.executeSql(
      """
      create table spend_report
      (
          account_id bigint,
          log_ts     timestamp(3),
          amount     bigint,
          primary key (account_id, log_ts) not enforced
      ) with (
          'connector' = 'jdbc',
          'driver' = 'com.mysql.jdbc.Driver',
          'password' = 'demo-sql',
          'table-name' = 'spend_report',
          'url' = 'jdbc:mysql://mysql:3306/sql-demo',
          'username' = 'sql-demo'
      )
      """
    )
    report(tEnv.from("transactions")).executeInsert("spend_report")
  }

  def report(transactions: Table): Table =
    transactions
      .window(Tumble over (1 hour) on $"transaction_time" as "log_ts")
      .groupBy($"account_id", $"log_ts")
      .select($"account_id", $"log_ts".start as "log_ts", $"amount".sum as "amount")

  def reportBatch(transactions: Table): Table =
    transactions
      .groupBy($"account_id", call(classOf[FloorToHour], $"transaction_time") as "log_ts")
      .select($"account_id", $"log_ts", $"amount".sum as "amount")

  class FloorToHour extends ScalarFunction {
    def eval(localDateTime: LocalDateTime): LocalDateTime =
      localDateTime.truncatedTo(ChronoUnit.HOURS)
  }
}
