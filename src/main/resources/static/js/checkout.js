(() => {
  document.querySelectorAll('[data-payment-confirmation]').forEach(form => form.addEventListener('submit', event => {
    if (!window.confirm('Xác nhận ghi nhận đơn thuê này đã thanh toán?')) { event.preventDefault(); return; }
    const button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    button.textContent = 'Đang xác nhận…';
  }));
})();
