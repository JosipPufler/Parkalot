const originalFetch = window.fetch;
let refreshPromise = null;

window.fetch = async function (...args) {

    const [url, options = {}] = args;

    const headers = new Headers(options.headers || {});

    const newOptions = {
        ...options,
        headers,
        credentials: options.credentials ?? 'same-origin'
    };

    let response = await originalFetch(url, newOptions);

    if (response.status !== 401 && response.status !== 403) {
        return response;
    }

    if (!url.startsWith('/rest/')) {
        return response;
    }

    if (!refreshPromise) {
        refreshPromise = originalFetch('/rest/auth/refresh', {
            method: 'POST',
            credentials: 'same-origin',
        }).finally(() => {
            refreshPromise = null;
        });
    }

    const refreshResponse = await refreshPromise;

    if (!refreshResponse.ok) {
        window.location.href = '/mvc/public/auth/login';
        return response;
    }

    return originalFetch(...args);
};