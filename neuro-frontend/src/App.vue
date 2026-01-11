<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import QrcodeVue from 'qrcode.vue';
import { jsPDF } from 'jspdf';
import ProfileSettings from './components/ProfileSettings.vue';
import Contacts from './components/Contacts.vue';
import Notifications from './components/Notifications.vue';
import RecurringPayments from './components/RecurringPayments.vue';
import PaymentRequests from './components/PaymentRequests.vue';
import PendingApprovals from './components/PendingApprovals.vue';
import Analytics from './components/Analytics.vue';

// --- State Management ---
const user = ref(JSON.parse(localStorage.getItem('wallet_user')) || null);
const token = ref(localStorage.getItem('jwt_token') || null);
const isLoginView = ref(true);
const currentView = ref('dashboard'); // 'dashboard', 'profile', 'contacts', 'notifications', 'recurring', 'payment-requests', 'approvals', 'analytics'
const notificationsRef = ref(null);
const unreadNotifCount = ref(0);
const authForm = ref({ username: '', password: '' });
const isDarkMode = ref(localStorage.getItem('darkMode') === 'true');
const isMobileMenuOpen = ref(false); // Mobile menu state

// Transaction Categories
const categories = ['OTHER', 'RENT', 'FOOD', 'SALARY', 'ENTERTAINMENT', 'UTILITIES', 'SHOPPING', 'HEALTHCARE', 'TRANSPORT', 'EDUCATION'];
const selectedCategory = ref('OTHER');
const transactionNote = ref('');

// Wallet Data
const balance = ref(0);
const recipientInput = ref('');
const amount = ref(0);
const transactions = ref([]);
const sortBy = ref('newest'); 
const isProcessing = ref(false);
const showSuccessToast = ref(false);
const errorMessage = ref('');

// Analytics & Search
const searchCategory = ref('');
const searchQuery = ref('');
const searchDateFrom = ref('');
const searchDateTo = ref('');

// --- Axios Config ---
// Add JWT to all requests
axios.interceptors.request.use(config => {
  if (token.value) {
    config.headers.Authorization = `Bearer ${token.value}`;
  }
  return config;
});

// Handle 401 Unauthorized
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      logout();
      errorMessage.value = "Session expired. Please login again.";
    }
    return Promise.reject(error);
  }
);

// --- Computed ---
const filteredTransactions = computed(() => {
  let list = [...transactions.value];
  
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase();
    list = list.filter(t => 
      t.senderId.toString().includes(q) || 
      t.receiverId.toString().includes(q) ||
      t.amount.toString().includes(q)
    );
  }

  return list.sort((a, b) => {
    return sortBy.value === 'newest' ? b.id - a.id : a.id - b.id;
  });
});

// --- Actions ---
const login = async () => {
  if (isProcessing.value) return;
  isProcessing.value = true;
  errorMessage.value = '';
  
  try {
    const res = await axios.post('http://localhost:8080/api/auth/login', authForm.value);
    
    // Store JWT token and user info
    token.value = res.data.token;
    localStorage.setItem('jwt_token', token.value);
    
    // Construct user object (backend returns id, username, token)
    user.value = {
      id: res.data.userId,
      username: res.data.username
    };
    
    // Fetch full user details to get avatar
    const userRes = await axios.get(`http://localhost:8080/api/users/${user.value.id}`);
    user.value = userRes.data;
    
    localStorage.setItem('wallet_user', JSON.stringify(user.value));
    currentView.value = 'dashboard';
    await fetchWalletData();
  } catch (error) {
    errorMessage.value = error.response?.data?.message || 'Access denied. Check credentials.';
  } finally {
    isProcessing.value = false;
  }
};

const register = async () => {
  if (isProcessing.value) return;
  isProcessing.value = true;
  errorMessage.value = '';
  
  try {
    await axios.post('http://localhost:8080/api/auth/register', authForm.value);
    isLoginView.value = true;
    showToast("Profile Created Successfully");
  } catch (error) {
    errorMessage.value = error.response?.data?.message || 'Registration rejected.';
  } finally {
    isProcessing.value = false;
  }
};

const fetchWalletData = async () => {
  if (!user.value || !token.value) return;
  try {
    // Fetch actual balance from database
    const balanceRes = await axios.get(`http://localhost:8080/api/wallet/balance/${user.value.id}`);
    balance.value = balanceRes.data;
    
    // Fetch transaction history
    const historyRes = await axios.get(`http://localhost:8080/api/wallet/history/${user.value.id}`);
    transactions.value = historyRes.data;
  } catch (error) {
    console.error("DATA_SYNC_ERROR", error);
  }
};

const handleTransfer = async () => {
  if (amount.value > balance.value) {
    errorMessage.value = "Insufficient balance in vault.";
    return;
  }
  if (!recipientInput.value || amount.value <= 0) return;

  isProcessing.value = true;
  try {
    const payload = {
      fromUserId: user.value.id,
      amount: amount.value
    };

    if (!isNaN(recipientInput.value)) {
      payload.toUserId = Number(recipientInput.value);
    } else {
      payload.toUsername = recipientInput.value;
    }

    await axios.post('http://localhost:8080/api/wallet/transfer', payload);
    
    showToast(`Transfer successful: RM ${amount.value.toFixed(2)}`);
    recipientInput.value = '';
    amount.value = 0;
    await fetchWalletData();
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "Transaction rejected by system.";
  } finally {
    isProcessing.value = false;
  }
};

const showToast = (msg) => {
  showSuccessToast.value = true;
  setTimeout(() => showSuccessToast.value = false, 3000);
};

const logout = () => {
  user.value = null;
  token.value = null;
  localStorage.removeItem('wallet_user');
  localStorage.removeItem('jwt_token');
  authForm.value = { username: '', password: '' };
  currentView.value = 'dashboard';
};

const updateUserData = (updatedUser) => {
  user.value = updatedUser;
  localStorage.setItem('wallet_user', JSON.stringify(updatedUser));
};

const handleTransferToContact = (contactUserId, contactNickname) => {
  currentView.value = 'dashboard';
  recipientInput.value = contactUserId.toString();
  // Scroll to transfer section
  setTimeout(() => {
    const transferSection = document.querySelector('.action-card');
    if (transferSection) transferSection.scrollIntoView({ behavior: 'smooth' });
  }, 100);
};

const formatDate = (timestamp) => {
  const date = new Date(timestamp);
  return date.toLocaleString(); // Or format as desired
};

const generateReceipt = (txn) => {
  const doc = new jsPDF();
  
  // Header
  doc.setFillColor(30, 41, 59); // Slate-800
  doc.rect(0, 0, 210, 40, 'F');
  doc.setTextColor(255, 255, 255);
  doc.setFontSize(22);
  doc.text("Neuro-Wallet", 105, 20, { align: "center" });
  
  // Content
  doc.setTextColor(0, 0, 0);
  doc.setFontSize(16);
  doc.text("Transaction Receipt", 105, 60, { align: "center" });
  
  doc.setFontSize(12);
  doc.text(`Reference ID: #${txn.id}`, 20, 80);
  doc.text(`Date: ${formatDate(txn.timestamp)}`, 20, 90);
  doc.text(`Type: ${txn.senderId === user.value.id ? 'Outgoing' : 'Incoming'}`, 20, 100);
  
  doc.setFontSize(14);
  doc.text(`Amount: RM ${txn.amount.toFixed(2)}`, 20, 120);
  
  doc.setFontSize(10);
  doc.setTextColor(100, 100, 100);
  doc.text("This receipt is computer generated and requires no signature.", 105, 280, { align: "center" });

  doc.save(`receipt_${txn.id}.pdf`);
};

// Toggle mobile menu
const toggleMobileMenu = () => {
  isMobileMenuOpen.value = !isMobileMenuOpen.value;
};

// Close mobile menu on navigate
const setView = (view) => {
  currentView.value = view;
  isMobileMenuOpen.value = false;
};

onMounted(() => {
  if (user.value && token.value) fetchWalletData();
});
</script>

<template>
  <div class="midnight-app">
    
    <!-- NOTIFICATION SYSTEM -->
    <Transition name="toast">
      <div v-if="showSuccessToast" class="toast-notification">
        <i class="fas fa-check-circle"></i>
        Action Synchronized
      </div>
    </Transition>

    <!-- DASHBOARD VIEW -->
    <template v-if="user">
      <nav class="midnight-nav">
        <div class="max-w-7xl mx-auto flex justify-between items-center w-full px-6 md:px-12 relative">
          <div class="flex items-center gap-4 cursor-pointer" @click="setView('dashboard')">
            <div class="logo-box-small">
              <i class="fa-solid fa-layer-group"></i>
            </div>
            <div class="flex flex-col">
              <span class="nav-title text-white">Neuro<span class="text-indigo-400">Wallet</span></span>
              <span class="nav-subtitle">Secure Ecosystem</span>
            </div>
          </div>
          
          <!-- Desktop Navigation Menu -->
          <div class="hidden lg:flex items-center gap-2">
            <button @click="setView('dashboard')" :class="['nav-menu-btn', { active: currentView === 'dashboard' }]">
              <i class="fas fa-home"></i>
            </button>
            <button @click="setView('contacts')" :class="['nav-menu-btn', { active: currentView === 'contacts' }]">
              <i class="fas fa-address-book"></i>
            </button>
            <button @click="setView('recurring')" :class="['nav-menu-btn', { active: currentView === 'recurring' }]">
              <i class="fas fa-sync"></i>
            </button>
            <button @click="setView('payment-requests')" :class="['nav-menu-btn', { active: currentView === 'payment-requests' }]">
              <i class="fas fa-qrcode"></i>
            </button>
            <button @click="setView('approvals')" :class="['nav-menu-btn', { active: currentView === 'approvals' }]">
              <i class="fas fa-check-circle"></i>
            </button>
            <button @click="setView('analytics')" :class="['nav-menu-btn', { active: currentView === 'analytics' }]">
              <i class="fas fa-chart-line"></i>
            </button>
            <button @click="setView('notifications')" :class="['nav-menu-btn', { active: currentView === 'notifications' }]">
              <i class="fas fa-bell"></i>
              <span v-if="unreadNotifCount > 0" class="notif-badge">{{ unreadNotifCount }}</span>
            </button>
          </div>

          <!-- Mobile Menu Toggle -->
          <div class="lg:hidden flex items-center">
             <button @click="toggleMobileMenu" class="mobile-menu-toggle">
               <i class="fas" :class="isMobileMenuOpen ? 'fa-times' : 'fa-bars'"></i>
             </button>
          </div>
          
          <div class="hidden lg:flex items-center gap-4">
            <!-- User Profile Menu -->
            <div class="flex items-center gap-3">
              <div @click="currentView = 'profile'" class="flex items-center gap-3 cursor-pointer group">
                <div class="w-10 h-10 rounded-full border border-white/20 overflow-hidden relative">
                   <img :src="user.avatarUrl ? `http://localhost:8080${user.avatarUrl}` : 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y'" 
                        class="w-full h-full object-cover" />
                </div>
                <span class="hidden sm:inline text-xs font-bold text-white/60 group-hover:text-white transition-colors capitalize">
                  {{ user.username }}
                </span>
              </div>
              
              <button @click="logout" class="icon-btn-logout ml-2" title="Disconnect Session">
                <i class="fa-solid fa-power-off"></i>
              </button>
            </div>
          </div>
        </div>
      </nav>

      <!-- Mobile Menu Overlay -->
      <Transition name="fade">
        <div v-if="isMobileMenuOpen" class="mobile-menu-overlay">
          <div class="mobile-menu-content">
            <button @click="setView('dashboard')" :class="['mobile-nav-btn', { active: currentView === 'dashboard' }]">
               <i class="fas fa-home"></i> Home
            </button>
            <button @click="setView('contacts')" :class="['mobile-nav-btn', { active: currentView === 'contacts' }]">
               <i class="fas fa-address-book"></i> Contacts
            </button>
            <button @click="setView('recurring')" :class="['mobile-nav-btn', { active: currentView === 'recurring' }]">
               <i class="fas fa-sync"></i> Recurring
            </button>
            <button @click="setView('payment-requests')" :class="['mobile-nav-btn', { active: currentView === 'payment-requests' }]">
               <i class="fas fa-qrcode"></i> Requests
            </button>
            <button @click="setView('approvals')" :class="['mobile-nav-btn', { active: currentView === 'approvals' }]">
               <i class="fas fa-check-circle"></i> Approvals
            </button>
            <button @click="setView('analytics')" :class="['mobile-nav-btn', { active: currentView === 'analytics' }]">
               <i class="fas fa-chart-line"></i> Analytics
            </button>
            <button @click="setView('notifications')" :class="['mobile-nav-btn', { active: currentView === 'notifications' }]">
               <div class="flex items-center gap-2">
                 <i class="fas fa-bell"></i> Notifications
                 <span v-if="unreadNotifCount > 0" class="mobile-badge">{{ unreadNotifCount }}</span>
               </div>
            </button>
            
            <div class="mobile-menu-divider"></div>
            
            <div class="flex items-center gap-3 px-4 py-3" @click="setView('profile')">
              <div class="w-8 h-8 rounded-full border border-white/20 overflow-hidden relative">
                 <img :src="user.avatarUrl ? `http://localhost:8080${user.avatarUrl}` : 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y'" 
                      class="w-full h-full object-cover" />
              </div>
              <span class="text-white text-sm font-bold">{{ user.username }}</span>
            </div>
            
            <button @click="logout" class="mobile-nav-btn logout">
               <i class="fa-solid fa-power-off"></i> Disconnect
            </button>
          </div>
        </div>
      </Transition>

      <main class="max-w-7xl mx-auto p-6 md:p-12 w-full flex-grow">
        
        <!-- DASHBOARD CONTENT -->
        <div v-if="currentView === 'dashboard'" class="grid grid-cols-1 lg:grid-cols-12 gap-8 md:gap-12 animate-in">
          
          <!-- SIDEBAR: BALANCE & TRANSFER -->
          <div class="lg:col-span-4 space-y-8">
            <div class="midnight-card balance-card">
               <div class="card-glow"></div>
               <span class="label-tiny-center">Liquidity Pool</span>
               <div class="balance-display">
                 <span class="currency">RM</span>
                 <h3 class="amount-large">{{ balance.toFixed(2) }}</h3>
               </div>
               <div class="status-indicator">
                 <div class="pulse-dot"></div>
                 Connected to Network
               </div>
            </div>

            <div class="midnight-card qr-identity-card">
              <h4 class="card-title">Node Identity</h4>
              <div class="flex flex-col items-center gap-4">
                <div class="bg-white p-4 rounded-xl">
                  <qrcode-vue :value="String(user.id)" :size="160" level="H" />
                </div>
                <div class="text-center">
                  <p class="text-purple-400 font-mono text-xl font-bold">Node #{{ user.id }}</p>
                  <p class="text-slate-400 text-xs mt-2 uppercase tracking-wider font-bold">Scan to initiate transfer</p>
                </div>
              </div>
            </div>

            <div class="midnight-card action-card p-10">
              <h4 class="card-title">Initiate Transfer</h4>
              <div class="space-y-8">
                <div class="input-group">
                   <div class="input-wrapper">
                    <input v-model="recipientInput" type="text" id="recipient" placeholder=" " required>
                    <label for="recipient" class="floating-label">Recipient (Node ID or Username)</label>
                    <i class="fa-solid fa-hashtag input-icon"></i>
                  </div>
                </div>
                <div class="input-group">
                   <div class="input-wrapper">
                    <input v-model="amount" type="number" id="amount" placeholder=" " required>
                    <label for="amount" class="floating-label">Asset Volume (RM)</label>
                    <i class="fa-solid fa-coins input-icon"></i>
                  </div>
                </div>
                
                <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

                <button @click="handleTransfer" :disabled="isProcessing" class="midnight-btn py-5">
                  <span v-if="!isProcessing">Execute Contract</span>
                  <i v-else class="fas fa-circle-notch animate-spin"></i>
                </button>
              </div>
            </div>
          </div>

          <!-- MAIN AREA: SYSTEM LEDGER -->
          <div class="lg:col-span-8 space-y-8">
            <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-6 px-2">
               <h3 class="section-title">System Ledger</h3>
               <div class="flex items-center gap-4">
                 <div class="relative search-wrapper">
                   <input v-model="searchQuery" type="text" placeholder="Filter records..." class="search-input" />
                   <i class="fas fa-search search-icon"></i>
                 </div>
               </div>
            </div>

            <div class="space-y-4">
              <TransitionGroup name="list">
                <div v-for="t in filteredTransactions" :key="t.id" class="ledger-item group hover:border-white/20">
                  <div class="flex items-center gap-6">
                    <div :class="t.senderId === user.id ? 'icon-box-debit' : 'icon-box-credit'">
                      <i :class="t.senderId === user.id ? 'fa-solid fa-arrow-up' : 'fa-solid fa-arrow-down'"></i>
                    </div>
                    <div>
                      <p class="ledger-title text-white/90 group-hover:text-white transition-colors">
                        {{ t.senderId === user.id ? 'Asset Transfer to Node #' + t.receiverId : 'Received Assets from Node #' + t.senderId }}
                      </p>
                      <p class="ledger-subtitle uppercase tracking-widest font-bold">SHA-256: 00{{ t.id }}</p>
                    </div>
                  </div>
                  <div class="text-right">
                <p class="font-bold" :class="t.senderId === user.id ? 'amount-debit' : 'amount-credit'">
                  {{ t.senderId === user.id ? '-' : '+' }} RM {{ t.amount.toFixed(2) }}
                </p>
                <p class="ledger-status">{{ formatDate(t.timestamp) }}</p>
                <button @click="generateReceipt(t)" class="text-[10px] text-purple-400 hover:text-purple-300 mt-1">
                  <i class="fa-solid fa-file-pdf mr-1"></i> Receipt
                </button>
              </div>
                </div>
              </TransitionGroup>

              <div v-if="filteredTransactions.length === 0" class="empty-state">
                <i class="fa-solid fa-database text-4xl mb-6 opacity-10"></i>
                <p class="uppercase tracking-widest font-black text-xs opacity-40">No activity detected in stream</p>
              </div>
            </div>
          </div>
        </div>

        <!-- PROFILE VIEW -->
        <div v-else-if="currentView === 'profile'" class="max-w-4xl mx-auto animate-in">
          <ProfileSettings :user="user" @update-user="updateUserData" @logout="logout" />
        </div>

        <!-- CONTACTS VIEW -->
        <div v-else-if="currentView === 'contacts'" class="animate-in">
          <Contacts :user="user" @transfer-to-contact="handleTransferToContact" />
        </div>

        <!-- NOTIFICATIONS VIEW -->
        <div v-else-if="currentView === 'notifications'" class="animate-in">
          <Notifications ref="notificationsRef" :user="user" />
        </div>

        <!-- RECURRING PAYMENTS VIEW -->
        <div v-else-if="currentView === 'recurring'" class="animate-in">
          <RecurringPayments :user="user" />
        </div>

        <!-- PAYMENT REQUESTS VIEW -->
        <div v-else-if="currentView === 'payment-requests'" class="animate-in">
          <PaymentRequests :user="user" />
        </div>

        <!-- PENDING APPROVALS VIEW -->
        <div v-else-if="currentView === 'approvals'" class="animate-in">
          <PendingApprovals :user="user" />
        </div>

        <!-- ANALYTICS VIEW -->
        <div v-else-if="currentView === 'analytics'" class="animate-in">
          <Analytics :user="user" />
        </div>

      </main>
    </template>

    <!-- AUTHENTICATION VIEW -->
    <template v-else>
      <div class="auth-container">
        <div class="login-card animate-in">
          <div class="login-header">
            <div class="logo-container-large">
              <i class="fa-solid fa-layer-group"></i>
            </div>
            <h2>{{ isLoginView ? 'System Login' : 'Register Node' }}</h2>
            <p>{{ isLoginView ? 'Link your identity to access the secure NeuroCore network.' : 'Create a new encrypted node profile to begin transactions.' }}</p>
          </div>

          <form @submit.prevent class="space-y-8">
            <div class="input-group">
              <div class="input-wrapper">
                <input v-model="authForm.username" type="text" id="username" placeholder=" " required autocomplete="off">
                <label for="username" class="floating-label">Username / Identity</label>
                <i class="fa-regular fa-user input-icon"></i>
              </div>
            </div>

            <div class="input-group">
              <div class="input-wrapper">
                <input v-model="authForm.password" type="password" id="passwordInput" placeholder=" " required>
                <label for="passwordInput" class="floating-label">Access Passphrase</label>
                <i class="fa-solid fa-lock input-icon"></i>
              </div>
            </div>

            <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

            <button @click="isLoginView ? login() : register()" type="button" class="midnight-btn" :disabled="isProcessing">
              <span v-if="!isProcessing">{{ isLoginView ? 'Establish Link' : 'Register Identity' }}</span>
              <i v-else class="fas fa-circle-notch animate-spin"></i>
            </button>
          </form>

          <div class="login-footer">
            <p>
              {{ isLoginView ? 'Unauthorized?' : 'Already Registered?' }}
              <a href="#" @click.prevent="isLoginView = !isLoginView">
                {{ isLoginView ? 'Request Access' : 'Return to Login' }}
              </a>
            </p>
          </div>
        </div>
      </div>
    </template>


  </div>
</template>

<style>
/* --- MIDNIGHT WALLET DESIGN SYSTEM --- */
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css');

:root {
  --bg-gradient-start: #0f172a;
  --bg-gradient-end: #1e293b;
  --card-bg: rgba(30, 41, 59, 0.7);
  --input-bg: rgba(15, 23, 42, 0.6);
  --text-main: #f8fafc;
  --text-muted: #94a3b8;
  --accent-primary: #6366f1;
  --accent-secondary: #8b5cf6;
  --accent-glow: rgba(99, 102, 241, 0.4);
  --radius-md: 16px;
  --radius-lg: 32px;
}

* { margin: 0; padding: 0; box-sizing: border-box; }

.midnight-app {
  height: 100vh;
  width: 100%;
  display: flex;
  flex-direction: column;
  font-family: 'Plus Jakarta Sans', sans-serif;
  color: var(--text-main);
  background: radial-gradient(circle at top center, var(--bg-gradient-end), var(--bg-gradient-start)) fixed;
  overflow: auto;
}

/* Glass Cards */
.midnight-card, .login-card {
  background: var(--card-bg);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 40px 100px -20px rgba(0, 0, 0, 0.6);
  position: relative;
  overflow: hidden;
}

.midnight-nav {
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(15, 23, 42, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: 1.25rem 0;
  position: sticky;
  top: 0;
  z-index: 50;
}

.auth-container {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}

.login-card {
  width: 100%;
  max-width: 460px;
  padding: 2.5rem 2.5rem;
}

/* Typography */
.nav-title { font-weight: 800; font-size: 1.5rem; letter-spacing: -1px; }
.nav-subtitle { font-size: 9px; color: var(--text-muted); font-weight: 800; text-transform: uppercase; letter-spacing: 0.1em; }
.card-title { font-weight: 800; font-size: 1.1rem; color: white; margin-bottom: 1.5rem; text-transform: uppercase; letter-spacing: 0.05em; }
.section-title { font-weight: 800; font-size: 2rem; letter-spacing: -1px; color: white; }
.label-tiny { font-size: 9px; font-weight: 800; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.2em; }
.label-tiny-center { display: block; font-size: 10px; font-weight: 800; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.3em; text-align: center; }

/* Dashboard Elements */
.balance-card { padding: 4rem 2rem; border-color: rgba(99, 102, 241, 0.2); }
.qr-identity-card { padding: 2.5rem 2rem; border-color: rgba(139, 92, 246, 0.2); background: linear-gradient(135deg, rgba(99, 102, 241, 0.05), rgba(139, 92, 246, 0.05)); }
.qr-identity-card .bg-white { box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3); }
.action-card { border-color: rgba(255, 255, 255, 0.08); }
.amount-large { font-size: 4rem; font-weight: 800; letter-spacing: -0.05em; color: white; line-height: 1; }
.currency { font-size: 1.5rem; font-weight: 800; color: var(--accent-primary); }
.balance-display { display: flex; align-items: center; justify-content: center; gap: 10px; margin-top: 1rem; }
.status-indicator { margin-top: 2.5rem; display: flex; align-items: center; justify-content: center; gap: 10px; font-size: 11px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.1em; }
.pulse-dot { width: 8px; height: 8px; background: #10b981; border-radius: 50%; box-shadow: 0 0 12px #10b981; animation: pulse 2s infinite; }

.ledger-item {
  background: rgba(255, 255, 255, 0.02);
  padding: 1.5rem 2rem;
  border-radius: var(--radius-md);
  border: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.ledger-title { font-weight: 700; font-size: 1.1rem; }
.ledger-subtitle { font-size: 10px; color: var(--text-muted); font-weight: 800; margin-top: 4px; letter-spacing: 0.1em; }
.ledger-status { font-size: 9px; font-weight: 800; text-transform: uppercase; color: var(--text-muted); letter-spacing: 0.2em; margin-top: 6px; }
.amount-debit { font-weight: 800; font-size: 1.35rem; color: #f87171; } /* Changed to red for debit */
.amount-credit { font-weight: 800; font-size: 1.35rem; color: #10b981; }

/* Icons */
.logo-container-large {
  width: 56px; height: 56px;
  margin: 0 auto 1.5rem;
  background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
  border-radius: 16px;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 15px 30px -8px var(--accent-glow);
  font-size: 1.5rem; color: white;
}
.logo-box-small {
  width: 44px; height: 44px;
  background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  color: white; font-size: 1.25rem;
}
.icon-box-debit, .icon-box-credit {
  width: 56px; height: 56px; border-radius: 16px; border: 1px solid; display: flex; align-items: center; justify-content: center; font-size: 1.2rem;
}
.icon-box-debit { background: rgba(249, 115, 22, 0.1); border-color: rgba(249, 115, 22, 0.2); color: #f97316; }
.icon-box-credit { background: rgba(16, 185, 129, 0.1); border-color: rgba(16, 185, 129, 0.2); color: #10b981; }

/* Form Elements */
.input-wrapper { position: relative; width: 100%; }
.input-wrapper input {
  width: 100%;
  padding: 1.1rem 1rem 1.1rem 3.5rem;
  background: var(--input-bg);
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: var(--radius-md);
  font-size: 0.95rem;
  color: white;
  outline: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.input-icon { position: absolute; left: 1.25rem; top: 50%; transform: translateY(-50%); color: var(--text-muted); font-size: 1.1rem; transition: color 0.3s; }
.floating-label { position: absolute; left: 3.5rem; top: 50%; transform: translateY(-50%); color: var(--text-muted); font-size: 0.9rem; pointer-events: none; transition: all 0.3s ease; }

.input-wrapper input:focus + .floating-label,
.input-wrapper input:not(:placeholder-shown) + .floating-label {
  top: 0; left: 1.2rem; transform: translateY(-50%) scale(0.75);
  background: #151e33;
  padding: 0 10px; border-radius: 6px;
  color: var(--accent-primary); font-weight: 800;
}
.input-wrapper input:focus { border-color: var(--accent-primary); background: rgba(15, 23, 42, 0.85); box-shadow: 0 0 0 5px rgba(99, 102, 241, 0.12); }
.input-wrapper input:focus ~ .input-icon { color: white; }

.midnight-btn {
  width: 100%;
  padding: 1.25rem;
  background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
  color: white; border: none; border-radius: var(--radius-md);
  font-size: 1rem; font-weight: 800; text-transform: uppercase; letter-spacing: 0.1em;
  cursor: pointer;
  box-shadow: 0 15px 30px -5px var(--accent-glow);
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.midnight-btn:hover:not(:disabled) { transform: translateY(-3px); box-shadow: 0 20px 40px -5px var(--accent-glow); filter: brightness(1.15); }
.midnight-btn:active { transform: translateY(0); }
.midnight-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.icon-btn-logout {
  width: 44px; height: 44px; background: rgba(255, 255, 255, 0.05); border-radius: 14px;
  color: var(--text-muted); display: flex; align-items: center; justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.1); transition: all 0.3s; cursor: pointer;
}
.icon-btn-logout:hover { background: #f87171; border-color: #ef4444; color: white; box-shadow: 0 0 20px rgba(239, 68, 68, 0.4); }

.search-input {
  background: rgba(15, 23, 42, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 0.8rem 1.5rem 0.8rem 3.2rem;
  font-size: 0.9rem; color: white; outline: none; width: 220px; transition: all 0.4s;
}
.search-input:focus { width: 320px; border-color: var(--accent-primary); background: rgba(15, 23, 42, 0.8); }
.search-icon { position: absolute; left: 1.25rem; top: 50%; transform: translateY(-50%); color: var(--text-muted); font-size: 0.9rem; }

/* Utilities */
.error-text { font-size: 13px; color: #f87171; font-weight: 700; text-align: center; background: rgba(248, 113, 113, 0.1); padding: 0.75rem; border-radius: 12px; margin-top: 1.5rem; }

/* Spacing utilities (replacing Tailwind) */
.input-group { margin-bottom: 1rem; }
.input-group:last-of-type { margin-bottom: 0; }
form.space-y-8 > * + * { margin-top: 1.25rem; }
.login-header { margin-bottom: 1.5rem; }
.login-header h2 { font-size: 1.5rem; font-weight: 800; margin-bottom: 0.5rem; }
.login-header p { color: var(--text-muted); font-size: 0.9rem; line-height: 1.5; }

.login-footer { margin-top: 2rem; text-align: center; border-top: 1px solid rgba(255,255,255,0.05); padding-top: 1.5rem; font-size: 0.9rem; color: var(--text-muted); }
.login-footer a { color: var(--accent-secondary); text-decoration: none; font-weight: 800; margin-left: 5px; transition: all 0.2s; }
.login-footer a:hover { color: white; text-decoration: underline; }

.toast-notification {
  position: fixed; top: 32px; left: 50%; transform: translateX(-50%); z-index: 100;
  background: white; color: #0f172a; padding: 14px 32px; border-radius: 50px;
  font-weight: 800; font-size: 13px; text-transform: uppercase; letter-spacing: 0.15em;
  box-shadow: 0 30px 60px rgba(0,0,0,0.5); display: flex; align-items: center; gap: 14px;
}

.empty-state { text-align: center; padding: 6rem 2rem; background: rgba(255, 255, 255, 0.01); border-radius: var(--radius-md); border: 2px dashed rgba(255, 255, 255, 0.05); }

/* Animations */
@keyframes pulse { 0% { transform: scale(0.9); opacity: 0.7; } 50% { transform: scale(1.1); opacity: 1; } 100% { transform: scale(0.9); opacity: 0.7; } }
.animate-in { animation: entrance 1s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
@keyframes entrance { from { opacity: 0; transform: translateY(60px); filter: blur(10px); } to { opacity: 1; transform: translateY(0); filter: blur(0); } }

/* Scrollbar */
::-webkit-scrollbar { width: 10px; }
::-webkit-scrollbar-track { background: var(--bg-gradient-start); }
::-webkit-scrollbar-thumb { background: #334155; border-radius: 10px; border: 3px solid var(--bg-gradient-start); }

input::-webkit-outer-spin-button, input::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }

/* Tailwind utility replacements */
.flex { display: flex; }
.flex-col { flex-direction: column; }
.flex-grow { flex-grow: 1; }
.items-center { align-items: center; }
.items-end { align-items: flex-end; }
.justify-between { justify-content: space-between; }
.justify-center { justify-content: center; }
.gap-3 { gap: 0.75rem; }
.gap-4 { gap: 1rem; }
.gap-6 { gap: 1.5rem; }
.gap-8 { gap: 2rem; }
.gap-10 { gap: 2.5rem; }
.gap-12 { gap: 3rem; }
.grid { display: grid; }
.grid-cols-1 { grid-template-columns: repeat(1, minmax(0, 1fr)); }
.relative { position: relative; }
.text-right { text-align: right; }
.text-center { text-align: center; }
.text-white { color: white; }
.text-xs { font-size: 0.75rem; }
.text-4xl { font-size: 2.25rem; }
.font-bold { font-weight: 700; }
.font-black { font-size: 900; }
.uppercase { text-transform: uppercase; }
.capitalize { text-transform: capitalize; }
.tracking-widest { letter-spacing: 0.1em; }
.opacity-10 { opacity: 0.1; }
.opacity-40 { opacity: 0.4; }
.cursor-pointer { cursor: pointer; }
.transition-colors { transition: color 0.3s; }
.w-full { width: 100%; }
.w-10 { width: 2.5rem; }
.h-full { height: 100%; }
.h-10 { height: 2.5rem; }
.rounded-full { border-radius: 9999px; }
.overflow-hidden { overflow: hidden; }
.object-cover { object-fit: cover; }
.max-w-7xl { max-width: 80rem; }
.mx-auto { margin-left: auto; margin-right: auto; }
.p-6 { padding: 1.5rem; }
.p-10 { padding: 2.5rem; }
.px-2 { padding-left: 0.5rem; padding-right: 0.5rem; }
.px-6 { padding-left: 1.5rem; padding-right: 1.5rem; }
.py-5 { padding-top: 1.25rem; padding-bottom: 1.25rem; }
.pr-8 { padding-right: 2rem; }
.mb-6 { margin-bottom: 1.5rem; }
.space-y-4 > * + * { margin-top: 1rem; }
.space-y-8 > * + * { margin-top: 2rem; }
.border-r { border-right-width: 1px; border-right-style: solid; }
.border-white\/10 { border-color: rgba(255, 255, 255, 0.1); }
.group:hover .group-hover\:text-white { color: white; }
.text-white\/60 { color: rgba(255, 255, 255, 0.6); }
.text-white\/90 { color: rgba(255, 255, 255, 0.9); }
.text-indigo-400 { color: #818cf8; }
.animate-spin { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.hidden { display: none; }
.inline { display: inline; }
.node-id { font-size: 1.1rem; font-weight: 800; color: white; letter-spacing: -0.02em; }

/* Media queries for responsive utilities */
@media (min-width: 640px) {
  .sm\:flex-row { flex-direction: row; }
  .sm\:items-center { align-items: center; }
  .sm\:inline { display: inline; }
}
@media (min-width: 768px) {
  .md\:flex { display: flex; }
  .md\:p-12 { padding: 3rem; }
  .md\:px-12 { padding-left: 3rem; padding-right: 3rem; }
  .md\:gap-8 { gap: 2rem; }
  .md\:gap-12 { gap: 3rem; }
}
@media (min-width: 1024px) {
  .lg\:col-span-4 { grid-column: span 4 / span 4; }
  .lg\:col-span-8 { grid-column: span 8 / span 8; }
  .lg\:grid-cols-12 { grid-template-columns: repeat(12, minmax(0, 1fr)); }
  .lg\:flex { display: flex; }
  .lg\:hidden { display: none; }
}

/* Navigation Menu Buttons */
.nav-menu-btn {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  font-size: 1.1rem;
}

.nav-menu-btn:hover {
  background: rgba(99, 102, 241, 0.15);
  border-color: rgba(99, 102, 241, 0.3);
  color: #6366f1;
  transform: translateY(-2px);
}

.nav-menu-btn.active {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-color: transparent;
  color: white;
  box-shadow: 0 10px 20px -5px rgba(99, 102, 241, 0.4);
}

.notif-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ef4444;
  color: white;
  font-size: 10px;
  font-weight: 800;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.6);
}

/* Mobile Navigation Styles */
.mobile-menu-toggle {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  cursor: pointer;
  transition: all 0.3s;
}
.mobile-menu-toggle:active { transform: scale(0.95); }

.mobile-menu-overlay {
  position: fixed;
  top: 85px; /* Height of nav bar approx */
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.95);
  backdrop-filter: blur(20px);
  z-index: 40;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.mobile-menu-content {
  background: rgba(30, 41, 59, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 24px;
  padding: 0.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.mobile-nav-btn {
  width: 100%;
  padding: 1rem 1.5rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  background: transparent;
  border: none;
  border-radius: 16px;
  color: #94a3b8;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
}

.mobile-nav-btn i { width: 24px; text-align: center; font-size: 1.1rem; }

.mobile-nav-btn:hover {
  background: rgba(255, 255, 255, 0.05);
  color: white;
}

.mobile-nav-btn.active {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.2), rgba(139, 92, 246, 0.2));
  color: #818cf8;
  border: 1px solid rgba(99, 102, 241, 0.3);
}

.mobile-nav-btn.logout {
  margin-top: 0.5rem;
  color: #f87171;
}
.mobile-nav-btn.logout:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.mobile-menu-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.1);
  margin: 0.5rem 1rem;
}

.mobile-badge {
  background: #ef4444;
  color: white;
  font-size: 0.75rem;
  font-weight: 800;
  padding: 2px 8px;
  border-radius: 12px;
}

/* Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>