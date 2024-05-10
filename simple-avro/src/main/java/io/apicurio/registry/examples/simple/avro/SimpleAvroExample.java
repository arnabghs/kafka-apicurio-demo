package io.apicurio.registry.examples.simple.avro;

import io.apicurio.registry.serde.SerdeConfig;
import io.apicurio.registry.serde.avro.AvroKafkaDeserializer;
import io.apicurio.registry.serde.avro.AvroKafkaSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;

/**
 * This example demonstrates how to use the Apicurio Registry in a very simple publish/subscribe
 * scenario with Avro as the serialization type.  The following aspects are demonstrated:
 *
 * <ol>
 *   <li>Configuring a Kafka Serializer for use with Apicurio Registry</li>
 *   <li>Configuring a Kafka Deserializer for use with Apicurio Registry</li>
 *   <li>Auto-register the Avro schema in the registry (registered by the producer)</li>
 *   <li>Data sent as a simple GenericRecord, no java beans needed</li>
 * </ol>
 * <p>
 * Pre-requisites:
 *
 * <ul>
 *   <li>Kafka must be running on localhost:29092</li>
 *   <li>Apicurio Registry must be running on localhost:8080</li>
 * </ul>
 *
 */
public class SimpleAvroExample {

    private static final String REGISTRY_URL = "http://localhost:8080"; // apicurio-server
    private static final String SERVERS = "localhost:9092";
    private static final String TOPIC_NAME = SimpleAvroExample.class.getSimpleName();
    private static final String SUBJECT_NAME = "Greeting";
    private static final String SCHEMA = "{\"type\":\"record\",\"name\":\"Greeting\",\"fields\":[{\"name\":\"Message\",\"type\":\"string\"},{\"name\":\"Time\",\"type\":\"long\"}]}";


    public static final void main(String[] args) throws Exception {
        System.out.println("Starting example " + SimpleAvroExample.class.getSimpleName());
        String topicName = TOPIC_NAME;
        String subjectName = SUBJECT_NAME;

        // Create the producer.
        Producer<Object, Object> producer = createKafkaProducer();
        // Produce 2 messages.
        int producedMessages = 0;
        try {
            Schema schema = new Schema.Parser().parse(SCHEMA);

            for (int idx = 0; idx < 2; idx++) {
                // Use the schema to create a record
                GenericRecord record = new GenericData.Record(schema);
                Date now = new Date();
                String message = "Hello (" + producedMessages++ + ")!";
                record.put("Message", message);
                record.put("Time",  now.getTime());

                // Send/produce the message on the Kafka Producer
                ProducerRecord<Object, Object> producedRecord = new ProducerRecord<>(topicName, subjectName, record);
                producer.send(producedRecord, (metadata, exception) ->
                {
                    if (exception == null) {
                        System.out.println("Sent record: " + producedRecord + metadata.partition() + " with offset " + metadata.offset());
                    } else {
                        System.err.println("Failed to send message: " + exception);
                        System.err.println("Failed to send message: " + exception);
                    }
                }); 

            }
        }
            catch(Exception e)  {
              e.printStackTrace();

            }
        finally {
            System.out.println("Closing the producer.");
            producer.flush();
            System.out.println("flushed the producer");
            producer.close();
            System.out.println("closed the producer");
        }

        // Create the consumer
        System.out.println("Creating the consumer.");
        KafkaConsumer<String, GenericRecord> consumer = createKafkaConsumer();

        // Subscribe to the topic
        System.out.println("Subscribing to topic " + topicName);
        consumer.subscribe(Collections.singletonList(topicName));

        // Consume the  messages.
        try {
            int messageCount = 0;
            System.out.println("Consuming messages.");
            while (messageCount < 2) {
                final ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofSeconds(1));
                messageCount += records.count();

                if (records.count() == 0) {
                    // Do nothing - no messages waiting.
                    System.out.println("No messages waiting...");
                } else records.forEach(record -> {
                    System.out.println("start consumption: " + records.count());
                    GenericRecord value = record.value();
                    System.out.println("Consumed a message: " + value.get("Message") + " @ " + new Date((long) value.get("Time")));
                });
            }
        } finally {
            consumer.close();
        }

        System.out.println("Done (success).");

    }

    /**
     * Creates the Kafka producer.
     */
    private static Producer<Object, Object> createKafkaProducer (){
        Properties props = new Properties();

        // Configure kafka settings
        props.putIfAbsent(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SERVERS);
        props.putIfAbsent(ProducerConfig.CLIENT_ID_CONFIG, "Producer-" + TOPIC_NAME);
        props.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
        props.putIfAbsent(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // Use the Apicurio Registry provided Kafka Serializer for Avro
        props.putIfAbsent(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroKafkaSerializer.class.getName());

        // Configure Service Registry location
        props.putIfAbsent(SerdeConfig.REGISTRY_URL, REGISTRY_URL);
        // Register the artifact if not found in the registry.
        props.putIfAbsent(SerdeConfig.AUTO_REGISTER_ARTIFACT, "true");
        props.putIfAbsent(SerdeConfig.ENABLE_HEADERS, "true");
        //props.putIfAbsent(SerdeConfig.EXPLICIT_ARTIFACT_ID, SUBJECT_NAME);

        //Just if security values are present, then we configure them.
        configureSecurityIfPresent(props);

        // Create the Kafka producer
        Producer<Object, Object> producer = new KafkaProducer<>(props);
        return producer;
    }

    /**
     * Creates the Kafka consumer.
     */
    private static KafkaConsumer<String, GenericRecord> createKafkaConsumer() {
        Properties props = new Properties();

        // Configure Kafka
        props.putIfAbsent(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SERVERS);
        props.putIfAbsent(ConsumerConfig.GROUP_ID_CONFIG, "Consumer-" + TOPIC_NAME);
        props.putIfAbsent(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.putIfAbsent(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.putIfAbsent(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        // Use the Apicurio Registry provided Kafka Deserializer for Avro
        props.putIfAbsent(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroKafkaDeserializer.class.getName());

        // Configure Service Registry location
        props.putIfAbsent(SerdeConfig.REGISTRY_URL, REGISTRY_URL);
        // No other configuration needed for the deserializer, because the globalId of the schema
        // the deserializer should use is sent as part of the payload.  So the deserializer simply
        // extracts that globalId and uses it to look up the Schema from the registry.

        //Just if security values are present, then we configure them.
        configureSecurityIfPresent(props);

        // Create the Kafka Consumer
        KafkaConsumer<String, GenericRecord> consumer = new KafkaConsumer<>(props);
        return consumer;
    }

    private static void configureSecurityIfPresent(Properties props) {
        System.out.println("----------Bypassing security----------");
//        final String tokenEndpoint = "XXXX";
//        if (tokenEndpoint != null) {
//
//            final String authUser = xxxx;
//            final String authPwd = xxxx;
//
//            props.putIfAbsent(SerdeConfig.AUTH_USERNAME, authUser);
//            props.putIfAbsent(SerdeConfig.AUTH_PASSWORD, authPwd);
//            /*props.putIfAbsent(SaslConfigs.SASL_MECHANISM, "OAUTHBEARER");
//            props.putIfAbsent(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, "io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler");
//            props.putIfAbsent("security.protocol", "SASL_SSL");
//            props.putIfAbsent(SerdeConfig.AUTH_TOKEN_ENDPOINT, tokenEndpoint);
//            props.putIfAbsent(SaslConfigs.SASL_JAAS_CONFIG, String.format("org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required " +
//                    "  oauth.client.id=\"%s\" "+
//                    "  oauth.client.secret=\"%s\" "+
//                    "  oauth.token.endpoint.uri=\"%s\" ;", authClient, authSecret, tokenEndpoint)); */
//        }
    }
}
