// Enumerate the tab types that can be opened
const TabTypes = {
  WELCOME: -1,
  DOC_REF: 0,
  EXPLORER_TREE: 1,
  PROCESSING: 2,
  TRACKER_DASHBOARD: 3,
  USER_ME: 4,
  AUTH_USERS: 5,
  AUTH_TOKENS: 6,
};

const pathPrefix = '/s';

const TabTypeDisplayInfo = {
  [TabTypes.WELCOME]: {
    getTitle: tabData => 'Welcome',
    path: `${pathPrefix}/welcome/`,
    icon: 'home',
  },
  [TabTypes.DOC_REF]: {
    getTitle: tabData => tabData.name,
    path: `${pathPrefix}/docref/`,
    icon: 'file outline',
  },
  [TabTypes.EXPLORER_TREE]: {
    getTitle: () => 'Explorer',
    path: `${pathPrefix}/explorer`,
    icon: 'eye',
  },
  [TabTypes.PROCESSING]: {
    getTitle: () => 'Processing',
    path: `${pathPrefix}/processing`,
    icon: 'play',
  },
  [TabTypes.TRACKER_DASHBOARD]: {
    getTitle: () => 'Trackers',
    path: `${pathPrefix}/trackers`,
    icon: 'tasks',
  },
  [TabTypes.USER_ME]: {
    getTitle: () => 'Me',
    path: `${pathPrefix}/me`,
    icon: 'user',
  },
  [TabTypes.AUTH_USERS]: {
    getTitle: () => 'Users',
    path: `${pathPrefix}/users`,
    icon: 'users',
  },
  [TabTypes.AUTH_TOKENS]: {
    getTitle: () => 'API Keys',
    path: `${pathPrefix}/apikeys`,
    icon: 'key',
  },
};

export { TabTypes, TabTypeDisplayInfo };

export default TabTypes;
