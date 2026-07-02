# Rental Apps API

Spring Boot microservice that manages Gold member stall assignments at rental locations. Consumes customer rental events from IBM MQ (CWA), persists data to Amazon DynamoDB, publishes events to Amazon Kinesis, and exposes REST APIs for the Rental Apps UI and admin operations.

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
src/main/java/com/hertz/
Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ RentalAppsListenerApplication.java   # @SpringBootApplication, @EnableJms, @EnableScheduling
Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ config/                             # ApplicationConfig, AwsConfig, KinesisConfig, Security
Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ configmq/                           # IBM MQ ConnectionFactory (primary + secondary)
Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ consumer/                           # CwaQueueConsumer Ã¢â‚¬â€ JMS listener
Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ database/                           # DbService, DbRetentionService, DTOs, exceptions, Utils
Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ exception/                          # @ControllerAdvice global exception handlers
Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ model/                              # Rental, CustomerBean, CwaMessageBean, Event, EventType
Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ restcontroller/                     # RentalAppsController (REST), RentalAppsAdminController (UI)
Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ scheduler/                          # CustomerDeletionJobScheduler
Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ service/                            # CustomerDataService, EventService, EventServiceImpl
Ã¢â€â€Ã¢â€â‚¬Ã¢â€â‚¬ util/                               # Constants

dynamodb/                               # Table creation JSON + seed data batch scripts
devops/                                 # Jenkinsfile, CI/CD pipeline configs
```

## API Endpoints

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

## Local Setup

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

Or run `RentalAppsListenerApplication.main()` from your IDE.

## UI Integration

1. Build the React UI app: `npm run build`
2. Copy the `build/` output into `src/main/resources/static/` in this project.

The `RentalAppsAdminController` serves `/index.html` for `/`, `/admin`, and `/admin/{path}` routes.

## DynamoDB Setup

Scripts are in `dynamodb/`. See [`dynamodb/readme.txt`](dynamodb/readme.txt) for full instructions.

```bash
# Create tables
aws dynamodb create-table --cli-input-json file://dynamodb/gbcustomer_createtable.json --profile <profile>
aws dynamodb create-table --cli-input-json file://dynamodb/gblocations_createtable.json --profile <profile>

# Seed location data
aws dynamodb batch-write-item --request-items file://dynamodb/gblocation_insert_batch1_v1.0.json --profile <profile>
aws dynamodb batch-write-item --request-items file://dynamodb/gblocation_insert_batch2_v1.0.json --profile <profile>
aws dynamodb batch-write-item --request-items file://dynamodb/gblocation_insert_batch3_v1.0.json --profile <profile>
```

> **Note:** Replace the table name on line 2 of each batch JSON to match the target environment (e.g., `rentalapps-locations-stage-783654729643-use1-data`).

## Actuator Endpoints

Exposed at the default path:

| Endpoint | Purpose |
|---|---|
| `/actuator/health` | Health check |
| `/actuator/env` | Environment properties |
| `/actuator/info` | Build/git info |
