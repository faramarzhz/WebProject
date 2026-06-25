(function applyThemeOnLoad(){
    const darkSetting = localStorage.getItem("darkMode");
    if(darkSetting === "false"){
        document.body.classList.add("light-mode");
    }
    else{
        document.body.classList.remove("light-mode");
    }
})();
const currentUserId = localStorage.getItem("currentUserId");
const urlParams = new URLSearchParams(window.location.search);
const chatId = urlParams.get("id");
let messages = [];
let sendTimes = [];
let pollingInterval = null;
let allMessages = [];
let isSearchMode = false;
let currentChatGlobal = null;
if(!currentUserId) {
    window.location.href = "Login.html";
}
loadMessages();
loadChatInfo();
// هر 3 ثانیه پیام‌های جدید رو بگیر (به جز موقع سرچ)
pollingInterval = setInterval(() => {
    if(!isSearchMode) loadMessages();
}, 3000);
async function loadChatInfo() {
    try {
        const response = await fetch("http://localhost:8080/api/chats?userId=" + encodeURIComponent(currentUserId));
        if(!response.ok) return;
        const chats = await response.json();
        const chat = chats.find(c => c.id === chatId);
        if(!chat) return;
        currentChatGlobal = chat;
        const savedChatId = localStorage.getItem("savedMessagesChatId");
        let chatName = "Chat";
        let avatarLetter = "C";
        let statusText = "";
        if(chat.id === savedChatId) {
            chatName = "Saved Messages";
            avatarLetter = "💾";
            statusText = "Your personal storage";
        } else if(chat.type === "private") {
            const otherUser = chat.participantIds?.find(id => id !== currentUserId) || "Unknown";
            chatName = otherUser;
            avatarLetter = otherUser.charAt(0).toUpperCase();
            statusText = "last seen recently";
        } else if(chat.type === "group") {
            chatName = chat.name || "Group Chat";
            avatarLetter = chatName.charAt(0).toUpperCase();
            statusText = chat.participantIds?.length + " members";
        }
        const chatNameEl = document.getElementById("chat-name");
        const chatStatusEl = document.getElementById("chat-status");
        const headerAvatarEl = document.getElementById("header-avatar");
        const infoNameEl = document.getElementById("info-name");
        const infoIdEl = document.getElementById("info-id");
        const infoAvatarEl = document.getElementById("info-avatar");
        if(chatNameEl) chatNameEl.textContent = chatName;
        if(chatStatusEl) chatStatusEl.textContent = statusText;
        if(headerAvatarEl) headerAvatarEl.textContent = avatarLetter;
        if(infoNameEl) infoNameEl.textContent = chatName;
        if(infoIdEl) infoIdEl.textContent = "#" + chatId.substring(0, 8);
        if(infoAvatarEl) infoAvatarEl.textContent = avatarLetter;
    } catch(error) {
        console.error("loadChatInfo error:", error);
    }
}
async function loadMessages() {
    try {
        const response = await fetch(`http://localhost:8080/api/chat/${chatId}/messages`);
        if(!response.ok) return;
        const data = await response.json();
        // فقط اگه پیام جدیدی اومده re-render کن
        if(JSON.stringify(data) !== JSON.stringify(messages)) {
            messages = data;
            allMessages = data;
            if(!isSearchMode) renderMessages(messages);
        }
    } catch(error) {
        console.error("loadMessages error:", error);
    }
}
function renderMessages(msgs) {
    const container = document.getElementById("messages-container");
    if(!container) return;
    const wasAtBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 50;
    container.innerHTML = "";
    if(!msgs || msgs.length === 0) {
        const emptyMsg = document.createElement("div");
        emptyMsg.style.cssText = "color:#888;text-align:center;padding:20px;";
        emptyMsg.textContent = isSearchMode ? "No messages found." : "No messages yet. Start the conversation!";
        container.appendChild(emptyMsg);
        return;
    }
    msgs.forEach(message => {
        if(!message || !message.id) return;
        const messageDiv = document.createElement("div");
        messageDiv.className = message.senderId === currentUserId ? "message my-message" : "message other-message";
        const contentDiv = document.createElement("div");
        contentDiv.className = "message-text";
        contentDiv.textContent = message.content || "(empty message)";
        const timeDiv = document.createElement("div");
        timeDiv.className = "message-time";
        timeDiv.textContent = formatTime(message.time);
        messageDiv.appendChild(contentDiv);
        messageDiv.appendChild(timeDiv);
        const options = document.createElement("div");
        options.className = "message-options";
        if(message.senderId === currentUserId) {
            const editBtn = document.createElement("button");
            editBtn.textContent = "✏";
            editBtn.title = "Edit";
            editBtn.onclick = () => alert("Edit feature coming in Phase 2");
            const deleteBtn = document.createElement("button");
            deleteBtn.textContent = "🗑";
            deleteBtn.title = "Delete";
            deleteBtn.onclick = () => alert("Delete feature coming in Phase 2");
            options.appendChild(editBtn);
            options.appendChild(deleteBtn);
        }
        const reportBtn = document.createElement("button");
        reportBtn.textContent = "🚩";
        reportBtn.title = "Report";
        reportBtn.onclick = () => reportMessage(message);
        options.appendChild(reportBtn);
        messageDiv.appendChild(options);
        container.appendChild(messageDiv);
    });
    // اسکرول به پایین فقط اگه قبلاً پایین بودیم
    if(wasAtBottom && !isSearchMode) {
        setTimeout(() => { container.scrollTop = container.scrollHeight; }, 0);
    }
}
async function reportMessage(message) {
    const reason = prompt("Why are you reporting this message?\n\n\"" + message.content + "\"");
    if(reason === null) return;
    if(reason.trim() === "") {
        alert("Please enter a reason.");
        return;
    }
    try {
        const res = await fetch(`http://localhost:8080/api/chat/${chatId}/report`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                reporterId: currentUserId,
                messageId: message.id,
                senderId: message.senderId,
                content: message.content,
                reason: reason.trim()
            })
        });
        if(res.ok) {
            alert("Message reported successfully.");
        } else {
            alert("Failed to report message.");
        }
    } catch(e) {
        alert("Error reporting message.");
    }
}
function toggleSearch() {
    const searchBar = document.getElementById("chat-search-bar");
    if(!searchBar) return;
    if(searchBar.style.display === "none" || searchBar.style.display === "") {
        searchBar.style.display = "flex";
        document.getElementById("chat-search-input").focus();
    } else {
        searchBar.style.display = "none";
        clearSearch();
    }
}
function searchMessages() {
    const query = document.getElementById("chat-search-input").value.toLowerCase().trim();
    if(query === "") {
        isSearchMode = false;
        renderMessages(allMessages);
        return;
    }
    isSearchMode = true;
    const filtered = allMessages.filter(m => m.content && m.content.toLowerCase().includes(query));
    renderMessages(filtered);
}
function clearSearch() {
    const input = document.getElementById("chat-search-input");
    if(input) input.value = "";
    isSearchMode = false;
    renderMessages(allMessages);
}
function formatTime(timestamp) {
    if(!timestamp) return "";
    try {
        const date = new Date(parseInt(timestamp));
        if(isNaN(date.getTime())) return "";
        const h = date.getHours().toString().padStart(2, "0");
        const m = date.getMinutes().toString().padStart(2, "0");
        return `${h}:${m}`;
    } catch(e) { return ""; }
}
async function sendMessage() {
    const input = document.getElementById("message-input");
    const content = input.value.trim();
    if(content === "") {
        alert("Message cannot be empty");
        return;
    }
    if(content.length > 1000) {
        alert("Message is too long (max 1000 characters)");
        return;
    }
    // rate limit: حداکثر 5 پیام در ثانیه
    const now = Date.now();
    sendTimes = sendTimes.filter(t => now - t < 1000);
    if(sendTimes.length >= 5) {
        alert("Maximum 5 messages per second. Please slow down.");
        return;
    }
    sendTimes.push(now);
    try {
        const res = await fetch(`http://localhost:8080/api/chat/${chatId}/send`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ senderId: currentUserId, content: content })
        });
        if(res.ok) {
            input.value = "";
            await loadMessages();
        } else {
            alert("Failed to send message. Status: " + res.status);
        }
    } catch(error) {
        alert("Error sending message: " + error.message);
    }
}
document.getElementById("message-input")?.addEventListener("keydown", function(event) {
    if(event.key === "Enter" && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
});
function goHome() {
    clearInterval(pollingInterval);
    window.location.href = "Home.html";
}
function openChatInfo() {
    const modal = document.getElementById("chat-info-modal");
    if(!modal || !currentChatGlobal) return;
    const btnContainer = modal.querySelector(".info-buttons");
    const membersSection = document.getElementById("group-members-section");
    const membersList = document.getElementById("members-list");
    if(!btnContainer) return;
    btnContainer.innerHTML = "";
    if(membersList) membersList.innerHTML = "";
    const savedChatId = localStorage.getItem("savedMessagesChatId");
    if(chatId === savedChatId) {
        if(membersSection) membersSection.style.display = "none";
        btnContainer.innerHTML = `
            <button onclick="toggleSearch(); closeChatInfo();">🔍 Search Messages</button>
            <button onclick="alert('Storage cleared!')">🧹 Clear Storage</button>
        `;
    } else if(currentChatGlobal.type === "private") {
        if(membersSection) membersSection.style.display = "none";
        const otherUser = currentChatGlobal.participantIds?.find(id => id !== currentUserId) || "Unknown";
        const infoNameEl = document.getElementById("info-name");
        const infoIdEl = document.getElementById("info-id");
        const infoAvatarEl = document.getElementById("info-avatar");
        if(infoNameEl) infoNameEl.textContent = otherUser;
        if(infoIdEl) infoIdEl.textContent = "@" + otherUser;
        if(infoAvatarEl) infoAvatarEl.textContent = otherUser.charAt(0).toUpperCase();
        btnContainer.innerHTML = `
            <button onclick="toggleSearch(); closeChatInfo();">🔍 Search Messages</button>
            <button onclick="addToContacts()">➕ Add to Contacts</button>
            <button onclick="toggleMute()">🔕 Mute Notifications</button>
            <button class="danger-button" onclick="blockUser()">🚫 Block User</button>
            <button onclick="archiveChat()">📦 Add to Archive</button>
        `;
    } else if(currentChatGlobal.type === "group") {
        if(membersSection) {
            membersSection.style.display = "block";
            if(currentChatGlobal.participantIds && membersList) {
                currentChatGlobal.participantIds.forEach(memberId => {
                    const li = document.createElement("li");
                    li.style.cssText = "padding: 4px 0; color: #ccc; font-size: 13px;";
                    li.textContent = `• ${memberId} ${memberId === currentUserId ? '(You)' : ''}`;
                    membersList.appendChild(li);
                });
            }
        }
        btnContainer.innerHTML = `
            <button onclick="toggleSearch(); closeChatInfo();">🔍 Search Messages</button>
            <button onclick="alert('Add member coming soon')">➕ Add Member</button>
            <button onclick="alert('Edit group coming soon')">✏️ Edit Group Info</button>
            <button onclick="archiveChat()">📦 Add to Archive</button>
            <button class="danger-button" onclick="alert('Leaving group...')">🚪 Leave Group</button>
        `;
    }
    modal.style.display = "flex";
}
function archiveChat() {
    let archivedIds = JSON.parse(localStorage.getItem("archivedChats") || "[]");
    if(!archivedIds.includes(chatId)) {
        archivedIds.push(chatId);
        localStorage.setItem("archivedChats", JSON.stringify(archivedIds));
    }
    alert("Chat added to archive.");
    closeChatInfo();
    window.location.href = "Home.html";
}
async function addToContacts() {
    if(!currentChatGlobal) return;
    const otherUser = currentChatGlobal.participantIds?.find(id => id !== currentUserId);
    if(!otherUser) return;
    try {
        const res = await fetch("http://localhost:8080/api/contact/add", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ userId: currentUserId, contactId: otherUser })
        });
        alert(res.ok ? "✓ User added to contacts!" : "Failed to add contact.");
    } catch(e) {
        alert("Error connecting to server.");
    }
    closeChatInfo();
}
function toggleMute() {
    if(!currentChatGlobal) return;
    const key = "muted_" + currentChatGlobal.id;
    const isMuted = localStorage.getItem(key) === "true";
    localStorage.setItem(key, String(!isMuted));
    alert(isMuted ? "🔔 Notifications unmuted." : "🔕 Notifications muted.");
    closeChatInfo();
}
function blockUser() {
    if(!currentChatGlobal) return;
    const otherUser = currentChatGlobal.participantIds?.find(id => id !== currentUserId);
    if(!otherUser) return;
    if(!confirm("Are you sure you want to block " + otherUser + "?")) return;
    let blocked = JSON.parse(localStorage.getItem("blockedUsers") || "[]");
    if(!blocked.includes(otherUser)) {
        blocked.push(otherUser);
        localStorage.setItem("blockedUsers", JSON.stringify(blocked));
    }
    alert("🚫 User " + otherUser + " has been blocked.");
    closeChatInfo();
}
function closeChatInfo() {
    const modal = document.getElementById("chat-info-modal");
    if(modal) modal.style.display = "none";
}
// کلیک خارج از مودال = بستنش
window.addEventListener("click", function(event) {
    const modal = document.getElementById("chat-info-modal");
    if(modal && event.target === modal) closeChatInfo();
});
