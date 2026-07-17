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
        return;
    }
    loadContacts(currentUserId);
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

/* ─────────────────────────────────────────────────────────────
   ساخت گروه جدید
───────────────────────────────────────────────────────────── */
function createNewGroup() {
    const currentUserId = localStorage.getItem("currentUserId");
    const groupNameInput = document.getElementById("group-name-input");
    const errorEl = document.getElementById("group-error-msg");
    if(!groupNameInput || !errorEl) {
        console.error("Group form elements not found");
        return;
    }
    errorEl.textContent = "";
    const groupName = groupNameInput.value.trim();

    if(!groupName) {
        showError("Please enter a group name.", errorEl);
        return;
    }
    if(groupName.length < 3 || groupName.length > 50) {
        showError("Group name should be between 3 and 50 characters.", errorEl);
        return;
    }

    fetch("http://localhost:8080/api/group/create", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            groupName: groupName,
            creatorId: currentUserId
        })
    })
    .then(response => response.json().then(data => ({ status: response.status, body: data })))
    .then(res => {
        if(res.status === 200 || res.status === 201) {
            const groupId = res.body.groupId;
            if(groupId) {
                window.location.href = "Chat.html?id=" + encodeURIComponent(groupId) + "&type=group";
            } else {
                showError("Unexpected response from server.", errorEl);
            }
        } else {
            showError(res.body.error || "Failed to create group. Status: " + res.status, errorEl);
        }
    })
    .catch(err => {
        console.error("Error creating group:", err);
        showError("Cannot connect to the server. Make sure the backend is running.", errorEl);
    });
}

/* ─────────────────────────────────────────────────────────────
   لیست مخاطبین
   نکته: تا وقتی اندپوینت GET /api/contacts نساختی، این تابع
   لیست خالی/خطا برمی‌گردونه و کاربر پیام "no contacts" می‌بینه.
───────────────────────────────────────────────────────────── */
function loadContacts(userId) {
    const container = document.getElementById("contacts-list");
    if(!container) return;
    fetch("http://localhost:8080/api/contacts?userId=" + encodeURIComponent(userId))
        .then(response => {
            if(!response.ok) throw new Error("Failed to load contacts");
            return response.json();
        })
        .then(contacts => {
            renderContacts(Array.isArray(contacts) ? contacts : []);
        })
        .catch(() => {
            container.innerHTML = "<p style='color:#888;text-align:center;padding:20px;font-size:13px;'>Could not load contacts.</p>";
        });
}
function renderContacts(contacts) {
    const container = document.getElementById("contacts-list");
    if(!container) return;
    container.innerHTML = "";
    if(contacts.length === 0) {
        const empty = document.createElement("p");
        empty.style.cssText = "color:#888;text-align:center;padding:20px;font-size:13px;";
        empty.textContent = "No contacts yet. Start a chat and add someone!";
        container.appendChild(empty);
        return;
    }
    contacts.forEach(function(contact) {
        const contactId = typeof contact === "string" ? contact : contact.userId;
        const item = document.createElement("div");
        item.className = "chat-item";
        item.innerHTML = `
            <div class="chat-avatar">${escapeHtml(contactId.charAt(0).toUpperCase())}</div>
            <div class="chat-info">
                <div class="chat-top">
                    <span class="chat-name">${escapeHtml(contactId)}</span>
                </div>
            </div>
        `;
        item.onclick = function(){ startChatWithContact(contactId); };
        container.appendChild(item);
    });
}
function startChatWithContact(contactId) {
    const currentUserId = localStorage.getItem("currentUserId");
    fetch("http://localhost:8080/api/chat/create", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            type: "private",
            userId1: currentUserId,
            userId2: contactId
        })
    })
    .then(response => response.json().then(data => ({ status: response.status, body: data })))
    .then(res => {
        if(res.status === 200 || res.status === 201) {
            const chatId = res.body.chatId;
            if(chatId) {
                window.location.href = "Chat.html?id=" + encodeURIComponent(chatId);
            }
        } else {
            alert(res.body.error || "Failed to start chat.");
        }
    })
    .catch(() => {
        alert("Cannot connect to the server.");
    });
}
function escapeHtml(text){
    if(!text) return "";
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
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
    const groupInput = document.getElementById("group-name-input");
    if(groupInput) {
        groupInput.addEventListener("keydown", function(event) {
            if(event.key === "Enter") {
                createNewGroup();
            }
        });
    }
});
