/**
 * Expense Tracker & Financial Assistant Frontend Application
 * Step 4: Multi-Page Application (Home, Notifications, Assistant, Budget, Spending, Add Expense, History)
 */

// Determine the API base URL dynamically
const getApiBase = () => {
    const origin = window.location.origin;
    if (origin && (origin.startsWith('http://') || origin.startsWith('https://'))) {
        if (origin.includes(':8080')) {
            return '/api/expenses';
        }
    }
    return 'http://localhost:8080/api/expenses';
};

const API_BASE = getApiBase();

// Application State
const state = {
    expenses: [],
    editingId: null,
    budgetPlan: null,
    notifications: [],
    monthlyComparison: null,
    selectedCategories: new Set(['Food', 'Rent', 'Savings', 'Education', 'Others']),
    selectedMonth: null,
    selectedWeek: null,
};

// Category Metadata (Icons & Palette Colors)
const CATEGORY_META = {
    'Food': { icon: '🍔', color: '#f59e0b' },
    'Rent': { icon: '🏠', color: '#3b82f6' },
    'Savings': { icon: '📈', color: '#10b981' },
    'Education': { icon: '🎓', color: '#8b5cf6' },
    'Gym': { icon: '💪', color: '#ec4899' },
    'Transportation': { icon: '🚗', color: '#06b6d4' },
    'Utilities': { icon: '⚡', color: '#eab308' },
    'Healthcare': { icon: '🏥', color: '#ef4444' },
    'Shopping': { icon: '🛍️', color: '#f97316' },
    'Housing': { icon: '🏡', color: '#6366f1' },
    'Entertainment': { icon: '🎬', color: '#a855f7' },
    'Others': { icon: '📦', color: '#64748b' },
    'Other': { icon: '📦', color: '#64748b' },
};

// DOM Elements
const elements = {
    // Header & Global
    btnRefresh: document.getElementById('btn-refresh'),
    btnToggleNotifs: document.getElementById('btn-toggle-notifs'),
    headerUnreadBadge: document.getElementById('header-unread-badge'),
    toast: document.getElementById('toast'),
    serverStatus: document.getElementById('server-status'),

    // Metrics Overview
    totalSpending: document.getElementById('total-spending'),
    totalTransactions: document.getElementById('total-transactions'),
    activeCategoriesCount: document.getElementById('active-categories-count'),
    topCategoryName: document.getElementById('top-category-name'),

    // Notifications Center
    notificationsSection: document.getElementById('notifications-section'),
    notificationsList: document.getElementById('notifications-list'),
    notificationsUnreadBadge: document.getElementById('notifications-unread-badge'),
    btnMarkAllRead: document.getElementById('btn-mark-all-read'),
    btnRefreshNotifications: document.getElementById('btn-refresh-notifications'),

    // AI Assistant
    assistantInput: document.getElementById('assistant-input'),
    btnMic: document.getElementById('btn-mic'),
    micLabel: document.getElementById('mic-label'),
    btnParse: document.getElementById('btn-parse'),
    btnClearAssistant: document.getElementById('btn-clear-assistant'),
    micStatus: document.getElementById('mic-status'),
    micStatusText: document.getElementById('mic-status-text'),
    assistantLang: document.getElementById('assistant-lang'),
    confirmationCard: document.getElementById('confirmation-card'),
    confirmIntentBadge: document.getElementById('confirm-intent-badge'),
    confirmConfidence: document.getElementById('confirm-confidence'),
    confirmationBody: document.getElementById('confirmation-body'),
    btnConfirmApply: document.getElementById('btn-confirm-apply'),
    btnConfirmEdit: document.getElementById('btn-confirm-edit'),
    btnConfirmCancel: document.getElementById('btn-confirm-cancel'),
    btnDismissConfirm: document.getElementById('btn-dismiss-confirm'),

    // Budget Guidelines
    budgetSection: document.getElementById('budget-section'),
    budgetSalary: document.getElementById('budget-salary'),
    budgetCategoryChips: document.getElementById('budget-category-chips'),
    btnGenerateGuidelines: document.getElementById('btn-generate-guidelines'),
    btnResetGuidelines: document.getElementById('btn-reset-guidelines'),
    guidelineSalary: document.getElementById('guideline-salary'),
    guidelineTotalAllocated: document.getElementById('guideline-total-allocated'),
    guidelineUnallocated: document.getElementById('guideline-unallocated'),
    guidelineAllocatedPct: document.getElementById('guideline-allocated-pct'),
    guidelineMultiBar: document.getElementById('guideline-multi-bar'),
    guidelineLegend: document.getElementById('guideline-legend'),
    guidelinesVsActualTbody: document.getElementById('guidelines-vs-actual-tbody'),

    // Current Monthly Budget
    cbMonthSelect: document.getElementById('cb-month-select'),
    cbMonthBadge: document.getElementById('cb-month-badge'),
    cbStatusBadge: document.getElementById('cb-status-badge'),
    cbSubtitle: document.getElementById('cb-subtitle'),
    cbAlertBox: document.getElementById('cb-alert-box'),
    cbBudgetVal: document.getElementById('cb-budget-val'),
    cbBudgetDesc: document.getElementById('cb-budget-desc'),
    cbSpentVal: document.getElementById('cb-spent-val'),
    cbRemainingVal: document.getElementById('cb-remaining-val'),
    cbRemainingDesc: document.getElementById('cb-remaining-desc'),
    cbUsageVal: document.getElementById('cb-usage-val'),
    cbUsageDesc: document.getElementById('cb-usage-desc'),
    cbProgressBar: document.getElementById('cb-progress-bar'),
    cbProgressSpentText: document.getElementById('cb-progress-spent-text'),
    cbProgressBudgetText: document.getElementById('cb-progress-budget-text'),
    cbFormMonthLabel: document.getElementById('cb-form-month-label'),
    monthlyBudgetInput: document.getElementById('monthly-budget-input'),
    setMonthlyBudgetForm: document.getElementById('set-monthly-budget-form'),
    btnSaveMonthlyBudget: document.getElementById('btn-save-monthly-budget'),

    // Current Weekly Budget
    wbWeekSelect: document.getElementById('wb-week-select'),
    wbWeekBadge: document.getElementById('wb-week-badge'),
    wbDatesBadge: document.getElementById('wb-dates-badge'),
    wbStatusBadge: document.getElementById('wb-status-badge'),
    wbSubtitle: document.getElementById('wb-subtitle'),
    wbBudgetVal: document.getElementById('wb-budget-val'),
    wbBudgetDesc: document.getElementById('wb-budget-desc'),
    wbSpentVal: document.getElementById('wb-spent-val'),
    wbRemainingVal: document.getElementById('wb-remaining-val'),
    wbRemainingDesc: document.getElementById('wb-remaining-desc'),
    wbUsageVal: document.getElementById('wb-usage-val'),
    wbUsageDesc: document.getElementById('wb-usage-desc'),
    wbProgressBar: document.getElementById('wb-progress-bar'),
    wbProgressSpentText: document.getElementById('wb-progress-spent-text'),
    wbProgressBudgetText: document.getElementById('wb-progress-budget-text'),
    wbFormWeekLabel: document.getElementById('wb-form-week-label'),
    weeklyBudgetInput: document.getElementById('weekly-budget-input'),
    setWeeklyBudgetForm: document.getElementById('set-weekly-budget-form'),
    btnSaveWeeklyBudget: document.getElementById('btn-save-weekly-budget'),

    // Expense Form & History Edit Panel
    editExpensePanel: document.getElementById('edit-expense-panel'),
    form: document.getElementById('expense-form'),
    formTitle: document.getElementById('form-title'),
    formBadge: document.getElementById('form-mode-badge'),
    expenseIdInput: document.getElementById('expense-id'),
    amountInput: document.getElementById('amount'),
    categorySelect: document.getElementById('category'),
    dateInput: document.getElementById('date'),
    descriptionInput: document.getElementById('description'),
    btnSubmit: document.getElementById('btn-submit'),
    btnCancel: document.getElementById('btn-cancel'),

    // Summaries & Insights
    categorySummaryContainer: document.getElementById('category-summary-container'),
    insightsContainer: document.getElementById('insights-container'),

    // Month-over-Month Comparison
    comparisonMonthSubtitle: document.getElementById('comparison-month-subtitle'),
    btnRefreshComparison: document.getElementById('btn-refresh-comparison'),
    labelPrevMonth: document.getElementById('label-prev-month'),
    labelCurMonth: document.getElementById('label-cur-month'),
    valPrevTotal: document.getElementById('val-prev-total'),
    valCurTotal: document.getElementById('val-cur-total'),
    valDiff: document.getElementById('val-diff'),
    valDiffSub: document.getElementById('val-diff-sub'),
    valPctChange: document.getElementById('val-pct-change'),
    valPctSub: document.getElementById('val-pct-sub'),
    cardDiff: document.getElementById('card-diff'),
    cardPctChange: document.getElementById('card-pct-change'),
    comparisonTbody: document.getElementById('comparison-tbody'),

    // Table
    searchInput: document.getElementById('table-search'),
    expensesTbody: document.getElementById('expenses-tbody'),
};

// ==========================================================================
// Formatting Helpers
// ==========================================================================
function formatCurrency(amount) {
    const num = Math.round(Number(amount) || 0);
    return `₹${num.toLocaleString('en-IN')}`;
}

function escapeHtml(text) {
    if (text === null || text === undefined) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function setDefaultDate() {
    if (elements.dateInput && !elements.dateInput.value) {
        const today = new Date().toISOString().split('T')[0];
        elements.dateInput.value = today;
    }
}

// Active Nav Link Highlighter
function updateActiveNavLink() {
    const path = window.location.pathname;
    const navItems = document.querySelectorAll('.main-nav .nav-item');
    navItems.forEach(item => {
        const href = item.getAttribute('href');
        if (!href) return;

        if (
            path.endsWith(href) ||
            (href === 'index.html' && (path.endsWith('/') || path === '' || path.endsWith('/index')))
        ) {
            item.classList.add('active');
        } else {
            item.classList.remove('active');
        }
    });
}

// Check URL Query Parameters for Auto-Fill
function checkUrlParameters() {
    const params = new URLSearchParams(window.location.search);

    // If on add-expense.html
    if (elements.amountInput && params.has('amount')) {
        elements.amountInput.value = params.get('amount');
    }
    if (elements.categorySelect && params.has('category')) {
        elements.categorySelect.value = params.get('category');
    }
    if (elements.dateInput && params.has('date')) {
        elements.dateInput.value = params.get('date');
    }
    if (elements.descriptionInput && params.has('description')) {
        elements.descriptionInput.value = params.get('description');
    }

    // If on budget.html
    if (elements.budgetSalary && params.has('salary')) {
        elements.budgetSalary.value = params.get('salary');
    }
    if (params.has('categories')) {
        const cats = params.get('categories').split(',').map(c => c.trim()).filter(Boolean);
        if (cats.length > 0) {
            state.selectedCategories = new Set(cats);
            updateCategoryChipsUI();
        }
    }
}

// ==========================================================================
// Toast Notification
// ==========================================================================
let toastTimeout = null;

function showToast(message, type = 'success') {
    if (!elements.toast) return;

    if (toastTimeout) {
        clearTimeout(toastTimeout);
    }

    elements.toast.textContent = message;
    elements.toast.className = `toast show toast-${type}`;

    toastTimeout = setTimeout(() => {
        elements.toast.className = 'toast';
    }, 3800);
}

function updateStatus(isOnline) {
    if (!elements.serverStatus) return;
    if (isOnline) {
        elements.serverStatus.textContent = 'Connected';
        elements.serverStatus.className = 'metric-value status-online';
    } else {
        elements.serverStatus.textContent = 'Offline';
        elements.serverStatus.className = 'metric-value status-offline';
    }
}

// ==========================================================================
// 1. In-App Notifications Center
// ==========================================================================
async function fetchNotifications() {
    try {
        const response = await fetch('/api/notifications', { method: 'GET' });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const list = await response.json();
        state.notifications = Array.isArray(list) ? list : [];
        renderNotifications();
    } catch (err) {
        console.error('Failed to fetch notifications:', err);
        if (elements.notificationsList) {
            elements.notificationsList.innerHTML = `
                <div class="empty-state">Unable to load notifications. Please check backend connection.</div>
            `;
        }
    }
}

function renderNotifications() {
    const list = state.notifications;
    const unreadCount = list.filter(n => !n.isRead).length;

    // Update unread badge in header nav globally on all pages
    if (elements.headerUnreadBadge) {
        elements.headerUnreadBadge.textContent = unreadCount;
        elements.headerUnreadBadge.style.display = unreadCount > 0 ? 'inline-block' : 'none';
    }

    if (!elements.notificationsList) return;

    // Update unread badge inside notifications page
    if (elements.notificationsUnreadBadge) {
        elements.notificationsUnreadBadge.textContent = `${unreadCount} Unread`;
        elements.notificationsUnreadBadge.style.backgroundColor = unreadCount > 0 ? '#3b82f6' : '#94a3b8';
    }

    if (list.length === 0) {
        elements.notificationsList.innerHTML = `
            <div class="empty-state" style="padding: 24px; text-align: center; color: var(--text-muted);">
                🎉 All caught up! Monthly budget alerts and weekly spending updates will appear here.
            </div>
        `;
        return;
    }

    elements.notificationsList.innerHTML = list.map(item => {
        const isUnread = !item.isRead;
        const typeClass = item.type || 'WEEKLY_PACE';

        let typeIcon = '📅';
        let badgeLabel = 'Weekly Pace';
        let severityClass = 'item-info';

        if (typeClass === 'MONTHLY_BUDGET_ALERT' || typeClass === 'ALERT' || typeClass.includes('EXCEEDED')) {
            typeIcon = '🚨';
            badgeLabel = 'Monthly Budget';
            severityClass = 'item-alert';
        } else if (typeClass === 'MONTHLY_BUDGET_WARNING' || typeClass === 'WARNING') {
            typeIcon = '⚠️';
            badgeLabel = 'Monthly Budget';
            severityClass = 'item-warning';
        } else if (typeClass === 'MONTHLY_BUDGET_INFO' || typeClass === 'INFO' || typeClass.includes('UNSET') || typeClass.includes('50')) {
            typeIcon = 'ℹ️';
            badgeLabel = 'Monthly Budget';
            severityClass = 'item-info';
        } else if (typeClass === 'CATEGORY_ALERT') {
            typeIcon = '⚠️';
            badgeLabel = 'Category Notice';
            severityClass = 'item-warning';
        } else if (typeClass === 'CATEGORY_PACE') {
            typeIcon = '📊';
            badgeLabel = 'Pacing Alert';
            severityClass = 'item-warning';
        } else if (typeClass === 'WEEKLY_PACE') {
            typeIcon = '📅';
            badgeLabel = 'Weekly Pace';
            severityClass = 'item-info';
        }

        const dateStr = item.createdAt ? item.createdAt.substring(0, 16).replace('T', ' ') : '';

        return `
            <div class="notification-item ${isUnread ? 'unread ' + severityClass : 'read'}" data-id="${item.id}">
                <div class="notif-left">
                    <span class="notif-type-icon">${typeIcon}</span>
                    <div class="notif-content">
                        <div class="notif-title-row">
                            <span class="notif-title">${escapeHtml(item.title)}</span>
                            <span class="notif-badge ${typeClass}">${escapeHtml(badgeLabel)}</span>
                            <span class="notif-time">${escapeHtml(dateStr)}</span>
                        </div>
                        <p class="notif-msg">${escapeHtml(item.message)}</p>
                    </div>
                </div>
                <div class="notif-actions">
                    ${isUnread ? `
                        <button type="button" class="btn-mark-read" onclick="handleMarkAsRead(${item.id})">
                            ✓ Mark as Read
                        </button>
                    ` : `
                        <span style="font-size: 0.75rem; color: #94a3b8; font-weight: 500;">✓ Read</span>
                    `}
                </div>
            </div>
        `;
    }).join('');
}

window.handleMarkAsRead = async function(id) {
    try {
        const res = await fetch(`/api/notifications/${id}/read`, { method: 'POST' });
        if (res.ok) {
            const target = state.notifications.find(n => n.id === id);
            if (target) target.isRead = true;
            renderNotifications();
            showToast('Notification marked as read', 'success');
        } else {
            showToast('Failed to mark notification as read', 'error');
        }
    } catch (e) {
        console.error('Error marking notification as read:', e);
        showToast('Network error', 'error');
    }
};

async function markAllNotificationsRead() {
    const unread = state.notifications.filter(n => !n.isRead);
    if (unread.length === 0) {
        showToast('No unread notifications to mark', 'success');
        return;
    }

    try {
        const res = await fetch('/api/notifications/read-all', { method: 'POST' });
        if (res.ok) {
            state.notifications.forEach(n => n.isRead = true);
            renderNotifications();
            showToast(`All ${unread.length} notifications marked as read!`, 'success');
        } else {
            for (const item of unread) {
                await fetch(`/api/notifications/${item.id}/read`, { method: 'POST' });
                item.isRead = true;
            }
            renderNotifications();
            showToast(`All ${unread.length} notifications marked as read!`, 'success');
        }
    } catch (e) {
        console.error('Error marking all as read:', e);
        fetchNotifications();
    }
}

// ==========================================================================
// 2. Selectable Monthly & Weekly Budgets Engine
// ==========================================================================

// ISO-8601 Week Helper matching Java WeekFields.ISO
function getIsoWeekInfo(targetDate) {
    const d = new Date(Date.UTC(targetDate.getFullYear(), targetDate.getMonth(), targetDate.getDate()));
    // Set to nearest Thursday: current date + 4 - current day number (Monday=1, Sunday=7)
    const dayNr = (d.getUTCDay() + 6) % 7; // Monday=0, Sunday=6
    d.setUTCDate(d.getUTCDate() - dayNr + 3);
    const weekBasedYear = d.getUTCFullYear();

    const firstThursday = new Date(Date.UTC(weekBasedYear, 0, 4));
    const firstDayNr = (firstThursday.getUTCDay() + 6) % 7;
    firstThursday.setUTCDate(firstThursday.getUTCDate() - firstDayNr + 3);

    const weekNumber = 1 + Math.round(((d.getTime() - firstThursday.getTime()) / 86400000) / 7);
    const weekKey = `${weekBasedYear}-W${String(weekNumber).padStart(2, '0')}`;

    // Calculate Monday and Sunday of this week
    const monday = new Date(targetDate.getFullYear(), targetDate.getMonth(), targetDate.getDate());
    const day = (monday.getDay() + 6) % 7;
    monday.setDate(monday.getDate() - day);

    const sunday = new Date(monday.getFullYear(), monday.getMonth(), monday.getDate());
    sunday.setDate(monday.getDate() + 6);

    const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const dtfStart = `${String(monday.getDate()).padStart(2, '0')} ${monthNames[monday.getMonth()]}`;
    const dtfEnd = `${String(sunday.getDate()).padStart(2, '0')} ${monthNames[sunday.getMonth()]}, ${sunday.getFullYear()}`;
    const dateRange = `${dtfStart} - ${dtfEnd}`;

    return {
        weekBasedYear,
        weekNumber,
        weekKey,
        weekLabel: `Week ${weekNumber}, ${weekBasedYear}`,
        dateRange,
        monday,
        sunday
    };
}

function initMonthlyBudgetSelector() {
    if (!elements.cbMonthSelect) return;

    const now = new Date();
    const curYear = now.getFullYear();
    const curMonth = now.getMonth(); // 0-11

    const monthNames = [
        'January', 'February', 'March', 'April', 'May', 'June',
        'July', 'August', 'September', 'October', 'November', 'December'
    ];

    const currentMonthKey = `${curYear}-${String(curMonth + 1).padStart(2, '0')}`;
    if (!state.selectedMonth) {
        state.selectedMonth = currentMonthKey;
    }

    elements.cbMonthSelect.innerHTML = '';
    for (let i = 0; i < 12; i++) {
        const d = new Date(curYear, curMonth + i, 1);
        const y = d.getFullYear();
        const m = d.getMonth();
        const key = `${y}-${String(m + 1).padStart(2, '0')}`;
        const name = `${monthNames[m]} ${y}`;
        const tag = (i === 0) ? ' (Current)' : '';

        const opt = document.createElement('option');
        opt.value = key;
        opt.textContent = `${name}${tag}`;
        elements.cbMonthSelect.appendChild(opt);
    }

    if (state.selectedMonth && !elements.cbMonthSelect.querySelector(`option[value="${state.selectedMonth}"]`)) {
        const opt = document.createElement('option');
        opt.value = state.selectedMonth;
        opt.textContent = state.selectedMonth;
        elements.cbMonthSelect.prepend(opt);
    }

    elements.cbMonthSelect.value = state.selectedMonth;

    if (!elements.cbMonthSelect.dataset.listenerAttached) {
        elements.cbMonthSelect.dataset.listenerAttached = 'true';
        elements.cbMonthSelect.addEventListener('change', (e) => {
            state.selectedMonth = e.target.value;
            fetchCurrentMonthlyBudget(state.selectedMonth);
        });
    }
}

function initWeeklyBudgetSelector() {
    if (!elements.wbWeekSelect) return;

    const today = new Date();
    const monday = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const day = (monday.getDay() + 6) % 7;
    monday.setDate(monday.getDate() - day);

    const curWeekInfo = getIsoWeekInfo(today);
    if (!state.selectedWeek) {
        state.selectedWeek = curWeekInfo.weekKey;
    }

    elements.wbWeekSelect.innerHTML = '';
    for (let i = 0; i < 16; i++) {
        const iterMonday = new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + (i * 7));
        const info = getIsoWeekInfo(iterMonday);

        const opt = document.createElement('option');
        opt.value = info.weekKey;
        const tag = (i === 0) ? ' (Current)' : '';
        opt.textContent = `${info.weekLabel} (${info.dateRange})${tag}`;
        elements.wbWeekSelect.appendChild(opt);
    }

    if (state.selectedWeek && !elements.wbWeekSelect.querySelector(`option[value="${state.selectedWeek}"]`)) {
        const opt = document.createElement('option');
        opt.value = state.selectedWeek;
        opt.textContent = state.selectedWeek;
        elements.wbWeekSelect.prepend(opt);
    }

    elements.wbWeekSelect.value = state.selectedWeek;

    if (!elements.wbWeekSelect.dataset.listenerAttached) {
        elements.wbWeekSelect.dataset.listenerAttached = 'true';
        elements.wbWeekSelect.addEventListener('change', (e) => {
            state.selectedWeek = e.target.value;
            fetchCurrentWeeklyBudget(state.selectedWeek);
        });
    }
}

// ==========================================================================
// 2A. Monthly Budget for Selected Month
// ==========================================================================
let currentMonthlyBudgetState = null;

async function fetchCurrentMonthlyBudget(targetMonth) {
    try {
        const month = targetMonth || state.selectedMonth || '';
        const url = month ? `/api/budget/current?month=${encodeURIComponent(month)}` : '/api/budget/current';
        const res = await fetch(url, { method: 'GET' });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        currentMonthlyBudgetState = data;
        if (data.monthYear) {
            state.selectedMonth = data.monthYear;
            if (elements.cbMonthSelect && elements.cbMonthSelect.value !== data.monthYear) {
                elements.cbMonthSelect.value = data.monthYear;
            }
        }
        renderCurrentMonthlyBudget(data);
    } catch (err) {
        console.error('Failed to load current monthly budget:', err);
    }
}

function renderCurrentMonthlyBudget(data) {
    if (!data) return;

    const monthName = data.monthName || 'Current Month';
    const budget = Number(data.budget) || 0;
    const totalSpent = Number(data.totalSpent) || 0;
    const remaining = Number(data.remaining) || 0;
    const usagePct = Number(data.usagePercentage) || 0;
    const status = data.status || 'No budget set';

    if (elements.cbMonthBadge) elements.cbMonthBadge.textContent = monthName;
    if (elements.cbFormMonthLabel) elements.cbFormMonthLabel.textContent = monthName;

    if (elements.cbBudgetVal) {
        elements.cbBudgetVal.textContent = budget > 0 ? formatCurrency(budget) : 'No budget set';
    }
    if (elements.cbBudgetDesc) {
        elements.cbBudgetDesc.textContent = budget > 0 ? 'Allocated for this month' : 'No budget allocated for this month';
    }
    if (elements.cbSpentVal) elements.cbSpentVal.textContent = formatCurrency(totalSpent);

    if (elements.cbRemainingVal) {
        if (budget > 0) {
            if (remaining < 0) {
                elements.cbRemainingVal.textContent = `-₹${Math.abs(Math.round(remaining)).toLocaleString('en-IN')}`;
                elements.cbRemainingVal.className = 'cb-stat-value text-danger';
                if (elements.cbRemainingDesc) elements.cbRemainingDesc.textContent = `Over budget by ${formatCurrency(Math.abs(remaining))}`;
            } else {
                elements.cbRemainingVal.textContent = formatCurrency(remaining);
                elements.cbRemainingVal.className = 'cb-stat-value text-success';
                if (elements.cbRemainingDesc) elements.cbRemainingDesc.textContent = 'Available balance to spend';
            }
        } else {
            elements.cbRemainingVal.textContent = '—';
            elements.cbRemainingVal.className = 'cb-stat-value';
            if (elements.cbRemainingDesc) elements.cbRemainingDesc.textContent = 'Budget not configured';
        }
    }

    if (elements.cbUsageVal) {
        if (budget > 0) {
            elements.cbUsageVal.textContent = `${usagePct.toFixed(1)}%`;
            elements.cbUsageVal.className = 'cb-stat-value text-primary';
            if (elements.cbUsageDesc) elements.cbUsageDesc.textContent = `${usagePct.toFixed(1)}% of ₹${Math.round(budget).toLocaleString('en-IN')} spent`;
        } else {
            elements.cbUsageVal.textContent = '—';
            elements.cbUsageVal.className = 'cb-stat-value';
            if (elements.cbUsageDesc) elements.cbUsageDesc.textContent = 'Budget not configured';
        }
    }

    // Status pill & Progress Bar
    if (elements.cbStatusBadge) {
        let pillClass = 'status-within';
        let pillText = 'Within budget';
        let barClass = 'progress-normal';

        if (status === 'Budget exceeded') {
            pillClass = 'status-danger';
            pillText = '🚨 Budget exceeded';
            barClass = 'progress-danger';
        } else if (status === 'Near budget limit') {
            pillClass = 'status-warning';
            pillText = '⚠️ Near budget limit';
            barClass = 'progress-warning';
        } else if (status === 'Within budget') {
            pillClass = 'status-within';
            pillText = '✓ Within budget';
            barClass = 'progress-normal';
        } else {
            pillClass = 'status-neutral';
            pillText = 'ℹ️ No budget set';
            barClass = 'progress-neutral';
        }

        elements.cbStatusBadge.className = `current-budget-status-pill ${pillClass}`;
        elements.cbStatusBadge.textContent = pillText;

        if (elements.cbProgressBar) {
            const visualPct = budget > 0 ? Math.min(100, Math.max(0, usagePct)) : 0;
            elements.cbProgressBar.style.width = `${visualPct}%`;
            elements.cbProgressBar.className = `cb-progress-bar-fill ${barClass}`;
        }
    }

    if (elements.cbProgressSpentText) {
        elements.cbProgressSpentText.textContent = `${formatCurrency(totalSpent)} spent`;
    }
    if (elements.cbProgressBudgetText) {
        elements.cbProgressBudgetText.textContent = budget > 0 ? `Budget: ${formatCurrency(budget)}` : 'Budget: Not set';
    }

    // Active Monthly Budget Alert Box on Dashboard & Budget Page
    if (elements.cbAlertBox) {
        if (budget <= 0) {
            elements.cbAlertBox.className = 'cb-alert-box info';
            elements.cbAlertBox.innerHTML = `<strong>ℹ️ No Monthly Budget:</strong> No monthly budget set for ${escapeHtml(monthName)}. Set a budget to track your spending.`;
            elements.cbAlertBox.style.display = 'block';
        } else if (remaining < 0 || usagePct > 100) {
            const overAmount = Math.abs(remaining);
            elements.cbAlertBox.className = 'cb-alert-box alert';
            elements.cbAlertBox.innerHTML = `<strong>🚨 Budget Exceeded:</strong> You have exceeded your monthly budget by ${formatCurrency(overAmount)}.`;
            elements.cbAlertBox.style.display = 'block';
        } else if (usagePct >= 100) {
            elements.cbAlertBox.className = 'cb-alert-box alert';
            elements.cbAlertBox.innerHTML = `<strong>🚨 Limit Reached:</strong> You have reached your monthly budget limit.`;
            elements.cbAlertBox.style.display = 'block';
        } else if (usagePct >= 80) {
            elements.cbAlertBox.className = 'cb-alert-box warning';
            elements.cbAlertBox.innerHTML = `<strong>⚠️ High Spending:</strong> You have used ${usagePct.toFixed(0)}% of your monthly budget. Consider reducing spending.`;
            elements.cbAlertBox.style.display = 'block';
        } else if (usagePct >= 50) {
            elements.cbAlertBox.className = 'cb-alert-box info';
            elements.cbAlertBox.innerHTML = `<strong>ℹ️ Spending Notice:</strong> You have used ${usagePct.toFixed(0)}% of your monthly budget.`;
            elements.cbAlertBox.style.display = 'block';
        } else {
            elements.cbAlertBox.style.display = 'none';
            elements.cbAlertBox.innerHTML = '';
        }
    }

    // Pre-populate input if not currently focused
    if (elements.monthlyBudgetInput && document.activeElement !== elements.monthlyBudgetInput) {
        elements.monthlyBudgetInput.value = budget > 0 ? Math.round(budget) : '';
    }
}

async function handleSetMonthlyBudget(e) {
    if (e) e.preventDefault();

    const amount = parseInt(elements.monthlyBudgetInput?.value, 10);
    if (!amount || isNaN(amount) || amount <= 0) {
        showToast('Please enter a valid monthly budget amount greater than 0', 'error');
        elements.monthlyBudgetInput?.focus();
        return;
    }

    const targetMonth = state.selectedMonth || new Date().toISOString().substring(0, 7);
    const payload = {
        budget: amount,
        month: targetMonth,
    };

    try {
        if (elements.btnSaveMonthlyBudget) elements.btnSaveMonthlyBudget.disabled = true;
        const res = await fetch('/api/budget/current', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });

        const data = await res.json();
        if (res.ok) {
            currentMonthlyBudgetState = data;
            renderCurrentMonthlyBudget(data);
            showToast(`✓ Monthly budget for ${data.monthName || 'this month'} set to ${formatCurrency(amount)}!`, 'success');
            // If budget guidelines input is present on page, update it too
            if (elements.budgetSalary && (!elements.budgetSalary.value || parseInt(elements.budgetSalary.value, 10) === 0)) {
                elements.budgetSalary.value = amount;
            }
            fetchNotifications();
        } else {
            showToast(data.error || 'Failed to save monthly budget', 'error');
        }
    } catch (err) {
        console.error('Error saving current monthly budget:', err);
        showToast('Network error saving monthly budget', 'error');
    } finally {
        if (elements.btnSaveMonthlyBudget) elements.btnSaveMonthlyBudget.disabled = false;
    }
}

// ==========================================================================
// 2B. Weekly Budget for Selected Week
// ==========================================================================
let currentWeeklyBudgetState = null;

async function fetchCurrentWeeklyBudget(targetWeek) {
    try {
        const week = targetWeek || state.selectedWeek || '';
        const url = week ? `/api/budget/weekly/current?week=${encodeURIComponent(week)}` : '/api/budget/weekly/current';
        const res = await fetch(url, { method: 'GET' });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        currentWeeklyBudgetState = data;
        if (data.weekKey) {
            state.selectedWeek = data.weekKey;
            if (elements.wbWeekSelect && elements.wbWeekSelect.value !== data.weekKey) {
                elements.wbWeekSelect.value = data.weekKey;
            }
        }
        renderCurrentWeeklyBudget(data);
    } catch (err) {
        console.error('Failed to load current weekly budget:', err);
    }
}

function renderCurrentWeeklyBudget(data) {
    if (!data) return;

    const weekLabel = data.weekLabel || 'Current Week';
    const dateRange = data.dateRange || '';
    const budget = Number(data.budget) || 0;
    const totalSpent = Number(data.totalSpent) || 0;
    const remaining = Number(data.remaining) || 0;
    const usagePct = Number(data.usagePercentage) || 0;
    const status = data.status || 'No budget set';

    if (elements.wbWeekBadge) elements.wbWeekBadge.textContent = weekLabel;
    if (elements.wbDatesBadge) {
        elements.wbDatesBadge.textContent = dateRange;
        elements.wbDatesBadge.style.display = dateRange ? 'inline-block' : 'none';
    }
    if (elements.wbFormWeekLabel) elements.wbFormWeekLabel.textContent = weekLabel;

    if (elements.wbBudgetVal) {
        elements.wbBudgetVal.textContent = budget > 0 ? formatCurrency(budget) : 'No budget set';
    }
    if (elements.wbBudgetDesc) {
        elements.wbBudgetDesc.textContent = budget > 0 ? 'Allocated for this week' : 'No weekly budget set';
    }
    if (elements.wbSpentVal) elements.wbSpentVal.textContent = formatCurrency(totalSpent);

    if (elements.wbRemainingVal) {
        if (budget > 0) {
            if (remaining < 0) {
                elements.wbRemainingVal.textContent = `-₹${Math.abs(Math.round(remaining)).toLocaleString('en-IN')}`;
                elements.wbRemainingVal.className = 'cb-stat-value text-danger';
                if (elements.wbRemainingDesc) elements.wbRemainingDesc.textContent = `Over budget by ${formatCurrency(Math.abs(remaining))}`;
            } else {
                elements.wbRemainingVal.textContent = formatCurrency(remaining);
                elements.wbRemainingVal.className = 'cb-stat-value text-success';
                if (elements.wbRemainingDesc) elements.wbRemainingDesc.textContent = 'Available balance';
            }
        } else {
            elements.wbRemainingVal.textContent = '—';
            elements.wbRemainingVal.className = 'cb-stat-value';
            if (elements.wbRemainingDesc) elements.wbRemainingDesc.textContent = 'Budget not configured';
        }
    }

    if (elements.wbUsageVal) {
        if (budget > 0) {
            elements.wbUsageVal.textContent = `${usagePct.toFixed(1)}%`;
            elements.wbUsageVal.className = 'cb-stat-value text-primary';
            if (elements.wbUsageDesc) elements.wbUsageDesc.textContent = `${usagePct.toFixed(1)}% of ₹${Math.round(budget).toLocaleString('en-IN')} spent`;
        } else {
            elements.wbUsageVal.textContent = '—';
            elements.wbUsageVal.className = 'cb-stat-value';
            if (elements.wbUsageDesc) elements.wbUsageDesc.textContent = 'Budget not configured';
        }
    }

    // Status pill & Progress Bar
    if (elements.wbStatusBadge) {
        let pillClass = 'status-within';
        let pillText = 'Within budget';
        let barClass = 'progress-normal';

        if (status === 'Budget exceeded') {
            pillClass = 'status-danger';
            pillText = '🚨 Budget exceeded';
            barClass = 'progress-danger';
        } else if (status === 'Near budget limit') {
            pillClass = 'status-warning';
            pillText = '⚠️ Near budget limit';
            barClass = 'progress-warning';
        } else if (status === 'Within budget') {
            pillClass = 'status-within';
            pillText = '✓ Within budget';
            barClass = 'progress-normal';
        } else {
            pillClass = 'status-neutral';
            pillText = 'ℹ️ No budget set';
            barClass = 'progress-neutral';
        }

        elements.wbStatusBadge.className = `current-budget-status-pill ${pillClass}`;
        elements.wbStatusBadge.textContent = pillText;

        if (elements.wbProgressBar) {
            const visualPct = budget > 0 ? Math.min(100, Math.max(0, usagePct)) : 0;
            elements.wbProgressBar.style.width = `${visualPct}%`;
            elements.wbProgressBar.className = `cb-progress-bar-fill ${barClass}`;
        }
    }

    if (elements.wbProgressSpentText) {
        elements.wbProgressSpentText.textContent = `${formatCurrency(totalSpent)} spent this week`;
    }
    if (elements.wbProgressBudgetText) {
        elements.wbProgressBudgetText.textContent = budget > 0 ? `Weekly Budget: ${formatCurrency(budget)}` : 'Weekly Budget: Not set';
    }

    // Pre-populate input if not currently focused
    if (elements.weeklyBudgetInput && document.activeElement !== elements.weeklyBudgetInput) {
        if (budget > 0) {
            elements.weeklyBudgetInput.value = Number.isInteger(budget) ? budget : parseFloat(budget.toFixed(2));
        } else {
            elements.weeklyBudgetInput.value = '';
        }
    }
}

async function handleSetWeeklyBudget(e) {
    if (e) e.preventDefault();

    const amount = parseFloat(elements.weeklyBudgetInput?.value);
    if (!amount || isNaN(amount) || amount <= 0) {
        showToast('Please enter a valid weekly budget amount greater than 0', 'error');
        elements.weeklyBudgetInput?.focus();
        return;
    }

    const targetWeek = state.selectedWeek || null;
    const payload = {
        budget: amount,
        week: targetWeek,
    };

    try {
        if (elements.btnSaveWeeklyBudget) elements.btnSaveWeeklyBudget.disabled = true;
        const res = await fetch('/api/budget/weekly/current', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });

        const data = await res.json();
        if (res.ok) {
            currentWeeklyBudgetState = data;
            renderCurrentWeeklyBudget(data);
            showToast(`✓ Weekly budget for ${data.weekLabel || 'selected week'} set to ${formatCurrency(amount)}!`, 'success');
            fetchNotifications();
        } else {
            showToast(data.error || 'Failed to save weekly budget', 'error');
        }
    } catch (err) {
        console.error('Error saving current weekly budget:', err);
        showToast('Network error saving weekly budget', 'error');
    } finally {
        if (elements.btnSaveWeeklyBudget) elements.btnSaveWeeklyBudget.disabled = false;
    }
}

// ==========================================================================
// 2. Automatic Budget Distribution Guidelines (Guidelines Only — Never Limits)
// ==========================================================================
async function fetchBudgetGuidelines() {
    try {
        const currentMonth = new Date().toISOString().substring(0, 7);
        const res = await fetch(`/api/budget/guidelines?month=${currentMonth}`, { method: 'GET' });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        state.budgetPlan = data;

        if (data && data.salary > 0) {
            if (elements.budgetSalary && !elements.budgetSalary.value) elements.budgetSalary.value = data.salary;
            if (data.guidelines) {
                const cats = Object.keys(data.guidelines);
                if (cats.length > 0) {
                    state.selectedCategories = new Set(cats);
                    updateCategoryChipsUI();
                }
            }
        }
        renderBudgetGuidelines(data);
    } catch (err) {
        console.error('Failed to load budget guidelines:', err);
    }
}

function updateCategoryChipsUI() {
    if (!elements.budgetCategoryChips) return;
    const chips = elements.budgetCategoryChips.querySelectorAll('.budget-chip');
    chips.forEach(chip => {
        const cat = chip.getAttribute('data-category');
        if (state.selectedCategories.has(cat)) {
            chip.classList.add('chip-active');
        } else {
            chip.classList.remove('chip-active');
        }
    });
}

async function handleGenerateBudgetGuidelines() {
    const salary = Math.max(0, parseInt(elements.budgetSalary?.value, 10) || 0);
    if (salary <= 0) {
        showToast('Please enter your monthly salary first (greater than 0)', 'error');
        elements.budgetSalary?.focus();
        return;
    }

    const categories = Array.from(state.selectedCategories);
    if (categories.length === 0) {
        showToast('Please select at least one category for your budget distribution', 'error');
        return;
    }

    const currentMonth = new Date().toISOString().substring(0, 7);
    const payload = {
        salary: salary,
        categories: categories,
        monthYear: currentMonth,
    };

    try {
        if (elements.btnGenerateGuidelines) elements.btnGenerateGuidelines.disabled = true;
        const res = await fetch('/api/budget/guidelines', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });

        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        state.budgetPlan = data;
        renderBudgetGuidelines(data);
        showToast(`✓ Suggested guidelines for ₹${salary.toLocaleString('en-IN')} saved to MySQL!`, 'success');

        fetchNotifications();
    } catch (err) {
        console.error('Failed to save budget guidelines:', err);
        showToast('Failed to save budget guidelines to MySQL', 'error');
    } finally {
        if (elements.btnGenerateGuidelines) elements.btnGenerateGuidelines.disabled = false;
    }
}

function renderBudgetGuidelines(data) {
    const salary = data?.salary || 0;
    const guidelines = data?.guidelines || {};
    const totalAllocated = data?.totalAllocated || 0;
    const unallocated = data?.unallocated || 0;

    // Stat Cards
    if (elements.guidelineSalary) elements.guidelineSalary.textContent = formatCurrency(salary);
    if (elements.guidelineTotalAllocated) elements.guidelineTotalAllocated.textContent = formatCurrency(totalAllocated);
    if (elements.guidelineUnallocated) elements.guidelineUnallocated.textContent = formatCurrency(unallocated);

    const allocatedPct = salary > 0 ? ((totalAllocated / salary) * 100).toFixed(1) : 0;
    if (elements.guidelineAllocatedPct) {
        elements.guidelineAllocatedPct.textContent = `${allocatedPct}% allocated (~15% unallocated cushion)`;
    }

    // Multi-Bar Distribution & Legend
    const cats = Object.keys(guidelines);
    if (salary > 0 && cats.length > 0 && elements.guidelineMultiBar && elements.guidelineLegend) {
        elements.guidelineMultiBar.innerHTML = cats.map(cat => {
            const detail = guidelines[cat] || {};
            const pct = detail.percentage || 0;
            const meta = CATEGORY_META[cat] || { color: '#64748b' };
            return `<div class="multi-bar" style="width: ${pct}%; background-color: ${meta.color};" title="${escapeHtml(cat)}: ${pct}%"></div>`;
        }).join('');

        elements.guidelineLegend.innerHTML = cats.map(cat => {
            const detail = guidelines[cat] || {};
            const pct = detail.percentage || 0;
            const meta = CATEGORY_META[cat] || { icon: '📦', color: '#64748b' };
            return `
                <span class="legend-item">
                    <span class="legend-dot" style="background-color: ${meta.color};"></span>
                    ${meta.icon} ${escapeHtml(cat)} (<strong>${pct}%</strong>)
                </span>
            `;
        }).join('') + `
            <span class="legend-item">
                <span class="legend-dot" style="background-color: #cbd5e1;"></span>
                🛡️ Flexible Cushion (<strong>${(100 - allocatedPct).toFixed(1)}%</strong>)
            </span>
        `;
    } else {
        if (elements.guidelineMultiBar) {
            elements.guidelineMultiBar.innerHTML = `<div class="multi-bar bar-empty" style="width: 100%; background: #e2e8f0;"></div>`;
        }
        if (elements.guidelineLegend) {
            elements.guidelineLegend.innerHTML = '';
        }
    }

    renderGuidelinesVsActualTable(guidelines);
}

function renderGuidelinesVsActualTable(guidelines) {
    if (!elements.guidelinesVsActualTbody) return;

    const cats = Object.keys(guidelines || {});
    if (cats.length === 0) {
        elements.guidelinesVsActualTbody.innerHTML = `
            <tr>
                <td colspan="5" class="empty-state">
                    Enter salary and click "Generate &amp; Save Guidelines" or confirm via Assistant to view suggested guidelines vs actual spending.
                </td>
            </tr>
        `;
        return;
    }

    const curCatsSpending = (state.monthlyComparison && state.monthlyComparison.currentCategories) || {};

    elements.guidelinesVsActualTbody.innerHTML = cats.map(cat => {
        const detail = guidelines[cat] || {};
        const guidelineAmount = detail.amount || 0;
        const guidelinePct = detail.percentage || 0;
        const actualSpent = curCatsSpending[cat] || 0;
        const meta = CATEGORY_META[cat] || { icon: '📦', color: '#64748b' };

        const isWithin = actualSpent <= guidelineAmount;
        const diff = guidelineAmount - actualSpent;
        const pacePct = guidelineAmount > 0 ? Math.round((actualSpent / guidelineAmount) * 100) : 0;

        let statusHtml = '';
        let paceClass = 'normal';

        if (isWithin) {
            statusHtml = `
                <span class="guideline-badge-status badge-within">
                    ✓ Within guideline (₹${diff.toLocaleString('en-IN')} available)
                </span>
            `;
            paceClass = pacePct > 75 ? 'warning' : 'normal';
        } else {
            const over = Math.abs(diff);
            statusHtml = `
                <span class="guideline-badge-status badge-flexible-note">
                    ₹${over.toLocaleString('en-IN')} above guideline <span style="font-weight: normal; opacity: 0.85;">(Flexible)</span>
                </span>
            `;
            paceClass = 'exceeded';
        }

        const barWidth = Math.min(100, Math.max(3, pacePct));

        return `
            <tr>
                <td>
                    <span style="font-size: 1.1rem; vertical-align: middle; margin-right: 6px;">${meta.icon}</span>
                    <strong>${escapeHtml(cat)}</strong>
                </td>
                <td>
                    <strong>${formatCurrency(guidelineAmount)}</strong>
                    <span style="font-size: 0.75rem; color: var(--text-muted); display: block;">${guidelinePct}% of salary</span>
                </td>
                <td>
                    <span style="font-weight: 700; color: ${actualSpent > 0 ? 'var(--text-main)' : 'var(--text-muted)'};">
                        ${formatCurrency(actualSpent)}
                    </span>
                </td>
                <td>
                    ${statusHtml}
                </td>
                <td>
                    <div class="spending-pace-box">
                        <div class="pace-track">
                            <div class="pace-fill ${paceClass}" style="width: ${barWidth}%;"></div>
                        </div>
                        <div class="pace-label">
                            <span>${actualSpent > 0 ? `${pacePct}% of suggested` : '0% used'}</span>
                            <span style="color: var(--text-muted);">Target: ${formatCurrency(guidelineAmount)}</span>
                        </div>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function handleResetGuidelines() {
    if (elements.budgetSalary) elements.budgetSalary.value = '';
    state.selectedCategories = new Set(['Food', 'Rent', 'Savings', 'Education', 'Others']);
    updateCategoryChipsUI();
    state.budgetPlan = null;
    renderBudgetGuidelines(null);
    showToast('Budget guidelines input reset', 'success');
}

// ==========================================================================
// 3. Month-over-Month Comparison Engine
// ==========================================================================
async function fetchMonthlyComparison() {
    try {
        const res = await fetch('/api/expenses/monthly-comparison', { method: 'GET' });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        state.monthlyComparison = data;
        renderMonthlyComparison(data);

        if (state.budgetPlan && state.budgetPlan.guidelines) {
            renderGuidelinesVsActualTable(state.budgetPlan.guidelines);
        }
    } catch (err) {
        console.error('Failed to load monthly comparison:', err);
    }
}

function renderMonthlyComparison(data) {
    if (!data) return;

    const curMonth = data.currentMonth || '';
    const prevMonth = data.previousMonth || '';
    const curTotal = data.currentTotal || 0;
    const prevTotal = data.previousTotal || 0;
    const diff = data.difference || 0;
    const pctChange = data.percentageChange || 0;

    if (elements.comparisonMonthSubtitle) {
        elements.comparisonMonthSubtitle.textContent = `Comparing ${curMonth} vs ${prevMonth}`;
    }

    if (elements.labelPrevMonth) elements.labelPrevMonth.textContent = `${prevMonth} Spending`;
    if (elements.labelCurMonth) elements.labelCurMonth.textContent = `${curMonth} Spending`;

    if (elements.valPrevTotal) elements.valPrevTotal.textContent = formatCurrency(prevTotal);
    if (elements.valCurTotal) elements.valCurTotal.textContent = formatCurrency(curTotal);

    if (elements.valDiff) {
        const sign = diff > 0 ? '+' : (diff < 0 ? '-' : '');
        elements.valDiff.textContent = `${sign}${formatCurrency(Math.abs(diff))}`;
        if (diff > 0) {
            elements.valDiff.className = 'comp-val diff-increase';
            if (elements.valDiffSub) elements.valDiffSub.textContent = 'Higher spending than last month';
        } else if (diff < 0) {
            elements.valDiff.className = 'comp-val diff-decrease';
            if (elements.valDiffSub) elements.valDiffSub.textContent = 'Lower spending than last month';
        } else {
            elements.valDiff.className = 'comp-val diff-equal';
            if (elements.valDiffSub) elements.valDiffSub.textContent = 'Equal spending to last month';
        }
    }

    if (elements.valPctChange) {
        const sign = pctChange > 0 ? '+' : '';
        elements.valPctChange.textContent = `${sign}${pctChange.toFixed(1)}%`;
        if (pctChange > 0) {
            elements.valPctChange.className = 'comp-val diff-increase';
            if (elements.valPctSub) elements.valPctSub.textContent = 'Spending increased';
        } else if (pctChange < 0) {
            elements.valPctChange.className = 'comp-val diff-decrease';
            if (elements.valPctSub) elements.valPctSub.textContent = 'Spending decreased';
        } else {
            elements.valPctChange.className = 'comp-val diff-equal';
            if (elements.valPctSub) elements.valPctSub.textContent = 'No change in spending';
        }
    }

    if (elements.comparisonTbody) {
        const curCats = data.currentCategories || {};
        const prevCats = data.previousCategories || {};

        const allCats = Array.from(new Set([...Object.keys(curCats), ...Object.keys(prevCats)])).sort();

        if (allCats.length === 0) {
            elements.comparisonTbody.innerHTML = `
                <tr>
                    <td colspan="5" class="empty-state">No transaction records found for ${curMonth} or ${prevMonth}.</td>
                </tr>
            `;
            return;
        }

        elements.comparisonTbody.innerHTML = allCats.map(cat => {
            const pVal = prevCats[cat] || 0;
            const cVal = curCats[cat] || 0;
            const catDiff = cVal - pVal;
            const meta = CATEGORY_META[cat] || { icon: '📦', color: '#64748b' };

            let pillClass = 'neutral';
            let pillText = '₹0 (0%)';
            if (catDiff > 0) {
                pillClass = 'increase';
                const p = pVal > 0 ? ((catDiff / pVal) * 100).toFixed(0) : '100';
                pillText = `+${formatCurrency(catDiff)} (+${p}%)`;
            } else if (catDiff < 0) {
                pillClass = 'decrease';
                const p = pVal > 0 ? ((Math.abs(catDiff) / pVal) * 100).toFixed(0) : '0';
                pillText = `-${formatCurrency(Math.abs(catDiff))} (-${p}%)`;
            }

            const maxVal = Math.max(pVal, cVal, 1);
            const prevWidth = Math.round((pVal / maxVal) * 100);
            const curWidth = Math.round((cVal / maxVal) * 100);

            return `
                <tr>
                    <td>
                        <span style="font-size: 1.1rem; margin-right: 6px;">${meta.icon}</span>
                        <strong>${escapeHtml(cat)}</strong>
                    </td>
                    <td>${formatCurrency(pVal)}</td>
                    <td><strong>${formatCurrency(cVal)}</strong></td>
                    <td>
                        <span class="diff-pill ${pillClass}">${pillText}</span>
                    </td>
                    <td>
                        <div class="comp-bar-container">
                            <span style="font-size: 0.72rem; color: #94a3b8; width: 35px;">Prev</span>
                            <div class="comp-bar-track">
                                <div class="comp-bar-prev" style="width: ${prevWidth}%;"></div>
                            </div>
                        </div>
                        <div class="comp-bar-container" style="margin-top: 4px;">
                            <span style="font-size: 0.72rem; color: #4f46e5; font-weight: 600; width: 35px;">Cur</span>
                            <div class="comp-bar-track">
                                <div class="comp-bar-cur" style="width: ${curWidth}%;"></div>
                            </div>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    }
}

// ==========================================================================
// 4. API Operations: Expenses CRUD & Summaries
// ==========================================================================
async function fetchExpenses() {
    try {
        const response = await fetch(`${API_BASE}`, { method: 'GET' });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const data = await response.json();
        state.expenses = Array.isArray(data) ? data : [];
        renderExpensesTable();
        updateStatus(true);
    } catch (err) {
        console.error('Failed to fetch expenses:', err);
        updateStatus(false);
        if (elements.expensesTbody) {
            elements.expensesTbody.innerHTML = `
                <tr>
                    <td colspan="6" class="empty-state">Unable to connect to backend server. Please check port 8080.</td>
                </tr>
            `;
        }
    }
}

async function fetchTotal() {
    try {
        const response = await fetch(`${API_BASE}/total`, { method: 'GET' });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const data = await response.json();
        const total = data.total || 0;
        if (elements.totalSpending) elements.totalSpending.textContent = formatCurrency(total);
        if (elements.totalTransactions) elements.totalTransactions.textContent = `${state.expenses.length} transaction${state.expenses.length === 1 ? '' : 's'} recorded`;
    } catch (err) {
        console.error('Failed to fetch total:', err);
    }
}

async function fetchCategorySummary() {
    try {
        const response = await fetch(`${API_BASE}/category-summary`, { method: 'GET' });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const summary = await response.json();
        renderCategorySummary(summary);
    } catch (err) {
        console.error('Failed to fetch category summary:', err);
        if (elements.categorySummaryContainer) {
            elements.categorySummaryContainer.innerHTML = `
                <div class="empty-state">Unable to load category summary.</div>
            `;
        }
    }
}

async function fetchInsights() {
    try {
        const response = await fetch(`${API_BASE}/insights`, { method: 'GET' });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const insights = await response.json();
        renderSpendingInsights(insights);
    } catch (err) {
        console.error('Failed to fetch spending insights:', err);
        if (elements.insightsContainer) {
            elements.insightsContainer.innerHTML = `
                <div class="empty-state">Unable to load insights.</div>
            `;
        }
    }
}

function renderCategorySummary(summary) {
    const entries = Object.entries(summary || {});
    if (elements.activeCategoriesCount) elements.activeCategoriesCount.textContent = entries.length;

    if (!elements.categorySummaryContainer) return;

    if (entries.length === 0) {
        if (elements.topCategoryName) elements.topCategoryName.textContent = 'Top Category: None';
        elements.categorySummaryContainer.innerHTML = `
            <div class="empty-state">No categorized expenses recorded yet.</div>
        `;
        return;
    }

    entries.sort((a, b) => b[1] - a[1]);
    const [topCat, topVal] = entries[0];
    if (elements.topCategoryName) elements.topCategoryName.textContent = `Top Category: ${topCat} (${formatCurrency(topVal)})`;

    elements.categorySummaryContainer.innerHTML = entries.map(([cat, total]) => {
        const meta = CATEGORY_META[cat] || { icon: '🏷️' };
        return `
            <div class="summary-item">
                <span class="summary-cat">
                    <span class="category-badge badge-${cat}">${meta.icon} ${escapeHtml(cat)}</span>
                </span>
                <span class="summary-amount">${formatCurrency(total)}</span>
            </div>
        `;
    }).join('');
}

function renderSpendingInsights(insights) {
    if (!elements.insightsContainer) return;

    const entries = Object.entries(insights || {});

    if (entries.length === 0) {
        elements.insightsContainer.innerHTML = `
            <div class="empty-state">Add expenses to see percentage distribution.</div>
        `;
        return;
    }

    entries.sort((a, b) => b[1] - a[1]);

    elements.insightsContainer.innerHTML = entries.map(([cat, pct]) => {
        const pctFormatted = Number(pct).toFixed(1);
        const meta = CATEGORY_META[cat] || { color: 'var(--primary)' };
        return `
            <div class="insight-row">
                <div class="insight-labels">
                    <span class="insight-name">${escapeHtml(cat)}</span>
                    <span class="insight-pct">${pctFormatted}%</span>
                </div>
                <div class="insight-progress-track">
                    <div class="insight-progress-fill" style="width: ${pctFormatted}%; background-color: ${meta.color};"></div>
                </div>
            </div>
        `;
    }).join('');
}

function renderExpensesTable() {
    if (!elements.expensesTbody) return;

    const filter = (elements.searchInput?.value || '').toLowerCase().trim();

    let list = state.expenses;
    if (filter) {
        list = list.filter(item =>
            (item.category && item.category.toLowerCase().includes(filter)) ||
            (item.description && item.description.toLowerCase().includes(filter)) ||
            (item.date && item.date.includes(filter))
        );
    }

    // Check if on Home Page (index.html) -> only show top 5 recent expenses
    const isHomePage = window.location.pathname.endsWith('index.html') || window.location.pathname.endsWith('/') || window.location.pathname === '';
    const displayList = isHomePage ? list.slice(0, 5) : list;

    if (displayList.length === 0) {
        const colspan = isHomePage ? 5 : 6;
        elements.expensesTbody.innerHTML = `
            <tr>
                <td colspan="${colspan}" class="empty-state">
                    ${filter ? 'No matching expenses found.' : 'No expenses recorded yet. Add your first expense above or via Assistant!'}
                </td>
            </tr>
        `;
        return;
    }

    elements.expensesTbody.innerHTML = displayList.map(exp => {
        const meta = CATEGORY_META[exp.category] || { icon: '🏷️' };

        if (isHomePage) {
            return `
                <tr>
                    <td><span class="expense-id-tag">#${exp.id}</span></td>
                    <td>${escapeHtml(exp.date)}</td>
                    <td>
                        <span class="category-badge badge-${exp.category || 'Other'}">
                            ${meta.icon} ${escapeHtml(exp.category || 'Other')}
                        </span>
                    </td>
                    <td class="desc-cell" title="${escapeHtml(exp.description || '')}">
                        ${escapeHtml(exp.description || '—')}
                    </td>
                    <td class="amount-cell">${formatCurrency(exp.amount)}</td>
                </tr>
            `;
        }

        return `
            <tr>
                <td><span class="expense-id-tag">#${exp.id}</span></td>
                <td>${escapeHtml(exp.date)}</td>
                <td>
                    <span class="category-badge badge-${exp.category || 'Other'}">
                        ${meta.icon} ${escapeHtml(exp.category || 'Other')}
                    </span>
                </td>
                <td class="desc-cell" title="${escapeHtml(exp.description || '')}">
                    ${escapeHtml(exp.description || '—')}
                </td>
                <td class="amount-cell">${formatCurrency(exp.amount)}</td>
                <td class="actions-cell">
                    <button class="btn btn-sm btn-secondary" onclick="handleEditClick(${exp.id})" title="Edit expense">
                        ✏️ Edit
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="handleDeleteClick(${exp.id})" title="Delete expense">
                        🗑️ Delete
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

// ==========================================================================
// Form Operations (Add / Edit Expense)
// ==========================================================================
async function handleFormSubmit(e) {
    e.preventDefault();

    const id = elements.expenseIdInput?.value;
    const amount = parseInt(elements.amountInput?.value, 10);
    const category = elements.categorySelect?.value;
    const date = elements.dateInput?.value;
    const description = (elements.descriptionInput?.value || '').trim();

    if (!amount || amount <= 0) {
        showToast('Please enter a valid positive amount', 'error');
        elements.amountInput?.focus();
        return;
    }

    if (!category) {
        showToast('Please select a category', 'error');
        elements.categorySelect?.focus();
        return;
    }

    if (!date) {
        showToast('Please select a valid date', 'error');
        elements.dateInput?.focus();
        return;
    }

    const payload = { amount, category, date, description };

    try {
        if (elements.btnSubmit) elements.btnSubmit.disabled = true;

        if (id) {
            // Update Existing Expense (PUT)
            const response = await fetch(`${API_BASE}/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
            });

            const result = await response.json();
            if (response.ok) {
                showToast(`Expense #${id} updated successfully!`, 'success');
                resetForm();
                if (elements.editExpensePanel) elements.editExpensePanel.style.display = 'none';
                await loadAll();
            } else {
                showToast(result.error || 'Failed to update expense.', 'error');
            }
        } else {
            // Create New Expense (POST)
            const response = await fetch(`${API_BASE}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
            });

            const result = await response.json();
            if (response.ok) {
                showToast('✓ Expense added successfully to MySQL!', 'success');
                resetForm();
                await loadAll();
            } else {
                showToast(result.error || 'Failed to add expense.', 'error');
            }
        }
    } catch (err) {
        console.error('Error submitting form:', err);
        showToast('Network error: Unable to contact server', 'error');
    } finally {
        if (elements.btnSubmit) elements.btnSubmit.disabled = false;
    }
}

window.handleEditClick = function(id) {
    const expense = state.expenses.find(e => e.id === id);
    if (!expense) return;

    state.editingId = id;
    if (elements.expenseIdInput) elements.expenseIdInput.value = expense.id;
    if (elements.amountInput) elements.amountInput.value = expense.amount;
    if (elements.categorySelect) elements.categorySelect.value = expense.category;
    if (elements.dateInput) elements.dateInput.value = expense.date;
    if (elements.descriptionInput) elements.descriptionInput.value = expense.description || '';

    if (elements.editExpensePanel) {
        elements.editExpensePanel.style.display = 'block';
        elements.editExpensePanel.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    if (elements.formTitle) elements.formTitle.textContent = `Edit Expense #${id}`;
    if (elements.formBadge) {
        elements.formBadge.textContent = `Editing #${id}`;
        elements.formBadge.className = 'badge badge-edit';
    }
    if (elements.btnSubmit) elements.btnSubmit.innerHTML = '<span>💾 Save Changes</span>';
    if (elements.btnCancel) elements.btnCancel.style.display = 'inline-flex';

    elements.amountInput?.focus();
};

function resetForm() {
    state.editingId = null;
    if (elements.form) elements.form.reset();
    if (elements.expenseIdInput) elements.expenseIdInput.value = '';
    if (elements.formTitle) elements.formTitle.textContent = 'Add New Expense';
    if (elements.formBadge) {
        elements.formBadge.textContent = 'New';
        elements.formBadge.className = 'badge badge-add';
    }
    if (elements.btnSubmit) elements.btnSubmit.innerHTML = '<span>➕ Add Expense</span>';
    if (elements.btnCancel) elements.btnCancel.style.display = 'none';
    if (elements.editExpensePanel) elements.editExpensePanel.style.display = 'none';
    setDefaultDate();
}

window.handleDeleteClick = async function(id) {
    const confirmDelete = window.confirm(`Are you sure you want to delete Expense #${id}?`);
    if (!confirmDelete) return;

    try {
        const response = await fetch(`${API_BASE}/${id}`, { method: 'DELETE' });
        const result = await response.json();

        if (response.ok) {
            showToast(`Expense #${id} deleted successfully.`, 'success');
            if (state.editingId === id) {
                resetForm();
            }
            await loadAll();
        } else {
            showToast(result.error || 'Failed to delete expense.', 'error');
        }
    } catch (err) {
        console.error('Error deleting expense:', err);
        showToast('Network error: Unable to contact server', 'error');
    }
};

// ==========================================================================
// 5. AI Assistant & Speech Recognition Logic
// ==========================================================================
let speechRecognitionInstance = null;
let isRecordingSpeech = false;
let currentParsedAiResult = null;

function initSpeechRecognition() {
    const SpeechRec = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRec) {
        console.warn('Web Speech API is not supported in this browser.');
        return null;
    }

    const rec = new SpeechRec();
    rec.continuous = false;
    rec.interimResults = true;
    rec.maxAlternatives = 1;

    rec.onstart = () => {
        isRecordingSpeech = true;
        if (elements.btnMic) elements.btnMic.classList.add('listening');
        if (elements.micLabel) elements.micLabel.textContent = 'Listening...';
        if (elements.micStatus) elements.micStatus.style.display = 'flex';
        if (elements.micStatusText) {
            const lang = elements.assistantLang?.value === 'te-IN' ? 'Telugu (తెలుగు)' : 'English / Tanglish';
            elements.micStatusText.textContent = `Listening in ${lang}... Speak now`;
        }
    };

    rec.onresult = (event) => {
        let finalTranscript = '';
        let interimTranscript = '';
        for (let i = event.resultIndex; i < event.results.length; i++) {
            const transcript = event.results[i][0].transcript;
            if (event.results[i].isFinal) {
                finalTranscript += transcript;
            } else {
                interimTranscript += transcript;
            }
        }
        const combined = finalTranscript || interimTranscript;
        if (elements.assistantInput && combined) {
            elements.assistantInput.value = combined;
        }
    };

    rec.onerror = (event) => {
        console.error('Speech recognition error:', event.error);
        stopSpeechRecognition();
        if (event.error === 'not-allowed') {
            showToast('Microphone access was denied. Please allow microphone permissions in browser.', 'error');
        } else if (event.error !== 'no-speech') {
            showToast(`Speech error: ${event.error}`, 'error');
        }
    };

    rec.onend = () => {
        stopSpeechRecognition();
        if (elements.assistantInput && elements.assistantInput.value.trim().length > 0) {
            analyzeAiInput();
        }
    };

    return rec;
}

function toggleSpeechRecognition() {
    const SpeechRec = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRec) {
        showToast('Speech-to-Text is not supported by this browser. Please use Chrome/Edge or type directly.', 'error');
        return;
    }

    if (!speechRecognitionInstance) {
        speechRecognitionInstance = initSpeechRecognition();
    }

    if (!speechRecognitionInstance) return;

    if (isRecordingSpeech) {
        speechRecognitionInstance.stop();
        stopSpeechRecognition();
    } else {
        speechRecognitionInstance.lang = elements.assistantLang?.value || 'te-IN';
        try {
            speechRecognitionInstance.start();
        } catch (e) {
            console.error('Failed to start speech recognition', e);
            stopSpeechRecognition();
        }
    }
}

function stopSpeechRecognition() {
    isRecordingSpeech = false;
    if (elements.btnMic) elements.btnMic.classList.remove('listening');
    if (elements.micLabel) elements.micLabel.textContent = 'Speak';
    if (elements.micStatus) elements.micStatus.style.display = 'none';
}

async function analyzeAiInput() {
    const text = (elements.assistantInput?.value || '').trim();
    if (!text) {
        showToast('Please type or speak a sentence first', 'error');
        elements.assistantInput?.focus();
        return;
    }

    try {
        if (elements.btnParse) {
            elements.btnParse.disabled = true;
            elements.btnParse.innerHTML = '<span>⏳ Analyzing...</span>';
        }

        const response = await fetch('/api/ai/parse', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json; charset=utf-8' },
            body: JSON.stringify({ text }),
        });

        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const result = await response.json();
        currentParsedAiResult = result;
        renderConfirmationCard(result);
    } catch (err) {
        console.error('Failed to analyze input:', err);
        showToast('Unable to reach AI parser service', 'error');
    } finally {
        if (elements.btnParse) {
            elements.btnParse.disabled = false;
            elements.btnParse.innerHTML = '<span>⚡ Understand</span>';
        }
    }
}

function renderConfirmationCard(res) {
    if (!elements.confirmationCard || !elements.confirmationBody) return;

    const isExpense = res.intent === 'EXPENSE';
    const isBudget = res.intent === 'BUDGET';

    if (elements.confirmIntentBadge) {
        elements.confirmIntentBadge.textContent = isExpense ? 'Expense Detected' : (isBudget ? 'Budget Plan Detected' : 'Unclear Input');
        elements.confirmIntentBadge.className = `badge ${isExpense ? 'badge-Food' : (isBudget ? 'badge-edit' : 'badge-Other')}`;
    }

    if (elements.confirmConfidence) {
        const pct = Math.round((res.confidence || 0.9) * 100);
        elements.confirmConfidence.textContent = `${pct}% confidence (${res.parserSource === 'LOCAL_SEMANTIC_ENGINE' ? 'Local Semantic Engine' : 'AI API'})`;
    }

    if (isExpense) {
        elements.confirmationBody.innerHTML = `
            <div class="confirm-grid">
                <div class="confirm-item">
                    <span class="confirm-label">Amount</span>
                    <span class="confirm-value amount-highlight">₹${(res.amount || 0).toLocaleString('en-IN')}</span>
                </div>
                <div class="confirm-item">
                    <span class="confirm-label">Category</span>
                    <span class="confirm-value"><span class="category-badge badge-${res.category || 'Other'}">${escapeHtml(res.category || 'Other')}</span></span>
                </div>
                <div class="confirm-item">
                    <span class="confirm-label">Date</span>
                    <span class="confirm-value">${escapeHtml(res.date || '')}</span>
                </div>
                <div class="confirm-item">
                    <span class="confirm-label">Description</span>
                    <span class="confirm-value">${escapeHtml(res.description || '—')}</span>
                </div>
            </div>
            <p class="confirm-note">💡 Click <strong>Confirm</strong> to save this expense directly to MySQL, or <strong>Edit</strong> to modify fields in the form.</p>
        `;
        if (elements.btnConfirmApply) {
            elements.btnConfirmApply.innerHTML = '<span>✓ Confirm &amp; Save Expense to MySQL</span>';
            elements.btnConfirmApply.disabled = res.amount <= 0;
        }
    } else if (isBudget) {
        const cats = Array.isArray(res.categories) ? res.categories : [];
        const catsHtml = cats.length > 0
            ? cats.map(c => `<span class="category-badge badge-${c}">${escapeHtml(c)}</span>`).join(' ')
            : '<span style="color:var(--text-muted);">Default Baseline Categories</span>';

        elements.confirmationBody.innerHTML = `
            <div class="confirm-grid">
                <div class="confirm-item">
                    <span class="confirm-label">Monthly Salary</span>
                    <span class="confirm-value amount-highlight">₹${(res.salary || 0).toLocaleString('en-IN')}</span>
                </div>
                <div class="confirm-item full-width">
                    <span class="confirm-label">Selected Categories for Budget Guidelines</span>
                    <div class="confirm-categories-list">${catsHtml}</div>
                </div>
            </div>
            <p class="confirm-note">💡 Click <strong>Confirm</strong> to automatically compute guidelines and store them in MySQL (Guidelines only, not limits).</p>
        `;
        if (elements.btnConfirmApply) {
            elements.btnConfirmApply.innerHTML = '<span>✓ Confirm &amp; Generate Guidelines in MySQL</span>';
            elements.btnConfirmApply.disabled = res.salary <= 0;
        }
    } else {
        elements.confirmationBody.innerHTML = `
            <p style="color:var(--danger); padding: 10px 0;">Could not reliably determine whether this is an expense or a budget plan. Please provide more detail (e.g. mention amounts and categories like food, rent, or salary).</p>
        `;
        if (elements.btnConfirmApply) {
            elements.btnConfirmApply.disabled = true;
        }
    }

    elements.confirmationCard.style.display = 'block';
    elements.confirmationCard.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

async function handleConfirmApply() {
    if (!currentParsedAiResult) return;

    if (currentParsedAiResult.intent === 'EXPENSE') {
        const payload = {
            amount: currentParsedAiResult.amount,
            category: currentParsedAiResult.category || 'Other',
            date: currentParsedAiResult.date || new Date().toISOString().substring(0, 10),
            description: currentParsedAiResult.description || '',
        };

        if (payload.amount <= 0) {
            showToast('Cannot save expense: amount must be greater than 0', 'error');
            return;
        }

        try {
            if (elements.btnConfirmApply) elements.btnConfirmApply.disabled = true;
            const res = await fetch('/api/expenses', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
            });
            const data = await res.json();
            if (res.ok) {
                showToast(`✓ Expense of ₹${payload.amount.toLocaleString('en-IN')} for ${payload.category} saved to MySQL!`, 'success');
                dismissConfirmationCard();
                if (elements.assistantInput) elements.assistantInput.value = '';
                await loadAll();
            } else {
                showToast(data.error || 'Failed to save expense', 'error');
            }
        } catch (e) {
            showToast('Network error saving expense', 'error');
        } finally {
            if (elements.btnConfirmApply) elements.btnConfirmApply.disabled = false;
        }
    } else if (currentParsedAiResult.intent === 'BUDGET') {
        const payload = {
            salary: currentParsedAiResult.salary,
            categories: currentParsedAiResult.categories || [],
            monthYear: new Date().toISOString().substring(0, 7),
        };

        if (payload.salary <= 0) {
            showToast('Cannot generate budget: salary must be greater than 0', 'error');
            return;
        }

        try {
            if (elements.btnConfirmApply) elements.btnConfirmApply.disabled = true;
            const res = await fetch('/api/budget/guidelines', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
            });
            const data = await res.json();
            if (res.ok) {
                showToast(`✓ Monthly budget guidelines for ₹${payload.salary.toLocaleString('en-IN')} saved to MySQL!`, 'success');
                dismissConfirmationCard();
                if (elements.assistantInput) elements.assistantInput.value = '';

                if (elements.budgetSalary) elements.budgetSalary.value = payload.salary;
                if (payload.categories.length > 0) {
                    state.selectedCategories = new Set(payload.categories);
                    updateCategoryChipsUI();
                }
                state.budgetPlan = data;
                renderBudgetGuidelines(data);
                fetchNotifications();
            } else {
                showToast(data.error || 'Failed to save budget guidelines', 'error');
            }
        } catch (e) {
            showToast('Network error saving budget guidelines', 'error');
        } finally {
            if (elements.btnConfirmApply) elements.btnConfirmApply.disabled = false;
        }
    }
}

function handleConfirmEdit() {
    if (!currentParsedAiResult) return;

    if (currentParsedAiResult.intent === 'EXPENSE') {
        // If on assistant.html, redirect to add-expense.html with parameters
        const url = `add-expense.html?amount=${currentParsedAiResult.amount || 0}&category=${encodeURIComponent(currentParsedAiResult.category || '')}&date=${encodeURIComponent(currentParsedAiResult.date || '')}&description=${encodeURIComponent(currentParsedAiResult.description || '')}`;
        window.location.href = url;
    } else if (currentParsedAiResult.intent === 'BUDGET') {
        // If on assistant.html, redirect to budget.html with parameters
        const cats = (currentParsedAiResult.categories || []).join(',');
        const url = `budget.html?salary=${currentParsedAiResult.salary || 0}&categories=${encodeURIComponent(cats)}`;
        window.location.href = url;
    }
}

function dismissConfirmationCard() {
    currentParsedAiResult = null;
    if (elements.confirmationCard) elements.confirmationCard.style.display = 'none';
}

// ==========================================================================
// 6. Master Data Coordination (Page-Aware)
// ==========================================================================
async function loadAll() {
    const tasks = [];

    // Always fetch notifications for global header badge
    tasks.push(fetchNotifications());

    // Fetch summary metrics if present on page
    if (elements.totalSpending || elements.activeCategoriesCount) {
        tasks.push(fetchTotal(), fetchCategorySummary());
    }

    // Fetch expenses if table is present
    if (elements.expensesTbody) {
        tasks.push(fetchExpenses());
    }

    // Fetch insights if container is present
    if (elements.insightsContainer) {
        tasks.push(fetchInsights());
    }

    // Fetch monthly comparison if comparison section is present
    if (elements.comparisonTbody || elements.valCurTotal) {
        tasks.push(fetchMonthlyComparison());
    }

    // Fetch budget guidelines if budget section is present
    if (elements.guidelinesVsActualTbody || elements.budgetSalary) {
        tasks.push(fetchBudgetGuidelines());
    }

    // Initialize budget selectors if elements exist
    if (elements.cbMonthSelect) {
        initMonthlyBudgetSelector();
    }
    if (elements.wbWeekSelect) {
        initWeeklyBudgetSelector();
    }

    // Fetch current monthly budget if card is present
    if (elements.cbBudgetVal || elements.setMonthlyBudgetForm) {
        tasks.push(fetchCurrentMonthlyBudget(state.selectedMonth));
    }

    // Fetch current weekly budget if card is present
    if (elements.wbBudgetVal || elements.setWeeklyBudgetForm) {
        tasks.push(fetchCurrentWeeklyBudget(state.selectedWeek));
    }

    await Promise.allSettled(tasks);
}

// ==========================================================================
// 7. Event Listeners Initialization
// ==========================================================================
document.addEventListener('DOMContentLoaded', () => {
    setDefaultDate();
    updateActiveNavLink();
    checkUrlParameters();
    loadAll();

    // Notification Center Controls
    if (elements.btnToggleNotifs) {
        elements.btnToggleNotifs.addEventListener('click', () => {
            window.location.href = 'notifications.html';
        });
    }

    if (elements.btnMarkAllRead) {
        elements.btnMarkAllRead.addEventListener('click', markAllNotificationsRead);
    }

    if (elements.btnRefreshNotifications) {
        elements.btnRefreshNotifications.addEventListener('click', () => {
            fetchNotifications();
            showToast('Notifications checked', 'success');
        });
    }

    // AI Assistant Handlers
    if (elements.btnMic) {
        elements.btnMic.addEventListener('click', toggleSpeechRecognition);
    }

    if (elements.btnParse) {
        elements.btnParse.addEventListener('click', analyzeAiInput);
    }

    if (elements.btnClearAssistant) {
        elements.btnClearAssistant.addEventListener('click', () => {
            if (elements.assistantInput) elements.assistantInput.value = '';
            dismissConfirmationCard();
        });
    }

    if (elements.assistantInput) {
        elements.assistantInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                analyzeAiInput();
            }
        });
    }

    // Confirmation Card Buttons
    if (elements.btnConfirmApply) {
        elements.btnConfirmApply.addEventListener('click', handleConfirmApply);
    }

    if (elements.btnConfirmEdit) {
        elements.btnConfirmEdit.addEventListener('click', handleConfirmEdit);
    }

    if (elements.btnConfirmCancel) {
        elements.btnConfirmCancel.addEventListener('click', dismissConfirmationCard);
    }

    if (elements.btnDismissConfirm) {
        elements.btnDismissConfirm.addEventListener('click', dismissConfirmationCard);
    }

    // Assistant Quick Example Chips
    document.querySelectorAll('.assistant-chips .chip').forEach(chip => {
        chip.addEventListener('click', () => {
            const example = chip.getAttribute('data-example');
            if (example && elements.assistantInput) {
                elements.assistantInput.value = example;
                analyzeAiInput();
            }
        });
    });

    // Budget Category Selection Chips
    if (elements.budgetCategoryChips) {
        elements.budgetCategoryChips.addEventListener('click', (e) => {
            const chip = e.target.closest('.budget-chip');
            if (!chip) return;
            const cat = chip.getAttribute('data-category');
            if (!cat) return;

            if (state.selectedCategories.has(cat)) {
                state.selectedCategories.delete(cat);
                chip.classList.remove('chip-active');
            } else {
                state.selectedCategories.add(cat);
                chip.classList.add('chip-active');
            }
        });
    }

    // Budget Guidelines Actions
    if (elements.btnGenerateGuidelines) {
        elements.btnGenerateGuidelines.addEventListener('click', handleGenerateBudgetGuidelines);
    }

    if (elements.btnResetGuidelines) {
        elements.btnResetGuidelines.addEventListener('click', handleResetGuidelines);
    }

    // Current Monthly Budget Form Handler
    if (elements.setMonthlyBudgetForm) {
        elements.setMonthlyBudgetForm.addEventListener('submit', handleSetMonthlyBudget);
    }

    // Current Weekly Budget Form Handler
    if (elements.setWeeklyBudgetForm) {
        elements.setWeeklyBudgetForm.addEventListener('submit', handleSetWeeklyBudget);
    }

    // Monthly Comparison Refresh
    if (elements.btnRefreshComparison) {
        elements.btnRefreshComparison.addEventListener('click', () => {
            fetchMonthlyComparison();
            showToast('Monthly comparison updated', 'success');
        });
    }

    // Expense Form Handlers
    if (elements.form) {
        elements.form.addEventListener('submit', handleFormSubmit);
    }

    if (elements.btnCancel) {
        elements.btnCancel.addEventListener('click', resetForm);
    }

    if (elements.btnRefresh) {
        elements.btnRefresh.addEventListener('click', () => {
            loadAll();
            showToast('Data refreshed', 'success');
        });
    }

    if (elements.searchInput) {
        elements.searchInput.addEventListener('input', renderExpensesTable);
    }
});
