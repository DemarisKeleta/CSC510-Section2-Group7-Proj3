import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  getCurrentUser,
  logout,
  getCalorieBudget,
  getFoodsWithinCalorieBudget,
  updateUserPreferences,
  logMeal,
  getAllergens,
  addAllergen,
  createOrder
} from '../services/api';

const MEAL_TYPES = [
  { value: 'BREAKFAST', label: 'Breakfast' },
  { value: 'LUNCH', label: 'Lunch' },
  { value: 'DINNER', label: 'Dinner' }
];

// Fully optional feature: a customer who never visits this page or never
// sets a goal here is completely unaffected elsewhere in the app.
const PersonalDiet = () => {
  const [user, setUser] = useState(null);
  const [budget, setBudget] = useState(null);
  const [goalInput, setGoalInput] = useState('');
  const [recommendation, setRecommendation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [mealType, setMealType] = useState('BREAKFAST');
  const [mealCalories, setMealCalories] = useState('');
  const [loggingMeal, setLoggingMeal] = useState(false);
  const [allergyInput, setAllergyInput] = useState([]);
  const [customAllergyInput, setCustomAllergyInput] = useState('');
  const [savingAllergies, setSavingAllergies] = useState(false);
  // Shared, site-wide allergen list - fetched fresh each load, and grows when
  // any customer adds a new one via handleAddCustomAllergy below.
  const [allergenOptions, setAllergenOptions] = useState([]);
  // Local cart: accumulates clicks across multiple food cards. Fully
  // self-contained on this page now - viewed and ordered from right here,
  // instead of handing off to CreateOrder.js (that hand-off used
  // location.state + a useEffect, which double-fired under React
  // StrictMode in development and silently doubled quantities).
  const [cart, setCart] = useState({});
  const [orderName, setOrderName] = useState('');
  const [placingOrder, setPlacingOrder] = useState(false);
  const navigate = useNavigate();

  const loadAll = useCallback(async () => {
    try {
      const [userData, budgetData, recommendationData, allergenList] = await Promise.all([
        getCurrentUser(),
        getCalorieBudget(),
        getFoodsWithinCalorieBudget(),
        getAllergens()
      ]);
      setUser(userData);
      setBudget(budgetData);
      setGoalInput(budgetData.dailyCalorieGoal != null ? String(budgetData.dailyCalorieGoal) : '');
      setRecommendation(recommendationData);
      setAllergenOptions(allergenList);
      // Parse the existing comma-separated dietaryRestrictions string into an array for the checkboxes below.
      const existingAllergies = userData.dietaryRestrictions
        ? userData.dietaryRestrictions.split(',').map((a) => a.trim().toUpperCase()).filter((a) => a.length > 0)
        : [];
      setAllergyInput(existingAllergies);
    } catch (error) {
      console.error('Error loading personal diet data:', error);
      navigate('/');
    } finally {
      setLoading(false);
    }
  }, [navigate]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const handleSaveGoal = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const newGoal = goalInput === '' ? null : parseInt(goalInput, 10);
      // Pass through the user's existing cost/dietary preferences unchanged -
      // this endpoint is shared with the onboarding quiz, and omitting them
      // here would overwrite those fields with blank values.
      await updateUserPreferences(user.costPreference, user.dietaryRestrictions, newGoal);
      await loadAll();
    } catch (error) {
      console.error('Error saving calorie goal:', error);
      alert('Failed to save your calorie goal. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  const handleLogMeal = async (e) => {
    e.preventDefault();
    if (mealCalories === '') return;
    setLoggingMeal(true);
    try {
      const updatedBudget = await logMeal(mealType, parseInt(mealCalories, 10));
      setBudget(updatedBudget);
      setMealCalories('');
      // Remaining budget changed, so the recommended-foods list needs refreshing too.
      const recommendationData = await getFoodsWithinCalorieBudget();
      setRecommendation(recommendationData);
    } catch (error) {
      console.error('Error logging meal:', error);
      alert('Failed to log meal. Please try again.');
    } finally {
      setLoggingMeal(false);
    }
  };

  const handleToggleAllergy = (allergen) => {
    setAllergyInput((prev) =>
      prev.includes(allergen) ? prev.filter((a) => a !== allergen) : [...prev, allergen]
    );
  };

  const handleAddCustomAllergy = async (e) => {
    e.preventDefault();
    const value = customAllergyInput.trim().toUpperCase();
    if (value === '') return;
    setCustomAllergyInput('');
    if (!allergyInput.includes(value)) {
      setAllergyInput((prev) => [...prev, value]);
    }
    try {
      // Persists this to the shared, site-wide list - from now on it shows
      // up as a regular checkbox here (for every customer) and in the
      // onboarding quiz and staff's Add Food form too, not just a one-off
      // entry for this customer.
      const updatedList = await addAllergen(value);
      setAllergenOptions(updatedList);
    } catch (error) {
      console.error('Error adding allergen to the shared list:', error);
      // Keep the customer's own selection even if the shared-list save failed -
      // handleSaveAllergies below will still persist it on their own profile.
    }
  };

  const handleRemoveAllergy = (allergen) => {
    setAllergyInput((prev) => prev.filter((a) => a !== allergen));
  };

  // Anything the customer has selected that isn't (yet) in the shared list -
  // should be rare/brief, since handleAddCustomAllergy above adds it to the
  // shared list right away, but this covers the moment in between or a failed save.
  const customAllergies = allergyInput.filter((a) => !allergenOptions.includes(a));

  const handleSaveAllergies = async (e) => {
    e.preventDefault();
    setSavingAllergies(true);
    try {
      const dietaryRestrictions = allergyInput.join(', ');
      // Pass through the user's existing cost/goal preferences unchanged -
      // this endpoint is shared with the onboarding quiz and the goal form
      // above, and omitting them here would overwrite those fields with blank values.
      await updateUserPreferences(user.costPreference, dietaryRestrictions, user.dailyCalorieGoal);
      await loadAll();
    } catch (error) {
      console.error('Error saving allergies:', error);
      alert('Failed to save your allergies. Please try again.');
    } finally {
      setSavingAllergies(false);
    }
  };

  // Calorie filtering already happened server-side (recommendation.foods); layer
  // allergy filtering on top here, client-side, same approach Recommendations.js
  // already uses for its own cost/allergy filtering.
  const allergyFreeFoods = (recommendation?.foods || []).filter((food) => {
    if (allergyInput.length === 0) return true;
    if (!food.allergies || food.allergies.length === 0) return true;
    const foodAllergies = food.allergies.map((a) => a.toLowerCase());
    const userRestrictions = allergyInput.map((a) => a.toLowerCase());
    return !userRestrictions.some((restriction) => foodAllergies.some((allergy) => allergy === restriction));
  });
  const excludedForAllergies = (recommendation?.foods?.length || 0) - allergyFreeFoods.length;

  const handleAddToCart = (food) => {
    setCart((prev) => {
      const currentQuantity = prev[food.id]?.quantity || 0;
      // never let the cart quantity exceed what's actually in stock, even if
      // this is called twice in a row (e.g. clicking both the card and the button)
      if (currentQuantity >= food.amount) {
        return prev;
      }
      return { ...prev, [food.id]: { food, quantity: currentQuantity + 1 } };
    });
  };

  const handleIncrementCartItem = (food) => {
    setCart((prev) => {
      const currentQuantity = prev[food.id]?.quantity || 0;
      // same stock cap as handleAddToCart above
      if (currentQuantity >= food.amount) {
        return prev;
      }
      return { ...prev, [food.id]: { food, quantity: currentQuantity + 1 } };
    });
  };

  const handleDecrementCartItem = (foodId) => {
    setCart((prev) => {
      const next = { ...prev };
      if (!next[foodId]) return next;
      if (next[foodId].quantity > 1) {
        next[foodId] = { ...next[foodId], quantity: next[foodId].quantity - 1 };
      } else {
        delete next[foodId];
      }
      return next;
    });
  };

  const handleClearCart = () => setCart({});

  const cartItemCount = Object.values(cart).reduce((total, item) => total + item.quantity, 0);

  const getCartItemsTotal = () =>
    Object.values(cart).reduce((total, item) => total + item.food.price * item.quantity, 0);

  const getCartDeliveryCost = () => parseFloat((0.3 * getCartItemsTotal()).toFixed(2));

  const getCartTotalPrice = () => parseFloat((getCartItemsTotal() + getCartDeliveryCost()).toFixed(2));

  // Each food card is already filtered to fit the remaining budget on its
  // own, but several of those individually-fitting items can still add up
  // to more than the remaining budget once combined in one cart - this
  // checks the cart's running total against it.
  const getCartTotalCalories = () =>
    Object.values(cart).reduce((total, item) => total + (item.food.calories || 0) * item.quantity, 0);

  const cartOverCalorieBudget =
    budget?.remainingCalories != null && getCartTotalCalories() > budget.remainingCalories;

  const handlePlaceOrder = async () => {
    if (cartItemCount === 0) {
      alert('Please add items to your cart before placing an order.');
      return;
    }
    if (!orderName.trim()) {
      alert('Please enter a name for your order.');
      return;
    }
    setPlacingOrder(true);
    try {
      const orderFoods = Object.values(cart).flatMap((item) =>
        Array(item.quantity).fill({ id: item.food.id })
      );
      const orderData = {
        name: orderName,
        foods: orderFoods,
        isFulfilled: false,
        cost: getCartTotalPrice(),
        status: 'Placed',
        deliveryCost: getCartDeliveryCost()
      };
      await createOrder(orderData);
      alert('Order placed successfully!');
      setCart({});
      setOrderName('');
      // The order's foods count toward today's consumption - refresh the budget and recommendation.
      await loadAll();
    } catch (error) {
      console.error('Error placing order:', error);
      alert('Failed to place order. Please try again.');
    } finally {
      setPlacingOrder(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  if (loading) {
    return (
      <div className="recommendations-container">
        <div className="loading">Loading...</div>
      </div>
    );
  }

  return (
    <div className="recommendations-container">
      <div className="recommendations-header">
        <h1 className="recommendations-title">Personal Diet</h1>
        <div className="header-actions">
          {/* takes the customer to the per-day calorie consumption history page */}
          <button
            className="nav-button"
            // navigate (not a page reload) to the new history route
            onClick={() => navigate('/calorie-history')}
          >
            History
          </button>
          {cartItemCount > 0 && (
            <button
              className="nav-button"
              onClick={() => document.getElementById('personal-diet-cart')?.scrollIntoView({ behavior: 'smooth' })}
            >
              View Cart ({cartItemCount})
            </button>
          )}
          <button className="logout-button" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </div>

      <div className="user-info-card">
        <h2>Set your daily calorie goal</h2>
        <p style={{ color: '#666', marginBottom: '16px' }}>
          Optional - used alongside your fitness activity to recommend menu items that fit your remaining budget for today. Leave blank to turn this off.
        </p>
        <form onSubmit={handleSaveGoal} style={{ display: 'flex', gap: '10px', justifyContent: 'center', flexWrap: 'wrap' }}>
          <input
            type="number"
            min="0"
            placeholder="e.g. 2000"
            value={goalInput}
            onChange={(e) => setGoalInput(e.target.value)}
            className="form-input"
            style={{ maxWidth: '200px', textAlign: 'center' }}
          />
          <button type="submit" className="update-preferences-button" disabled={saving}>
            {saving ? 'Saving...' : 'Save Goal'}
          </button>
        </form>

        <div className="preferences-summary" style={{ marginTop: '20px' }}>
          <div className="preference-item">
            <span className="preference-label">Burned Today:</span>
            <span className="preference-value">
              {budget?.fitnessDataTracked ? `${budget.caloriesBurnedToday} cal` : 'Not tracked'}
            </span>
          </div>
          <div className="preference-item">
            <span className="preference-label">Consumed Today:</span>
            <span className="preference-value">
              {budget?.caloriesConsumedToday ?? 0} cal (from today's orders)
            </span>
          </div>
          <div className="preference-item">
            <span className="preference-label">Remaining Budget:</span>
            <span className="preference-value">
              {!budget?.goalSet
                ? 'No daily goal set'
                : !budget?.fitnessDataTracked
                ? 'Fitness activity not tracked'
                : `${budget.remainingCalories} cal`}
            </span>
          </div>
        </div>
      </div>

      <div className="user-info-card">
        <h2>Log a meal</h2>
        <p style={{ color: '#666', marginBottom: '16px' }}>
          Log what you've eaten today, even if it wasn't ordered through FoodSeer. The first meal you log gets your full budget; each meal logged after that is reduced by what you've already logged.
        </p>
        <form onSubmit={handleLogMeal} style={{ display: 'flex', gap: '10px', justifyContent: 'center', flexWrap: 'wrap' }}>
          <select
            value={mealType}
            onChange={(e) => setMealType(e.target.value)}
            className="form-input"
            style={{ maxWidth: '160px' }}
          >
            {MEAL_TYPES.map((m) => (
              <option key={m.value} value={m.value}>{m.label}</option>
            ))}
          </select>
          <input
            type="number"
            min="0"
            placeholder="Calories eaten"
            value={mealCalories}
            onChange={(e) => setMealCalories(e.target.value)}
            className="form-input"
            style={{ maxWidth: '160px', textAlign: 'center' }}
          />
          <button type="submit" className="update-preferences-button" disabled={loggingMeal}>
            {loggingMeal ? 'Logging...' : 'Log Meal'}
          </button>
        </form>

        <div className="preferences-summary" style={{ marginTop: '20px' }}>
          {MEAL_TYPES.map((m) => {
            const entry = budget?.loggedMealsToday?.find((e) => e.mealType === m.value);
            return (
              <div className="preference-item" key={m.value}>
                <span className="preference-label">{m.label}:</span>
                <span className="preference-value">
                  {entry ? `${entry.calories} cal` : 'Not logged yet'}
                </span>
              </div>
            );
          })}
        </div>
      </div>

      <div className="user-info-card">
        <h2>Your allergies</h2>
        <p style={{ color: '#666', marginBottom: '16px' }}>
          Select anything you're allergic to. Menu items below are filtered to exclude these, on top of your calorie budget.
        </p>
        <form onSubmit={handleSaveAllergies}>
          <div className="allergens-grid">
            {allergenOptions.map((allergen) => (
              <label key={allergen} className="allergen-checkbox">
                <input
                  type="checkbox"
                  checked={allergyInput.includes(allergen)}
                  onChange={() => handleToggleAllergy(allergen)}
                />
                <span>{allergen}</span>
              </label>
            ))}
          </div>

          <p style={{ color: '#666', margin: '16px 0 8px' }}>
            Don't see your allergy above? Add it here:
          </p>
          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            <input
              type="text"
              placeholder="e.g. Cilantro"
              value={customAllergyInput}
              onChange={(e) => setCustomAllergyInput(e.target.value)}
              onKeyDown={(e) => {
                // Without this, Enter here would submit the whole form (Save
                // Allergies) instead of adding this one custom entry, since
                // it's the only text input in the form.
                if (e.key === 'Enter') {
                  e.preventDefault();
                  handleAddCustomAllergy(e);
                }
              }}
              className="form-input"
              style={{ maxWidth: '220px' }}
            />
            <button type="button" className="update-preferences-button" onClick={handleAddCustomAllergy}>
              Add
            </button>
          </div>

          {customAllergies.length > 0 && (
            <div style={{ marginTop: '12px', display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
              {customAllergies.map((allergen) => (
                <span
                  key={allergen}
                  style={{
                    background: '#fdecea',
                    color: '#c0392b',
                    borderRadius: '16px',
                    padding: '4px 10px',
                    fontSize: '0.9em',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '6px'
                  }}
                >
                  {allergen}
                  <button
                    type="button"
                    onClick={() => handleRemoveAllergy(allergen)}
                    aria-label={`Remove ${allergen}`}
                    style={{ background: 'none', border: 'none', color: '#c0392b', cursor: 'pointer', fontWeight: 'bold' }}
                  >
                    ✕
                  </button>
                </span>
              ))}
            </div>
          )}

          {allergyInput.length > 0 && (
            <div className="selected-allergies" style={{ marginTop: '12px' }}>
              <strong>Selected:</strong> {allergyInput.join(', ')}
            </div>
          )}
          <button type="submit" className="update-preferences-button" style={{ marginTop: '16px' }} disabled={savingAllergies}>
            {savingAllergies ? 'Saving...' : 'Save Allergies'}
          </button>
        </form>
      </div>

      <div className="user-info-card" id="personal-diet-cart">
        <h2>Your Cart</h2>
        {cartItemCount === 0 ? (
          <p style={{ color: '#666' }}>Your cart is empty. Click "Add to Cart" on any item below.</p>
        ) : (
          <>
            <div style={{ marginBottom: '16px' }}>
              {Object.values(cart).map((item) => (
                <div
                  key={item.food.id}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '10px 0',
                    borderBottom: '1px solid #eee'
                  }}
                >
                  <div style={{ textAlign: 'left' }}>
                    <strong>{item.food.foodName}</strong>
                    <div style={{ color: '#666', fontSize: '0.9em' }}>
                      ${item.food.price} × {item.quantity} = ${(item.food.price * item.quantity).toFixed(2)}
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <button
                      type="button"
                      className="quantity-button"
                      onClick={() => handleDecrementCartItem(item.food.id)}
                    >
                      -
                    </button>
                    <span>{item.quantity}</span>
                    <button
                      type="button"
                      className="quantity-button"
                      onClick={() => handleIncrementCartItem(item.food)}
                      disabled={item.quantity >= item.food.amount}
                    >
                      +
                    </button>
                  </div>
                </div>
              ))}
            </div>

            {cartOverCalorieBudget && (
              <div
                style={{
                  background: '#fdecea',
                  color: '#c0392b',
                  border: '1px solid #f5c6cb',
                  borderRadius: '8px',
                  padding: '12px 16px',
                  marginBottom: '16px',
                  fontWeight: 'bold'
                }}
              >
                ⚠️ This cart is {getCartTotalCalories() - budget.remainingCalories} calories over your remaining budget
                ({getCartTotalCalories()} cal in cart vs. {budget.remainingCalories} cal remaining).
              </div>
            )}

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'center', flexWrap: 'wrap', marginBottom: '16px' }}>
              <input
                type="text"
                placeholder="Order name, e.g. Lunch Order"
                value={orderName}
                onChange={(e) => setOrderName(e.target.value)}
                className="form-input"
                style={{ maxWidth: '240px' }}
              />
            </div>

            <div className="preferences-summary" style={{ marginBottom: '16px' }}>
              <div className="preference-item">
                <span className="preference-label">Total Items:</span>
                <span className="preference-value">{cartItemCount}</span>
              </div>
              <div className="preference-item">
                <span className="preference-label">Total Calories:</span>
                <span className="preference-value" style={{ color: cartOverCalorieBudget ? '#c0392b' : undefined }}>
                  {getCartTotalCalories()} cal
                  {budget?.remainingCalories != null && ` / ${budget.remainingCalories} cal budget`}
                </span>
              </div>
              <div className="preference-item">
                <span className="preference-label">Delivery fee:</span>
                <span className="preference-value">${getCartDeliveryCost()}</span>
              </div>
              <div className="preference-item">
                <span className="preference-label">Total Price:</span>
                <span className="preference-value">${getCartTotalPrice()}</span>
              </div>
            </div>

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'center' }}>
              <button type="button" className="cancel-button" onClick={handleClearCart} disabled={placingOrder}>
                Clear Cart
              </button>
              <button
                type="button"
                className="update-preferences-button"
                onClick={handlePlaceOrder}
                disabled={placingOrder}
              >
                {placingOrder ? 'Placing Order...' : 'Place Order'}
              </button>
            </div>
            {!orderName.trim() && (
              <p style={{ color: '#c0392b', marginTop: '8px', fontSize: '0.9em' }}>
                Enter an order name above before placing your order.
              </p>
            )}
          </>
        )}
      </div>

      <div className="recommendations-content">
        <h2>Menu Items That Fit Your Budget and Allergies</h2>
        {!recommendation?.budgetApplied && (
          <div className="no-recommendations">
            <p>Set a daily calorie goal above to see menu items filtered to your remaining budget.</p>
            <p>Until then, this page won't affect anything else in FoodSeer - it's entirely optional.</p>
          </div>
        )}
        {recommendation?.budgetApplied && recommendation.excludedForMissingCalories > 0 && (
          <p className="no-recommendations">
            {recommendation.excludedForMissingCalories} menu item(s) have no recorded calorie value and are excluded from this list.
          </p>
        )}
        {excludedForAllergies > 0 && (
          <p className="no-recommendations">
            {excludedForAllergies} menu item(s) are excluded for matching one of your selected allergies.
          </p>
        )}
        {recommendation?.budgetApplied && allergyFreeFoods.length === 0 && (
          <div className="no-recommendations">
            <p>No menu items fit your remaining calorie budget and allergies right now.</p>
          </div>
        )}
        {recommendation?.budgetApplied && allergyFreeFoods.length > 0 && (
          <div className="recommendations-grid">
            {allergyFreeFoods.map((food) => (
              <div
                key={food.id}
                className="recommendation-card"
                onClick={() => {
                  if (food.amount > 0) handleAddToCart(food);
                }}
                style={{ cursor: food.amount > 0 ? 'pointer' : 'not-allowed', opacity: food.amount > 0 ? 1 : 0.6 }}
                title={food.amount > 0 ? 'Click to add to cart' : 'Out of stock'}
              >
                <div className="recommendation-icon">🍽️</div>
                <h3>{food.foodName}</h3>
                <div className="food-details">
                  <p><strong>Calories:</strong> {food.calories}</p>
                  <p><strong>Price:</strong> ${food.price}</p>
                  <p><strong>Available:</strong> {food.amount > 0 ? `${food.amount} units` : 'Out of stock'}</p>
                </div>
                <div className="recommendation-match">
                  {food.amount > 0 ? '✓ Available' : '✗ Out of Stock'}
                </div>
                {cart[food.id]?.quantity > 0 && (
                  <p style={{ color: '#2ecc71', fontWeight: 'bold', margin: '6px 0 0' }}>
                    {cart[food.id].quantity} in cart
                  </p>
                )}
                {food.amount > 0 && (
                  <button
                    type="button"
                    className="update-preferences-button"
                    style={{ marginTop: '10px', width: '100%' }}
                    onClick={(e) => {
                      e.stopPropagation();
                      handleAddToCart(food);
                    }}
                  >
                    Add to Cart
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default PersonalDiet;
