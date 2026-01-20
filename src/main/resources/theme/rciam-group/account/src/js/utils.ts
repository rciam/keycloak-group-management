import { environment } from "../environment";

export const getError = (response: any) => {
  let error = response?.data?.error_description
    ? response.data.error_description
    : response?.data?.error
    ? response.data.error
    : "unexpectedError";
  return error;
};

export function kcPath(path: string): string {
  return `${new URL(environment.baseUrl).pathname}${path}`;
}

export function ValidateEmail(value:string) {
  var validRegex =
    /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$/;

  if (value.match(validRegex)) {
    return true;
  } else {
    return false;
  }
}
export function isIntegerOrNumericString(value:any) {
  if (Number.isInteger(value)||typeof value === 'string' && /^\d+$/.test(value)) {
    // If the value is already an integer or is a string containing only numbers, return true
    return true;
  }
  return false;
}
