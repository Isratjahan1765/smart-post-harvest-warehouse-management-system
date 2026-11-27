const FULL = ['MANAGER', 'ADMIN'];

export const isFullAccess = (role) => FULL.includes(role);
export const canEdit = (role) => isFullAccess(role);

export const allowedPages = (role) => {
  if (isFullAccess(role)) return null; // null = all pages
  if (role === 'DRIVER') return ['Drivers', 'Profile'];
  return ['Profile'];
};

export const canView = (role, page) => {
  const pages = allowedPages(role);
  return pages === null || pages.includes(page);
};

export const homePage = (role) => {
  if (isFullAccess(role)) return 'Dashboard';
  if (role === 'DRIVER') return 'Drivers';
  return 'Profile';
};
