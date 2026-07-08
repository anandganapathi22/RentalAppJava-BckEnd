# Rental Apps API

Spring Boot microservice that manages Gold member stall assignments at rental locations. Consumes customer rental events from IBM MQ (CWA) and optionally Kafka, persists data to Amazon DynamoDB, publishes events to Amazon Kinesis, and exposes REST APIs for the Rental Apps UI and admin operations.

## Technology Stack

| Component | Version |
|---|---|
| Java | 17 |
| Spring Boot | 3.1.x (parent: msf-service-parent 3.37.0) |
| Maven | 3.x (wrapper included) |
| AWS SDK (DynamoDB, Kinesis, STS) | 1.12.787 |
| IBM MQ JMS | 3.1.0 |
| Jackson (XML + JSON) | 2.14.2 |
| Gson | 2.8.9 |
| Log4j2 | via Spring Boot starter |
| Lombok | via parent POM |

## Architecture

```
IBM MQ (CWA)                            React UI / Admin
  Ã¢â€â€š                                          Ã¢â€â€š
  Ã¢â€“Â¼                                          Ã¢â€“Â¼
CwaQueueConsumer Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€“Âº CustomerDataService Ã¢â€”â€žÃ¢â€â‚¬Ã¢â€â‚¬ RentalAppsController
                            Ã¢â€â€š
                Ã¢â€Å’Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€Â¼Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€Â
                Ã¢â€“Â¼           Ã¢â€“Â¼           Ã¢â€“Â¼
            DbService  DbRetention  EventServiceImpl
                Ã¢â€â€š       Service         Ã¢â€â€š
                Ã¢â€“Â¼                       Ã¢â€“Â¼
           DynamoDB                Kinesis Stream
     (Customer, Location,
       Shadow tables)
```

### Data Flow

1. **Inbound (MQ)** Ã¢â‚¬â€ CwaQueueConsumer listens on primary/secondary IBM MQ queues, filters messages by hex-encoded location correlation IDs, deserializes XML payloads into Rental objects, and delegates to CustomerDataService.
2. **Processing** Ã¢â‚¬â€ CustomerDataService formats customer names (US vs EU conventions), determines source system from RA pattern, and routes to DbService for add/update/delete.
3. **Persistence** Ã¢â‚¬â€ DbService performs DynamoDB CRUD on the customer table, logs operations to the shadow audit table (with TTL), and caches location timezones in memory.
4. **Outbound (Kinesis)** Ã¢â‚¬â€ EventServiceImpl publishes add/update/delete events as JSON to a Kinesis data stream.
5. **Scheduled Cleanup** Ã¢â‚¬â€ CustomerDeletionJobScheduler runs every 30 minutes, scans all customers, and deletes records older than the configured threshold for the configured region (US or EU).

## Project Structure

```
src/main/java/com/rentalapps/
  RentalAppsListenerApplication.java   # Spring Boot entry point
  config/                              # application, security, database, and scheduler config
  configmq/                            # IBM MQ connection factory setup
  consumer/                            # inbound MQ and Kafka consumers
  controller/                          # REST and UI controllers
  exception/                           # application exceptions and global handlers
  model/                               # domain/event models
  scheduler/                           # scheduled jobs
  service/                             # business and persistence services
  util/                                # constants and utility helpers
  vo/                                  # request, response, and value objects
```

## API Endpoints

## Architecture Console

The app serves a local architecture console at `/` and `/admin`. It models the Kafka-based target architecture with
local topic counters, event publishing, display projections, and audit history.

Key endpoints:

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/architecture/overview` | Topic counters, customer-state counts, and recent local events |
| POST | `/api/architecture/events` | Publish a US/EU rental event through the local architecture flow |
| GET | `/api/architecture/display/{region}` | Display/API projection for `US` or `EU`, optionally filtered by `locationId` |
| GET | `/api/architecture/audit?limit={n}` | Recent audit records from the audit table |

### Customer

| Method | Endpoint | Description |
|---|---|---|
| GET | `/StallDetails?locationId={code}` | Lite customer list (name + stall) for a location |
| GET | `/StallDetails2?locationId={code}` | Full customer details for a location |
| POST | `/AdhocChanges?locationId={code}` | Ad-hoc add/update/delete from admin UI |

### Location Admin

| Method | Endpoint | Description |
|---|---|---|
| POST | `/admin/locations` | Add a new location |
| PUT | `/admin/locations` | Update an existing location |
| DELETE | `/admin/locations` | Remove a location |
| GET | `/admin/locations/{id}` | Get a single location |
| GET | `/admin/locations/list/{ids}` | Get multiple locations (comma-separated) |

### Utilities

| Method | Endpoint | Description |
|---|---|---|
| DELETE | `/admin/dataretention?locationId={code}&timeInterval={minutes}` | Manual old-data deletion |
| GET | `/admin/locations/{id}/correlation-from-location` | Encode location code Ã¢â€ â€™ hex correlation ID |
| GET | `/admin/locations/{id}/location-from-correlation` | Decode hex correlation ID Ã¢â€ â€™ location code |
| GET | `/admin/time/{continent}/{city}/localtime-from-location` | Local time for a timezone |

## DynamoDB Tables

| Table Pattern | Partition Key | Sort Key | GSI | Purpose |
|---|---|---|---|---|
| `rentalapps-{env}-{account}-{region}-data` | `id` (locationCode#name#oneClub) | Ã¢â‚¬â€ | `LocationCode-index` | Active customer stall assignments |
| `rentalapps-locations-{env}-{account}-{region}-data` | `hertzLocationCode` | Ã¢â‚¬â€ | Ã¢â‚¬â€ | Location reference (display name, timezone) |
| `rentalapps-shadow-{env}-{account}-{region}-data` | `id` | `operationTime` | Ã¢â‚¬â€ | Audit log with TTL-based expiry |

## Configuration

Properties are supplied via environment-specific YAML from an external config repo and merged with the base `application.yml`.

### AWS & DynamoDB

```yaml
aws:
  region: us-east-1
  dynamo:
    table: rentalapps-dev-122691834089-use1-data
    locationTable: rentalapps-locations-dev-122691834089-use1-data
    shadowTable: rentalapps-shadow-dev-122691834089-use1-data
  kinesis:
    eventStream: <stream-name>
    eventTimeout: 100
```

### Business Rules

```yaml
gb:
  config:
    firstNameLengthCut: 2
    oldnessOfCustomerDataInMinutes: 1380
    enableDeletionScheduler: "true"
    deletionRegion: "EU"                  # US or EU
    enableQualTesting: "true"
    shadowTableEnable: "true"
    shadowTableExpiryMonths: 3
```

### IBM MQ

```yaml
goldSign:
  MQ:
    host:
      primary: AWSDCMQDHUB01.hertz.net
      secondary: AWSDCMQDHUB02.hertz.net
    port:
      primary: 2022
      secondary: 2023
    queue:
      manager:
        primary: AWSDCMQDHUB01
        secondary: AWSDCMQDHUB02
    channel:
      primary: DTHUB01CCGSV03
      secondary: DTHUB02CCGSV03
    request:
      queue:
        primary: DTHUB01IQGSV03
        secondary: DTHUB02IQGSV03
    reply:
      queue:
        primary: DTHUB01OQGSV03
        secondary: DTHUB02OQGSV03
    charset:
      primary: 819
      secondary: 819
    consumer:
      locationsFilter: >
        JMSCorrelationID='ID:<hex>' OR ...
```

The `locationsFilter` is a JMS message selector containing hex-encoded location codes. Each location's correlation ID is its code encoded as hex and right-padded with `20` (space) bytes. Use the `/admin/locations/{id}/correlation-from-location` endpoint to generate a correlation ID for a new location.

### Kafka

Kafka consumption is optional and can run alongside IBM MQ. Enable it with environment variables:

```powershell
$env:RENTAL_KAFKA_ENABLED="true"
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
$env:KAFKA_GROUP_ID="rental-applications"
$env:KAFKA_TOPIC_RENTAL_US="rental-events-us"
$env:KAFKA_TOPIC_RENTAL_EU="rental-events-eu"
$env:KAFKA_TOPIC_ADHOC="adhoc-goldboard-events"
```

Accepted payloads:

- JSON rental event with fields such as `action`, `locationCode`, `customerName`, `oneClub`, `ra`, `stall`, `arrivalDate`, and `arrivalTime`.
- JSON wrapper with `rental` as an object or array.
- CWA XML payload, with the location supplied by Kafka key or `locationCode` header when the XML does not include it.

Example JSON message:

```json
{
  "action": "add",
  "locationCode": "OKC11",
  "customerName": "Smith John",
  "oneClub": "OC123",
  "ra": "RA100",
  "stall": "A12",
  "arrivalDate": "07/02/2026",
  "arrivalTime": "10:30"
}
```

### RabbitMQ

RabbitMQ consumption is optional and is intended for local Docker testing. The RabbitMQ consumer accepts the same JSON
or XML payload shapes as the Kafka consumer and delegates to the existing queue processing flow, so consumed messages
are written to the configured database.

The root `docker-compose.yml` starts RabbitMQ with the management UI and enables RabbitMQ in the application container:

```bash
docker compose up --build
```

RabbitMQ endpoints:

| Endpoint | Value |
|---|---|
| AMQP | `localhost:5672` |
| Management UI | `http://localhost:15672` |
| Username/password | `guest` / `guest` |
| Exchange | `rental.events` |
| Queue | `rental.events.local` |
| Routing key | `rental.events` |

To run outside Docker, enable RabbitMQ with environment variables:

```powershell
$env:RENTAL_RABBITMQ_ENABLED="true"
$env:SPRING_RABBITMQ_HOST="localhost"
$env:SPRING_RABBITMQ_PORT="5672"
$env:SPRING_RABBITMQ_USERNAME="guest"
$env:SPRING_RABBITMQ_PASSWORD="guest"
```

Post a local event to RabbitMQ through the app:

```bash
curl -X POST http://localhost:8081/api/rabbitmq/events \
  -H "Content-Type: application/json" \
  -d '{
    "action": "add",
    "locationCode": "OKC11",
    "customerName": "Smith John",
    "oneClub": "OC123",
    "ra": "RA100",
    "stall": "A12",
    "arrivalDate": "07/02/2026",
    "arrivalTime": "10:30"
  }'
```

The RabbitMQ listener consumes from `rental.events.local` and persists the customer. Verify the DB write with:

```bash
curl "http://localhost:8081/StallDetails2?locationId=OKC11"
```

## Local Setup

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

Or run `RentalAppsListenerApplication.main()` from your IDE.

## Jenkins CI/CD

The repository includes a Jenkins pipeline in `Jenkinsfile`. Use **Build with Parameters** and choose `DEPLOY_ENV`
from `DEV`, `STAGE`, or `PROD`.

Each run does the following:

1. `./mvnw -B clean verify` as the pre-merge build and test gate.
2. CodeQL database creation and Java security analysis.
3. Optional CodeQL SARIF upload.
4. Deployment only to the selected `DEPLOY_ENV`.
5. Artifact archival for the WAR/JAR and `target/codeql/codeql-results.sarif`.

The local Jenkins image under `jenkins-local/` installs the CodeQL CLI and uses `Jenkinsfile` for the seeded
`RentalAppJava-BckEnd` job. Start it with:

```bash
cd jenkins-local
docker compose up --build
```

Pipeline parameters:

| Parameter | Purpose |
|---|---|
| `DEPLOY_ENV` | Deployment target: `DEV`, `STAGE`, or `PROD`. |
| `RUN_CODEQL` | Enables or disables CodeQL before deployment. Default: `true`. |
| `UPLOAD_CODEQL_RESULTS` | Uploads SARIF to GitHub code scanning when enabled. Requires a valid `github-rentalapp` credential and GitHub code scanning access. |
| `GITHUB_REPOSITORY` | Repository slug used for optional SARIF upload. |

The deploy hook is `scripts/jenkins/deploy.sh`. Replace the echo-only deployment placeholder with the real command for
your target platform, such as Tomcat, Kubernetes, ECS, or another release system.

## UI Integration

1. Build the React UI app: `npm run build`
2. Copy the `build/` output into `src/main/resources/static/` in this project.

The `RentalAppsAdminController` serves `/index.html` for `/`, `/admin`, and `/admin/{path}` routes.

## Actuator Endpoints

Exposed at the default path:

| Endpoint | Purpose |
|---|---|
| `/actuator/health` | Health check |
| `/actuator/env` | Environment properties |
| `/actuator/info` | Build/git info |
