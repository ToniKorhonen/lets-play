// Products Management JavaScript

const currentUserData = window.currentUser || null;
let jwtToken = null;

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    // Get JWT token if user is logged in
    if (currentUserData) {
        fetchJwtToken().then(() => {
            loadProducts();
        });
    } else {
        loadProducts();
    }

    // Create product form handler
    const createForm = document.getElementById('createProductForm');
    if (createForm) {
        createForm.addEventListener('submit', handleCreateProduct);
    }

    // Edit product form handler
    const editForm = document.getElementById('editProductForm');
    if (editForm) {
        editForm.addEventListener('submit', handleUpdateProduct);
    }

    // Modal close handler
    const modal = document.getElementById('editModal');
    const closeBtn = document.querySelector('.close');
    if (closeBtn) {
        closeBtn.onclick = function() {
            modal.style.display = 'none';
        }
    }
    window.onclick = function(event) {
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    }
});

// Fetch JWT token for authenticated user
async function fetchJwtToken() {
    try {
        const response = await fetch('/web/token');
        if (response.ok) {
            const data = await response.json();
            jwtToken = data.token;
            console.log('JWT token obtained successfully');
        } else {
            console.error('Failed to obtain JWT token');
        }
    } catch (error) {
        console.error('Error fetching JWT token:', error);
    }
}

// Show message to user
function showMessage(message, isError = false) {
    const container = document.getElementById('message-container');
    container.innerHTML = `<div class="${isError ? 'error-message' : 'success-message'}">${message}</div>`;
    setTimeout(() => {
        container.innerHTML = '';
    }, 5000);
}

// Load all products
async function loadProducts() {
    try {
        const response = await fetch('/api/products');
        if (!response.ok) {
            throw new Error('Failed to load products');
        }
        const products = await response.json();
        displayProducts(products);
    } catch (error) {
        console.error('Error loading products:', error);
        document.getElementById('products-container').innerHTML =
            '<p class="error-message">Failed to load products. Please try again later.</p>';
    }
}

// Display products in the UI
function displayProducts(products) {
    const container = document.getElementById('products-container');

    if (products.length === 0) {
        container.innerHTML = '<p>No products available yet. Be the first to create one!</p>';
        return;
    }

    container.innerHTML = products.map(product => {
        const isOwner = currentUserData && product.userId === currentUserData.email;
        const isAdmin = currentUserData && currentUserData.role === 'ADMIN';
        const canEdit = isOwner || isAdmin;

        return `
            <div class="product-card">
                <h3>${escapeHtml(product.name)}</h3>
                <p><strong>Description:</strong> ${escapeHtml(product.description || 'No description')}</p>
                <p><strong>Price:</strong> $${product.price.toFixed(2)}</p>
                <p><strong>Owner:</strong> ${escapeHtml(product.ownerName)}</p>
                ${canEdit ? `
                    <div class="product-actions">
                        <button class="btn-edit" onclick="openEditModal('${product.id}', '${escapeHtml(product.name)}', '${escapeHtml(product.description || '')}', ${product.price})">Edit</button>
                        <button class="btn-delete" onclick="deleteProduct('${product.id}')">Delete</button>
                    </div>
                ` : ''}
            </div>
        `;
    }).join('');
}

// Handle product creation
async function handleCreateProduct(event) {
    event.preventDefault();

    if (!jwtToken) {
        showMessage('Authentication error. Please refresh the page and try again.', true);
        return;
    }

    const formData = {
        name: document.getElementById('productName').value,
        description: document.getElementById('productDescription').value,
        price: parseFloat(document.getElementById('productPrice').value)
    };

    try {
        const response = await fetch('/api/products', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${jwtToken}`
            },
            body: JSON.stringify(formData)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to create product');
        }

        showMessage('Product created successfully!');
        document.getElementById('createProductForm').reset();
        loadProducts();
    } catch (error) {
        console.error('Error creating product:', error);
        showMessage(error.message, true);
    }
}

// Open edit modal
function openEditModal(id, name, description, price) {
    document.getElementById('editProductId').value = id;
    document.getElementById('editProductName').value = name;
    document.getElementById('editProductDescription').value = description;
    document.getElementById('editProductPrice').value = price;
    document.getElementById('editModal').style.display = 'block';
}

// Handle product update
async function handleUpdateProduct(event) {
    event.preventDefault();

    if (!jwtToken) {
        showMessage('Authentication error. Please refresh the page and try again.', true);
        return;
    }

    const id = document.getElementById('editProductId').value;
    const formData = {
        name: document.getElementById('editProductName').value,
        description: document.getElementById('editProductDescription').value,
        price: parseFloat(document.getElementById('editProductPrice').value)
    };

    try {
        const response = await fetch(`/api/products/${id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${jwtToken}`
            },
            body: JSON.stringify(formData)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to update product');
        }

        showMessage('Product updated successfully!');
        document.getElementById('editModal').style.display = 'none';
        loadProducts();
    } catch (error) {
        console.error('Error updating product:', error);
        showMessage(error.message, true);
    }
}

// Delete product
async function deleteProduct(id) {
    if (!confirm('Are you sure you want to delete this product?')) {
        return;
    }

    if (!jwtToken) {
        showMessage('Authentication error. Please refresh the page and try again.', true);
        return;
    }

    try {
        const response = await fetch(`/api/products/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${jwtToken}`
            }
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to delete product');
        }

        showMessage('Product deleted successfully!');
        loadProducts();
    } catch (error) {
        console.error('Error deleting product:', error);
        showMessage(error.message, true);
    }
}

// Escape HTML to prevent XSS
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

