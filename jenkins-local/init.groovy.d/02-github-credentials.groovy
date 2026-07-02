import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl

def username = System.getenv('GITHUB_USERNAME')
def token = System.getenv('GITHUB_TOKEN')

if (username?.trim() && token?.trim()) {
    def store = SystemCredentialsProvider.getInstance().getStore()
    def existing = SystemCredentialsProvider.getInstance().getCredentials().find {
        it.id == 'github-rentalapp'
    }

    if (existing == null) {
        def credential = new UsernamePasswordCredentialsImpl(
            CredentialsScope.GLOBAL,
            'github-rentalapp',
            'GitHub credentials for RentalAppJava-BckEnd',
            username,
            token
        )
        store.addCredentials(Domain.global(), credential)
    }
}
