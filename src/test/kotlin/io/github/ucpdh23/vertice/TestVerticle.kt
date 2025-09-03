package io.github.ucpdh23.vertice

class TestVerticle : AbstractVerticle("Test", VerticleSetup(Actions.entries, listOf(Events.INITIALIZE)){ setup -> setup.withMemory = true}) {

  enum class Actions(override val payloadClass: Class<*>) : Action {
    INIT(String::class.java)
    ;
  }


  fun init(argument: String) : ActionResult {
      return ActionResult(true);
  }
}
