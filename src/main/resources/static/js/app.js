/**
 * Operations Under Pressure — Client Application
 * Vanilla JavaScript (ES6+), Fetch API, Safe DOM manipulation
 */

(function () {
    'use strict';

    // Application State
    const state = {
        token: sessionStorage.getItem('ops_token') || null,
        user: null,
        teams: [],
        currentView: 'dashboard',
        currentPreset: 'all',
        selectedTeamId: '',
        currentPage: 0,
        pageSize: 20,
        totalPages: 1,
        totalElements: 0,
        filters: {
            search: '',
            status: '',
            priority: '',
            type: ''
        },
        activeItem: null,
        teamMembersCache: {}
    };

    // DOM Elements Cache
    const el = {
        loginView: document.getElementById('login-view'),
        appView: document.getElementById('app-view'),
        loginForm: document.getElementById('login-form'),
        loginError: document.getElementById('login-error'),
        loginUsername: document.getElementById('login-username'),
        loginPassword: document.getElementById('login-password'),
        btnTogglePassword: document.getElementById('btn-toggle-password'),
        btnLoginSubmit: document.getElementById('btn-login-submit'),
        btnLogout: document.getElementById('btn-logout'),

        currentUserAvatar: document.getElementById('current-user-avatar'),
        currentUserName: document.getElementById('current-user-name'),
        currentUserRole: document.getElementById('current-user-role'),
        globalTeamFilter: document.getElementById('global-team-filter'),
        pageTitle: document.getElementById('page-title'),

        navItems: document.querySelectorAll('.nav-item'),
        badgeNavAssigned: document.getElementById('badge-nav-assigned'),
        badgeNavUrgent: document.getElementById('badge-nav-urgent'),
        badgeNavApproval: document.getElementById('badge-nav-approval'),

        // Views
        viewDashboard: document.getElementById('view-dashboard'),
        viewItems: document.getElementById('view-items'),

        // KPIs
        kpiTotalOpen: document.getElementById('kpi-total-open'),
        kpiAssignedMe: document.getElementById('kpi-assigned-me'),
        kpiUnassignedUrgent: document.getElementById('kpi-unassigned-urgent'),
        kpiAwaitingApproval: document.getElementById('kpi-awaiting-approval'),
        dashboardRecentTbody: document.getElementById('dashboard-recent-tbody'),
        btnViewAllFromDash: document.getElementById('btn-view-all-from-dash'),

        // Table & Filters
        itemsTable: document.getElementById('items-table'),
        itemsTbody: document.getElementById('items-tbody'),
        filterSearch: document.getElementById('filter-search'),
        filterStatus: document.getElementById('filter-status'),
        filterPriority: document.getElementById('filter-priority'),
        filterType: document.getElementById('filter-type'),
        btnApplyFilters: document.getElementById('btn-apply-filters'),
        btnClearFilters: document.getElementById('btn-clear-filters'),

        // Pagination
        paginationInfo: document.getElementById('pagination-info'),
        pageCurrentIndicator: document.getElementById('page-current-indicator'),
        btnPrevPage: document.getElementById('btn-prev-page'),
        btnNextPage: document.getElementById('btn-next-page'),

        // Modals
        modalCreate: document.getElementById('modal-create'),
        btnOpenCreateModal: document.getElementById('btn-open-create-modal'),
        btnCloseCreateModal: document.getElementById('btn-close-create-modal'),
        btnCancelCreate: document.getElementById('btn-cancel-create'),
        formCreateItem: document.getElementById('form-create-item'),
        createItemError: document.getElementById('create-item-error'),
        createTeam: document.getElementById('create-team'),
        createAssignee: document.getElementById('create-assignee'),
        btnSubmitCreate: document.getElementById('btn-submit-create'),

        modalDetail: document.getElementById('modal-detail'),
        btnCloseDetailModal: document.getElementById('btn-close-detail-modal'),
        detailTypeBadge: document.getElementById('detail-type-badge'),
        detailPriorityBadge: document.getElementById('detail-priority-badge'),
        detailStatusBadge: document.getElementById('detail-status-badge'),
        detailVersion: document.getElementById('detail-version'),
        detailConflictAlert: document.getElementById('detail-conflict-alert'),
        detailConflictMsg: document.getElementById('detail-conflict-msg'),
        btnReloadLatest: document.getElementById('btn-reload-latest'),
        detailTitle: document.getElementById('detail-title'),
        detailDescription: document.getElementById('detail-description'),
        detailTeam: document.getElementById('detail-team'),
        detailAssignee: document.getElementById('detail-assignee'),
        detailCreatedBy: document.getElementById('detail-created-by'),
        detailDueDate: document.getElementById('detail-due-date'),
        detailCreatedAt: document.getElementById('detail-created-at'),
        detailUpdatedAt: document.getElementById('detail-updated-at'),

        actionClaimGroup: document.getElementById('action-claim-group'),
        btnDetailClaim: document.getElementById('btn-detail-claim'),
        actionAssignGroup: document.getElementById('action-assign-group'),
        selectAssignMember: document.getElementById('select-assign-member'),
        btnDetailAssign: document.getElementById('btn-detail-assign'),
        actionTransitionsGroup: document.getElementById('action-transitions-group'),
        transitionButtonsContainer: document.getElementById('transition-buttons-container'),
        btnDetailEditToggle: document.getElementById('btn-detail-edit-toggle'),

        formEditItem: document.getElementById('form-edit-item'),
        editTitle: document.getElementById('edit-title'),
        editPriority: document.getElementById('edit-priority'),
        editDueDate: document.getElementById('edit-due-date'),
        editDescription: document.getElementById('edit-description'),
        btnCancelEdit: document.getElementById('btn-cancel-edit'),
        btnSaveEdit: document.getElementById('btn-save-edit'),

        detailTimelineList: document.getElementById('detail-timeline-list'),
        detailCommentsList: document.getElementById('detail-comments-list'),
        commentsCount: document.getElementById('comments-count'),
        formAddComment: document.getElementById('form-add-comment'),
        commentInput: document.getElementById('comment-input'),
        toastContainer: document.getElementById('toast-container')
    };

    // =========================================================================
    // API Helper Functions
    // =========================================================================
    async function apiRequest(endpoint, options = {}) {
        const headers = options.headers || {};
        if (state.token) {
            headers['Authorization'] = `Bearer ${state.token}`;
        }
        if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
            headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(options.body);
        }
        options.headers = headers;

        try {
            const response = await fetch(endpoint, options);

            if (response.status === 401) {
                handleLogout();
                showToast('Your session has expired. Please log in again.', 'error');
                throw new Error('Unauthorized');
            }

            const data = await response.json().catch(() => null);

            if (!response.ok) {
                const error = new Error(data && data.message ? data.message : `HTTP Error ${response.status}`);
                error.status = response.status;
                error.data = data;
                throw error;
            }

            return data;
        } catch (err) {
            console.error('API Request Failed:', endpoint, err);
            throw err;
        }
    }

    // =========================================================================
    // UI Helpers: Notifications & Safe DOM
    // =========================================================================
    function showToast(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.textContent = message;

        el.toastContainer.appendChild(toast);
        setTimeout(() => {
            toast.remove();
        }, 4000);
    }

    function formatDate(isoString) {
        if (!isoString) return '—';
        const date = new Date(isoString);
        return date.toLocaleDateString(undefined, {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    function createBadge(type, value) {
        const span = document.createElement('span');
        span.className = `badge badge-${type}-${value}`;
        span.textContent = value ? value.replace('_', ' ') : '—';
        return span;
    }

    // =========================================================================
    // Authentication Flow
    // =========================================================================
    async function handleLogin(e) {
        e.preventDefault();
        el.loginError.classList.add('hidden');
        el.loginError.textContent = '';

        const username = el.loginUsername.value.trim();
        const password = el.loginPassword.value;

        if (!username || !password) {
            el.loginError.textContent = 'Please enter both username and password.';
            el.loginError.classList.remove('hidden');
            return;
        }

        setLoading(el.btnLoginSubmit, true);

        try {
            const data = await apiRequest('/api/auth/login', {
                method: 'POST',
                body: { username, password }
            });

            state.token = data.token;
            state.user = data.user;
            state.teams = data.teams;
            sessionStorage.setItem('ops_token', data.token);

            showApp();
            showToast(`Welcome back, ${data.user.displayName}!`, 'success');
        } catch (err) {
            el.loginError.textContent = err.message || 'Authentication failed. Please verify credentials.';
            el.loginError.classList.remove('hidden');
        } finally {
            setLoading(el.btnLoginSubmit, false);
        }
    }

    function handleLogout() {
        state.token = null;
        state.user = null;
        state.teams = [];
        sessionStorage.removeItem('ops_token');

        el.appView.classList.add('hidden');
        el.loginView.classList.remove('hidden');
        el.loginUsername.value = '';
        el.loginPassword.value = '';
        el.loginError.classList.add('hidden');
    }

    async function checkAuthSession() {
        if (!state.token) {
            handleLogout();
            return;
        }

        try {
            const data = await apiRequest('/api/auth/me');
            state.user = data.user;
            state.teams = data.teams;
            showApp();
        } catch (err) {
            handleLogout();
        }
    }

    function showApp() {
        el.loginView.classList.add('hidden');
        el.appView.classList.remove('hidden');

        // Render current user profile
        el.currentUserName.textContent = state.user.displayName;
        el.currentUserAvatar.textContent = state.user.displayName.charAt(0).toUpperCase();

        const roleSummaries = state.teams.map(t => `${t.name}: ${t.userRole}`).join(' | ');
        el.currentUserRole.textContent = state.teams.length > 0 ? `${state.teams[0].userRole}` : 'No teams';
        el.currentUserRole.title = roleSummaries;

        // Populate team selectors
        populateTeamSelectors();

        // Switch to default view
        switchView('dashboard');
        loadDashboardData();
    }

    function populateTeamSelectors() {
        // Global Team Filter
        el.globalTeamFilter.innerHTML = '<option value="">All My Teams</option>';
        state.teams.forEach(team => {
            const opt = document.createElement('option');
            opt.value = team.id;
            opt.textContent = `${team.name} (${team.userRole})`;
            el.globalTeamFilter.appendChild(opt);
        });

        // Create Modal Team Selector
        el.createTeam.innerHTML = '<option value="">Select Team</option>';
        // Only include teams where user has MEMBER or higher
        state.teams
            .filter(t => t.userRole !== 'VIEWER')
            .forEach(team => {
                const opt = document.createElement('option');
                opt.value = team.id;
                opt.textContent = `${team.name} (${team.userRole})`;
                el.createTeam.appendChild(opt);
            });
    }

    // =========================================================================
    // View Navigation & Switching
    // =========================================================================
    function switchView(viewName, preset = 'all') {
        state.currentView = viewName;
        state.currentPreset = preset;

        el.navItems.forEach(item => {
            const matchView = item.dataset.view === viewName;
            const matchPreset = !item.dataset.preset || item.dataset.preset === preset;
            item.classList.toggle('active', matchView && matchPreset);
        });

        if (viewName === 'dashboard') {
            el.pageTitle.textContent = 'Dashboard';
            el.viewDashboard.classList.remove('hidden');
            el.viewItems.classList.add('hidden');
            loadDashboardData();
        } else if (viewName === 'items') {
            el.viewDashboard.classList.add('hidden');
            el.viewItems.classList.remove('hidden');

            if (preset === 'assigned-to-me') {
                el.pageTitle.textContent = 'Work Items — Assigned to Me';
            } else if (preset === 'unassigned-urgent') {
                el.pageTitle.textContent = 'Work Items — Unassigned Urgent';
            } else if (preset === 'awaiting-approval') {
                el.pageTitle.textContent = 'Work Items — Awaiting Approval';
            } else {
                el.pageTitle.textContent = 'All Work Items';
            }

            state.currentPage = 0;
            loadWorkItems();
        }
    }

    // =========================================================================
    // Dashboard Data
    // =========================================================================
    async function loadDashboardData() {
        try {
            const summary = await apiRequest('/api/dashboard/summary');

            el.kpiTotalOpen.textContent = summary.totalOpenItems;
            el.kpiAssignedMe.textContent = summary.assignedToMe;
            el.kpiUnassignedUrgent.textContent = summary.unassignedUrgent;
            el.kpiAwaitingApproval.textContent = summary.awaitingApproval;

            // Update navigation badges
            updateNavBadge(el.badgeNavAssigned, summary.assignedToMe);
            updateNavBadge(el.badgeNavUrgent, summary.unassignedUrgent);
            updateNavBadge(el.badgeNavApproval, summary.awaitingApproval);

            // Render Recent Items
            renderRecentTable(summary.recentItems);
        } catch (err) {
            console.error('Failed to load dashboard summary:', err);
        }
    }

    function updateNavBadge(badgeEl, count) {
        if (count > 0) {
            badgeEl.textContent = count;
            badgeEl.classList.remove('hidden');
        } else {
            badgeEl.classList.add('hidden');
        }
    }

    function renderRecentTable(items) {
        el.dashboardRecentTbody.innerHTML = '';

        if (!items || items.length === 0) {
            el.dashboardRecentTbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">No recently updated items.</td></tr>';
            return;
        }

        items.forEach(item => {
            const tr = document.createElement('tr');
            tr.addEventListener('click', () => openDetailModal(item.id));

            const tdTitle = document.createElement('td');
            tdTitle.className = 'table-title-cell';
            tdTitle.textContent = item.title;

            const tdType = document.createElement('td');
            tdType.appendChild(createBadge('type', item.type));

            const tdPriority = document.createElement('td');
            tdPriority.appendChild(createBadge('priority', item.priority));

            const tdStatus = document.createElement('td');
            tdStatus.appendChild(createBadge('status', item.status));

            const tdTeam = document.createElement('td');
            tdTeam.textContent = item.team ? item.team.name : '—';

            const tdAssignee = document.createElement('td');
            tdAssignee.textContent = item.assignee ? item.assignee.displayName : 'Unassigned';
            if (!item.assignee) tdAssignee.classList.add('text-muted');

            tr.append(tdTitle, tdType, tdPriority, tdStatus, tdTeam, tdAssignee);
            el.dashboardRecentTbody.appendChild(tr);
        });
    }

    // =========================================================================
    // Work Items Table & Server-Side Filtering / Pagination
    // =========================================================================
    async function loadWorkItems() {
        el.itemsTbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted">Loading work items...</td></tr>';

        let url = '';
        const params = new URLSearchParams({
            page: state.currentPage,
            size: state.pageSize,
            sort: 'createdAt,desc'
        });

        if (state.selectedTeamId) {
            params.append('teamId', state.selectedTeamId);
        }

        if (state.currentPreset === 'assigned-to-me') {
            url = `/api/dashboard/assigned-to-me?${params.toString()}`;
        } else if (state.currentPreset === 'unassigned-urgent') {
            url = `/api/dashboard/unassigned-urgent?${params.toString()}`;
        } else if (state.currentPreset === 'awaiting-approval') {
            url = `/api/dashboard/awaiting-approval?${params.toString()}`;
        } else {
            // Standard query with custom filters
            if (state.filters.search) params.append('search', state.filters.search);
            if (state.filters.status) params.append('status', state.filters.status);
            if (state.filters.priority) params.append('priority', state.filters.priority);
            if (state.filters.type) params.append('type', state.filters.type);
            url = `/api/items?${params.toString()}`;
        }

        try {
            const pageData = await apiRequest(url);

            state.totalPages = pageData.totalPages || 1;
            state.totalElements = pageData.totalElements || 0;

            renderItemsTable(pageData.content);
            updatePaginationControls();
        } catch (err) {
            el.itemsTbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger">Failed to load work items: ${err.message}</td></tr>`;
        }
    }

    function renderItemsTable(items) {
        el.itemsTbody.innerHTML = '';

        if (!items || items.length === 0) {
            el.itemsTbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted">No work items matching the criteria.</td></tr>';
            return;
        }

        items.forEach(item => {
            const tr = document.createElement('tr');
            tr.addEventListener('click', () => openDetailModal(item.id));

            const tdTitle = document.createElement('td');
            tdTitle.className = 'table-title-cell';
            tdTitle.textContent = item.title;

            const tdType = document.createElement('td');
            tdType.appendChild(createBadge('type', item.type));

            const tdPriority = document.createElement('td');
            tdPriority.appendChild(createBadge('priority', item.priority));

            const tdStatus = document.createElement('td');
            tdStatus.appendChild(createBadge('status', item.status));

            const tdTeam = document.createElement('td');
            tdTeam.textContent = item.team ? item.team.name : '—';

            const tdAssignee = document.createElement('td');
            tdAssignee.textContent = item.assignee ? item.assignee.displayName : 'Unassigned';
            if (!item.assignee) tdAssignee.classList.add('text-muted');

            const tdDueDate = document.createElement('td');
            tdDueDate.textContent = formatDate(item.dueDate);

            const tdUpdated = document.createElement('td');
            tdUpdated.textContent = formatDate(item.updatedAt);

            tr.append(tdTitle, tdType, tdPriority, tdStatus, tdTeam, tdAssignee, tdDueDate, tdUpdated);
            el.itemsTbody.appendChild(tr);
        });
    }

    function updatePaginationControls() {
        const start = state.totalElements === 0 ? 0 : state.currentPage * state.pageSize + 1;
        const end = Math.min((state.currentPage + 1) * state.pageSize, state.totalElements);

        el.paginationInfo.textContent = `Showing ${start}-${end} of ${state.totalElements} items`;
        el.pageCurrentIndicator.textContent = `Page ${state.currentPage + 1} of ${Math.max(1, state.totalPages)}`;

        el.btnPrevPage.disabled = state.currentPage === 0;
        el.btnNextPage.disabled = state.currentPage >= state.totalPages - 1;
    }

    // =========================================================================
    // Create Work Item (Screen D)
    // =========================================================================
    async function handleTeamChangeForAssignees(teamId) {
        el.createAssignee.innerHTML = '<option value="">Unassigned</option>';
        if (!teamId) return;

        try {
            const members = await getTeamMembers(teamId);
            members.forEach(m => {
                const opt = document.createElement('option');
                opt.value = m.userId;
                opt.textContent = `${m.displayName} (${m.role})`;
                el.createAssignee.appendChild(opt);
            });
        } catch (err) {
            console.error('Failed to load team members:', err);
        }
    }

    async function getTeamMembers(teamId) {
        if (state.teamMembersCache[teamId]) {
            return state.teamMembersCache[teamId];
        }
        const members = await apiRequest(`/api/teams/${teamId}/members`);
        state.teamMembersCache[teamId] = members;
        return members;
    }

    async function handleCreateItemSubmit(e) {
        e.preventDefault();
        el.createItemError.classList.add('hidden');
        el.createItemError.textContent = '';

        const title = document.getElementById('create-title').value.trim();
        const teamId = el.createTeam.value;
        const type = document.getElementById('create-type').value;
        const priority = document.getElementById('create-priority').value;
        const assigneeId = el.createAssignee.value || null;
        const dueDateVal = document.getElementById('create-due-date').value;
        const description = document.getElementById('create-description').value.trim();

        if (!title || !teamId || !type || !priority) {
            el.createItemError.textContent = 'Please complete all required fields.';
            el.createItemError.classList.remove('hidden');
            return;
        }

        const dueDate = dueDateVal ? new Date(dueDateVal).toISOString() : null;

        const requestPayload = {
            title,
            teamId,
            type,
            priority,
            assigneeId,
            dueDate,
            description
        };

        // Client-side generated Idempotency Key
        const idempotencyKey = crypto.randomUUID ? crypto.randomUUID() : `key-${Date.now()}-${Math.random()}`;

        setLoading(el.btnSubmitCreate, true);

        try {
            await apiRequest('/api/items', {
                method: 'POST',
                headers: {
                    'Idempotency-Key': idempotencyKey
                },
                body: requestPayload
            });

            showToast('Work item created successfully!', 'success');
            closeCreateModal();

            // Refresh table and dashboard
            if (state.currentView === 'items') {
                loadWorkItems();
            } else {
                loadDashboardData();
            }
        } catch (err) {
            el.createItemError.textContent = err.message || 'Failed to create work item.';
            el.createItemError.classList.remove('hidden');
        } finally {
            setLoading(el.btnSubmitCreate, false);
        }
    }

    function openCreateModal() {
        el.formCreateItem.reset();
        el.createItemError.classList.add('hidden');
        el.createAssignee.innerHTML = '<option value="">Unassigned</option>';
        el.modalCreate.classList.remove('hidden');
    }

    function closeCreateModal() {
        el.modalCreate.classList.add('hidden');
    }

    // =========================================================================
    // Work Item Detail & Actions (Screen E)
    // =========================================================================
    async function openDetailModal(itemId) {
        el.modalDetail.classList.remove('hidden');
        el.detailConflictAlert.classList.add('hidden');
        el.formEditItem.classList.add('hidden');

        await loadItemDetail(itemId);
    }

    async function loadItemDetail(itemId) {
        try {
            const item = await apiRequest(`/api/items/${itemId}`);
            state.activeItem = item;

            // Header & badges
            el.detailTitle.textContent = item.title;
            el.detailDescription.textContent = item.description || 'No description provided.';
            el.detailVersion.textContent = item.version;

            el.detailTypeBadge.className = 'badge badge-type';
            el.detailTypeBadge.textContent = item.type;

            el.detailPriorityBadge.className = `badge badge-priority-${item.priority}`;
            el.detailPriorityBadge.textContent = item.priority;

            el.detailStatusBadge.className = `badge badge-status-${item.status}`;
            el.detailStatusBadge.textContent = item.status.replace('_', ' ');

            // Attributes
            el.detailTeam.textContent = item.team ? item.team.name : '—';
            el.detailAssignee.textContent = item.assignee ? item.assignee.displayName : 'Unassigned';
            el.detailCreatedBy.textContent = item.createdBy ? item.createdBy.displayName : '—';
            el.detailDueDate.textContent = formatDate(item.dueDate);
            el.detailCreatedAt.textContent = formatDate(item.createdAt);
            el.detailUpdatedAt.textContent = formatDate(item.updatedAt);

            // Populate Edit Form values
            el.editTitle.value = item.title;
            el.editPriority.value = item.priority;
            el.editDescription.value = item.description || '';
            el.editDueDate.value = item.dueDate ? item.dueDate.substring(0, 16) : '';

            // Operational Action Buttons
            renderDetailActions(item);

            // Load Timeline & Comments
            loadTimeline(item.id);
            loadComments(item.id);
        } catch (err) {
            showToast(`Failed to load item: ${err.message}`, 'error');
            closeDetailModal();
        }
    }

    function renderDetailActions(item) {
        // 1. Claim Action
        if (item.canClaim) {
            el.actionClaimGroup.classList.remove('hidden');
        } else {
            el.actionClaimGroup.classList.add('hidden');
        }

        // 2. Assign Action (Lead only)
        if (item.canAssign) {
            el.actionAssignGroup.classList.remove('hidden');
            populateAssigneeDropdown(item.team.id, item.assignee ? item.assignee.id : null);
        } else {
            el.actionAssignGroup.classList.add('hidden');
        }

        // 3. Workflow Transitions
        el.transitionButtonsContainer.innerHTML = '';
        if (item.availableTransitions && item.availableTransitions.length > 0) {
            el.actionTransitionsGroup.classList.remove('hidden');
            item.availableTransitions.forEach(targetStatus => {
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'btn btn-secondary btn-sm';
                btn.textContent = `Move to ${targetStatus.replace('_', ' ')}`;
                btn.addEventListener('click', () => handleWorkflowTransition(targetStatus));
                el.transitionButtonsContainer.appendChild(btn);
            });
        } else {
            el.actionTransitionsGroup.classList.add('hidden');
        }

        // 4. Edit details toggle
        if (item.canEdit) {
            el.btnDetailEditToggle.classList.remove('hidden');
        } else {
            el.btnDetailEditToggle.classList.add('hidden');
        }
    }

    async function populateAssigneeDropdown(teamId, currentAssigneeId) {
        el.selectAssignMember.innerHTML = '<option value="">Unassigned</option>';
        try {
            const members = await getTeamMembers(teamId);
            members.forEach(m => {
                const opt = document.createElement('option');
                opt.value = m.userId;
                opt.textContent = m.displayName;
                if (currentAssigneeId && m.userId === currentAssigneeId) {
                    opt.selected = true;
                }
                el.selectAssignMember.appendChild(opt);
            });
        } catch (err) {
            console.error('Failed to populate assignees:', err);
        }
    }

    async function handleClaimItem() {
        if (!state.activeItem) return;

        try {
            const updated = await apiRequest(`/api/items/${state.activeItem.id}/claim`, {
                method: 'POST'
            });
            showToast('Work item successfully claimed!', 'success');
            loadItemDetail(updated.id);
            refreshActiveList();
        } catch (err) {
            handleActionConflict(err);
        }
    }

    async function handleAssignItem() {
        if (!state.activeItem) return;
        const assigneeId = el.selectAssignMember.value || null;

        try {
            const updated = await apiRequest(`/api/items/${state.activeItem.id}/assign`, {
                method: 'POST',
                body: {
                    assigneeId: assigneeId,
                    version: state.activeItem.version
                }
            });
            showToast('Work item assignment updated!', 'success');
            loadItemDetail(updated.id);
            refreshActiveList();
        } catch (err) {
            handleActionConflict(err);
        }
    }

    async function handleWorkflowTransition(targetStatus) {
        if (!state.activeItem) return;

        try {
            const updated = await apiRequest(`/api/items/${state.activeItem.id}/transition`, {
                method: 'POST',
                body: {
                    targetStatus: targetStatus,
                    version: state.activeItem.version
                }
            });
            showToast(`Status updated to ${targetStatus.replace('_', ' ')}!`, 'success');
            loadItemDetail(updated.id);
            refreshActiveList();
        } catch (err) {
            handleActionConflict(err);
        }
    }

    async function handleEditItemSubmit(e) {
        e.preventDefault();
        if (!state.activeItem) return;

        const title = el.editTitle.value.trim();
        const priority = el.editPriority.value;
        const description = el.editDescription.value.trim();
        const dueDateVal = el.editDueDate.value;
        const dueDate = dueDateVal ? new Date(dueDateVal).toISOString() : null;

        try {
            const updated = await apiRequest(`/api/items/${state.activeItem.id}`, {
                method: 'PATCH',
                body: {
                    title,
                    priority,
                    description,
                    dueDate,
                    version: state.activeItem.version
                }
            });
            showToast('Item details saved successfully!', 'success');
            el.formEditItem.classList.add('hidden');
            loadItemDetail(updated.id);
            refreshActiveList();
        } catch (err) {
            handleActionConflict(err);
        }
    }

    function handleActionConflict(err) {
        if (err.status === 409) {
            el.detailConflictMsg.textContent = err.message || 'This item has changed since you opened it. Reload the latest version before editing.';
            el.detailConflictAlert.classList.remove('hidden');
            showToast('Conflict: Stale version detected.', 'error');
        } else {
            showToast(err.message || 'Action failed', 'error');
        }
    }

    function closeDetailModal() {
        el.modalDetail.classList.add('hidden');
        state.activeItem = null;
    }

    function refreshActiveList() {
        if (state.currentView === 'items') {
            loadWorkItems();
        } else {
            loadDashboardData();
        }
    }

    // =========================================================================
    // Activity Timeline & Comments
    // =========================================================================
    async function loadTimeline(itemId) {
        el.detailTimelineList.innerHTML = '<div class="text-muted">Loading timeline...</div>';
        try {
            const pageData = await apiRequest(`/api/items/${itemId}/activity?size=50`);
            renderTimeline(pageData.content);
        } catch (err) {
            el.detailTimelineList.innerHTML = '<div class="text-danger">Failed to load activity history.</div>';
        }
    }

    function renderTimeline(events) {
        el.detailTimelineList.innerHTML = '';
        if (!events || events.length === 0) {
            el.detailTimelineList.innerHTML = '<div class="text-muted">No activity events recorded yet.</div>';
            return;
        }

        events.forEach(ev => {
            const item = document.createElement('div');
            item.className = 'timeline-item';

            const dot = document.createElement('div');
            dot.className = 'timeline-dot';

            const content = document.createElement('div');
            content.className = 'timeline-content';

            const meta = document.createElement('div');
            meta.className = 'timeline-meta';

            const actor = document.createElement('span');
            actor.className = 'timeline-actor';
            actor.textContent = ev.actorName;

            const time = document.createTextNode(` • ${formatDate(ev.createdAt)}`);
            meta.append(actor, time);

            const msg = document.createElement('div');
            msg.className = 'timeline-msg';
            msg.textContent = ev.message || `${ev.eventType}: ${ev.oldValue || '—'} → ${ev.newValue || '—'}`;

            content.append(meta, msg);
            item.append(dot, content);
            el.detailTimelineList.appendChild(item);
        });
    }

    async function loadComments(itemId) {
        el.detailCommentsList.innerHTML = '<div class="text-muted">Loading comments...</div>';
        try {
            const comments = await apiRequest(`/api/items/${itemId}/comments`);
            renderComments(comments);
        } catch (err) {
            el.detailCommentsList.innerHTML = '<div class="text-danger">Failed to load comments.</div>';
        }
    }

    function renderComments(comments) {
        el.detailCommentsList.innerHTML = '';
        el.commentsCount.textContent = comments ? comments.length : 0;

        if (!comments || comments.length === 0) {
            el.detailCommentsList.innerHTML = '<div class="text-muted">No comments yet. Be the first to start the discussion!</div>';
            return;
        }

        comments.forEach(c => {
            const box = document.createElement('div');
            box.className = 'comment-item';

            const meta = document.createElement('div');
            meta.className = 'comment-meta';

            const author = document.createElement('span');
            author.className = 'comment-author';
            author.textContent = c.authorName;

            const time = document.createElement('span');
            time.textContent = formatDate(c.createdAt);

            meta.append(author, time);

            const content = document.createElement('div');
            content.className = 'comment-content';
            content.textContent = c.content;

            box.append(meta, content);
            el.detailCommentsList.appendChild(box);
        });
    }

    async function handleAddCommentSubmit(e) {
        e.preventDefault();
        if (!state.activeItem) return;

        const content = el.commentInput.value.trim();
        if (!content) return;

        try {
            await apiRequest(`/api/items/${state.activeItem.id}/comments`, {
                method: 'POST',
                body: { content }
            });

            el.commentInput.value = '';
            showToast('Comment posted.', 'success');
            loadComments(state.activeItem.id);
            loadTimeline(state.activeItem.id);
        } catch (err) {
            showToast(err.message || 'Failed to post comment', 'error');
        }
    }

    // Helper: Button Loading state
    function setLoading(btn, isLoading) {
        if (!btn) return;
        btn.disabled = isLoading;
        const textSpan = btn.querySelector('.btn-text');
        const spinSpan = btn.querySelector('.btn-spinner');
        if (textSpan) textSpan.classList.toggle('hidden', isLoading);
        if (spinSpan) spinSpan.classList.toggle('hidden', !isLoading);
    }

    // =========================================================================
    // Event Listeners Initialization
    // =========================================================================
    function initEventListeners() {
        // Login & Auth
        el.loginForm.addEventListener('submit', handleLogin);
        el.btnLogout.addEventListener('click', handleLogout);

        el.btnTogglePassword.addEventListener('click', () => {
            const isPassword = el.loginPassword.type === 'password';
            el.loginPassword.type = isPassword ? 'text' : 'password';
            el.btnTogglePassword.textContent = isPassword ? 'Hide' : 'Show';
        });

        // Quickfill Demo Credentials
        document.querySelectorAll('.btn-cred-quickfill').forEach(btn => {
            btn.addEventListener('click', () => {
                const username = btn.dataset.user;
                el.loginUsername.value = username;
                el.loginPassword.value = 'Password123!';
                el.loginError.classList.add('hidden');
            });
        });

        // Global Team Filter
        el.globalTeamFilter.addEventListener('change', (e) => {
            state.selectedTeamId = e.target.value;
            if (state.currentView === 'items') {
                state.currentPage = 0;
                loadWorkItems();
            } else {
                loadDashboardData();
            }
        });

        // Sidebar Navigation
        el.navItems.forEach(btn => {
            btn.addEventListener('click', () => {
                const view = btn.dataset.view;
                const preset = btn.dataset.preset || 'all';
                switchView(view, preset);
            });
        });

        el.btnViewAllFromDash.addEventListener('click', () => {
            switchView('items', 'all');
        });

        // KPI Card clicks to quickly filter
        document.getElementById('card-assigned-me').addEventListener('click', () => {
            switchView('items', 'assigned-to-me');
        });
        document.getElementById('card-unassigned-urgent').addEventListener('click', () => {
            switchView('items', 'unassigned-urgent');
        });
        document.getElementById('card-awaiting-approval').addEventListener('click', () => {
            switchView('items', 'awaiting-approval');
        });
        document.getElementById('card-total-open').addEventListener('click', () => {
            switchView('items', 'all');
        });

        // Filters Toolbar
        el.btnApplyFilters.addEventListener('click', () => {
            state.filters.search = el.filterSearch.value.trim();
            state.filters.status = el.filterStatus.value;
            state.filters.priority = el.filterPriority.value;
            state.filters.type = el.filterType.value;
            state.currentPage = 0;
            loadWorkItems();
        });

        el.btnClearFilters.addEventListener('click', () => {
            el.filterSearch.value = '';
            el.filterStatus.value = '';
            el.filterPriority.value = '';
            el.filterType.value = '';
            state.filters = { search: '', status: '', priority: '', type: '' };
            state.currentPage = 0;
            loadWorkItems();
        });

        // Pagination buttons
        el.btnPrevPage.addEventListener('click', () => {
            if (state.currentPage > 0) {
                state.currentPage--;
                loadWorkItems();
            }
        });

        el.btnNextPage.addEventListener('click', () => {
            if (state.currentPage < state.totalPages - 1) {
                state.currentPage++;
                loadWorkItems();
            }
        });

        // Create Modal
        el.btnOpenCreateModal.addEventListener('click', openCreateModal);
        el.btnCloseCreateModal.addEventListener('click', closeCreateModal);
        el.btnCancelCreate.addEventListener('click', closeCreateModal);
        el.formCreateItem.addEventListener('submit', handleCreateItemSubmit);
        el.createTeam.addEventListener('change', (e) => handleTeamChangeForAssignees(e.target.value));

        // Detail Modal
        el.btnCloseDetailModal.addEventListener('click', closeDetailModal);
        el.btnReloadLatest.addEventListener('click', () => {
            if (state.activeItem) {
                el.detailConflictAlert.classList.add('hidden');
                loadItemDetail(state.activeItem.id);
            }
        });

        el.btnDetailClaim.addEventListener('click', handleClaimItem);
        el.btnDetailAssign.addEventListener('click', handleAssignItem);
        el.btnDetailEditToggle.addEventListener('click', () => {
            el.formEditItem.classList.toggle('hidden');
        });
        el.btnCancelEdit.addEventListener('click', () => {
            el.formEditItem.classList.add('hidden');
        });
        el.formEditItem.addEventListener('submit', handleEditItemSubmit);

        // Detail Tabs
        document.querySelectorAll('.tab-button').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('.tab-button').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');

                const tab = btn.dataset.tab;
                document.getElementById('tab-panel-timeline').classList.toggle('hidden', tab !== 'timeline');
                document.getElementById('tab-panel-comments').classList.toggle('hidden', tab !== 'comments');
            });
        });

        // Comments
        el.formAddComment.addEventListener('submit', handleAddCommentSubmit);
    }

    // Initialize application on page load
    document.addEventListener('DOMContentLoaded', () => {
        initEventListeners();
        checkAuthSession();
    });

})();
