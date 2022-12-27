package me.zeroest.pushserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PushServerApplication

fun main(args: Array<String>) {
    runApplication<PushServerApplication>(*args)
}
