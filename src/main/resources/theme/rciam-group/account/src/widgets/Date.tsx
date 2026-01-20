export const isPastDate = (date: Date): string => {
  const currentDate = new Date();
  return isFirstDateBeforeSecond(date, currentDate, "pastDateError");
};

export const addDays = (date: Date, days: number): Date => {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
};
export const dateParse = (date: string) => {
  const split = date.split("-");
  if (split.length !== 3) {
    return new Date();
  }
  const month = split[1];
  const day = split[2];
  const year = split[0];
  return new Date(
    `${year.padStart(4, "0")}-${month.padStart(2, "0")}-${day.padStart(
      2,
      "0"
    )}T00:00:00`
  );
};

export const isFirstDateBeforeSecond = (
  firstDate: Date | null,
  secondDate: Date,
  errorMessage: string
) => {
  // Normalize both dates to remove the time part for an accurate comparison
  if (firstDate) {
    const firstDateWithoutTime = new Date(
      firstDate.getFullYear(),
      firstDate.getMonth(),
      firstDate.getDate()
    );
    const secondDateWithoutTime = new Date(
      secondDate.getFullYear(),
      secondDate.getMonth(),
      secondDate.getDate()
    );

    // Check if the first date is before the second date
    if (firstDateWithoutTime < secondDateWithoutTime) {
      return errorMessage;
    } else {
      return "";
    }
  } else {
    return "";
  }
};
export const dateFormat = (date: Date) =>
  date
    .toLocaleDateString("en-CA", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    })
    .replace(/\//g, "-");

export function formatDateToString(date: any) {
  // Ensure that the input is a valid Date object
  if (!(date instanceof Date) || isNaN(date.getTime())) {
    throw new Error("Invalid date format. Please provide a valid Date object.");
  }
  const month = date.toLocaleString("default", { month: "long" });
  const day = date.getDate();
  const year = date.getFullYear();

  return `${month} ${day}, ${year}`;
}

export const isFutureDate = (date: Date) => {
  const currentDate = new Date();

  if (date > currentDate) {
    return true;
  } else {
    return false;
  }
};
