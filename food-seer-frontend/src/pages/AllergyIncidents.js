import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  getCurrentUser,
  getAllFoods,
  getIncidents,
  reportRestaurantIncident,
  acknowledgeIncident,
} from '../services/api';
import { ALLERGEN_SUGGESTIONS, formatDateTime, toDateTimeInputValue } from '../components/allergyOptions';

// Staff and admin page: report a possible cross-contact incident, then see
// every incident and attach what the restaurant offers affected customers.
// Customers are warned privately; this page shows counts, never their names.
const AllergyIncidents = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [foods, setFoods] = useState([]);
  const [incidents, setIncidents] = useState([]);

  const [allergen, setAllergen] = useState('');
  const [selectedFoodIds, setSelectedFoodIds] = useState([]);
  const [windowStart, setWindowStart] = useState(() => {
    const twoHoursAgo = new Date(Date.now() - 2 * 60 * 60 * 1000);
    return toDateTimeInputValue(twoHoursAgo);
  });
  const [windowEnd, setWindowEnd] = useState(() => toDateTimeInputValue(new Date()));
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState('');
  const [formSuccess, setFormSuccess] = useState('');

  const [remedyDrafts, setRemedyDrafts] = useState({});
  const [acknowledging, setAcknowledging] = useState({});
  const [listError, setListError] = useState('');

  const loadIncidents = async () => {
    try {
      const data = await getIncidents();
      setIncidents(data);
      setListError('');
    } catch (error) {
      setListError(error.message);
    }
  };

  useEffect(() => {
    const load = async () => {
      try {
        const user = await getCurrentUser();
        if (user.role !== 'ROLE_ADMIN' && user.role !== 'ROLE_STAFF') {
          alert('Access denied. Staff or Admin privileges required.');
          navigate('/');
          return;
        }
        const menu = await getAllFoods();
        setFoods(menu);
        await loadIncidents();
      } catch (error) {
        console.error('Error loading the allergy incidents page:', error);
        navigate('/');
      } finally {
        setLoading(false);
      }
    };
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const toggleFood = (foodId) => {
    if (selectedFoodIds.includes(foodId)) {
      setSelectedFoodIds(selectedFoodIds.filter((id) => id !== foodId));
    } else {
      setSelectedFoodIds([...selectedFoodIds, foodId]);
    }
  };

  const handleReport = async (event) => {
    event.preventDefault();
    setFormError('');
    setFormSuccess('');

    if (allergen.trim() === '') {
      setFormError('Please enter the allergen.');
      return;
    }
    if (selectedFoodIds.length === 0) {
      setFormError('Please select at least one dish.');
      return;
    }
    if (windowStart === '' || windowEnd === '') {
      setFormError('Please enter both the start and the end of the time window.');
      return;
    }
    if (windowStart > windowEnd) {
      setFormError('The window start must be before the window end.');
      return;
    }

    setSubmitting(true);
    try {
      const result = await reportRestaurantIncident(allergen.trim(), selectedFoodIds, windowStart, windowEnd);
      const customerWord = result.notifiedCount === 1 ? 'customer' : 'customers';
      const orderWord = result.exposedOrderCount === 1 ? 'order' : 'orders';
      setFormSuccess(
        `Incident #${result.incidentId} saved. ${result.exposedOrderCount} ${orderWord} fell in the window, and `
          + `${result.notifiedCount} ${customerWord} with this allergy were warned privately.`
      );
      setAllergen('');
      setSelectedFoodIds([]);
      await loadIncidents();
    } catch (error) {
      setFormError(error.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleAcknowledge = async (incidentId) => {
    const remedy = remedyDrafts[incidentId] || '';
    if (remedy.trim() === '') {
      setListError('Please describe what the restaurant offers, for example a refund or a free remake.');
      return;
    }
    setAcknowledging({ ...acknowledging, [incidentId]: true });
    try {
      await acknowledgeIncident(incidentId, remedy.trim());
      setRemedyDrafts({ ...remedyDrafts, [incidentId]: '' });
      await loadIncidents();
    } catch (error) {
      setListError(error.message);
    } finally {
      setAcknowledging({ ...acknowledging, [incidentId]: false });
    }
  };

  if (loading) {
    return (
      <div className="orders-container">
        <div className="loading">Loading allergy incidents...</div>
      </div>
    );
  }

  return (
    <div className="orders-container">
      <div className="orders-header">
        <h1>⚠️ Allergy Incidents</h1>
      </div>

      <form className="trace-card trace-form" onSubmit={handleReport}>
        <h2>Report a possible cross-contact</h2>
        <p className="trace-muted">
          Customers who ordered these dishes in this window and have this allergy saved get a private notice.
          Nobody is told who else was affected.
        </p>

        <label className="trace-field">
          <span>Allergen</span>
          <input
            type="text"
            list="trace-allergen-suggestions"
            value={allergen}
            onChange={(event) => setAllergen(event.target.value)}
            placeholder="For example: Peanuts"
          />
          <datalist id="trace-allergen-suggestions">
            {ALLERGEN_SUGGESTIONS.map((option) => (
              <option key={option} value={option} />
            ))}
          </datalist>
        </label>

        <div className="trace-field">
          <span>Dishes that may carry it</span>
          <div className="trace-dish-grid">
            {foods.map((food) => (
              <label key={food.id} className={`trace-dish ${selectedFoodIds.includes(food.id) ? 'selected' : ''}`}>
                <input
                  type="checkbox"
                  checked={selectedFoodIds.includes(food.id)}
                  onChange={() => toggleFood(food.id)}
                />
                <span>{food.foodName}</span>
              </label>
            ))}
          </div>
        </div>

        <div className="trace-window">
          <label className="trace-field">
            <span>Window start</span>
            <input type="datetime-local" value={windowStart} onChange={(event) => setWindowStart(event.target.value)} />
          </label>
          <label className="trace-field">
            <span>Window end</span>
            <input type="datetime-local" value={windowEnd} onChange={(event) => setWindowEnd(event.target.value)} />
          </label>
        </div>

        {formError && <div className="trace-error">{formError}</div>}
        {formSuccess && <div className="trace-success">{formSuccess}</div>}

        <button type="submit" className="create-button" disabled={submitting}>
          {submitting ? 'Tracing...' : 'Trace and warn customers'}
        </button>
      </form>

      <h2 className="trace-section-title">All incidents</h2>
      {listError && <div className="trace-error">{listError}</div>}

      {incidents.length === 0 ? (
        <div className="no-orders">
          <p>No incidents reported yet.</p>
        </div>
      ) : (
        <div className="orders-list">
          {incidents.map((incident) => (
            <div key={incident.id} className="trace-card">
              <div className="order-header">
                <h3>
                  Incident #{incident.id}: {incident.allergen}
                </h3>
                <span className={`status-badge ${incident.restaurantAcknowledgedAt ? 'fulfilled' : 'pending'}`}>
                  {incident.restaurantAcknowledgedAt ? '✓ Acknowledged' : '⏳ Needs a response'}
                </span>
              </div>

              <p>
                <strong>Reported by:</strong>{' '}
                {incident.source === 'CUSTOMER'
                  ? `a customer, after order #${incident.sourceOrderId}`
                  : 'the restaurant'}{' '}
                at {formatDateTime(incident.reportedAt)}
              </p>
              <p>
                <strong>Suspect dishes:</strong> {incident.suspectFoodNames.join(', ')}
              </p>
              <p>
                <strong>Window:</strong> {formatDateTime(incident.windowStart)} to {formatDateTime(incident.windowEnd)}
              </p>
              <p>
                <strong>Orders in the window:</strong> {incident.exposedOrderCount ?? 0} ·{' '}
                <strong>Customers warned:</strong> {incident.notifiedCount ?? 0}
              </p>

              {incident.remedy && (
                <div className="trace-remedy">
                  <strong>Our offer to affected customers:</strong> {incident.remedy}
                  <div className="trace-muted">
                    Acknowledged by {incident.acknowledgedBy} at {formatDateTime(incident.restaurantAcknowledgedAt)}
                  </div>
                </div>
              )}

              <div className="trace-ack">
                <input
                  type="text"
                  value={remedyDrafts[incident.id] || ''}
                  onChange={(event) => setRemedyDrafts({ ...remedyDrafts, [incident.id]: event.target.value })}
                  placeholder={incident.remedy ? 'Update the offer' : 'What do you offer? For example: Full refund or a free remake'}
                  maxLength={500}
                />
                <button
                  type="button"
                  className="create-button"
                  disabled={acknowledging[incident.id] === true}
                  onClick={() => handleAcknowledge(incident.id)}
                >
                  {incident.remedy ? 'Update offer' : 'Acknowledge'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default AllergyIncidents;
