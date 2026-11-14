
const originalFetch = window.fetch;
let refreshPromise = null;

function showRequestError(response, message) {
    if (typeof showToast !== "function") {
        console.error(message);
        return;
    }

    showToast(
        "danger",
        response.title || "Request failed",
        message
    );
}

async function handleFailedResponse(response) {
    if (response.ok) {
        return;
    }

    try {
        const problem = await response.clone().json();

        const message = problem.detail
            || problem.message
            || problem.error
            || response.statusText
            || "An unexpected error occurred.";

        showRequestError(problem, message);
    } catch {
        showRequestError(
            {
                title: response.statusText || "Request failed"
            },
            response.statusText || "An unexpected error occurred."
        );
    }
}

window.fetch = async function (...args) {
    const [url, options = {}] = args;

    const headers = new Headers(options.headers || {});

    const newOptions = {
        ...options,
        headers,
        credentials: options.credentials ?? "same-origin"
    };

    let response = await originalFetch(url, newOptions);

    if (
        (response.status === 401 || response.status === 403)
        && typeof url === "string"
        && url.startsWith("/rest/")
    ) {
        if (!refreshPromise) {
            refreshPromise = originalFetch("/rest/auth/refresh", {
                method: "POST",
                credentials: "same-origin"
            }).finally(() => {
                refreshPromise = null;
            });
        }

        const refreshResponse = await refreshPromise;

        if (!refreshResponse.ok) {
            window.location.href = "/mvc/public/auth/login";
            return response;
        }

        response = await originalFetch(url, newOptions);
    }

    if (
        !response.ok
        && typeof url === "string"
        && url.startsWith("/rest/")
        && response.status !== 401
        && response.status !== 403
    ) {
        await handleFailedResponse(response);
    }

    return response;
};

$(document).ajaxError(function (event, xhr, settings) {
    if (!settings.url || !settings.url.startsWith("/rest/")) {
        return;
    }

    if (xhr.status === 401 || xhr.status === 403) {
        return;
    }

    const problem = xhr.responseJSON || {};

    const message = problem.detail
        || problem.message
        || problem.error
        || xhr.statusText
        || "An unexpected error occurred.";

    showRequestError(
        {
            title: problem.title || "Request failed"
        },
        message
    );
});

$(".select2").select2({
    width: "100%",
    placeholder: window.i18n.common.selectOption,
    allowClear: true
});
