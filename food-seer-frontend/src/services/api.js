const API_BASE_URL = 'http://localhost:8080';

// Helper function to get token from localStorage
const getAuthToken = () => {
  return localStorage.getItem('token');
};

// Helper function to create headers
const createHeaders = (includeAuth = true) => {
  const headers = {
    'Content-Type': 'application/json',
  };
  
  if (includeAuth) {
    const token = getAuthToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }
  
  return headers;
};

// Auth API calls
export const login = async (username, password) => {
  try {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: createHeaders(false),
      body: JSON.stringify({ username, password }),
    });
    
    if (!response.ok) {
      throw new Error('Login failed');
    }
    
    const data = await response.json();
    
    // Store token in localStorage
    if (data.accessToken) {
      localStorage.setItem('token', data.accessToken);
      localStorage.setItem('username', username);
    }
    
    return data;
  } catch (error) {
    console.error('Login error:', error);
    throw error;
  }
};

export const register = async (username, email, password, role) => {
  try {
      const response = await fetch(`${API_BASE_URL}/auth/register`, {
        method: 'POST',
        headers: createHeaders(false),
        body: JSON.stringify({ username, email, password, role }),
      });
      if (!response.ok) {
        const data = await response.json();
        throw new Error(data.error);
      }
      return await response.json();
   } catch (error) {
      console.error('Registration error:', error);
      throw error;
   }
};

export const logout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('username');
};

// User API calls
export const getCurrentUser = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/users/me`, {
      method: 'GET',
      headers: createHeaders(true),
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch user');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Get current user error:', error);
    throw error;
  }
};

export const updateUserPreferences = async (costPreference, dietaryRestrictions, dailyCalorieGoal = null) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/users/me/preferences`, {
      method: 'PUT',
      headers: createHeaders(true),
      body: JSON.stringify({ costPreference, dietaryRestrictions, dailyCalorieGoal }),
    });

    if (!response.ok) {
      throw new Error('Failed to update preferences');
    }

    return await response.json();
  } catch (error) {
    console.error('Update preferences error:', error);
    throw error;
  }
};

export const getCalorieBudget = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/users/me/calorieBudget`, {
      method: 'GET',
      headers: createHeaders(true),
    });

    if (!response.ok) {
      throw new Error('Failed to fetch calorie budget');
    }

    return await response.json();
  } catch (error) {
    console.error('Get calorie budget error:', error);
    throw error;
  }
};

// fetches the signed-in user's per-day calorie consumption history, most recent day first
export const getCalorieHistory = async () => {
  // network/parsing errors are caught below and re-thrown after logging
  try {
    // GET the history endpoint, attaching the auth token via createHeaders(true)
    const response = await fetch(`${API_BASE_URL}/api/users/me/calorieHistory`, {
      // this is a read-only lookup
      method: 'GET',
      // true = include the Authorization header, since this endpoint requires login
      headers: createHeaders(true),
    });

    // a non-2xx status means the request failed server-side
    if (!response.ok) {
      // surface a readable error to the caller instead of an unhandled rejection
      throw new Error('Failed to fetch calorie history');
    }

    // parse and return the JSON array of { date, caloriesConsumed } rows
    return await response.json();
  } catch (error) {
    // log for debugging, then...
    console.error('Get calorie history error:', error);
    // ...let the caller (the History page) handle showing an error state
    throw error;
  }
};

export const getAllergens = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/allergens`, {
      method: 'GET',
      headers: createHeaders(true),
    });

    if (!response.ok) {
      throw new Error('Failed to fetch allergens');
    }

    return await response.json();
  } catch (error) {
    console.error('Get allergens error:', error);
    throw error;
  }
};

export const addAllergen = async (name) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/allergens`, {
      method: 'POST',
      headers: createHeaders(true),
      body: JSON.stringify({ name }),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || 'Failed to add allergen');
    }

    return await response.json();
  } catch (error) {
    console.error('Add allergen error:', error);
    throw error;
  }
};

export const logMeal = async (mealType, calories) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/users/me/mealLog`, {
      method: 'POST',
      headers: createHeaders(true),
      body: JSON.stringify({ mealType, calories }),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || 'Failed to log meal');
    }

    return await response.json();
  } catch (error) {
    console.error('Log meal error:', error);
    throw error;
  }
};

// Check if user is authenticated
export const isAuthenticated = () => {
  return !!getAuthToken();
};

// Food API calls
export const getAllFoods = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/foods`, {
      method: 'GET',
      headers: createHeaders(true),
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch foods');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Get all foods error:', error);
    throw error;
  }
};

export const getFoodsWithinCalorieBudget = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/foods/withinCalorieBudget`, {
      method: 'GET',
      headers: createHeaders(true),
    });

    if (!response.ok) {
      throw new Error('Failed to fetch calorie-budget-aware foods');
    }

    return await response.json();
  } catch (error) {
    console.error('Get foods within calorie budget error:', error);
    throw error;
  }
};

export const getFoodById = async (id) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/foods/${id}`, {
      method: 'GET',
      headers: createHeaders(true),
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch food');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Get food by id error:', error);
    throw error;
  }
};

export const createFood = async (foodData) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/foods`, {
      method: 'POST',
      headers: createHeaders(true),
      body: JSON.stringify(foodData),
    });
    
    if (!response.ok) {
      throw new Error('Failed to create food');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Create food error:', error);
    throw error;
  }
};

export const updateFood = async (foodData) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/foods/updateFood`, {
      method: 'POST',
      headers: createHeaders(true),
      body: JSON.stringify(foodData),
    });
    
    if (!response.ok) {
      throw new Error('Failed to update food');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Update food error:', error);
    throw error;
  }
};

export const deleteFood = async (id) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/foods/${id}`, {
      method: 'DELETE',
      headers: createHeaders(true),
    });
    
    if (!response.ok) {
      const errorText = await response.text();
      if (response.status === 409) {
        // Conflict: food is part of unfulfilled orders
        throw new Error(errorText);
      }
      throw new Error('Failed to delete food');
    }
    
    return await response.text();
  } catch (error) {
    console.error('Delete food error:', error);
    throw error;
  }
};

// Inventory API calls
export const getInventory = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/inventory`, {
      method: 'GET',
      headers: createHeaders(true),
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch inventory');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Get inventory error:', error);
    throw error;
  }
};

export const updateInventory = async (inventoryData) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/inventory`, {
      method: 'POST',
      headers: createHeaders(true),
      body: JSON.stringify(inventoryData),
    });
    
    if (!response.ok) {
      throw new Error('Failed to update inventory');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Update inventory error:', error);
    throw error;
  }
};

// Order API calls
export const getAllOrders = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/orders`, {
      method: 'GET',
      headers: createHeaders(true),
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch orders');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Get all orders error:', error);
    throw error;
  }
};

export const getMyOrders = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/orders/my-orders`, {
      method: 'GET',
      headers: createHeaders(true),
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch my orders');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Get my orders error:', error);
    throw error;
  }
};

export const getFulfilledOrders = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/orders/fulfilledOrders`, {
      method: 'GET',
      headers: createHeaders(true),
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch fulfilled orders');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Get fulfilled orders error:', error);
    throw error;
  }
};

export const getUnfulfilledOrders = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/orders/unfulfilledOrders`, {
      method: 'GET',
      headers: createHeaders(true),
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch unfulfilled orders');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Get unfulfilled orders error:', error);
    throw error;
  }
};

export const getOrderById = async (id) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/orders/${id}`, {
      method: 'GET',
      headers: createHeaders(true),
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch order');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Get order by id error:', error);
    throw error;
  }
};

export const createOrder = async (orderData) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/orders`, {
      method: 'POST',
      headers: createHeaders(true),
      body: JSON.stringify(orderData),
    });
    
    if (!response.ok) {
      throw new Error('Failed to create order');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Create order error:', error);
    throw error;
  }
};

export const fulfillOrder = async (orderId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/orders/fulfillOrder`, {
      method: 'POST',
      headers: createHeaders(true),
      body: JSON.stringify({ id: orderId }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to fulfill order');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Fulfill order error:', error);
    throw error;
  }
};

// Admin User Management API calls
export const getAllUsers = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/users`, {
      method: 'GET',
      headers: createHeaders(true),
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch users');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Get all users error:', error);
    throw error;
  }
};

export const getUserById = async (id) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/users/${id}`, {
      method: 'GET',
      headers: createHeaders(true),
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch user');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Get user by id error:', error);
    throw error;
  }
};

export const updateUserRole = async (id, role) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/users/${id}/role`, {
      method: 'PUT',
      headers: createHeaders(true),
      body: JSON.stringify({ role }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to update user role');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Update user role error:', error);
    throw error;
  }
};

export const deleteUser = async (id) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/users/${id}`, {
      method: 'DELETE',
      headers: createHeaders(true),
    });
    
    if (!response.ok) {
      throw new Error('Failed to delete user');
    }
    
    // Check if response has content before parsing JSON
    const text = await response.text();
    return text ? JSON.parse(text) : { success: true };
  } catch (error) {
    console.error('Delete user error:', error);
    throw error;
  }
};

// Chat API calls
export const sendChatMessage = async (message) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/chat`, {
      method: 'POST',
      headers: createHeaders(true),
      body: JSON.stringify({ message }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to send message to AI');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Chat error:', error);
    throw error;
  }
};

export const rateFoodItem = async (orderId, foodId, rating) => {
  try {
    // Note: rating is passed as a query parameter (?rating=5.0)
    const response = await fetch(`${API_BASE_URL}/api/foods/orders/${orderId}/${foodId}/rate?rating=${rating}`, {
      method: 'POST',
      headers: createHeaders(true),
    });
    
    if (!response.ok) {
      // We try to get the error message from the backend (e.g., "You have already rated this...")
      const errorText = await response.text();
      throw new Error(errorText || 'Failed to submit rating');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Rate food error:', error);
    throw error;
  }
};

export const fetchDriverDashboard = async (user) => {
  const response = await fetch(`${API_BASE_URL}/api/driverStats?username=${encodeURIComponent(user)}`, {
    method: "GET",
    headers: createHeaders(true),
  });

  if (!response.ok) {
    throw new Error("Failed to fetch driver dashboard data");
  }

  return await response.json();
};

export const acknowledgeAllergyHandoff = async (orderId) => {
  const response = await fetch(
    `${API_BASE_URL}/api/orders/${orderId}/allergy-handoff/acknowledge`,
    {
      method: "POST",
      headers: createHeaders(true),
    }
  );

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || "Failed to acknowledge allergy warning");
  }

  return await response.json();
};

export const getAllergyHandoffStatus = async (orderId) => {
  const response = await fetch(
    `${API_BASE_URL}/api/orders/${orderId}/allergy-handoff/status`,
    {
      method: "GET",
      headers: createHeaders(true),
    }
  );

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || "Failed to fetch allergy handoff status");
  }

  return await response.json();
};

export const claimOrder = async (orderId) => {
  const response = await fetch(`${API_BASE_URL}/api/orders/${orderId}/claim`, {
    method: "POST",
    headers: createHeaders(true),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || "Failed to claim order");
  }

  return await response.json();
};

export const updateOrderStatus = async (orderId, status, username) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/orders/${orderId}`, {
      method: 'POST',
      headers: createHeaders(true),
      body: JSON.stringify({username, status}),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || 'Failed to update order status');
    }

    return await response.json();
  } catch (error) {
    console.error('Update order status error:', error);
    throw error;
  }
};

export const getAvailableOrders = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/orders/availableOrders`, {
      method: 'GET',
      headers: createHeaders(true),
    });

    if (!response.ok) {
      throw new Error('Failed to fetch unfulfilled orders');
    }

    return await response.json();
  } catch (error) {
    console.error('Get unfulfilled orders error:', error);
    throw error;
  }
};

export const getActiveOrders = async (username) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/orders/activeOrders/${username}`, {
      method: 'GET',
      headers: createHeaders(true),
    });

    if (!response.ok) {
      throw new Error('Failed to fetch active orders');
    }

    return await response.json();
  } catch (error) {
    console.error('Get active orders error:', error);
    throw error;
  }
};

// Allergy trace-back API calls

// Reads the backend's error message from a failed response, when it has one.
const readErrorMessage = async (response, fallback) => {
  try {
    const data = await response.json();
    if (data && data.message) {
      return data.message;
    }
  } catch (parseError) {
    // The body was not JSON, so use the fallback message.
  }
  return fallback;
};

// Staff: report that dishes may carry an allergen during a time window.
export const reportRestaurantIncident = async (allergen, foodIds, windowStart, windowEnd) => {
  const response = await fetch(`${API_BASE_URL}/api/incidents`, {
    method: 'POST',
    headers: createHeaders(true),
    body: JSON.stringify({ allergen, foodIds, windowStart, windowEnd }),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Failed to report the incident'));
  }
  return await response.json();
};

// Customer: report a reaction after one of your own orders.
export const reportCustomerReaction = async (orderId, allergen) => {
  const response = await fetch(`${API_BASE_URL}/api/incidents/reaction`, {
    method: 'POST',
    headers: createHeaders(true),
    body: JSON.stringify({ orderId, allergen }),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Failed to report the reaction'));
  }
  return await response.json();
};

// Staff: list every incident, newest first.
export const getIncidents = async () => {
  const response = await fetch(`${API_BASE_URL}/api/incidents`, {
    method: 'GET',
    headers: createHeaders(true),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Failed to load incidents'));
  }
  return await response.json();
};

// Staff: acknowledge an incident and say what the restaurant offers.
export const acknowledgeIncident = async (incidentId, remedy) => {
  const response = await fetch(`${API_BASE_URL}/api/incidents/${incidentId}/acknowledge`, {
    method: 'POST',
    headers: createHeaders(true),
    body: JSON.stringify({ remedy }),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Failed to acknowledge the incident'));
  }
  return await response.json();
};

// Customer: your own safety notices, newest first.
export const getMyNotifications = async () => {
  const response = await fetch(`${API_BASE_URL}/api/notifications/my`, {
    method: 'GET',
    headers: createHeaders(true),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Failed to load notices'));
  }
  return await response.json();
};

// Customer: how many of your notices you have not acknowledged yet.
export const getMyUnreadNotificationCount = async () => {
  const response = await fetch(`${API_BASE_URL}/api/notifications/my/unread-count`, {
    method: 'GET',
    headers: createHeaders(true),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Failed to load the notice count'));
  }
  const data = await response.json();
  return data.count;
};

// Customer: acknowledge one of your notices.
export const acknowledgeNotification = async (notificationId) => {
  const response = await fetch(`${API_BASE_URL}/api/notifications/${notificationId}/acknowledge`, {
    method: 'POST',
    headers: createHeaders(true),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Failed to acknowledge the notice'));
  }
  return await response.json();
};
