package io.github.ucpdh23.vertice

enum class Events(override val payloadClass: Class<*>) : Event {
  INITIALIZE(String::class.java)
  ;
}
