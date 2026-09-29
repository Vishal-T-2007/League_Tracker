// Dashboard JavaScript

document.addEventListener('DOMContentLoaded', () => {
    loadDashboardStats();
    loadDashboardPreview();
});

async function loadDashboardStats() {
    try {
        const response = await fetch('/api/dashboard/stats');
        if (!response.ok) throw new Error('Failed to load dashboard statistics');
        const stats = await response.json();

        document.getElementById('stat-teams').textContent = stats.totalTeams;
        document.getElementById('stat-fixtures').textContent = stats.totalFixtures;
        document.getElementById('stat-matches').textContent = stats.completedMatches;
        
        const leaderEl = document.getElementById('stat-leader');
        leaderEl.textContent = stats.currentLeader || 'No leader yet';
        leaderEl.title = stats.currentLeader || 'No leader yet';
    } catch (error) {
        console.error('Error fetching dashboard stats:', error);
        document.getElementById('stat-teams').textContent = '0';
        document.getElementById('stat-fixtures').textContent = '0';
        document.getElementById('stat-matches').textContent = '0';
        document.getElementById('stat-leader').textContent = 'No leader yet';
    }
}

async function loadDashboardPreview() {
    try {
        const [standingsRes, fixturesRes] = await Promise.all([
            fetch('/api/standings'),
            fetch('/api/fixtures')
        ]);

        if (standingsRes.ok) {
            const standings = await standingsRes.json();
            const topStandingsBody = document.getElementById('top-standings-body');
            const topStandingsEmpty = document.getElementById('top-standings-empty');
            const topStandingsTable = document.getElementById('top-standings-table');

            if (!standings || standings.length === 0) {
                if (topStandingsEmpty) topStandingsEmpty.style.display = 'block';
                if (topStandingsTable) topStandingsTable.style.display = 'none';
            } else {
                if (topStandingsEmpty) topStandingsEmpty.style.display = 'none';
                if (topStandingsTable) topStandingsTable.style.display = 'table';
                if (topStandingsBody) {
                    topStandingsBody.innerHTML = standings.slice(0, 5).map(s => `
                        <tr>
                            <td><span class="rank-badge ${s.position <= 3 ? 'rank-' + s.position : 'rank-other'}">${s.position}</span></td>
                            <td style="font-weight: bold;">${escapeHtml(s.teamName)}</td>
                            <td>${s.played}</td>
                            <td>${s.wins}</td>
                            <td>${s.draws}</td>
                            <td>${s.losses}</td>
                            <td style="font-weight: bold; color: #1565c0;">${s.points}</td>
                        </tr>
                    `).join('');
                }
            }
        }

        if (fixturesRes.ok) {
            const fixtures = await fixturesRes.json();
            const recentFixturesBody = document.getElementById('recent-fixtures-body');
            const recentFixturesEmpty = document.getElementById('recent-fixtures-empty');
            const recentFixturesTable = document.getElementById('recent-fixtures-table');

            if (!fixtures || fixtures.length === 0) {
                if (recentFixturesEmpty) recentFixturesEmpty.style.display = 'block';
                if (recentFixturesTable) recentFixturesTable.style.display = 'none';
            } else {
                if (recentFixturesEmpty) recentFixturesEmpty.style.display = 'none';
                if (recentFixturesTable) recentFixturesTable.style.display = 'table';
                if (recentFixturesBody) {
                    recentFixturesBody.innerHTML = fixtures.slice(0, 5).map(f => {
                        const scoreDisplay = f.status === 'COMPLETED' 
                            ? `<span style="font-weight: bold; color: #2e7d32;">${f.homeScore} - ${f.awayScore}</span>`
                            : `<span style="color: #aaa;">-</span>`;
                        const statusBadge = f.status === 'COMPLETED'
                            ? `<span class="badge badge-completed">Completed</span>`
                            : `<span class="badge badge-scheduled">Scheduled</span>`;

                        return `
                            <tr>
                                <td>Round ${f.roundNumber}</td>
                                <td style="font-weight: 600;">${escapeHtml(f.homeTeamName)} vs ${escapeHtml(f.awayTeamName)}</td>
                                <td>${statusBadge}</td>
                                <td>${scoreDisplay}</td>
                            </tr>
                        `;
                    }).join('');
                }
            }
        }
    } catch (err) {
        console.error('Error fetching preview data:', err);
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;")
              .replace(/</g, "&lt;")
              .replace(/>/g, "&gt;")
              .replace(/"/g, "&quot;")
              .replace(/'/g, "&#039;");
}
