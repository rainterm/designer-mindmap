<template>
  <div class="page-container monitor-page">
    <n-spin :show="booting">
      <section class="monitor-section">
        <div class="section-head">
          <div class="section-head-left">
            <h2>总览</h2>
            <span class="section-tip">当前资源占用</span>
          </div>
          <div class="section-head-right">
            <span class="muted">每 {{ refreshSec }} 秒自动刷新</span>
            <n-button quaternary :loading="loading" @click="refresh">刷新</n-button>
          </div>
        </div>
        <div class="gauge-row">
          <div v-for="g in gaugeMeta" :key="g.key" class="gauge-item">
            <div :ref="(el) => setGaugeEl(g.key, el)" class="gauge-chart" />
            <div class="gauge-label">{{ g.title }}</div>
            <div class="gauge-sub muted">{{ g.sub }}</div>
          </div>
        </div>
        <div v-if="server?.disks?.length" class="panel mt-16">
          <div class="panel-title">磁盘分区</div>
          <n-data-table :columns="diskColumns" :data="server.disks" :bordered="false" :flex-height="false" />
        </div>
      </section>
    </n-spin>
  </div>
</template>

<script setup>
import { computed, h, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { NProgress } from 'naive-ui'
import * as echarts from 'echarts/core'
import { GaugeChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import { getServerMonitor } from '@/api/monitor'
import { colors, radius } from '@/theme'

echarts.use([GaugeChart, CanvasRenderer])

const radiusPx = Number.parseInt(radius, 10) || 0

const refreshSec = 60
const loading = ref(false)
const booting = ref(true)
const server = ref(null)

const gaugeEls = { cpu: null, mem: null, jvm: null, disk: null }
const gauges = { cpu: null, mem: null, jvm: null, disk: null }
let timer = null

const primaryDisk = computed(() => (server.value?.disks || [])[0] || null)

const gaugeMeta = computed(() => [
  {
    key: 'cpu',
    title: '系统 CPU',
    sub: `进程 ${fmtPct(server.value?.cpu?.processUsage)} · ${server.value?.cpu?.availableProcessors ?? '-'} 核`
  },
  {
    key: 'mem',
    title: '物理内存',
    sub: `${server.value?.mem?.usedHuman || '-'} / ${server.value?.mem?.totalHuman || '-'}`
  },
  {
    key: 'jvm',
    title: 'JVM 堆',
    sub: `${server.value?.jvm?.heapUsedHuman || '-'} / ${server.value?.jvm?.heapMaxHuman || '-'}`
  },
  {
    key: 'disk',
    title: '磁盘',
    sub: `${primaryDisk.value?.usedHuman || '-'} / ${primaryDisk.value?.totalHuman || '-'}`
  }
])

const diskColumns = [
  { title: '路径', key: 'path' },
  { title: '总量', key: 'totalHuman', width: 110 },
  { title: '已用', key: 'usedHuman', width: 110 },
  { title: '可用', key: 'freeHuman', width: 110 },
  {
    title: '使用率',
    key: 'usage',
    width: 150,
    render: (row) =>
      h(NProgress, {
        type: 'line',
        percentage: Math.min(Number(row.usage) || 0, 100),
        indicatorPlacement: 'inside',
        height: 14,
        borderRadius: radiusPx,
        status: row.usage >= 90 ? 'error' : row.usage >= 75 ? 'warning' : 'success'
      })
  }
]

function setGaugeEl(key, el) {
  gaugeEls[key] = el
}

function fmtPct(v) {
  if (v == null || Number.isNaN(Number(v))) return '-'
  return `${Number(v).toFixed(1)}%`
}

function gaugeOption(value, color) {
  const v = value == null || Number.isNaN(Number(value)) ? 0 : Math.max(0, Math.min(100, Number(value)))
  return {
    series: [
      {
        type: 'gauge',
        startAngle: 210,
        endAngle: -30,
        min: 0,
        max: 100,
        radius: '92%',
        center: ['50%', '60%'],
        progress: { show: true, width: 10, itemStyle: { color } },
        axisLine: { lineStyle: { width: 10, color: [[1, colors.border]] } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        pointer: { show: false },
        anchor: { show: false },
        title: { show: false },
        detail: {
          valueAnimation: true,
          offsetCenter: [0, '4%'],
          formatter: '{value}%',
          color: colors.text,
          fontSize: 20,
          fontWeight: 600
        },
        data: [{ value: Number(v.toFixed(1)) }]
      }
    ]
  }
}

function initCharts() {
  ;['cpu', 'mem', 'jvm', 'disk'].forEach((k) => {
    if (gaugeEls[k]) gauges[k] = echarts.init(gaugeEls[k])
  })
}

function updateGauges() {
  gauges.cpu?.setOption(gaugeOption(server.value?.cpu?.systemUsage, colors.primary), true)
  gauges.mem?.setOption(gaugeOption(server.value?.mem?.usage, '#0EA5E9'), true)
  gauges.jvm?.setOption(gaugeOption(server.value?.jvm?.heapUsage, '#F59E0B'), true)
  gauges.disk?.setOption(gaugeOption(primaryDisk.value?.usage, '#8B5CF6'), true)
}

async function refresh() {
  loading.value = true
  try {
    const res = await getServerMonitor()
    server.value = res.data
    await nextTick()
    updateGauges()
  } finally {
    loading.value = false
    booting.value = false
  }
}

function onResize() {
  Object.values(gauges).forEach((c) => c?.resize())
}

onMounted(async () => {
  await nextTick()
  initCharts()
  await refresh()
  timer = setInterval(refresh, refreshSec * 1000)
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('resize', onResize)
  Object.values(gauges).forEach((c) => c?.dispose())
})
</script>

<style scoped>
/* 仪表盘高度按视口算，避免内容不足一屏时也撑满而出现多余空白 */
.page-container.monitor-page {
  min-height: calc(100vh - var(--layout-nav-height, 44px) - var(--layout-tags-height, 36px));
}

.muted {
  color: var(--color-text-muted);
  font-size: 12px;
}

.monitor-section {
  margin-top: 0;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.section-head-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.section-head-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-head h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}

.section-tip {
  font-size: 12px;
  color: var(--color-text-muted);
}

.gauge-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.gauge-item {
  background: var(--color-surface);
  border-radius: var(--radius);
  padding: 8px 8px 14px;
  text-align: center;
  border: 1px solid var(--color-border);
}

.gauge-chart {
  height: 140px;
}

.gauge-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
}

.gauge-sub {
  margin-top: 2px;
}

.panel {
  background: var(--color-surface);
  border-radius: var(--radius);
  padding: 16px 18px;
  border: 1px solid var(--color-border);
}

.panel-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 12px;
}

.mt-16 {
  margin-top: 16px;
}

@media (max-width: 900px) {
  .gauge-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
