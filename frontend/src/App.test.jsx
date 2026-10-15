import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.jsx'

const menu = [{
  id: 1,
  name: 'Idly',
  description: 'Steamed rice cakes',
  price: 35,
  category: 'Breakfast',
  imageUrl: '',
  mealCategory: 'BREAKFAST',
  available: true,
}]

const slot = {
  id: 2,
  name: 'Breakfast 1',
  mealCategory: 'BREAKFAST',
  startTime: '08:00',
  endTime: '09:00',
  capacity: 100,
  active: true,
}

const jsonResponse = (data, status = 200) => ({
  ok: status >= 200 && status < 300,
  status,
  text: async () => JSON.stringify(data),
})

describe('SmartMeal application', () => {
  beforeEach(() => {
    localStorage.clear()
    global.fetch = vi.fn(async (url) => {
      if (String(url).includes('/menu')) return jsonResponse(menu)
      if (String(url).includes('/pickup-slots')) return jsonResponse([slot])
      return jsonResponse([])
    })
  })

  it('loads the menu and signs a student in through the API', async () => {
    global.fetch.mockImplementation(async (url, options = {}) => {
      if (String(url).includes('/menu')) return jsonResponse(menu)
      if (String(url).includes('/pickup-slots')) return jsonResponse([slot])
      if (String(url).includes('/auth/login')) {
        expect(JSON.parse(options.body)).toEqual({ email: 'student@example.test', password: 'strong-pass-1' })
        return jsonResponse({ token: 'signed-token', email: 'student@example.test', fullName: 'Test Student', role: 'STUDENT' })
      }
      if (String(url).includes('/orders/my')) return jsonResponse([])
      return jsonResponse([])
    })

    render(<App />)
    expect(await screen.findByText('Idly')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Sign In' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /send email otp/i })).not.toBeInTheDocument()
    expect(screen.queryByLabelText(/email otp/i)).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'student@example.test' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'strong-pass-1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Sign In' }))

    expect(await screen.findByText('Signed in as Test Student')).toBeInTheDocument()
    expect(localStorage.getItem('smartmeal-token')).toBe('signed-token')
    await waitFor(() => expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/orders/my'),
      expect.objectContaining({ headers: expect.any(Headers) }),
    ))
  })

  it('registers a student through the API', async () => {
    global.fetch.mockImplementation(async (url, options = {}) => {
      if (String(url).includes('/menu')) return jsonResponse(menu)
      if (String(url).includes('/pickup-slots')) return jsonResponse([slot])
      if (String(url).includes('/auth/register')) {
        expect(JSON.parse(options.body)).toMatchObject({
          fullName: 'Test Student',
          studentId: 'ST-42',
          email: 'student@example.test',
        })
        return jsonResponse({ token: 'registered-token', email: 'student@example.test', fullName: 'Test Student', role: 'STUDENT' }, 201)
      }
      if (String(url).includes('/orders/my')) return jsonResponse([])
      return jsonResponse([])
    })

    render(<App />)
    fireEvent.click(screen.getByRole('button', { name: 'Register' }))
    fireEvent.change(screen.getByLabelText('Full name'), { target: { value: 'Test Student' } })
    fireEvent.change(screen.getByLabelText('Student ID'), { target: { value: 'ST-42' } })
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'student@example.test' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'strong-pass-1' } })
    fireEvent.change(screen.getByLabelText('Confirm password'), { target: { value: 'strong-pass-1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText('Signed in as Test Student')).toBeInTheDocument()
    expect(localStorage.getItem('smartmeal-token')).toBe('registered-token')
  })

  it('submits the selected date, pickup slot, and item quantities', async () => {
    global.fetch.mockImplementation(async (url, options = {}) => {
      if (String(url).includes('/menu')) return jsonResponse(menu)
      if (String(url).includes('/pickup-slots')) return jsonResponse([slot])
      if (String(url).includes('/auth/login')) return jsonResponse({ token: 'token', email: 'student@example.test', fullName: 'Test Student', role: 'STUDENT' })
      if (String(url).includes('/orders/my')) return jsonResponse([])
      if (String(url).endsWith('/orders')) {
        expect(options.method).toBe('POST')
        expect(JSON.parse(options.body)).toMatchObject({
          mealCategory: 'BREAKFAST',
          bookingDate: '2099-05-06',
          pickupSlotId: 2,
          items: [{ foodId: 1, quantity: 1 }],
        })
        return jsonResponse({ id: 1, status: 'PLACED' }, 201)
      }
      return jsonResponse([])
    })

    render(<App />)
    await screen.findByText('Idly')
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'student@example.test' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'strong-pass-1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Sign In' }))
    await screen.findByText('Signed in as Test Student')
    fireEvent.click(screen.getByRole('button', { name: 'Add to booking' }))
    fireEvent.change(screen.getByLabelText('Pickup date'), { target: { value: '2099-05-06' } })
    fireEvent.change(screen.getByLabelText('Pickup slot'), { target: { value: '2' } })
    fireEvent.click(screen.getByRole('button', { name: 'Place booking' }))

    expect(await screen.findByText('Booking placed successfully.')).toBeInTheDocument()
  })

  it('displays API errors instead of a fake success state', async () => {
    global.fetch.mockImplementation(async (url) => {
      if (String(url).includes('/menu')) return jsonResponse({ error: 'Menu service unavailable' }, 503)
      if (String(url).includes('/pickup-slots')) return jsonResponse([])
      return jsonResponse([])
    })
    render(<App />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Menu service unavailable')
  })
})