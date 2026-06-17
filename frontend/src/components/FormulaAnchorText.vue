<template>
  <span class="formula-anchor-text" v-html="renderedHtml" @click="handleClick"></span>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { renderFormulaAnchors, detectFormulaReferences } from '@/utils/formula-parser'
import { getFormulaByNumber } from '@/api/formula'

const props = defineProps({
  text: {
    type: String,
    default: ''
  },
  paperId: {
    type: [String, Number],
    required: true
  }
})

const emit = defineEmits(['navigate'])

const loading = ref(false)

const renderedHtml = computed(() => {
  return renderFormulaAnchors(props.text)
})

const references = computed(() => {
  return detectFormulaReferences(props.text)
})

async function handleClick(event) {
  const target = event.target
  if (target.classList && target.classList.contains('formula-anchor')) {
    const number = target.getAttribute('data-number')
    if (!number) return
    
    event.stopPropagation()
    await navigateToFormula(number)
  }
}

async function navigateToFormula(number) {
  loading.value = true
  try {
    const res = await getFormulaByNumber(props.paperId, number)
    const data = res.data || res
    if (data && (data.page !== undefined || data.y !== undefined)) {
      emit('navigate', {
        number,
        page: data.page,
        y: data.y
      })
    } else {
      emit('navigate', { number, page: null, y: null })
    }
  } catch (e) {
    console.error('获取公式坐标失败', e)
    emit('navigate', { number, page: null, y: null })
    ElMessage.warning('未找到该公式的位置信息')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.formula-anchor-text {
  line-height: inherit;
}

:deep(.formula-anchor) {
  color: #409eff;
  text-decoration: underline;
  text-decoration-style: dashed;
  cursor: pointer;
  transition: all 0.2s;
  padding: 0 2px;
  border-radius: 3px;
}

:deep(.formula-anchor:hover) {
  background-color: rgba(64, 158, 255, 0.1);
  color: #66b1ff;
}
</style>
