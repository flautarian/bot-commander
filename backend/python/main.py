from kafka_producer import produce_message
from kafka_consumer import KafkaConsumerDaemon
import argparse
import subprocess
import uuid

def handle_callback(task):
    """ Handle the callback logic based on the consumed message. """
    print(f'Received order {task}')
    if task['action'] == 'exec_script':
        print(f'Received script execution order, proceeding to execute it')
        # Execute the task
        script_value = task['parameters']['value']
        try:
            # Execute the script
            result = subprocess.run(script_value, shell=True, check=True, text=True, capture_output=True)
            print(f'Script output: {result.stdout}')
        except subprocess.CalledProcessError as e:
            print(f'Script execution failed: {e.stderr}')
            result = e.stderr
        # Produce a callback message
        callback_task = {
            'action': 'callback',
            'parameters': {
                'message': result.stdout
            }
        }
        produce_message(bootstrap_servers, 'callback', callback_task)

def main(bootstrap_servers, topic, group_id):
    # Create and start the consumer daemon
    consumer_daemon = KafkaConsumerDaemon(bootstrap_servers, topic, group_id, handle_callback)
    consumer_daemon.start()

    print(f"Kafka consumer opened successfully on topic {topic}")

    try:
        # Keep the main thread alive
        while True:
            pass
    except KeyboardInterrupt:
        print("Stopping consumer...")
        consumer_daemon.stop()
        consumer_daemon.join()

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Kafka Producer and Consumer')
    parser.add_argument('--bootstrap-servers', type=str, required=True, help='Kafka bootstrap servers address')
    parser.add_argument('--topic', type=str, required=True, help='Kafka topic')
    args = parser.parse_args()

    bootstrap_servers = args.bootstrap_servers
    topic = args.topic
    group_id = str(uuid.uuid4())
    main(bootstrap_servers, topic, group_id)
