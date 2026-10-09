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
    console.log('[firebase-messaging-sw.js] Received background message: ', payload);
    
    const notificationTitle = payload.notification?.title || payload.data?.title || 'Thông báo mới từ SQB';
    const notificationOptions = {
      body: payload.notification?.body || payload.data?.body || '',
      icon: '/favicon.svg',
      data: payload.data,
    };

    self.registration.showNotification(notificationTitle, notificationOptions);
  });
}

// Lắng nghe sự kiện người dùng bấm vào thông báo hệ thống
self.addEventListener('notificationclick', (event) => {
  event.notification.close();

  const targetUrl = event.notification.data?.targetUrl || event.notification.data?.url || '/';

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windowClients) => {
      // 1. Nếu đã có tab SQB đang mở, focus vào tab đó và điều hướng
      for (const client of windowClients) {
        if (client.url.includes(self.location.origin) && 'focus' in client) {
          if ('navigate' in client && targetUrl !== '/') {
            client.navigate(targetUrl);
          }
          return client.focus();
        }
      }
      // 2. Nếu chưa có tab nào mở, mở window mới
      if (clients.openWindow) {
        return clients.openWindow(targetUrl);
      }
    })
  );
});
