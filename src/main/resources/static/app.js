const API_BASE = '/reservation';

const state = {
  bookings: [],
};

const $ = (sel, ctx = document) => ctx.querySelector(sel);

const form = $('#booking-form');
const feedback = $('#feedback');
const tbody = $('#bookings-body');
const emptyMsg = $('#bookings-empty');

function showFeedback(message, type) {
  feedback.className = 'feedback feedback--visible';
  feedback.innerHTML = `<p class="feedback__msg feedback__msg--${type}">${message}</p>`;
  setTimeout(() => {
    feedback.classList.remove('feedback--visible');
  }, 4000);
}

function formatDate(iso) {
  if (!iso) return '-';
  return iso.replace('T', ' ').substring(0, 16);
}

function mapBooking(r) {
  return {
    id: r.id,
    spaceId: r.space ? r.space.id : null,
    spaceName: r.space ? r.space.name : null,
    startDate: r.startDate,
    endDate: r.endDate,
    userEmail: r.user ? r.user.email : null,
    status: r.status,
  };
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

function renderBookings() {
  tbody.innerHTML = '';

  if (state.bookings.length === 0) {
    emptyMsg.classList.add('bookings-empty--visible');
    return;
  }

  emptyMsg.classList.remove('bookings-empty--visible');

  state.bookings.forEach((b, index) => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${escapeHtml(b.spaceName || String(b.spaceId))}</td>
      <td>${escapeHtml(formatDate(b.startDate))}</td>
      <td>${escapeHtml(formatDate(b.endDate))}</td>
      <td>${escapeHtml(b.userEmail || '-')}</td>
      <td><button class="table__delete" data-index="${index}">Eliminar</button></td>
    `;
    tbody.appendChild(tr);
  });
}

async function loadBookings() {
  try {
    const res = await fetch(`${API_BASE}/all`);
    if (!res.ok) throw new Error('Error al cargar reservas');
    const data = await res.json();
    state.bookings = data.map(mapBooking);
  } catch {
    state.bookings = [];
    showFeedback('No se pudieron cargar las reservas del servidor.', 'error');
  } finally {
    renderBookings();
  }
}

async function createBooking(data) {
  const res = await fetch(`${API_BASE}/add`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error('Error al crear la reserva');
  return res.json();
}

async function deleteBooking(id) {
  const res = await fetch(`${API_BASE}/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error('Error al eliminar la reserva');
}

function getFormData() {
  return {
    spaceId: parseInt($('#space-id').value, 10),
    startDate: $('#start-date').value + 'T00:00:00',
    endDate: $('#end-date').value + 'T00:00:00',
    userEmail: $('#user-email').value.trim(),
  };
}

function validateForm(data) {
  if (!data.spaceId || isNaN(data.spaceId)) return 'El ID del espacio debe ser un número válido.';
  if (!data.startDate) return 'La fecha de inicio es obligatoria.';
  if (!data.endDate) return 'La fecha de fin es obligatoria.';
  if (!data.userEmail) return 'El correo electrónico es obligatorio.';
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.userEmail)) {
    return 'El formato del correo no es válido.';
  }
  if (data.endDate < data.startDate) {
    return 'La fecha de fin debe ser posterior o igual a la fecha de inicio.';
  }
  return null;
}

function resetForm() {
  form.reset();
  $('#space-id').focus();
}

async function handleSubmit(e) {
  e.preventDefault();

  const data = getFormData();
  const error = validateForm(data);
  if (error) {
    showFeedback(error, 'error');
    return;
  }

  const btn = $('.form__submit', form);
  btn.disabled = true;
  btn.textContent = 'Creando...';

  try {
    const created = await createBooking(data);
    state.bookings.push(mapBooking(created));
    renderBookings();
    showFeedback('Reserva creada correctamente.', 'success');
    resetForm();
  } catch {
    showFeedback('No se pudo crear la reserva. Verifica que el ID del espacio y el correo del usuario existan.', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Crear reserva';
  }
}

function handleDelete(e) {
  const btn = e.target.closest('.table__delete');
  if (!btn) return;

  const index = parseInt(btn.dataset.index, 10);
  const booking = state.bookings[index];
  if (!booking) return;
  if (!confirm('¿Eliminar esta reserva?')) return;

  deleteBooking(booking.id)
    .then(() => {
      state.bookings.splice(index, 1);
      renderBookings();
      showFeedback('Reserva eliminada.', 'success');
    })
    .catch(() => {
      showFeedback('No se pudo eliminar la reserva.', 'error');
    });
}

form.addEventListener('submit', handleSubmit);
tbody.addEventListener('click', handleDelete);

document.addEventListener('DOMContentLoaded', loadBookings);
