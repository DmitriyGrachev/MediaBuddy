document.addEventListener("DOMContentLoaded", function () {
    const testForm = document.getElementById("testForm");
    const pressedButton = document.getElementById("fetchButton");
    const pressedButtonResult = document.getElementById("fetchButtonResult");
    const errorMessage = document.getElementById("errorMessage");
    const inputValue = document.getElementById("valueInput");
    const loading = document.getElementById("loading");
    const button = document.getElementById("fetchButton");

    if (testForm && pressedButton && pressedButtonResult && errorMessage && inputValue && loading && button) {
        testForm.addEventListener("submit", function (event) {
            event.preventDefault(); // Предотвращаем стандартную отправку формы
            fetchTestAsync();
        });

        async function fetchTestAsync() {
            loading.classList.remove("d-none");
            button.disabled = true;
            try {
                const response = await fetch("/api/test", {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        input: inputValue.value
                    })
                });
                if (response.status === 400) {
                    throw new Error("Input cannot be empty");
                } else if (response.status === 500) {
                    throw new Error("Server error");
                } else if (!response.ok) {
                    throw new Error(`HTTP error: ${response.status}`);
                }
                const message = await response.json();
                renderMessage(message);
                inputValue.value = ""; // Очистить поле
            } catch (error) {
                errorMessage.textContent = error.message.includes("Failed to fetch")
                    ? "Network error"
                    : `Ошибка: ${error.message}`;
                errorMessage.classList.remove("d-none");
                pressedButtonResult.innerHTML = "";
            } finally {
                loading.classList.add("d-none");
                button.disabled = false;
            }
        }

        function renderMessage(message) {
            const messageResult = `
                <div class="alert alert-success">
                    <p><strong>Message:</strong> ${message.message}</p>
                    <p><strong>Status:</strong> ${message.status}</p>
                </div>`;
            pressedButtonResult.innerHTML = messageResult;
            errorMessage.classList.add("d-none"); // Скрыть ошибку
        }
    }
});