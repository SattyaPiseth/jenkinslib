def call(Map config = [:]) {
    // Default values
    def image = config.get('image', 'my-default-image')
    def registry = config.get('registry', 'my-default-registry')
    def tag = config.get('tag', 'latest')
    def containerPort = config.get('containerPort', '8080')
    def hostPort = config.get('hostPort', '8080')
    def projectName = config.get('projectName', 'angular-muyleang-ing')

    pipeline {
        agent any

        stages {
            stage('Git Clone') {
                steps {
                    // git branch: 'main', url: 'https://github.com/SattyaPiseth/nuxt-framework.git'
                    git branch: 'main', url: 'https://gitlab.com/ecommerce7750702/frontend.git'
                }
            }

            stage('Prepare Dockerfile') {
                steps {
                    script {
                        def writeDockerfile = {
                            def dockerfileContent = libraryResource 'nuxt.dockerfile'
                            writeFile file: 'Dockerfile', text: dockerfileContent
                        }
                        writeDockerfile()
                    }
                }
            }

            stage('Build Docker Image') {
                steps {
                    script {
                        echo "Building Docker image: ${registry}/${image}:${tag}"
                        sh """
                            docker build --build-arg PROJECT_NAME=${projectName} -t ${registry}/${image}:${tag} .
                        """
                    }
                }
            }

            stage('Docker Hub Login and Push') {
                steps {
                    script {
                        withCredentials([usernamePassword(credentialsId: 'docker-hub', passwordVariable: 'PASS', usernameVariable: 'USER')]) {
                            sh """
                                docker login -u ${USER} -p ${PASS}
                                docker push ${registry}/${image}:${tag}
                            """
                        }
                    }
                }
            }

            stage('Deploy Docker Container') {
                steps {
                    script {
                        echo "Deploying Docker container: ${registry}/${image}:${tag}"
                        sh """
                            docker rm -f ${image} || true
                            docker run -d -p ${hostPort}:${containerPort} --name ${image} ${registry}/${image}:${tag}
                        """
                    }
                }
            }
        }
    }
}
