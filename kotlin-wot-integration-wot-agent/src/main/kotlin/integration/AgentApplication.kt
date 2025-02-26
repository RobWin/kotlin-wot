package integration

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


fun main(args: Array<String>) {
    runApplication<AgentApplication>(*args)
}

@SpringBootApplication
class AgentApplication
