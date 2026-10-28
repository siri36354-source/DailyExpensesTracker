const $ = (selector) => document.querySelector(selector);
const money = (value) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(Number(value) || 0);
const categoryMeta = {
  Food: { icon: '🍲', color: '#25865b' },
  Travel: { icon: '🚌', color: '#e6b54a' },
  Education: { icon: '📚', color: '#6d8ed1' },
  Shopping: { icon: '🛍️', color: '#d78b73' },
  Other: { icon: '✳', color: '#a38bc7' }
};
let expenses = [];
let summary = { monthlySpending: 0, budget: 0, remainingBudget: 0, totalExpenses: 0, categoryTotals: {} };
let toastTimer;

function localToday() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}
function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]));
}
async function request(url, options = {}) {
  let response;
  try {
    response = await fetch(url, options);
  } catch (error) {
    if (window.location.protocol === 'file:') {
      throw new Error('Do not open index.html directly. Start the Java website using run-website.bat, then open http://localhost:8080 (or the port shown in the terminal).');
    }
    throw new Error('Cannot connect to the Java server. Run run-website.bat from the project folder and keep its terminal open. Then refresh this page.');
  }
  let data;
  try {
    data = await response.json();
  } catch (error) {
    throw new Error('The server returned an invalid response. Stop the server, run run-website.bat again, and refresh this page.');
  }
  if (!response.ok) throw new Error(data.error || 'Something went wrong. Please try again.');
  return data;
}
async function refresh() {
  const [loadedExpenses, loadedSummary] = await Promise.all([
    request('/api/expenses'), request('/api/summary')
  ]);
  expenses = loadedExpenses.sort((a, b) => b.date.localeCompare(a.date));
  summary = loadedSummary;
  renderSummary();
  renderChart();
  renderExpenses();
}
function renderSummary() {
  $('#monthly-spending').textContent = money(summary.monthlySpending);
  $('#remaining-budget').textContent = money(summary.remainingBudget);
  $('#total-expenses').textContent = money(summary.totalExpenses);
  $('#month-label').textContent = new Date().toLocaleDateString('en-IN', { month: 'long', year: 'numeric' });
  $('#today-label').textContent = new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).toUpperCase();
  const budget = Number(summary.budget) || 0;
  const spent = Number(summary.monthlySpending) || 0;
  const progress = budget > 0 ? Math.min(100, (spent / budget) * 100) : 0;
  $('#budget-progress').style.width = `${progress}%`;
  if (budget <= 0) $('#budget-foot').textContent = 'Set a budget to track your progress';
  else if (spent > budget) $('#budget-foot').textContent = `${money(spent - budget)} over your monthly budget`;
  else $('#budget-foot').textContent = `${Math.round(progress)}% of monthly budget used`;
}
function renderChart() {
  const categories = Object.entries(summary.categoryTotals || {}).filter(([, amount]) => Number(amount) > 0);
  const total = categories.reduce((sum, [, amount]) => sum + Number(amount), 0);
  $('#pie-total').textContent = money(total).replace('₹', '₹');
  if (!categories.length || total <= 0) {
    $('#pie-chart').style.background = 'conic-gradient(#e4ece6 0deg 360deg)';
    $('#chart-legend').innerHTML = '<p class="empty-legend">Add an expense this month to see your category breakdown.</p>';
    return;
  }
  let current = 0;
  const slices = categories.map(([category, amount]) => {
    const start = current;
    current += Number(amount) / total * 360;
    return `${categoryMeta[category]?.color || '#7c9987'} ${start}deg ${current}deg`;
  });
  $('#pie-chart').style.background = `conic-gradient(${slices.join(',')})`;
  $('#chart-legend').innerHTML = categories.sort((a, b) => b[1] - a[1]).map(([category, amount]) => {
    const meta = categoryMeta[category] || categoryMeta.Other;
    const share = Math.round(Number(amount) / total * 100);
    return `<div class="legend-item"><span class="legend-swatch" style="background:${meta.color}"></span><span>${escapeHtml(category)} <span style="color:#a0aaa2">${share}%</span></span><span class="legend-value">${money(amount)}</span></div>`;
  }).join('');
}
function renderExpenses() {
  const query = $('#search-input').value.trim().toLowerCase();
  const categoryFilter = $('#filter-category').value;
  const filtered = expenses.filter(expense => {
    const matchesQuery = `${expense.description} ${expense.category} ${expense.date} ${expense.amount}`.toLowerCase().includes(query);
    return matchesQuery && (categoryFilter === 'All' || expense.category === categoryFilter);
  });
  $('#record-count').textContent = `${filtered.length} transaction${filtered.length === 1 ? '' : 's'}`;
  if (!filtered.length) {
    $('#expense-rows').innerHTML = `<tr><td class="empty-table" colspan="4">${expenses.length ? 'No expenses match your search.' : 'No expenses yet. Add your first expense above.'}</td></tr>`;
    return;
  }
  $('#expense-rows').innerHTML = filtered.map(expense => {
    const meta = categoryMeta[expense.category] || categoryMeta.Other;
    const date = new Date(`${expense.date}T00:00:00`).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
    return `<tr><td><div class="expense-name"><span class="category-icon">${meta.icon}</span><span>${escapeHtml(expense.description)}</span></div></td><td><span class="category-pill">${escapeHtml(expense.category)}</span></td><td>${date}</td><td class="amount-column">${money(expense.amount)}</td></tr>`;
  }).join('');
}
function showToast(message) {
  const toast = $('#toast');
  toast.textContent = message;
  toast.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('show'), 2800);
}
$('#expense-form').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const message = $('#expense-message');
  message.textContent = '';
  message.classList.remove('error');
  const formData = new FormData(form);
  const amount = Number(formData.get('amount'));
  if (!Number.isFinite(amount) || amount <= 0) {
    message.textContent = 'Enter an amount greater than zero.';
    message.classList.add('error');
    return;
  }
  if (String(formData.get('date')) > localToday()) {
    message.textContent = 'Date cannot be in the future.';
    message.classList.add('error');
    return;
  }
  try {
    await request('/api/expenses', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' }, body: new URLSearchParams(formData) });
    form.reset();
    $('#date').value = localToday();
    await refresh();
    message.textContent = 'Expense saved successfully.';
    showToast('Expense saved successfully');
  } catch (error) {
    message.textContent = error.message;
    message.classList.add('error');
  }
});
$('#set-budget').addEventListener('click', async () => {
  const current = Number(summary.budget) || 0;
  const answer = window.prompt('Enter your monthly budget in ₹:', current > 0 ? String(current) : '');
  if (answer === null) return;
  const budget = Number(answer.trim());
  if (!Number.isFinite(budget) || budget <= 0) {
    showToast('Enter a budget greater than zero');
    return;
  }
  try {
    await request('/api/budget', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' }, body: new URLSearchParams({ budget: String(budget) }) });
    await refresh();
    showToast('Monthly budget saved');
  } catch (error) {
    showToast(error.message);
  }
});
$('#search-input').addEventListener('input', renderExpenses);
$('#filter-category').addEventListener('change', renderExpenses);
$('#date').max = localToday();
$('#date').value = localToday();
refresh().catch(error => {
  $('#expense-rows').innerHTML = `<tr><td class="empty-table" colspan="4">${escapeHtml(error.message)} Make sure the Java server is running.</td></tr>`;
  showToast(error.message || 'Could not connect to the local Java server');
});
