(() => {
  document.querySelectorAll('[data-payment-confirmation]').forEach(form => form.addEventListener('submit', event => {
    if (!window.confirm('Xác nhận ghi nhận đơn thuê này đã thanh toán?')) { event.preventDefault(); return; }
    const button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    button.textContent = 'Đang xác nhận…';
  }));
  document.querySelectorAll('[data-copy-target]').forEach(button => button.addEventListener('click', async () => {
    const value = button.previousElementSibling?.dataset.copyValue;
    if (!value || !navigator.clipboard) return;
    try {
      await navigator.clipboard.writeText(value);
      const original = button.textContent;
      button.textContent = 'Đã sao chép';
      setTimeout(() => { button.textContent = original; }, 1200);
    } catch (_) { /* Clipboard access can be unavailable outside HTTPS. */ }
  }));
})();
