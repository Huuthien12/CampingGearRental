(() => {
  document.querySelectorAll('[data-customer-picker]').forEach(picker => {
    const search = picker.querySelector('#customer-picker-search');
    const selectedId = picker.querySelector('[data-customer-id]');
    const selection = picker.querySelector('[data-customer-selection]');
    const options = [...picker.querySelectorAll('.customer-picker-option')];
    const select = option => {
      selectedId.value = option.dataset.customerId;
      selection.textContent = `${option.querySelector('strong').textContent} — ${option.querySelector('span').textContent}`;
      options.forEach(item => item.setAttribute('aria-selected', item === option));
    };
    options.forEach(option => {
      option.addEventListener('click', () => select(option));
      if (option.dataset.customerId === selectedId.value) select(option);
    });
    search?.addEventListener('input', () => {
      const query = search.value.trim().toLocaleLowerCase('vi-VN');
      options.forEach(option => { option.hidden = !option.dataset.customerSearch.toLocaleLowerCase('vi-VN').includes(query); });
    });
    search?.addEventListener('keydown', event => {
      if (event.key !== 'ArrowDown') return;
      const first = options.find(option => !option.hidden);
      if (first) { event.preventDefault(); first.focus(); }
    });
  });
})();
