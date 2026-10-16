import { createBrowserRouter } from 'react-router-dom'
import { AppShell } from './AppShell'
import { ProtectedRoute } from '../auth/ProtectedRoute'
import { LoginPage, RegisterPage } from '../pages/AuthPages'
import { CartPage } from '../pages/CartPage'
import { OrdersPage } from '../pages/OrdersPage'
import { ProductDetailPage } from '../pages/ProductDetailPage'
import { ProductsPage } from '../pages/ProductsPage'
import { VendorAnalyticsPage } from '../pages/VendorAnalyticsPage'
import { VendorOrdersPage } from '../pages/VendorOrdersPage'
import { VendorProductsPage } from '../pages/VendorProductsPage'
import { VendorOverviewPage } from '../pages/VendorOverviewPage'
import { VendorEarningsPage } from '../pages/VendorEarningsPage'
import { VendorReviewsPage } from '../pages/VendorReviewsPage'
import { VendorStorePage } from '../pages/VendorStorePage'
import { WishlistPage } from '../pages/WishlistPage'
import { AdminDashboardPage, AdminOrdersPage, AdminPayoutsPage, AdminPortalLayout, AdminProductsPage, AdminReviewsPage, AdminUsersPage, AdminVendorsPage } from '../pages/AdminPages'
import {
  HomePage,
  NotFoundPage,
  OrderDetailPage,
} from '../pages/Pages'

const customerRoles = ['ROLE_CUSTOMER', 'ROLE_VENDOR']
const marketplaceRoles = customerRoles

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppShell />,
    children: [
      {
        path: 'admin',
        element: <ProtectedRoute allowedRoles={['ROLE_ADMIN']}><AdminPortalLayout /></ProtectedRoute>,
        children: [
          { index: true, element: <AdminDashboardPage /> },
          { path: 'users', element: <AdminUsersPage /> },
          { path: 'vendors', element: <AdminVendorsPage /> },
          { path: 'products', element: <AdminProductsPage /> },
          { path: 'orders', element: <AdminOrdersPage /> },
          { path: 'payouts', element: <AdminPayoutsPage /> },
          { path: 'reviews', element: <AdminReviewsPage /> },
        ],
      },
      { index: true, element: <HomePage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
      { path: 'products', element: <ProtectedRoute allowedRoles={marketplaceRoles}><ProductsPage /></ProtectedRoute> },
      { path: 'products/:productId', element: <ProtectedRoute allowedRoles={marketplaceRoles}><ProductDetailPage /></ProtectedRoute> },
      { path: 'cart', element: <ProtectedRoute allowedRoles={customerRoles}><CartPage /></ProtectedRoute> },
      { path: 'orders', element: <ProtectedRoute allowedRoles={customerRoles}><OrdersPage /></ProtectedRoute> },
      { path: 'orders/:orderId', element: <ProtectedRoute allowedRoles={customerRoles}><OrderDetailPage /></ProtectedRoute> },
      { path: 'wishlist', element: <ProtectedRoute allowedRoles={customerRoles}><WishlistPage /></ProtectedRoute> },
      { path: 'vendor', element: <ProtectedRoute allowedRoles={['ROLE_VENDOR']}><VendorOverviewPage /></ProtectedRoute> },
      { path: 'vendor/products', element: <ProtectedRoute allowedRoles={['ROLE_VENDOR']}><VendorProductsPage /></ProtectedRoute> },
      { path: 'vendor/orders', element: <ProtectedRoute allowedRoles={['ROLE_VENDOR']}><VendorOrdersPage /></ProtectedRoute> },
      { path: 'vendor/analytics', element: <ProtectedRoute allowedRoles={['ROLE_VENDOR']}><VendorAnalyticsPage /></ProtectedRoute> },
      { path: 'vendor/earnings', element: <ProtectedRoute allowedRoles={['ROLE_VENDOR']}><VendorEarningsPage /></ProtectedRoute> },
      { path: 'vendor/reviews', element: <ProtectedRoute allowedRoles={['ROLE_VENDOR']}><VendorReviewsPage /></ProtectedRoute> },
      { path: 'vendor/store', element: <ProtectedRoute allowedRoles={['ROLE_VENDOR']}><VendorStorePage /></ProtectedRoute> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
