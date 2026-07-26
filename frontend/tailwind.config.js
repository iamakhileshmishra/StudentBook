/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
      },
      colors: {
        brand: {
          50:  '#eef5ff',
          100: '#d9e8ff',
          200: '#bcd6ff',
          300: '#8ebcff',
          400: '#5896ff',
          500: '#2563eb',
          600: '#1a4fd6',
          700: '#153cb5',
          800: '#173493',
          900: '#192e75',
          950: '#131d47',
        },
      },
    },
  },
  plugins: [],
}
