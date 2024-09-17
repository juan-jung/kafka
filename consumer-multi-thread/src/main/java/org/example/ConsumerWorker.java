package org.example;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

public class ConsumerWorker implements Runnable {
    private Properties prop;
    private String topic;
    private String threadName;
    private KafkaConsumer<String, String> consumer;

    ConsumerWorker(Properties prop, String topic, int number) {
        this.prop = prop;
        this.topic = topic;
        this.threadName = "consumer-thread-" + number;
    }

    @Override
    public void run() {
        consumer = new KafkaConsumer<>(prop);
//        consumer.subscribe(Arrays.asList(topic));
        consumer.subscribe(Arrays.asList(topic), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                System.out.println(threadName + " 할당된 파티션: " + partitions);
            }

            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                System.out.println(threadName + " 회수된 파티션: " + partitions);
            }
        });

        System.out.println(threadName + "activated");
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println(threadName + " >> " + record.value());
                }
                consumer.commitSync();
            }
        } catch (WakeupException e) {
            System.out.println(threadName + " trigger WakeupException");
        } finally {
            consumer.commitSync();
            consumer.close();
        }
    }

    public void shutdown() {
        consumer.wakeup();
    }
}
