package io.github.ucpdh23.vertice

import java.lang.reflect.Method

interface EventBusItem {
  val name: String

  val payloadClass: Class<*>?
}

enum class EventBusItemType {
  ACTION,
  EVENT
}

data class EventBusItemDeclaration(val item: EventBusItem, val method: Method)


interface Action : EventBusItem

data class ActionResult(val ok: Boolean, val response: Any?) {
  constructor(ok: Boolean) : this(ok, null)
}

interface Event: EventBusItem {

  companion object {
    const val EVENT: String = "__EVENT__"
  }

}

data class EventResult(val ok: Boolean)
