package com.itmo.microservices.demo.delivery.impl.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.eventbus.EventBus
import com.itmo.microservices.commonlib.annotations.InjectEventLogger
import com.itmo.microservices.commonlib.logging.EventLogger
import com.itmo.microservices.demo.common.exception.NotFoundException
import com.itmo.microservices.demo.common.metrics.DemoServiceMetricsCollector
import com.itmo.microservices.demo.delivery.api.model.DeliveryInfoRecordModel
import com.itmo.microservices.demo.delivery.api.service.DeliveryService
import com.itmo.microservices.demo.delivery.impl.entity.DeliveryInfoRecord
import com.itmo.microservices.demo.delivery.impl.entity.DeliverySubmissionOutcome
import com.itmo.microservices.demo.delivery.impl.event.OrderStatusChanged
import com.itmo.microservices.demo.delivery.impl.repository.DeliveryInfoRecordRepository
import com.itmo.microservices.demo.delivery.impl.utils.toModel
import com.itmo.microservices.demo.notifications.impl.service.StubNotificationService
import com.itmo.microservices.demo.order.api.model.OrderDto
import com.itmo.microservices.demo.order.api.model.OrderStatus
import com.itmo.microservices.demo.order.impl.entity.OrderEntity
import com.itmo.microservices.demo.order.impl.repository.OrderRepository
import com.itmo.microservices.demo.order.impl.util.toEntity
import com.itmo.microservices.demo.products.impl.repository.ProductsRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.configurationprocessor.json.JSONObject
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpConnectTimeoutException
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PostConstruct
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaDuration
const val url ="http://77.234.215.138:30027"
//val url ="http://127.0.0.1:30027"
@Service
class Timer {
    //Virtual time
    var time: Int = 0

    @PostConstruct
    fun timerStart() {
        val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
        executor.scheduleAtFixedRate(
            { this.time++ },
            0,
            1000,
            TimeUnit.MILLISECONDS
        )
    }

    fun get_time() = this.time
}


@Suppress("UnstableApiUsage")
@EnableScheduling
@Service
class DefaultDeliveryService(
    private val deliveryInfoRecordRepository: DeliveryInfoRecordRepository,
    private val orderRepository: OrderRepository,
    private val eventBus: EventBus,
    private val timer: Timer,
    private val productsRepository: ProductsRepository
) : DeliveryService {

    @Autowired
    private lateinit var metricsCollector: DemoServiceMetricsCollector

    private val postToken = mapOf("clientSecret" to "8ddfb4e8-7f83-4c33-b7ac-8504f7c99205")
    private val objectMapper = ObjectMapper()
    private val postBody: String = objectMapper.writeValueAsString(postToken)

    @OptIn(ExperimentalTime::class)
    private val timeout = Duration.seconds(10).toJavaDuration()
    val httpClient: HttpClient = HttpClient.newBuilder().build()


    private fun getPostHeaders(body: String): HttpRequest {
        return HttpRequest.newBuilder()
            .uri(URI.create("$url/transactions"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(java.time.Duration.ofSeconds(5))
            .build()
    }


    @InjectEventLogger
    private lateinit var eventLogger: EventLogger

    companion object {
        val log: Logger = LoggerFactory.getLogger(StubNotificationService::class.java)
    }

    @Autowired
    var pollingForResult: PollingForResult? = null

    var countOrdersWaitingForDeliver = AtomicInteger(0)

    override fun getSlots(number: Int): List<Int> {
        var list = mutableListOf<Int>()
        var startTime: Int = timer.get_time() + 30 + 3 * countOrdersWaitingForDeliver.get()
        for (i: Int in 1..number) {
            list.add(startTime)
            startTime += 3
        }
        log.info("return list of slot")
        return list.toList()
    }

    override fun getDeliveryHistoryById(transactionId: String):List<DeliveryInfoRecordModel> {
        val list =  deliveryInfoRecordRepository.getAllByTransactionId(UUID.fromString(transactionId))
        var mList = mutableListOf<DeliveryInfoRecordModel>()
        for(e in list){
            mList.add(e.toModel())
        }
        return mList.toList()
    }


    override fun delivery(orderDto: OrderDto) {
        log.info("a new delivery set")
        val order = orderRepository.findByIdOrNull(orderDto.id) ?: throw NotFoundException("Order ${orderDto.id} not found")
        order.status = OrderStatus.SHIPPING
        orderRepository.save(order)
        metricsCollector.shippingOrdersTotalCounter.increment()
        metricsCollector.currentShippingOrdersGauge.incrementAndGet() // when success or fail , dec 1
        delivery(orderDto, 1)
    }

    fun countRefundMoneyAmount(order: OrderDto): Double {
        var refund = 0.0
        order.itemsMap?.forEach { (productId, count) ->
            val product = productsRepository.findById(productId)
            refund += product.get().price?.times(count) ?: 0
        }
        return refund
    }

    override fun delivery(orderDto: OrderDto, times: Int) {
        if (orderDto.deliveryDuration!! < this.timer.get_time()) {
            log.info("a delivery EXPIRED : now is "+this.timer.get_time()+"but seleted was"+orderDto.deliveryDuration)
            if(times == 1){
                //never used external system
                //Количество заказов, которые перешли в возврат, поскольку ваша система предсказала неправильное время и время на доставку истекло еще до отправки во внешнюю систему
                metricsCollector.refunedDueToWrongTimePredictionOrder.increment()
            }
            metricsCollector.expiredDeliveryOrder.increment()
            metricsCollector.failedDelivery.increment()
            //?
            metricsCollector.externalSystemExpenseDeliveryCounter.increment(50.0)// should be  the total price of the products in order // and also see definition
            val refundMoneyAmount = countRefundMoneyAmount(orderDto)
            metricsCollector.refundedMoneyAmountDeliveryFailedCounter.increment(refundMoneyAmount)
            metricsCollector.currentShippingOrdersGauge.decrementAndGet()
            val timeStamp = System.currentTimeMillis()
            deliveryInfoRecordRepository.save(
                DeliveryInfoRecord(
                    DeliverySubmissionOutcome.EXPIRED,
                    timeStamp,
                    1,
                    timeStamp,
                    orderDto.id!!,
                    timeStamp
                )
            )

            val order = orderRepository.findByIdOrNull(orderDto.id) ?: throw NotFoundException("Order ${orderDto.id} not found")
            order.status = OrderStatus.REFUND
            orderRepository.save(order)

        }
        else {
            //没有过期
            try {
                orderDto.id?.let { eventBus.post(OrderStatusChanged(it, OrderStatus.SHIPPING)) }
                log.info("send delivery requesting")
                val response = httpClient.send(getPostHeaders(postBody), HttpResponse.BodyHandlers.ofString())
                val responseJson = JSONObject(response.body())
                if (response.statusCode() == 200) {
                    log.info("delivery processing , maybe fail")
                    Thread.sleep(2000)
                    pollingForResult?.getDeliveryResult(orderDto, responseJson, 1)
                } else {
                    delivery(orderDto,times+1)
                }
            } catch (e: HttpConnectTimeoutException) {
                log.info("Request timeout!--${times} time(s)")
                delivery(orderDto,times+1)
                return
            }
            orderDto.id?.let { eventBus.post(OrderStatusChanged(it, OrderStatus.COMPLETED)) }
        }
    }
}


@Service
class PollingForResult(
    private val deliveryInfoRecordRepository: DeliveryInfoRecordRepository,
    private val timer: Timer,
    private val metricsCollector: DemoServiceMetricsCollector,
    private val orderRepository: OrderRepository,
    private val productsRepository: ProductsRepository
) {
    private val postToken = mapOf("clientSecret" to "8ddfb4e8-7f83-4c33-b7ac-8504f7c99205")
    private val objectMapper = ObjectMapper()
    private val postBody: String = objectMapper.writeValueAsString(postToken)

    @OptIn(ExperimentalTime::class)
    private val timeout = Duration.seconds(10).toJavaDuration()
    val httpClient: HttpClient = HttpClient.newBuilder().build()

    private fun getGetHeaders(id: String): HttpRequest {
        return HttpRequest.newBuilder()
            .uri(URI.create("$url/transactions/$id"))
            .timeout(this.timeout)
            .header("Content-Type", "application/json")
            .timeout(java.time.Duration.ofSeconds(5))
            .build()
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(StubNotificationService::class.java)
    }

    var schedulePool: ScheduledExecutorService = Executors.newScheduledThreadPool(2)

    fun countRefundMoneyAmount(order: OrderDto): Double {
        var refund = 0.0
        order.itemsMap?.forEach { (productId, count) ->
            val product = productsRepository.findById(productId)
            refund += product.get().price?.times(count) ?: 0
        }
        return refund
    }

    fun getDeliveryResult(orderDto: OrderDto, responseJson_post: JSONObject, times: Int) {
        if (times > 3) {
            return
        }
        schedulePool.schedule(
            {
                val response_poll = httpClient.send(
                    getGetHeaders(responseJson_post.getString("id")),
                    HttpResponse.BodyHandlers.ofString()
                )
                val responseJson_poll = JSONObject(response_poll.body())
                log.info("getting response from 3th system")
                if (responseJson_poll.getString("status") == "SUCCESS") {
                    log.info("delivery success")
                    metricsCollector.successDelivery.increment()
                    val order = orderRepository.findByIdOrNull(orderDto.id) ?: throw NotFoundException("Order ${orderDto.id} not found")
                    order.status = OrderStatus.COMPLETED
                    orderRepository.save(order)

                    metricsCollector.currentShippingOrdersGauge.decrementAndGet()
                    deliveryInfoRecordRepository.save(
                        DeliveryInfoRecord(
                            DeliverySubmissionOutcome.SUCCESS,
                            responseJson_poll.getLong("delta"),
                            times,
                            responseJson_poll.getLong("completedTime"),
                            orderDto.id!!,
                            responseJson_poll.getLong("submitTime")
                        )
                    )
                } else {
                    log.info("delivery fail")
                    metricsCollector.failedDelivery.increment()
                    metricsCollector.currentShippingOrdersGauge.decrementAndGet()
                    val order = orderRepository.findByIdOrNull(orderDto.id) ?: throw NotFoundException("Order ${orderDto.id} not found")
                    order.status = OrderStatus.REFUND
                    orderRepository.save(order)

                    deliveryInfoRecordRepository.save(
                        DeliveryInfoRecord(
                            DeliverySubmissionOutcome.FAILURE,
                            responseJson_poll.getLong("delta"),
                            times,
                            responseJson_poll.getLong("completedTime"),
                            orderDto.id!!,
                            responseJson_poll.getLong("submitTime")
                        )
                    )
                    val refundMoneyAmount = countRefundMoneyAmount(orderDto)
                    metricsCollector.refundedMoneyAmountDeliveryFailedCounter.increment(refundMoneyAmount)
                }

            }, (5).toLong(), TimeUnit.SECONDS
        )
    }

}




