(function applyThemeOnLoad(){
    const darkSetting = localStorage.getItem("darkMode");
    if(darkSetting === "false"){
        document.body.classList.add("light-mode");
    }
    else{
        document.body.classList.remove("light-mode");
    }
})();
function login() {
    const userIdInput = document.getElementById("username").value.trim();
    const passwordInput = document.getElementById("password").value.trim();
    const errorEl = document.getElementById("error-msg");
    if(!errorEl) return;
    if(userIdInput === "") {
        showError("Please enter your User ID.");
        return;
    }
    if(passwordInput === "") {
        showError("Please enter your password.");
        return;
    }
    // userId باید بین 3 تا 30 کاراکتر باشه
    if(userIdInput.length < 3 || userIdInput.length > 30) {
        showError("User ID should be between 3 and 30 characters.");
        return;
    }
    fetch("http://localhost:8080/api/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId: userIdInput, password: passwordInput })
    })
    .then(response => response.json().then(data => ({ status: response.status, body: data })))
    .then(res => {
        if(res.status === 200) {
            showError("");
            localStorage.setItem("currentUserId", userIdInput);
            const user = {
                username: res.body.username || userIdInput,
                userId: userIdInput,
                photo: null
            };
            localStorage.setItem("loggedInUser", JSON.stringify(user));
            window.location.href = "Home.html";
        } else if(res.status === 429) {
            // rate limit یا قفل شدن اکانت
            showError(res.body.error || "Account temporarily locked.");
        } else {
            showError(res.body.error || "Login failed. Please check your credentials.");
        }
    })
    .catch(err => {
        showError("Cannot connect to the backend server. Make sure it's running on port 8080.");
    });
}
function showError(message) {
    const errorEl = document.getElementById("error-msg");
    if(errorEl) errorEl.textContent = message;
}
document.addEventListener("keydown", function(event) {
    if(event.key === "Enter") login();
});
