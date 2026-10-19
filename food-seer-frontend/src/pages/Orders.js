import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  getMyOrders,
  getCurrentUser,
  rateFoodItem,
  reportCustomerReaction,
  updateUserPreferences,
  addAllergen
} from '../services/api'; // Added rateFoodItem
import { ALLERGEN_SUGGESTIONS } from '../components/allergyOptions';

const Orders = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all'); // all, fulfilled, unfulfilled
  // The customer's own saved allergies (set on Personal Diet / the onboarding
  // quiz), so the reaction-report suggestions below reflect what they've
  // actually told FoodSeer they're allergic to, not just generic examples.
  const [userAllergies, setUserAllergies] = useState([]);
  // Full user record, kept so handleReportReaction below can pass through
  // costPreference/dailyCalorieGoal unchanged when saving a new allergy -
  // updateUserPreferences overwrites those fields if they're omitted.
  const [currentUser, setCurrentUser] = useState(null);
  
  // New state to track dropdown values: { "foodId-orderId": 5 }
  const [ratings, setRatings] = useState({});

  // Allergy reaction reports: which order's form is open, what was typed, and
  // the result shown on each order card.
  const [reactionOrderId, setReactionOrderId] = useState(null);
  const [reactionAllergen, setReactionAllergen] = useState('');
  const [reactionSubmitting, setReactionSubmitting] = useState(false);
  const [reactionResults, setReactionResults] = useState({});
  
  const navigate = useNavigate();

  useEffect(() => {
    fetchOrders();
  }, [navigate]);

  const fetchOrders = async () => {
    try {
      const userData = await getCurrentUser(); // Verify authentication
      setCurrentUser(userData);
      const restrictions = userData?.dietaryRestrictions
        ? userData.dietaryRestrictions.split(',').map((a) => a.trim()).filter((a) => a.length > 0)
        : [];
      setUserAllergies(restrictions);
      const ordersData = await getMyOrders(); // Get only current user's orders
      setOrders(ordersData);
    } catch (error) {
      console.error('Error fetching orders:', error);
      navigate('/');
    } finally {
      setLoading(false);
    }
  };

  // The customer's own allergies first (most relevant to what they'd actually
  // be reporting a reaction to), then the generic examples, deduplicated
  // case-insensitively so an allergy they already added isn't listed twice.
  const reactionAllergenSuggestions = [
    ...userAllergies,
    ...ALLERGEN_SUGGESTIONS.filter(
      (suggestion) => !userAllergies.some((a) => a.toLowerCase() === suggestion.toLowerCase())
    )
  ];

  const handleBack = () => {
    navigate('/recommendations');
  };

  const handleCreateOrder = () => {
    navigate('/create-order');
  };

  const getFilteredOrders = () => {
    switch (filter) {
      case 'fulfilled':
        return orders.filter(order => order.isFulfilled);
      case 'unfulfilled':
        return orders.filter(order => !order.isFulfilled);
      default:
        return orders;
    }
  };

  const getTotalPrice = (order) => {
    // order.cost already includes the delivery fee (set when the order was placed);
    // only fall back to summing food prices for an older order that has no stored cost
    if (order.cost !== undefined && order.cost !== null) {
      return order.cost;
    }
    return order.foods.reduce((total, food) => total + food.price, 0);
  };

  // --- NEW RATING HANDLERS ---

  const handleRatingChange = (key, value) => {
    setRatings(prev => ({ ...prev, [key]: value }));
  };

  const handleSubmitRating = async (orderId, foodId) => {
    // Create a unique key for the dropdown state
    const uniqueKey = `${foodId}-${orderId}`;
    const ratingValue = ratings[uniqueKey] || 5; // Default to 5

    try {
      await rateFoodItem(orderId, foodId, ratingValue);
      alert("Rating submitted successfully! ⭐");
      
      // Refresh orders to update the "ratedFoodIds" list and disable the button
      fetchOrders(); 
    } catch (error) {
        // Handle the 409 or 400 errors from backend
        if (error.message && error.message.includes("already rated")) {
             alert("You have already rated this item.");
        } else {
             alert("Error submitting rating: " + error.message);
        }
    }
  };

  // ---------------------------

  const openReactionForm = (orderId) => {
    setReactionOrderId(orderId);
    setReactionAllergen('');
  };

  const closeReactionForm = () => {
    setReactionOrderId(null);
    setReactionAllergen('');
  };

  const handleReportReaction = async (orderId) => {
    if (reactionAllergen.trim() === '') {
      setReactionResults({ ...reactionResults, [orderId]: { ok: false, text: 'Please enter the allergen you reacted to.' } });
      return;
    }
    setReactionSubmitting(true);
    try {
      await reportCustomerReaction(orderId, reactionAllergen.trim());

      // A reported reaction means this is a real, known allergy for this
      // customer now - save it to their profile too, not just the incident
      // record, so Personal Diet and future order filtering reflect it.
      const normalizedAllergen = reactionAllergen.trim().toUpperCase();
      const wasAlreadyKnown = userAllergies.some((a) => a.toUpperCase() === normalizedAllergen);
      if (!wasAlreadyKnown && currentUser) {
        const updatedRestrictions = [...userAllergies, normalizedAllergen].join(', ');
        await updateUserPreferences(currentUser.costPreference, updatedRestrictions, currentUser.dailyCalorieGoal);
        setUserAllergies((prev) => [...prev, normalizedAllergen]);
        // Also add it to the shared, site-wide allergen list (same list
        // Personal Diet, the onboarding quiz, and staff's Add Food form use),
        // so it's a real selectable option everywhere, not just this profile.
        try {
          await addAllergen(normalizedAllergen);
        } catch (allergenError) {
          console.error('Error adding allergen to the shared list:', allergenError);
        }
      }

      setReactionResults({
        ...reactionResults,
        [orderId]: {
          ok: true,
          text: 'Thank you. The restaurant has been notified, and other customers who may have been exposed '
            + 'will get a private notice. If you feel unwell, please seek medical care.'
            + (wasAlreadyKnown ? '' : ` We've also added "${normalizedAllergen}" to your saved allergies.`),
        },
      });
      closeReactionForm();
    } catch (error) {
      setReactionResults({ ...reactionResults, [orderId]: { ok: false, text: error.message } });
    } finally {
      setReactionSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="orders-container">
        <div className="loading">Loading orders...</div>
      </div>
    );
  }

  const filteredOrders = getFilteredOrders();

  return (
    <div className="orders-container">
      <div className="orders-header">
        <h1>📦 My Orders</h1>
        <div className="header-actions">
          <button className="create-button" onClick={handleCreateOrder}>
            Create New Order
          </button>
          <button className="back-button" onClick={handleBack}>
            Back
          </button>
        </div>
      </div>

      <div className="orders-filters">
        <button
          className={`filter-button ${filter === 'all' ? 'active' : ''}`}
          onClick={() => setFilter('all')}
        >
          All Orders ({orders.length})
        </button>
        <button
          className={`filter-button ${filter === 'unfulfilled' ? 'active' : ''}`}
          onClick={() => setFilter('unfulfilled')}
        >
          Pending ({orders.filter(o => !o.isFulfilled).length})
        </button>
        <button
          className={`filter-button ${filter === 'fulfilled' ? 'active' : ''}`}
          onClick={() => setFilter('fulfilled')}
        >
          Fulfilled ({orders.filter(o => o.isFulfilled).length})
        </button>
      </div>

      {filteredOrders.length === 0 ? (
        <div className="no-orders">
          <p>No orders found.</p>
          <button className="create-button" onClick={handleCreateOrder}>
            Create Your First Order
          </button>
        </div>
      ) : (
        <div className="orders-list">
          {filteredOrders.map(order => (
            <div key={order.id} className="order-card">
              <div className="order-header">
                <h3>{order.name || `Order #${order.id}`}</h3>
                <span className={`status-badge ${order.isFulfilled ? 'fulfilled' : 'pending'}`}>
                  {order.isFulfilled ? '✓ Fulfilled' : '⏳ Pending'}
                </span>
              </div>
              
              <div className="order-details">
                <div className="order-info">
                  <p><strong>Order ID:</strong> #{order.id}</p>
                  <p><strong>Total Items:</strong> {order.foods.length}</p>
                  <p><strong>Total Price:</strong> ${getTotalPrice(order)}</p>

                  {/* Allergy reaction report */}
                  {reactionOrderId === order.id ? (
                    <div className="trace-reaction-form">
                      <input
                        type="text"
                        list={`trace-reaction-allergens-${order.id}`}
                        value={reactionAllergen}
                        onChange={(event) => setReactionAllergen(event.target.value)}
                        placeholder="What did you react to? For example: Peanuts"
                      />
                      <datalist id={`trace-reaction-allergens-${order.id}`}>
                        {reactionAllergenSuggestions.map((option) => (
                          <option key={option} value={option} />
                        ))}
                      </datalist>
                      <button
                        className="create-button"
                        disabled={reactionSubmitting}
                        onClick={() => handleReportReaction(order.id)}
                      >
                        {reactionSubmitting ? 'Sending...' : 'Send report'}
                      </button>
                      <button className="back-button" onClick={closeReactionForm}>
                        Cancel
                      </button>
                    </div>
                  ) : (
                    <button className="trace-report-button" onClick={() => openReactionForm(order.id)}>
                      ⚠️ Report an allergic reaction
                    </button>
                  )}
                  {reactionResults[order.id] && (
                    <div className={reactionResults[order.id].ok ? 'trace-success' : 'trace-error'}>
                      {reactionResults[order.id].text}
                    </div>
                  )}
                </div>

                <div className="order-items">
                  <h4>Items:</h4>
                  {/* Changed <ul> to <div> for better layout control */}
                  <div className="food-list-container">
                    {order.foods.map((food, index) => {
                      
                      // --- RATING LOGIC ---
                      // Check if backend says this ID is already rated
                      const isRated = order.ratedFoodIds && order.ratedFoodIds.includes(food.id);
                      // Only allow rating if order is Fulfilled AND not yet rated
                      const canRate = order.isFulfilled && !isRated;
                      
                      const uniqueKey = `${food.id}-${order.id}`;

                      return (
                        <div key={`${food.id}-${index}`} className="food-item-row" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', borderBottom: '1px dotted #eee' }}>
                          
                          {/* Left Side: Food Info */}
                          <div className="food-info-left">
                             <span style={{ fontWeight: 'bold' }}>{food.foodName}</span> 
                             <span style={{ marginLeft: '10px', color: '#666' }}>${food.price}</span>
                             {/* Removed Average Rating Display as requested */}
                          </div>

                          {/* Right Side: Rating Controls */}
                          <div className="food-rating-right">
                             {isRated ? (
                               <button disabled className="btn-disabled" style={{ backgroundColor: '#eee', color: '#888', border: '1px solid #ddd', padding: '5px 10px', cursor: 'not-allowed' }}>
                                 Rated ✅
                               </button>
                             ) : (
                               canRate ? (
                                 <div style={{ display: 'flex', gap: '5px' }}>
                                   <select 
                                      value={ratings[uniqueKey] || 5}
                                      onChange={(e) => handleRatingChange(uniqueKey, e.target.value)}
                                      style={{ padding: '5px', borderRadius: '4px' }}
                                   >
                                      {/* Generate options from 5 down to 0.5 in 0.5 steps */}
                                      {[5, 4.5, 4, 3.5, 3, 2.5, 2, 1.5, 1, 0.5].map(val => (
                                        <option key={val} value={val}>{val} ★</option>
                                      ))}
                                   </select>
                                   <button 
                                      onClick={() => handleSubmitRating(order.id, food.id)}
                                      className="create-button" // Reusing your green button style
                                      style={{ padding: '5px 10px', fontSize: '0.9rem' }}
                                   >
                                      Rate
                                   </button>
                                 </div>
                               ) : (
                                 // If order is pending
                                 <span style={{ fontSize: '0.85rem', color: '#999', fontStyle: 'italic' }}>
                                   {order.isFulfilled ? "" : "Wait for delivery"}
                                 </span>
                               )
                             )}
                          </div>

                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default Orders;