import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { getAllFoods, createOrder, getCurrentUser } from '../services/api';
import PantryPairing from '../components/PantryPairing';

const CreateOrder = () => {
  const [foods, setFoods] = useState([]);
  const [restrictedAllergens, setRestrictedAllergens] = useState([]);
  const [cart, setCart] = useState({});
  const [orderName, setOrderName] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [notification, setNotification] = useState(null);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const fetchFoods = async () => {
      try {
        const user = await getCurrentUser();

	const restrictions = (user.dietaryRestrictions || '')
		.split(',')
		.map(item => item.trim().toUpperCase())
		.filter(Boolean);

	setRestrictedAllergens(restrictions);

	const foodsData = await getAllFoods();
        setFoods(foodsData.filter(food => food.amount > 0));
      } catch (error) {
        console.error('Error fetching foods:', error);
        navigate('/');
      } finally {
        setLoading(false);
      }
    };

    fetchFoods();
  }, [navigate]);

  // Handle a single food item handed off from the chatbot's recommendation.
  useEffect(() => {
    if (location.state?.addToCart && foods.length > 0) {
      const recommendedFood = location.state.addToCart;
      const foodInStock = foods.find(f => f.id === recommendedFood.id);

      if (foodInStock) {
        setCart(prev => ({
          ...prev,
          [foodInStock.id]: {
            food: foodInStock,
            quantity: (prev[foodInStock.id]?.quantity || 0) + 1
          }
        }));
        setNotification(`✅ ${foodInStock.foodName} has been added to your cart!`);
        setTimeout(() => setNotification(null), 5000);
      } else {
        setNotification(`⚠️ Sorry, ${recommendedFood.foodName} is currently out of stock.`);
        setTimeout(() => setNotification(null), 5000);
      }
      window.history.replaceState({}, document.title);
    }
  }, [location.state, foods]);

  // Handle a whole cart handed off from Personal Diet - a customer there can
  // click "Add to Cart" on several items before coming here, so this applies
  // all of them at once instead of just the one addToCart handles above.
  useEffect(() => {
    if (location.state?.addMultipleToCart && foods.length > 0) {
      const items = location.state.addMultipleToCart;
      const addedNames = [];
      const outOfStockNames = [];

      setCart(prev => {
        const next = { ...prev };
        for (const item of items) {
          const foodInStock = foods.find(f => f.id === item.food.id);
          if (foodInStock) {
            next[foodInStock.id] = {
              food: foodInStock,
              quantity: (next[foodInStock.id]?.quantity || 0) + item.quantity
            };
            addedNames.push(foodInStock.foodName);
          } else {
            outOfStockNames.push(item.food.foodName);
          }
        }
        return next;
      });

      if (addedNames.length > 0) {
        setNotification(`✅ ${addedNames.join(', ')} added to your cart!`);
        setTimeout(() => setNotification(null), 5000);
      } else if (outOfStockNames.length > 0) {
        setNotification(`⚠️ Sorry, ${outOfStockNames.join(', ')} ${outOfStockNames.length > 1 ? 'are' : 'is'} currently out of stock.`);
        setTimeout(() => setNotification(null), 5000);
      }
      window.history.replaceState({}, document.title);
    }
  }, [location.state, foods]);

  const addToCart = (food) => {
    setCart(prev => ({
      ...prev,
      [food.id]: { food, quantity: (prev[food.id]?.quantity || 0) + 1 }
    }));
  };

  const removeFromCart = (foodId) => {
    setCart(prev => {
      const newCart = { ...prev };
      if (newCart[foodId].quantity > 1) {
        newCart[foodId].quantity -= 1;
      } else {
        delete newCart[foodId];
      }
      return newCart;
    });
  };

  const clearCart = () => { setCart({}); };

  const getDeliveryCost = () => {
    const itemsTotal = Object.values(cart).reduce((total, item) => total + (item.food.price * item.quantity), 0);
    return parseFloat((0.3 * itemsTotal).toFixed(2));
  }

  const getTotalPrice = () => {
    const itemsTotal = Object.values(cart).reduce((total, item) => total + (item.food.price * item.quantity), 0);
    const delivery = getDeliveryCost();
    return parseFloat((itemsTotal + delivery).toFixed(2));
  };

  const getTotalItems = () => {
    return Object.values(cart).reduce((total, item) => total + item.quantity, 0);
  };

  const handleSubmitOrder = async () => {
    if (getTotalItems() === 0) {
      alert('Please add items to your cart before placing an order.');
      return;
    }
    if (!orderName.trim()) {
      alert('Please enter a name for your order.');
      return;
    }

    setSubmitting(true);
    try {
      const orderFoods = Object.values(cart).flatMap(item => 
        Array(item.quantity).fill({ id: item.food.id })
      );
      const orderData = { name: orderName, foods: orderFoods, isFulfilled: false, cost: getTotalPrice(), status: 'Placed', deliveryCost: getDeliveryCost() };
      console.log('Submitting order data:', orderData);
      await createOrder(orderData);
      alert('Order placed successfully!');
      navigate('/orders');
    } catch (error) {
      console.error('Error creating order:', error);
      alert('Failed to create order. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const filteredFoods = foods.filter(food =>
    food.foodName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  // --- HELPER: Render Stars ---
  const renderStars = (rating) => {
    return (
      <span style={{ marginRight: '5px' }}>
        {[1, 2, 3, 4, 5].map(star => (
          <span key={star} style={{ color: rating >= star ? '#f1c40f' : '#e0e0e0', fontSize: '1rem' }}>
            ★
          </span>
        ))}
      </span>
    );
  };

  if (loading) return <div className="create-order-container"><div className="loading">Loading...</div></div>;

  return (
    <div className="create-order-container">
      <div className="create-order-header">
        <h1>🛒 Create New Order</h1>
        <button className="back-button" onClick={() => navigate('/recommendations')}>Back</button>
      </div>

      {notification && <div className="notification-banner">{notification}</div>}

      <div className="order-content">
        <div className="foods-section">
          <h2>Available Foods</h2>
          <div className="search-box">
            <input
              type="text"
              placeholder="Search foods..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="search-input"
            />
          </div>

          {filteredFoods.length === 0 ? (
            <p>No foods available for ordering.</p>
          ) : (
            <div className="foods-list">
              {filteredFoods.map(food => (
                <div key={food.id} className="food-item">
                  <div className="food-item-info" style={{ width: '100%' }}>
                    
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '5px' }}>
                        <h3 style={{ margin: 0 }}>{food.foodName}</h3>
                        <span style={{ fontWeight: 'bold', color: '#2ecc71', fontSize: '1.1rem', backgroundColor: '#e8f8f5', padding: '2px 8px', borderRadius: '4px' }}>
                            ${food.price}
                        </span>
                    </div>

                    {/* CORRECTED RATING ROW */}
                    <div className="food-rating-row" style={{ marginBottom: '8px', fontSize: '0.9rem', display: 'flex', alignItems: 'center' }}>
                         {food.rating > 0 ? (
                             <>
                                {renderStars(food.rating)}
                                <strong style={{ marginRight: '5px' }}>{food.rating.toFixed(1)}</strong>
                                <span style={{ color: '#7f8c8d', fontSize: '0.85rem' }}>({food.numberOfRatings})</span>
                             </>
                         ) : (
                             <span style={{ color: '#95a5a6', fontStyle: 'italic' }}>No ratings yet</span>
                         )}
                    </div>

                    <div style={{ fontSize: '0.85rem', color: '#666' }}>
                        <p style={{ margin: '2px 0' }}>Stock: {food.amount} available</p>
                        {food.allergies && food.allergies.length > 0 && (
                            <p className="allergies-text" style={{ margin: '2px 0', color: '#e74c3c' }}>
                                Allergies: {food.allergies.join(', ')}
                            </p>
                        )}
                    </div>
                  </div>
                  
                  <button
                    className="add-button"
                    onClick={() => addToCart(food)}
                    disabled={cart[food.id]?.quantity >= food.amount}
                    style={{ marginTop: '10px', width: '100%' }}
                  >
                    Add to Cart
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="cart-section">
          <h2>Your Cart</h2>
          <div className="order-name-input">
            <label htmlFor="orderName">Order Name:</label>
            <input type="text" id="orderName" placeholder="e.g., Lunch Order" value={orderName} onChange={(e) => setOrderName(e.target.value)} className="text-input" />
          </div>

          {getTotalItems() === 0 ? (
            <p className="empty-cart">Your cart is empty</p>
          ) : (
            <>
              <div className="cart-items">
                {Object.values(cart).map(item => (
                  <div key={item.food.id} className="cart-item">
                    <div className="cart-item-info">
                      <h4>{item.food.foodName}</h4>
                      <p>${item.food.price} × {item.quantity} = ${item.food.price * item.quantity}</p>
                    </div>
                    <div className="cart-item-actions">
                      <button className="quantity-button" onClick={() => removeFromCart(item.food.id)}>-</button>
                      <span className="quantity">{item.quantity}</span>
                      <button className="quantity-button" onClick={() => addToCart(item.food)} disabled={item.quantity >= item.food.amount}>+</button>
                    </div>
                  </div>
                ))}
              </div>
              <div className="cart-summary">
                <div className="summary-row"><span>Total Items:</span><span>{getTotalItems()}</span></div>
                <div className="summary-row"><span>Delivery fee:</span><span>{getDeliveryCost()}</span></div>
                <div className="summary-row total"><span>Total Price:</span><span>${getTotalPrice()}</span></div>
              </div>
              <div className="cart-actions">
                <button className="clear-button" onClick={clearCart} disabled={submitting}>Clear Cart</button>
                <button className="submit-button" onClick={handleSubmitOrder} disabled={submitting || !orderName.trim()}>
                  {submitting ? 'Placing Order...' : 'Place Order'}
                </button>
              </div>
            </>
          )}
        </div>
      </div>
      <PantryPairing
	cart={cart}
	restrictedAllergens={restrictedAllergens}
/>
    </div>
  );
};

export default CreateOrder;
