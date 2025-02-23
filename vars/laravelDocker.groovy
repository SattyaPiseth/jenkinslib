def call(Map config = [:]) {
    // Default values
    def image = config.get('image', 'my-default-image')
    def registry = config.get('registry', 'my-default-registry')
    def tag = config.get('tag', 'latest')
    def containerPort = config.get('containerPort', '9000')
    def hostPort = config.get('hostPort', '8080')
    def projectName = config.get('projectName', 'my-default-projectName')

    pipeline {
        agent any

        stages {
            stage('Git Clone') {
                steps {
                    git branch: 'main', url: 'https://github.com/SattyaPiseth/laravel_first_project.git'
                }
            }

            stage('Prepare Dockerfile') {
                steps {
                    script {
                        def writeDockerfile = {
                            def dockerfileContent = readFile 'jenkinslib/resources/laravel.dockerfile'  // Read from repo
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
                                echo ${PASS} | docker login -u ${USER} --password-stdin
                                docker push ${registry}/${image}:${tag}
                            """
                        }
                    }
                }
            }

            stage('Run Docker Container') {
                steps {
                    script {
                        echo "Running Docker container..."

                        def availablePort = hostPort.toInteger()

                        // Loop until a free port is found
                        while (sh(script: "docker ps --format '{{.Ports}}' | grep -q ':${availablePort}->'", returnStatus: true) == 0) {
                            echo "Port ${availablePort} is in use. Trying next port..."
                            availablePort++
                        }

                        echo "Port ${availablePort} is available. Starting container..."

                        // Run the container mapping the available host port to the container port
                        sh """
                        docker rm -f ${image} || true
                        docker run -d --name ${image} -p ${availablePort}:${containerPort} ${registry}/${image}:${tag} || \
                        echo 'Failed to start container. Check logs for details.'
                        """

                        echo "Container running on port ${availablePort}."
                    }
                }
            }
        }
    }
}
