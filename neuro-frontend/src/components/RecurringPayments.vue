<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

const props = defineProps(['user']);

const recurringPayments = ref([]);
const isLoading = ref(false);
const showCreateModal = ref(false);
const newPayment = ref({
  recipientId: '',
  amount: '',
  frequency: 'MONTHLY',
  category: 'OTHER',
  note: ''
});
const errorMessage = ref('');
const successMessage = ref('');

const frequencies = ['DAILY', 'WEEKLY', 'MONTHLY'];
const categories = ['OTHER', 'RENT', 'UTILITIES', 'SUBSCRIPTION', 'SALARY', 'INSURANCE'];

const fetchRecurringPayments = async () => {
  isLoading.value = true;
  try {
    const res = await axios.get(`http://localhost:8080/api/wallet/recurring/${props.user.id}`);
    recurringPayments.value = res.data;
  } catch (error) {
    console.error('Failed to fetch recurring payments', error);
  } finally {
    isLoading.value = false;
  }
};

const createRecurringPayment = async () => {
  if (!newPayment.value.recipientId || !newPayment.value.amount) {
    showMessage('Please fill in all required fields', true);
    return;
  }

  try {
    const payload = {
      userId: props.user.id,
      recipientId: Number(newPayment.value.recipientId),
      amount: Number(newPayment.value.amount),
      frequency: newPayment.value.frequency,
      category: newPayment.value.category,
      note: newPayment.value.note
    };

    await axios.post('http://localhost:8080/api/wallet/recurring', payload);
    showMessage('Recurring payment created successfully', false);
    showCreateModal.value = false;
    newPayment.value = {
      recipientId: '',
      amount: '',
      frequency: 'MONTHLY',
      category: 'OTHER',
      note: ''
    };
    await fetchRecurringPayments();
  } catch (error) {
    showMessage(error.response?.data?.message || 'Failed to create recurring payment', true);
  }
};

const cancelPayment = async (paymentId) => {
  if (!confirm('Are you sure you want to cancel this recurring payment?')) return;

  try {
    await axios.delete(`http://localhost:8080/api/wallet/recurring/${paymentId}`);
    showMessage('Recurring payment cancelled', false);
    await fetchRecurringPayments();
  } catch (error) {
    showMessage('Failed to cancel payment', true);
  }
};

const showMessage = (msg, isError) => {
  if (isError) {
    errorMessage.value = msg;
    setTimeout(() => errorMessage.value = '', 3000);
  } else {
    successMessage.value = msg;
    setTimeout(() => successMessage.value = '', 3000);
  }
};

const formatDate = (dateStr) => {
  const date = new Date(dateStr);
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
};

const getFrequencyIcon = (freq) => {
  const icons = {
    DAILY: 'fa-calendar-day',
    WEEKLY: 'fa-calendar-week',
    MONTHLY: 'fa-calendar'
  };
  return icons[freq] || 'fa-sync';
};

const getCategoryColor = (category) => {
  const colors = {
    RENT: 'orange',
    UTILITIES: 'blue',
    SUBSCRIPTION: 'purple',
    SALARY: 'green',
    INSURANCE: 'red',
    OTHER: 'slate'
  };
  return colors[category] || 'slate';
};

onMounted(() => {
  fetchRecurringPayments();
});
</script>

<template>
  <div class="recurring-container">
    <div class="flex justify-between items-center mb-8">
      <div>
        <h3 class="section-title">Recurring Payments</h3>
        <p class="text-slate-400 text-sm mt-2">Manage automated scheduled payments</p>
      </div>
      <button @click="showCreateModal = true" class="midnight-btn-sm">
        <i class="fas fa-plus mr-2"></i> New Recurring Payment
      </button>
    </div>

    <!-- Messages -->
    <div v-if="successMessage" class="success-message mb-4">
      <i class="fas fa-check-circle mr-2"></i> {{ successMessage }}
    </div>
    <div v-if="errorMessage" class="error-text mb-4">{{ errorMessage }}</div>

    <!-- Loading State -->
    <div v-if="isLoading" class="text-center py-12">
      <i class="fas fa-circle-notch fa-spin text-4xl text-indigo-400"></i>
      <p class="text-slate-400 mt-4">Loading recurring payments...</p>
    </div>

    <!-- Recurring Payments List -->
    <div v-else class="space-y-4">
      <div v-for="payment in recurringPayments" :key="payment.id" class="payment-card">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4">
            <div :class="['payment-icon', getCategoryColor(payment.category)]">
              <i :class="['fas', getFrequencyIcon(payment.frequency)]"></i>
            </div>
            <div>
              <div class="flex items-center gap-3">
                <h5 class="payment-title">To Node #{{ payment.recipientId }}</h5>
                <span :class="['status-badge', payment.isActive ? 'active' : 'inactive']">
                  {{ payment.isActive ? 'Active' : 'Inactive' }}
                </span>
              </div>
              <p class="payment-details">
                {{ payment.frequency }} • {{ payment.category }} 
                <span v-if="payment.note" class="text-slate-500">• {{ payment.note }}</span>
              </p>
              <p class="payment-next">Next payment: {{ formatDate(payment.nextPaymentDate) }}</p>
            </div>
          </div>
          <div class="flex items-center gap-6">
            <div class="text-right">
              <p class="payment-amount">RM {{ payment.amount.toFixed(2) }}</p>
              <p class="payment-frequency">{{ payment.frequency.toLowerCase() }}</p>
            </div>
            <button 
              v-if="payment.isActive" 
              @click="cancelPayment(payment.id)" 
              class="icon-btn delete"
              title="Cancel"
            >
              <i class="fas fa-times"></i>
            </button>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="recurringPayments.length === 0" class="empty-state">
        <i class="fas fa-sync text-6xl mb-6 opacity-10"></i>
        <p class="uppercase tracking-widest font-black text-xs opacity-40">No recurring payments</p>
        <button @click="showCreateModal = true" class="midnight-btn mt-6">
          Create Your First Recurring Payment
        </button>
      </div>
    </div>

    <!-- Create Modal -->
    <Transition name="modal">
      <div v-if="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
        <div class="modal-content">
          <div class="flex justify-between items-center mb-6">
            <h3 class="text-xl font-bold text-white">New Recurring Payment</h3>
            <button @click="showCreateModal = false" class="text-slate-400 hover:text-white">
              <i class="fas fa-times text-xl"></i>
            </button>
          </div>

          <div class="space-y-6">
            <div class="input-group">
              <div class="input-wrapper">
                <input v-model="newPayment.recipientId" type="number" id="recipientId" placeholder=" " required>
                <label for="recipientId" class="floating-label">Recipient Node ID</label>
                <i class="fa-solid fa-hashtag input-icon"></i>
              </div>
            </div>

            <div class="input-group">
              <div class="input-wrapper">
                <input v-model="newPayment.amount" type="number" step="0.01" id="amount" placeholder=" " required>
                <label for="amount" class="floating-label">Amount (RM)</label>
                <i class="fa-solid fa-coins input-icon"></i>
              </div>
            </div>

            <div class="input-group">
              <label class="block text-xs text-indigo-300 uppercase font-bold tracking-widest mb-2 pl-1">Frequency</label>
              <select v-model="newPayment.frequency" class="select-input">
                <option v-for="freq in frequencies" :key="freq" :value="freq">{{ freq }}</option>
              </select>
            </div>

            <div class="input-group">
              <label class="block text-xs text-indigo-300 uppercase font-bold tracking-widest mb-2 pl-1">Category</label>
              <select v-model="newPayment.category" class="select-input">
                <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
              </select>
            </div>

            <div class="input-group">
              <div class="input-wrapper">
                <input v-model="newPayment.note" type="text" id="note" placeholder=" ">
                <label for="note" class="floating-label">Note (Optional)</label>
                <i class="fa-solid fa-sticky-note input-icon"></i>
              </div>
            </div>

            <button @click="createRecurringPayment" class="midnight-btn w-full">
              <i class="fas fa-plus mr-2"></i> Create Recurring Payment
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.recurring-container {
  animation: fadeIn 0.5s ease-out;
}

.payment-card {
  background: rgba(255, 255, 255, 0.02);
  padding: 1.5rem;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.payment-card:hover {
  border-color: rgba(99, 102, 241, 0.3);
  background: rgba(99, 102, 241, 0.05);
  transform: translateY(-2px);
}

.payment-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.3rem;
  flex-shrink: 0;
}

.payment-icon.orange {
  background: rgba(249, 115, 22, 0.1);
  border: 1px solid rgba(249, 115, 22, 0.2);
  color: #f97316;
}

.payment-icon.blue {
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.2);
  color: #3b82f6;
}

.payment-icon.purple {
  background: rgba(168, 85, 247, 0.1);
  border: 1px solid rgba(168, 85, 247, 0.2);
  color: #a855f7;
}

.payment-icon.green {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.2);
  color: #10b981;
}

.payment-icon.red {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.payment-icon.slate {
  background: rgba(148, 163, 184, 0.1);
  border: 1px solid rgba(148, 163, 184, 0.2);
  color: #94a3b8;
}

.payment-title {
  font-weight: 700;
  font-size: 1.05rem;
  color: white;
}

.payment-details {
  font-size: 0.8rem;
  color: #94a3b8;
  margin-top: 4px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.payment-next {
  font-size: 0.75rem;
  color: #6366f1;
  margin-top: 6px;
  font-weight: 700;
}

.payment-amount {
  font-weight: 800;
  font-size: 1.4rem;
  color: white;
}

.payment-frequency {
  font-size: 0.7rem;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-weight: 700;
  margin-top: 2px;
}

.status-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-size: 0.65rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.status-badge.active {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.3);
  color: #10b981;
}

.status-badge.inactive {
  background: rgba(148, 163, 184, 0.1);
  border: 1px solid rgba(148, 163, 184, 0.3);
  color: #94a3b8;
}

.icon-btn {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}

.icon-btn.delete:hover {
  background: rgba(239, 68, 68, 0.2);
  border-color: #ef4444;
  color: #ef4444;
}

.select-input {
  width: 100%;
  padding: 1rem;
  background: rgba(15, 23, 42, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  color: white;
  font-weight: 500;
  outline: none;
  transition: all 0.3s;
}

.select-input:focus {
  border-color: #6366f1;
  background: rgba(15, 23, 42, 0.8);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
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

.success-message {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.3);
  color: #10b981;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  font-size: 0.875rem;
  font-weight: 700;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal-content {
  background: rgba(30, 41, 59, 0.95);
  backdrop-filter: blur(24px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 2rem;
  max-width: 520px;
  width: 90%;
  box-shadow: 0 40px 100px -20px rgba(0, 0, 0, 0.8);
}

.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-content,
.modal-leave-to .modal-content {
  transform: scale(0.9) translateY(20px);
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
