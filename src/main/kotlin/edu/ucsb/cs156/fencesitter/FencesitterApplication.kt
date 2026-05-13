package edu.ucsb.cs156.federate

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class federateApplication

fun main(args: Array<String>) {
    runApplication<federateApplication>(*args)
}
