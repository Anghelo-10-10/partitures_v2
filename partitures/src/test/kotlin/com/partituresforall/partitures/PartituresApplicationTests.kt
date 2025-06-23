package com.partituresforall.partitures

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableJpaRepositories("com.partituresforall.partitures.repositories")
@EntityScan("com.partituresforall.partitures.models.entities")
class PartituresApplication

fun main(args: Array<String>) {
	runApplication<PartituresApplication>(*args)
}