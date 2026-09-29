// Standings Page JavaScript

document.addEventListener('DOMContentLoaded', () => {
    loadStandings();
});

async function loadStandings() {
    const tableContainer = document.getElementById('standings-table-container');
    const emptyState = document.getElementById('standings-empty-state');
    const tableBody = document.getElementById('standings-table-body');
    const standingsCountBadge = document.getElementById('standings-count-badge');

    try {
        const response = await fetch('/api/standings');
        if (!response.ok) throw new Error('Failed to fetch tournament standings');
        const standings = await response.json();

        if (standingsCountBadge) {
            standingsCountBadge.textContent = `${standings.length} ${standings.length === 1 ? 'Team' : 'Teams'}`;
        }

        if (!standings || standings.length === 0) {
            tableContainer.style.display = 'none';
            emptyState.style.display = 'block';
            return;
        }

        emptyState.style.display = 'none';
        tableContainer.style.display = 'block';

        tableBody.innerHTML = standings.map(s => {
            const rankClass = s.position <= 3 ? `rank-${s.position}` : 'rank-other';
            const leaderBg = s.position === 1 && s.points > 0 ? 'background-color: #f0f4ff;' : '';

            return `
                <tr style="${leaderBg}">
                    <td>
                        <span class="rank-badge ${rankClass}">${s.position}</span>
                    </td>
                    <td style="font-weight: bold;">${escapeHtml(s.teamName)}</td>
                    <td>${s.played}</td>
                    <td style="color: #2e7d32; font-weight: bold;">${s.wins}</td>
                    <td style="color: #555;">${s.draws}</td>
                    <td style="color: #c62828;">${s.losses}</td>
                    <td style="font-weight: bold; color: #1565c0;">${s.points}</td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        console.error('Error loading standings:', error);
        showToast(error.message || 'Error loading standings', 'error');
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
