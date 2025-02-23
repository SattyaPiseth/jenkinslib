pipeline {
    agent any

    environment {
        // Git repository containing your Laravel project
        GIT_REPO        = "https://github.com/SattyaPiseth/laravel_first_project.git"
        // Name for the Docker image
        IMAGE_NAME      = "sattypiseth/laravel_first_project"
        // Docker image tag
        DOCKER_TAG      = "latest"
        // Name of the Dockerfile in your repository
        DOCKERFILE      = "laravel.dockerfile"
        // Starting port for mapping (will increment if already in use)
        TARGET_PORT     = "8000"
        // Name for the container to run
        CONTAINER_NAME  = "laravel_app"
    }

    stages {
        stage('Checkout Jenkins Library') {
            steps {
                echo "Cloning Jenkins shared library repository..."
                // This stage checks out the shared library. Adjust as needed.
                git url: "https://github.com/SattyaPiseth/jenkinslib.git", branch: 'main'
            }
        }
        stage('Checkout Laravel Project') {
            steps {
                echo "Checking out repository: ${GIT_REPO} on branch 'main'..."
                // Explicitly checkout the 'main' branch from your Laravel repository.
                git branch: 'main', url: "${GIT_REPO}"
            }
        }
        stage('Build Docker Image') {
            steps {
                script {
                    echo "Building Docker image ${IMAGE_NAME}:${DOCKER_TAG} using ${DOCKERFILE}..."
                    dockerImage = docker.build("${IMAGE_NAME}:${DOCKER_TAG}", "-f ${DOCKERFILE} .")
                }
            }
        }
        stage('Test Docker Image') {
            steps {
                script {
                    echo "Running a test container to verify the image..."
                    // Start a temporary container for testing (detached)
                    def testContainer = dockerImage.run("-d")
                    
                    // Allow some time for the container to start
                    sleep time: 10, unit: "SECONDS"
                    
                    // Output logs for verification
                    sh "docker logs ${testContainer.id}"
                    
                    // Stop the test container after verification
                    testContainer.stop()
                }
            }
        }
        stage('Run Docker Container') {
            steps {
                script {
                    echo "Searching for an available host port starting at ${TARGET_PORT}..."
                    
                    def availablePort = TARGET_PORT.toInteger()
                    
                    // Loop until a free port is found on the host
                    while (sh(script: "docker ps --format '{{.Ports}}' | grep -q ':${availablePort}->'", returnStatus: true) == 0) {
                        echo "Port ${availablePort} is in use. Trying next port..."
                        availablePort++
                    }
                    
                    echo "Port ${availablePort} is available. Starting container mapping host:${availablePort} to container:80..."
                    
                    // Run the container mapping the available host port to port 80 in the container
                    sh """
                    docker run -d --name ${CONTAINER_NAME} -p ${availablePort}:80 ${IMAGE_NAME}:${DOCKER_TAG} || \
                    echo 'Failed to start container. Check logs for details.'
                    """
                    
                    echo "Container '${CONTAINER_NAME}' is running on host port ${availablePort}."
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline execution completed for ${IMAGE_NAME}"
        }
    }
}
