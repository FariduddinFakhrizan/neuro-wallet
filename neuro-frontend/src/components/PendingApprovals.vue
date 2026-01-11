<script setup>
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';

const props = defineProps(['user']);

const pendingApprovals = ref([]);
const approvalRequests = ref([]);
const isLoading = ref(false);
const activeTab = ref('pending'); // 'pending' or 'requests'
const errorMessage = ref('');
const successMessage = ref('');

const fetchData = async () => {
  isLoading.value = true;
  try {
    // Get approvals waiting for this user to approve
    const pendingRes = await axios.get(`http://localhost:8080/api/wallet/pending-approvals/${props.user.id}`);
    pendingApprovals.value = pendingRes.data;

    // Get approval requests this user has made
    const requestsRes = await axios.get(`http://localhost:8080/api/wallet/approval-requests/${props.user.id}`);
    approvalRequests.value = requestsRes.data;
  } catch (error) {
    console.error('Failed to fetch approvals', error);
  } finally {
    isLoading.value = false;
  }
};

const approveTransaction = async (approvalId) => {
  if (!confirm('Are you sure you want to approve this transaction?')) return;

  try {
    const payload = { approverId: props.user.id };
    await axios.post(`http://localhost:8080/api/wallet/pending-approvals/${approvalId}/approve`, payload);
    showMessage('Transaction approved and executed successfully', false);
    await fetchData();
  } catch (error) {
    showMessage(error.response?.data?.message || 'Failed to approve transaction', true);
  }
};

const rejectTransaction = async (approvalId) => {
  if (!confirm('Are you sure you want to reject this transaction?')) return;

  try {
    const payload = { approverId: props.user.id };
    await axios.post(`http://localhost:8080/api/wallet/pending-approvals/${approvalId}/reject`, payload);
    showMessage('Transaction rejected', false);
    await fetchData();
  } catch (error) {
    showMessage(error.response?.data?.message || 'Failed to reject transaction', true);
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

const formatDate = (timestamp) => {
  const date = new Date(timestamp);
  return date.toLocaleString();
};

const getStatusColor = (status) => {
  const colors = {
    PENDING: 'blue',
    APPROVED: 'green',
    REJECTED: 'red'
  };
  return colors[status] || 'slate';
};

const pendingCount = computed(() => pendingApprovals.value.filter(a => a.status === 'PENDING').length);

onMounted(() => {
  fetchData();
});
</script>

<template>
  <div class="approvals-container">
    <div class="flex justify-between items-center mb-8">
      <div>
        <h3 class="section-title">Transaction Approvals</h3>
        <p class="text-slate-400 text-sm mt-2">Multi-signature approval for transactions ≥ RM 1,000</p>
      </div>
      <div class="approval-threshold">
        <i class="fas fa-shield-alt mr-2"></i>
        Threshold: RM 1,000
      </div>
    </div>

    <!-- Messages -->
    <div v-if="successMessage" class="success-message mb-4">
      <i class="fas fa-check-circle mr-2"></i> {{ successMessage }}
    </div>
    <div v-if="errorMessage" class="error-text mb-4">{{ errorMessage }}</div>

    <!-- Tabs -->
    <div class="filter-tabs mb-6">
      <button 
        :class="{ active: activeTab === 'pending' }" 
        @click="activeTab = 'pending'"
        class="filter-tab"
      >
        <i class="fas fa-clock mr-2"></i>
        Pending Actions ({{ pendingCount }})
      </button>
      <button 
        :class="{ active: activeTab === 'requests' }" 
        @click="activeTab = 'requests'"
        class="filter-tab"
      >
        <i class="fas fa-paper-plane mr-2"></i>
        My Requests
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="text-center py-12">
      <i class="fas fa-circle-notch fa-spin text-4xl text-indigo-400"></i>
      <p class="text-slate-400 mt-4">Loading approvals...</p>
    </div>

    <!-- Pending Approvals (To Approve) -->
    <div v-else-if="activeTab === 'pending'" class="space-y-4">
      <div v-for="approval in pendingApprovals" :key="approval.id" class="approval-card">
        <div class="flex items-start justify-between">
          <div class="flex items-start gap-4">
            <div class="approval-icon pending">
              <i class="fas fa-hourglass-half"></i>
            </div>
            <div>
              <div class="flex items-center gap-3 mb-2">
                <h5 class="approval-title">Approval Request</h5>
                <span :class="['status-badge', getStatusColor(approval.status)]">
                  {{ approval.status }}
                </span>
              </div>
              <div class="approval-details">
                <p><strong>From:</strong> Node #{{ approval.requesterId }}</p>
                <p><strong>To:</strong> Node #{{ approval.recipientId }}</p>
                <p><strong>Amount:</strong> RM {{ approval.amount.toFixed(2) }}</p>
                <p><strong>Category:</strong> {{ approval.category }}</p>
                <p v-if="approval.note"><strong>Note:</strong> {{ approval.note }}</p>
                <p class="text-slate-500 mt-2">Requested: {{ formatDate(approval.createdAt) }}</p>
              </div>
            </div>
          </div>
          <div v-if="approval.status === 'PENDING'" class="flex gap-3">
            <button 
              @click="approveTransaction(approval.id)" 
              class="action-btn approve"
            >
              <i class="fas fa-check mr-2"></i> Approve
            </button>
            <button 
              @click="rejectTransaction(approval.id)" 
              class="action-btn reject"
            >
              <i class="fas fa-times mr-2"></i> Reject
            </button>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="pendingApprovals.length === 0" class="empty-state">
        <i class="fas fa-check-double text-6xl mb-6 opacity-10"></i>
        <p class="uppercase tracking-widest font-black text-xs opacity-40">No pending approvals</p>
      </div>
    </div>

    <!-- My Approval Requests -->
    <div v-else class="space-y-4">
      <div v-for="request in approvalRequests" :key="request.id" class="approval-card">
        <div class="flex items-start justify-between">
          <div class="flex items-start gap-4">
            <div :class="['approval-icon', getStatusColor(request.status)]">
              <i :class="['fas', request.status === 'APPROVED' ? 'fa-check-circle' : request.status === 'REJECTED' ? 'fa-times-circle' : 'fa-clock']"></i>
            </div>
            <div>
              <div class="flex items-center gap-3 mb-2">
                <h5 class="approval-title">Your Approval Request</h5>
                <span :class="['status-badge', getStatusColor(request.status)]">
                  {{ request.status }}
                </span>
              </div>
              <div class="approval-details">
                <p><strong>To:</strong> Node #{{ request.recipientId }}</p>
                <p><strong>Amount:</strong> RM {{ request.amount.toFixed(2) }}</p>
                <p><strong>Category:</strong> {{ request.category }}</p>
                <p><strong>Approver:</strong> Node #{{ request.approverId }}</p>
                <p v-if="request.note"><strong>Note:</strong> {{ request.note }}</p>
                <p class="text-slate-500 mt-2">Requested: {{ formatDate(request.createdAt) }}</p>
                <p v-if="request.processedAt" class="text-slate-500">
                  Processed: {{ formatDate(request.processedAt) }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="approvalRequests.length === 0" class="empty-state">
        <i class="fas fa-inbox text-6xl mb-6 opacity-10"></i>
        <p class="uppercase tracking-widest font-black text-xs opacity-40">No approval requests made</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.approvals-container {
  animation: fadeIn 0.5s ease-out;
}

.approval-card {
  background: rgba(255, 255, 255, 0.02);
  padding: 1.75rem;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.approval-card:hover {
  border-color: rgba(99, 102, 241, 0.3);
  background: rgba(99, 102, 241, 0.05);
}

.approval-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.4rem;
  flex-shrink: 0;
}

.approval-icon.pending,
.approval-icon.blue {
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.2);
  color: #3b82f6;
}

.approval-icon.green {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.2);
  color: #10b981;
}

.approval-icon.red {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.approval-title {
  font-weight: 700;
  font-size: 1.1rem;
  color: white;
}

.approval-details {
  font-size: 0.9rem;
  color: #94a3b8;
  line-height: 1.8;
}

.approval-details p strong {
  color: #cbd5e1;
  font-weight: 700;
  margin-right: 0.5rem;
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

.status-badge.red {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #ef4444;
}

.action-btn {
  padding: 0.65rem 1.25rem;
  border-radius: 10px;
  font-weight: 700;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
}

.action-btn.approve {
  background: rgba(16, 185, 129, 0.15);
  border: 1px solid rgba(16, 185, 129, 0.3);
  color: #10b981;
}

.action-btn.approve:hover {
  background: rgba(16, 185, 129, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(16, 185, 129, 0.3);
}

.action-btn.reject {
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #ef4444;
}

.action-btn.reject:hover {
  background: rgba(239, 68, 68, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(239, 68, 68, 0.3);
}

.approval-threshold {
  padding: 0.75rem 1.5rem;
  background: rgba(251, 191, 36, 0.1);
  border: 1px solid rgba(251, 191, 36, 0.3);
  border-radius: 12px;
  color: #fbbf24;
  font-weight: 700;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
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

.success-message {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.3);
  color: #10b981;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  font-size: 0.875rem;
  font-weight: 700;
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
