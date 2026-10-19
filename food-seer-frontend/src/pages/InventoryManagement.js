import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAllFoods, createFood, updateFood, deleteFood, getCurrentUser, getAllergens } from '../services/api';

const InventoryManagement = () => {
  const [foods, setFoods] = useState([]);
  // Shared, site-wide allergen list (same source Personal Diet and the
  // onboarding quiz read from) - so staff can tag food with any allergen a
  // customer has added, not just the original fixed set.
  const [allergenOptions, setAllergenOptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAddForm, setShowAddForm] = useState(false);
  const [editingFood, setEditingFood] = useState(null);
  const [formData, setFormData] = useState({
    foodName: '',
    amount: 0,
    price: 0,
    calories: '',
    allergies: []
  });
  const [currentUser, setCurrentUser] = useState(null);
  const navigate = useNavigate();

  const fetchFoods = async () => {
    try {
      const user = await getCurrentUser();
      setCurrentUser(user);

      // Check if user has staff or admin role
      if (user.role !== 'ROLE_ADMIN' && user.role !== 'ROLE_STAFF') {
        alert('Access denied. Staff or Admin privileges required.');
        navigate('/');
        return;
      }

      const foodsData = await getAllFoods();
      setFoods(foodsData);
    } catch (error) {
      console.error('Error fetching foods:', error);
      navigate('/');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFoods();
  }, []);

  useEffect(() => {
    getAllergens()
      .then(setAllergenOptions)
      .catch((error) => console.error('Error loading allergen list:', error));
  }, []);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const resetForm = () => {
    setFormData({
      foodName: '',
      amount: 0,
      price: 0,
      calories: '',
      allergies: []
    });
    setEditingFood(null);
    setShowAddForm(false);
  };

  const handleAllergyToggle = (allergen) => {
    setFormData(prev => ({
      ...prev,
      allergies: prev.allergies.includes(allergen)
        ? prev.allergies.filter(a => a !== allergen)
        : [...prev.allergies, allergen]
    }));
  };

  const handleAddFood = async (e) => {
    e.preventDefault();
    
    if (!formData.foodName.trim()) {
      alert('Food name is required');
      return;
    }

    try {
      const foodData = {
        foodName: formData.foodName.toUpperCase(),
        amount: parseInt(formData.amount),
        price: parseInt(formData.price),
        calories: formData.calories === '' ? null : parseInt(formData.calories),
        allergies: formData.allergies
      };

      await createFood(foodData);
      alert('Food added successfully!');
      resetForm();
      await fetchFoods();
    } catch (error) {
      console.error('Error adding food:', error);
      alert('Failed to add food. Please try again.');
    }
  };

  const handleEditFood = (food) => {
    setEditingFood(food);
    setFormData({
      foodName: food.foodName,
      amount: food.amount,
      price: food.price,
      calories: food.calories != null ? String(food.calories) : '',
      allergies: food.allergies || []
    });
    setShowAddForm(true);
  };

  const handleUpdateFood = async (e) => {
    e.preventDefault();

    if (!formData.foodName.trim()) {
      alert('Food name is required');
      return;
    }

    try {
      const foodData = {
        foodName: formData.foodName.toUpperCase(),
        amount: parseInt(formData.amount),
        price: parseInt(formData.price),
        calories: formData.calories === '' ? null : parseInt(formData.calories),
        allergies: formData.allergies
      };

      await updateFood(foodData);
      alert('Food updated successfully!');
      resetForm();
      await fetchFoods();
    } catch (error) {
      console.error('Error updating food:', error);
      alert('Failed to update food. Please try again.');
    }
  };

  const handleDeleteFood = async (foodId, foodName) => {
    if (!window.confirm(`Are you sure you want to delete ${foodName}?`)) {
      return;
    }

    try {
      await deleteFood(foodId);
      alert('Food deleted successfully!');
      await fetchFoods();
    } catch (error) {
      console.error('Error deleting food:', error);
      alert(error.message || 'Failed to delete food. Please try again.');
    }
  };

  const handleBack = () => {
    navigate('/order-management');
  };

  const handleDashboard = () => {
    navigate('/inventory-dashboard');
  };

  if (loading) {
    return (
      <div className="admin-dashboard-container">
        <div className="loading">Loading dashboard...</div>
      </div>
    );
  }

  return (
    <div className="admin-dashboard-container">
      <div className="dashboard-header">
        <h1>📦 Inventory Management</h1>
        <div className="header-actions">
          <button className="add-button" onClick={() => setShowAddForm(!showAddForm)}>
            {showAddForm ? 'Cancel' : '+ Add Food'}
          </button>

          {/* Admin-only Dashboard button */}
          {currentUser?.role === 'ROLE_ADMIN' && (
            <button
              className="primary-button dashboard-button"
              onClick={handleDashboard}
              style={{ marginLeft: '10px' }}
            >
              📊 Inventory Dashboard
            </button>
          )}

          <button className="back-button" onClick={handleBack}>
            Back
          </button>
        </div>
      </div>

      <div className="dashboard-stats">
        <div className="stat-card">
          <h3>Total Foods</h3>
          <p className="stat-number">{foods.length}</p>
        </div>
        <div className="stat-card">
          <h3>In Stock</h3>
          <p className="stat-number">{foods.filter(f => f.amount > 0).length}</p>
        </div>
        <div className="stat-card">
          <h3>Out of Stock</h3>
          <p className="stat-number">{foods.filter(f => f.amount === 0).length}</p>
        </div>
      </div>

      {showAddForm && (
        <div className="food-form-container">
          <h2>{editingFood ? 'Edit Food' : 'Add New Food'}</h2>
          <form onSubmit={editingFood ? handleUpdateFood : handleAddFood} className="food-form">
            <div className="form-group">
              <label htmlFor="foodName">Food Name *</label>
              <input
                type="text"
                id="foodName"
                name="foodName"
                value={formData.foodName}
                onChange={handleInputChange}
                required
                placeholder="Enter food name"
              />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="amount">Amount *</label>
                <input
                  type="number"
                  id="amount"
                  name="amount"
                  value={formData.amount}
                  onChange={handleInputChange}
                  required
                  min="0"
                  placeholder="Stock amount"
                />
              </div>

              <div className="form-group">
                <label htmlFor="price">Price ($) *</label>
                <input
                  type="number"
                  id="price"
                  name="price"
                  value={formData.price}
                  onChange={handleInputChange}
                  required
                  min="0"
                  placeholder="Price"
                />
              </div>

              <div className="form-group">
                <label htmlFor="calories">Calories</label>
                <input
                  type="number"
                  id="calories"
                  name="calories"
                  value={formData.calories}
                  onChange={handleInputChange}
                  min="0"
                  placeholder="Optional"
                />
              </div>
            </div>

            <div className="form-group">
              <label>Allergens (select all that apply)</label>
              <div className="allergens-grid">
                {allergenOptions.map(allergen => (
                  <label key={allergen} className="allergen-checkbox">
                    <input
                      type="checkbox"
                      checked={formData.allergies.includes(allergen)}
                      onChange={() => handleAllergyToggle(allergen)}
                    />
                    <span>{allergen}</span>
                  </label>
                ))}
              </div>
              {formData.allergies.length > 0 && (
                <div className="selected-allergies">
                  Selected: {formData.allergies.join(', ')}
                </div>
              )}
            </div>

            <div className="form-actions">
              <button type="button" className="cancel-button" onClick={resetForm}>
                Cancel
              </button>
              <button type="submit" className="submit-button">
                {editingFood ? 'Update Food' : 'Add Food'}
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="foods-table-container">
        <h2>Food Inventory</h2>
        {foods.length === 0 ? (
          <p>No foods in inventory. Add some to get started!</p>
        ) : (
          <table className="foods-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Price</th>
                <th>Calories</th>
                <th>Stock</th>
                <th>Rating</th>
                <th>Allergies</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {foods.map(food => (
                <tr key={food.id}>
                  <td>{food.id}</td>
                  <td>{food.foodName}</td>
                  <td>${food.price}</td>
                  <td>{food.calories != null ? `${food.calories} cal` : '—'}</td>
                  <td>
                    <span className={food.amount > 0 ? 'stock-positive' : 'stock-zero'}>
                      {food.amount}
                    </span>
                  </td>
                  <td>
                    {food.rating > 0 ? (
                        <span style={{ whiteSpace: 'nowrap' }}>
                            <span style={{ color: '#f1c40f' }}>⭐</span> {food.rating.toFixed(1)} 
                            <span style={{ fontSize: '0.85em', color: '#777', marginLeft: '4px' }}>
                                ({food.numberOfRatings})
                            </span>
                        </span>
                    ) : (
                        <span style={{ color: '#ccc', fontStyle: 'italic' }}>No ratings</span>
                    )}
                  </td>
                  <td>{food.allergies && food.allergies.length > 0 ? food.allergies.join(', ') : 'None'}</td>
                  <td className="actions-cell">
                    <button
                      className="edit-button"
                      onClick={() => handleEditFood(food)}
                    >
                      Edit
                    </button>
                    <button
                      className="delete-button"
                      onClick={() => handleDeleteFood(food.id, food.foodName)}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default InventoryManagement;
