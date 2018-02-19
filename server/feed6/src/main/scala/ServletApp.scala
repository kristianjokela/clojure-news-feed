/**
 * News Feed
 * news feed api
 *
 * OpenAPI spec version: 1.0.0
 * Contact: media@glennengstrand.info
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 */

package io.swagger.app

import org.scalatra.swagger.{ ApiInfo, SwaggerWithAuth, Swagger }
import org.scalatra.swagger.{ JacksonSwaggerBase, Swagger }
import org.scalatra.ScalatraServlet
import org.json4s.{ DefaultFormats, Formats }

class ResourcesApp(implicit protected val swagger: SwaggerApp)
  extends ScalatraServlet with JacksonSwaggerBase {
  before() {
    response.headers += ("Access-Control-Allow-Origin" -> "*")
  }
}

class SwaggerApp extends Swagger(apiInfo = ApiSwagger.apiInfo, apiVersion = "1.0", swaggerVersion = Swagger.SpecVersion)

object ApiSwagger {
  val apiInfo = ApiInfo(
    """News Feed""",
    """news feed api""",
    """http://swagger.io""",
    """media@glennengstrand.info""",
    """Apache 2.0""",
    """http://www.apache.org/licenses/LICENSE-2.0.html""")
}
