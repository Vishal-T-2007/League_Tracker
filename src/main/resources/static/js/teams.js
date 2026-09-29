// Teams Page JavaScript

document.addEventListener('DOMContentLoaded', () => {
    loadTeams();

    const addTeamForm = document.getElementById('add-team-form');
    if (addTeamForm) {
        addTeamForm.addEventListener('submit', handleAddTeam);
    }

    const editTeamForm = document.getElementById('edit-team-form');
    if (editTeamForm) {
        editTeamForm.addEventListener('submit', handleUpdateTeam);
    }
});

async function loadTeams() {
    const tableContainer = document.getElementById('teams-table-container');
    const emptyState = document.getElementById('teams-empty-state');
    const tableBody = document.getElementById('teams-table-body');
    const teamCountBadge = document.getElementById('team-count-badge');

    try {
        const response = await fetch('/api/teams');
        if (!response.ok) throw new Error('Failed to fetch teams');
        const teams = await response.json();

        if (teamCountBadge) {
            teamCountBadge.textContent = `${teams.length} ${teams.length === 1 ? 'Team' : 'Teams'}`;
        }

        if (!teams || teams.length === 0) {
            tableContainer.style.display = 'none';
            emptyState.style.display = 'block';
            return;
        }

        emptyState.style.display = 'none';
        tableContainer.style.display = 'block';

        tableBody.innerHTML = teams.map((team, idx) => {
            const dateStr = team.createdAt ? new Date(team.createdAt).toLocaleDateString(undefined, {
                year: 'numeric', month: 'short', day: 'numeric'
            }) : '-';

            return `
                <tr>
                    <td style="color: #888;">${idx + 1}</td>
                    <td style="font-weight: bold;">${escapeHtml(team.name)}</td>
                    <td style="color: #555;">${dateStr}</td>
                    <td>
                        <div style="display: flex; gap: 6px;">
                            <button class="btn btn-secondary btn-sm" onclick="openEditModal(${team.id}, '${escapeJsString(team.name)}')">
                                Edit
                            </button>
                            <button class="btn btn-danger btn-sm" onclick="confirmDeleteTeam(${team.id}, '${escapeJsString(team.name)}')">
                                Delete
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        console.error('Error loading teams:', error);
        showToast(error.message || 'Error loading teams', 'error');
    }
}

async function handleAddTeam(e) {
    e.preventDefault();
    const input = document.getElementById('team-name-input');
    const name = input.value.trim();

    if (!name) {
        showToast('Team name cannot be blank.', 'error');
        return;
    }

    try {
        const response = await fetch('/api/teams', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || 'Failed to register team');
        }

        showToast(`Team "${name}" registered successfully!`, 'success');
        input.value = '';
        loadTeams();
    } catch (error) {
        console.error('Error registering team:', error);
        showToast(error.message, 'error');
    }
}

function openEditModal(id, currentName) {
    document.getElementById('edit-team-id').value = id;
    document.getElementById('edit-team-name').value = currentName;
    const modal = document.getElementById('edit-modal');
    modal.classList.add('active');
}

function closeEditModal() {
    const modal = document.getElementById('edit-modal');
    modal.classList.remove('active');
}

async function handleUpdateTeam(e) {
    e.preventDefault();
    const id = document.getElementById('edit-team-id').value;
    const name = document.getElementById('edit-team-name').value.trim();

    if (!name) {
        showToast('Team name cannot be blank.', 'error');
        return;
    }

    try {
        const response = await fetch(`/api/teams/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || 'Failed to update team');
        }

        showToast(`Team renamed to "${name}" successfully!`, 'success');
        closeEditModal();
        loadTeams();
    } catch (error) {
        console.error('Error updating team:', error);
        showToast(error.message, 'error');
    }
}

async function confirmDeleteTeam(id, name) {
    const confirmed = window.confirm(
        `Deleting this team will also remove its fixtures and match results. Continue?`
    );

    if (!confirmed) return;

    try {
        const response = await fetch(`/api/teams/${id}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            const data = await response.json().catch(() => ({}));
            throw new Error(data.message || 'Failed to delete team');
        }

        showToast(`Team "${name}" deleted and tournament records safely updated.`, 'success');
        loadTeams();
    } catch (error) {
        console.error('Error deleting team:', error);
        showToast(error.message, 'error');
    }
}

function showToast(message, type = 'success') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <span>${escapeHtml(message)}</span>
        <button style="background:transparent; border:none; color:inherit; cursor:pointer; margin-left:10px;" onclick="this.parentElement.remove()">✕</button>
    `;

    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;")
              .replace(/</g, "&lt;")
              .replace(/>/g, "&gt;")
              .replace(/"/g, "&quot;")
              .replace(/'/g, "&#039;");
}

function escapeJsString(str) {
    if (!str) return '';
    return str.replace(/'/g, "\\'").replace(/"/g, '&quot;');
}
