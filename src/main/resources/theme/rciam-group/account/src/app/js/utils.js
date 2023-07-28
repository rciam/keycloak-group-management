export function ValidateEmail(value) {

    var validRegex = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$/;
  
    if (value.match(validRegex)) {
      return true;
  
    } else {
      return false;
    }
  
  }

export function deepEqual(x, y) {
  const ok = Object.keys, tx = typeof x, ty = typeof y;
  return x && y && tx === 'object' && tx === ty ? (
    ok(x).length === ok(y).length &&
      ok(x).every(key => deepEqual(x[key], y[key]))
  ) : (x === y);
}

export function isIntegerOrNumericString(value) {
  if (Number.isInteger(value)||typeof value === 'string' && /^\d+$/.test(value)) {
    // If the value is already an integer or is a string containing only numbers, return true
    return true;
  }
  return false;
}