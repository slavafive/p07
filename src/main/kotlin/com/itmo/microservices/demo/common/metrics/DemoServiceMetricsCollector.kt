package com.itmo.microservices.demo.common.metrics

import com.itmo.microservices.commonlib.metrics.CommonMetricsCollector
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DemoServiceMetricsCollector(serviceName: String): CommonMetricsCollector(serviceName) {
    constructor() : this(SERVICE_NAME)

    lateinit var catalogShown: Counter
    lateinit var itemAdded: Counter
    lateinit var orderCreated: Counter
    lateinit var itemBookRequest: Counter
    lateinit var shippingOrdersTotal: Counter


    @Autowired
    fun setMetrics(meterRegistry: MeterRegistry) {
        //Количество просмотров каталога продукции
        catalogShown = meterRegistry.counter("catalog.shown")
        //Количество добавлений товара (товаров) в заказ
        itemAdded = meterRegistry.counter("item.added")
        //Создание нового заказа
        orderCreated = meterRegistry.counter("order.created")
        //Количество запросов на бронирование товаров для заказа
        itemBookRequest = meterRegistry.counter("item.book.request", listOf(Tag.of("STATUS", "FAILED")))
        shippingOrdersTotal = meterRegistry.counter("shipping.orders.total")
    }

    companion object {
        const val SERVICE_NAME = "demo_service"
    }
}
