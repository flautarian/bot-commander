from confluent_kafka import Producer
import json

def delivery_report(err, msg):
    """ Called once for each message produced to indicate delivery result. """
    if err is not None:
        print(f'Message delivery failed: {err}')
    else:
        print(f'Message delivered successfully to {msg.topic()}')

def produce_message(bootstrap_servers, topic, task):
    """ Produce a message to the Kafka topic. """
    conf_producer = {
        'bootstrap.servers': bootstrap_servers
    }
    producer = Producer(**conf_producer)
    producer.produce(topic, json.dumps(task), callback=delivery_report)
    producer.flush()
