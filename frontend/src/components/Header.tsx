import React from 'react';

interface HeaderProps {
  title: string;
  theme: 'light' | 'dark';
  toggleTheme: () => void;
}

export const Header: React.FC<HeaderProps> = ({ title, theme, toggleTheme }) => {
  return (
    <header className="header">
      <h1 className="page-title">{title}</h1>
      <div className="header-actions">
        <button 
          className="theme-toggle" 
          onClick={toggleTheme} 
          aria-label="Toggle theme"
          title={theme === 'light' ? '다크 모드로 전환' : '라이트 모드로 전환'}
        >
          {theme === 'light' ? '🌙' : '☀️'}
        </button>
        
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginLeft: '8px' }}>
          <div style={{
            width: '38px',
            height: '38px',
            borderRadius: '50%',
            backgroundColor: 'var(--primary)',
            color: 'white',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 700,
            fontSize: '0.85rem',
            boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
          }}>
            AD
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
            <span style={{ fontSize: '0.875rem', fontWeight: 700, lineHeight: 1 }}>Administrator</span>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', lineHeight: 1 }}>시스템 관리자</span>
          </div>
        </div>
      </div>
    </header>
  );
};
