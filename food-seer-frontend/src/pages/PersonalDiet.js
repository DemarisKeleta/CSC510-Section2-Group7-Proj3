import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  getCurrentUser,
  logout,
  getCalorieBudget,
  getFoodsWithinCalorieBudget,
  updateUserPreferences,
  logMeal
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
  const navigate = useNavigate();

  const loadAll = useCallback(async () => {
    try {
      const [userData, budgetData, recommendationData] = await Promise.all([
        getCurrentUser(),
        getCalorieBudget(),
        getFoodsWithinCalorieBudget()
      ]);
      setUser(userData);
      setBudget(budgetData);
      setGoalInput(budgetData.dailyCalorieGoal != null ? String(budgetData.dailyCalorieGoal) : '');
      setRecommendation(recommendationData);
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
        <h1 className="recommendations-title">🔥 Personal Diet</h1>
        <div className="header-actions">
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
            <span className="preference-label">🔥 Burned Today:</span>
            <span className="preference-value">
              {budget?.fitnessDataTracked ? `${budget.caloriesBurnedToday} cal` : 'Not tracked'}
            </span>
          </div>
          <div className="preference-item">
            <span className="preference-label">🍽️ Consumed Today:</span>
            <span className="preference-value">
              {budget?.caloriesConsumedToday ?? 0} cal (from today's orders)
            </span>
          </div>
          <div className="preference-item">
            <span className="preference-label">🎯 Remaining Budget:</span>
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

      <div className="recommendations-content">
        <h2>Menu Items That Fit Your Budget</h2>
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
        {recommendation?.budgetApplied && recommendation.foods.length === 0 && (
          <div className="no-recommendations">
            <p>No menu items fit your remaining calorie budget right now.</p>
          </div>
        )}
        {recommendation?.budgetApplied && recommendation.foods.length > 0 && (
          <div className="recommendations-grid">
            {recommendation.foods.map((food) => (
              <div key={food.id} className="recommendation-card">
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
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default PersonalDiet;
