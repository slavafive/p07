package com.itmo.microservices.demo.order.api.messaging

import com.itmo.microservices.demo.order.api.model.BusketDto

data class BusketCreatedEvent(val busket: BusketDto)
data class BusketUpdatedEvent(val busket: BusketDto)
data class BusketDeletedEvent(val busket: BusketDto)