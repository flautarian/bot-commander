from confluent_kafka import Consumer
import json
import threading

class KafkaConsumerDaemon(threading.Thread):
    def __init__(self, bootstrap_servers, topic, groupId, callback):
        threading.Thread.__init__(self)
        self.daemon = True  # Set as a daemon thread
        self.bootstrap_servers = bootstrap_servers
        self.topic = topic
        self.groupId = groupId
        self.callback = callback
        self.running = True

    def run(self):
        conf_consumer = {
            'bootstrap.servers': self.bootstrap_servers,
            'group.id': self.groupId,
            'auto.offset.reset': 'latest',
            'enable.auto.commit': 'true',
            'enable.auto.offset.store': 'false'
        }
        consumer = Consumer(conf_consumer)
        print(f"Initializing kafka consumer with config {conf_consumer}")
        consumer.subscribe([self.topic])
        print(f"Kafka consumer subscribed to {self.topic} topic")

        try:
            while self.running:
                msg = consumer.poll(timeout=1.0)

                if msg is None:
                    continue
                if msg.error():
                    print(f'Consumer error: {msg.error()}')
                    continue

                # Process the message
                task = json.loads(msg.value().decode('utf-8'))

                # Execute the callback if needed
                if self.callback:
                    self.callback(task)

        except Exception as e:
            print(f'Exception in consumer: {e}')
        finally:
            # Close down consumer to commit final offsets.
            consumer.close()

    def stop(self):
        self.running = False
