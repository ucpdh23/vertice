package io.github.ucpdh23.vertice

import io.vertx.core.Future
import io.vertx.core.VerticleBase
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.Json
import java.lang.reflect.Method
import java.util.concurrent.Callable
import java.util.function.Consumer
import java.util.logging.Logger
import java.util.regex.Pattern


abstract class AbstractVerticle(val name: String, val setup: VerticleSetup) : VerticleBase() {

  private val availableActions : MutableMap<String, EventBusItemDeclaration> = LinkedHashMap()
  private val availableEvents : MutableMap<String, EventBusItemDeclaration> = LinkedHashMap()

  companion object {
    val VERTICLE_NAME_PATTERN: Pattern = Pattern.compile("\\.([^\\.]*)Verticle\\d?\\.")
    val LOGGER: Logger = Logger.getLogger(AbstractVerticle::class.java.name)
  }

  override fun start() : Future<Any> {
    setup();

    val eb = vertx.eventBus()

    if (availableActions.isNotEmpty())
      eb.consumer<JsonObject>(this.name).handler { it -> processEventBusItem(it,
        EventBusItemType.ACTION, availableActions) }

    if (availableEvents.isNotEmpty())
      eb.consumer<JsonObject>(Event.EVENT).handler { it -> processEventBusItem(it,
        EventBusItemType.EVENT, availableEvents) }

    return Future.succeededFuture()
  }


  private fun processEventBusItem(message: Message<JsonObject>, type: EventBusItemType, availableItems: Map<String, EventBusItemDeclaration>) {
    val body = message.body()

    val itemName: String = body.getString("item")

    val declaration = availableItems[itemName]
    if (declaration == null) {
      if (type == EventBusItemType.ACTION)
        LOGGER.warning("Cannot perform action [$itemName] in verticle [$name]")
      return
    }

    val eventBusItem: EventBusItem = declaration.item
    val method: Method = declaration.method

    vertx.executeBlocking<Any>(Callable<Any> { ->
        try {
          val parameterCount = method.parameterCount
          var result: Any? = null
          if (parameterCount == 0) {
            result = method.invoke(this)
          } else if (parameterCount == 1) {
            val beanClass = eventBusItem.payloadClass

            var parameter: Any? = message
            if (beanClass != null) {
                val entity: JsonObject = body.getJsonObject("bean")
                parameter =
                  if (beanClass == JsonObject::class.java) entity else JsonUtils.toBean(entity.encode(), beanClass)
            }

            result = method.invoke(this, parameter)
          } else if (parameterCount == 2) {
            val beanClass = eventBusItem.payloadClass
            val entity: JsonObject = body.getJsonObject("bean")
            val newInstance: Any =
              if (beanClass == JsonObject::class.java) entity else JsonUtils.toBean(entity.encode(), beanClass)

            result = method.invoke(this, newInstance, message)
          }

          if (result is ActionResult) {
            if (type != EventBusItemType.ACTION) {

            }

            if (result.response != null) {
              val response = JsonUtils.toJson(result.response!!)
              message.replyAndRequest<Json>(response)
            }
          } else if (result is EventResult) {
            LOGGER.info("$result")
          }
        } catch (e: Exception) {
          LOGGER.warning("Item: [$itemName] Type: [$type] \n$e")
        }
      })

  }

  private fun setup() {
    val availableFunctions = extractFunctions()

    supportedEventBusItems(this.setup.actions, EventBusItemType.ACTION, availableFunctions, availableActions);
    supportedEventBusItems(this.setup.events, EventBusItemType.EVENT, availableFunctions, availableEvents);
  }

  private fun extractFunctions() : Map<String, Method> {
    return this.javaClass.methods.associateBy { it.name }
  }

  private fun resolveVerticleName(canonicalName: String) : String {
    val matcher = VERTICLE_NAME_PATTERN.matcher(canonicalName);

    if (matcher.find()) {
      return matcher.group(1).lowercase() + ".verticle"
    }

    throw RuntimeException("Not found pattern of 'Verticle' into canonicalName: $canonicalName");
  }

  private fun checkActionsFromSameClass(action : Action) {
    val actionsVerticleName: String = resolveVerticleName(action.javaClass.getCanonicalName())

    if (actionsVerticleName == name) {
      LOGGER.info("adding support for actions of verticleName [$actionsVerticleName]")
    } else {
      LOGGER.severe("actions VerticleName [$actionsVerticleName] is not equals to verticle provided name [$name]")
      throw RuntimeException(("verticleName $name").toString() + " is not valid")
    }
  }

  private fun supportedEventBusItems(items: List<EventBusItem>, type: EventBusItemType, availableFunctions: Map<String, Method>, availableItems: MutableMap<String, EventBusItemDeclaration>) {
    if (items.isNotEmpty() && type == EventBusItemType.ACTION) {
      checkActionsFromSameClass(items[0] as Action)
    } else {
      LOGGER.info("No items of type $type to support")
    }

    for (item in items) {
      availableItems[item.name] = EventBusItemDeclaration(item, availableFunctions.getValue(item.name.lowercase()))
    }

    LOGGER.info("Done")
  }

}

data class VerticleSetup(internal val actions: List<Action> = ArrayList(), internal val events: List<Event> = ArrayList(), public var withMemory: Boolean = false) {
  constructor(actions: List<Action>, events: List<Event>, function: Consumer<VerticleSetup>) : this() {
    function.accept(this)
  }
}
