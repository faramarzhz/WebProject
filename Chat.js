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
const chatType   = urlParams.get("type") || "private"; // group یا private
let messages        = [];
let sendTimes       = [];
let allMessages     = [];
let isSearchMode    = false;
let currentChatGlobal = null;
let chatSocket      = null;
if(!currentUserId){
    window.location.href = "Login.html";
}
// endpoint های مناسب بر اساس نوع چت
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
                // پیام جدید رو مستقیم به لیست اضافه می‌کنه بدون reload کامل
                if(!messages.find(function(m){ return m.id === data.message.id; })){
                    messages.push(data.message);
                    allMessages.push(data.message);
                    if(!isSearchMode) renderMessages(messages);
                }
            }else if(data.type === "messageEdited"){
                // پیام ویرایش‌شده رو در لیست پیدا کن و آپدیت کن
                const idx = messages.findIndex(function(m){ return m.id === data.message.id; });
                if(idx !== -1){
                    messages[idx]    = data.message;
                    allMessages[idx] = data.message;
                    if(!isSearchMode) renderMessages(messages);
                }
            }else if(data.type === "messageDeleted"){
                loadMessages();
            }else if(data.type === "memberAdded" && data.groupId === chatId){
                // عضو جدیدی به این گروه اضافه شد، info رو reload کن
                loadChatInfo();
            }else if(data.type === "memberRemoved" && data.groupId === chatId){
                if(data.userId === currentUserId){
                    // خود ما از گروه اخراج شدیم
                    alert("You have been removed from this group.");
                    window.location.href = "Home.html";
                }else{
                    // عضو دیگه‌ای حذف شد، info رو آپدیت کن
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
        // اگه توی chats پیدا نشد، شاید گروهه
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
            statusText   = "last seen recently";
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
        const timeDiv = document.createElement("div");
        timeDiv.className   = "message-time";
        timeDiv.textContent = formatTime(message.time);
        messageDiv.appendChild(contentDiv);
        messageDiv.appendChild(timeDiv);
        // نمایش ری‌اکشن‌های موجود
        const reactions = message.reactions || {};
        if(Object.keys(reactions).length > 0){
            const reactionsBar = buildReactionsBar(message.id, reactions);
            messageDiv.appendChild(reactionsBar);
        }
        const bottomRow = document.createElement("div");
        bottomRow.style.cssText = "display:flex;align-items:center;gap:4px;margin-top:4px;position:relative;";
        // دکمه باز کردن picker
        const addReactBtn = document.createElement("button");
        addReactBtn.textContent = "😊";
        addReactBtn.title       = "Add Reaction";
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
        options.className = "message-options";
        options.style.flex = "1";
        if(message.senderId === currentUserId){
            const editBtn = document.createElement("button");
            editBtn.textContent = "✏";
            editBtn.title       = "Edit";
            editBtn.onclick     = function(){ editMessage(message.id, message.content); };
            const deleteBtn = document.createElement("button");
            deleteBtn.textContent = "🗑";
            deleteBtn.title       = "Delete";
            deleteBtn.onclick     = function(){ deleteMessage(message.id); };
            options.appendChild(editBtn);
            options.appendChild(deleteBtn);
        }
        const reportBtn = document.createElement("button");
        reportBtn.textContent = "🚩";
        reportBtn.title       = "Report";
        reportBtn.onclick     = function(){ reportMessage(message); };
        options.appendChild(reportBtn);
        bottomRow.appendChild(options);
        messageDiv.appendChild(bottomRow);
        container.appendChild(messageDiv);
    });
    if(wasAtBottom && !isSearchMode){
        setTimeout(function(){ container.scrollTop = container.scrollHeight; }, 0);
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
    // endpoint ویرایش بر اساس نوع چت
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
    // endpoint حذف بر اساس نوع چت
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
    // ری‌اکشن‌ها رو گروه‌بندی می‌کنه: emoji → [userIds]
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
        bubble.innerHTML = emoji + ' <span class="reaction-count">' + users.length + '</span>';
        bubble.onclick   = function(){ toggleReaction(msgId, emoji, reactions[currentUserId]); };
        bar.appendChild(bubble);
    });
    return bar;
}
async function toggleReaction(msgId, emoji, myCurrentEmoji){
    // اگه همین ایموجی رو قبلاً زده، unreact کن؛ وگرنه react کن
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
    // ضد اسپم: حداکثر ۵ پیام در ثانیه
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
            // پیام خودم رو بلافاصله load می‌کنه چون WebSocket فقط به طرف مقابل می‌فرسته
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
            <button onclick="alert('Add member coming soon')">➕ Add Member</button>
            <button onclick="alert('Edit group coming soon')">✏️ Edit Group Info</button>
            <button onclick="archiveChat()">📦 Add to Archive</button>
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
        btnContainer.innerHTML = `
            <button onclick="toggleSearch(); closeChatInfo();">🔍 Search Messages</button>
            <button onclick="addToContacts()">➕ Add to Contacts</button>
            <button onclick="toggleMute()">🔕 Mute Notifications</button>
            <button class="danger-button" onclick="blockUser()">🚫 Block User</button>
            <button onclick="archiveChat()">📦 Add to Archive</button>
        `;
    }
    modal.style.display = "flex";
}
function archiveChat(){
    let archivedIds = JSON.parse(localStorage.getItem("archivedChats") || "[]");
    if(!archivedIds.includes(chatId)){
        archivedIds.push(chatId);
        localStorage.setItem("archivedChats", JSON.stringify(archivedIds));
    }
    alert("Chat added to archive.");
    closeChatInfo();
    window.location.href = "Home.html";
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
async function toggleMute(){
    if(!currentChatGlobal) return;
    const key      = "muted_" + currentChatGlobal.id;
    const isMuted  = localStorage.getItem(key) === "true";
    const endpoint = isMuted ? "/api/chat/unmute" : "/api/chat/mute";
    try{
        const res = await fetch("http://localhost:8080" + endpoint, {
            method:  "POST",
            headers: {"Content-Type": "application/json"},
            body:    JSON.stringify({userId: currentUserId, chatId: currentChatGlobal.id})
        });
        if(res.ok){
            localStorage.setItem(key, String(!isMuted));
            alert(isMuted ? "🔔 Notifications unmuted." : "🔕 Notifications muted.");
        }else{
            alert("Failed to update mute setting.");
        }
    }catch(e){
        localStorage.setItem(key, String(!isMuted));
        alert(isMuted ? "🔔 Unmuted." : "🔕 Muted.");
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
function closeChatInfo(){
    const modal = document.getElementById("chat-info-modal");
    if(modal) modal.style.display = "none";
}
window.addEventListener("click", function(event){
    const modal = document.getElementById("chat-info-modal");
    if(modal && event.target === modal) closeChatInfo();
    // بستن همه picker های باز
    document.querySelectorAll(".reaction-picker.open").forEach(function(p){ p.classList.remove("open"); });
});
