import { Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./features/auth/AuthContext";
import { useAuth } from "./features/auth/useAuth";
import { ProtectedRoute } from "./components/layout/ProtectedRoute";
import Login from "./pages/auth/Login";
import Register from "./pages/auth/Register";
import ComingSoon from "./pages/ComingSoon";
import { LandingPage } from "./pages/LandingPage";
import { CreateProgramPage } from "./pages/CreateProgramPage";
import { ManualInputPage } from "./pages/ManualInputPage";
import { ActivityLogPage } from "./pages/ActivityLogPage";
import { SearchPage } from "./pages/SearchPage";
import { UploadPage } from "./features/upload/UploadPage";
import { ProgramDetailPage } from "./features/vault/ProgramDetailPage";
import { TheaterModePage } from "./features/theater/TheaterModePage";

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
        <Route path="/" element={<ProtectedRoute><LandingPage /></ProtectedRoute>} />
        <Route path="/programs/create" element={<ProtectedRoute><CreateProgramPage /></ProtectedRoute>} />
        <Route path="/manual-input" element={<ProtectedRoute><ManualInputPage /></ProtectedRoute>} />
        <Route path="/log-activity" element={<ProtectedRoute><ActivityLogPage /></ProtectedRoute>} />
        <Route path="/vault/search" element={<ProtectedRoute><SearchPage /></ProtectedRoute>} />
        <Route path="/vault/programs/:id" element={<ProtectedRoute><ProgramDetailPage /></ProtectedRoute>} />
        <Route path="/upload" element={<ProtectedRoute><UploadPage /></ProtectedRoute>} />
        <Route path="/workout/session/:sessionId" element={<ProtectedRoute><TheaterModePage /></ProtectedRoute>} />
        <Route path="/new-workout" element={<ProtectedRoute><ComingSoon title="AI Generation" /></ProtectedRoute>} />
        <Route path="/coming-soon/:feature" element={<ProtectedRoute><ComingSoon /></ProtectedRoute>} />
      </Routes>
    </AuthProvider>
  );
}
