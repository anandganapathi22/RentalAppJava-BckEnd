pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    parameters {
        choice(name: 'PIPELINE_FLOW', choices: ['main', 'cd-promote', 'cd-deploy'], description: 'main builds, scans, and deploys dev. cd-promote advances dev->stage then stage->prod. cd-deploy deploys a selected environment.')
        choice(name: 'TARGET_ENV', choices: ['auto', 'dev', 'stage', 'prod'], description: 'Used by cd-deploy. Leave auto for main and cd-promote.')
        booleanParam(name: 'RUN_CODEQL', defaultValue: true, description: 'Run CodeQL security analysis during the main CI flow.')
        booleanParam(name: 'UPLOAD_CODEQL_RESULTS', defaultValue: false, description: 'Upload CodeQL SARIF to GitHub code scanning. Requires GitHub code scanning access.')
        string(name: 'GITHUB_REPOSITORY', defaultValue: 'anandganapathi22/RentalAppJava-BckEnd', description: 'owner/repo for optional CodeQL SARIF upload.')
    }

    environment {
        APP_NAME = 'rental-applications'
        CODEQL_DB_DIR = '.codeql-db/java'
        CODEQL_RESULTS_DIR = 'target/codeql'
        CODEQL_SARIF = 'target/codeql/codeql-results.sarif'
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
        PROMOTION_STATE_FILE = '.jenkins/promotion-state'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'chmod +x mvnw scripts/jenkins/*.sh'
            }
        }

        stage('main: Build and Test') {
            when {
                expression { return params.PIPELINE_FLOW == 'main' }
            }
            steps {
                sh './mvnw -B clean verify'
                junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
            }
        }

        stage('main: CodeQL') {
            when {
                allOf {
                    expression { return params.PIPELINE_FLOW == 'main' }
                    expression { return params.RUN_CODEQL }
                }
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
                      --command="./mvnw -B -DskipTests clean package"

                    mkdir -p "$CODEQL_RESULTS_DIR"
                    "$CODEQL_BIN" database analyze "$CODEQL_DB_DIR" \
                      codeql/java-queries:codeql-suites/java-security-extended.qls \
                      --format=sarifv2.1.0 \
                      --output="$CODEQL_SARIF" \
                      --sarif-category=java
                '''
            }
        }

        stage('main: Upload CodeQL Results') {
            when {
                allOf {
                    expression { return params.PIPELINE_FLOW == 'main' }
                    expression { return params.RUN_CODEQL }
                    expression { return params.UPLOAD_CODEQL_RESULTS }
                }
            }
            steps {
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
            }
        }

        stage('main: Deploy Dev') {
            when {
                expression { return params.PIPELINE_FLOW == 'main' }
            }
            steps {
                sh 'bash scripts/jenkins/deploy.sh dev target/*.war'
                sh 'mkdir -p .jenkins && printf "dev\\n" > "$PROMOTION_STATE_FILE"'
            }
        }

        stage('cd-promote: Promote') {
            when {
                expression { return params.PIPELINE_FLOW == 'cd-promote' }
            }
            steps {
                input message: 'Promote the current release to the next environment?', ok: 'Promote'
                sh 'bash scripts/jenkins/promote.sh "$PROMOTION_STATE_FILE" target/*.war'
            }
        }

        stage('cd-deploy: Direct Deploy') {
            when {
                expression { return params.PIPELINE_FLOW == 'cd-deploy' }
            }
            steps {
                script {
                    if (params.TARGET_ENV == 'auto') {
                        error('TARGET_ENV must be dev, stage, or prod for cd-deploy.')
                    }
                }
                input message: "Deploy directly to ${params.TARGET_ENV}?", ok: 'Deploy'
                sh 'bash scripts/jenkins/deploy.sh "$TARGET_ENV" target/*.war'
                sh 'mkdir -p .jenkins && printf "%s\\n" "$TARGET_ENV" > "$PROMOTION_STATE_FILE"'
            }
        }
    }

    post {
        always {
            archiveArtifacts allowEmptyArchive: true, artifacts: 'target/*.war,target/*.jar,target/codeql/*.sarif'
        }
    }
}
