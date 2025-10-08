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

// Admin functionality - Load all users
async function loadAllUsers() {
    if (!currentUserData || currentUserData.role !== 'ADMIN') {
        showMessage('You must be an admin to view all users', true);
        return;
    }

    const usersList = document.getElementById('users-list');
    const usersContainer = document.getElementById('users-container');
    const refreshBtn = document.getElementById('refreshUsersBtn');

    usersList.style.display = 'block';
    refreshBtn.style.display = 'inline-block';
    usersContainer.innerHTML = '<p>Loading users...</p>';

    try {
        const response = await fetch('/web/admin/users', {
            method: 'GET',
            headers: {
                [csrfHeader]: csrfToken
            }
        });

        if (!response.ok) {
            throw new Error('Failed to load users. Status: ' + response.status);
        }

        const users = await response.json();
        displayUsers(users);
    } catch (error) {
        console.error('Error loading users:', error);
        usersContainer.innerHTML = `<p class="error-message">Error loading users: ${escapeHtml(error.message)}</p>`;
    }
}

function displayUsers(users) {
    const usersContainer = document.getElementById('users-container');

    if (!users || users.length === 0) {
        usersContainer.innerHTML = '<p>No users found.</p>';
        return;
    }

    let html = '';
    users.forEach(user => {
        const roleClass = user.role === 'ADMIN' ? 'role-admin' : 'role-user';
        const roleName = user.role === 'ADMIN' ? 'Admin' : 'User';

        html += `
            <div class="user-card">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <div>
                        <h4 style="margin: 0 0 8px 0;">${escapeHtml(user.name)}</h4>
                        <p style="margin: 4px 0; color: #666;">Email: ${escapeHtml(user.email)}</p>
                        <p style="margin: 4px 0; color: #666;">ID: ${escapeHtml(user.id)}</p>
                        <span class="role-badge ${roleClass}">${roleName}</span>
                    </div>
                    <div>
                        ${user.id !== currentUserData.id ? `
                            <button class="btn-delete-account" onclick="deleteUser('${escapeHtml(user.id)}', '${escapeHtml(user.name)}')">
                                Delete User
                            </button>
                        ` : '<em>Current user</em>'}
                    </div>
                </div>
            </div>
        `;
    });

    usersContainer.innerHTML = html;
}

async function deleteUser(userId, userName) {
    if (!confirm(`Are you sure you want to delete user "${userName}"? This action cannot be undone.`)) {
        return;
    }

    try {
        const response = await fetch(`/web/admin/users/${userId}`, {
            method: 'DELETE',
            headers: {
                [csrfHeader]: csrfToken
            }
        });

        if (response.ok) {
            showMessage(`User "${userName}" deleted successfully`, false);
            refreshUsers();
        } else {
            const error = await response.json().catch(() => ({ message: 'Failed to delete user' }));
            throw new Error(error.message || error.error || 'Failed to delete user');
        }
    } catch (error) {
        console.error('Error deleting user:', error);
        showMessage('Error: ' + error.message, true);
    }
}

function refreshUsers() {
    loadAllUsers();
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
