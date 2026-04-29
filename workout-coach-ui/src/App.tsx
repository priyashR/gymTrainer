import { Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./features/auth/AuthContext";
import { useAuth } from "./features/auth/useAuth";
import { ProtectedRoute } from "./components/layout/ProtectedRoute";
import Login from "./pages/auth/Login";
import Register from "./pages/auth/Register";
import Home from "./pages/Home";
import ComingSoon from "./pages/ComingSoon";

/** Redirects authenticated users away from public-only routes. */
function PublicOnly({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();
  if (isLoading) return <div aria-busy="true">Loading…</div>;
  if (isAuthenticated) return <Navigate to="/" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        {/* Public routes — redirect to home if already authenticated */}
        <Route path="/login" element={<PublicOnly><Login /></PublicOnly>} />
        <Route path="/register" element={<PublicOnly><Register /></PublicOnly>} />

        {/* Protected routes */}
        <Route path="/" element={<ProtectedRoute><Home /></ProtectedRoute>} />
        <Route path="/new-workout" element={<ProtectedRoute><ComingSoon title="New Workout" /></ProtectedRoute>} />
        <Route path="/my-performance" element={<ProtectedRoute><ComingSoon title="My Performance" /></ProtectedRoute>} />
        <Route path="/workout" element={<ProtectedRoute><ComingSoon title="Workout" /></ProtectedRoute>} />
      </Routes>
    </AuthProvider>
  );
}
