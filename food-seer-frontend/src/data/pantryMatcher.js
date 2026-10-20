
import { pantryRecipes } from './pantryRecipes';

// Recommend recipes for a restaurant entree, ranked by pantry availability.
export const getPantryPairings = (entree, availableIngredients = [], restrictedAllergens = []) => {
  const owned = new Set(availableIngredients.map(item => item.trim().toLowerCase()));
  const normalizeAllergen = (value) => {
  	const normalized = value.trim().toUpperCase();

  	const aliases = {
    		DAIRY: 'MILK',
    		LACTOSE: 'MILK',
    		GLUTEN: 'WHEAT',
    		'TREE NUTS': 'TREE-NUTS',
  	};

  	return aliases[normalized] || normalized;
  };

  const restricted = new Set(
    restrictedAllergens.map(normalizeAllergen)
  );

  return pantryRecipes
    .filter(recipe => recipe.entree.toUpperCase() === entree.trim().toUpperCase())
    .filter(recipe =>
  	!recipe.allergens.some(allergen =>
    	  restricted.has(normalizeAllergen(allergen))
  	)
    )
    .map(recipe => {
      const available = recipe.ingredients.filter(ingredient =>
        owned.has(ingredient.toLowerCase())
      );

      const missing = recipe.ingredients.filter(ingredient =>
        !owned.has(ingredient.toLowerCase())
      );

      return {
        ...recipe,
        available,
        missing,
        matchPercentage: Math.round(
          (available.length / recipe.ingredients.length) * 100
        ),
        readyToMake: missing.length === 0
      };
    })
    .sort((a, b) =>
      b.matchPercentage - a.matchPercentage ||
      a.minutes - b.minutes
    );
};
