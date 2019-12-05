// This Jenkins build requires a configmap called jenkin-config with the following in it:
//
// password_qtxn=<cfms-postman-operator userid password>
// password_nonqtxn=<cfms-postman-non-operator userid password>
// client_secret=<keycloak client secret>
// zap_with_url=<zap command including dev url for analysis> 
// namespace=<openshift project namespace>
// url=<url of api>/api/v1/
// authurl=<Keycloak domain>
// clientid=<keycload Client ID>
// realm=<keycloak realm>

def WAIT_TIMEOUT = 10
def TAG_NAMES = ['dev', 'prod']
def BUILDS = ['drawio-build', 'drawio']
def DEP_ENV_NAMES = ['test', 'prod']
def label = "mypod-${UUID.randomUUID().toString()}"

String getNameSpace() {
    def NAMESPACE = sh (
        script: 'oc describe configmap jenkin-config | awk  -F  "=" \'/^namespace/{print $2}\'',
        returnStdout: true
    ).trim()
    return NAMESPACE
}

// Get an image's hash tag
String getImageTagHash(String imageName, String tag = "") {

  if(!tag?.trim()) {
    tag = "latest"
  }

  def istag = openshift.raw("get istag ${imageName}:${tag} -o template --template='{{.image.dockerImageReference}}'")
  return istag.out.tokenize('@')[1].trim()
}

podTemplate(
    label: label, 
    name: 'jenkins-python3nodejs', 
    serviceAccount: 'jenkins', 
    cloud: 'openshift', 
    containers: [
        containerTemplate(
            name: 'jnlp',
            image: '172.50.0.2:5000/openshift/jenkins-slave-python3nodejs',
            resourceRequestCpu: '1000m',
            resourceLimitCpu: '2000m',
            resourceRequestMemory: '2Gi',
            resourceLimitMemory: '4Gi',
            workingDir: '/tmp',
            command: '',
            args: '${computer.jnlpmac} ${computer.name}'
        )
    ]
){
    node(label) {      
    stage("Build Drawio-Build..") {
        script: {
            openshift.withCluster() {
                openshift.withProject() {

                    // Find all of the build configurations associated to the application using labels ...
                    def bc = openshift.selector("bc", "${BUILDS[0]}")
                    echo "Started builds: ${bc.names()}"
                    bc.startBuild("--wait").logs("-f")
                }
                echo "Build complete ..."
            }
        }
    }
    stage("Build Drawio..") {
        script: {
            openshift.withCluster() {
                openshift.withProject() {

                    // Find all of the build configurations associated to the application using labels ...
                    def bc = openshift.selector("bc", "${BUILDS[1]}")
                    echo "Started builds: ${bc.names()}"
                    bc.startBuild("--wait").logs("-f")
                }
                echo "Build complete ..."
            }
        }
    }

    stage("Deploy Dev") {
        script: {
            openshift.withCluster() {
                openshift.withProject() {
                    echo "Tagging ${BUILDS[1]} for deployment to ${TAG_NAMES[0]} ..."

                    // Don't tag with BUILD_ID so the pruner can do it's job; it won't delete tagged images.
                    // Tag the images for deployment based on the image's hash
                    def IMAGE_HASH = getImageTagHash("${BUILDS[1]}")
                    echo "IMAGE_HASH: ${IMAGE_HASH}"
                    openshift.tag("${BUILDS[1]}@${IMAGE_HASH}", "${BUILDS[1]}:${TAG_NAMES[0]}")
                }

                def NAME_SPACE = getNameSpace()
                openshift.withProject("${NAME_SPACE}-${DEP_ENV_NAMES[0]}") {
                    def dc = openshift.selector('dc', "${BUILDS[1]}")
                    // Wait for the deployment to complete.
                    // This will wait until the desired replicas are all available
                    dc.rollout().status()
                }
                echo "Deployment Complete."
            }
        }
    }
    }

    node {
    stage("Deploy Prod") {
        input "Deploy to Prod?"
        script: {
            openshift.withCluster() {
                openshift.withProject() {
                    echo "Tagging ${BUILDS[1]} for deployment to ${TAG_NAMES[1]} ..."

                    // Don't tag with BUILD_ID so the pruner can do it's job; it won't delete tagged images.
                    // Tag the images for deployment based on the image's hash
                    def IMAGE_HASH = getImageTagHash("${BUILDS[1]}")
                    echo "IMAGE_HASH: ${IMAGE_HASH}"
                    openshift.tag("${BUILDS[1]}@${IMAGE_HASH}", "${BUILDS[1]}:${TAG_NAMES[1]}")
                }

                def NAME_SPACE = getNameSpace()
                openshift.withProject("${NAME_SPACE}-${DEP_ENV_NAMES[1]}") {
                    def dc = openshift.selector('dc', "${BUILDS[1]}")
                    // Wait for the deployment to complete.
                    // This will wait until the desired replicas are all available
                    dc.rollout().status()
                }
                echo "Deployment Complete."
            }
        }
    }
    }
}