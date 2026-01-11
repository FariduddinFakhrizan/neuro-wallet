<script setup>
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler
} from 'chart.js';
import { Line } from 'vue-chartjs';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler
);

const props = defineProps(['user']);

const selectedDays = ref(30);
const spendingData = ref(null);
const balanceHistory = ref([]);
const isLoading = ref(false);

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      backgroundColor: 'rgba(15, 23, 42, 0.9)',
      titleColor: '#fff',
      bodyColor: '#cbd5e1',
      borderColor: 'rgba(255, 255, 255, 0.1)',
      borderWidth: 1,
      padding: 10,
      displayColors: false,
    }
  },
  scales: {
    x: {
      grid: { display: false, drawBorder: false },
      ticks: { color: '#64748b', font: { size: 10 } }
    },
    y: {
      grid: { color: 'rgba(255, 255, 255, 0.05)', drawBorder: false },
      ticks: { color: '#64748b', font: { size: 10 }, callback: (val) => 'RM ' + val }
    }
  },
  elements: {
    line: { tension: 0.4 },
    point: { radius: 0, hitRadius: 10, hoverRadius: 4 }
  }
};

const chartData = computed(() => {
  if (!balanceHistory.value || balanceHistory.value.length === 0) return null;
  
  return {
    labels: balanceHistory.value.map(d => {
      const date = new Date(d.date);
      return `${date.getDate()}/${date.getMonth() + 1}`;
    }),
    datasets: [{
      label: 'Balance',
      backgroundColor: (ctx) => {
        const canvas = ctx.chart.ctx;
        const gradient = canvas.createLinearGradient(0, 0, 0, 400);
        gradient.addColorStop(0, 'rgba(99, 102, 241, 0.5)');
        gradient.addColorStop(1, 'rgba(99, 102, 241, 0.0)');
        return gradient;
      },
      borderColor: '#6366f1',
      data: balanceHistory.value.map(d => d.balance),
      fill: true,
    }]
  };
});

const fetchData = async () => {
  isLoading.value = true;
  spendingData.value = null; // Reset to trigger transitions
  
  try {
    const [spendingRes, historyRes] = await Promise.all([
      axios.get(`http://localhost:8080/api/analytics/spending/${props.user.id}?days=${selectedDays.value}`),
      axios.get(`http://localhost:8080/api/analytics/balance-history/${props.user.id}?days=${selectedDays.value}`)
    ]);
    
    spendingData.value = spendingRes.data;
    balanceHistory.value = historyRes.data;
  } catch (error) {
    console.error('Failed to fetch analytics', error);
  } finally {
    isLoading.value = false;
  }
};

const exportToCSV = async () => {
  try {
    const res = await axios.get(`http://localhost:8080/api/analytics/export/${props.user.id}`, {
      responseType: 'blob'
    });
    
    const url = window.URL.createObjectURL(new Blob([res.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `transactions_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    link.remove();
  } catch (error) {
    console.error('Failed to export CSV', error);
  }
};

const getCategoryColor = (category) => {
  const colors = {
    RENT: '#f97316',
    FOOD: '#10b981',
    SALARY: '#22c55e',
    ENTERTAINMENT: '#a855f7',
    UTILITIES: '#3b82f6',
    SHOPPING: '#ec4899',
    HEALTHCARE: '#ef4444',
    TRANSPORT: '#f59e0b',
    EDUCATION: '#6366f1',
    OTHER: '#64748b'
  };
  return colors[category] || '#64748b';
};

const getCategoryIcon = (category) => {
  const icons = {
    RENT: 'fa-home',
    FOOD: 'fa-utensils',
    SALARY: 'fa-money-bill-wave',
    ENTERTAINMENT: 'fa-film',
    UTILITIES: 'fa-lightbulb',
    SHOPPING: 'fa-shopping-bag',
    HEALTHCARE: 'fa-heartbeat',
    TRANSPORT: 'fa-car',
    EDUCATION: 'fa-graduation-cap',
    OTHER: 'fa-circle'
  };
  return icons[category] || 'fa-circle';
};

const getPercentage = (amount, total) => {
  if (total === 0) return 0;
  return ((amount / total) * 100).toFixed(1);
};

let pollingInterval;

onMounted(() => {
  fetchData();
  // Poll for real-time updates every 5 seconds
  pollingInterval = setInterval(() => {
    // Silent fetch (don't set isLoading)
    const silentFetch = async () => {
      try {
        const [spendingRes, historyRes] = await Promise.all([
          axios.get(`http://localhost:8080/api/analytics/spending/${props.user.id}?days=${selectedDays.value}`),
          axios.get(`http://localhost:8080/api/analytics/balance-history/${props.user.id}?days=${selectedDays.value}`)
        ]);
        spendingData.value = spendingRes.data;
        balanceHistory.value = historyRes.data;
      } catch (error) {
        console.error('Failed to poll analytics', error);
      }
    };
    silentFetch();
  }, 5000);
});

import { onUnmounted } from 'vue';
onUnmounted(() => {
  if (pollingInterval) clearInterval(pollingInterval);
});
</script>

<template>
  <div class="analytics-container">
    <div class="flex flex-col md:flex-row md:justify-between md:items-center gap-4 mb-8">
      <div>
        <h3 class="section-title">Analytics Dashboard</h3>
        <p class="text-slate-400 text-sm mt-2">Spending insights and transaction reports</p>
      </div>
      <button @click="exportToCSV" class="midnight-btn-sm whitespace-nowrap">
        <i class="fas fa-download mr-2"></i> Export CSV
      </button>
    </div>

    <!-- Time Period Selector -->
    <div class="period-selector mb-8">
      <button 
        v-for="days in [1, 7, 30, 90]" 
        :key="days"
        :class="{ active: selectedDays === days }" 
        @click="selectedDays = days; fetchData()"
        class="period-btn"
      >
        {{ days === 1 ? 'Today' : days === 7 ? 'Last Week' : days === 30 ? 'Last Month' : '3 Months' }}
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="text-center py-12">
      <div class="inline-block relative">
         <i class="fas fa-circle-notch fa-spin text-4xl text-indigo-400"></i>
      </div>
      <p class="text-slate-400 mt-4 animate-pulse">Analyzing financial data...</p>
    </div>

    <!-- Analytics Content -->
    <div v-else-if="spendingData" class="space-y-8 animate-in">
      
      <!-- Trend Chart -->
      <div class="chart-card">
        <h4 class="text-sm font-bold text-slate-400 uppercase tracking-wider mb-4">
          Balance Trend
        </h4>
        <div class="h-[250px] w-full">
           <Line v-if="chartData" :data="chartData" :options="chartOptions" />
           <div v-else class="h-full flex items-center justify-center text-slate-500 text-sm">
             Not enough data to display trend
           </div>
        </div>
      </div>

      <!-- Summary Cards -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div class="stat-card">
          <div class="stat-icon green">
            <i class="fas fa-arrow-down"></i>
          </div>
          <div>
            <p class="stat-label">Total Received</p>
            <p class="stat-value green">RM {{ spendingData.totalIncome?.toFixed(2) || '0.00' }}</p>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon orange">
            <i class="fas fa-arrow-up"></i>
          </div>
          <div>
            <p class="stat-label">Total Spent</p>
            <p class="stat-value orange">RM {{ spendingData.totalExpenses?.toFixed(2) || '0.00' }}</p>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon blue">
            <i class="fas fa-chart-line"></i>
          </div>
          <div>
            <p class="stat-label">Net Change</p>
            <p :class="['stat-value', (spendingData.totalIncome - spendingData.totalExpenses) >= 0 ? 'green' : 'red']">
              RM {{ ((spendingData.totalIncome || 0) - (spendingData.totalExpenses || 0)).toFixed(2) }}
            </p>
          </div>
        </div>
      </div>

      <!-- Spending by Category -->
      <div class="category-breakdown">
        <h4 class="text-lg font-bold text-white mb-6 flex items-center">
          <i class="fas fa-chart-pie mr-3 text-indigo-400"></i>
          Spending by Category
        </h4>

        <div v-if="spendingData.categoryBreakdown && Object.keys(spendingData.categoryBreakdown).length > 0" class="space-y-4">
          <div v-for="(amount, category) in spendingData.categoryBreakdown" :key="category" class="category-item">
            <div class="flex items-center justify-between mb-2">
              <div class="flex items-center gap-3">
                <div class="category-dot" :style="{ backgroundColor: getCategoryColor(category) }"></div>
                <div class="flex items-center gap-2">
                  <i :class="['fas', getCategoryIcon(category)]" :style="{ color: getCategoryColor(category) }"></i>
                  <span class="category-name">{{ category }}</span>
                </div>
              </div>
              <div class="text-right">
                <p class="category-amount">RM {{ amount.toFixed(2) }}</p>
                <p class="category-percentage">{{ getPercentage(amount, spendingData.totalExpenses) }}%</p>
              </div>
            </div>
            <div class="progress-bar">
              <div 
                class="progress-fill" 
                :style="{ 
                  width: getPercentage(amount, spendingData.totalExpenses) + '%',
                  backgroundColor: getCategoryColor(category)
                }"
              ></div>
            </div>
          </div>
        </div>

        <div v-else class="empty-state-small">
          <i class="fas fa-chart-bar text-4xl mb-4 opacity-10"></i>
          <p class="text-slate-500 text-sm">No spending data available for this period</p>
        </div>
      </div>
    </div>

    <!-- No Data State -->
    <div v-else class="empty-state">
      <i class="fas fa-chart-line text-6xl mb-6 opacity-10"></i>
      <p class="uppercase tracking-widest font-black text-xs opacity-40">No analytics data available</p>
    </div>
  </div>
</template>

<style scoped>
.analytics-container {
  max-width: 1000px;
  margin: 0 auto;
}

.period-selector {
  display: flex;
  background: rgba(15, 23, 42, 0.6);
  padding: 0.35rem;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  overflow-x: auto;
}

.period-btn {
  flex: 1;
  padding: 0.6rem 1rem;
  background: transparent;
  border: none;
  color: #64748b;
  font-weight: 700;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s;
  white-space: nowrap;
}

.period-btn.active {
  background: rgba(255, 255, 255, 0.1);
  color: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.period-btn:hover:not(.active) {
  color: #94a3b8;
  background: rgba(255, 255, 255, 0.02);
}

.chart-card {
  background: rgba(30, 41, 59, 0.4);
  padding: 1.5rem;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  margin-bottom: 2rem;
}

.stat-card {
  background: rgba(30, 41, 59, 0.4);
  padding: 1.5rem;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  gap: 1.25rem;
  transition: all 0.3s;
}

.stat-card:hover {
  border-color: rgba(99, 102, 241, 0.3);
  background: rgba(30, 41, 59, 0.6);
  transform: translateY(-2px);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  flex-shrink: 0;
}

.stat-icon.green {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.stat-icon.orange {
  background: rgba(249, 115, 22, 0.1);
  color: #f97316;
}

.stat-icon.blue {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.stat-label {
  font-size: 0.7rem;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-weight: 700;
  margin-bottom: 0.25rem;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: white;
}

.stat-value.green { color: #10b981; }
.stat-value.orange { color: #f97316; }
.stat-value.red { color: #ef4444; }

.category-breakdown {
  background: rgba(30, 41, 59, 0.4);
  padding: 2rem;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.category-item {
  padding: 1rem;
  background: rgba(15, 23, 42, 0.3);
  border-radius: 12px;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.category-item:hover {
  border-color: rgba(255, 255, 255, 0.05);
  transform: translateX(4px);
}

.category-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  box-shadow: 0 0 8px currentColor;
}

.category-name {
  font-weight: 700;
  font-size: 0.85rem;
  color: #e2e8f0;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.category-amount {
  font-weight: 800;
  font-size: 1rem;
  color: white;
}

.category-percentage {
  font-size: 0.7rem;
  color: #94a3b8;
  font-weight: 700;
}

.progress-bar {
  width: 100%;
  height: 6px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 10px;
  transition: width 1s cubic-bezier(0.4, 0, 0.2, 1);
}

.midnight-btn-sm {
  padding: 0.6rem 1.25rem;
  background: rgba(99, 102, 241, 0.1);
  color: #818cf8;
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 10px;
  font-weight: 700;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  cursor: pointer;
  transition: all 0.3s;
}

.midnight-btn-sm:hover {
  background: #6366f1;
  color: white;
  border-color: #6366f1;
  box-shadow: 0 0 15px rgba(99, 102, 241, 0.4);
}

.empty-state-small {
  text-align: center;
  padding: 3rem 2rem;
}

.animate-in {
  animation: fadeIn 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); filter: blur(4px); }
  to { opacity: 1; transform: translateY(0); filter: blur(0); }
}
</style>
