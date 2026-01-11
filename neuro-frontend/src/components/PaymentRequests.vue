<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import QrcodeVue from 'qrcode.vue';

const props = defineProps(['user']);

const paymentRequests = ref([]);
const isLoading = ref(false);
const showCreateModal = ref(false);
const showQRModal = ref(false);
const selectedRequest = ref(null);
const newRequest = ref({
  amount: '',
  note: '',
  expiryHours: 24
});
const errorMessage = ref('');
const successMessage = ref('');

const fetchPaymentRequests = async () => {
  isLoading.value = true;
  try {
    const res = await axios.get(`http://localhost:8080/api/wallet/payment-requests/${props.user.id}`);
    paymentRequests.value = res.data;
  } catch (error) {
    console.error('Failed to fetch payment requests', error);
  } finally {
    isLoading.value = false;
  }
};

const createPaymentRequest = async () => {
  if (!newRequest.value.amount) {
    showMessage('Please enter an amount', true);
    return;
  }

  try {
    const payload = {
      requesterId: props.user.id,
      amount: Number(newRequest.value.amount),
      note: newRequest.value.note,
      expiryHours: Number(newRequest.value.expiryHours)
    };

    await axios.post('http://localhost:8080/api/wallet/payment-request', payload);
    showMessage('Payment request created successfully', false);
    showCreateModal.value = false;
    newRequest.value = { amount: '', note: '', expiryHours: 24 };
    await fetchPaymentRequests();
  } catch (error) {
    showMessage(error.response?.data?.message || 'Failed to create payment request', true);
  }
};

const cancelRequest = async (requestId) => {
  if (!confirm('Are you sure you want to cancel this payment request?')) return;

  try {
    await axios.delete(`http://localhost:8080/api/wallet/payment-request/${requestId}?userId=${props.user.id}`);
    showMessage('Payment request cancelled', false);
    await fetchPaymentRequests();
  } catch (error) {
    showMessage('Failed to cancel request', true);
  }
};

const showQRCode = (request) => {
  selectedRequest.value = request;
  showQRModal.value = true;
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
  return date.toLocaleString();
};

const getStatusColor = (status) => {
  const colors = {
    PENDING: 'blue',
    PAID: 'green',
    EXPIRED: 'orange',
    CANCELLED: 'red'
  };
  return colors[status] || 'slate';
};

const getQRData = (request) => {
  // Generate QR data with request ID for payment
  return JSON.stringify({
    type: 'PAYMENT_REQUEST',
    requestId: request.id,
    requesterId: request.requesterId,
    amount: request.amount,
    note: request.note
  });
};

onMounted(() => {
  fetchPaymentRequests();
});
</script>

<template>
  <div class="payment-requests-container">
    <div class="flex justify-between items-center mb-8">
      <div>
        <h3 class="section-title">Payment Requests</h3>
        <p class="text-slate-400 text-sm mt-2">Create QR-scannable payment requests</p>
      </div>
      <button @click="showCreateModal = true" class="midnight-btn-sm">
        <i class="fas fa-qrcode mr-2"></i> New Payment Request
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
      <p class="text-slate-400 mt-4">Loading payment requests...</p>
    </div>

    <!-- Payment Requests List -->
    <div v-else class="space-y-4">
      <div v-for="request in paymentRequests" :key="request.id" class="request-card">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4">
            <div class="request-qr-preview" @click="showQRCode(request)">
              <i class="fas fa-qrcode text-2xl"></i>
            </div>
            <div>
              <div class="flex items-center gap-3">
                <h5 class="request-title">RM {{ request.amount.toFixed(2) }}</h5>
                <span :class="['status-badge', getStatusColor(request.status)]">
                  {{ request.status }}
                </span>
              </div>
              <p class="request-note" v-if="request.note">{{ request.note }}</p>
              <p class="request-details">
                Created: {{ formatDate(request.createdAt) }}
              </p>
              <p class="request-details" v-if="request.expiresAt">
                Expires: {{ formatDate(request.expiresAt) }}
              </p>
              <p class="request-details" v-if="request.paidAt">
                Paid: {{ formatDate(request.paidAt) }}
              </p>
            </div>
          </div>
          <div class="flex gap-3">
            <button 
              v-if="request.status === 'PENDING'" 
              @click="showQRCode(request)" 
              class="icon-btn qr"
              title="Show QR Code"
            >
              <i class="fas fa-qrcode"></i>
            </button>
            <button 
              v-if="request.status === 'PENDING'" 
              @click="cancelRequest(request.id)" 
              class="icon-btn delete"
              title="Cancel"
            >
              <i class="fas fa-times"></i>
            </button>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="paymentRequests.length === 0" class="empty-state">
        <i class="fas fa-file-invoice-dollar text-6xl mb-6 opacity-10"></i>
        <p class="uppercase tracking-widest font-black text-xs opacity-40">No payment requests</p>
        <button @click="showCreateModal = true" class="midnight-btn mt-6">
          Create Your First Payment Request
        </button>
      </div>
    </div>

    <!-- Create Modal -->
    <Transition name="modal">
      <div v-if="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
        <div class="modal-content">
          <div class="flex justify-between items-center mb-6">
            <h3 class="text-xl font-bold text-white">New Payment Request</h3>
            <button @click="showCreateModal = false" class="text-slate-400 hover:text-white">
              <i class="fas fa-times text-xl"></i>
            </button>
          </div>

          <div class="space-y-6">
            <div class="input-group">
              <div class="input-wrapper">
                <input v-model="newRequest.amount" type="number" step="0.01" id="amount" placeholder=" " required>
                <label for="amount" class="floating-label">Amount (RM)</label>
                <i class="fa-solid fa-coins input-icon"></i>
              </div>
            </div>

            <div class="input-group">
              <div class="input-wrapper">
                <input v-model="newRequest.note" type="text" id="note" placeholder=" ">
                <label for="note" class="floating-label">Note (Optional)</label>
                <i class="fa-solid fa-sticky-note input-icon"></i>
              </div>
            </div>

            <div class="input-group">
              <label class="block text-xs text-indigo-300 uppercase font-bold tracking-widest mb-2 pl-1">Expiry Time (Hours)</label>
              <select v-model="newRequest.expiryHours" class="select-input">
                <option :value="1">1 Hour</option>
                <option :value="6">6 Hours</option>
                <option :value="12">12 Hours</option>
                <option :value="24">24 Hours</option>
                <option :value="48">48 Hours</option>
                <option :value="72">72 Hours</option>
              </select>
            </div>

            <button @click="createPaymentRequest" class="midnight-btn w-full">
              <i class="fas fa-qrcode mr-2"></i> Create Payment Request
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- QR Code Modal -->
    <Transition name="modal">
      <div v-if="showQRModal && selectedRequest" class="modal-overlay" @click.self="showQRModal = false">
        <div class="modal-content text-center">
          <div class="flex justify-between items-center mb-6">
            <h3 class="text-xl font-bold text-white">Payment Request QR Code</h3>
            <button @click="showQRModal = false" class="text-slate-400 hover:text-white">
              <i class="fas fa-times text-xl"></i>
            </button>
          </div>

          <div class="bg-white p-6 rounded-xl inline-block mb-6">
            <qrcode-vue :value="getQRData(selectedRequest)" :size="240" level="H" />
          </div>

          <div class="text-left space-y-3">
            <p class="text-white font-bold text-2xl">RM {{ selectedRequest.amount.toFixed(2) }}</p>
            <p class="text-slate-400" v-if="selectedRequest.note">{{ selectedRequest.note }}</p>
            <p class="text-xs text-slate-500 uppercase tracking-wide">Request ID: #{{ selectedRequest.id }}</p>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.payment-requests-container {
  animation: fadeIn 0.5s ease-out;
}

.request-card {
  background: rgba(255, 255, 255, 0.02);
  padding: 1.5rem;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.request-card:hover {
  border-color: rgba(99, 102, 241, 0.3);
  background: rgba(99, 102, 241, 0.05);
  transform: translateY(-2px);
}

.request-qr-preview {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: pointer;
  transition: all 0.3s;
  flex-shrink: 0;
}

.request-qr-preview:hover {
  transform: scale(1.1);
  box-shadow: 0 10px 20px rgba(99, 102, 241, 0.4);
}

.request-title {
  font-weight: 800;
  font-size: 1.3rem;
  color: white;
}

.request-note {
  font-size: 0.9rem;
  color: #94a3b8;
  margin-top: 4px;
  font-weight: 500;
}

.request-details {
  font-size: 0.75rem;
  color: #64748b;
  margin-top: 6px;
  font-weight: 600;
}

.status-badge {
  padding: 0.3rem 0.85rem;
  border-radius: 20px;
  font-size: 0.65rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.status-badge.blue {
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.3);
  color: #3b82f6;
}

.status-badge.green {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.3);
  color: #10b981;
}

.status-badge.orange {
  background: rgba(249, 115, 22, 0.1);
  border: 1px solid rgba(249, 115, 22, 0.3);
  color: #f97316;
}

.status-badge.red {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #ef4444;
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

.icon-btn.qr:hover {
  background: rgba(99, 102, 241, 0.2);
  border-color: #6366f1;
  color: #6366f1;
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
