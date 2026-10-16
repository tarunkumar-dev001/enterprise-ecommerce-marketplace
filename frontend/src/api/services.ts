import { apiClient, AUTH_TOKEN_KEY } from './client'
import type {
  CartResponse,
  AdminDashboardResponse,
  AdminProductResponse,
  AdminUserResponse,
  AdminVendorResponse,
  AdminLedgerSummaryResponse,
  CheckoutItemRequest,
  CouponResponse,
  LoginResponse,
  OrderResponse,
  ShippingAddress,
  PageQuery,
  PaymentResponse,
  ProductRequest,
  ProductDetailsResponse,
  ProductPageResponse,
  ProductReviewResponse,
  ProductResponse,
  UserProfileResponse,
  VendorAnalyticsResponse,
  VendorLedgerSummaryResponse,
  VendorProductResponse,
  WishlistResponse,
} from '../types/api'

export const adminApi = {
  async dashboard() { const { data } = await apiClient.get<AdminDashboardResponse>('/admin/dashboard'); return data },
  async users(role?: string) { const { data } = await apiClient.get<AdminUserResponse[]>('/admin/users', { params: role ? { role } : {} }); return data },
  async vendors() { const { data } = await apiClient.get<AdminVendorResponse[]>('/admin/vendors'); return data },
  async products() { const { data } = await apiClient.get<AdminProductResponse[]>('/admin/products'); return data },
  async approveProduct(productId: number) { const { data } = await apiClient.patch<AdminProductResponse>(`/admin/products/${productId}/approve`); return data },
  async rejectProduct(productId: number, reason: string) { const { data } = await apiClient.patch<AdminProductResponse>(`/admin/products/${productId}/reject`, { reason }); return data },
  async suspendProduct(productId: number, reason: string) { const { data } = await apiClient.patch<AdminProductResponse>(`/admin/products/${productId}/suspend`, { reason }); return data },
  async reinstateProduct(productId: number, reason: string) { const { data } = await apiClient.patch<AdminProductResponse>(`/admin/products/${productId}/reinstate`, { reason }); return data },
  async orders() { const { data } = await apiClient.get<OrderResponse[]>('/admin/orders'); return data },
  async reviews() { const { data } = await apiClient.get<ProductReviewResponse[]>('/admin/reviews'); return data },
  async ledger() { const { data } = await apiClient.get<AdminLedgerSummaryResponse>('/admin/ledger/all'); return data },
}

export const authApi = {
  async login(username: string, password: string) {
    const { data } = await apiClient.post<LoginResponse>('/auth/login', { username, password })

    // Defensive check: Ensure backend returned a valid JSON object with token & user
    if (!data || typeof data !== 'object' || !data.token || !data.user) {
      throw new Error('Invalid authentication payload received from the server.')
    }

    window.localStorage.setItem(AUTH_TOKEN_KEY, data.token)
    return data
  },
  async register(username: string, email: string, password: string, accountType: 'CUSTOMER' | 'VENDOR' = 'CUSTOMER') {
    const { data } = await apiClient.post<string>('/auth/register', { username, email, password, accountType })
    return data
  },
}

export const profileApi = {
  async get() {
    const { data } = await apiClient.get<UserProfileResponse>('/users/me')
    return data
  },
  async update(username: string, email: string) {
    const { data } = await apiClient.put<UserProfileResponse>('/users/me', { username, email })
    return data
  },
}

export const sessionApi = {
  logout() {
    window.localStorage.removeItem(AUTH_TOKEN_KEY)
  },
}

export const productApi = {
  async browse(query: PageQuery = {}) {
    const { data } = await apiClient.get<ProductPageResponse>('/products', { params: query })
    return data
  },
  async details(productId: number) {
    const { data } = await apiClient.get<ProductDetailsResponse>(`/products/${productId}`)
    return data
  },
  async create(product: ProductRequest) {
    const { data } = await apiClient.post<ProductResponse>('/products', product)
    return data
  },
  async update(productId: number, product: ProductRequest) {
    const { data } = await apiClient.put<ProductResponse>(`/products/${productId}`, product)
    return data
  },
  async reviews(productId: number) {
    const { data } = await apiClient.get<ProductReviewResponse[]>(`/products/${productId}/reviews`)
    return data
  },
  async addReview(productId: number, rating: number, text: string) {
    const { data } = await apiClient.post<ProductReviewResponse>(`/products/${productId}/reviews`, { rating, text })
    return data
  },
}

export const cartApi = {
  async get() {
    const { data } = await apiClient.get<CartResponse>('/cart')
    return data
  },
  async addItem(productId: number, quantity: number) {
    const { data } = await apiClient.post<CartResponse>('/cart/items', { productId, quantity })
    return data
  },
  async updateItemQuantity(productId: number, quantity: number) {
    const { data } = await apiClient.put<CartResponse>(`/cart/items/${productId}`, { quantity })
    return data
  },
  async removeItem(productId: number) {
    const { data } = await apiClient.delete<CartResponse>(`/cart/items/${productId}`)
    return data
  },
  async applyCoupon(code: string) {
    const { data } = await apiClient.post<CartResponse>('/cart/coupon', { code })
    return data
  },
  async removeCoupon() {
    const { data } = await apiClient.delete<CartResponse>('/cart/coupon')
    return data
  },
}

export const orderApi = {
  async checkout(items: CheckoutItemRequest[], couponCode?: string, shippingAddress?: ShippingAddress) {
    const { data } = await apiClient.post<OrderResponse>('/orders/checkout', { items, couponCode, shippingAddress })
    return data
  },
  async place() {
    const { data } = await apiClient.post<OrderResponse>('/orders')
    return data
  },
  async list() {
    const { data } = await apiClient.get<OrderResponse[]>('/orders')
    return data
  },
  async details(orderId: number) {
    const { data } = await apiClient.get<OrderResponse>(`/orders/${orderId}`)
    return data
  },
  async cancel(orderId: number) {
    const { data } = await apiClient.post<OrderResponse>(`/orders/${orderId}/cancel`)
    return data
  },
  async simulatePayment(orderId: number, simulateFailure = false) {
    const { data } = await apiClient.post<PaymentResponse>(`/orders/${orderId}/payments`, { simulateFailure })
    return data
  },
  async downloadInvoice(orderId: number) {
    const { data } = await apiClient.get<Blob>(`/orders/${orderId}/invoice`, { responseType: 'blob' })
    return data
  },
}

export const wishlistApi = {
  async get() {
    const { data } = await apiClient.get<WishlistResponse>('/wishlist')
    return data
  },
  async addItem(productId: number) {
    const { data } = await apiClient.post<WishlistResponse>('/wishlist/items', { productId })
    return data
  },
  async removeItem(productId: number) {
    const { data } = await apiClient.delete<WishlistResponse>(`/wishlist/items/${productId}`)
    return data
  },
  async moveItemToCart(productId: number) {
    const { data } = await apiClient.post<CartResponse>(`/wishlist/items/${productId}/move-to-cart`)
    return data
  },
}

export const vendorApi = {
  async analytics(limit = 10) {
    const { data } = await apiClient.get<VendorAnalyticsResponse>('/vendor/analytics', { params: { limit } })
    return data
  },
  async listProducts() {
    const { data } = await apiClient.get<VendorProductResponse[]>('/vendor/products')
    return data
  },
  async listOrders() {
    const { data } = await apiClient.get<OrderResponse[]>('/vendor/orders')
    return data
  },
  async ledger() {
    const { data } = await apiClient.get<VendorLedgerSummaryResponse>('/vendor/ledger')
    return data
  },
  async reviews() {
    const { data } = await apiClient.get<ProductReviewResponse[]>('/vendor/reviews')
    return data
  },
  async updateOrderStatus(orderId: number, status: OrderResponse['status']) {
    const { data } = await apiClient.patch<OrderResponse>(`/vendor/orders/${orderId}/status`, { status })
    return data
  },
}

export const couponApi = {
  async create(code: string, discountType: 'PERCENTAGE' | 'FIXED', discountValue: number, options?: {
    expiresAt?: string
    usageLimit?: number
  }) {
    const { data } = await apiClient.post<CouponResponse>('/coupons', {
      code,
      discountType,
      discountValue,
      ...options,
    })
    return data
  },
}
