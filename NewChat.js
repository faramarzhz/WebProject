(function applyThemeOnLoad(){
    const darkSetting = localStorage.getItem("darkMode");
    if(darkSetting === "false"){
        document.body.classList.add("light-mode");
    }
    else{
        document.body.classList.remove("light-mode");
    }
})();
window.onload = function() {
    const currentUserId = localStorage.getItem("currentUserId");
    if(!currentUserId) {
        window.location.href = "Login.html";
    }
};
function goBack() {
    window.location.href = "Home.html";
}
function createNewChat() {
    const currentUserId = localStorage.getItem("currentUserId");
    const targetUserId = document.getElementById("target-userid-input").value.trim();
    const errorEl = document.getElementById("error-msg");
    if(!errorEl) {
        console.error("Error element not found");
        return;
    }
    errorEl.textContent = "";
    if(!targetUserId) {
        showError("Please enter a User ID.", errorEl);
        return;
    }
    if(targetUserId.toLowerCase() === currentUserId.toLowerCase()) {
        showError("You cannot start a chat with yourself.", errorEl);
        return;
    }
    if(targetUserId.length < 3 || targetUserId.length > 30) {
        showError("User ID should be between 3 and 30 characters.", errorEl);
        return;
    }
    fetch("http://localhost:8080/api/chat/create", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            type: "private",
            userId1: currentUserId,
            userId2: targetUserId
        })
    })
    .then(response => response.json().then(data => ({ status: response.status, body: data })))
    .then(res => {
        if(res.status === 200 || res.status === 201) {
            const chatId = res.body.chatId;
            if(chatId) {
                window.location.href = "Chat.html?id=" + encodeURIComponent(chatId);
            } else {
                showError("Unexpected response from server.", errorEl);
            }
        } else {
            showError(res.body.error || "Failed to create chat. Status: " + res.status, errorEl);
        }
    })
    .catch(err => {
        console.error("Error creating chat:", err);
        showError("Cannot connect to the server. Make sure the backend is running.", errorEl);
    });
}
function showError(message, element) {
    if(element) {
        element.textContent = message;
        element.style.color = "#ff4444";
    }
}
document.addEventListener("DOMContentLoaded", function() {
    const input = document.getElementById("target-userid-input");
    if(input) {
        input.addEventListener("keydown", function(event) {
            if(event.key === "Enter") {
                createNewChat();
            }
        });
    }
});
