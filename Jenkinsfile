@Library('global-shared-lib') _
pipeline {
    agent any
    environment {
        REPO_URL = "git@github.com:basmaoueslati/trainingTests.git"  
        BRANCH_NAME = "main"  // Update to your branch
    }

    stages {
        //Continuous Integration
        stage("Ckeck version of ansible") {
            steps{
                sh """
                ansible --version
                ansible-galaxy collection list | grep community.kubernetes
                which ansible
                """
        }
        }
        stage('Calculate Version') {
                    steps {
                        script {
                            // Read current version from POM
                            CURRENT_VERSION = sh(
                                script: 'mvn help:evaluate -Dexpression=revision -q -DforceStdout',
                                returnStdout: true
                            ).trim()
                            
                            // Parse and increment version
                            def parts = CURRENT_VERSION.split('\\.')
                            def newPatch = (parts[2] as Integer) + 1
                            NEXT_VERSION = "${parts[0]}.${parts[1]}.${newPatch}"
                            
                            echo "Updating version: ${CURRENT_VERSION} → ${NEXT_VERSION}"
        
                        }
                    }
                }
        // Set version in POM
        stage('Set Version') {
            steps {
                sh "mvn versions:set-property -Dproperty=revision -DnewVersion=${NEXT_VERSION}"
                sh "mvn versions:commit"
                
                sh """
                # Configure Git user
                git config --local user.email "basma.oueslati@gmail.com"
                git config --local user.name "Jenkins"
                
                # Add and commit changes
                git add pom.xml
                git commit -m "Bump version to ${NEXT_VERSION}"
                """
            }
        }
        stage('Push Changes') {
            steps {
                sshagent(['github-ssh-key']) {  
                    sh """
                    git pull origin ${BRANCH_NAME} || true
                    git push origin HEAD:${BRANCH_NAME}
                    """
                }
            }
        }
     
        //Continuous Integration
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests=true'
            }
            post {
                always {
                    sendStageNotification(
                        recipient: 'oueslatibasma2020@gmail.com',
                        stageName: 'Build'
                    )
                }
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    sendStageNotification(
                        recipient: 'oueslatibasma2020@gmail.com',
                        stageName: 'Test'
                    )
                }
            }
        }
        stage('Sonar Analysis') {
             steps {
                withSonarQubeEnv ("SonarQube"){
                   sh '''
                       mvn clean verify sonar:sonar \
                      -Dsonar.projectKey=compare-app \

                       '''
                    }
                timeout(time: 15, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                    }
                        }
            post {
                always {
                    sendStageNotification(
                        recipient: 'oueslatibasma2020@gmail.com',
                        stageName: 'Sonar Analysis'
                    )
                }
            }
        }
        //Continuous Delivery
        stage('Upload to nexus'){
            steps {
                nexusArtifactUploader artifacts: [
                    [
                        artifactId: 'numeric', classifier: '', 
                        file: "target/numeric-${NEXT_VERSION}.jar",  
                        type: 'jar'
                    ]
                ], 
                credentialsId: 'nexus', 
                groupId: 'com.devops', 
                nexusUrl: '35.180.71.148:8081', 
                nexusVersion: 'nexus3', 
                protocol: 'http', 
                repository: 'compare', 
                version: "${NEXT_VERSION}" 
            }
            post {
                always {
                    sendStageNotification(
                        recipient: 'oueslatibasma2020@gmail.com',
                        stageName: 'Upload to Nexus'
                    )
                }
            }
        }
        stage('Docker Build&Push via Ansible') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'docker',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )
                ]) {
                    script {
                        // Explicitly pass NEXT_VERSION as an extra variable to Ansible
                        sh """
                            echo "NEXT_VERSION=${NEXT_VERSION}"  # Debug: Check if version is set
                            ansible-playbook playbook-delivery.yml \
                                -e build_context=${WORKSPACE} \
                                -e NEXT_VERSION=${NEXT_VERSION}
                        """
                    }
                }
            }
           post {
                always {
                    sendStageNotification(
                        recipient: 'oueslatibasma2020@gmail.com',
                        stageName: 'Docker Build&Push via Ansible'
                    )
                }
            }
        }
        stage('Clean Old Docker Images on Local') {
            steps {
                echo '___Cleaning up unused Docker images___'
                sh 'docker image prune -f'

                sh '''
                docker images --filter=reference='basmaoueslati/compare-appf25*' --format '{{.ID}} {{.Repository}}:{{.Tag}}' \
                  | awk '{print $1}' \
                  | xargs -r docker rmi -f
                '''
            }
            post {
                always {
                    sendStageNotification(
                        recipient: 'oueslatibasma2020@gmail.com',
                        stageName: 'Clean Old Docker Images on Local'
                    )
                }
            }
        }
        //Continuous Deployment
        stage('Run Ansible Playbook') {
            steps {
                sh """
                    ansible-playbook -i inventory.ini playbook.yml -e NEXT_VERSION=${NEXT_VERSION}
                """
            }
            post {
                always {
                    sendStageNotification(
                        recipient: 'oueslatibasma2020@gmail.com',
                        stageName: 'Run Ansible Playbook'
                    )
                }
            }
}

    }
}

