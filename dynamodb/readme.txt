[ Disclaimer: Here all command examples are taken from my local system, which includes exact screen shots. These command outputs might not match with your system. However, it conveys the idea. All input JSON required for the AWS CLI commands are uploaded in this folder.]

Prerequisite:
1. Configure your aws sso profile using aws configure sso command

2. List aws profile in your local system by using below commands

C:\>aws configure list-profiles

Output:
Goldboard-SSM-109711356331
default

3. Verify through your sso profile you are able to list S3 buckets defined against your account.

C:\>aws s3 ls --profile Goldboard-SSM-109711356331

Output:
2022-08-15 19:41:45 ansiblessm-hertz-dash
2024-01-10 01:37:18 audit-mef3-files
2022-12-15 20:47:32 cf-templates-z7lyjls6bakn-us-east-1
2023-10-11 03:06:24 dashboard-upload-archive.stripe.hertz.io
2023-10-11 03:05:41 dashboard-upload.stripe.hertz.io
2023-02-10 21:58:49 hanv-dash-0-s3-lambda-001
2023-02-10 22:00:10 hanv-dash-0-s3-staticwebsite-001
2023-08-09 09:29:31 protego-fsp-109711356331
2023-02-23 20:20:06 stripe-poc.dev.stripe.hertz.io


4. Executing create table script to persist customer data: .

C:\>aws dynamodb create-table --cli-input-json file://C:\goldboard\gbcustomer_createtable3.json --profile Goldboard-SSM-109711356331

Output:
{
    "TableDescription": {
        "AttributeDefinitions": [
            {
                "AttributeName": "id",
                "AttributeType": "S"
            },
            {
                "AttributeName": "locationCode",
                "AttributeType": "S"
            }
        ],
        "TableName": "GBCustomerData-dev",
        "KeySchema": [
            {
                "AttributeName": "id",
                "KeyType": "HASH"
            }
        ],
        "TableStatus": "CREATING",
        "CreationDateTime": "2024-02-07T16:20:35.425000+05:30",
        "ProvisionedThroughput": {
            "NumberOfDecreasesToday": 0,
            "ReadCapacityUnits": 5,
            "WriteCapacityUnits": 5
        },
        "TableSizeBytes": 0,
        "ItemCount": 0,
        "TableArn": "arn:aws:dynamodb:us-east-1:109711356331:table/GBCustomerData-dev",
        "TableId": "6cebaecb-355a-4af1-9324-c21b8e4be57b",
        "GlobalSecondaryIndexes": [
            {
                "IndexName": "LocationCode-index",
                "KeySchema": [
                    {
                        "AttributeName": "locationCode",
                        "KeyType": "HASH"
                    }
                ],
                "Projection": {
                    "ProjectionType": "ALL"
                },
                "IndexStatus": "CREATING",
                "ProvisionedThroughput": {
                    "NumberOfDecreasesToday": 0,
                    "ReadCapacityUnits": 5,
                    "WriteCapacityUnits": 5
                },
                "IndexSizeBytes": 0,
                "ItemCount": 0,
                "IndexArn": "arn:aws:dynamodb:us-east-1:109711356331:table/GBCustomerData-dev/index/LocationCode-index"
            }
        ]
    }
}

5. Insert multiple records in the above table. Output shows, all customer records are inserted from the JSON file.

C:\>aws dynamodb batch-write-item --request-items file://C:\goldboard\gbcustomer_putmultiitems.json --profile Goldboard-SSM-109711356331

Output:
{
    "UnprocessedItems": {}
}

6. List all customer records from the above table just inserted.

C:\>aws dynamodb scan --table-name GBCustomerData-dev --profile Goldboard-SSM-109711356331

Output:
{
    "Items": [
        {
            "stall": {
                "S": "GOLD"
            },
            "customerName": {
                "S": "Partha Banerjee"
            },
            "ra": {
                "S": "222222RA"
            },
            "oneClub": {
                "S": "222222OC"
            },
            "locationCode": {
                "S": "OKOKC11"
            },
            "arrivalTime": {
                "S": "22:22"
            },
            "createdDatetime": {
                "S": "2023-09-18T08:35:55Z"
            },
            "id": {
                "S": "OKOKC11#Partha Banerjee#222222OC"
            },
            "updatedDatetime": {
                "S": "2023-09-18T08:35:55Z"
            },
            "arrivalDate": {
                "S": "22/08/2023"
            }
        },
        {
            "stall": {
                "S": "CPRESS"
            },
            "customerName": {
                "S": "Subhasis Mitra"
            },
            "ra": {
                "S": "111111RA"
            },
            "oneClub": {
                "S": "111111OC"
            },
            "locationCode": {
                "S": "OKOKC11"
            },
            "arrivalTime": {
                "S": "07:10"
            },
            "createdDatetime": {
                "S": "2023-09-18T08:35:55Z"
            },
            "id": {
                "S": "OKOKC11#Subhasis Mitra#111111OC"
            },
            "updatedDatetime": {
                "S": "2023-09-18T08:35:55Z"
            },
            "arrivalDate": {
                "S": "02/08/2023"
            }
        },
        {
            "stall": {
                "S": "STALL"
            },
            "customerName": {
                "S": "Saptarshi Chak"
            },
            "ra": {
                "S": "333333RA"
            },
            "oneClub": {
                "S": "333333OC"
            },
            "locationCode": {
                "S": "CALAX15"
            },
            "arrivalTime": {
                "S": "22:33"
            },
            "createdDatetime": {
                "S": "2023-09-18T08:35:55Z"
            },
            "id": {
                "S": "CALAX15#Saparshi Chak#333333OC"
            },
            "updatedDatetime": {
                "S": "2023-09-18T08:35:55Z"
            },
            "arrivalDate": {
                "S": "23/08/2023"
            }
        }
    ],
    "Count": 3,
    "ScannedCount": 3,
    "ConsumedCapacity": null
}

7. Executing create table script to persist location data: .

C:\>aws dynamodb create-table --cli-input-json file://C:\goldboard\gblocations_createtable.json --profile Goldboard-SSM-109711356331

Output:
Similar as above.

8. Insert multiple records in the above location table. Output shows, all location records are inserted from the JSON file.

C:\>aws dynamodb batch-write-item --request-items file://C:\goldboard\gblocation_putmultiitems.json --profile Goldboard-SSM-109711356331

Output:
Similar as above.

9. List all location records from the above location table just inserted.

C:\>aws dynamodb scan --table-name GBLocationData-dev --profile Goldboard-SSM-109711356331

Output:
{
    "Items": [
        {
            "displayName": {
                "S": "Oklahoma City"
            },
            "hertzLocationCode": {
                "S": "OKOKC11"
            }
        },
        {
            "displayName": {
                "S": "Minneapolis "
            },
            "hertzLocationCode": {
                "S": "MNMIN10"
            }
        },
        {
            "displayName": {
                "S": "Heathrow"
            },
            "hertzLocationCode": {
                "S": "UKLHR50"
            }
        }
    ],
    "Count": 3,
    "ScannedCount": 3,
    "ConsumedCapacity": null
}

================================================================================================
10. (12-Sep-2024, Author: Subhasis): We have placed 3 batch scripts to load location reference data into location DynamoDB table. The script files are:
    a. gblocation_insert_batch1_v1.0.json
    b. gblocation_insert_batch2_v1.0.json
    c. gblocation_insert_batch3_v1.0.json
These batch scripts are tested successfully in local instance of DynamoDB table, namely GBLocationData-dev. As Engineering team, doesn't have write access to any AWS environments, namely, Dev, Stage and Production, we were unable to test these scripts in AWS env. 

To execute these batch scripts in Dev, just replace local table name with environment specific table name in line number 2 (in all the 3 script files). Reference data part of the scripts will remain untouched.
Example: 
For Dev, line number 2 will be: 
    "goldboard-locations-dev-122691834089-use1-data": [ instead of "GBLocationData-dev": [
For Stage, line number 2 will be: 
    "goldboard-locations-stage-783654729643-use1-data": [ instead of "GBLocationData-dev": [
For Production, line number 2 will be: 
    "goldboard-locations-prod-770666784407-use1-data": [ instead of "GBLocationData-dev": [
