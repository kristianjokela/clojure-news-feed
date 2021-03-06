package info.glennengstrand.news.resource

import io.vertx.lang.scala.ScalaVerticle
import io.vertx.scala.ext.web.{Router, RoutingContext}
import io.vertx.scala.ext.web.handler.BodyHandler
import io.vertx.lang.scala.ScalaLogger
import scala.concurrent.Future
import io.vertx.scala.core.Vertx
import info.glennengstrand.news.service.InMemoryCache
import info.glennengstrand.news.event._

object OutboundResource extends NewsFeedResource {
  private val ns = "Outbound::"
  private def getOutbound(router: Router, vertx: Vertx): Unit = {
    vertx.deployVerticle(ScalaVerticle.nameForVerticle[GetOutboundEvent], genDeployOptions(Topics.GetOutbound))
    router
      .get("/participant/:id/outbound")
      .handler(rc => {
        val k = key(ns)
        InMemoryCache.put(k, rc)
        publish(Topics.GetOutbound.name, k, vertx)
      })
  }
  private def searchOutbound(router: Router, vertx: Vertx): Unit = {
    vertx.deployVerticle(ScalaVerticle.nameForVerticle[SearchOutboundEvent], genDeployOptions(Topics.SearchOutbound))
    router
      .get("/outbound")
      .handler(rc => {
        val k = key(ns)
        InMemoryCache.put(k, rc)
        publish(Topics.SearchOutbound.name, k, vertx)
      })
  }
  private def createOutbound(router: Router, vertx: Vertx): Unit = {
    vertx.deployVerticle(ScalaVerticle.nameForVerticle[CreateOutboundEvent], genDeployOptions(Topics.CreateOutbound))
    router.post().handler(BodyHandler.create)
    router
      .post("/participant/:id/outbound")
      .handler(rc => {
        val k = key(ns)
        InMemoryCache.put(k, rc)
        publish(Topics.CreateOutbound.name, k, vertx)
      })
  }
  def route(router: Router, vertx: Vertx): Unit = {
    createOutbound(router, vertx)
    getOutbound(router, vertx)
    searchOutbound(router, vertx)
  }
}