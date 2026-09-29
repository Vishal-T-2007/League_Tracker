// Fixtures Page JavaScript

document.addEventListener('DOMContentLoaded', () => {
    loadFixtures();

    const generateBtn = document.getElementById('generate-fixtures-btn');
    if (generateBtn) {
        generateBtn.addEventListener('click', handleGenerateFixtures);
    }

    const resetBtn = document.getElementById('reset-fixtures-btn');
    if (resetBtn) {
        resetBtn.addEventListener('click', handleResetFixtures);
    }

    const matchForm = document.getElementById('match-result-form');
    if (matchForm) {
        matchForm.addEventListener('submit', handleSaveMatchResult);
    }
});

let isEditMode = false;

async function loadFixtures() {
    const tableContainer = document.getElementById('fixtures-table-container');
    const emptyState = document.getElementById('fixtures-empty-state');
    const tableBody = document.getElementById('fixtures-table-body');
    const fixtureCountBadge = document.getElementById('fixture-count-badge');
    const resetBtn = document.getElementById('reset-fixtures-btn');

    try {
        const response = await fetch('/api/fixtures');
        if (!response.ok) throw new Error('Failed to fetch fixtures');
        const fixtures = await response.json();

        if (fixtureCountBadge) {
            fixtureCountBadge.textContent = `${fixtures.length} ${fixtures.length === 1 ? 'Fixture' : 'Fixtures'}`;
        }

        if (!fixtures || fixtures.length === 0) {
            tableContainer.style.display = 'none';
            emptyState.style.display = 'block';
            if (resetBtn) resetBtn.style.display = 'none';
            return;
        }

        emptyState.style.display = 'none';
        tableContainer.style.display = 'block';
        if (resetBtn) resetBtn.style.display = 'inline-flex';

        tableBody.innerHTML = fixtures.map(f => {
            const isCompleted = f.status === 'COMPLETED';
            const statusBadge = isCompleted
                ? `<span class="badge badge-completed">Completed</span>`
                : `<span class="badge badge-scheduled">Scheduled</span>`;

            let resultDisplay = '<span style="color: #aaa;">-</span>';
            if (isCompleted && f.homeScore !== null && f.awayScore !== null) {
                resultDisplay = `<span style="font-weight: bold; color: #1565c0;">${f.homeScore} - ${f.awayScore}</span>`;
            }

            const dateStr = f.scheduledDate ? new Date(f.scheduledDate).toLocaleDateString(undefined, {
                month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
            }) : '-';

            const actionBtn = isCompleted
                ? `<button class="btn btn-secondary btn-sm" onclick="openMatchModal(${f.id}, '${escapeJsString(f.homeTeamName)}', '${escapeJsString(f.awayTeamName)}', ${f.homeScore}, ${f.awayScore}, true)">
                        Edit Result
                   </button>`
                : `<button class="btn btn-primary btn-sm" onclick="openMatchModal(${f.id}, '${escapeJsString(f.homeTeamName)}', '${escapeJsString(f.awayTeamName)}', '', '', false)">
                        Enter Result
                   </button>`;

            return `
                <tr>
                    <td>Round ${f.roundNumber}</td>
                    <td style="font-weight: bold;">${escapeHtml(f.homeTeamName)}</td>
                    <td style="font-weight: bold;">${escapeHtml(f.awayTeamName)}</td>
                    <td style="color: #555; font-size: 13px;">${dateStr}</td>
                    <td>${statusBadge}</td>
                    <td style="text-align: center;">${resultDisplay}</td>
                    <td>${actionBtn}</td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        console.error('Error loading fixtures:', error);
        showToast(error.message || 'Error loading fixtures', 'error');
    }
}

async function handleGenerateFixtures() {
    try {
        const response = await fetch('/api/fixtures/generate', {
            method: 'POST'
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || 'Failed to generate fixtures');
        }

        showToast(`Successfully generated ${data.length} round-robin tournament fixtures!`, 'success');
        loadFixtures();
    } catch (error) {
        console.error('Error generating fixtures:', error);
        showToast(error.message, 'error');
    }
}

async function handleResetFixtures() {
    const confirmed = window.confirm('Are you sure you want to reset all fixtures and match results? This will also clear match contributions from the standings.');
    if (!confirmed) return;

    try {
        const response = await fetch('/api/fixtures', {
            method: 'DELETE'
        });

        if (!response.ok) {
            const data = await response.json().catch(() => ({}));
            throw new Error(data.message || 'Failed to reset fixtures');
        }

        showToast('All tournament fixtures and matches have been reset.', 'success');
        loadFixtures();
    } catch (error) {
        console.error('Error resetting fixtures:', error);
        showToast(error.message, 'error');
    }
}

function openMatchModal(fixtureId, homeTeam, awayTeam, homeScore, awayScore, editMode) {
    isEditMode = editMode;
    document.getElementById('modal-fixture-id').value = fixtureId;
    document.getElementById('modal-home-team-name').textContent = homeTeam;
    document.getElementById('modal-away-team-name').textContent = awayTeam;
    
    document.getElementById('modal-home-score').value = (homeScore !== null && homeScore !== undefined) ? homeScore : '';
    document.getElementById('modal-away-score').value = (awayScore !== null && awayScore !== undefined) ? awayScore : '';

    const titleEl = document.getElementById('match-modal-title');
    titleEl.textContent = editMode ? 'Edit Match Result' : 'Record Match Result';

    const modal = document.getElementById('match-modal');
    modal.classList.add('active');
    document.getElementById('modal-home-score').focus();
}

function closeMatchModal() {
    const modal = document.getElementById('match-modal');
    modal.classList.remove('active');
}

async function handleSaveMatchResult(e) {
    e.preventDefault();
    const fixtureId = document.getElementById('modal-fixture-id').value;
    const homeScoreVal = document.getElementById('modal-home-score').value;
    const awayScoreVal = document.getElementById('modal-away-score').value;

    if (homeScoreVal === '' || awayScoreVal === '') {
        showToast('Please provide both home and away scores.', 'error');
        return;
    }

    const homeScore = parseInt(homeScoreVal, 10);
    const awayScore = parseInt(awayScoreVal, 10);

    if (isNaN(homeScore) || isNaN(awayScore) || homeScore < 0 || awayScore < 0) {
        showToast('Scores must be non-negative integers.', 'error');
        return;
    }

    const url = `/api/matches/${fixtureId}/result`;
    const method = isEditMode ? 'PUT' : 'POST';

    try {
        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ homeScore, awayScore })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || 'Failed to save match result');
        }

        showToast(isEditMode ? 'Match result updated successfully!' : 'Match result recorded successfully!', 'success');
        closeMatchModal();
        loadFixtures();
    } catch (error) {
        console.error('Error saving match result:', error);
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
