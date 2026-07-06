pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    parameters {
        booleanParam(name: 'RUN_CODEQL', defaultValue: true, description: 'Run CodeQL security analysis during CI.')
        booleanParam(name: 'UPLOAD_CODEQL_RESULTS', defaultValue: false, description: 'Upload CodeQL SARIF to GitHub code scanning. Requires GitHub code scanning access.')
        choice(name: 'DEPLOY_ENV', choices: ['none', 'dev', 'stage', 'prod'], description: 'Optional manual deployment target.')
        string(name: 'GITHUB_REPOSITORY', defaultValue: 'anandganapathi22/RentalAppJava-BckEnd', description: 'owner/repo for optional CodeQL SARIF upload.')
    }

    environment {
        APP_NAME = 'rental-applications'
        CODEQL_DB_DIR = '.codeql-db/java'
        CODEQL_RESULTS_DIR = 'target/codeql'
        CODEQL_SARIF = 'target/codeql/codeql-results.sarif'
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Precheck Build and Tests') {
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw -B clean verify'
            }
        }

        stage('CodeQL Analysis') {
            when {
                expression { return params.RUN_CODEQL }
            }
            steps {
                sh '''
                    set -eux
                    rm -rf "$CODEQL_DB_DIR" "$CODEQL_RESULTS_DIR"
                    mkdir -p "$CODEQL_RESULTS_DIR"

                    codeql database create "$CODEQL_DB_DIR" \
                      --language=java \
                      --source-root=. \
                      --command="./mvnw -B -DskipTests clean package"

                    codeql database analyze "$CODEQL_DB_DIR" \
                      codeql/java-queries:codeql-suites/java-security-extended.qls \
                      --format=sarifv2.1.0 \
                      --output="$CODEQL_SARIF" \
                      --sarif-category=java
                '''
            }
        }

        stage('Upload CodeQL Results') {
            when {
                allOf {
                    expression { return params.RUN_CODEQL }
                    expression { return params.UPLOAD_CODEQL_RESULTS }
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'github-rentalapp', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN')]) {
                    sh '''
                        set -eux
                        codeql github upload-results \
                          --repository="$GITHUB_REPOSITORY" \
                          --ref="refs/heads/${BRANCH_NAME:-main}" \
                          --commit="$GIT_COMMIT" \
                          --sarif="$CODEQL_SARIF"
                    '''
                }
            }
        }

        stage('Deploy Dev') {
            when {
                anyOf {
                    branch 'develop'
                    expression { return params.DEPLOY_ENV == 'dev' }
                }
            }
            steps {
                sh 'bash scripts/jenkins/deploy.sh dev target/*.war'
            }
        }

        stage('Deploy Stage') {
            when {
                anyOf {
                    branch 'stage'
                    expression { return params.DEPLOY_ENV == 'stage' }
                }
            }
            steps {
                input message: 'Deploy to stage?', ok: 'Deploy'
                sh 'bash scripts/jenkins/deploy.sh stage target/*.war'
            }
        }

        stage('Deploy Prod') {
            when {
                anyOf {
                    branch 'main'
                    expression { return params.DEPLOY_ENV == 'prod' }
                }
            }
            steps {
                input message: 'Deploy to production?', ok: 'Deploy'
                sh 'bash scripts/jenkins/deploy.sh prod target/*.war'
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
            archiveArtifacts allowEmptyArchive: true, artifacts: 'target/*.war,target/*.jar,target/codeql/*.sarif'
        }
    }
}
