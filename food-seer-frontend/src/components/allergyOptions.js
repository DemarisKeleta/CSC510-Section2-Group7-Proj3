// Common allergens offered as suggestions in the report forms. People can
// still type any allergen; the backend understands plurals and synonyms.
export const ALLERGEN_SUGGESTIONS = [
  'Peanuts',
  'Tree nuts',
  'Milk',
  'Eggs',
  'Fish',
  'Shellfish',
  'Wheat',
  'Gluten',
  'Soy',
  'Sesame',
  'Mustard',
  'Sulfites',
];

// Formats a backend date-time such as "2026-10-12T11:00:00" for display.
export const formatDateTime = (value) => {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
};

// Turns a Date into the "YYYY-MM-DDTHH:mm" text a datetime-local input uses.
export const toDateTimeInputValue = (date) => {
  const pad = (number) => String(number).padStart(2, '0');
  const year = date.getFullYear();
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  const hours = pad(date.getHours());
  const minutes = pad(date.getMinutes());
  return `${year}-${month}-${day}T${hours}:${minutes}`;
};
