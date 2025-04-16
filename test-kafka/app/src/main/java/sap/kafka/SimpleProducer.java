package sap.kafka;

import java.util.Properties;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class SimpleProducer {
 
 public static void main(String[] args) throws Exception{
        
    /* config */

    Properties props = new Properties();   
    props.put("bootstrap.servers", "localhost:29092");	// server IP    
    props.put("acks", "all");							// acknowledgments for producer requests
    props.put("retries", 0);							// if the request fails, the producer can automatically retry,
    props.put("batch.size", 16384);						// batch buffer size 
    props.put("linger.ms", 1);							// Reduce the no of requests less than 0       .   
    props.put("buffer.memory", 33554432);				// total amount of memory available to the producer for buffering
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    
    /* create a producer */
    
    Producer<String, String> producer = new KafkaProducer<String, String>(props);
          
    /* produce some events on a topic */

    String topicName = "my-event-channel";

	System.out.println("Producing events to: " + topicName);

    for(int i = 0; i < 10; i++) {
       var ev = new ProducerRecord<String, String>(topicName, Integer.toString(i), Integer.toString(i));
	   producer.send(ev);
    }

    producer.close();
	
    System.out.println("done.");
 }
}

