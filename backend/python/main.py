from kafka_producer import produce_message
from kafka_consumer import KafkaConsumerDaemon
import argparse
import subprocess
import uuid

def handle_callback(task, group_id):
    """ Handle the callback logic based on the consumed message. """
    print(f'Received order {task}')
    if task['actionType'] == 'exec_script':
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
            'actionType': 'callback',
            'parameters': {
                'groupId': group_id,
                'taskId': task['id']
            },
            'result': result.stdout
        }
        produce_message(bootstrap_servers, 'callback', callback_task)

def main(bootstrap_servers, topic):
    # Create and start the consumer daemon
    group_id = str(uuid.uuid4())
    consumer_daemon = KafkaConsumerDaemon(bootstrap_servers, topic, group_id, handle_callback)
    consumer_daemon.start()

    print(f"Kafka consumer opened successfully on topic {topic}")

    # Produce initial message with a unique group ID
    initial_task = {
        'id': '',
        'actionType': 'init',
        'parameters': {
            'groupId': group_id
        },
        'result': "success"
    }
    produce_message(bootstrap_servers, "init", initial_task)
    print(f"Initial message produced to topic {topic} with group ID {group_id}")

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
    main(bootstrap_servers, topic)
