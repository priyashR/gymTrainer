import { Link, useLocation } from "react-router-dom";

interface ComingSoonProps {
  title?: string;
}

export default function ComingSoon({ title }: ComingSoonProps) {
  const location = useLocation();
  const pageTitle = title ?? (location.state as { title?: string })?.title ?? "This feature";

  return (
    <main style={{ maxWidth: 400, margin: "4rem auto", textAlign: "center", padding: "0 1rem" }}>
      <h1>{pageTitle}</h1>
      <p>Coming Soon</p>
      <Link to="/">Back to Home</Link>
    </main>
  );
}
