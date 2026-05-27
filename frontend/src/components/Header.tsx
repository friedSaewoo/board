import React from 'react';
import { Member } from '../types';

interface HeaderProps {
  title: string;
  theme: 'light' | 'dark';
  toggleTheme: () => void;
  currentUser: Member | null;
  onLogout: () => void;
  onNavigateToAuth: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  title,
  theme,
  toggleTheme,
  currentUser,
  onLogout,
  onNavigateToAuth,
}) => {
  const initials = currentUser?.name?.slice(0, 2).toUpperCase() || 'GU';

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

        <div className="header-user-card">
          <div className="header-avatar">{initials}</div>
          <div className="header-user-text">
            <span>{currentUser ? currentUser.name : 'Guest'}</span>
            <small>{currentUser ? currentUser.email : '로그인이 필요합니다'}</small>
          </div>
          {currentUser ? (
            <button className="btn btn-secondary header-auth-btn" onClick={onLogout}>
              로그아웃
            </button>
          ) : (
            <button className="btn btn-primary header-auth-btn" onClick={onNavigateToAuth}>
              로그인
            </button>
          )}
        </div>
      </div>
    </header>
  );
};
