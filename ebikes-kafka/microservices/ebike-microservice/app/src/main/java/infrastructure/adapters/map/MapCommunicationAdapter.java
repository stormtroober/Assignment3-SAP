package infrastructure.adapters.map;

import application.ports.MapCommunicationPort;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class MapCommunicationAdapter implements MapCommunicationPort {
    private Producer<String, String> producer;
    private final String topicName = "ebike-updates";

    public MapCommunicationAdapter() {
        // Kafka producer setup
        Properties props = new Properties();
        props.put("bootstrap.servers", "kafka:9092");
        props.put("acks", "all");
        props.put("retries", 5);  // Increase from 0
        props.put("reconnect.backoff.ms", 1000);
        props.put("reconnect.backoff.max.ms", 5000);
        props.put("retry.backoff.ms", 500);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        this.producer = new KafkaProducer<>(props);
    }

    @Override
    public void sendUpdate(JsonObject ebike) {
        System.out.println("Sending EBike update to Kafka topic: " + topicName);
        System.out.println(ebike.encodePrettily());
        producer.send(new ProducerRecord<>(topicName, ebike.getString("id"), ebike.encode()));
    }

    public void sendAllUpdates(JsonArray ebikes) {
        System.out.println("Sending all EBike updates to Kafka topic: " + topicName);
        System.out.println(ebikes.encodePrettily());
        for (int i = 0; i < ebikes.size(); i++) {
            JsonObject ebike = ebikes.getJsonObject(i);
            producer.send(new ProducerRecord<>(topicName, ebike.getString("id"), ebike.encode()));
        }
    }
}