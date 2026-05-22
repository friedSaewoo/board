import React from 'react';

interface SidebarProps {
  activeMenu: string;
  setActiveMenu: (menu: 'dashboard' | 'board' | 'settings') => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ activeMenu, setActiveMenu }) => {
  return (
    <aside className="sidebar">
      <div className="logo-section">
        <span className="logo-icon">⚡</span>
        <span>BOARD ADMIN</span>
      </div>
      <nav className="nav-links">
        <button
          className={`nav-link ${activeMenu === 'dashboard' ? 'active' : ''}`}
          onClick={() => setActiveMenu('dashboard')}
          style={{ border: 'none', background: 'none', width: '100%', textAlign: 'left' }}
        >
          <span>📊</span>
          <span>대시보드</span>
        </button>
        <button
          className={`nav-link ${activeMenu === 'board' ? 'active' : ''}`}
          onClick={() => setActiveMenu('board')}
          style={{ border: 'none', background: 'none', width: '100%', textAlign: 'left' }}
        >
          <span>📋</span>
          <span>게시물 목록</span>
        </button>
        <button
          className={`nav-link ${activeMenu === 'settings' ? 'active' : ''}`}
          onClick={() => setActiveMenu('settings')}
          style={{ border: 'none', background: 'none', width: '100%', textAlign: 'left' }}
        >
          <span>⚙️</span>
          <span>시스템 설정</span>
        </button>
      </nav>
    </aside>
  );
};
