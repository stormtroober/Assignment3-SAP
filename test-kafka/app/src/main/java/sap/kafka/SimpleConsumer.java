package sap.kafka;

import java.util.List;
import java.util.Properties;
import java.time.Duration;
import java.util.Arrays;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class SimpleConsumer {
   public static void main(String[] args) throws Exception {
      
	  /* config */

      Properties props = new Properties();
      props.put("bootstrap.servers", "localhost:29092");
      props.put("group.id", "test");
      props.put("enable.auto.commit", "true");
      props.put("auto.commit.interval.ms", "1000");
      props.put("session.timeout.ms", "30000");
      props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
      props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
      
      /* create a consumer */
      
      try (KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props)) {
		
    	  /* subscribe to a topic and wait for events */

    	  String topicName = "my-event-channel"; 

		  consumer.subscribe(List.of(topicName));
		  
		  System.out.println("Subscribed to topic: " + topicName);
		  
		  while (true) {
			  
			  /* blocking poll -- possibly with a timeout */
			  
			  ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(Long.MAX_VALUE));
			  
			  /* process(records); // application-specific processing */
			  
			  for (ConsumerRecord<String, String> record : records) {      
				  
				  /* print the offset,key and value for the consumer records */
				  
			      System.out.printf("offset = %d, key = %s, value = %s\n", 
			            record.offset(), record.key(), record.value());    	  
			  }
			  
			  consumer.commitSync();
			}
      }
   }
}