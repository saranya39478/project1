
const API_URL = "/api/messages";
const SHARE_API_URL = "/api/shares";

const surpriseBtn = document.getElementById("surpriseBtn");
const surpriseMessage = document.getElementById("surpriseMessage");
const messageForm = document.getElementById("messageForm");
const messageList = document.getElementById("messageList");
const statusText = document.getElementById("status");
const passwordGate = document.getElementById("passwordGate");
const mainContent = document.getElementById("mainContent");
const shareTokenMatch = window.location.pathname.match(
    /^\/share\/([A-Za-z0-9_-]{43})$/
);

// Floating heart animation
function createHeart() {
    const heart = document.createElement("span");
    heart.className = "floating-heart";
    heart.textContent = ["💗", "💕", "💖", "❤️"][
        Math.floor(Math.random() * 4)
    ];

    heart.style.left = Math.random() * 100 + "vw";
    heart.style.fontSize = 18 + Math.random() * 22 + "px";

    document.getElementById("hearts").appendChild(heart);
    setTimeout(() => heart.remove(), 6000);
}

setInterval(createHeart, 450);

// Surprise button
surpriseBtn.addEventListener("click", () => {
    surpriseMessage.textContent =
        "You are my favourite person. My world is brighter with you! ❤️🥹";

    for (let i = 0; i < 12; i++) {
        setTimeout(createHeart, i * 100);
    }
});

// Load messages from backend
async function loadMessages() {
    try {
        const response = await fetch(API_URL);

        if (!response.ok) {
            throw new Error(`Could not load messages (HTTP ${response.status})`);
        }

        const messages = await response.json();
        messageList.replaceChildren();

        if (messages.length === 0) {
            messageList.textContent = "No messages yet. Be the first! 💗";
            return;
        }

        messages.forEach((item) => {
            const card = document.createElement("div");
            card.className = "message";

            const author = document.createElement("strong");
            author.textContent = "❤️ " + item.senderName;

            const content = document.createElement("p");
            content.textContent = item.message;

            card.append(author, content);
            messageList.appendChild(card);
        });
    } catch (error) {
        console.error("Could not load messages.", error);
        messageList.textContent =
            "Could not load messages. Check that the application is running and refresh.";
    }
}

// Create a password-protected message link.
messageForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const senderName = document.getElementById("senderName").value.trim();
    const message = document.getElementById("loveMessage").value.trim();
    const password = document.getElementById("sharePasswordToSet").value;

    if (!senderName || !message || password.length < 8) {
        statusText.textContent =
            "Enter your name, message, and a password of at least 8 characters.";
        return;
    }

    const submitButton = messageForm.querySelector(
        'button[type="submit"]'
    );
    submitButton.disabled = true;
    statusText.textContent = "Saving your message...";

    try {
        const response = await fetch(SHARE_API_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ senderName, message, password })
        });

        if (!response.ok) {
            const result = await response.json().catch(() => ({}));
            throw new Error(result.error || `Could not create link (HTTP ${response.status})`);
        }

        const result = await response.json();
        messageForm.reset();
        const shareLink = document.getElementById("shareLink");
        shareLink.value = result.shareUrl;
        document.getElementById("shareResult").hidden = false;
        statusText.textContent = "Your private message link is ready! 💖";
    } catch (error) {
        console.error("Could not create the private message link.", error);
        statusText.textContent = error.message || "Could not create the link. Please try again.";
    } finally {
        submitButton.disabled = false;
    }
});

document.getElementById("copyShareLink").addEventListener("click", async () => {
    try {
        await navigator.clipboard.writeText(document.getElementById("shareLink").value);
        statusText.textContent = "Private link copied. Send the password separately.";
    } catch (error) {
        console.error("Could not copy the private link.", error);
        statusText.textContent = "Copy the link from the box above and send the password separately.";
    }
});

async function openPrivateMessage(token, password) {
    const unlockResponse = await fetch(`${SHARE_API_URL}/${encodeURIComponent(token)}/unlock`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ password })
    });

    if (unlockResponse.status === 401) {
        throw new Error("That password is incorrect. Please try again.");
    }
    if (!unlockResponse.ok) {
        throw new Error(`Could not unlock this link (HTTP ${unlockResponse.status}).`);
    }

    const response = await fetch(`${SHARE_API_URL}/${encodeURIComponent(token)}`);
    if (!response.ok) {
        throw new Error(`Could not load the private message (HTTP ${response.status}).`);
    }

    const sharedMessage = await response.json();
    const card = document.createElement("div");
    card.className = "message";
    const author = document.createElement("strong");
    author.textContent = `❤️ ${sharedMessage.senderName}`;
    const content = document.createElement("p");
    content.textContent = sharedMessage.message;
    card.append(author, content);
    messageList.replaceChildren(card);
    document.getElementById("createShareSection").hidden = true;
    document.getElementById("messagesSection").querySelector("h2").textContent =
        "A Message From the Heart";
}

if (shareTokenMatch) {
    mainContent.hidden = true;
    passwordGate.hidden = false;
    document.getElementById("unlockForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        const input = document.getElementById("sharePassword");
        const unlockStatus = document.getElementById("unlockStatus");
        const submitButton = event.currentTarget.querySelector('button[type="submit"]');
        submitButton.disabled = true;
        unlockStatus.textContent = "Checking password...";

        try {
            await openPrivateMessage(shareTokenMatch[1], input.value);
            document.documentElement.classList.add("message-open");
            passwordGate.hidden = true;
            mainContent.hidden = false;
        } catch (error) {
            console.error("Could not open the private message.", error);
            unlockStatus.textContent = error.message || "Could not open this link. Please try again.";
        } finally {
            submitButton.disabled = false;
        }
    });
} else {
    loadMessages();
}
