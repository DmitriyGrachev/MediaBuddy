class WebSocketNotificationClient {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 3000;
        this.maxReconnectDelay = 30000;
        this.heartbeatInterval = null;
        this.token = sessionStorage.getItem('token');
        this.notifications = [];
        this.unreadCount = 0;

        // Bind methods
        this.connect = this.connect.bind(this);
        this.disconnect = this.disconnect.bind(this);
        this.onConnected = this.onConnected.bind(this);
        this.onError = this.onError.bind(this);
        this.onDisconnected = this.onDisconnected.bind(this);
        this.handleNotification = this.handleNotification.bind(this);

        this.initializeUI();
    }

    initializeUI() {
        // Clear all notifications button
        document.getElementById('clearAllNotifications').addEventListener('click', (e) => {
            e.preventDefault();
            this.clearAllNotifications();
        });

        // Mark notifications as read when dropdown is opened
        document.getElementById('notificationBellLink').addEventListener('click', () => {
            setTimeout(() => this.markAllAsRead(), 100);
        });
        this.attachEventListeners();
    }

    connect() {
        if (this.connected || !this.token) {
            console.log('CLIENT_JS: Already connected or no token available');
            return;
        }

        console.log(`CLIENT_JS: Attempting WebSocket connection. Attempt ${this.reconnectAttempts + 1}`);
        this.updateConnectionStatus('connecting');

        try {
            if (this.stompClient) {
                try {
                    this.stompClient.disconnect();
                } catch (e) {
                    console.warn('CLIENT_JS: Error cleaning up previous connection:', e);
                }
            }
            const socket = new SockJS(`/ws?token=${encodeURIComponent(this.token)}`);
            this.stompClient = Stomp.over(socket);

            this.stompClient.heartbeat.outgoing = 20000;
            this.stompClient.heartbeat.incoming = 20000;
            this.stompClient.reconnect_delay = 0;

            this.stompClient.debug = (str) => {
                if (str.includes('ERROR') || str.includes('CONNECTED') || str.includes('DISCONNECT')) {
                    console.log('CLIENT_STOMP_DEBUG:', str);
                }
            };

            this.stompClient.connect(
                {},
                (frame) => {
                    console.log('CLIENT_JS: Connection frame received:', frame);
                    this.onConnected(frame);
                },
                (error) => {
                    console.error('CLIENT_JS: Connection error:', error);
                    this.onError(error);
                }
            );

            socket.onclose = (event) => {
                console.log('CLIENT_JS: WebSocket closed:', event);
                this.onDisconnected();
            };

        } catch (error) {
            console.error('CLIENT_JS: Error creating WebSocket connection:', error);
            this.scheduleReconnect();
        }
    }

    onConnected(frame) {
        console.log('CLIENT_JS: WebSocket connected successfully');
        this.connected = true;
        this.reconnectAttempts = 0;
        this.reconnectDelay = 3000;

        try {
            const subscription = this.stompClient.subscribe(
                '/user/queue/notifications',
                (message) => {
                    this.handleNotification(message);
                }
            );
            console.log('CLIENT_JS: Successfully subscribed to notifications');
        } catch (error) {
            console.error('CLIENT_JS: Error subscribing to notifications:', error);
        }

        this.startHeartbeat();
        this.updateConnectionStatus('connected');
    }

    onError(error) {
        console.error('CLIENT_JS: WebSocket connection error:', error);
        this.connected = false;
        this.updateConnectionStatus('disconnected');

        if (this.stompClient) {
            try {
                this.stompClient.disconnect();
            } catch (disconnectError) {
                console.warn('CLIENT_JS: Error during disconnect:', disconnectError);
            }
            this.stompClient = null;
        }

        this.scheduleReconnect();
    }

    onDisconnected() {
        console.log('CLIENT_JS: WebSocket disconnected');
        this.connected = false;
        this.stopHeartbeat();
        this.updateConnectionStatus('disconnected');

        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.scheduleReconnect();
        }
    }

    scheduleReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('CLIENT_JS: Max reconnection attempts reached');
            this.showReconnectButton();
            return;
        }

        this.reconnectAttempts++;
        const delay = Math.min(this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1), this.maxReconnectDelay);

        console.log(`CLIENT_JS: Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`);

        setTimeout(() => {
            if (!this.connected) {
                this.connect();
            }
        }, delay);
    }

    startHeartbeat() {
        this.stopHeartbeat();
        this.heartbeatInterval = setInterval(() => {
            if (this.connected && this.stompClient) {
                try {
                    this.stompClient.send('/app/ping', {}, JSON.stringify({}));
                } catch (error) {
                    console.warn('CLIENT_JS: Failed to send heartbeat ping:', error);
                }
            }
        }, 25000);
    }

    stopHeartbeat() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval);
            this.heartbeatInterval = null;
        }
    }

    handleNotification(message) {
        try {
            console.log('CLIENT_JS: Raw message received:', message);

            let notification;
            if (typeof message.body === 'string') {
                notification = JSON.parse(message.body);
            } else {
                notification = message.body;
            }

            console.log('CLIENT_JS: Parsed notification:', notification);
            this.addNotification(notification);
        } catch (error) {
            console.error('CLIENT_JS: Error parsing notification:', error);
            console.error('CLIENT_JS: Message body was:', message.body);

            this.addNotification({
                message: message.body || 'New notification received',
                timestamp: Date.now(),
                type: 'info'
            });
        }
    }

    addNotification(notification) {
        const notificationObj = {
            id: Date.now() + Math.random(),
            message: notification.message || notification.content || 'New notification',
            timestamp: notification.timestamp || Date.now(),
            type: notification.type || 'info',
            read: false
        };

        this.notifications.unshift(notificationObj);
        this.unreadCount++;

        this.updateNotificationBadge();
        this.updateNotificationDropdown();
        this.showToastNotification(notificationObj);

        // Keep only last 50 notifications
        if (this.notifications.length > 50) {
            this.notifications = this.notifications.slice(0, 50);
        }
    }

    showToastNotification(notification) {
        const toastContainer = document.getElementById('toastContainer');

        const toastElement = document.createElement('div');
        toastElement.className = 'toast notification-toast';
        toastElement.setAttribute('role', 'alert');
        toastElement.setAttribute('aria-live', 'assertive');
        toastElement.setAttribute('aria-atomic', 'true');
        toastElement.setAttribute('data-bs-autohide', 'true');
        toastElement.setAttribute('data-bs-delay', '5000');

        const iconClass = this.getNotificationIcon(notification.type);

        toastElement.innerHTML = `
          <div class="toast-header">
            <i class="bi ${iconClass} me-2"></i>
            <strong class="me-auto">MediaBuddy</strong>
            <small>${this.formatTimestamp(notification.timestamp)}</small>
            <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
          </div>
          <div class="toast-body">
            ${notification.message}
          </div>
        `;

        toastContainer.appendChild(toastElement);

        const toast = new bootstrap.Toast(toastElement);
        toast.show();

        // Remove element after it's hidden
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    }

    updateNotificationBadge() {
        const badge = document.getElementById('notificationBadge');
        if (this.unreadCount > 0) {
            badge.textContent = this.unreadCount > 99 ? '99+' : this.unreadCount;
            badge.classList.remove('d-none');
        } else {
            badge.classList.add('d-none');
        }
    }

    updateNotificationDropdown() {
        const notificationList = document.getElementById('notificationList');
        const noNotifications = document.getElementById('noNotifications');

        // Clear existing notifications (except header and divider)
        const existingItems = notificationList.querySelectorAll('.notification-item');
        existingItems.forEach(item => item.remove());

        if (this.notifications.length === 0) {
            noNotifications.style.display = 'block';
        } else {
            noNotifications.style.display = 'none';

            this.notifications.forEach(notification => {
                const li = document.createElement('li');
                li.className = `notification-item ${notification.read ? 'notification-read' : 'notification-unread'}`;

                const iconClass = this.getNotificationIcon(notification.type);

                li.innerHTML = `
              <a class="dropdown-item" href="#" data-notification-id="${notification.id}">
                <div class="d-flex align-items-start">
                  <i class="bi ${iconClass} me-2 mt-1"></i>
                  <div class="flex-grow-1">
                    <div>${notification.message}</div>
                    <small class="notification-timestamp">${this.formatTimestamp(notification.timestamp)}</small>
                  </div>
                </div>
              </a>
            `;

                notificationList.appendChild(li);
            });
        }
    }

    getNotificationIcon(type) {
        switch (type) {
            case 'success': return 'bi-check-circle-fill text-success';
            case 'warning': return 'bi-exclamation-triangle-fill text-warning';
            case 'error': return 'bi-x-circle-fill text-danger';
            case 'info':
            default: return 'bi-info-circle-fill text-info';
        }
    }

    formatTimestamp(timestamp) {
        const date = new Date(timestamp);
        const now = new Date();
        const diff = now - date;

        if (diff < 60000) return 'Just now';
        if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
        if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`;
        return date.toLocaleDateString();
    }

    markAllAsRead() {
        this.notifications.forEach(notification => {
            notification.read = true;
        });
        this.unreadCount = 0;
        this.updateNotificationBadge();
        this.updateNotificationDropdown();
    }

    clearAllNotifications() {
        this.notifications = [];
        this.unreadCount = 0;
        this.updateNotificationBadge();
        this.updateNotificationDropdown();
    }

    updateConnectionStatus(status) {
        const statusElement = document.getElementById('connectionStatus');
        statusElement.classList.remove('d-none', 'status-connected', 'status-disconnected', 'status-connecting');

        switch (status) {
            case 'connected':
                statusElement.textContent = 'Connected';
                statusElement.classList.add('status-connected');
                setTimeout(() => statusElement.classList.add('d-none'), 3000);
                break;
            case 'disconnected':
                statusElement.textContent = 'Disconnected';
                statusElement.classList.add('status-disconnected');
                break;
            case 'connecting':
                statusElement.textContent = 'Connecting...';
                statusElement.classList.add('status-connecting');
                break;
        }
    }

    showReconnectButton() {
        const button = document.createElement('button');
        button.textContent = 'Reconnect WebSocket';
        button.className = 'btn btn-primary position-fixed';
        button.style.cssText = 'top: 10px; right: 10px; z-index: 10000;';
        button.onclick = () => {
            this.reconnectAttempts = 0;
            button.remove();
            this.connect();
        };
        document.body.appendChild(button);
    }

    disconnect() {
        console.log('CLIENT_JS: Manually disconnecting WebSocket');
        this.reconnectAttempts = this.maxReconnectAttempts;
        this.stopHeartbeat();

        if (this.stompClient && this.connected) {
            this.stompClient.disconnect(() => {
                console.log('CLIENT_JS: WebSocket disconnected successfully');
            });
        }

        this.connected = false;
        this.stompClient = null;
        this.updateConnectionStatus('disconnected');
    }

    updateToken(newToken) {
        this.token = newToken;
        sessionStorage.setItem('token', newToken);

        if (this.connected) {
            this.disconnect();
            setTimeout(() => this.connect(), 1000);
        }
    }
    attachEventListeners() {
        // Добавляем обработчик для закрытия соединения при уходе со страницы
        window.addEventListener('beforeunload', () => {
            if (this.stompClient && this.connected) {
                this.stompClient.disconnect();
            }
        });
    }
}

// Initialize WebSocket client
let notificationClient;

document.addEventListener('DOMContentLoaded', function() {
    console.log('CLIENT_JS: DOM loaded, initializing notifications system.');

    const token = sessionStorage.getItem('token');
    if (token) {
        console.log('CLIENT_JS: Token found, initializing WebSocket connection.');
        notificationClient = new WebSocketNotificationClient();
        notificationClient.connect();
    } else {
        console.log('CLIENT_JS: No token found, WebSocket not initialized.');
    }
});
async function logout(){
    sessionStorage.removeItem(token);
    window.location.href = "/";
}
window.addEventListener('beforeunload', () => {
    if (this.stompClient && this.isConnected) {
        this.stompClient.disconnect();
    }
});

document.addEventListener('visibilitychange', function() {
    if (notificationClient) {
        if (document.hidden) {
            console.log('CLIENT_JS: Page hidden, maintaining connection');
        } else {
            console.log('CLIENT_JS: Page visible');
            if (!notificationClient.connected && notificationClient.token) {
                notificationClient.connect();
            }
        }
    }
});

window.reconnectWebSocket = function() {
    if (notificationClient) {
        notificationClient.reconnectAttempts = 0;
        notificationClient.connect();
    }
};
