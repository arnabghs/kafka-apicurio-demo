package io.apicurio.registry.examples.simple.avro;

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
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;

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
 */
public class SimpleAvroExample {
    private static final String REGISTRY_URL = "https://company-api-domain/schreg/compatibility/apis/ccompat/v6"; // apicurio-server
    private static final String SERVERS = "localhost:9092";
    private static final String TOPIC_NAME = "MY_CONFLUENT_TOPIC-3";
    private static final String KEY = "key1";
    private static final String SCHEMA = "{\"type\":\"record\",\"name\":\"Greeting\",\"fields\":[{\"name\":\"Message\",\"type\":\"string\"},{\"name\":\"Time\",\"type\":\"long\"}]}";


    public static final void main(String[] args) throws Exception {
        System.out.println("Starting example " + SimpleAvroExample.class.getSimpleName());

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
                record.put("Message", "Hello (" + producedMessages++ + ")!");
                record.put("Time", now.getTime());

                // Send/produce the message on the Kafka Producer
                ProducerRecord<Object, Object> producedRecord = new ProducerRecord<>(TOPIC_NAME, KEY, record);
                producer.send(producedRecord, (metadata, exception) ->
                {
                    if (exception == null) {
                        System.out.println("Sent record: " + producedRecord + metadata.partition() + " with offset " + metadata.offset());
                    } else {
                        System.err.println("Failed to send message: " + exception);
                    }
                });

            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
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
        System.out.println("Subscribing to topic " + TOPIC_NAME);
        consumer.subscribe(Collections.singletonList(TOPIC_NAME));

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
    private static Producer<Object, Object> createKafkaProducer() {
        Properties props = new Properties();

        // Configure kafka settings
        props.putIfAbsent(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SERVERS);
        props.putIfAbsent(ProducerConfig.CLIENT_ID_CONFIG, "Producer-" + TOPIC_NAME);
        props.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");

        // Because key is usually string or uuid, hence we use string serializer
        props.putIfAbsent(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // Use the Confluent provided Kafka Serializer for Avro
         props.putIfAbsent(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
         props.putIfAbsent(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, REGISTRY_URL);

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
        // Use the Confluent provided Kafka Deserializer for Avro
         props.putIfAbsent(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
         props.putIfAbsent(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, REGISTRY_URL);


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
        // No Security
        final String BEARER_TOKEN = "Generate token";


        // BEARER TOKEN configuration
        props.putIfAbsent(AbstractKafkaSchemaSerDeConfig.BEARER_AUTH_CREDENTIALS_SOURCE,  AbstractKafkaSchemaSerDeConfig.BEARER_AUTH_CREDENTIALS_SOURCE_DEFAULT);
        props.putIfAbsent(AbstractKafkaSchemaSerDeConfig.BEARER_AUTH_TOKEN_CONFIG,  BEARER_TOKEN);
    }
}
