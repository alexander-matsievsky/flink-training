package spendreport

import org.apache.flink.table.api.{EnvironmentSettings, Table, TableEnvironment}
import org.apache.flink.types.{Row, RowUtils}

import java.util.stream.{Collectors, StreamSupport}
import java.util.{Spliterator, Spliterators}

class SpendReportTest extends org.scalatest.funsuite.AnyFunSuite {
  private def materialize(table: Table): java.util.List[Row] =
    StreamSupport
      .stream(Spliterators.spliteratorUnknownSize(table.execute.collect, Spliterator.ORDERED),
              false)
      .collect(Collectors.toList())

  test("SpendReport.report") {
    val tEnv = TableEnvironment.create(EnvironmentSettings.newInstance.inBatchMode.build)
    val transactions = tEnv.sqlQuery(
      """
      select cast(account_id as bigint)             as account_id,
             cast(amount as bigint)                 as amount,
             cast(transaction_time as timestamp(3)) as transaction_time
      from (values (1, 188, to_timestamp('2020-01-01') + interval '12' minute),
                   (2, 374, to_timestamp('2020-01-01') + interval '47' minute),
                   (3, 112, to_timestamp('2020-01-01') + interval '36' minute),
                   (4, 478, to_timestamp('2020-01-01') + interval '3' minute),
                   (5, 208, to_timestamp('2020-01-01') + interval '8' minute),
                   (1, 379, to_timestamp('2020-01-01') + interval '53' minute),
                   (2, 351, to_timestamp('2020-01-01') + interval '32' minute),
                   (3, 320, to_timestamp('2020-01-01') + interval '31' minute),
                   (4, 259, to_timestamp('2020-01-01') + interval '19' minute),
                   (5, 273, to_timestamp('2020-01-01') + interval '42' minute))
               as t(account_id, amount, transaction_time)
      """
    )
    val report = tEnv.sqlQuery(
      """
      select cast(account_id as bigint)   as account_id,
             cast(log_ts as timestamp(3)) as log_ts,
             cast(amount as bigint)       as amount
      from (values (1, '2020-01-01', 567),
                   (2, '2020-01-01', 725),
                   (3, '2020-01-01', 432),
                   (4, '2020-01-01', 737),
                   (5, '2020-01-01', 481))
               as t(account_id, log_ts, amount)
      """
    )
    assert(
      RowUtils.compareRows(materialize(SpendReport.report(transactions)), materialize(report), true)
    )
  }
}
