(function () {
    const form = document.getElementById("login-form");
    if (!form) {
        return;
    }

    const usernameInput = document.getElementById("username");
    const passwordInput = document.getElementById("password");
    const submitButton = document.getElementById("submit-login");
    const togglePasswordButton = document.querySelector(".toggle-password");
    const capsLockWarning = document.getElementById("caps-lock-warning");

    if (togglePasswordButton && passwordInput) {
        const updateToggleVisibility = function () {
            const hasPassword = passwordInput.value.length > 0;
            const isFocused = document.activeElement === passwordInput;
            togglePasswordButton.hidden = !(hasPassword && isFocused);
        };

        updateToggleVisibility();

        passwordInput.addEventListener("input", updateToggleVisibility);
        passwordInput.addEventListener("focus", updateToggleVisibility);
        passwordInput.addEventListener("blur", updateToggleVisibility);

        togglePasswordButton.addEventListener("click", function () {
            const isPassword = passwordInput.type === "password";
            passwordInput.type = isPassword ? "text" : "password";
            togglePasswordButton.classList.toggle("is-visible", isPassword);
            togglePasswordButton.setAttribute(
                "aria-label",
                isPassword ? "Ocultar senha" : "Mostrar senha"
            );
        });
    }

    if (passwordInput && capsLockWarning) {
        let capsLockOn = false;

        const setCapsWarning = function (isOn) {
            capsLockOn = isOn;
            capsLockWarning.hidden = !capsLockOn;
        };

        const isLetterKey = function (key) {
            return typeof key === "string" && key.length === 1 && /[a-z]/i.test(key);
        };

        const detectCapsLock = function (event) {
            if (typeof event.getModifierState === "function") {
                return event.getModifierState("CapsLock");
            }

            return false;
        };

        const updateCapsWarning = function (event) {
            if (document.activeElement !== passwordInput) {
                capsLockWarning.hidden = true;
                return;
            }

            if (event.key === "CapsLock") {
                setCapsWarning(!capsLockOn);
                return;
            }

            if (!isLetterKey(event.key)) {
                return;
            }

            setCapsWarning(detectCapsLock(event));
        };

        setCapsWarning(false);

        passwordInput.addEventListener("keydown", updateCapsWarning);
        passwordInput.addEventListener("focus", function () {
            setCapsWarning(false);
        });

        passwordInput.addEventListener("blur", function () {
            setCapsWarning(false);
        });
    }

    form.addEventListener("submit", function (event) {
        const username = usernameInput ? usernameInput.value.trim() : "";
        const password = passwordInput ? passwordInput.value : "";

        if (usernameInput) {
            usernameInput.classList.toggle("input-error", username.length < 3);
        }
        if (passwordInput) {
            passwordInput.classList.toggle("input-error", password.length < 3);
        }

        if (username.length < 3 || password.length < 3) {
            event.preventDefault();
            return;
        }

        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = "Entrando...";
        }
    });
})();
