export type UserInfo = {
  id: number
  username: string
  email: string
  roles: string[]
}

export type UserProfileResponse = UserInfo

export type LoginResponse = {
  token: string
  user: UserInfo
}

export type ProductResponse = {
  id: number
  name: string
  description: string | null
  price: number
  stock: number
  category: string
  imageUrl: string | null
  vendorId: number
  vendorUsername: string
  moderationStatus: ProductModerationStatus | null
  moderationReason: string | null
}

export type ProductModerationStatus = 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'SUSPENDED'

export type ProductDetailsResponse = ProductResponse & {
  averageRating: number
  reviewCount: number
}

export type ProductPageResponse = {
  content: ProductResponse[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  sortBy: string
  direction: 'ASC' | 'DESC'
}

export type CartItemResponse = {
  id: number
  productId: number
  productName: string
  imageUrl: string | null
  vendorId: number
  vendorUsername: string
  quantity: number
  unitPrice: number
  subtotal: number
}

export type CartResponse = {
  id: number
  items: CartItemResponse[]
  subtotal: number
  discountAmount: number
  couponCode: string | null
  totalAmount: number
}

export type OrderItemResponse = {
  id: number
  productId: number
  productName: string
  imageUrl: string | null
  quantity: number
  unitPrice: number
  subtotal: number
}

export type CheckoutItemRequest = {
  productId: number
  quantity: number
}

export type CheckoutRequest = {
  items: CheckoutItemRequest[]
  couponCode?: string
  shippingAddress?: ShippingAddress
}

export type ShippingAddress = {
  fullName: string
  phone: string
  addressLine: string
  city: string
  state: string
  postalCode: string
  country: string
}

export type SubOrderResponse = {
  id: number | null
  vendorId: number
  vendorUsername: string
  totalAmount: number
  status: 'PLACED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'
  items: OrderItemResponse[]
}

export type OrderResponse = {
  id: number
  userId: number
  username: string
  totalAmount: number
  subtotalAmount: number
  discountAmount: number
  couponCode: string | null
  status: 'PLACED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'
  paymentStatus: 'PENDING' | 'PAID' | 'FAILED'
  paymentReference: string | null
  placedAt: string
  shippingAddress: ShippingAddress | null
  items: OrderItemResponse[]
  subOrders: SubOrderResponse[]
}

export type PaymentResponse = {
  orderId: number
  amount: number
  paymentStatus: 'PENDING' | 'PAID' | 'FAILED'
  transactionReference: string
  message: string
}

export type WishlistItemResponse = {
  productId: number
  productName: string
  imageUrl: string | null
  price: number
  category: string
  stock: number
  vendorUsername: string
  addedAt: string
}

export type WishlistResponse = {
  id: number
  items: WishlistItemResponse[]
}

export type VendorAnalyticsResponse = {
  totalSalesRevenue: number
  orderCount: number
  unitsSold: number
  bestSellingProducts: {
    productId: number
    productName: string
    unitsSold: number
    salesRevenue: number
  }[]
}

export type PageQuery = {
  page?: number
  size?: number
  search?: string
  sortBy?: 'id' | 'name' | 'price' | 'category' | 'stock'
  direction?: 'asc' | 'desc'
  category?: string
  minPrice?: number
  maxPrice?: number
}

export type VendorProductResponse = ProductResponse

export type ProductRequest = Omit<Pick<ProductResponse, 'name' | 'description' | 'price' | 'stock' | 'category'>, 'description'> & {
  description: string
  imageUrl: string
}

export type ProductReviewResponse = {
  id: number
  productId: number
  customerId: number
  customerUsername: string
  rating: number
  text: string
  createdAt: string
  productName?: string
  vendorUsername?: string
}

export type VendorLedgerRecordResponse = {
  id: number
  orderId: number
  subOrderId: number
  vendorId: number
  vendorUsername: string
  totalOrderAmount: number
  platformCommissionFee: number
  netVendorPayout: number
  payoutStatus: 'PENDING' | 'PAID'
  orderStatus: OrderResponse['status']
  paymentStatus: OrderResponse['paymentStatus']
  createdAt: string
  paidAt: string | null
}

export type VendorLedgerSummaryResponse = {
  vendorId: number
  vendorUsername: string
  grossSales: number
  totalCommission: number
  netEarnings: number
  pendingPayouts: number
  paidOut: number
  records: VendorLedgerRecordResponse[]
}

export type AdminLedgerSummaryResponse = {
  grossSales: number
  platformCommission: number
  vendorNetEarnings: number
  pendingPayouts: number
  paidOut: number
  records: VendorLedgerRecordResponse[]
}

export type CouponResponse = {
  id: number
  code: string
  discountType: 'PERCENTAGE' | 'FIXED'
  discountValue: number
  vendorUsername: string | null
  expiresAt: string | null
  usageLimit: number | null
  usageCount: number
  active: boolean
}

export type AdminUserResponse = Pick<UserInfo, 'id' | 'username' | 'email' | 'roles'>
export type AdminProductResponse = Pick<ProductResponse, 'id' | 'name' | 'category' | 'imageUrl' | 'vendorId' | 'vendorUsername' | 'price' | 'stock'> & {
  averageRating: number
  reviewCount: number
  unitsSold: number
  status: string
  moderationStatus: ProductModerationStatus | null
  moderationReason: string | null
  stockStatus: string
}
export type AdminVendorResponse = {
  id: number; username: string; email: string; productCount: number; orderCount: number
  grossRevenue: number; pendingPayout: number; paidPayout: number; status: string
}
export type AdminDashboardResponse = {
  totalUsers: number; totalVendors: number; totalCustomers: number; totalProducts: number
  totalOrders: number; totalRevenue: number; pendingPayouts: number; platformCommission: number
  orderStatuses: Record<string, number>; recentOrders: OrderResponse[]; topProducts: AdminProductResponse[]
  topVendors: AdminVendorResponse[]; recentReviews: ProductReviewResponse[]
}



