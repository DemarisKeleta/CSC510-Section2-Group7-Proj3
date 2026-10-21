import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { logout, getCurrentUser, getMyUnreadNotificationCount } from '../services/api';

const Navigation = () => {
  const [user, setUser] = useState(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [unreadNotices, setUnreadNotices] = useState(0);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const userData = await getCurrentUser();
        setUser(userData);
      } catch (error) {
        console.error('Error fetching user:', error);
      }
    };

    fetchUser();
  }, [location]);

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  // Customers: keep the unread food safety notice count fresh on every page change.
  useEffect(() => {
    const fetchUnreadNotices = async () => {
      if (user?.role !== 'ROLE_CUSTOMER') {
        setUnreadNotices(0);
        return;
      }
      try {
        const count = await getMyUnreadNotificationCount();
        setUnreadNotices(count);
      } catch (error) {
        console.error('Error fetching safety notices:', error);
      }
    };

    fetchUnreadNotices();
  }, [user, location]);

  const isAdmin = user?.role === 'ROLE_ADMIN';
  const isStaff = user?.role === 'ROLE_STAFF' || isAdmin;
  const isStandardUser = user?.role === 'ROLE_CUSTOMER';
  const isDriver = user?.role === 'ROLE_DRIVER';

  const handleBrandClick = () => {
    if (isStaff) {
      navigate('/order-management');
    } else {
      navigate('/recommendations');
    }
  };

  return (
    <nav className="main-navigation">
      <div className="nav-container">
        <div className="nav-brand" onClick={handleBrandClick}>
          🍽️ FoodSeer
        </div>

        <button
          className="nav-toggle"
          onClick={() => setMenuOpen(!menuOpen)}
        >
          ☰
        </button>

        <div className={`nav-menu ${menuOpen ? 'open' : ''}`}>
          <div className="nav-links">
            {/* Customer-only menu items */}
            {isStandardUser && (
              <>
                <button
                  className={`nav-link ${location.pathname === '/recommendations' ? 'active' : ''}`}
                  onClick={() => {
                    navigate('/recommendations');
                    setMenuOpen(false);
                  }}
                >
                  🏠 Home
                </button>

                <button
                  className={`nav-link ${location.pathname === '/chatbot' ? 'active' : ''}`}
                  onClick={() => {
                    navigate('/chatbot');
                    setMenuOpen(false);
                  }}
                >
                  🤖 AI Assistant
                </button>

                <button
                  className={`nav-link ${location.pathname === '/quiz' ? 'active' : ''}`}
                  onClick={() => {
                    navigate('/quiz');
                    setMenuOpen(false);
                  }}
                >
                  📋 Quick Quiz
                </button>

                <button
                  className={`nav-link ${location.pathname === '/inventory' ? 'active' : ''}`}
                  onClick={() => {
                    navigate('/inventory');
                    setMenuOpen(false);
                  }}
                >
                  🏪 Browse Foods
                </button>

                <button
                  className={`nav-link ${location.pathname === '/create-order' ? 'active' : ''}`}
                  onClick={() => {
                    navigate('/create-order');
                    setMenuOpen(false);
                  }}
                >
                  🛒 Create Order
                </button>

                <button
                  className={`nav-link ${location.pathname === '/orders' ? 'active' : ''}`}
                  onClick={() => {
                    navigate('/orders');
                    setMenuOpen(false);
                  }}
                >
                  📦 My Orders
                </button>

                <button
                  className={`nav-link ${location.pathname === '/notices' ? 'active' : ''}`}
                  onClick={() => {
                    navigate('/notices');
                    setMenuOpen(false);
                  }}
                >
                  🔔 Safety Notices{unreadNotices > 0 ? ` (${unreadNotices})` : ''}
                </button>

                <button
                  className={`nav-link ${location.pathname === '/personal-diet' ? 'active' : ''}`}
                  onClick={() => {
                    navigate('/personal-diet');
                    setMenuOpen(false);
                  }}
                >
                  🔥 Personal Diet
                </button>
              </>
            )}

            {/* Staff & Admin menu items */}
            {isStaff && (
              <>
                {isStandardUser && <div className="nav-divider"></div>}
                <button
                  className={`nav-link staff ${location.pathname === '/order-management' ? 'active' : ''}`}
                  onClick={() => {
                    navigate('/order-management');
                    setMenuOpen(false);
                  }}
                >
                  📦 Order Management
                </button>

                <button
                  className={`nav-link staff ${location.pathname === '/inventory-management' ? 'active' : ''}`}
                  onClick={() => {
                    navigate('/inventory-management');
                    setMenuOpen(false);
                  }}
                >
                  🏪 Inventory Management
                </button>

                <button
                  className={`nav-link staff ${location.pathname === '/allergy-incidents' ? 'active' : ''}`}
                  onClick={() => {
                    navigate('/allergy-incidents');
                    setMenuOpen(false);
                  }}
                >
                  ⚠️ Allergy Incidents
                </button>
              </>
            )}

            {/* Admin-only menu items */}
            {isAdmin && (
              <>
                <button
                  className={`nav-link admin ${location.pathname === '/users' ? 'active' : ''}`}
                  onClick={() => {
                    navigate('/users');
                    setMenuOpen(false);
                  }}
                >
                  👥 User Management
                </button>
              </>
            )}

            {/* Driver-only menu item */}
            {isDriver && (
              <>
                <button
                  className={`nav-link driver ${location.pathname === '/driver-dashboard' ? 'active' : ''}`}
                  onClick={() => {
                    navigate('/driver-dashboard');
                    setMenuOpen(false);
                  }}
                >
                  🚚 Driver Dashboard
                </button>
              </>
            )}
          </div>

          <div className="nav-user">
            {user && (
              <>
                <span className="user-info">
                  <span className="username">{user.username}</span>
                  <span className={`role-badge ${user.role.toLowerCase().replace('role_', '')}`}>
                    {user.role.replace('ROLE_', '')}
                  </span>
                </span>
                <button className="logout-button" onClick={handleLogout}>
                  Logout
                </button>
              </>
            )}
          </div>
        </div>
      </div>
      {isStandardUser && unreadNotices > 0 && location.pathname !== '/notices' && (
        <div className="trace-banner" onClick={() => navigate('/notices')}>
          ⚠️ You have {unreadNotices} unread food safety {unreadNotices === 1 ? 'notice' : 'notices'} about your
          orders. Tap to read.
        </div>
      )}
    </nav>
  );
};

export default Navigation;
