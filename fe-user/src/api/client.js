// Minimal API client skeleton — replace fetch with preferred HTTP library
const BASE_URL = process.env.API_BASE_URL || '/api';

async function request(path, options = {}) {
    const url = `${BASE_URL}${path}`;
    const res = await fetch(url, Object.assign({
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
    }, options));
    if (!res.ok) {
        const text = await res.text();
        const err = new Error(`API error ${res.status}: ${text}`);
        err.status = res.status;
        throw err;
    }
    return res.status === 204 ? null : res.json();
}

export async function getProducts(query = '') {
    return request(`/products${query ? `?${query}` : ''}`);
}

export async function getProduct(id) {
    return request(`/products/${id}`);
}

export async function postLogin(payload) {
    return request('/auth/login', { method: 'POST', body: JSON.stringify(payload) });
}

export default { getProducts, getProduct, postLogin };
