// Home page JavaScript - Account deletion functionality

// Initialize variables (will be set from HTML)
const currentUserData = window.currentUser || null;
const csrfToken = window.csrfToken || null;
const csrfHeader = window.csrfHeader || null;

async function confirmDeleteAccount() {
    if (!currentUserData || !currentUserData.id) {
        showMessage('User information not available. Please refresh the page.', true);
        return;
    }

    try {
        // Appeler le nouvel endpoint qui invalide la session puis supprime le compte
        const response = await fetch('/web/delete-account', {
            method: 'POST',
            headers: {
                [csrfHeader]: csrfToken
            }
        });

        if (response.ok) {
            // Redirection immédiate vers la page d'accueil
            window.location.href = '/';
        } else {
            const error = await response.json().catch(() => ({ message: 'Failed to delete account' }));
            throw new Error(error.message || error.error || 'Failed to delete account');
        }
    } catch (error) {
        console.error('Error deleting account:', error);
        showMessage('Error: ' + error.message, true);
    }
}

function showMessage(message, isError) {
    const container = document.getElementById('message-container');
    container.innerHTML = `<div class="${isError ? 'error-message' : 'success-message'}">${escapeHtml(message)}</div>`;
    setTimeout(() => {
        container.innerHTML = '';
    }, 5000);
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

