package com.fluxpay.engine

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FluxPayApplication

fun main(args: Array<String>) {
    runApplication<FluxPayApplication>(*args)
}
