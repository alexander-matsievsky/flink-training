package spendreport

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector
import org.apache.flink.walkthrough.common.entity.{Alert, Transaction}

object FraudDetector {
  val SMALL_AMOUNT: Double = 1.00
  val LARGE_AMOUNT: Double = 500.00
  val ONE_MINUTE: Long = 60 * 1000L
}

@SerialVersionUID(1L)
class FraudDetector extends KeyedProcessFunction[Long, Transaction, Alert] {
  @transient private var prevSmallAmount: ValueState[java.lang.Boolean] = _
  @transient private var prevSmallAmountTimeout: ValueState[java.lang.Long] = _

  @throws[Exception]
  override def onTimer(timestamp: Long,
                       ctx: KeyedProcessFunction[Long, Transaction, Alert]#OnTimerContext,
                       out: Collector[Alert]): Unit = {
    prevSmallAmount.clear()
    prevSmallAmountTimeout.clear()
  }

  @throws[Exception]
  override def open(parameters: Configuration): Unit = {
    prevSmallAmount =
      getRuntimeContext.getState(new ValueStateDescriptor("prevSmallAmount", Types.BOOLEAN))
    prevSmallAmountTimeout =
      getRuntimeContext.getState(new ValueStateDescriptor("prevSmallAmountTimeout", Types.LONG))
  }

  @throws[Exception]
  def processElement(transaction: Transaction,
                     context: KeyedProcessFunction[Long, Transaction, Alert]#Context,
                     collector: Collector[Alert]): Unit =
    (prevSmallAmount.value, transaction.getAmount) match {
      case (java.lang.Boolean.TRUE, amount) if amount > FraudDetector.LARGE_AMOUNT =>
        val alert = new Alert
        alert.setId(transaction.getAccountId)
        collector.collect(alert)
        context.timerService.deleteProcessingTimeTimer(prevSmallAmountTimeout.value)
        prevSmallAmount.clear()
        prevSmallAmountTimeout.clear()
      case (_, amount) if amount < FraudDetector.SMALL_AMOUNT =>
        val timeout = context.timerService.currentProcessingTime + FraudDetector.ONE_MINUTE
        context.timerService.registerProcessingTimeTimer(timeout)
        prevSmallAmount.update(true)
        prevSmallAmountTimeout.update(timeout)
      case _ =>
        context.timerService.deleteProcessingTimeTimer(prevSmallAmountTimeout.value)
        prevSmallAmount.clear()
        prevSmallAmountTimeout.clear()
    }
}
