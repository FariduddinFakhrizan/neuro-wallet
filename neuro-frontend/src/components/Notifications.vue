<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

const props = defineProps(['user']);

const notifications = ref([]);
const unreadCount = ref(0);
const isLoading = ref(false);
const filter = ref('all'); // 'all', 'unread'

const fetchNotifications = async () => {
  isLoading.value = true;
  try {
    const res = await axios.get(`http://localhost:8080/api/notifications/${props.user.id}`);
    notifications.value = res.data;
    
    const countRes = await axios.get(`http://localhost:8080/api/notifications/${props.user.id}/unread-count`);
    unreadCount.value = countRes.data.count;
  } catch (error) {
    console.error('Failed to fetch notifications', error);
  } finally {
    isLoading.value = false;
  }
};

const markAsRead = async (notificationId) => {
  try {
    await axios.post(`http://localhost:8080/api/notifications/${notificationId}/read`);
    await fetchNotifications();
  } catch (error) {
    console.error('Failed to mark as read', error);
  }
};

const markAllAsRead = async () => {
  try {
    await axios.post(`http://localhost:8080/api/notifications/${props.user.id}/read-all`);
    await fetchNotifications();
  } catch (error) {
    console.error('Failed to mark all as read', error);
  }
};

const deleteNotification = async (notificationId) => {
  try {
    await axios.delete(`http://localhost:8080/api/notifications/${notificationId}`);
    await fetchNotifications();
  } catch (error) {
    console.error('Failed to delete notification', error);
  }
};

const getNotificationIcon = (type) => {
  const icons = {
    TRANSACTION_RECEIVED: 'fa-arrow-down',
    TRANSACTION_SENT: 'fa-arrow-up',
    LOW_BALANCE: 'fa-exclamation-triangle',
    LARGE_TRANSACTION: 'fa-bolt',
    APPROVAL_PENDING: 'fa-clock',
    APPROVAL_APPROVED: 'fa-check-circle',
    APPROVAL_REJECTED: 'fa-times-circle',
    PAYMENT_REQUEST_RECEIVED: 'fa-file-invoice-dollar',
    BUDGET_ALERT: 'fa-chart-line',
    RECURRING_PAYMENT_PROCESSED: 'fa-sync'
  };
  return icons[type] || 'fa-bell';
};

const getNotificationColor = (type) => {
  const colors = {
    TRANSACTION_RECEIVED: 'green',
    TRANSACTION_SENT: 'orange',
    LOW_BALANCE: 'red',
    LARGE_TRANSACTION: 'yellow',
    APPROVAL_PENDING: 'blue',
    APPROVAL_APPROVED: 'green',
    APPROVAL_REJECTED: 'red',
    PAYMENT_REQUEST_RECEIVED: 'purple',
    BUDGET_ALERT: 'orange',
    RECURRING_PAYMENT_PROCESSED: 'indigo'
  };
  return colors[type] || 'slate';
};

const formatDate = (timestamp) => {
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now - date;
  
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);
  
  if (minutes < 1) return 'Just now';
  if (minutes < 60) return `${minutes}m ago`;
  if (hours < 24) return `${hours}h ago`;
  if (days < 7) return `${days}d ago`;
  return date.toLocaleDateString();
};

const filteredNotifications = () => {
  if (filter.value === 'unread') {
    return notifications.value.filter(n => !n.isRead);
  }
  return notifications.value;
};

onMounted(() => {
  fetchNotifications();
});

defineExpose({ fetchNotifications, unreadCount });
</script>

<template>
  <div class="notifications-container">
    <div class="flex justify-between items-center mb-8">
      <div>
        <h3 class="section-title">Notifications</h3>
        <p class="text-slate-400 text-sm mt-2">{{ unreadCount }} unread notifications</p>
      </div>
      <div class="flex gap-3">
        <button 
          v-if="unreadCount > 0" 
          @click="markAllAsRead" 
          class="midnight-btn-sm"
        >
          <i class="fas fa-check-double mr-2"></i> Mark All Read
        </button>
      </div>
    </div>

    <!-- Filter Tabs -->
    <div class="filter-tabs mb-6">
      <button 
        :class="{ active: filter === 'all' }" 
        @click="filter = 'all'"
        class="filter-tab"
      >
        All
      </button>
      <button 
        :class="{ active: filter === 'unread' }" 
        @click="filter = 'unread'"
        class="filter-tab"
      >
        Unread ({{ unreadCount }})
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="text-center py-12">
      <i class="fas fa-circle-notch fa-spin text-4xl text-indigo-400"></i>
      <p class="text-slate-400 mt-4">Loading notifications...</p>
    </div>

    <!-- Notifications List -->
    <div v-else class="space-y-3">
      <TransitionGroup name="list">
        <div 
          v-for="notif in filteredNotifications()" 
          :key="notif.id" 
          :class="['notification-item', { unread: !notif.isRead }]"
        >
          <div class="flex items-start gap-4">
            <div :class="['notification-icon', getNotificationColor(notif.type)]">
              <i :class="['fas', getNotificationIcon(notif.type)]"></i>
            </div>
            <div class="flex-1">
              <p class="notification-message">{{ notif.message }}</p>
              <p class="notification-time">{{ formatDate(notif.createdAt) }}</p>
            </div>
            <div class="flex gap-2">
              <button 
                v-if="!notif.isRead" 
                @click="markAsRead(notif.id)" 
                class="icon-btn-small" 
                title="Mark as read"
              >
                <i class="fas fa-check"></i>
              </button>
              <button 
                @click="deleteNotification(notif.id)" 
                class="icon-btn-small delete" 
                title="Delete"
              >
                <i class="fas fa-trash"></i>
              </button>
            </div>
          </div>
        </div>
      </TransitionGroup>

      <!-- Empty State -->
      <div v-if="filteredNotifications().length === 0" class="empty-state">
        <i class="fas fa-bell-slash text-6xl mb-6 opacity-10"></i>
        <p class="uppercase tracking-widest font-black text-xs opacity-40">
          {{ filter === 'unread' ? 'No unread notifications' : 'No notifications yet' }}
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.notifications-container {
  animation: fadeIn 0.5s ease-out;
}

.notification-item {
  background: rgba(255, 255, 255, 0.02);
  padding: 1.25rem;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.notification-item.unread {
  background: rgba(99, 102, 241, 0.05);
  border-color: rgba(99, 102, 241, 0.2);
}

.notification-item:hover {
  border-color: rgba(99, 102, 241, 0.3);
  transform: translateX(4px);
}

.notification-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  flex-shrink: 0;
}

.notification-icon.green {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.2);
  color: #10b981;
}

.notification-icon.orange {
  background: rgba(249, 115, 22, 0.1);
  border: 1px solid rgba(249, 115, 22, 0.2);
  color: #f97316;
}

.notification-icon.red {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.notification-icon.blue {
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.2);
  color: #3b82f6;
}

.notification-icon.yellow {
  background: rgba(251, 191, 36, 0.1);
  border: 1px solid rgba(251, 191, 36, 0.2);
  color: #fbbf24;
}

.notification-icon.purple {
  background: rgba(168, 85, 247, 0.1);
  border: 1px solid rgba(168, 85, 247, 0.2);
  color: #a855f7;
}

.notification-icon.indigo {
  background: rgba(99, 102, 241, 0.1);
  border: 1px solid rgba(99, 102, 241, 0.2);
  color: #6366f1;
}

.notification-message {
  color: white;
  font-weight: 600;
  font-size: 0.95rem;
  line-height: 1.5;
}

.notification-time {
  color: #94a3b8;
  font-size: 0.75rem;
  font-weight: 700;
  margin-top: 4px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.icon-btn-small {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 0.85rem;
}

.icon-btn-small:hover {
  background: rgba(99, 102, 241, 0.2);
  border-color: #6366f1;
  color: white;
}

.icon-btn-small.delete:hover {
  background: rgba(239, 68, 68, 0.2);
  border-color: #ef4444;
  color: #ef4444;
}

.filter-tabs {
  display: flex;
  gap: 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.filter-tab {
  padding: 0.75rem 1.5rem;
  background: transparent;
  border: none;
  color: #94a3b8;
  font-weight: 700;
  font-size: 0.875rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}

.filter-tab.active {
  color: #6366f1;
  border-bottom-color: #6366f1;
}

.filter-tab:hover:not(.active) {
  color: white;
}

.midnight-btn-sm {
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: white;
  border: none;
  border-radius: 12px;
  font-weight: 700;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 10px 20px -5px rgba(99, 102, 241, 0.4);
}

.midnight-btn-sm:hover {
  transform: translateY(-2px);
  box-shadow: 0 15px 30px -5px rgba(99, 102, 241, 0.5);
}

.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}

.list-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.list-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
