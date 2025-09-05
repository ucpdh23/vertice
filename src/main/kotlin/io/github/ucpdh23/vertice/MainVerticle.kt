package io.github.ucpdh23.vertice


import io.vertx.core.Future
import io.vertx.core.VerticleBase

/**
 * This is an example of a verticle that starts an HTTP server.
 */
class MainVerticle : VerticleBase() {

  override fun start() : Future<*> {
    return vertx
      .createHttpServer()
      .requestHandler { req ->
        req.response()
          .putHeader("content-type", "text/plain")
          .end("Hello from Vert.x!")
      }
    .listen(8888).onSuccess { http ->
      println("HTTP server started on port 8888")
    }
  }
}
