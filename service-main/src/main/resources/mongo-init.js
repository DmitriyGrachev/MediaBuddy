// Create the filmproject database and user
db = db.getSiblingDB('filmproject');
db.createUser({
    user: 'filmuser',
    pwd: 'filmpass',
    roles: [{ role: 'readWrite', db: 'filmproject' }]
});

// Insert a test document to create the database
db.test.insertOne({ message: 'Database initialized' });