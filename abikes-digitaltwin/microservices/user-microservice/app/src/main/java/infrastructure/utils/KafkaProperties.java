package infrastructure.utils;

import infrastructure.config.ServiceConfiguration;
import io.vertx.core.json.JsonObject;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

public class KafkaProperties {
  private final String brokerAddress;

  public KafkaProperties(ServiceConfiguration config) {
    JsonObject kafkaConfig = config.getKafkaConfig();
    this.brokerAddress = kafkaConfig.getString("host") + ":" + kafkaConfig.getInteger("port");
  }

  public Properties getProducerProperties() {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerAddress);
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.RETRIES_CONFIG, 5);
    props.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, 1000);
    props.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 5000);
    props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 500);
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
    props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
    props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
    props.put(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringSerializer");

    props.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        "io.confluent.kafka.serializers.KafkaAvroSerializer");

    props.put("schema.registry.url", "http://schema-registry:8081");

    return props;
  }

  public Properties getAvroConsumerProperties() {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerAddress);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "user-group");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");

    props.put(
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringDeserializer");

    props.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        "io.confluent.kafka.serializers.KafkaAvroDeserializer");

    // Avro-specific config
    props.put("schema.registry.url", "http://schema-registry:8081");
    props.put("specific.avro.reader", true);

    return props;
  }
}
