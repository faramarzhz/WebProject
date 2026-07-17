(function applyThemeOnLoad(){
    const darkSetting = localStorage.getItem("darkMode");
    if(darkSetting === "false"){
        document.body.classList.add("light-mode");
    }else{
        document.body.classList.remove("light-mode");
    }
})();
window.onload = function(){
    const currentUserId = localStorage.getItem("currentUserId");
    if(!currentUserId){
        window.location.href = "Login.html";
        return;
    }
    loadArchivedChats(currentUserId);
};
function loadArchivedChats(userId){
    const chatsReq  = fetch("http://localhost:8080/api/chats?userId="  + encodeURIComponent(userId)).then(function(r){ return r.json(); });
    const groupsReq = fetch("http://localhost:8080/api/groups?userId=" + encodeURIComponent(userId)).then(function(r){ return r.json(); });
    Promise.all([chatsReq, groupsReq])
    .then(function(results){
        const chats  = Array.isArray(results[0]) ? results[0] : [];
        const groups = Array.isArray(results[1]) ? results[1] : [];
        const normalizedGroups = groups.map(function(g){
            return {
                id:                 g.id,
                type:               "group",
                name:               g.name,
                lastMessageContent: g.lastMessageContent,
                lastMessageTime:    g.lastMessageTime,
                isArchived:         g.isArchived,
                participantIds:     g.memberIds || []
            };
        });
        const merged = chats.concat(normalizedGroups);
        const archived = merged.filter(function(c){ return c.isArchived; });
        renderArchived(archived, userId);
    })
    .catch(function(){
        document.getElementById("archive-list").innerHTML =
            "<p style='color:#ff4444;text-align:center;padding:20px;'>Could not load chats.</p>";
    });
}
function renderArchived(chats, currentUserId){
    const container = document.getElementById("archive-list");
    container.innerHTML = "";
    if(chats.length === 0){
        const empty = document.createElement("p");
        empty.style.cssText = "color:#888;text-align:center;padding:30px;font-size:13px;";
        empty.textContent = "No archived chats.";
        container.appendChild(empty);
        return;
    }
    chats.forEach(function(chat){
        let chatName = "Unknown Chat";
        let avatarLetter = "?";
        let avatarBg = "#00cc55";
        if(chat.type === "private"){
            const other = (chat.participantIds || []).find(function(id){ return id !== currentUserId; }) || currentUserId;
            chatName = other;
            avatarLetter = chatName.charAt(0).toUpperCase();
        }else if(chat.type === "group"){
            chatName = chat.name || "Group Chat";
            avatarLetter = chatName.charAt(0).toUpperCase();
            avatarBg = "#9b59b6";
        }
        const item = document.createElement("div");
        item.className = "chat-item";
        item.style.position = "relative";
        // دکمه خروج از آرشیو
        const unarchiveBtn = document.createElement("button");
        unarchiveBtn.style.cssText = "position:absolute;right:8px;top:50%;transform:translateY(-50%);width:auto;padding:3px 9px;font-size:11px;background:#333;color:#aaa;border-radius:4px;cursor:pointer;border:none;";
        unarchiveBtn.textContent = "📤 Unarchive";
        unarchiveBtn.onclick = function(e){
            e.stopPropagation();
            unarchiveChat(chat.id);
        };
        item.innerHTML = `
            <div class="chat-avatar" style="background-color:${avatarBg};">${avatarLetter}</div>
            <div class="chat-info" style="padding-right:100px;">
                <div class="chat-top">
                    <span class="chat-name">${escapeHtml(chatName)}</span>
                    <span class="chat-time">${formatTime(chat.lastMessageTime)}</span>
                </div>
                <div class="chat-bottom">
                    <span class="chat-preview">${escapeHtml(chat.lastMessageContent || "No messages yet")}</span>
                </div>
            </div>
        `;
        item.onclick = function(){ window.location.href = "Chat.html?id=" + encodeURIComponent(chat.id); };
        item.appendChild(unarchiveBtn);
        container.appendChild(item);
    });
}
function unarchiveChat(chatId){
    const currentUserId = localStorage.getItem("currentUserId");
    fetch("http://localhost:8080/api/chat/unarchive", {
        method:  "POST",
        headers: {"Content-Type": "application/json"},
        body:    JSON.stringify({userId: currentUserId, chatId: chatId})
    })
    .then(function(){ loadArchivedChats(currentUserId); })
    .catch(function(){ alert("Could not unarchive chat."); });
}
function formatTime(timestamp){
    if(!timestamp) return "";
    try{
        const date = new Date(parseInt(timestamp));
        if(isNaN(date.getTime())) return "";
        const today = new Date();
        const yesterday = new Date(today);
        yesterday.setDate(yesterday.getDate() - 1);
        if(date.toDateString() === today.toDateString()){
            return date.toLocaleTimeString("en-US", {hour: "2-digit", minute: "2-digit"});
        }else if(date.toDateString() === yesterday.toDateString()){
            return "Yesterday";
        }else{
            return date.toLocaleDateString();
        }
    }catch(e){ return ""; }
}
function escapeHtml(text){
    if(!text) return "";
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
}
function goBack(){ window.location.href ="Home.html"; }