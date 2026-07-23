import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/** Format a number as Indian currency: ₹1,50,000 */
export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(amount)
}

/** Convert a number to words in Indian number system */
export function numberToWords(amount: number): string {
  if (amount === 0) return 'Zero Only'

  const ones = [
    '', 'One', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine',
    'Ten', 'Eleven', 'Twelve', 'Thirteen', 'Fourteen', 'Fifteen', 'Sixteen',
    'Seventeen', 'Eighteen', 'Nineteen',
  ]
  const tens = [
    '', '', 'Twenty', 'Thirty', 'Forty', 'Fifty', 'Sixty', 'Seventy', 'Eighty', 'Ninety',
  ]

  function convertHundreds(n: number): string {
    let result = ''
    if (n >= 100) {
      result += ones[Math.floor(n / 100)] + ' Hundred '
      n %= 100
    }
    if (n >= 20) {
      result += tens[Math.floor(n / 10)] + ' '
      n %= 10
    }
    if (n > 0) result += ones[n] + ' '
    return result
  }

  const rupees = Math.floor(amount)
  const paise  = Math.round((amount - rupees) * 100)

  let result = ''
  if (rupees >= 10000000) {
    result += convertHundreds(Math.floor(rupees / 10000000)) + 'Crore '
  }
  if (rupees % 10000000 >= 100000) {
    result += convertHundreds(Math.floor((rupees % 10000000) / 100000)) + 'Lakh '
  }
  if (rupees % 100000 >= 1000) {
    result += convertHundreds(Math.floor((rupees % 100000) / 1000)) + 'Thousand '
  }
  if (rupees % 1000 >= 100) {
    result += convertHundreds(Math.floor((rupees % 1000) / 100)) + 'Hundred '
  }
  if (rupees % 100 > 0) {
    result += convertHundreds(rupees % 100)
  }

  let finalResult = result.trim()
  if (paise > 0) {
    finalResult += ` and ${convertHundreds(paise).trim()} Paise`
  }
  return finalResult + ' Only'
}

/** Format a date to "22 Jul 2025" */
export function formatDate(dateStr: string | Date): string {
  const d = new Date(dateStr)
  return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })
}

/** Get month name from number (1-12) */
export function getMonthName(month: number): string {
  return new Date(2000, month - 1, 1).toLocaleString('en-IN', { month: 'long' })
}
