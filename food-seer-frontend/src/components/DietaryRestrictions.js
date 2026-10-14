import React, { useState, useEffect } from 'react';

const DietaryRestrictions = ({ restrictions, onUpdate, onNext, onPrevious, canGoNext, isLastStep }) => {
  const [selectedRestrictions, setSelectedRestrictions] = useState(restrictions);

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

  // Comprehensive allergen options matching backend
  const allergenOptions = [
    { value: 'MILK', label: 'Milk/Dairy' },
    { value: 'LACTOSE', label: 'Lactose' },
    { value: 'EGGS', label: 'Eggs' },
    { value: 'FISH', label: 'Fish' },
    { value: 'SHELLFISH', label: 'Shellfish' },
    { value: 'TREE-NUTS', label: 'Tree Nuts' },
    { value: 'PEANUTS', label: 'Peanuts' },
    { value: 'WHEAT', label: 'Wheat' },
    { value: 'GLUTEN', label: 'Gluten' },
    { value: 'SOY', label: 'Soy' },
    { value: 'SESAME', label: 'Sesame' },
    { value: 'CORN', label: 'Corn' },
    { value: 'SULFITES', label: 'Sulfites' },
    { value: 'MUSTARD', label: 'Mustard' },
    { value: 'MEAT', label: 'Meat (All)' },
    { value: 'BEEF', label: 'Beef' },
    { value: 'PORK', label: 'Pork' },
    { value: 'POULTRY', label: 'Poultry' },
    { value: 'GELATIN', label: 'Gelatin' },
    { value: 'CAFFEINE', label: 'Caffeine' }
  ];

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
