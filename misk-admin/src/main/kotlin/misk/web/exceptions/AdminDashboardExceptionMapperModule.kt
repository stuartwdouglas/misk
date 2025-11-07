package misk.web.exceptions

import misk.exceptions.UnauthenticatedException
import misk.exceptions.UnauthorizedException
import misk.inject.KAbstractModule

/**
 * Module that installs admin-specific exception mappers to provide better error pages
 * for authentication and authorization failures on admin dashboard paths.
 */
class AdminDashboardExceptionMapperModule : KAbstractModule() {
  override fun configure() {
    // Register mappers for authentication and authorization exceptions on admin paths
    install(ExceptionMapperModule.create<UnauthenticatedException, AdminDashboardUnauthenticatedExceptionMapper>())
    install(ExceptionMapperModule.create<UnauthorizedException, AdminDashboardUnauthorizedExceptionMapper>())
  }
}