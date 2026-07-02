import hudson.security.FullControlOnceLoggedInAuthorizationStrategy
import hudson.security.HudsonPrivateSecurityRealm
import jenkins.model.Jenkins

def instance = Jenkins.get()
def adminUser = System.getenv('JENKINS_ADMIN_ID') ?: 'admin'
def adminPassword = System.getenv('JENKINS_ADMIN_PASSWORD') ?: 'admin123'

def securityRealm = new HudsonPrivateSecurityRealm(false)
if (securityRealm.getUser(adminUser) == null) {
    securityRealm.createAccount(adminUser, adminPassword)
}
instance.setSecurityRealm(securityRealm)

def authorizationStrategy = new FullControlOnceLoggedInAuthorizationStrategy()
authorizationStrategy.setAllowAnonymousRead(false)
instance.setAuthorizationStrategy(authorizationStrategy)

instance.save()
