
export const pantryRecipes = [
  {
    id: 1,
    entree: "CHICKEN WRAP",
    name: "Creamy Honey Mustard",
    ingredients: ["Mayonnaise", "Mustard", "Honey", "Lemon juice"],
    minutes: 5,
    instructions: "Mix mayonnaise, mustard, honey, and lemon juice until smooth. Serve as a dip for your chicken wrap.",
    allergens: ["EGGS", "MUSTARD"]
  },
  {
    id: 2,
    entree: "CHICKEN WRAP",
    name: "Garlic Yogurt Dip",
    ingredients: ["Plain yogurt", "Garlic", "Lemon juice"],
    minutes: 5,
    instructions: "Stir plain yogurt, minced garlic, and lemon juice together until creamy. Chill briefly if desired.",
    allergens: ["MILK", "DAIRY", "LACTOSE"]
  },
  {
    id: 3,
    entree: "BURRITO",
    name: "Red Salsa",
    ingredients: ["Canned tomatoes", "Onion", "Lime", "Garlic", "Chili powder"],
    minutes: 5,
    instructions: "Blend the tomatoes, onion, lime juice, garlic, and chili powder until smooth. Season to taste.",
    allergens: []
  },
  {
    id: 4,
    entree: "BURRITO",
    name: "Avocado Lime Crema",
    ingredients: ["Avocado", "Plain yogurt", "Lime", "Salt"],
    minutes: 5,
    instructions: "Mash or blend avocado with yogurt, lime juice, and salt until creamy.",
    allergens: ["MILK", "DAIRY", "LACTOSE"]
  },
  {
    id: 5,
    entree: "STEAK",
    name: "Garlic Herb Butter",
    ingredients: ["Butter", "Garlic", "Parsley"],
    minutes: 5,
    instructions: "Mix softened butter with minced garlic and parsley. Add a spoonful to the warm steak.",
    allergens: ["MILK", "DAIRY", "LACTOSE"]
  },
  {
    id: 6,
    entree: "STEAK",
    name: "Roasted Potatoes",
    ingredients: ["Potato", "Olive oil", "Salt", "Paprika"],
    minutes: 30,
    instructions: "Cut potatoes into wedges, toss with olive oil, salt, and paprika, and roast at 425°F until tender and golden.",
    allergens: []
  },
  {
    id: 7,
    entree: "SALMON",
    name: "Cucumber Lemon Salad",
    ingredients: ["Cucumber", "Lemon juice", "Olive oil", "Salt"],
    minutes: 10,
    instructions: "Slice cucumber thinly and toss with lemon juice, olive oil, and salt.",
    allergens: []
  },
  {
    id: 8,
    entree: "SALMON",
    name: "Honey Garlic Glaze",
    ingredients: ["Honey", "Garlic", "Soy sauce", "Lemon juice"],
    minutes: 5,
    instructions: "Simmer honey, minced garlic, soy sauce, and lemon juice briefly until slightly thickened. Spoon over fully cooked salmon.",
    allergens: ["SOY", "WHEAT", "GLUTEN"]
  },
  {
    id: 9,
    entree: "PIZZA SLICE",
    name: "Garlic Marinara Dip",
    ingredients: ["Tomato sauce", "Garlic", "Italian seasoning"],
    minutes: 8,
    instructions: "Warm tomato sauce with minced garlic and Italian seasoning. Serve as a dipping sauce.",
    allergens: []
  },
  {
    id: 10,
    entree: "PIZZA SLICE",
    name: "Parmesan Garlic Dip",
    ingredients: ["Mayonnaise", "Parmesan", "Garlic", "Lemon juice"],
    minutes: 5,
    instructions: "Combine mayonnaise, grated Parmesan, garlic, and lemon juice into a creamy dip.",
    allergens: ["EGGS", "MILK", "DAIRY", "LACTOSE"]
  },
  {
    id: 11,
    entree: "SUSHI ROLL",
    name: "Quick Pickled Cucumbers",
    ingredients: ["Cucumber", "Rice vinegar", "Sugar", "Salt"],
    minutes: 10,
    instructions: "Thinly slice cucumber, toss with rice vinegar, sugar, and salt, and let sit for several minutes.",
    allergens: []
  },
  {
    id: 12,
    entree: "SUSHI ROLL",
    name: "Spicy Mayo",
    ingredients: ["Mayonnaise", "Sriracha", "Lime"],
    minutes: 5,
    instructions: "Mix mayonnaise, sriracha, and fresh lime juice until smooth.",
    allergens: ["EGGS"]
  },
  {
    id: 13,
    entree: "PASTA",
    name: "Garlic Toast",
    ingredients: ["Bread", "Garlic", "Olive oil"],
    minutes: 10,
    instructions: "Brush bread with olive oil and minced garlic, then toast until golden.",
    allergens: ["WHEAT", "GLUTEN"]
  },
  {
    id: 14,
    entree: "PASTA",
    name: "Lemon Herb Dressing",
    ingredients: ["Olive oil", "Lemon juice", "Parsley", "Garlic"],
    minutes: 5,
    instructions: "Whisk olive oil, lemon juice, parsley, and minced garlic. Drizzle over pasta or serve on the side.",
    allergens: []
  },
  {
    id: 15,
    entree: "VEGGIE BURGER",
    name: "Seasoned Potato Wedges",
    ingredients: ["Potato", "Olive oil", "Paprika", "Salt"],
    minutes: 30,
    instructions: "Slice potatoes into wedges, toss with olive oil, paprika, and salt, and bake at 425°F until crisp and cooked through.",
    allergens: []
  },
  {
    id: 16,
    entree: "VEGGIE BURGER",
    name: "Quick Coleslaw",
    ingredients: ["Cabbage", "Mayonnaise", "Lemon juice", "Salt"],
    minutes: 10,
    instructions: "Shred cabbage and toss with mayonnaise, lemon juice, and salt. Serve chilled.",
    allergens: ["EGGS"]
  }
];

export const pantryIngredients = [
  ...new Set(pantryRecipes.flatMap(recipe => recipe.ingredients))
].sort();
