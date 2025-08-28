db = db.getSiblingDB('filmproject');

// Создаем пользователя с правами на чтение и запись только для этой базы
db.createUser({
    user: "appuser",
    pwd: "appPass123",
    roles: [
        { role: "readWrite", db: "filmproject" }
    ]
});

// --- Дополнительные команды ---
// Безопасно удаляем индекс, только если он существует

// Проверяем, существует ли коллекция 'notifications'
if (db.getCollectionNames().includes('notifications')) {
    const indexes = db.notifications.getIndexes();
    const indexExists = indexes.some(index => index.name === "ttl-expire-notification");

    // Если индекс с таким именем найден, удаляем его
    if (indexExists) {
        print("Index 'ttl-expire-notification' found. Dropping it.");
        db.notifications.dropIndex("ttl-expire-notification");
    } else {
        print("Index 'ttl-expire-notification' not found. Skipping drop.");
    }
} else {
    print("Collection 'notifications' not found. Skipping index check.");
}