(function applyThemeOnLoad(){
    const darkSetting = localStorage.getItem("darkMode");
    if(darkSetting === "false"){
        document.body.classList.add("light-mode");
    }else{
        document.body.classList.remove("light-mode");
    }
})();
const currentUserId = localStorage.getItem("currentUserId");
const urlParams  = new URLSearchParams(window.location.search);
const chatId     = urlParams.get("id");
const chatType   = urlParams.get("type") || "private";
let messages        = [];
let sendTimes       = [];
let allMessages     = [];
let isSearchMode    = false;
let currentChatGlobal = null;
let chatSocket      = null;
if(!currentUserId){
    window.location.href = "Login.html";
}
const msgEndpoint  = chatType === "group" ? "http://localhost:8080/api/group/" + chatId + "/messages"
                                          : "http://localhost:8080/api/chat/"  + chatId + "/messages";
const sendEndpoint = chatType === "group" ? "http://localhost:8080/api/group/" + chatId + "/send"
                                          : "http://localhost:8080/api/chat/"  + chatId + "/send";
loadMessages();
loadChatInfo();
connectChatSocket();
function connectChatSocket(){
    chatSocket = new WebSocket("ws://localhost:8080?userId=" + encodeURIComponent(currentUserId));
    chatSocket.onmessage = function(event){
        try{
            const data = JSON.parse(event.data);
            if(data.type === "newMessage"){
                if(!messages.find(function(m){ return m.id === data.message.id; })){
                    messages.push(data.message);
                    allMessages.push(data.message);
                    if(!isSearchMode) renderMessages(messages);
                }
            }else if(data.type === "messageEdited"){
                const idx = messages.findIndex(function(m){ return m.id === data.message.id; });
                if(idx !== -1){
                    messages[idx]    = data.message;
                    allMessages[idx] = data.message;
                    if(!isSearchMode) renderMessages(messages);
                }
            }else if(data.type === "messageDeleted"){
                loadMessages();
            }else if(data.type === "memberAdded" && data.groupId === chatId){
                loadChatInfo();
            }else if(data.type === "memberRemoved" && data.groupId === chatId){
                if(data.userId === currentUserId){
                    alert("You have been removed from this group.");
                    window.location.href = "Home.html";
                }else{
                    loadChatInfo();
                }
            }
        }catch(e){}
    };
    chatSocket.onclose = function(){
        setTimeout(function(){ connectChatSocket(); }, 3000);
    };
    chatSocket.onerror = function(){
        chatSocket.close();
    };
}
async function loadChatInfo(){
    try{
        const response = await fetch("http://localhost:8080/api/chats?userId=" + encodeURIComponent(currentUserId));
        if(!response.ok) return;
        const chats = await response.json();
        let chat = chats.find(function(c){ return c.id === chatId; });
        if(!chat && chatType === "group"){
            const gRes = await fetch("http://localhost:8080/api/groups?userId=" + encodeURIComponent(currentUserId));
            if(gRes.ok){
                const groups = await gRes.json();
                const g = groups.find(function(gr){ return gr.id === chatId; });
                if(g){
                    chat = {
                        id:             g.id,
                        type:           "group",
                        name:           g.name,
                        isPinned:       g.isPinned,
                        isArchived:     g.isArchived,
                        isMuted:        g.isMuted,
                        participantIds: g.memberIds || []
                    };
                }
            }
        }
        if(!chat) return;
        currentChatGlobal = chat;
        const savedChatId = localStorage.getItem("savedMessagesChatId");
        let chatName     = "Chat";
        let avatarLetter = "C";
        let statusText   = "";
        if(chat.id === savedChatId){
            chatName     = "Saved Messages";
            avatarLetter = "💾";
            statusText   = "Your personal storage";
        }else if(chat.type === "group"){
            chatName     = chat.name || "Group Chat";
            avatarLetter = chatName.charAt(0).toUpperCase();
            statusText   = (chat.participantIds || []).length + " members";
        }else{
            const otherUser = (chat.participantIds || []).find(function(id){ return id !== currentUserId; }) || "Unknown";
            chatName     = otherUser;
            avatarLetter = otherUser.charAt(0).toUpperCase();
            // ━━━ مورد ۸: last seen از سرور خونده می‌شه ━━━
            fetchLastSeen(otherUser);
        }
        const chatNameEl     = document.getElementById("chat-name");
        const chatStatusEl   = document.getElementById("chat-status");
        const headerAvatarEl = document.getElementById("header-avatar");
        const infoNameEl     = document.getElementById("info-name");
        const infoIdEl       = document.getElementById("info-id");
        const infoAvatarEl   = document.getElementById("info-avatar");
        if(chatNameEl)     chatNameEl.textContent     = chatName;
        if(chatStatusEl)   chatStatusEl.textContent   = statusText;
        if(headerAvatarEl) headerAvatarEl.textContent = avatarLetter;
        if(infoNameEl)     infoNameEl.textContent     = chatName;
        if(infoIdEl)       infoIdEl.textContent       = "#" + chatId.substring(0, 8);
        if(infoAvatarEl)   infoAvatarEl.textContent   = avatarLetter;
    }catch(error){
        console.error("loadChatInfo error:", error);
    }
}
async function fetchLastSeen(otherUserId){
    try{
        const res = await fetch("http://localhost:8080/api/user/lastseen?userId=" + encodeURIComponent(otherUserId));
        const chatStatusEl = document.getElementById("chat-status");
        if(!chatStatusEl) return;
        if(!res.ok){
            chatStatusEl.textContent = "last seen recently";
            return;
        }
        const data = await res.json();
        if(data.online){
            chatStatusEl.textContent = "online";
            chatStatusEl.style.color = "#00cc55";
        }else if(data.lastSeen){
            chatStatusEl.textContent = "last seen " + formatLastSeen(data.lastSeen);
            chatStatusEl.style.color = "";
        }else{
            chatStatusEl.textContent = "last seen recently";
        }
    }catch(e){
        const el = document.getElementById("chat-status");
        if(el) el.textContent = "last seen recently";
    }
}
function formatLastSeen(timestamp){
    try{
        const date = new Date(parseInt(timestamp));
        const now  = new Date();
        const diff = now - date;
        if(diff < 60000)           return "just now";
        if(diff < 3600000)         return Math.floor(diff / 60000) + "m ago";
        if(diff < 86400000)        return "today at " + date.getHours().toString().padStart(2,"0") + ":" + date.getMinutes().toString().padStart(2,"0");
        if(diff < 172800000)       return "yesterday";
        return date.toLocaleDateString();
    }catch(e){ return "recently"; }
}
async function loadMessages(){
    try{
        const response = await fetch(msgEndpoint);
        if(!response.ok) return;
        const data = await response.json();
        if(JSON.stringify(data) !== JSON.stringify(messages)){
            messages    = data;
            allMessages = data;
            if(!isSearchMode) renderMessages(messages);
        }
    }catch(error){
        console.error("loadMessages error:", error);
    }
}
function renderMessages(msgs){
    const container = document.getElementById("messages-container");
    if(!container) return;
    const wasAtBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 50;
    container.innerHTML = "";
    if(!msgs || msgs.length === 0){
        const emptyMsg = document.createElement("div");
        emptyMsg.style.cssText = "color:#888;text-align:center;padding:20px;";
        emptyMsg.textContent = isSearchMode ? "No messages found." : "No messages yet. Start the conversation!";
        container.appendChild(emptyMsg);
        return;
    }
    msgs.forEach(function(message){
        if(!message || !message.id) return;
        const messageDiv = document.createElement("div");
        messageDiv.className = message.senderId === currentUserId ? "message my-message" : "message other-message";
        const contentDiv = document.createElement("div");
        contentDiv.className   = "message-text";
        contentDiv.textContent = message.content || "(empty message)";
        // نشان‌دهنده ویرایش‌شده بودن پیام
        if(message.isEdited && !message.isDeleted){
            const editedTag = document.createElement("span");
            editedTag.style.cssText = "font-size:10px;opacity:0.6;margin-left:4px;";
            editedTag.textContent = "(edited)";
            contentDiv.appendChild(editedTag);
        }
        const timeDiv = document.createElement("div");
        timeDiv.className   = "message-time";
        timeDiv.textContent = formatTime(message.time);
        messageDiv.appendChild(contentDiv);
        messageDiv.appendChild(timeDiv);
        const reactions = message.reactions || {};
        if(Object.keys(reactions).length > 0){
            messageDiv.appendChild(buildReactionsBar(message.id, reactions));
        }
        const bottomRow = document.createElement("div");
        bottomRow.style.cssText = "display:flex;align-items:center;gap:4px;margin-top:4px;position:relative;";
        const addReactBtn = document.createElement("button");
        addReactBtn.textContent   = "😊";
        addReactBtn.title         = "Add Reaction";
        addReactBtn.style.cssText = "background:rgba(255,255,255,0.1);border:none;border-radius:50%;width:24px;height:24px;cursor:pointer;font-size:13px;display:flex;align-items:center;justify-content:center;";
        const picker = document.createElement("div");
        picker.className = "reaction-picker";
        ["👍","❤️","😂","😮","😢","🔥","👏","🎉"].forEach(function(emoji){
            const span = document.createElement("span");
            span.textContent = emoji;
            span.onclick = function(){
                picker.classList.remove("open");
                toggleReaction(message.id, emoji, reactions[currentUserId]);
            };
            picker.appendChild(span);
        });
        addReactBtn.onclick = function(e){
            e.stopPropagation();
            picker.classList.toggle("open");
        };
        bottomRow.appendChild(addReactBtn);
        bottomRow.appendChild(picker);
        const options = document.createElement("div");
        options.className  = "message-options";
        options.style.flex = "1";
        if(message.senderId === currentUserId && !message.isDeleted){
            const editBtn       = document.createElement("button");
            editBtn.textContent = "✏";
            editBtn.title       = "Edit";
            editBtn.onclick     = function(){ editMessage(message.id, message.content); };
            const deleteBtn       = document.createElement("button");
            deleteBtn.textContent = "🗑";
            deleteBtn.title       = "Delete";
            deleteBtn.onclick     = function(){ deleteMessage(message.id); };
            options.appendChild(editBtn);
            options.appendChild(deleteBtn);
            if(message.isEdited || message.isDeleted){
                const historyBtn       = document.createElement("button");
                historyBtn.textContent = "📋";
                historyBtn.title       = "Edit History";
                historyBtn.onclick     = function(){ showMessageHistory(message.id); };
                options.appendChild(historyBtn);
            }
        }
        if(!message.isDeleted){
            const reportBtn       = document.createElement("button");
            reportBtn.textContent = "🚩";
            reportBtn.title       = "Report";
            reportBtn.onclick     = function(){ reportMessage(message); };
            options.appendChild(reportBtn);
        }
        bottomRow.appendChild(options);
        messageDiv.appendChild(bottomRow);
        if(message.mediaPath){
            const mediaEl = buildMediaElement(message.mediaPath);
            if(mediaEl) messageDiv.appendChild(mediaEl);
        }
        container.appendChild(messageDiv);
    });
    if(wasAtBottom && !isSearchMode){
        setTimeout(function(){ container.scrollTop = container.scrollHeight; }, 0);
    }
}
function buildMediaElement(mediaPath){
    const ext = mediaPath.split(".").pop().toLowerCase();
    if(["jpg","jpeg","png","gif","webp"].includes(ext)){
        const img = document.createElement("img");
        img.src   = "http://localhost:8080/uploads/" + mediaPath;
        img.style.cssText = "max-width:220px;max-height:220px;border-radius:8px;margin-top:6px;display:block;cursor:pointer;";
        img.onclick = function(){ window.open(img.src, "_blank"); };
        return img;
    }
    const link = document.createElement("a");
    link.href      = "http://localhost:8080/uploads/" + mediaPath;
    link.textContent = "📎 " + mediaPath;
    link.target    = "_blank";
    link.style.cssText = "display:block;margin-top:6px;color:#00cc55;font-size:13px;";
    return link;
}
async function showMessageHistory(msgId){
    try{
        const res = await fetch("http://localhost:8080/api/chat/" + chatId + "/message/history?messageId=" + encodeURIComponent(msgId));
        if(!res.ok){
            alert("Could not load history.");
            return;
        }
        const data = await res.json();
        const history = data.history || [];
        if(history.length === 0){
            alert("No edit history available.");
            return;
        }
        const overlay = document.createElement("div");
        overlay.style.cssText = "position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.7);z-index:2000;display:flex;justify-content:center;align-items:center;";
        const box = document.createElement("div");
        box.style.cssText = "background:#1e1e1e;border-radius:12px;padding:20px;max-width:400px;width:90%;max-height:70vh;overflow-y:auto;position:relative;";
        const title = document.createElement("h3");
        title.textContent = "📋 Edit History";
        title.style.cssText = "color:#00cc55;margin-bottom:14px;";
        const closeBtn = document.createElement("button");
        closeBtn.textContent = "✕";
        closeBtn.style.cssText = "position:absolute;top:12px;right:12px;background:#333;border:none;color:white;width:28px;height:28px;border-radius:50%;cursor:pointer;font-size:16px;";
        closeBtn.onclick = function(){ document.body.removeChild(overlay); };
        box.appendChild(closeBtn);
        box.appendChild(title);
        history.forEach(function(entry, i){
            const item = document.createElement("div");
            item.style.cssText = "border-bottom:1px solid #333;padding:8px 0;";
            const ver = document.createElement("div");
            ver.style.cssText = "font-size:11px;color:#888;margin-bottom:4px;";
            ver.textContent = "Version " + (i + 1) + " — " + new Date(parseInt(entry.editedAt)).toLocaleString();
            const content = document.createElement("div");
            content.style.cssText = "color:#f0f0f0;font-size:13px;";
            content.textContent = entry.previousContent;
            item.appendChild(ver);
            item.appendChild(content);
            box.appendChild(item);
        });
        overlay.appendChild(box);
        overlay.onclick = function(e){ if(e.target === overlay) document.body.removeChild(overlay); };
        document.body.appendChild(overlay);
    }catch(e){
        alert("Error loading history.");
    }
}
async function editMessage(msgId, currentContent){
    const newContent = prompt("Edit message:", currentContent);
    if(newContent === null) return;
    if(newContent.trim() === ""){
        alert("Message cannot be empty.");
        return;
    }
    if(newContent.trim() === currentContent) return;
    const editEndpoint = chatType === "group"
        ? "http://localhost:8080/api/group/" + chatId + "/message/edit"
        : "http://localhost:8080/api/chat/"  + chatId + "/message/edit";
    try{
        const res = await fetch(editEndpoint, {
            method:  "POST",
            headers: {"Content-Type": "application/json"},
            body:    JSON.stringify({messageId: msgId, newContent: newContent.trim(), senderId: currentUserId})
        });
        if(res.ok){
            await loadMessages();
        }else{
            const data = await res.json();
            alert(data.error || "Failed to edit message.");
        }
    }catch(e){
        alert("Error connecting to server.");
    }
}
async function deleteMessage(msgId){
    if(!confirm("Delete this message?")) return;
    const deleteEndpoint = chatType === "group"
        ? "http://localhost:8080/api/group/" + chatId + "/message/delete"
        : "http://localhost:8080/api/chat/"  + chatId + "/message/delete";
    try{
        const res = await fetch(deleteEndpoint, {
            method:  "POST",
            headers: {"Content-Type": "application/json"},
            body:    JSON.stringify({messageId: msgId, senderId: currentUserId})
        });
        if(res.ok){
            await loadMessages();
        }else{
            const data = await res.json();
            alert(data.error || "Failed to delete message.");
        }
    }catch(e){
        alert("Error connecting to server.");
    }
}
function buildReactionsBar(msgId, reactions){
    const bar = document.createElement("div");
    bar.className = "reactions-bar";
    const grouped = {};
    Object.entries(reactions).forEach(function(entry){
        const uid = entry[0], emoji = entry[1];
        if(!grouped[emoji]) grouped[emoji] = [];
        grouped[emoji].push(uid);
    });
    Object.entries(grouped).forEach(function(entry){
        const emoji = entry[0], users = entry[1];
        const bubble = document.createElement("div");
        bubble.className = "reaction-bubble" + (users.includes(currentUserId) ? " mine" : "");
        bubble.title     = users.join(", ");
        bubble.innerHTML = emoji + ' <span class="reaction-count">' + users.length + "</span>";
        bubble.onclick   = function(){ toggleReaction(msgId, emoji, reactions[currentUserId]); };
        bar.appendChild(bubble);
    });
    return bar;
}
async function toggleReaction(msgId, emoji, myCurrentEmoji){
    if(myCurrentEmoji === emoji){
        await fetch("http://localhost:8080/api/chat/" + chatId + "/message/unreact", {
            method:  "POST",
            headers: {"Content-Type": "application/json"},
            body:    JSON.stringify({messageId: msgId, userId: currentUserId})
        });
    }else{
        await fetch("http://localhost:8080/api/chat/" + chatId + "/message/react", {
            method:  "POST",
            headers: {"Content-Type": "application/json"},
            body:    JSON.stringify({messageId: msgId, userId: currentUserId, emoji: emoji})
        });
    }
    await loadMessages();
}
async function reportMessage(message){
    const reason = prompt("Why are you reporting this message?\n\n\"" + message.content + "\"");
    if(reason === null) return;
    if(reason.trim() === ""){
        alert("Please enter a reason.");
        return;
    }
    try{
        const res = await fetch("http://localhost:8080/api/chat/" + chatId + "/report", {
            method:  "POST",
            headers: {"Content-Type": "application/json"},
            body:    JSON.stringify({
                reporterId: currentUserId,
                messageId:  message.id,
                senderId:   message.senderId,
                content:    message.content,
                reason:     reason.trim()
            })
        });
        alert(res.ok ? "Message reported successfully." : "Failed to report message.");
    }catch(e){
        alert("Error reporting message.");
    }
}
async function sendFile(file){
    if(!file) return;
    if(file.size > 20 * 1024 * 1024){
        alert("File size must be under 20MB.");
        return;
    }
    const formData = new FormData();
    formData.append("file", file);
    formData.append("senderId", currentUserId);
    formData.append("chatId", chatId);
    formData.append("chatType", chatType);
    try{
        const res = await fetch("http://localhost:8080/api/upload", {
            method: "POST",
            body:   formData
        });
        if(res.ok){
            await loadMessages();
        }else{
            alert("Failed to upload file.");
        }
    }catch(e){
        alert("Error uploading file.");
    }
}
document.addEventListener("DOMContentLoaded", function(){
    const attachBtn = document.querySelector(".attach-button");
    const fileInput = document.getElementById("file-input");
    if(attachBtn && fileInput){
        attachBtn.onclick = function(){ fileInput.click(); };
        fileInput.onchange = function(){
            if(fileInput.files[0]){
                sendFile(fileInput.files[0]);
                fileInput.value = "";
            }
        };
    }
});
async function sendMessage(){
    const input   = document.getElementById("message-input");
    const content = input.value.trim();
    if(content === ""){
        alert("Message cannot be empty");
        return;
    }
    if(content.length > 1000){
        alert("Message is too long (max 1000 characters)");
        return;
    }
    const now = Date.now();
    sendTimes = sendTimes.filter(function(t){ return now - t < 1000; });
    if(sendTimes.length >= 5){
        alert("Maximum 5 messages per second. Please slow down.");
        return;
    }
    sendTimes.push(now);
    try{
        const res = await fetch(sendEndpoint, {
            method:  "POST",
            headers: {"Content-Type": "application/json"},
            body:    JSON.stringify({senderId: currentUserId, content: content})
        });
        if(res.ok){
            input.value = "";
            await loadMessages();
        }else{
            alert("Failed to send message. Status: " + res.status);
        }
    }catch(error){
        alert("Error sending message: " + error.message);
    }
}
document.getElementById("message-input")?.addEventListener("keydown", function(event){
    if(event.key === "Enter" && !event.shiftKey){
        event.preventDefault();
        sendMessage();
    }
});
function toggleSearch(){
    const searchBar = document.getElementById("chat-search-bar");
    if(!searchBar) return;
    if(searchBar.style.display === "none" || searchBar.style.display === ""){
        searchBar.style.display = "flex";
        document.getElementById("chat-search-input").focus();
    }else{
        searchBar.style.display = "none";
        clearSearch();
    }
}
function searchMessages(){
    const query = document.getElementById("chat-search-input").value.toLowerCase().trim();
    if(query === ""){
        isSearchMode = false;
        renderMessages(allMessages);
        return;
    }
    isSearchMode = true;
    const filtered = allMessages.filter(function(m){ return m.content && m.content.toLowerCase().includes(query); });
    renderMessages(filtered);
}
function clearSearch(){
    const input = document.getElementById("chat-search-input");
    if(input) input.value = "";
    isSearchMode = false;
    renderMessages(allMessages);
}
function formatTime(timestamp){
    if(!timestamp) return "";
    try{
        const date = new Date(parseInt(timestamp));
        if(isNaN(date.getTime())) return "";
        const h = date.getHours().toString().padStart(2, "0");
        const m = date.getMinutes().toString().padStart(2, "0");
        return h + ":" + m;
    }catch(e){ return ""; }
}
function goHome(){
    if(chatSocket) chatSocket.close();
    window.location.href = "Home.html";
}
function openChatInfo(){
    const modal = document.getElementById("chat-info-modal");
    if(!modal || !currentChatGlobal) return;
    const btnContainer   = modal.querySelector(".info-buttons");
    const membersSection = document.getElementById("group-members-section");
    const membersList    = document.getElementById("members-list");
    if(!btnContainer) return;
    btnContainer.innerHTML = "";
    if(membersList) membersList.innerHTML = "";
    const savedChatId = localStorage.getItem("savedMessagesChatId");
    if(chatId === savedChatId){
        if(membersSection) membersSection.style.display = "none";
        btnContainer.innerHTML = `
            <button onclick="toggleSearch(); closeChatInfo();">🔍 Search Messages</button>
            <button onclick="alert('Storage cleared!')">🧹 Clear Storage</button>
        `;
    }else if(currentChatGlobal.type === "group"){
        if(membersSection){
            membersSection.style.display = "block";
            if(currentChatGlobal.participantIds && membersList){
                currentChatGlobal.participantIds.forEach(function(memberId){
                    const li = document.createElement("li");
                    li.style.cssText = "padding:4px 0;color:#ccc;font-size:13px;";
                    li.textContent   = "• " + memberId + (memberId === currentUserId ? " (You)" : "");
                    membersList.appendChild(li);
                });
            }
        }
        btnContainer.innerHTML = `
            <button onclick="toggleSearch(); closeChatInfo();">🔍 Search Messages</button>
            <button onclick="openAddMemberModal()">➕ Add Member</button>
            <button onclick="openEditGroupModal()">✏️ Edit Group Info</button>
            <button onclick="archiveChat()">📦 ${currentChatGlobal.isArchived ? "Unarchive" : "Add to Archive"}</button>
            <button class="danger-button" onclick="leaveGroup()">🚪 Leave Group</button>
        `;
    }else{
        if(membersSection) membersSection.style.display = "none";
        const otherUser = (currentChatGlobal.participantIds || []).find(function(id){ return id !== currentUserId; }) || "Unknown";
        const infoNameEl   = document.getElementById("info-name");
        const infoIdEl     = document.getElementById("info-id");
        const infoAvatarEl = document.getElementById("info-avatar");
        if(infoNameEl)   infoNameEl.textContent   = otherUser;
        if(infoIdEl)     infoIdEl.textContent     = "@" + otherUser;
        if(infoAvatarEl) infoAvatarEl.textContent = otherUser.charAt(0).toUpperCase();
        const isMuted = currentChatGlobal.isMuted;
        btnContainer.innerHTML = `
            <button onclick="toggleSearch(); closeChatInfo();">🔍 Search Messages</button>
            <button onclick="addToContacts()">➕ Add to Contacts</button>
            <button onclick="toggleMute()">${isMuted ? "🔔 Unmute" : "🔕 Mute"} Notifications</button>
            <button class="danger-button" onclick="blockUser()">🚫 Block User</button>
            <button onclick="archiveChat()">📦 ${currentChatGlobal.isArchived ? "Unarchive" : "Add to Archive"}</button>
        `;
    }
    modal.style.display = "flex";
}
async function archiveChat(){
    if(!currentChatGlobal) return;
    const isArchived = currentChatGlobal.isArchived;
    const endpoint   = isArchived ? "/api/chat/unarchive" : "/api/chat/archive";
    try{
        const res = await fetch("http://localhost:8080" + endpoint, {
            method:  "POST",
            headers: {"Content-Type": "application/json"},
            body:    JSON.stringify({userId: currentUserId, chatId: chatId})
        });
        if(res.ok){
            closeChatInfo();
            window.location.href = "Home.html";
        }else{
            alert("Failed to update archive.");
        }
    }catch(e){
        alert("Error connecting to server.");
    }
}
async function toggleMute(){
    if(!currentChatGlobal) return;
    const isMuted  = currentChatGlobal.isMuted;
    const endpoint = isMuted ? "/api/chat/unmute" : "/api/chat/mute";
    try{
        const res = await fetch("http://localhost:8080" + endpoint, {
            method:  "POST",
            headers: {"Content-Type": "application/json"},
            body:    JSON.stringify({userId: currentUserId, chatId: chatId})
        });
        if(res.ok){
            currentChatGlobal.isMuted = !isMuted;
            alert(isMuted ? "🔔 Notifications unmuted." : "🔕 Notifications muted.");
        }else{
            alert("Failed to update mute.");
        }
    }catch(e){
        alert("Error connecting to server.");
    }
    closeChatInfo();
}
async function leaveGroup(){
    if(!confirm("Are you sure you want to leave this group?")) return;
    try{
        const res = await fetch("http://localhost:8080/api/group/removemember", {
            method:  "POST",
            headers: {"Content-Type": "application/json"},
            body:    JSON.stringify({groupId: chatId, requesterId: currentUserId, userId: currentUserId})
        });
        if(res.ok){
            window.location.href = "Home.html";
        }else{
            alert("Failed to leave group.");
        }
    }catch(e){
        alert("Error connecting to server.");
    }
}
async function addToContacts(){
    if(!currentChatGlobal) return;
    const otherUser = (currentChatGlobal.participantIds || []).find(function(id){ return id !== currentUserId; });
    if(!otherUser) return;
    try{
        const res = await fetch("http://localhost:8080/api/contact/add", {
            method:  "POST",
            headers: {"Content-Type": "application/json"},
            body:    JSON.stringify({userId: currentUserId, contactId: otherUser})
        });
        alert(res.ok ? "✓ User added to contacts!" : "Failed to add contact.");
    }catch(e){
        alert("Error connecting to server.");
    }
    closeChatInfo();
}
async function blockUser(){
    if(!currentChatGlobal) return;
    const otherUser = (currentChatGlobal.participantIds || []).find(function(id){ return id !== currentUserId; });
    if(!otherUser) return;
    if(!confirm("Are you sure you want to block " + otherUser + "?")) return;
    try{
        const res = await fetch("http://localhost:8080/api/user/block", {
            method:  "POST",
            headers: {"Content-Type": "application/json"},
            body:    JSON.stringify({userId: currentUserId, targetId: otherUser})
        });
        alert(res.ok ? "🚫 User " + otherUser + " has been blocked." : "Failed to block user.");
    }catch(e){
        alert("Error connecting to server.");
    }
    closeChatInfo();
    window.location.href = "Home.html";
}
function openAddMemberModal(){
    closeChatInfo();
    const overlay = document.createElement("div");
    overlay.id = "dynamic-modal";
    overlay.className = "chat-info-modal";
    overlay.style.display = "flex";
    overlay.innerHTML = `
        <div class="chat-info-box" style="align-items:stretch;">
            <button class="close-info" onclick="removeDynamicModal()">✕</button>
            <h2 style="color:#00cc55;text-align:center;margin-top:15px;">➕ Add Member</h2>
            <div style="width:100%;margin-top:12px;">
                <input type="text" id="add-member-input" placeholder="Enter User ID"
                    style="width:100%;padding:12px;background:#2b2b2b;color:white;border:1px solid #444;border-radius:6px;font-size:14px;outline:none;box-sizing:border-box;">
                <div id="add-member-error" style="color:#ff4444;font-size:12px;min-height:18px;margin-top:4px;"></div>
                <button onclick="confirmAddMember()"
                    style="width:100%;padding:12px;background:#00cc55;color:#000;border:none;border-radius:6px;font-weight:bold;cursor:pointer;margin-top:8px;">
                    Add Member
                </button>
            </div>
        </div>
    `;
    overlay.onclick = function(e){ if(e.target === overlay) removeDynamicModal(); };
    document.body.appendChild(overlay);
}
async function confirmAddMember(){
    const input = document.getElementById("add-member-input");
    const errEl = document.getElementById("add-member-error");
    if(!input || !errEl) return;
    const userId = input.value.trim();
    if(!userId){
        errEl.textContent = "Please enter a User ID.";
        return;
    }
    try{
        const res = await fetch("http://localhost:8080/api/group/addmember", {
            method:  "POST",
            headers: {"Content-Type": "application/json"},
            body:    JSON.stringify({groupId: chatId, requesterId: currentUserId, userId: userId})
        });
        const data = await res.json();
        if(res.ok){
            removeDynamicModal();
            await loadChatInfo();
        }else{
            errEl.textContent = data.error || "Failed to add member.";
        }
    }catch(e){
        errEl.textContent = "Error connecting to server.";
    }
}
function openEditGroupModal(){
    closeChatInfo();
    const overlay = document.createElement("div");
    overlay.id = "dynamic-modal";
    overlay.className = "chat-info-modal";
    overlay.style.display = "flex";
    overlay.innerHTML = `
        <div class="chat-info-box" style="align-items:stretch;">
            <button class="close-info" onclick="removeDynamicModal()">✕</button>
            <h2 style="color:#00cc55;text-align:center;margin-top:15px;">✏️ Edit Group</h2>
            <div style="width:100%;margin-top:12px;">
                <input type="text" id="edit-group-name-input" placeholder="New group name"
                    value="${currentChatGlobal ? (currentChatGlobal.name || "") : ""}"
                    style="width:100%;padding:12px;background:#2b2b2b;color:white;border:1px solid #444;border-radius:6px;font-size:14px;outline:none;box-sizing:border-box;">
                <div id="edit-group-error" style="color:#ff4444;font-size:12px;min-height:18px;margin-top:4px;"></div>
                <button onclick="confirmEditGroup()"
                    style="width:100%;padding:12px;background:#00cc55;color:#000;border:none;border-radius:6px;font-weight:bold;cursor:pointer;margin-top:8px;">
                    Save Changes
                </button>
            </div>
        </div>
    `;
    overlay.onclick = function(e){ if(e.target === overlay) removeDynamicModal(); };
    document.body.appendChild(overlay);
}
async function confirmEditGroup(){
    const input = document.getElementById("edit-group-name-input");
    const errEl = document.getElementById("edit-group-error");
    if(!input || !errEl) return;
    const newName = input.value.trim();
    if(!newName){
        errEl.textContent = "Group name cannot be empty.";
        return;
    }
    try{
        const res = await fetch("http://localhost:8080/api/group/update", {
            method:  "POST",
            headers: {"Content-Type": "application/json"},
            body:    JSON.stringify({groupId: chatId, requesterId: currentUserId, groupName: newName})
        });
        const data = await res.json();
        if(res.ok){
            removeDynamicModal();
            await loadChatInfo();
        }else{
            errEl.textContent = data.error || "Failed to update group.";
        }
    }catch(e){
        errEl.textContent = "Error connecting to server.";
    }
}
function removeDynamicModal(){
    const m = document.getElementById("dynamic-modal");
    if(m) document.body.removeChild(m);
}
function closeChatInfo(){
    const modal = document.getElementById("chat-info-modal");
    if(modal) modal.style.display = "none";
}
window.addEventListener("click", function(event){
    const modal = document.getElementById("chat-info-modal");
    if(modal && event.target === modal) closeChatInfo();
    document.querySelectorAll(".reaction-picker.open").forEach(function(p){ p.classList.remove("open"); });
});
