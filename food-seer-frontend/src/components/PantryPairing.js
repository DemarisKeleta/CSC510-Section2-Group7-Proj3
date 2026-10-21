
import React, { useState } from 'react';
import { pantryIngredients } from '../data/pantryRecipes';
import { getPantryPairings } from '../data/pantryMatcher';

const PantryPairing = ({ cart, restrictedAllergens = [] }) => {
  const [selectedIngredients, setSelectedIngredients] = useState([]);
  const [selectedPairings, setSelectedPairings] = useState({});

  const toggleIngredient = (ingredient) => {
    setSelectedIngredients(previous =>
      previous.includes(ingredient)
        ? previous.filter(item => item !== ingredient)
        : [...previous, ingredient]
    );
  };

  const eligibleEntrees = Object.values(cart)
    .map(item => item.food.foodName)
    .filter((name, index, names) =>
      names.indexOf(name) === index &&
      getPantryPairings(name, [], []).length > 0
    );

  if (eligibleEntrees.length === 0) {
    return null;
  }

  return (
    <section style={{
      marginTop: '24px',
      padding: '20px',
      border: '1px solid #ddd',
      borderRadius: '12px',
      backgroundColor: '#fafafa'
    }}>
      <h2>Pantry-Paired Ordering</h2>
      <p>
        Complete your restaurant meal with something homemade!
        Select the ingredients you already have to discover compatible pairings.
      </p>

      <h3>What's in your pantry?</h3>
      <div style={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: '8px',
        marginBottom: '20px'
      }}>
        {pantryIngredients.map(ingredient => (
          <button
            key={ingredient}
            type="button"
            onClick={() => toggleIngredient(ingredient)}
            aria-pressed={selectedIngredients.includes(ingredient)}
            style={{
              padding: '8px 12px',
              borderRadius: '20px',
              border: '1px solid #bbb',
              cursor: 'pointer',
              backgroundColor: selectedIngredients.includes(ingredient)
                ? '#d4edda'
                : 'white',
              color: '#222'
            }}
          >
            {selectedIngredients.includes(ingredient) ? '✓ ' : '+ '}
            {ingredient}
          </button>
        ))}
      </div>

      {eligibleEntrees.map(entree => {
        const pairings = getPantryPairings(
          entree,
          selectedIngredients,
          restrictedAllergens
        );

        const selectedId = selectedPairings[entree];
        const chosen = pairings.find(recipe => recipe.id === selectedId);

        return (
          <div key={entree} style={{
            borderTop: '1px solid #ddd',
            paddingTop: '16px',
            marginTop: '16px'
          }}>
            <h3>Pair with: {entree}</h3>

            {pairings.length === 0 ? (
              <p>No compatible homemade pairings are available for this entree.</p>
            ) : (
              <>
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                  gap: '12px'
                }}>
                  {pairings.map((recipe, index) => (
                    <div key={recipe.id} style={{
                      padding: '14px',
                      border: selectedId === recipe.id
                        ? '2px solid #28a745'
                        : '1px solid #ddd',
                      borderRadius: '10px',
                      backgroundColor: 'white'
                    }}>
                      {index === 0 && (
                        <strong style={{ color: '#218838' }}>
                          Best pantry match
                        </strong>
                      )}

                      <h4>{recipe.name}</h4>
                      <p>{recipe.minutes} minute preparation</p>

                      <p>
                        <strong>Ingredient match:</strong>{' '}
                        {recipe.matchPercentage}%
                      </p>

                      {recipe.readyToMake ? (
                        <p style={{ color: '#218838' }}>
                          ✓ You have everything needed!
                        </p>
                      ) : (
                        <p>
                          <strong>Missing:</strong>{' '}
                          {recipe.missing.join(', ')}
                        </p>
                      )}

                      <p>
                        <strong>Ingredients:</strong>{' '}
                        {recipe.ingredients.join(', ')}
                      </p>

                      <button
                        type="button"
                        onClick={() => setSelectedPairings(previous => ({
                          ...previous,
                          [entree]: recipe.id
                        }))}
                        style={{
                          padding: '8px 12px',
                          cursor: 'pointer',
                          borderRadius: '6px'
                        }}
                      >
                        {selectedId === recipe.id
                          ? 'Selected ✓'
                          : 'Choose this pairing'}
                      </button>
                    </div>
                  ))}
                </div>

                {chosen && (
                  <div style={{
                    marginTop: '16px',
                    padding: '14px',
                    backgroundColor: '#eaf7ee',
                    borderRadius: '8px'
                  }}>
                    <h4>Your homemade pairing: {chosen.name}</h4>
                    <p><strong>Instructions:</strong> {chosen.instructions}</p>
                    <p>
                      Homemade pairing is not added to your restaurant bill.
                    </p>

                    <button
                      type="button"
                      onClick={() => setSelectedPairings(previous => {
                        const next = { ...previous };
                        delete next[entree];
                        return next;
                      })}
                    >
                      Skip / Remove Pairing
                    </button>
                  </div>
                )}
              </>
            )}
          </div>
        );
      })}
    </section>
  );
};

export default PantryPairing;
