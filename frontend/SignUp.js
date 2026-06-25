(function applyThemeOnLoad(){
    const darkSetting = localStorage.getItem("darkMode");
    if(darkSetting === "false"){
        document.body.classList.add("light-mode");
    }
    else{
        document.body.classList.remove("light-mode");
    }
})();
function register(){
    const username = document.getElementById("username").value.trim();
    const userId = document.getElementById("userId").value.trim();
    const password = document.getElementById("password").value;
    const repeatPassword = document.getElementById("repeatPassword").value;
    if(username === "" || userId === "" || password === "" || repeatPassword === ""){
        showError("Please fill in all fields.");
        return;
    }
    const userIdRegex = /^[a-zA-Z0-9_]+$/;
    if(!userIdRegex.test(userId)){
        showError("User ID can only contain letters, numbers, and underscore (_).");
        return;
    }
    if(password.length < 8){
        showError("Password must be at least 8 characters.");
        return;
    }
    if(password.toLowerCase().includes(username.toLowerCase())){
        showError("Password cannot contain your username.");
        return;
    }
    // پسورد باید uppercase، lowercase، عدد و کاراکتر خاص داشته باشه
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*]).{8,}$/;
    if(!passwordRegex.test(password)){
        showError("Password must contain uppercase, lowercase, number, and special character (!@#$%^&*).");
        return;
    }
    if(password !== repeatPassword){
        showError("Passwords do not match.");
        return;
    }
    fetch("http://localhost:8080/api/signup", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            username: username,
            userId: userId,
            password: password,
            confirmPassword: repeatPassword
        })
    })
    .then(response => response.json().then(data => ({ status: response.status, body: data })))
    .then(res => {
        if(res.status === 201 || res.status === 200){
            showError("");
            alert("Registration successful! Redirecting to login...");
            window.location.href = "Login.html";
        } else {
            showError(res.body.error || "Registration failed.");
        }
    })
    .catch(err => {
        showError("Cannot connect to the backend server.");
        console.error(err);
    });
}
function showError(message){
    document.getElementById("error-msg").textContent = message;
}
document.addEventListener("keydown", function(event){
    if(event.key === "Enter"){
        register();
    }
});
