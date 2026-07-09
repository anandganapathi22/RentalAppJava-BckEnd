pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    parameters {
        choice(name: 'DEPLOY_ENV', choices: ['DEV', 'STAGE', 'PROD'], description: 'Choose the environment to deploy.')
        booleanParam(name: 'RUN_CODEQL', defaultValue: true, description: 'Run CodeQL security analysis before deployment.')
        booleanParam(name: 'UPLOAD_CODEQL_RESULTS', defaultValue: false, description: 'Upload CodeQL SARIF to GitHub code scanning. Requires GitHub code scanning access.')
        string(name: 'GITHUB_REPOSITORY', defaultValue: 'anandganapathi22/RentalAppJava-BckEnd', description: 'owner/repo for optional CodeQL SARIF upload.')
    }

    environment {
        APP_NAME = 'rental-applications'
        CODEQL_DB_DIR = '.codeql-db/java'
        CODEQL_RESULTS_DIR = 'target/codeql'
        CODEQL_SARIF = 'target/codeql/codeql-results.sarif'
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
        JDK25_HOME = '.tools/jdk-25'
        JDK25_URL = 'https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jdk/hotspot/normal/eclipse'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'chmod +x mvnw scripts/jenkins/*.sh'
            }
        }

        stage('Install JDK 25') {
            steps {
                sh '''
                    set -eux
                    if [ ! -x "$JDK25_HOME/bin/java" ]; then
                      rm -rf "$JDK25_HOME" .tools/jdk25.tar.gz .tools/jdk25-extract
                      mkdir -p .tools/jdk25-extract
                      curl -L -o .tools/jdk25.tar.gz "$JDK25_URL"
                      tar -xzf .tools/jdk25.tar.gz -C .tools/jdk25-extract --strip-components=1
                      mv .tools/jdk25-extract "$JDK25_HOME"
                      rm .tools/jdk25.tar.gz
                    fi
                    "$JDK25_HOME/bin/java" -version
                '''
            }
        }

        stage('Build and Test') {
            steps {
                sh '''
                    export JAVA_HOME="$PWD/$JDK25_HOME"
                    export PATH="$JAVA_HOME/bin:$PATH"
                    ./mvnw -B clean verify
                '''
                junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
            }
        }

        stage('CodeQL') {
            when {
                expression { return params.RUN_CODEQL }
            }
            steps {
                sh '''
                    set -eux
                    CODEQL_BIN="$(command -v codeql || true)"
                    if [ -z "$CODEQL_BIN" ]; then
                      CODEQL_BIN=".tools/codeql/codeql"
                      if [ ! -x "$CODEQL_BIN" ]; then
                        rm -rf .tools/codeql .tools/codeql.zip
                        mkdir -p .tools
                        curl -L -o .tools/codeql.zip https://github.com/github/codeql-cli-binaries/releases/latest/download/codeql-linux64.zip
                        unzip -q .tools/codeql.zip -d .tools
                        rm .tools/codeql.zip
                      fi
                    fi
                    "$CODEQL_BIN" version
                    "$CODEQL_BIN" pack download codeql/java-queries

                    rm -rf "$CODEQL_DB_DIR" "$CODEQL_RESULTS_DIR"
                    mkdir -p "$(dirname "$CODEQL_DB_DIR")" "$CODEQL_RESULTS_DIR"

                    "$CODEQL_BIN" database create "$CODEQL_DB_DIR" \
                      --language=java \
                      --source-root=. \
                      --command="export JAVA_HOME=$PWD/$JDK25_HOME && export PATH=$JAVA_HOME/bin:$PATH && ./mvnw -B -DskipTests clean package"

                    mkdir -p "$CODEQL_RESULTS_DIR"
                    "$CODEQL_BIN" database analyze "$CODEQL_DB_DIR" \
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
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    withCredentials([usernamePassword(credentialsId: 'github-rentalapp', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN')]) {
                        sh '''
                            set -eux
                            CODEQL_BIN="$(command -v codeql || true)"
                            if [ -z "$CODEQL_BIN" ]; then
                              CODEQL_BIN=".tools/codeql/codeql"
                            fi
                            "$CODEQL_BIN" github upload-results \
                              --repository="$GITHUB_REPOSITORY" \
                              --ref="refs/heads/${BRANCH_NAME:-main}" \
                              --commit="$GIT_COMMIT" \
                              --sarif="$CODEQL_SARIF"
                        '''
                    }
                    echo 'CodeQL SARIF upload completed.'
                }
            }
        }

        stage('Deploy Selected Environment') {
            steps {
                script {
                    def selectedEnvironment = (params.DEPLOY_ENV ?: 'DEV').toLowerCase()
                    sh "bash scripts/jenkins/deploy.sh ${selectedEnvironment} target/*.war"
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts allowEmptyArchive: true, artifacts: 'target/*.war,target/*.jar,target/codeql/*.sarif'
        }
    }
}
