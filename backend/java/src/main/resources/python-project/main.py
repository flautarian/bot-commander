import platform
from kafka_producer import produce_message
from kafka_consumer import KafkaConsumerDaemon
import argparse
import subprocess
import uuid
import os 

def handle_callback(task, group_id):
    """ Handle the callback logic based on the consumed message. """
    print(f'Received order {task}')
    if task['actionType'] == 'exec_script' or task['actionType'] == 'os_exec_script':
        print(f'Received script execution order, proceeding to execute it')
        # Execute the task
        script_value = task['parameters']['value']
        try:
            # Execute the script
            if task['actionType'] == 'exec_script':
                # For exec_script, use subprocess.run
                result = subprocess.run(script_value, shell=True, check=True, text=True, capture_output=True)
                print(f'Script output: {result.stdout}')
            else:
                result = script_value.system(script_value)
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
            'groupId': group_id,
            'os': platform.platform()
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

def read_config_file(file_path):
    config = {}
    with open(file_path, 'r') as file:
        for line in file:
            line = line.strip()
            if line and not line.startswith('#'):  # Ignore comments and empty lines
                key, value = line.split('=', 1)
                config[key.strip()] = value.strip()
    return config

if __name__ == '__main__':
    # Read configuration from config.txt
    config = read_config_file('config.txt')
    
    if not config:
        print("Configuration file is empty or not found.")
        exit(1)

    bootstrap_servers = config.get('bootstrap_servers')
    topic = config.get('topic')

    main(bootstrap_servers, topic)
