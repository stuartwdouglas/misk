package misk.web.exceptions

import jakarta.inject.Inject
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import misk.exceptions.UnauthenticatedException
import misk.web.HttpCall
import misk.web.Response
import misk.web.ResponseBody
import misk.web.mediatype.MediaTypes
import misk.web.toResponseBody
import okhttp3.Headers.Companion.headersOf
import org.slf4j.event.Level
import java.net.HttpURLConnection

/**
 * Exception mapper that provides a better user experience for 401 errors on admin dashboard paths.
 * Instead of returning plain text responses, this mapper renders an HTML error page with helpful
 * information and guidance for users trying to access admin functionality.
 */
internal class AdminDashboardUnauthenticatedExceptionMapper @Inject constructor(
  private val httpCall: HttpCall
) : ExceptionMapper<UnauthenticatedException> {
  
  override fun toResponse(th: UnauthenticatedException): Response<ResponseBody> {
    // Only provide custom HTML response for admin dashboard paths
    if (!httpCall.url.encodedPath.startsWith("/_admin/")) {
      // Fall back to default mapper for non-admin paths
      return Response(
        "unauthenticated".toResponseBody(),
        headersOf("Content-Type", MediaTypes.TEXT_PLAIN_UTF8),
        HttpURLConnection.HTTP_UNAUTHORIZED
      )
    }
    
    return createHtmlErrorResponse(
      title = "Authentication Required",
      statusCode = HttpURLConnection.HTTP_UNAUTHORIZED,
      heading = "Authentication Required", 
      message = "You need to be authenticated to access the admin dashboard.",
      suggestion = "Please log in and try again."
    )
  }
  
  private fun createHtmlErrorResponse(
    title: String,
    statusCode: Int,
    heading: String,
    message: String,
    suggestion: String
  ): Response<ResponseBody> {
    val html = createHTML().html {
      head {
        title(title)
        meta {
          name = "viewport"
          content = "width=device-width, initial-scale=1"
        }
        style {
          +"""
            body {
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
              margin: 0;
              padding: 40px 20px;
              background-color: #f8f9fa;
              color: #495057;
              line-height: 1.5;
            }
            .container {
              max-width: 600px;
              margin: 0 auto;
              background: white;
              padding: 40px;
              border-radius: 8px;
              box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            }
            h1 {
              color: #dc3545;
              margin-top: 0;
              font-size: 24px;
            }
            .status-code {
              font-size: 48px;
              font-weight: bold;
              color: #dc3545;
              margin-bottom: 10px;
            }
            .message {
              font-size: 16px;
              margin-bottom: 20px;
            }
            .suggestion {
              padding: 16px;
              background-color: #e9ecef;
              border-radius: 4px;
              border-left: 4px solid #007bff;
            }
            .back-link {
              margin-top: 30px;
            }
            .back-link a {
              color: #007bff;
              text-decoration: none;
              font-weight: 500;
            }
            .back-link a:hover {
              text-decoration: underline;
            }
          """.trimIndent()
        }
      }
      body {
        div("container") {
          div("status-code") { +"$statusCode" }
          h1 { +heading }
          p("message") { +message }
          div("suggestion") {
            strong { +"What can you do?" }
            br
            +suggestion
          }
          div("back-link") {
            a {
              href = "/"
              +"← Return to Home"
            }
          }
        }
      }
    }
    
    return Response(
      html.toResponseBody(),
      headersOf("Content-Type", MediaTypes.TEXT_HTML_UTF8),
      statusCode
    )
  }
  
  override fun loggingLevel(th: UnauthenticatedException): Level = Level.WARN
  
  override fun isError(th: UnauthenticatedException): Boolean = true
}