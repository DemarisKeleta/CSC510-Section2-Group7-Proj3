
import { getPantryPairings } from './pantryMatcher';

describe('Pantry-Paired Ordering', () => {
  test('returns two recipes for a supported entree', () => {
    const results = getPantryPairings('CHICKEN WRAP');

    expect(results).toHaveLength(2);
  });

  test('ranks recipes by available pantry ingredients', () => {
    const pantry = ['Plain yogurt', 'Garlic', 'Lemon juice'];

    const results = getPantryPairings('CHICKEN WRAP', pantry);

    expect(results[0].name).toBe('Garlic Yogurt Dip');
    expect(results[0].matchPercentage).toBe(100);
    expect(results[0].readyToMake).toBe(true);
  });

  test('identifies missing ingredients', () => {
    const results = getPantryPairings('CHICKEN WRAP', ['Garlic']);

    const yogurtDip = results.find(
      recipe => recipe.name === 'Garlic Yogurt Dip'
    );

    expect(yogurtDip.missing).toContain('Plain yogurt');
    expect(yogurtDip.readyToMake).toBe(false);
  });

  test('excludes recipes containing restricted allergens', () => {
    const results = getPantryPairings('CHICKEN WRAP', [], ['MILK']);

    expect(results.map(recipe => recipe.name)).toEqual([
      'Creamy Honey Mustard'
    ]);
  });

  test('returns no recommendations for unsupported entrees', () => {
    const results = getPantryPairings('UNKNOWN ENTREE');

    expect(results).toEqual([]);
  });
});
