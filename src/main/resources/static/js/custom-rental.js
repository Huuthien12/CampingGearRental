(() => {
  const maxItems = 10, catalog = document.getElementById('equipment-catalog');
  if (!catalog) return;
  const selected = new Map(), selectedItems = document.getElementById('selected-items'), selectedCount = document.getElementById('selected-count'), selectedTypes = document.getElementById('selected-types'), selectedQuantity = document.getElementById('selected-quantity'), message = document.getElementById('selection-message');
  const equipmentById = new Map([...catalog.querySelectorAll('.equipment-card')].map(card => [card.dataset.equipmentId, card]));
  const setMessage = text => { message.textContent = text; };
  const render = () => {
    selectedItems.replaceChildren();
    if (!selected.size) selectedItems.innerHTML = '<p class="empty-selection">Chưa có thiết bị nào được chọn.</p>';
    let quantityTotal = 0;
    [...selected.values()].forEach((item, index) => {
      quantityTotal += item.quantity;
      const row = document.createElement('div'); row.className = 'selected-item';
      row.innerHTML = '<span class="selected-item-icon" aria-hidden="true">🧰</span><div><strong></strong><small></small><span class="selected-price"></span></div><div class="quantity-controls"><button type="button" aria-label="Giảm số lượng">−</button><output></output><button type="button" aria-label="Tăng số lượng">+</button><button type="button" class="remove-item" aria-label="Xóa thiết bị">×</button></div>';
      row.querySelector('strong').textContent = item.name; row.querySelector('small').textContent = item.id; row.querySelector('.selected-price').textContent = `${item.price} đ / ngày`; row.querySelector('output').textContent = item.quantity;
      const buttons = row.querySelectorAll('button'); buttons[0].disabled = item.quantity === 1;
      buttons[0].addEventListener('click', () => { item.quantity--; render(); });
      buttons[1].addEventListener('click', () => { if (item.quantity < item.stock) { item.quantity++; render(); } else setMessage(`Số lượng tối đa hiện có của ${item.name} là ${item.stock}.`); });
      buttons[2].addEventListener('click', () => { selected.delete(item.id); setMessage(''); render(); });
      const id = document.createElement('input'); id.type = 'hidden'; id.name = `items[${index}].equipmentId`; id.value = item.id;
      const quantity = document.createElement('input'); quantity.type = 'hidden'; quantity.name = `items[${index}].quantity`; quantity.value = item.quantity;
      row.append(id, quantity); selectedItems.append(row);
    });
    selectedCount.textContent = `${selected.size} / ${maxItems} món`; selectedTypes.textContent = selected.size; selectedQuantity.textContent = quantityTotal;
    catalog.querySelectorAll('.add-equipment').forEach(button => { const card = button.closest('.equipment-card'); button.disabled = card.dataset.available !== 'true' || selected.has(card.dataset.equipmentId); });
  };
  const add = card => { const id = card.dataset.equipmentId; if (selected.has(id)) return; if (selected.size === maxItems) { setMessage(`Mỗi đơn chỉ được chọn tối đa ${maxItems} loại thiết bị.`); return; } selected.set(id, { id, name: card.dataset.name, price: card.dataset.price, stock: Number(card.dataset.stock), quantity: 1 }); setMessage(''); render(); };
  catalog.addEventListener('click', event => { const button = event.target.closest('.add-equipment'); if (button) add(button.closest('.equipment-card')); });
  document.querySelectorAll('.category-filter').forEach(button => button.addEventListener('click', () => { const filter = button.dataset.filter; document.querySelectorAll('.category-filter').forEach(item => item.classList.toggle('active', item === button)); catalog.querySelectorAll('.equipment-card').forEach(card => { card.hidden = filter !== 'all' && card.dataset.category !== filter; }); }));
  document.querySelectorAll('#existing-items span').forEach(item => { const card = equipmentById.get(item.dataset.equipmentId); if (card && item.dataset.quantity) selected.set(item.dataset.equipmentId, { id: item.dataset.equipmentId, name: card.dataset.name, price: card.dataset.price, stock: Number(card.dataset.stock), quantity: Number(item.dataset.quantity) }); });
  render();
})();
