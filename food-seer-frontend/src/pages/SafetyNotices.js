import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyNotifications, acknowledgeNotification } from '../services/api';
import { formatDateTime } from '../components/allergyOptions';

// Customer page: private food safety notices about orders that may have been
// exposed to an allergen on the customer's saved list.
const SafetyNotices = () => {
  const navigate = useNavigate();
  const [notices, setNotices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadNotices = async () => {
    try {
      const data = await getMyNotifications();
      setNotices(data);
      setError('');
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadNotices();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleAcknowledge = async (noticeId) => {
    try {
      await acknowledgeNotification(noticeId);
      await loadNotices();
    } catch (ackError) {
      setError(ackError.message);
    }
  };

  if (loading) {
    return (
      <div className="orders-container">
        <div className="loading">Loading your notices...</div>
      </div>
    );
  }

  return (
    <div className="orders-container">
      <div className="orders-header">
        <h1>🔔 Food Safety Notices</h1>
        <div className="header-actions">
          <button className="back-button" onClick={() => navigate('/orders')}>
            My Orders
          </button>
        </div>
      </div>

      {error && <div className="trace-error">{error}</div>}

      {notices.length === 0 ? (
        <div className="no-orders">
          <p>You have no food safety notices.</p>
        </div>
      ) : (
        <div className="orders-list">
          {notices.map((notice) => (
            <div key={notice.id} className={`trace-card ${notice.acknowledged ? '' : 'trace-unread'}`}>
              <div className="order-header">
                <h3>Order #{notice.orderId}</h3>
                <span className={`status-badge ${notice.acknowledged ? 'fulfilled' : 'pending'}`}>
                  {notice.acknowledged ? '✓ Read' : '⚠️ New'}
                </span>
              </div>
              <p>{notice.message}</p>
              {notice.remedy ? (
                <div className="trace-remedy">
                  <strong>The restaurant offers:</strong> {notice.remedy}
                </div>
              ) : (
                <p className="trace-muted">The restaurant has been told and will follow up with an offer.</p>
              )}
              <p className="trace-muted">Sent {formatDateTime(notice.createdAt)}</p>
              {!notice.acknowledged && (
                <button className="create-button" onClick={() => handleAcknowledge(notice.id)}>
                  Got it
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default SafetyNotices;
