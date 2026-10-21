import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  getUnfulfilledOrders,
  getFulfilledOrders,
  fulfillOrder,
  getCurrentUser,
  acknowledgeAllergyHandoff,
  getAllergyHandoffStatus
} from '../services/api';

const OrderManagement = () => {
  const [unfulfilledOrders, setUnfulfilledOrders] = useState([]);
  const [fulfilledOrders, setFulfilledOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [view, setView] = useState('unfulfilled'); // unfulfilled or fulfilled
  const [processing, setProcessing] = useState({});
  const [allergyStatus, setAllergyStatus] = useState({});
  const navigate = useNavigate();

  const fetchOrders = async () => {
    try {
      const user = await getCurrentUser();

      if (user.role !== 'ROLE_ADMIN' && user.role !== 'ROLE_STAFF') {
        alert('Access denied. Staff or Admin privileges required.');
        navigate('/');
        return;
      }

      const unfulfilled = await getUnfulfilledOrders();
      const fulfilled = await getFulfilledOrders();

      setUnfulfilledOrders(unfulfilled);
      setFulfilledOrders(fulfilled);

      // Retrieve acknowledgment status for orders with dietary restrictions.
      if (user.role === 'ROLE_STAFF') {
        const statusEntries = await Promise.all(
          unfulfilled
            .filter(order => order.allergyRestrictions?.trim())
            .map(async order => {
              try {
                const status = await getAllergyHandoffStatus(order.id);
                return [order.id, status];
              } catch (error) {
                console.error(
                  `Failed to load allergy status for order ${order.id}:`,
                    error
                );
                return [order.id, null];
              }
            })
          );

          setAllergyStatus(Object.fromEntries(statusEntries));
        }
    } catch (error) {
      console.error('Error fetching orders:', error);
      navigate('/');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, []);

  const handleAcknowledgeAllergy = async (orderId) => {
  setProcessing(prev => ({ ...prev, [orderId]: true }));

  try {
    await acknowledgeAllergyHandoff(orderId);
    alert('Allergy warning acknowledged successfully!');
    await fetchOrders();
  } catch (error) {
    console.error('Allergy acknowledgment failed:', error);
    alert(error.message || 'Failed to acknowledge allergy warning.');
  } finally {
    setProcessing(prev => ({ ...prev, [orderId]: false }));
  }
};

  const handleFulfillOrder = async (orderId) => {
    if (!window.confirm('Mark this order as fulfilled?')) return;

    setProcessing(prev => ({ ...prev, [orderId]: true }));

    try {
      await fulfillOrder(orderId);
      alert('Order fulfilled successfully!');
      await fetchOrders();
    } catch (error) {
      console.error('Error fulfilling order:', error);
      alert('Failed to fulfill order. Please try again.');
    } finally {
      setProcessing(prev => ({ ...prev, [orderId]: false }));
    }
  };

  // order.cost already includes the delivery fee (set when the order was placed);
  // only fall back to summing food prices for an older order that has no stored cost
  const getTotalPrice = (order) =>
    order.cost !== undefined && order.cost !== null
      ? order.cost
      : order.foods.reduce((total, food) => total + food.price, 0);

  if (loading) return <div className="staff-dashboard-container"><div className="loading">Loading orders...</div></div>;

  const displayOrders = view === 'unfulfilled' ? unfulfilledOrders : fulfilledOrders;

  return (
    <div className="staff-dashboard-container">
      <div className="dashboard-header">
        <h1>📦 Order Management</h1>
        <div style={{ display: 'flex', gap: '10px' }}>
          {/* Navigate to Order Dashboard page */}
          <button 
            className="dashboard-button" 
            onClick={() => navigate('/order-dashboard')}
          >
            📊 Order Dashboard
          </button>
          <button className="back-button" onClick={() => navigate('/inventory-management')}>
            Back
          </button>
        </div>
      </div>

      <div className="dashboard-stats">
        <div className="stat-card">
          <h3>Pending Orders</h3>
          <p className="stat-number">{unfulfilledOrders.length}</p>
        </div>
        <div className="stat-card">
          <h3>Fulfilled Today</h3>
          <p className="stat-number">{fulfilledOrders.length}</p>
        </div>
        <div className="stat-card">
          <h3>Total Orders</h3>
          <p className="stat-number">{unfulfilledOrders.length + fulfilledOrders.length}</p>
        </div>
      </div>

      <div className="view-toggle">
        <button
          className={`toggle-button ${view === 'unfulfilled' ? 'active' : ''}`}
          onClick={() => setView('unfulfilled')}
        >
          Pending Orders ({unfulfilledOrders.length})
        </button>
        <button
          className={`toggle-button ${view === 'fulfilled' ? 'active' : ''}`}
          onClick={() => setView('fulfilled')}
        >
          Fulfilled Orders ({fulfilledOrders.length})
        </button>
      </div>

      {displayOrders.length === 0 ? (
        <div className="no-orders">
          <p>No {view} orders at this time.</p>
        </div>
      ) : (
        <div className="orders-section">
          {displayOrders.map(order => (
            <div key={order.id} className={`order-card ${view === 'unfulfilled' ? 'pending' : 'completed'}`}>
              <div className="order-header">
                <div className="order-title">
                  <h3>{order.name || `Order #${order.id}`}</h3>
                  <span className="order-id">ID: #{order.id}</span>
                </div>
                {view === 'unfulfilled' ? (
                  <button
                    className="fulfill-button"
                    onClick={() => handleFulfillOrder(order.id)}
                    disabled={processing[order.id]}
                  >
                    {processing[order.id] ? 'Processing...' : '✓ Fulfill Order'}
                  </button>
                ) : (
                  <span className="fulfilled-badge">✓ Fulfilled</span>
                )}
              </div>

              <div className="order-summary">
                <div className="summary-item">
                  <span className="label">Total Items:</span>
                  <span className="value">{order.foods.length}</span>
                </div>
                <div className="summary-item">
                  <span className="label">Total Price:</span>
                  <span className="value">${getTotalPrice(order)}</span>
                </div>
              </div>

              {view === 'unfulfilled' && order.allergyRestrictions?.trim() && (
                <div className="allergy-handoff-warning">
                  <h4>Dietary Restriction Warning</h4>

                  <p>
                    <strong>Customer restrictions:</strong>{' '}
                    {order.allergyRestrictions}
                  </p>

                  {allergyStatus[order.id]?.staffAcknowledged ? (
                    <p className="allergy-acknowledged">
                      ✓ Restaurant allergy warning acknowledged
                    </p>
                  ) : allergyStatus[order.id] === undefined ? (
  <p>Checking acknowledgment status...</p>
                  ) : allergyStatus[order.id] === null ? (
                    <p>Unable to verify acknowledgment status. Please refresh.</p>
                  ) : (
                    <button
                      className="fulfill-button"
                      onClick={() => handleAcknowledgeAllergy(order.id)}
                      disabled={processing[order.id]}
                    >
                      {processing[order.id]
                        ? 'Processing...'
                        : 'Acknowledge Allergy Warning'}
                    </button>
                  )}
                  </div>
                )}

              <div className="order-items-detail">
                <h4>Order Items:</h4>
                <table className="items-table">
                  <thead>
                    <tr>
                      <th>Item</th>
                      <th>Price</th>
                      <th>Allergies</th>
                    </tr>
                  </thead>
                  <tbody>
                    {order.foods.map((food, index) => (
                      <tr key={`${food.id}-${index}`}>
                        <td>{food.foodName}</td>
                        <td>${food.price}</td>
                        <td>{food.allergies && food.allergies.length > 0 ? food.allergies.join(', ') : 'None'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default OrderManagement;
