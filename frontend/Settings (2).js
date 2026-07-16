(function applyThemeOnLoad(){
    const darkSetting = localStorage.getItem("darkMode");
    if(darkSetting === "false"){
        document.body.classList.add("light-mode");
    }else{
        document.body.classList.remove("light-mode");
    }
})();
function init(){
    const currentUserId = localStorage.getItem("currentUserId");
    if(!currentUserId){
        window.location.href = "Login.html";
        return;
    }
    const user = JSON.parse(localStorage.getItem("loggedInUser") || "{}");
    const usernameInput = document.getElementById("username-input");
    const useridInput   = document.getElementById("userid-input");
    const avatar        = document.getElementById("settings-avatar");
    if(usernameInput && user.username){
        usernameInput.value = user.username;
    }
    if(useridInput){
        useridInput.value = user.userId || currentUserId;
    }
    if(avatar){
        if(user.photo){
            avatar.style.backgroundImage = "url(" + user.photo + ")";
            avatar.style.backgroundSize  = "cover";
            avatar.textContent = "";
        }else{
            const name = user.username || currentUserId;
            avatar.textContent = name.charAt(0).toUpperCase();
        }
    }
    const darkToggle = document.getElementById("dark-toggle");
    const dark = localStorage.getItem("darkMode");
    if(darkToggle){
        darkToggle.checked = dark !== "false";
    }
    // مورد ۹: لود لیست بلاک‌شده‌ها
    loadBlockedUsers(currentUserId);
}
// ━━━ مورد ۹: گرفتن لیست بلاک‌شده‌ها از سرور ━━━
function loadBlockedUsers(userId){
    const container = document.getElementById("blocked-list-container");
    if(!container) return;
    fetch("http://localhost:8080/api/user/blocked?userId=" + encodeURIComponent(userId))
    .then(function(res){
        if(!res.ok) throw new Error("failed");
        return res.json();
    })
    .then(function(data){
        const blocked = Array.isArray(data.blockedUsers) ? data.blockedUsers : [];
        renderBlockedList(blocked);
    })
    .catch(function(){
        container.innerHTML = "<p style='color:#888;font-size:13px;'>Could not load blocked users.</p>";
    });
}
function renderBlockedList(blockedUsers){
    const container = document.getElementById("blocked-list-container");
    if(!container) return;
    container.innerHTML = "";
    if(blockedUsers.length === 0){
        const empty = document.createElement("p");
        empty.style.cssText = "color:#888;font-size:13px;";
        empty.textContent = "No blocked users.";
        container.appendChild(empty);
        return;
    }
    blockedUsers.forEach(function(userId){
        const row = document.createElement("div");
        row.style.cssText = "display:flex;justify-content:space-between;align-items:center;padding:8px 0;border-bottom:1px solid #333;";
        const nameEl = document.createElement("span");
        nameEl.style.cssText = "color:#f0f0f0;font-size:14px;";
        nameEl.textContent = userId;
        const unblockBtn = document.createElement("button");
        unblockBtn.style.cssText = "width:auto;padding:5px 12px;background:#cc3333;color:white;border:none;border-radius:5px;cursor:pointer;font-size:12px;font-weight:bold;";
        unblockBtn.textContent = "Unblock";
        unblockBtn.onclick = function(){ unblockUser(userId); };
        row.appendChild(nameEl);
        row.appendChild(unblockBtn);
        container.appendChild(row);
    });
}
// ━━━ مورد ۹: آنبلاک کردن از API ━━━
async function unblockUser(targetId){
    if(!confirm("Unblock " + targetId + "?")) return;
    const currentUserId = localStorage.getItem("currentUserId");
    try{
        const res = await fetch("http://localhost:8080/api/user/unblock", {
            method:  "POST",
            headers: {"Content-Type": "application/json"},
            body:    JSON.stringify({userId: currentUserId, targetId: targetId})
        });
        if(res.ok){
            showMsg("✓ " + targetId + " unblocked.", "success");
            loadBlockedUsers(currentUserId);
        }else{
            showMsg("Failed to unblock user.", "error");
        }
    }catch(e){
        showMsg("Error connecting to server.", "error");
    }
}
function saveUsername(){
    const val = document.getElementById("username-input").value.trim();
    if(val === ""){
        showMsg("Username cannot be empty.", "error");
        return;
    }
    if(val.length < 3 || val.length > 30){
        showMsg("Username should be between 3 and 30 characters.", "error");
        return;
    }
    const user = JSON.parse(localStorage.getItem("loggedInUser") || "{}");
    user.username = val;
    localStorage.setItem("loggedInUser", JSON.stringify(user));
    const avatar = document.getElementById("settings-avatar");
    if(avatar && !user.photo){
        avatar.textContent = val.charAt(0).toUpperCase();
    }
    showMsg("✓ Username updated successfully.", "success");
}
function saveUserId(){
    const val = document.getElementById("userid-input").value.trim();
    if(val === ""){
        showMsg("User ID cannot be empty.", "error");
        return;
    }
    if(val.length < 3 || val.length > 30){
        showMsg("User ID should be between 3 and 30 characters.", "error");
        return;
    }
    const userIdRegex = /^[a-zA-Z0-9_]+$/;
    if(!userIdRegex.test(val)){
        showMsg("User ID can only contain letters, numbers, and underscore (_).", "error");
        return;
    }
    const user = JSON.parse(localStorage.getItem("loggedInUser") || "{}");
    user.userId = val;
    localStorage.setItem("loggedInUser", JSON.stringify(user));
    localStorage.setItem("currentUserId", val);
    showMsg("✓ User ID updated successfully.", "success");
}
function changePhoto(){
    const fileInput = document.getElementById("photo-input");
    if(fileInput) fileInput.click();
}
function handlePhoto(event){
    const file = event.target.files[0];
    if(!file) return;
    if(!file.type.startsWith("image/")){
        showMsg("Please select a valid image file.", "error");
        return;
    }
    if(file.size > 5 * 1024 * 1024){
        showMsg("Image size should be less than 5MB.", "error");
        return;
    }
    const reader = new FileReader();
    reader.onload = function(e){
        const user = JSON.parse(localStorage.getItem("loggedInUser") || "{}");
        user.photo = e.target.result;
        localStorage.setItem("loggedInUser", JSON.stringify(user));
        const avatar = document.getElementById("settings-avatar");
        if(avatar){
            avatar.style.backgroundImage = "url(" + e.target.result + ")";
            avatar.style.backgroundSize  = "cover";
            avatar.textContent = "";
        }
        showMsg("✓ Photo updated successfully.", "success");
    };
    reader.onerror = function(){ showMsg("Error reading file.", "error"); };
    reader.readAsDataURL(file);
}
function removePhoto(){
    const user = JSON.parse(localStorage.getItem("loggedInUser") || "{}");
    delete user.photo;
    localStorage.setItem("loggedInUser", JSON.stringify(user));
    const avatar = document.getElementById("settings-avatar");
    if(avatar){
        avatar.style.backgroundImage = "";
        avatar.textContent = user.username ? user.username.charAt(0).toUpperCase() : "?";
    }
    showMsg("✓ Photo removed.", "success");
}
function toggleTheme(){
    const isDark = document.getElementById("dark-toggle").checked;
    localStorage.setItem("darkMode", isDark);
    if(isDark){
        document.body.classList.remove("light-mode");
    }else{
        document.body.classList.add("light-mode");
    }
}
function logout(){
    if(!confirm("Are you sure you want to logout?")) return;
    localStorage.removeItem("loggedInUser");
    localStorage.removeItem("currentUserId");
    window.location.href = "Login.html";
}
function deleteAccount(){
    if(!confirm("Are you sure? This will permanently delete your account and all data.")) return;
    if(!confirm("⚠️ This action cannot be undone. Are you absolutely sure?")) return;
    const currentUserId = localStorage.getItem("currentUserId");
    fetch("http://localhost:8080/api/user/delete", {
        method:  "POST",
        headers: {"Content-Type": "application/json"},
        body:    JSON.stringify({userId: currentUserId})
    })
    .then(function(response){
        if(response.ok){
            localStorage.removeItem("loggedInUser");
            localStorage.removeItem("currentUserId");
            localStorage.removeItem("savedMessagesChatId");
            showMsg("✓ Account deleted.", "success");
            setTimeout(function(){ window.location.href = "Login.html"; }, 1000);
        }else{
            showMsg("Failed to delete account.", "error");
        }
    })
    .catch(function(){ showMsg("Cannot connect to server.", "error"); });
}
function showMsg(msg, type){
    const el = document.getElementById("error-msg");
    if(!el) return;
    el.textContent = msg;
    if(type === "success"){
        el.style.color = "#00cc55";
    }else if(type === "error"){
        el.style.color = "#ff4444";
    }else{
        el.style.color = "#ffaa00";
    }
    setTimeout(function(){ el.textContent = ""; }, 4000);
}
function goBack(){
    window.location.href = "Home.html";
}
window.addEventListener("DOMContentLoaded", init);
