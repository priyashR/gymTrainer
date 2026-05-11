import { Link } from 'react-router-dom';
import { useVaultSearch } from './useVaultSearch';
import { VaultItemCard } from './VaultItemCard';

/**
 * Vault search page with text input (300ms debounce), focus area and modality
 * dropdown filters, search button, loading indicator, empty state, and results list.
 *
 * Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9, 8.10
 */
export function VaultSearchPage() {
  const { state, setQuery, setFocusArea, setModality, executeSearch } = useVaultSearch();

  return (
    <main style={{ maxWidth: 900, margin: '0 auto', padding: '2rem 1rem' }}>
      <header style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem' }}>
        <h1 style={{ margin: 0 }}>Vault Search</h1>
        <Link to="/" style={{ marginLeft: 'auto', fontSize: '0.9rem' }}>
          ← Home
        </Link>
      </header>

      {/* Search controls */}
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', marginBottom: '1.5rem' }}>
        {/* Text input with debounce */}
        <input
          type="text"
          placeholder="Search programs…"
          value={state.query}
          onChange={(e) => setQuery(e.target.value)}
          aria-label="Search query"
          style={{
            flex: '1 1 200px',
            padding: '0.6rem 0.75rem',
            fontSize: '1rem',
            border: '1px solid #ccc',
            borderRadius: 4,
          }}
        />

        {/* Focus Area dropdown */}
        <select
          value={state.focusArea}
          onChange={(e) => setFocusArea(e.target.value)}
          aria-label="Focus Area filter"
          style={{ padding: '0.6rem 0.75rem', fontSize: '1rem', borderRadius: 4, border: '1px solid #ccc' }}
        >
          <option value="">All Focus Areas</option>
          <option value="Push">Push</option>
          <option value="Pull">Pull</option>
          <option value="Metcon">Metcon</option>
          <option value="Full Body">Full Body</option>
        </select>

        {/* Modality dropdown */}
        <select
          value={state.modality}
          onChange={(e) => setModality(e.target.value)}
          aria-label="Modality filter"
          style={{ padding: '0.6rem 0.75rem', fontSize: '1rem', borderRadius: 4, border: '1px solid #ccc' }}
        >
          <option value="">All Modalities</option>
          <option value="CrossFit">CrossFit</option>
          <option value="Hypertrophy">Hypertrophy</option>
        </select>

        {/* Search button */}
        <button
          type="button"
          onClick={executeSearch}
          style={{
            padding: '0.6rem 1.25rem',
            fontSize: '1rem',
            cursor: 'pointer',
            borderRadius: 4,
            border: '1px solid #333',
            background: '#333',
            color: '#fff',
          }}
        >
          Search
        </button>
      </div>

      {/* Loading indicator */}
      {state.loading && (
        <p aria-busy="true" style={{ color: '#666' }}>
          Searching…
        </p>
      )}

      {/* Error state */}
      {state.error && (
        <div role="alert" style={{ color: '#c62828', marginBottom: '1rem' }}>
          {state.error}
        </div>
      )}

      {/* Empty state */}
      {!state.loading && state.searched && state.results.length === 0 && !state.error && (
        <p style={{ color: '#666', fontStyle: 'italic' }}>
          No programs found matching your search.
        </p>
      )}

      {/* Results list */}
      {!state.loading && state.results.length > 0 && (
        <div style={{ display: 'grid', gap: '0.75rem' }}>
          {state.results.map((item) => (
            <VaultItemCard key={item.id} item={item} />
          ))}
        </div>
      )}
    </main>
  );
}
