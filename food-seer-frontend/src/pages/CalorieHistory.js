// React hooks: state for the fetched history list/loading flag, and effect to load it on mount
import React, { useState, useEffect } from 'react';
// lets the back button return to Personal Diet
import { useNavigate } from 'react-router-dom';
// logout handler, matching the pattern used on Personal Diet
import { logout, getCalorieHistory } from '../services/api';

// Calorie History page: lists the signed-in customer's total calories
// consumed (orders + logged meals) for each day they have history for,
// most recent day first. Read-only - there is nothing to edit here.
function CalorieHistory() {
  // used for the "Back to Personal Diet" and logout navigation
  const navigate = useNavigate();
  // the list of { date, caloriesConsumed } rows returned by the backend
  const [history, setHistory] = useState([]);
  // true while the initial fetch is in flight
  const [loading, setLoading] = useState(true);
  // set if the fetch fails, shown instead of the table
  const [error, setError] = useState(null);

  // fetch the history once when the page mounts
  useEffect(() => {
    // local async function, since the effect callback itself can't be async
    const loadHistory = async () => {
      // network errors are caught below and turned into the error state
      try {
        // ask the backend for every day this user has consumption recorded for
        const data = await getCalorieHistory();
        // store the fetched rows so the table can render them
        setHistory(data);
      } catch (err) {
        // show a readable message instead of leaving the page stuck loading
        setError('Could not load your calorie history. Please try again later.');
      } finally {
        // either way, the initial fetch is done
        setLoading(false);
      }
    };
    // kick off the fetch
    loadHistory();
    // empty dependency array: fetch once on mount only
  }, []);

  // signs the user out and returns to login, matching Personal Diet's handler
  const handleLogout = () => {
    // clear the stored auth token
    logout();
    // send the user back to the login page
    navigate('/');
  };

  // formats an ISO date string (e.g. "2026-10-17") as a more readable label
  const formatDate = (isoDate) => {
    // split the ISO string into its numeric year/month/day parts
    const [year, month, day] = isoDate.split('-').map(Number);
    // construct using explicit y/m/d so this isn't shifted by the browser's timezone parsing of a bare date string
    const date = new Date(year, month - 1, day);
    // render as e.g. "Sun, Oct 18, 2026"
    return date.toLocaleDateString(undefined, { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric' });
  };

  // the page's JSX
  return (
    // shares the same page chrome/spacing as Personal Diet and Recommendations
    <div className="recommendations-container">
      <div className="recommendations-header">
        <h1 className="recommendations-title">Calorie History</h1>
        <div className="header-actions">
          {/* returns to the Personal Diet page this was linked from */}
          <button className="nav-button" onClick={() => navigate('/personal-diet')}>
            Back to Personal Diet
          </button>
          {/* signs the user out */}
          <button className="logout-button" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </div>

      {/* shown only while the initial fetch is in flight */}
      {loading && <div className="loading">Loading your calorie history...</div>}

      {/* shown only if the fetch failed */}
      {!loading && error && <div className="loading">{error}</div>}

      {/* shown only once loaded, with no error, and no rows to display */}
      {!loading && !error && history.length === 0 && (
        <div className="loading">
          No calorie history yet. Place an order or log a meal on the Personal Diet page to start tracking.
        </div>
      )}

      {/* shown only once loaded, with no error, and at least one row to display */}
      {!loading && !error && history.length > 0 && (
        <div className="foods-table-container">
          <h2>Calories Consumed by Day</h2>
          <table className="foods-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Calories Consumed</th>
              </tr>
            </thead>
            <tbody>
              {/* one row per day the backend returned, already sorted most-recent-first */}
              {history.map((entry) => (
                // the ISO date string is unique per row, so it's a safe React key
                <tr key={entry.date}>
                  {/* human-readable date label */}
                  <td>{formatDate(entry.date)}</td>
                  {/* that day's total calories consumed */}
                  <td>{entry.caloriesConsumed}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

// exported for use in App.js's route table
export default CalorieHistory;
