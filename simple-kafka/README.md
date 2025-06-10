# Get Started with Apicurio Registry and Kafka in 5 mins

The entire process runs in docker so that you dont have to install anything on your host machine. 
Still if you want to install and run kafka on your host machine, the instructions are given below.  

**Prerequites:**
- Docker should be installed in your system

## Run Kafka Brokers with UI :

```
docker compose -f kafka-ui-docker-compose.yaml up -d
```

You'll be able to access the UI at - http://localhost:9090

- Create a cluster with any name
- Connect with kafka brokers at, **host:** broker, **port:** 29092
- Click on `Validate` and then `Submit`
- Wait for sometime, refresh, you'll see the cluster
- Go to `Topics` section and add a topic (No of partitions can be kept 1)
- You can now produce messages from UI
- Check the consumed messages from `Messages` tab

Alternatively you can consume and produce through interactive shell following this doc : https://developer.confluent.io/confluent-tutorials/kafka-on-docker/

Messages produced will be seen in the UI.

---
## Run [Apicurio registry](https://www.apicur.io/registry/docs/apicurio-registry/3.0.x/index.html) server and UI in docker

### Apicurio registry server
```
docker run -it -p 8080:8080 apicurio/apicurio-registry:3.0.6
```

Access API documentation at - http://localhost:8080/apis

At this point, you'll be able to create and fetch schemas from registry.

**Note:** Good to use Postman or any other API Platform for better visualisation

**Sample curl to create schema in registry:**
```
curl --location 'http://localhost:8080/apis/registry/v3/groups/my-group/artifacts' \
--header 'Content-type: application/json; artifactType=AVRO' \
--header 'X-Registry-ArtifactId: share-price' \
--data '{
    "artifactId": "mytopic-value-2",
    "artifactType": "AVRO",
    "name": "price",
    "firstVersion": {
        "version": "1.0.0",
        "content": {
            "content": "{\"type\":\"record\",\"name\":\"ExampleType\",\"fields\":[{\"name\":\"sdfgfsdgsdg\",\"type\":\"string\"}]}",
            "contentType": "application/json",
            "references": []
        },
        "name": "ExampleType",
        "description": "A simple example of an Avro type.",
        "labels": {}
    }
}'
```

**Sample curl to fetch the schema in registry:**
```
curl --location 'http://localhost:8080/apis/registry/v3/ids/globalIds/1'
```
<br/>

### Apicurio registry UI
```
docker run -it -p 8888:8080 apicurio/apicurio-registry-ui:3.0.6
```

Access UI at : http://localhost:8888/explore

Already one artifact will be there, created by last API call.

Try creating a new schema registry artifact through UI.<br/>
Give any names and values, if unsure about content, use the AVRO schema from this intro page - https://www.apicur.io/registry/docs/apicurio-registry/3.0.x/getting-started/assembly-intro-to-the-registry.html

Check Global ID from UI, and hit the GET endpoint. You'll get the schema that you just created from UI !

---

## Running Kafka-producer locally on host

This section is optional. Feel free to try it out to test the connection of the kafka broker running in docker from your host machine.

Install Kafka source from - https://kafka.apache.org/downloads

or

Direct zip download link - https://dlcdn.apache.org/kafka/3.9.0/kafka-3.9.0-src.tgz

Unzip it, from root dir. Run - `./gradlew jar -PscalaVersion=2.13.14`

It will install kafka on the system.

### Producing messages to broker running in docker

Go to **bin** directory

Run : `./kafka-console-producer.sh --bootstrap-server localhost:9092 --topic {same topic name}`

It will open a interactive shell where you can start producing messages!

---

## Updating Apicurio Registry thorugh Java program

- Get IntelliJ idea Ultimate/Community edition
- Open repo. It should automatically fetch dependencies.<br/> Otherwise run: `mvn package clean`
- After successful build, you can run the program from intellij editor.

In the 'RUN' console, you'll be able to follow the logs that it produced two messages and consumed them as well and their values will be printed.

Also refresh the kafka UI and schema registry UI to view the newly created topics, messages and artifacts !

---


