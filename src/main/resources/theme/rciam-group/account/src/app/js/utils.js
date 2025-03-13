import { Msg } from '../widgets/Msg';

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

export const dateParse = (date) => {
  const split = date.split('-');
  if (split.length !== 3) {
  return new Date();
  }
  const month = split[1];
  const day = split[2];
  const year = split[0];
  return new Date(`${year.padStart(4, '0')}-${month.padStart(2, '0')}-${day.padStart(2, '0')}T00:00:00`);
};

export const getCurrentDate = ()=>{
  const date = new Date();

  let currentDay= String(date.getDate()).padStart(2, '0');

  let currentMonth = String(date.getMonth()+1).padStart(2,"0");

  let currentYear = date.getFullYear();

  // we will display the date as YYYY-MM-DD 

  return `${currentYear}-${currentMonth}-${currentDay}`;
}

export const isFutureDate = (date) => {
  const currentDate = new Date();

  if (date > currentDate) {
    return true;
  }
  else{
    return false;
  }
  
};


export function formatDateToString(date) {
  // Ensure that the input is a valid Date object
  if (!(date instanceof Date) || isNaN(date.getTime())) {
    throw new Error('Invalid date format. Please provide a valid Date object.');
  }

  const month = date.toLocaleString('default', { month: 'long' });
  const day = date.getDate();
  const year = date.getFullYear();

  return `${month} ${day}, ${year}`;
}

export const isFirstDateBeforeSecond = (firstDate, secondDate, errorMessage) => {
  // Normalize both dates to remove the time part for an accurate comparison    
  if (firstDate) {
      const firstDateWithoutTime = new Date(firstDate.getFullYear(), firstDate.getMonth(), firstDate.getDate());
      const secondDateWithoutTime = new Date(secondDate.getFullYear(), secondDate.getMonth(), secondDate.getDate());

      // Check if the first date is before the second date
      if (firstDateWithoutTime < secondDateWithoutTime) {
          return errorMessage;
      } else {
          return "";
      }
  }
  else {
      return "";
  }
};


export const getError = (response)=>{
  let error = response?.data?.error_description?response.data.error_description:response?.data?.error?response.data.error:Msg.localize('unexpectedError');
  return error;
}