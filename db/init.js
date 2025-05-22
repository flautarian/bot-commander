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

print("Creating main user...");
// Create the user
db.createUser({
    user: 'botUser',
    pwd: 'botPassword',
    roles: [{ role: 'readWrite', db: 'botCommander' }]
});

print("Initialization done!");
