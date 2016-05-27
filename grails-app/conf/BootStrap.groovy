import docker.registry.*
import docker.registry.acl.AccessLevel
import docker.registry.web.TrustAnySSL
import grails.plugin.springsecurity.web.access.intercept.InterceptUrlMapFilterInvocationDefinition
import grails.util.Environment

class BootStrap {
  def authService
  def grailsApplication

  def yamlConfig
  def filterInvocationInterceptor

  def init = { servletContext ->

    def yamlConfigProperties = Collections.list(yamlConfig.propertyNames()).collectEntries {
      key -> [key, yamlConfig.get(key)]
    }

    log.info "Yaml config: $yamlConfigProperties"

    def authEnabled = yamlConfig.get('registry.auth.enabled')
    log.info "auth enabled: ${authEnabled}"
    if (authEnabled) {
      filterInvocationInterceptor.rejectPublicInvocations = true
      def config = grailsApplication.config
      config.grails.plugin.springsecurity.interceptUrlMap = config.auth.InterceptUrlMap
      def filterDef = (InterceptUrlMapFilterInvocationDefinition) filterInvocationInterceptor.securityMetadataSource
      filterDef.reset()
    }

    def user
    //initializing auth if no roles or users exists
    if (!(Role.list() || User.list())) {
      user = new User(username: 'test', password: 'test').save(failOnError: true)
      def admin = new User(username: 'admin', password: 'admin').save(failOnError: true)
      def uiRole = new Role('UI_USER').save()
      def uiAdminRole = new Role('UI_ADMIN').save()
      def role = new Role('read-all').save(failOnError: true)
      def write = new Role('write-all').save(failOnError: true)

      def acl = new AccessControl(name: 'hello', ip: '', level: AccessLevel.PULL).save(failOnError: true)
      def writeAcl = new AccessControl(name: 'hello', ip: '', level: AccessLevel.PUSH).save(failOnError: true)
      UserRole.create(user, role, true)
      UserRole.create(user, uiRole, true)
      UserRole.create(admin, uiAdminRole, true)
      RoleAccess.create(role, acl)
      RoleAccess.create(write, writeAcl)

      //log.info authService.login("test", "testPassword")
    }
    if (Environment.current == Environment.DEVELOPMENT) {
      (1..100).each { i ->
        new Event(user: user, repo: 'some', ip: "$i.$i.$i.$i", action: 'pull', tag: 'latest', time: new Date()).save()
      }
    }

    if (yamlConfig.get('registry.trust_any_ssl')) {
      log.info "Trusting any SSL certificate"
      TrustAnySSL.init()
    }

    log.info grailsApplication.config.registry
  }
  def destroy = {
  }
}
