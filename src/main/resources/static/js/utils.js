function showToast(type, title, message) {
    const toast = $("#toast");

    toast.removeClass("bg-success bg-danger text-white");

    if (type === "success") {
        toast.addClass("bg-success text-white");
    } else {
        toast.addClass("bg-danger text-white");
    }

    $("#toastTitle").text(title);
    $("#toastBody").text(message);

    toast.toast("show");
}

function formatDate(value) {
    if (!value) return "N/A";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) return value;

    return date.toLocaleString();
}