var adminDB = db.getSiblingDB('admin');
print("Creating superuser...");

// Create the root user (superuser)
adminDB.createUser({
    user: 'superuser',
    pwd: 'superuser',
    roles: [{ role: 'root', db: 'admin' }]
});

// Authenticate as the root user
adminDB.auth('superuser', 'superuser');

print("Creating database...");
var db = db.getSiblingDB('botCommander');

print("Creating bot tasks collection...");
db.createCollection('bots');

print("Creating processes collection...");
db.createCollection('processes');

/* 
task_template example:
{id: "1",
    name: "Command_to_open_calculator_wait_3_seconds_and_then_close_it",
    description: "Open calculator, wait 3 seconds and then close it",
    actionType: "exec_script",
    parameters: [
        {
            key: "value",
            value: "calc.exe\n timeout 3\n taskkill /im calc.exe\n ",
        }
    ],
}
 */

print("Creating main user...");
// Create the user
db.createUser({
    user: 'botUser',
    pwd: 'botPassword',
    roles: [{ role: 'readWrite', db: 'botCommander' }]
});

print("Initialization done!");
