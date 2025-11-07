package misk.web.exceptions

import jakarta.inject.Inject
import misk.exceptions.UnauthenticatedException
import misk.exceptions.UnauthorizedException
import misk.inject.KAbstractModule
import misk.testing.MiskTest
import misk.testing.MiskTestModule
import misk.web.Get
import misk.web.ResponseContentType
import misk.web.WebActionModule
import misk.web.WebServerTestingModule
import misk.web.actions.WebAction
import misk.web.dashboard.AdminDashboardAccess
import misk.web.jetty.JettyService
import misk.web.mediatype.MediaTypes
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@MiskTest(startService = true)
internal class AdminDashboardExceptionMapperTest {
  @MiskTestModule val module = TestModule()

  @Inject lateinit var jettyService: JettyService

  @Test
  fun adminUnauthenticatedReturnsHtmlPage() {
    val response = get("/_admin/unauthenticated")
    assertThat(response.code).isEqualTo(401)
    
    val body = response.body?.string() ?: ""
    assertThat(body).contains("<html>", "Authentication Required")
    assertThat(body).contains("You need to be authenticated to access the admin dashboard")
    assertThat(body).contains("Please log in and try again")
    assertThat(response.header("Content-Type")).contains("text/html")
  }

  @Test
  fun adminUnauthorizedReturnsHtmlPage() {
    val response = get("/_admin/unauthorized")
    assertThat(response.code).isEqualTo(403)
    
    val body = response.body?.string() ?: ""
    assertThat(body).contains("<html>", "Access Denied")
    assertThat(body).contains("You don't have permission to access this admin dashboard feature")
    assertThat(body).contains("Contact your administrator to request the necessary permissions")
    assertThat(response.header("Content-Type")).contains("text/html")
  }

  @Test
  fun nonAdminUnauthenticatedReturnsPlainText() {
    val response = get("/unauthenticated")
    assertThat(response.code).isEqualTo(401)
    assertThat(response.body?.string()).isEqualTo("unauthenticated")
    assertThat(response.header("Content-Type")).contains("text/plain")
  }

  @Test
  fun nonAdminUnauthorizedReturnsPlainText() {
    val response = get("/unauthorized")
    assertThat(response.code).isEqualTo(403)
    assertThat(response.body?.string()).isEqualTo("unauthorized")
    assertThat(response.header("Content-Type")).contains("text/plain")
  }

  private fun get(path: String): okhttp3.Response {
    val httpClient = OkHttpClient()
    val request = Request.Builder()
      .get()
      .url(jettyService.httpServerUrl.newBuilder()
        .encodedPath(path)
        .build()
      )
      .build()
    return httpClient.newCall(request).execute()
  }

  class AdminUnauthenticatedAction @Inject constructor() : WebAction {
    @Get("/_admin/unauthenticated")
    @ResponseContentType(MediaTypes.TEXT_PLAIN_UTF8)
    @AdminDashboardAccess
    fun get(): String {
      throw UnauthenticatedException()
    }
  }

  class AdminUnauthorizedAction @Inject constructor() : WebAction {
    @Get("/_admin/unauthorized")
    @ResponseContentType(MediaTypes.TEXT_PLAIN_UTF8)
    @AdminDashboardAccess
    fun get(): String {
      throw UnauthorizedException()
    }
  }

  class NonAdminUnauthenticatedAction @Inject constructor() : WebAction {
    @Get("/unauthenticated")
    @ResponseContentType(MediaTypes.TEXT_PLAIN_UTF8)
    fun get(): String {
      throw UnauthenticatedException()
    }
  }

  class NonAdminUnauthorizedAction @Inject constructor() : WebAction {
    @Get("/unauthorized")
    @ResponseContentType(MediaTypes.TEXT_PLAIN_UTF8)
    fun get(): String {
      throw UnauthorizedException()
    }
  }

  class TestModule : KAbstractModule() {
    override fun configure() {
      install(WebServerTestingModule())
      install(WebActionModule.create<AdminUnauthenticatedAction>())
      install(WebActionModule.create<AdminUnauthorizedAction>())
      install(WebActionModule.create<NonAdminUnauthenticatedAction>())
      install(WebActionModule.create<NonAdminUnauthorizedAction>())
      
      // Install our custom exception mappers
      install(AdminDashboardExceptionMapperModule())
    }
  }
}