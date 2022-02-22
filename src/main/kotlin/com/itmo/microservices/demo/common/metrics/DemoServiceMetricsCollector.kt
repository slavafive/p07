package com.itmo.microservices.demo.common.metrics

import com.itmo.microservices.commonlib.metrics.CommonMetricsCollector
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*

@Component
class DemoServiceMetricsCollector(serviceName: String): CommonMetricsCollector(serviceName) {
    constructor() : this(SERVICE_NAME)

    lateinit var productsServiceGetItemsCounter: Counter
    lateinit var shipping_orders_total: Counter


    @Autowired
    fun setMetrics(meterRegistry: MeterRegistry) {
        productsServiceGetItemsCounter = meterRegistry.counter("products.service.get.items.counter")
        shipping_orders_total = meterRegistry.counter("shipping.orders.total")
    }

    companion object {
        const val SERVICE_NAME = "demo_service"
    }
}
