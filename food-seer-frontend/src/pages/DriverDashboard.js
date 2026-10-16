import React, { useEffect, useState } from "react";
import {fetchDriverDashboard, getCurrentUser, getAvailableOrders, getActiveOrders, updateOrderStatus, getUnfulfilledOrders, claimOrder } from "../services/api";

const DriverDashboard = () => {
  const [stats, setStats] = useState(null);
  const [availableOrders, setAvailableOrders] = useState([]);
  const [activeOrders, setActiveOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const loadDashboard = async () => {
      try {
        const user = await getCurrentUser();

        const [
          dashboardStats,
          available,
          active
        ] = await Promise.all([
          fetchDriverDashboard(user.username),
          getAvailableOrders(user.username),
          getActiveOrders(user.username)
        ]);

        setStats(dashboardStats);
        setAvailableOrders(available);
        setActiveOrders(active);
      } catch (err) {
        console.error(err);
        setError("Failed to load dashboard data.");
      } finally {
        setLoading(false);
      }
    };

    loadDashboard();
  }, []);

  const handleClaim = async (orderId) => {
  try {
    await claimOrder(orderId);
    window.location.reload();
  } catch (err) {
    console.error(err);
    setError(err.message || "Failed to claim order.");
  }
};

  const handlePickUp = async (orderId) => {
    try {
      const user = await getCurrentUser();

      await updateOrderStatus(orderId, "Picked Up", user.username);

      // 🔁 Force refresh (as requested)
      window.location.reload();

    } catch (err) {
      console.error(err);
      setError("Failed to pick up order.");
    }
  };

  const handleDeliver = async (orderId) => {
    try {
      const user = await getCurrentUser();

      await updateOrderStatus(orderId, "Delivered", user.username);

      // 🔁 Force refresh (as requested)
      window.location.reload();

    } catch (err) {
      console.error(err);
      setError("Failed to deliver order.");
    }
  };

  if (loading) {
    return <div className="dashboard-page">Loading...</div>;
  }

  if (error) {
    return <div className="dashboard-page">{error}</div>;
  }

  return (
    <div className="driver-dashboard-page">

      {/* ---------------- Stats ---------------- */}
      <div className="dashboard-stats">

        <div className="stat-card">
          <h3>Total Deliveries</h3>
          <p className="stat-number">{stats.totalDeliveries}</p>
        </div>

        <div className="stat-card">
          <h3>Today’s Earnings</h3>
          <p className="stat-number">${stats.totalEarning}</p>
        </div>

      </div>


      <div className="order-wrapper">
          {/* ---------------- Active Orders ---------------- */}
          <section className="orders-section active">
              <div className="orders-container">
                <div className="orders-header">
                  <h2>Active Orders</h2>
                </div>

                <div className="orders-list">
                  {activeOrders.length === 0 ? (
                    <p>No active orders.</p>
                  ) : (
                    activeOrders.map((order) => (
                      <div key={order.id} className="order-card active">

                        <div className="order-header">
                          <h3>Order #{order.id}</h3>
                          <span className="status-badge active">
                            PICKED UP
                          </span>
                        </div>

                        <div className="order-summary">
                          <div className="summary-item">
                            <span className="label">Earnings</span>
                            <span className="value">${order.deliveryCost}</span>
                          </div>
                        </div>

                        <button
                          className="fulfill-button"
                          onClick={() => handleDeliver(order.id)}
                        >
                          Mark as Delivered
                        </button>

                      </div>
                    ))
                  )}
                </div>
              </div>
          </section>

          {/* ---------------- Available Orders ---------------- */}
        <section className="orders-section available">
            <div className="orders-container">
              <div className="orders-header">
                <h2>Available Orders</h2>
              </div>

              <div className="orders-list">
                {availableOrders.length === 0 ? (
                  <p>No available orders.</p>
                ) : (
                  availableOrders.map((order) => (
                    <div key={order.id} className="order-card pending">

                      <div className="order-header">
                        <h3>Order #{order.id}</h3>
                        <span className="status-badge pending">
                          PLACED
                        </span>
                      </div>

                      <div className="order-summary">
                        <div className="summary-item">
                          <span className="label">Earnings</span>
                          <span className="value">${order.deliveryCost}</span>
                        </div>
                      </div>

                      <button
                        className="fulfill-button"
                        onClick={() => handleClaim(order.id)}
                      >
                        Claim Order
                      </button>

                    </div>
                  ))
                )}
              </div>
            </div>
        </section>

       </div>
    </div>
  );
};

export default DriverDashboard;