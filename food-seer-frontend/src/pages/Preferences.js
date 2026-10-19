import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import BudgetQuestion from '../components/BudgetQuestion';
import DietaryRestrictions from '../components/DietaryRestrictions';
import { updateUserPreferences, getCurrentUser } from '../services/api';

function Preferences() {
  const [currentStep, setCurrentStep] = useState(1);
  const [quizData, setQuizData] = useState({
    budget: '',
    dietaryRestrictions: []
  });
  // the user's existing daily calorie goal (set on Personal Diet), carried through
  // on save below so finishing this quiz doesn't wipe it back to null
  const [dailyCalorieGoal, setDailyCalorieGoal] = useState(null);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  // Load existing preferences when component mounts
  useEffect(() => {
    const loadPreferences = async () => {
      try {
        const user = await getCurrentUser();
        console.log('📥 Loading user preferences:', user);
        
        if (user.costPreference || user.dietaryRestrictions) {
          // Parse existing preferences
          const existingBudget = user.costPreference || '';
          const existingDietary = user.dietaryRestrictions || '';

          // Parse dietary restrictions - KEEP ORIGINAL CASE (should be UPPERCASE)
          const dietaryArray = existingDietary ? existingDietary.split(',').map(d => d.trim().toUpperCase()) : [];

          console.log('✅ Parsed preferences:', {
            budget: existingBudget,
            dietaryRestrictions: dietaryArray
          });

          setQuizData({
            budget: existingBudget,
            dietaryRestrictions: dietaryArray
          });
        }
        // always capture the existing goal, independent of the costPreference/dietaryRestrictions check above
        setDailyCalorieGoal(user.dailyCalorieGoal ?? null);
      } catch (error) {
        console.error('❌ Error loading preferences:', error);
        // If error, just show empty form
      } finally {
        setLoading(false);
      }
    };

    loadPreferences();
  }, []);

  const handleNext = () => {
    if (currentStep < 2) {
      setCurrentStep(currentStep + 1);
    }
  };

  const handlePrevious = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1);
    }
  };

  const updateQuizData = (field, value) => {
    setQuizData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleFinish = async () => {
    setSaving(true);
    try {
      // Prepare preferences
      const costPreference = quizData.budget;
      const dietaryRestrictions = quizData.dietaryRestrictions.join(', ');

      console.log('💾 Saving preferences to backend:', {
        costPreference,
        dietaryRestrictions,
        dietaryRestrictionsArray: quizData.dietaryRestrictions
      });

      // Save preferences to backend - pass the existing calorie goal through so this
      // quiz (which has no calorie-goal step of its own) doesn't overwrite it with null
      await updateUserPreferences(costPreference, dietaryRestrictions, dailyCalorieGoal);
      
      console.log('✅ Preferences saved successfully!');

      // Navigate to recommendations
      navigate('/recommendations');
    } catch (error) {
      console.error('❌ Error saving preferences:', error);
      alert('Failed to save preferences. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  const renderCurrentStep = () => {
    switch (currentStep) {
      case 1:
        return (
          <BudgetQuestion
            budget={quizData.budget}
            onUpdate={updateQuizData}
            onNext={handleNext}
            onPrevious={handlePrevious}
            canGoNext={quizData.budget !== ''}
          />
        );
      case 2:
        return (
          <DietaryRestrictions
            restrictions={quizData.dietaryRestrictions}
            onUpdate={updateQuizData}
            onNext={handleFinish}
            onPrevious={handlePrevious}
            canGoNext={!saving}
            isLastStep={true}
          />
        );
      default:
        return null;
    }
  };

  if (loading) {
    return (
      <div className="App">
        <div className="loading">Loading preferences...</div>
      </div>
    );
  }

  return (
    <div className="App">
      {renderCurrentStep()}
    </div>
  );
}

export default Preferences;

