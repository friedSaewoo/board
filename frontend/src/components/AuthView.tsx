import React, { useState } from 'react';

interface AuthViewProps {
  onLogin: (email: string, password: string) => Promise<boolean>;
  onSignup: (name: string, email: string, password: string) => Promise<boolean>;
}

type AuthMode = 'login' | 'signup';

export const AuthView: React.FC<AuthViewProps> = ({ onLogin, onSignup }) => {
  const [mode, setMode] = useState<AuthMode>('login');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isSignup = mode === 'signup';

  const switchMode = (nextMode: AuthMode) => {
    setMode(nextMode);
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (isSignup && !name.trim()) {
      setError('이름을 입력해 주세요.');
      return;
    }
    if (!email.trim()) {
      setError('이메일을 입력해 주세요.');
      return;
    }
    if (!password.trim()) {
      setError('비밀번호를 입력해 주세요.');
      return;
    }

    setError('');
    setIsSubmitting(true);

    try {
      const success = isSignup
        ? await onSignup(name.trim(), email.trim(), password)
        : await onLogin(email.trim(), password);

      if (!success) {
        setError(isSignup ? '회원가입에 실패했습니다.' : '로그인에 실패했습니다.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="auth-layout">
      <section className="auth-hero section-card">
        <div className="auth-badge">SESSION AUTH</div>
        <h2>BOARD ADMIN에 오신 것을 환영합니다</h2>
        <p>
          Spring Security 세션 로그인을 사용합니다. 로그인 요청은 백엔드 기본 규칙에 맞춰
          <strong> username=email & password</strong> 형식으로 전송됩니다.
        </p>
        <div className="auth-feature-list">
          <div>
            <span>🔐</span>
            <strong>세션 기반 인증</strong>
            <p>로그인 성공 시 JSESSIONID 쿠키로 인증 상태를 유지합니다.</p>
          </div>
          <div>
            <span>🧾</span>
            <strong>내 정보 조회</strong>
            <p>/members/me API로 현재 로그인 사용자를 확인합니다.</p>
          </div>
          <div>
            <span>🚪</span>
            <strong>로그아웃</strong>
            <p>/logout 요청으로 서버 세션을 종료합니다.</p>
          </div>
        </div>
      </section>

      <section className="auth-card section-card">
        <div className="auth-tabs">
          <button
            type="button"
            className={`auth-tab ${mode === 'login' ? 'active' : ''}`}
            onClick={() => switchMode('login')}
          >
            로그인
          </button>
          <button
            type="button"
            className={`auth-tab ${mode === 'signup' ? 'active' : ''}`}
            onClick={() => switchMode('signup')}
          >
            회원가입
          </button>
        </div>

        <div className="section-header" style={{ marginBottom: '24px' }}>
          <div>
            <h2 className="section-title" style={{ fontSize: '1.35rem' }}>
              {isSignup ? '새 계정 만들기' : '계정 로그인'}
            </h2>
            <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)', marginTop: '4px' }}>
              {isSignup
                ? '이름, 이메일, 비밀번호를 입력해 회원 계정을 생성합니다.'
                : '가입한 이메일과 비밀번호로 관리자 패널에 접속합니다.'}
            </p>
          </div>
        </div>

        <form onSubmit={handleSubmit}>
          {error && <div className="form-error">⚠️ {error}</div>}

          {isSignup && (
            <div className="form-group">
              <label className="form-label" htmlFor="signup-name">이름</label>
              <input
                id="signup-name"
                className="form-input"
                type="text"
                placeholder="홍길동"
                value={name}
                onChange={(e) => setName(e.target.value)}
                disabled={isSubmitting}
              />
            </div>
          )}

          <div className="form-group">
            <label className="form-label" htmlFor="auth-email">이메일</label>
            <input
              id="auth-email"
              className="form-input"
              type="email"
              placeholder="user@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={isSubmitting}
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="auth-password">비밀번호</label>
            <input
              id="auth-password"
              className="form-input"
              type="password"
              placeholder="비밀번호를 입력하세요"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={isSubmitting}
            />
          </div>

          <button className="btn btn-primary auth-submit" type="submit" disabled={isSubmitting}>
            {isSubmitting ? (
              <>
                <div className="loading-spinner" style={{ width: '14px', height: '14px', borderWidth: '2px' }} />
                처리 중...
              </>
            ) : (
              isSignup ? '회원가입 후 로그인' : '로그인'
            )}
          </button>
        </form>
      </section>
    </div>
  );
};
