import React, { useState, useEffect } from 'react';
import { getAllergens } from '../services/api';

const DietaryRestrictions = ({ restrictions, onUpdate, onNext, onPrevious, canGoNext, isLastStep }) => {
  const [selectedRestrictions, setSelectedRestrictions] = useState(restrictions);
  // Shared, site-wide allergen list (same source Personal Diet reads from and
  // adds to) - starts empty and loads below, rather than a fixed hardcoded set.
  const [allergenOptions, setAllergenOptions] = useState([]);

  // Sync state with prop when it changes (e.g., when user returns to update preferences)
  useEffect(() => {
    console.log('🔄 DietaryRestrictions received restrictions prop:', restrictions);
    setSelectedRestrictions(restrictions);
  }, [restrictions]);

  // Log initial state
  useEffect(() => {
    console.log('🎯 DietaryRestrictions component mounted with:', {
      restrictions,
      selectedRestrictions
    });
  }, []);

  useEffect(() => {
    getAllergens()
      .then(setAllergenOptions)
      .catch((error) => console.error('Error loading allergen list:', error));
  }, []);

  const handleRestrictionChange = (value) => {
    let newRestrictions;
    if (selectedRestrictions.includes(value)) {
      console.log(`➖ Removing restriction: ${value}`);
      newRestrictions = selectedRestrictions.filter(r => r !== value);
    } else {
      console.log(`➕ Adding restriction: ${value}`);
      newRestrictions = [...selectedRestrictions, value];
    }
    
    console.log('📝 Updated dietary restrictions:', newRestrictions);
    setSelectedRestrictions(newRestrictions);
    onUpdate('dietaryRestrictions', newRestrictions);
  };

  const handleNext = () => {
    if (canGoNext) {
      onNext();
    }
  };

  return (
    <div className="quiz-container allergen-selection">
      <div className="icon-container">
        <div className="icon-carrot">🥕</div>
      </div>
      
      <h1 className="question-title">Do you have any allergies or dietary restrictions?</h1>
      <p className="question-subtitle">Select all that apply, or skip to see all options</p>
      
      <div className="allergens-scroll-container">
        <div className="allergens-selection-grid">
          {allergenOptions.map((option) => (
            <label
              key={option.value}
              className={`allergen-option ${selectedRestrictions.includes(option.value) ? 'selected' : ''}`}
            >
              <input
                type="checkbox"
                checked={selectedRestrictions.includes(option.value)}
                onChange={() => handleRestrictionChange(option.value)}
              />
              <span>{option.label}</span>
            </label>
          ))}
        </div>
      </div>

      {selectedRestrictions.length > 0 && (
        <div className="selected-restrictions-summary">
          <strong>Selected ({selectedRestrictions.length}):</strong> {selectedRestrictions.join(', ')}
        </div>
      )}
      
      <div className="navigation">
        <button className="previous-button" onClick={onPrevious}>
          <span className="previous-icon">←</span>
          Previous
        </button>
        <button
          className="next-button"
          onClick={handleNext}
          disabled={!canGoNext}
        >
          {isLastStep ? 'Finish' : 'Next'}
          <span className="next-icon">{isLastStep ? '✓' : '→'}</span>
        </button>
      </div>
    </div>
  );
};

export default DietaryRestrictions;
