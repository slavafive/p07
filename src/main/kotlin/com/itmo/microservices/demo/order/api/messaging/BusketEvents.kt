package com.itmo.microservices.demo.order.api.messaging

import com.itmo.microservices.demo.order.api.model.BusketModel

data class BusketCreatedEvent(val busket: BusketModel)
data class BusketUpdatedEvent(val busket: BusketModel)
data class BusketDeletedEvent(val busket: BusketModel)