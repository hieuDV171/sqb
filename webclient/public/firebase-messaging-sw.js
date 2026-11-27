// Register before Firebase so we handle clicks on its automatically displayed notifications too.
self.addEventListener('notificationclick', (event) => {
  const notificationData = event.notification.data;
  const data = notificationData?.FCM_MSG?.data ?? notificationData;
  if (!data) return;

  event.stopImmediatePropagation();
  event.notification.close();
  event.waitUntil(openPushTarget(data));
});

async function openPushTarget(data) {
  const notificationId = typeof data.notificationId === 'string' && /^[1-9]\d{0,18}$/.test(data.notificationId)
    ? data.notificationId
    : null;
  const fallback = notificationId ? '/notifications' : '/';
  let target = new URL(fallback, self.location.origin);
  try {
    const requested = new URL(data.targetUrl || data.url || fallback, self.location.origin);
    if (requested.origin === self.location.origin) target = requested;
  } catch {
    // Keep the in-app fallback for an invalid destination.
  }
  if (notificationId) target.searchParams.set('pushNotificationId', notificationId);

  const windowClients = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
  const client = windowClients.find((tab) => new URL(tab.url).origin === self.location.origin);
  if (client) {
    if (!notificationId && target.href === new URL('/', self.location.origin).href) return client.focus();
    try {
      const navigated = await client.navigate(target.href);
      if (navigated) return navigated.focus();
    } catch {
      // Fall back to opening the destination if the current tab cannot navigate.
    }
  }
  return self.clients.openWindow(target.href);
}

// Existing notifications can be handled as soon as the worker updates.
self.addEventListener('install', (event) => event.waitUntil(self.skipWaiting()));

// Scripts for firebase and firebase messaging in Service Worker
importScripts('https://www.gstatic.com/firebasejs/10.13.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.13.0/firebase-messaging-compat.js');

// Parse Firebase config dynamically from Service Worker registration URL query parameters
const params = new URLSearchParams(self.location.search);

const firebaseConfig = {
  apiKey: params.get('apiKey'),
  authDomain: params.get('authDomain'),
  projectId: params.get('projectId'),
  storageBucket: params.get('storageBucket'),
  messagingSenderId: params.get('messagingSenderId'),
  appId: params.get('appId'),
};

// Initialize the Firebase app in the service worker only if required parameters exist
if (firebaseConfig.apiKey && firebaseConfig.projectId) {
  firebase.initializeApp(firebaseConfig);

  // Retrieve an instance of Firebase Messaging so that it can handle background messages
  const messaging = firebase.messaging();

  messaging.onBackgroundMessage((payload) => {
    // Firebase already displays messages with a notification payload.
    if (payload.notification) return;

    return self.registration.showNotification(payload.data?.title || 'Thông báo mới từ SQB', {
      body: payload.data?.body || '',
      icon: '/favicon.svg?v=sqb-3',
      data: payload.data,
    });
  });
}
