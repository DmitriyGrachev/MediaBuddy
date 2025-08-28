// Обновленный JavaScript с поддержкой CSRF токена и обработкой ошибок аутентификации
document.addEventListener('DOMContentLoaded', () => {
    const chatToggleButton = document.getElementById('chat-toggle-button');
    const chatWindow = document.getElementById('chat-window');
    const chatCloseButton = document.getElementById('chat-close-button');
    const userInput = document.getElementById('user-input');
    const sendButton = document.getElementById('send-button');
    const chatMessagesContainer = document.getElementById('chat-messages');

    let chatHistory = [];

    // Попытка загрузить историю чата из sessionStorage
    try {
        const storedHistory = sessionStorage.getItem('chatUserHistory');
        if (storedHistory) {
            chatHistory = JSON.parse(storedHistory);
            chatHistory.forEach(msg => appendMessageToUI(msg.sender, msg.text, false));
        }
    } catch (e) {
        console.error("Ошибка загрузки истории чата:", e);
        sessionStorage.removeItem('chatUserHistory');
    }

    // Открыть/закрыть окно чата
    if (chatToggleButton) {
        chatToggleButton.addEventListener('click', () => {
            chatWindow.classList.remove('hidden');
            chatToggleButton.classList.add('hidden');
            userInput.focus();
        });
    }

    if (chatCloseButton) {
        chatCloseButton.addEventListener('click', () => {
            chatWindow.classList.add('hidden');
            chatToggleButton.classList.remove('hidden');
        });
    }

    // Отправка сообщения по клику
    if (sendButton) {
        sendButton.addEventListener('click', handleSendMessage);
    }

    // Отправка сообщения по нажатию Enter
    if (userInput) {
        userInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                handleSendMessage();
            }
        });
    }

    function handleSendMessage() {
        const message = userInput.value.trim();
        if (!message) return;

        appendMessageToUI('user', message);
        userInput.value = '';

        // Получаем CSRF токен, если он есть на странице
        let csrfToken = '';
        const csrfElement = document.querySelector('meta[name="_csrf"]');
        if (csrfElement) {
            csrfToken = csrfElement.getAttribute('content');
        }

        fetch('/api/chat/ask', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...(csrfToken && {'X-CSRF-TOKEN': csrfToken})
            },
            credentials: 'include', // Важно для передачи cookies с сессией
            body: JSON.stringify({ message: message })
        })
            .then(response => {
                if (!response.ok) {
                    if (response.status === 401 || response.status === 403) {
                        // Обработка ошибок авторизации
                        throw new Error('Ошибка авторизации. Пожалуйста, обновите страницу или войдите снова.');
                    } else {
                        // Другие ошибки
                        return response.text().then(text => {
                            throw new Error(`Ошибка сервера: ${response.status} ${text || ''}`);
                        });
                    }
                }
                return response.json();
            })
            .then(data => {
                if (data && data.reply) {
                    appendMessageToUI('assistant', data.reply);
                } else {
                    appendMessageToUI('assistant', 'Не удалось получить ответ от сервера.');
                }
            })
            .catch(error => {
                console.error('Ошибка при отправке сообщения:', error);
                appendMessageToUI('assistant', `Ошибка: ${error.message}. Попробуйте позже.`);
            });
    }

    function appendMessageToUI(sender, text, saveToHistory = true) {
        const messageDiv = document.createElement('div');
        messageDiv.classList.add('message', sender);
        messageDiv.textContent = text;

        chatMessagesContainer.appendChild(messageDiv);
        chatMessagesContainer.scrollTop = chatMessagesContainer.scrollHeight;

        if (saveToHistory) {
            chatHistory.push({ sender, text });
            try {
                sessionStorage.setItem('chatUserHistory', JSON.stringify(chatHistory));
            } catch (e) {
                console.error("Ошибка сохранения истории чата:", e);
            }
        }
    }

    // Начальное состояние
    if (chatWindow) chatWindow.classList.add('hidden');
    if (chatToggleButton) chatToggleButton.classList.remove('hidden');
});
