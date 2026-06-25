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
    const avatarEl = document.getElementById("user-avatar");
    if(avatarEl) avatarEl.textContent = currentUserId.charAt(0).toUpperCase();
    // چت "Saved Messages" رو از سرور بگیر و ذخیره کن
    fetch("http://localhost:8080/api/chat/saved?userId=" + encodeURIComponent(currentUserId))
        .then(r => r.json())
        .then(data => {
            if(data.chatId) {
                localStorage.setItem("savedMessagesChatId", data.chatId);
            }
        })
        .catch(() => {});
    loadUserChats(currentUserId);
    setInterval(function() {
        const uid = localStorage.getItem("currentUserId");
        if(uid) loadUserChats(uid);
    }, 3000);
};
let allChats = [];
function loadUserChats(userId) {
    fetch("http://localhost:8080/api/chats?userId=" + encodeURIComponent(userId))
    .then(response => {
        if(response.status === 200) return response.json();
        throw new Error("Failed to load chats");
    })
    .then(chats => {
        const newChats = Array.isArray(chats) ? chats : [];
        // فقط وقتی چت‌ها تغییر کردن re-render کن
        if(JSON.stringify(newChats) !== JSON.stringify(allChats)) {
            allChats = newChats;
            renderChats(allChats);
        }
    })
    .catch(() => {
        const container = document.getElementById("chat-list");
        if(container) container.innerHTML = "<p style='color:#ff4444;text-align:center;padding:20px;'>Could not load chats.</p>";
    });
}
function renderChats(chats) {
    const container = document.getElementById("chat-list");
    if(!container) return;
    container.innerHTML = "";
    const currentUserId = localStorage.getItem("currentUserId");
    const archivedIds = JSON.parse(localStorage.getItem("archivedChats") || "[]");
    const savedChatId = localStorage.getItem("savedMessagesChatId");
    const pinnedIds = JSON.parse(localStorage.getItem("pinnedChats") || "[]");
    const activeChats = chats.filter(function(c){ return c && c.id && !archivedIds.includes(c.id); });
    activeChats.sort(function(a, b){
        const timeA = a.lastMessageTime ? parseInt(a.lastMessageTime) : 0;
        const timeB = b.lastMessageTime ? parseInt(b.lastMessageTime) : 0;
        return timeB - timeA;
    });
    // pinned اول، بقیه بعد
    const pinnedChats   = activeChats.filter(function(c){ return pinnedIds.includes(c.id); });
    const unpinnedChats = activeChats.filter(function(c){ return !pinnedIds.includes(c.id); });
    const sortedChats   = pinnedChats.concat(unpinnedChats);
    if(sortedChats.length === 0 && chats.length === 0) {
        const empty = document.createElement("p");
        empty.style.cssText = "color:#888;text-align:center;padding:20px;font-size:13px;";
        empty.textContent = "No chats yet. Start a new conversation!";
        container.appendChild(empty);
    }
    sortedChats.forEach(chat => renderChatItem(container, chat, currentUserId, archivedIds, false));
    const archivedChats = chats.filter(c => c && c.id && archivedIds.includes(c.id));
    archivedChats.sort(function(a, b){
        const timeA = a.lastMessageTime ? parseInt(a.lastMessageTime) : 0;
        const timeB = b.lastMessageTime ? parseInt(b.lastMessageTime) : 0;
        return timeB - timeA;
    });
    if(archivedChats.length > 0) {
        const archiveHeader = document.createElement("div");
        archiveHeader.style.cssText = "color:#00cc55;font-size:12px;padding:10px 15px;cursor:pointer;border-top:1px solid #333;margin-top:10px;";
        archiveHeader.textContent = "📦 Archived Chats (" + archivedChats.length + ")";
        archiveHeader.onclick = () => {
            const archiveList = document.getElementById("archive-list");
            if(archiveList) archiveList.style.display = archiveList.style.display === "none" ? "block" : "none";
        };
        container.appendChild(archiveHeader);
        const archiveList = document.createElement("div");
        archiveList.id = "archive-list";
        archiveList.style.display = "none";
        archivedChats.forEach(chat => renderChatItem(archiveList, chat, currentUserId, archivedIds, true));
        container.appendChild(archiveList);
    }
}
function renderChatItem(container, chat, currentUserId, archivedIds, isArchived) {
    const savedChatId = localStorage.getItem("savedMessagesChatId");
    let chatName = "Unknown Chat";
    let avatarLetter = "?";
    let avatarBg = "#00cc55";
    if(chat.id === savedChatId) {
        chatName = "Saved Messages";
        avatarLetter = "💾";
        avatarBg = "#7c5cbf";
    } else if(chat.type === "private") {
        const otherUser = chat.participantIds?.find(id => id !== currentUserId) || currentUserId;
        chatName = otherUser;
        avatarLetter = chatName.charAt(0).toUpperCase();
    } else if(chat.type === "group") {
        chatName = chat.name || "Group Chat";
        avatarLetter = chatName.charAt(0).toUpperCase();
        avatarBg = "#9b59b6";
    }
    const lastSeen = parseInt(localStorage.getItem("lastSeen_" + chat.id) || "0");
    const totalMessages = chat.totalMessages || 0;
    const unreadCount = Math.max(0, totalMessages - lastSeen);
    const badge = unreadCount > 0 ? `<span class="badge">${unreadCount}</span>` : "";
    const pinnedIds = JSON.parse(localStorage.getItem("pinnedChats") || "[]");
    const isPinned = pinnedIds.includes(chat.id);
    const item = document.createElement("div");
    item.className = "chat-item";
    item.style.position = "relative";
    if(isPinned) item.style.borderLeft = "3px solid #00cc55";
    const btnContainer = document.createElement("div");
    btnContainer.style.cssText = "position:absolute;right:8px;top:50%;transform:translateY(-50%);display:flex;gap:4px;z-index:10;";
    const pinBtn = document.createElement("button");
    pinBtn.style.cssText = "width:auto;padding:3px 7px;font-size:11px;background:#333;color:#aaa;border-radius:4px;cursor:pointer;border:none;";
    pinBtn.textContent = isPinned ? "📌" : "📍";
    pinBtn.title = isPinned ? "Unpin" : "Pin";
    pinBtn.onclick = (e) => {
        e.stopPropagation();
        togglePin(chat.id, isPinned);
    };
    const archiveBtn = document.createElement("button");
    archiveBtn.style.cssText = "width:auto;padding:3px 7px;font-size:11px;background:#333;color:#aaa;border-radius:4px;cursor:pointer;border:none;";
    archiveBtn.textContent = isArchived ? "📤" : "📦";
    archiveBtn.title = isArchived ? "Unarchive" : "Archive";
    archiveBtn.onclick = (e) => {
        e.stopPropagation();
        toggleArchive(chat.id, isArchived);
    };
    btnContainer.appendChild(pinBtn);
    btnContainer.appendChild(archiveBtn);
    item.innerHTML = `
        <div class="chat-avatar" style="background-color:${avatarBg}; display:flex; justify-content:center; align-items:center;">${escapeHtml(avatarLetter)}</div>
        <div class="chat-info" style="padding-right:75px;">
            <div class="chat-top">
                <span class="chat-name">${isPinned ? "📌 " : ""}${escapeHtml(chatName)}</span>
                <span class="chat-time">${formatTime(chat.lastMessageTime)}</span>
            </div>
            <div class="chat-bottom">
                <span class="chat-preview">${escapeHtml(chat.lastMessageContent || "No messages yet")}</span>
                ${badge}
            </div>
        </div>
    `;
    item.onclick = () => openChat(chat.id);
    item.appendChild(btnContainer);
    container.appendChild(item);
}
function togglePin(chatId, isPinned) {
    let pinnedIds = JSON.parse(localStorage.getItem("pinnedChats") || "[]");
    if(isPinned) {
        pinnedIds = pinnedIds.filter(id => id !== chatId);
    } else {
        pinnedIds.push(chatId);
    }
    localStorage.setItem("pinnedChats", JSON.stringify(pinnedIds));
    renderChats(allChats);
}
function toggleArchive(chatId, isArchived) {
    let archivedIds = JSON.parse(localStorage.getItem("archivedChats") || "[]");
    if(isArchived) {
        archivedIds = archivedIds.filter(id => id !== chatId);
    } else {
        archivedIds.push(chatId);
    }
    localStorage.setItem("archivedChats", JSON.stringify(archivedIds));
    renderChats(allChats);
}
function formatTime(timestamp) {
    if(!timestamp) return "";
    try {
        const date = new Date(parseInt(timestamp));
        if(isNaN(date.getTime())) return "";
        const today = new Date();
        const yesterday = new Date(today);
        yesterday.setDate(yesterday.getDate() - 1);
        if(date.toDateString() === today.toDateString()) {
            return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
        } else if(date.toDateString() === yesterday.toDateString()) {
            return "Yesterday";
        } else {
            return date.toLocaleDateString();
        }
    } catch(e) { return ""; }
}
function escapeHtml(text) {
    if(!text) return "";
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
function searchChats() {
    const query = document.getElementById("search-input").value.toLowerCase().trim();
    const currentUserId = localStorage.getItem("currentUserId");
    if(query === "") {
        renderChats(allChats);
        return;
    }
    const savedChatId = localStorage.getItem("savedMessagesChatId");
    const filtered = allChats.filter(function(chat) {
        if(!chat) return false;
        if(chat.id === savedChatId) {
            return "saved messages".includes(query);
        }
        if(chat.type === "private") {
            const other = (chat.participantIds || []).find(function(id){ return id !== currentUserId; });
            return other ? other.toLowerCase().includes(query) : false;
        } else {
            return (chat.name || "").toLowerCase().includes(query);
        }
    });
    renderChats(filtered);
}
function openChat(chatId) {
    const chat = allChats.find(c => c.id === chatId);
    if(chat) {
        localStorage.setItem("lastSeen_" + chatId, String(chat.totalMessages || 0));
    }
    window.location.href = "Chat.html?id=" + encodeURIComponent(chatId);
}
function goSettings() {
    window.location.href = "Settings.html";
}
function goNewChat() {
    window.location.href = "NewChat.html";
}
