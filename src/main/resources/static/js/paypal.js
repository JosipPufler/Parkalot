// /js/paypal.js

$(function () {
    const permitModal = $('#permit-modal');

    const confirmButton = document.getElementById('confirm-permit');
    const permitDetailsSection =
        document.getElementById('permit-details-section');
    const paypalPaymentSection =
        document.getElementById('paypal-payment-section');
    const paypalButtonContainer =
        document.getElementById('paypal-button-container');
    const permitMessage = document.getElementById('permit-message');

    let paypalOrderId = null;
    let paypalButtonsRendered = false;

    // Read the CSRF token if CookieCsrfTokenRepository is configured.
    function getRequestHeaders() {
        const headers = {
            'Content-Type': 'application/json'
        };

        const csrfCookie = document.cookie
            .split('; ')
            .find(cookie => cookie.startsWith('XSRF-TOKEN='));

        if (csrfCookie) {
            headers['X-XSRF-TOKEN'] = decodeURIComponent(
                csrfCookie.substring('XSRF-TOKEN='.length)
            );
        }

        return headers;
    }

    function showMessage(message, isError = true) {
        permitMessage.textContent = message;
        permitMessage.classList.toggle('text-danger', isError);
        permitMessage.classList.toggle('text-success', !isError);
    }

    function showPaymentSection() {
        permitDetailsSection.classList.add('d-none');
        paypalPaymentSection.classList.remove('d-none');
        confirmButton.classList.add('d-none');

        showMessage('Your permit request was created. Complete payment below.', false);

        // Render the buttons only once. Their createOrder callback reads
        // the current paypalOrderId whenever the buyer clicks PayPal.
        if (!paypalButtonsRendered) {
            renderPayPalButtons();
        }
    }

    function renderPayPalButtons() {
        if (!window.paypal) {
            showMessage('PayPal could not be loaded. Please refresh the page.');
            return;
        }

        paypal.Buttons({
            createOrder: function () {
                if (!paypalOrderId) {
                    return Promise.reject(
                        new Error('No pending PayPal order exists.')
                    );
                }

                // The backend has already created this order.
                // Do not create another order here.
                return paypalOrderId;
            },

            onApprove: async function (data) {
                try {
                    showMessage('Verifying your payment...', false);

                    const response = await fetch(
                        '/rest/payment/permit/verify-order',
                        {
                            method: 'POST',
                            headers: getRequestHeaders(),
                            body: JSON.stringify({
                                orderId: data.orderID
                            })
                        }
                    );

                    if (!response.ok) {
                        const errorText = await response.text();

                        throw new Error(
                            errorText || 'Payment verification failed.'
                        );
                    }

                    // Your backend should capture the order, verify that
                    // the capture is COMPLETED, check its amount and currency,
                    // and mark the associated permit as paid.

                } catch (error) {
                    console.error('Payment verification failed:', error);

                    showMessage(
                        'Payment could not be verified. ' +
                        'Please contact support before attempting to pay again.'
                    );
                }
            },

            onCancel: function () {
                showMessage(
                    'Payment was cancelled. Your pending permit has not been activated.'
                );
            },

            onError: function (error) {
                console.error('PayPal error:', error);

                showMessage(
                    'An error occurred during payment. Please try again.'
                );
            }

        }).render('#paypal-button-container')
            .then(() => {
                paypalButtonsRendered = true;
            })
            .catch(error => {
                console.error('Could not render PayPal buttons:', error);
                showMessage('Could not load PayPal checkout. Please try again.');
            });
    }

    confirmButton.addEventListener('click', async function () {
        let selectedVehicle = window.vehicles.getSelectedVehicle();
        let selectedArea = window.areas.getSelectedArea();

        if (!selectedVehicle || !selectedArea) {
            showMessage('Select a vehicle and parking area first.');
            return;
        }

        const startDate =
            document.getElementById('permit-start-date').value;

        if (!startDate) {
            showMessage('Please select a permit start date.');
            return;
        }

        const permitRequest = {
            vehicleId: permitModal.data("vehicle-id"),
            areaId: permitModal.data("area-id"),
            startDate: startDate,
            permitDuration:
            document.getElementById('permit-duration').value
        };

        confirmButton.disabled = true;
        showMessage('Creating your permit request...', false);

        try {
            // This endpoint creates the pending permit AND the PayPal order.
            const response = await fetch('/rest/permits', {
                method: 'POST',
                headers: getRequestHeaders(),
                body: JSON.stringify(permitRequest)
            });

            if (!response.ok) {
                const errorText = await response.text();

                throw new Error(
                    errorText || 'Could not create the permit request.'
                );
            }

            const result = await response.json();

            if (!result.orderId) {
                throw new Error(
                    'The server did not return a PayPal order ID.'
                );
            }

            // Keep only the order ID. The pending permit is already stored
            // in the database by the backend.
            paypalOrderId = result.orderId;

            showPaymentSection();

        } catch (error) {
            console.error('Could not create permit:', error);

            showMessage(
                error.message || 'Could not create the permit request.'
            );

        } finally {
            confirmButton.disabled = false;
        }
    });

    // Reset the UI if the user closes the modal before paying.
    permitModal.on('hidden.bs.modal', function () {
        paypalOrderId = null;

        permitDetailsSection.classList.remove('d-none');
        paypalPaymentSection.classList.add('d-none');
        confirmButton.classList.remove('d-none');
        confirmButton.disabled = false;

        showMessage('');

        // Keep the PayPal buttons rendered for the next order.
        // createOrder will use the new order ID.
    });
});