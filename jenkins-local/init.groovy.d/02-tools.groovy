import hudson.model.JDK
import hudson.tools.InstallSourceProperty
import hudson.tools.ToolProperty
import hudson.tools.ToolPropertyDescriptor
import jenkins.model.Jenkins

def instance = Jenkins.get()
def jdkDescriptor = instance.getDescriptorByType(JDK.DescriptorImpl)

if (!jdkDescriptor.installations.find { it.name == 'jdk17' }) {
    def properties = [new InstallSourceProperty([])] as List<ToolProperty<? extends ToolPropertyDescriptor>>
    def jdk = new JDK('jdk17', System.getenv('JAVA_HOME') ?: '/opt/java/openjdk', properties)
    jdkDescriptor.setInstallations((jdkDescriptor.installations + jdk) as JDK[])
    jdkDescriptor.save()
}
