import { Link } from 'react-router-dom';
import type { VaultItem } from '../../types/vault';

interface VaultItemCardProps {
  item: VaultItem;
}

const cardStyle: React.CSSProperties = {
  display: 'block',
  padding: '1rem 1.25rem',
  border: '1px solid #e0e0e0',
  borderRadius: 8,
  textDecoration: 'none',
  color: 'inherit',
  transition: 'border-color 0.15s',
};

const labelStyle: React.CSSProperties = {
  fontSize: '0.75rem',
  color: '#666',
  textTransform: 'uppercase',
  letterSpacing: '0.05em',
};

/**
 * Displays a search result card with name, goal, durationWeeks, and contentSource.
 * Clickable to navigate to the program detail page.
 *
 * Requirements: 8.2, 8.5
 */
export function VaultItemCard({ item }: VaultItemCardProps) {
  return (
    <Link to={`/vault/programs/${item.id}`} style={cardStyle} aria-label={`View ${item.name}`}>
      <h3 style={{ margin: '0 0 0.25rem', fontSize: '1.1rem' }}>{item.name}</h3>
      {item.goal && (
        <p style={{ margin: '0 0 0.5rem', color: '#444', fontSize: '0.9rem' }}>{item.goal}</p>
      )}
      <div style={{ display: 'flex', gap: '1.5rem', flexWrap: 'wrap' }}>
        <span>
          <span style={labelStyle}>Duration: </span>
          {item.durationWeeks} {item.durationWeeks === 1 ? 'week' : 'weeks'}
        </span>
        <span>
          <span style={labelStyle}>Source: </span>
          {formatContentSource(item.contentSource)}
        </span>
      </div>
    </Link>
  );
}

function formatContentSource(source: VaultItem['contentSource']): string {
  switch (source) {
    case 'AI_GENERATED':
      return 'AI Generated';
    case 'UPLOADED':
      return 'Uploaded';
    case 'MANUAL':
      return 'Manual';
    default:
      return source;
  }
}
