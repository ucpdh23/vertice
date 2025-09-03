package io.github.ucpdh23.vertice

import io.vertx.core.json.JsonObject

class JsonUtils {
  companion object {
    fun toJson(response: Any): Any? {
      return null;
    }

    fun toBean(encode: String?, beanClass: Class<*>?): JsonObject {
      return JsonObject.of()
    }
  }

}
