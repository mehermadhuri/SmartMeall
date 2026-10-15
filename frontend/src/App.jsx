import { useCallback, useEffect, useMemo, useState } from 'react'
import { apiRequest } from './api.js'
import './App.css'

const localDate = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
const today = () => localDate(new Date())
const weekLater = () => {
  const date = new Date()
  date.setDate(date.getDate() + 7)
  return localDate(date)
}
const money = (amount) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(Number(amount || 0))

const readStoredUser = () => {
  try {
    return JSON.parse(localStorage.getItem('smartmeal-user') || 'null')
  } catch {
    return null
  }
}

function App() {
  const [menu, setMenu] = useState([])
  const [cart, setCart] = useState([])
  const [slots, setSlots] = useState([])
  const [token, setToken] = useState(() => localStorage.getItem('smartmeal-token') || '')
  const [user, setUser] = useState(readStoredUser)
  const [authMode, setAuthMode] = useState('login')
  const [form, setForm] = useState({ fullName: '', email: '', password: '', confirmPassword: '', studentId: '' })
  const [mealCategory, setMealCategory] = useState('BREAKFAST')
  const [bookingDate, setBookingDate] = useState(today())
  const [pickupSlotId, setPickupSlotId] = useState('')
  const [editingOrderId, setEditingOrderId] = useState(null)
  const [orders, setOrders] = useState([])
  const [adminSummary, setAdminSummary] = useState(null)
  const [demand, setDemand] = useState([])
  const [forecast, setForecast] = useState(null)
  const [forecastMeal, setForecastMeal] = useState('BREAKFAST')
  const [forecastDate, setForecastDate] = useState(weekLater())
  const [menuForm, setMenuForm] = useState({ name: '', description: '', price: '', category: 'Breakfast', imageUrl: '', mealCategory: 'BREAKFAST', available: true })
  const [editingMenuId, setEditingMenuId] = useState(null)
  const [slotForm, setSlotForm] = useState({ name: '', mealCategory: 'BREAKFAST', startTime: '08:00', endTime: '09:00', capacity: 100, active: true })
  const [editingSlotId, setEditingSlotId] = useState(null)
  const [loading, setLoading] = useState(false)
  const [menuLoading, setMenuLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const isAdmin = user?.role === 'ADMIN'
  const total = useMemo(() => cart.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0), [cart])
  const visibleMenu = useMemo(
    () => menu.filter((item) => item.mealCategory?.toUpperCase() === mealCategory),
    [menu, mealCategory],
  )

  const loadOrders = useCallback(async () => {
    if (!token) return
    setOrders(await apiRequest('/orders/my', { token }))
  }, [token])

  const loadAdminData = useCallback(async () => {
    if (!token || !isAdmin) return
    const [summary, slotData, demandData] = await Promise.all([
      apiRequest('/admin/summary', { token }),
      apiRequest('/admin/pickup-slots', { token }),
      apiRequest(`/admin/demand?from=${today()}&to=${weekLater()}`, { token }),
    ])
    setAdminSummary(summary)
    setSlots(slotData)
    setDemand(demandData)
  }, [isAdmin, token])

  useEffect(() => {
    let active = true
    apiRequest('/menu')
      .then((data) => { if (active) setMenu(data) })
      .catch((requestError) => { if (active) setError(requestError.message) })
      .finally(() => { if (active) setMenuLoading(false) })
    return () => { active = false }
  }, [])

  useEffect(() => {
    let active = true
    apiRequest(`/pickup-slots?mealCategory=${encodeURIComponent(mealCategory)}`)
      .then((data) => {
        if (!active) return
        setSlots((current) => isAdmin ? current : data)
      })
      .catch((requestError) => { if (active) setError(requestError.message) })
    return () => { active = false }
  }, [isAdmin, mealCategory])

  useEffect(() => {
    if (!token) {
      setOrders([])
      setAdminSummary(null)
      setDemand([])
      return
    }
    let active = true
    Promise.all([loadOrders(), loadAdminData()])
      .catch((requestError) => { if (active) setError(requestError.message) })
    return () => { active = false }
  }, [loadAdminData, loadOrders, token])

  const updateAuth = (data) => {
    const profile = { email: data.email, fullName: data.fullName, role: data.role }
    setToken(data.token)
    setUser(profile)
    localStorage.setItem('smartmeal-token', data.token)
    localStorage.setItem('smartmeal-user', JSON.stringify(profile))
  }

  const handleAuthSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setSuccess('')
    if (authMode === 'register' && form.password !== form.confirmPassword) {
      setError('Password and confirmation must match.')
      return
    }
    if (form.password.length < 8) {
      setError('Password must contain at least 8 characters.')
      return
    }
    setLoading(true)
    try {
      const path = authMode === 'login' ? '/auth/login' : '/auth/register'
      const body = authMode === 'login'
        ? { email: form.email, password: form.password }
        : { fullName: form.fullName, email: form.email, studentId: form.studentId, password: form.password, confirmPassword: form.confirmPassword }
      updateAuth(await apiRequest(path, { method: 'POST', body }))
      setSuccess(authMode === 'login' ? 'Signed in successfully.' : 'Account created successfully.')
      setForm({ fullName: '', email: '', password: '', confirmPassword: '', studentId: '' })
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }

  const logout = () => {
    localStorage.removeItem('smartmeal-token')
    localStorage.removeItem('smartmeal-user')
    setToken('')
    setUser(null)
    setOrders([])
    setCart([])
    setEditingOrderId(null)
    setSuccess('You have been signed out.')
    setError('')
  }

  const addToCart = (item) => {
    setCart((current) => {
      const existing = current.find((entry) => entry.id === item.id)
      return existing
        ? current.map((entry) => entry.id === item.id ? { ...entry, quantity: entry.quantity + 1 } : entry)
        : [...current, { ...item, quantity: 1 }]
    })
  }

  const updateCartQuantity = (itemId, delta) => {
    setCart((current) => current
      .map((item) => item.id === itemId ? { ...item, quantity: Math.max(0, item.quantity + delta) } : item)
      .filter((item) => item.quantity > 0))
  }

  const handlePlaceOrder = async () => {
    setError('')
    setSuccess('')
    if (!token) return setError('Sign in before placing a booking.')
    if (cart.length === 0) return setError('Add at least one meal to your cart.')
    if (!pickupSlotId) return setError('Choose an available pickup slot.')
    setLoading(true)
    try {
      const payload = {
        mealCategory,
        bookingDate,
        pickupSlotId: Number(pickupSlotId),
        items: cart.map((item) => ({ foodId: item.id, quantity: item.quantity })),
      }
      await apiRequest(editingOrderId ? `/orders/${editingOrderId}` : '/orders', {
        method: editingOrderId ? 'PUT' : 'POST',
        token,
        body: payload,
      })
      setCart([])
      setEditingOrderId(null)
      setSuccess(editingOrderId ? 'Booking updated.' : 'Booking placed successfully.')
      await loadOrders()
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }

  const editBooking = (order) => {
    setMealCategory(order.mealCategory)
    setBookingDate(order.bookingDate)
    setPickupSlotId(String(order.pickupSlotId || ''))
    setCart(order.items.map((item) => ({
      id: item.foodId,
      name: item.name,
      price: item.unitPrice,
      quantity: item.quantity,
      imageUrl: '',
    })))
    setEditingOrderId(order.id)
    setError('')
    setSuccess('Update the date, slot, or quantities, then save your booking.')
    document.getElementById('cart')?.scrollIntoView({ behavior: 'smooth' })
  }

  const cancelBooking = async (orderId) => {
    setError('')
    setSuccess('')
    try {
      await apiRequest(`/orders/${orderId}`, { method: 'DELETE', token })
      setSuccess('Booking cancelled.')
      await loadOrders()
    } catch (requestError) {
      setError(requestError.message)
    }
  }

  const saveMenuItem = async (event) => {
    event.preventDefault()
    setError('')
    try {
      const payload = { ...menuForm, price: Number(menuForm.price) }
      await apiRequest(editingMenuId ? `/admin/menu/${editingMenuId}` : '/admin/menu', {
        method: editingMenuId ? 'PUT' : 'POST',
        token,
        body: payload,
      })
      setMenuForm({ name: '', description: '', price: '', category: 'Breakfast', imageUrl: '', mealCategory: 'BREAKFAST', available: true })
      setEditingMenuId(null)
      setMenu(await apiRequest('/menu'))
      setSuccess('Menu saved.')
    } catch (requestError) {
      setError(requestError.message)
    }
  }

  const savePickupSlot = async (event) => {
    event.preventDefault()
    setError('')
    try {
      await apiRequest(editingSlotId ? `/admin/pickup-slots/${editingSlotId}` : '/admin/pickup-slots', {
        method: editingSlotId ? 'PUT' : 'POST',
        token,
        body: { ...slotForm, capacity: Number(slotForm.capacity) },
      })
      setSlotForm({ name: '', mealCategory: 'BREAKFAST', startTime: '08:00', endTime: '09:00', capacity: 100, active: true })
      setEditingSlotId(null)
      await loadAdminData()
      setSuccess('Pickup slot saved.')
    } catch (requestError) {
      setError(requestError.message)
    }
  }

  const loadForecast = async (event) => {
    event.preventDefault()
    setError('')
    setForecast(null)
    try {
      const query = new URLSearchParams({ mealCategory: forecastMeal, targetDate: forecastDate })
      setForecast(await apiRequest(`/admin/forecast?${query}`, { token }))
    } catch (requestError) {
      setError(requestError.message)
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand-wrap">
          <div className="brand-mark">S</div>
          <div><h1>SmartMeal</h1><small>Campus meal pre-booking</small></div>
        </div>
        <nav className="nav" aria-label="Main navigation">
          <a href="#menu">Menu</a><a href="#cart">Booking</a>
          {user && <a href="#history">My bookings</a>}
          {isAdmin && <a href="#admin">Admin</a>}
        </nav>
        <div className="auth-actions">
          {user ? <><span>Signed in as {user.fullName}</span><button onClick={logout}>Logout</button></> : <a href="#auth">Sign in</a>}
        </div>
      </header>

      <main className="main-layout">
        <section className="hero card">
          <div><p className="eyebrow">Campus dining, made simple</p><h2>Choose a meal and pickup time.</h2><p>Book breakfast and lunch ahead. Your booking is managed by the SmartMeal service.</p></div>
          <div className="hero-meta"><div><strong>Booking policy</strong><span>Changes close before pickup</span></div><div><strong>Secure access</strong><span>Student and admin roles</span></div></div>
        </section>

        {(error || success) && <div className={error ? 'error notice' : 'success notice'} role={error ? 'alert' : 'status'}>{error || success}</div>}

        <section id="menu" className="content-grid">
          <div className="menu-panel card">
            <div className="section-head"><h3>Menu</h3><span>{visibleMenu.length} {mealCategory.toLowerCase()} items</span></div>
            <div className="field-row">
              <label>Meal
                <select value={mealCategory} onChange={(event) => { setMealCategory(event.target.value); setCart([]); setPickupSlotId(''); setEditingOrderId(null) }}>
                  <option value="BREAKFAST">Breakfast</option><option value="LUNCH">Lunch</option>
                </select>
              </label>
            </div>
            {menuLoading ? <p role="status">Loading menu…</p> : visibleMenu.length === 0 ? <p className="empty-state">No meals are currently listed for this category.</p> : (
              <div className="menu-grid">
                {visibleMenu.map((item) => (
                  <article className="food-card" key={item.id}>
                    {item.imageUrl && <img src={item.imageUrl} alt="" loading="lazy" />}
                    <div className="food-card-body">
                      <div className="food-header"><h4>{item.name}</h4><span className="price">{money(item.price)}</span></div>
                      <p>{item.description}</p>
                      <div className="chip-row"><span>{item.category}</span><span>{item.available ? 'Available' : 'Sold out'}</span></div>
                      <button onClick={() => addToCart(item)} disabled={!item.available}>Add to booking</button>
                      {isAdmin && <button className="secondary-button" onClick={() => { setEditingMenuId(item.id); setMenuForm({ name: item.name, description: item.description, price: item.price, category: item.category, imageUrl: item.imageUrl, mealCategory: item.mealCategory, available: item.available }) }}>Edit meal</button>}
                    </div>
                  </article>
                ))}
              </div>
            )}
          </div>

          <aside id="cart" className="cart-panel card">
            <div className="section-head"><h3>{editingOrderId ? 'Update booking' : 'Your booking'}</h3><span>{cart.reduce((sum, item) => sum + item.quantity, 0)} portions</span></div>
            <div className="booking-fields">
              <label>Pickup date<input type="date" min={today()} value={bookingDate} onChange={(event) => setBookingDate(event.target.value)} required /></label>
              <label>Pickup slot
                <select value={pickupSlotId} onChange={(event) => setPickupSlotId(event.target.value)} required>
                  <option value="">Select a slot</option>
                  {slots.filter((slot) => slot.active && slot.mealCategory === mealCategory).map((slot) => (
                    <option key={slot.id} value={slot.id}>{slot.name} ({slot.startTime}–{slot.endTime})</option>
                  ))}
                </select>
              </label>
            </div>
            {cart.length === 0 ? <p className="empty-state">Add meals from the menu to start.</p> : (
              <div className="cart-items">
                {cart.map((item) => (
                  <div className="cart-item" key={item.id}>
                    <div className="cart-item-meta"><strong>{item.name}</strong><span>{money(item.price)}</span></div>
                    <div className="quantity-controls">
                      <button aria-label={`Decrease ${item.name}`} onClick={() => updateCartQuantity(item.id, -1)}>-</button>
                      <span>{item.quantity}</span>
                      <button aria-label={`Increase ${item.name}`} onClick={() => updateCartQuantity(item.id, 1)}>+</button>
                    </div>
                  </div>
                ))}
              </div>
            )}
            <div className="totals"><div><span>Total</span><strong>{money(total)}</strong></div>
              <button className="primary" onClick={handlePlaceOrder} disabled={loading || cart.length === 0}>{loading ? 'Saving…' : editingOrderId ? 'Save booking changes' : 'Place booking'}</button>
              {editingOrderId && <button className="secondary-button" onClick={() => { setEditingOrderId(null); setCart([]) }}>Stop editing</button>}
              {!user && <p>Sign in or create a student account to book.</p>}
            </div>
          </aside>
        </section>

        {!user && <section id="auth" className="card auth-panel">
          <div className="section-head"><h3>{authMode === 'login' ? 'Sign In' : 'Create student account'}</h3><button className="switch-btn" onClick={() => { setAuthMode(authMode === 'login' ? 'register' : 'login'); setError('') }}>{authMode === 'login' ? 'Register' : 'I already have an account'}</button></div>
          <form onSubmit={handleAuthSubmit} className="auth-form">
            {authMode === 'register' && <>
              <label>Full name<input autoComplete="name" required value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} /></label>
              <label>Student ID<input required value={form.studentId} onChange={(event) => setForm({ ...form, studentId: event.target.value })} /></label>
            </>}
            <label>Email<input type="email" autoComplete="email" required value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
            <label>Password<input type="password" autoComplete={authMode === 'login' ? 'current-password' : 'new-password'} minLength={8} required value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} /></label>
            {authMode === 'register' && <label>Confirm password<input type="password" autoComplete="new-password" minLength={8} required value={form.confirmPassword} onChange={(event) => setForm({ ...form, confirmPassword: event.target.value })} /></label>}
            <button className="primary" type="submit" disabled={loading}>{loading ? 'Please wait…' : authMode === 'login' ? 'Sign In' : 'Create account'}</button>
          </form>
        </section>}

        {user && <section id="history" className="card history-panel">
          <div className="section-head"><h3>My bookings</h3><button className="secondary-button" onClick={() => loadOrders().catch((requestError) => setError(requestError.message))}>Refresh</button></div>
          {orders.length === 0 ? <p className="empty-state">You have no bookings yet.</p> : <div className="table-wrap">
            <table><thead><tr><th>Date</th><th>Pickup slot</th><th>Meals</th><th>Total</th><th>Status</th><th>Actions</th></tr></thead>
              <tbody>{orders.map((order) => <tr key={order.id}>
                <td>{order.bookingDate || 'Date unavailable'}</td><td>{order.pickupSlotName || 'Slot unavailable'}<br />{order.pickupStartTime || ''}</td>
                <td>{order.items.map((item) => `${item.name} × ${item.quantity}`).join(', ')}</td><td>{money(order.totalAmount)}</td><td>{order.status}</td>
                <td>{order.status === 'PLACED' && <><button className="text-button" onClick={() => editBooking(order)}>Edit</button><button className="text-button danger-text" onClick={() => cancelBooking(order.id)}>Cancel</button></>}</td>
              </tr>)}</tbody>
            </table>
          </div>}
        </section>}

        {isAdmin && <section id="admin" className="admin-section">
          <h2>Administration</h2>
          {adminSummary && <div className="summary-grid">
            <div className="card"><strong>Students</strong><span>{adminSummary.totalStudents}</span></div>
            <div className="card"><strong>Bookings today</strong><span>{adminSummary.bookingsToday}</span></div>
            <div className="card"><strong>Active bookings</strong><span>{adminSummary.activeBookings}</span></div>
            <div className="card"><strong>Peak meal</strong><span>{adminSummary.peakMealCategory}</span></div>
          </div>}
          <div className="admin-grid">
            <form className="card auth-form" onSubmit={saveMenuItem}>
              <h3>{editingMenuId ? 'Edit menu item' : 'Add menu item'}</h3>
              <label>Name<input required value={menuForm.name} onChange={(event) => setMenuForm({ ...menuForm, name: event.target.value })} /></label>
              <label>Description<input required value={menuForm.description} onChange={(event) => setMenuForm({ ...menuForm, description: event.target.value })} /></label>
              <label>Price<input type="number" min="0.01" step="0.01" required value={menuForm.price} onChange={(event) => setMenuForm({ ...menuForm, price: event.target.value })} /></label>
              <label>Category<input required value={menuForm.category} onChange={(event) => setMenuForm({ ...menuForm, category: event.target.value })} /></label>
              <label>Image URL<input type="url" required value={menuForm.imageUrl} onChange={(event) => setMenuForm({ ...menuForm, imageUrl: event.target.value })} /></label>
              <label>Meal<select value={menuForm.mealCategory} onChange={(event) => setMenuForm({ ...menuForm, mealCategory: event.target.value })}><option>BREAKFAST</option><option>LUNCH</option></select></label>
              <label className="checkbox-field"><input type="checkbox" checked={menuForm.available} onChange={(event) => setMenuForm({ ...menuForm, available: event.target.checked })} /> Available</label>
              <button className="primary" type="submit">Save menu item</button>
            </form>
            <form className="card auth-form" onSubmit={savePickupSlot}>
              <h3>{editingSlotId ? 'Edit pickup slot' : 'Add pickup slot'}</h3>
              <label>Name<input required value={slotForm.name} onChange={(event) => setSlotForm({ ...slotForm, name: event.target.value })} /></label>
              <label>Meal<select value={slotForm.mealCategory} onChange={(event) => setSlotForm({ ...slotForm, mealCategory: event.target.value })}><option>BREAKFAST</option><option>LUNCH</option></select></label>
              <label>Starts<input type="time" required value={slotForm.startTime} onChange={(event) => setSlotForm({ ...slotForm, startTime: event.target.value })} /></label>
              <label>Ends<input type="time" required value={slotForm.endTime} onChange={(event) => setSlotForm({ ...slotForm, endTime: event.target.value })} /></label>
              <label>Portion capacity<input type="number" min="1" required value={slotForm.capacity} onChange={(event) => setSlotForm({ ...slotForm, capacity: event.target.value })} /></label>
              <label className="checkbox-field"><input type="checkbox" checked={slotForm.active} onChange={(event) => setSlotForm({ ...slotForm, active: event.target.checked })} /> Active</label>
              <button className="primary" type="submit">Save pickup slot</button>
              <h4>Existing pickup slots</h4>
              {slots.map((slot) => <button type="button" className="slot-row" key={slot.id} onClick={() => { setEditingSlotId(slot.id); setSlotForm({ ...slot, capacity: slot.capacity }) }}>{slot.name} · {slot.mealCategory} · {slot.active ? 'Active' : 'Inactive'}</button>)}
            </form>
            <div className="card demand-panel">
              <h3>Demand by date, meal, and pickup slot</h3>
              {demand.length === 0 ? <p className="empty-state">No bookings in the next week.</p> : <div className="table-wrap"><table><thead><tr><th>Date</th><th>Meal</th><th>Slot</th><th>Portions</th><th>Bookings</th></tr></thead><tbody>
                {demand.map((row) => <tr key={`${row.bookingDate}-${row.mealCategory}-${row.pickupSlotId}`}><td>{row.bookingDate}</td><td>{row.mealCategory}</td><td>{row.pickupSlotName}</td><td>{row.totalQuantity}</td><td>{row.bookingCount}</td></tr>)}
              </tbody></table></div>}
            </div>
            <form className="card auth-form forecast-panel" onSubmit={loadForecast}>
              <h3>Demand forecast</h3>
              <label>Meal<select value={forecastMeal} onChange={(event) => setForecastMeal(event.target.value)}><option>BREAKFAST</option><option>LUNCH</option></select></label>
              <label>Forecast date<input type="date" required value={forecastDate} onChange={(event) => setForecastDate(event.target.value)} /></label>
              <button className="primary" type="submit">Forecast demand</button>
              {forecast && <p role="status">{forecast.predictedQuantity} portions · {forecast.method} · {forecast.sampleCount} historical dates. {forecast.explanation}</p>}
            </form>
          </div>
        </section>}
      </main>
    </div>
  )
}

export default App
