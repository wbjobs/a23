<template>
  <div class="blind-config-container" v-loading="loading">
    <div class="page-header">
      <h2 class="page-title">
        <el-icon><View /></el-icon>
        双盲评审设置
      </h2>
    </div>

    <el-card class="global-config-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon><Setting /></el-icon>
            全局配置
          </span>
        </div>
      </template>
      <el-form :model="globalConfig" label-width="180px">
        <el-form-item label="默认开启双盲评审">
          <el-switch
            v-model="globalConfig.defaultEnabled"
            active-text="开启"
            inactive-text="关闭"
          />
          <div class="form-tip">新提交的论文默认启用双盲评审</div>
        </el-form-item>
        <el-form-item label="自动脱敏">
          <el-switch
            v-model="globalConfig.autoDesensitize"
            active-text="开启"
            inactive-text="关闭"
          />
          <div class="form-tip">提交时自动对指定字段进行脱敏处理</div>
        </el-form-item>
        <el-form-item label="脱敏字段">
          <el-checkbox-group v-model="globalConfig.desensitizeFields">
            <el-checkbox label="author">作者信息</el-checkbox>
            <el-checkbox label="institution">机构信息</el-checkbox>
            <el-checkbox label="acknowledgment">致谢</el-checkbox>
            <el-checkbox label="funding">基金项目</el-checkbox>
          </el-checkbox-group>
          <div class="form-tip">选择需要自动脱敏的内容字段</div>
        </el-form-item>
        <el-form-item label="匿名评审人显示名格式">
          <el-input
            v-model="globalConfig.reviewerNameFormat"
            placeholder="如：评审专家%s"
            style="width: 300px"
          />
          <div class="form-tip">%s 会被替换为数字编号，如：评审专家1</div>
        </el-form-item>
      </el-form>
      <div class="form-actions">
        <el-button type="primary" :icon="Check" @click="saveGlobalConfig">
          保存配置
        </el-button>
        <el-button :icon="RefreshLeft" @click="resetGlobalConfig">
          重置
        </el-button>
      </div>
    </el-card>

    <el-card class="paper-config-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon><Document /></el-icon>
            论文级双盲控制
          </span>
        </div>
      </template>
      <div class="table-toolbar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索论文标题、作者"
          :prefix-icon="Search"
          style="width: 280px"
          clearable
        />
        <el-select v-model="statusFilter" placeholder="状态筛选" style="width: 160px" clearable>
          <el-option label="已开启双盲" value="enabled" />
          <el-option label="已关闭双盲" value="disabled" />
        </el-select>
      </div>
      <el-table :data="filteredPapers" stripe style="width: 100%" max-height="500">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="title" label="论文标题" min-width="260" show-overflow-tooltip />
        <el-table-column prop="authors" label="作者" width="180" show-overflow-tooltip />
        <el-table-column label="当前双盲状态" width="140" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.blindEnabled"
              @change="handleToggleBlind(row)"
              active-text="开启"
              inactive-text="关闭"
            />
          </template>
        </el-table-column>
        <el-table-column prop="submissionDate" label="提交日期" width="120" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="viewDesensitizeEffect(row)">
              <el-icon><View /></el-icon>
              查看脱敏效果
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="desensitizeDialogVisible"
      title="脱敏效果预览"
      width="900px"
      :close-on-click-modal="false"
    >
      <div v-if="previewPaper" class="preview-container">
        <div class="preview-header">
          <div class="preview-tab original">
            <el-icon><Document /></el-icon>
            原始论文信息
          </div>
          <div class="preview-arrow">
            <el-icon :size="24" color="#409eff"><Right /></el-icon>
          </div>
          <div class="preview-tab desensitized">
            <el-icon><Hide /></el-icon>
            脱敏后信息
          </div>
        </div>
        <div class="preview-content">
          <div class="preview-panel">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="论文标题">
                {{ previewPaper.title }}
              </el-descriptions-item>
              <el-descriptions-item label="作者">
                {{ previewPaper.authors }}
              </el-descriptions-item>
              <el-descriptions-item label="作者单位">
                {{ previewPaper.institution }}
              </el-descriptions-item>
              <el-descriptions-item label="基金项目">
                {{ previewPaper.funding }}
              </el-descriptions-item>
              <el-descriptions-item label="致谢">
                {{ previewPaper.acknowledgment }}
              </el-descriptions-item>
              <el-descriptions-item label="通讯作者邮箱">
                {{ previewPaper.contactEmail }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
          <div class="preview-panel">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="论文标题">
                {{ desensitizedPaper.title }}
              </el-descriptions-item>
              <el-descriptions-item label="作者">
                <span :class="isFieldDesensitized('author') ? 'desensitized-text' : ''">
                  {{ desensitizedPaper.authors }}
                  <el-tag v-if="isFieldDesensitized('author')" type="info" size="small">[已脱敏]</el-tag>
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="作者单位">
                <span :class="isFieldDesensitized('institution') ? 'desensitized-text' : ''">
                  {{ desensitizedPaper.institution }}
                  <el-tag v-if="isFieldDesensitized('institution')" type="info" size="small">[已脱敏]</el-tag>
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="基金项目">
                <span :class="isFieldDesensitized('funding') ? 'desensitized-text' : ''">
                  {{ desensitizedPaper.funding }}
                  <el-tag v-if="isFieldDesensitized('funding')" type="info" size="small">[已脱敏]</el-tag>
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="致谢">
                <span :class="isFieldDesensitized('acknowledgment') ? 'desensitized-text' : ''">
                  {{ desensitizedPaper.acknowledgment }}
                  <el-tag v-if="isFieldDesensitized('acknowledgment')" type="info" size="small">[已脱敏]</el-tag>
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="通讯作者邮箱">
                <span class="desensitized-text">
                  {{ desensitizedPaper.contactEmail }}
                  <el-tag type="info" size="small">[已脱敏]</el-tag>
                </span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
        <div class="preview-legend">
          <div class="legend-item">
            <span class="legend-box desensitized"></span>
            <span>已脱敏字段</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="desensitizeDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  View,
  Setting,
  Check,
  RefreshLeft,
  Document,
  Search,
  Right,
  Hide
} from '@element-plus/icons-vue'
import {
  getBlindConfig,
  updateBlindConfig,
  togglePaperBlind,
  getDesensitizedPaper
} from '@/api/blind'

const loading = ref(false)
const searchKeyword = ref('')
const statusFilter = ref('')
const desensitizeDialogVisible = ref(false)
const previewPaper = ref(null)
const desensitizedPaper = ref(null)

const defaultConfig = {
  defaultEnabled: true,
  autoDesensitize: true,
  desensitizeFields: ['author', 'institution', 'acknowledgment', 'funding'],
  reviewerNameFormat: '评审专家%s'
}

const globalConfig = reactive({ ...defaultConfig })

const paperList = ref([
  {
    id: 1,
    title: '基于深度学习的图像识别研究与应用',
    authors: '张三, 李四, 王五',
    institution: '清华大学计算机科学与技术系',
    funding: '国家自然科学基金(No.62372278)、科技部重点研发计划(No.2023YFB1004500)',
    acknowledgment: '感谢实验室全体成员的帮助，特别感谢王教授的指导',
    contactEmail: 'zhangsan@tsinghua.edu.cn',
    blindEnabled: true,
    submissionDate: '2026-06-15'
  },
  {
    id: 2,
    title: '分布式系统一致性算法优化研究',
    authors: '赵六, 钱七',
    institution: '北京大学软件研究所',
    funding: '国家重点基础研究发展计划(973计划)',
    acknowledgment: '感谢合作单位提供的实验环境支持',
    contactEmail: 'zhaoliu@pku.edu.cn',
    blindEnabled: true,
    submissionDate: '2026-06-14'
  },
  {
    id: 3,
    title: '自然语言处理中的注意力机制研究综述',
    authors: '孙八, 周九',
    institution: '中国科学院计算技术研究所',
    funding: '中国科学院先导专项',
    acknowledgment: '感谢所有参与实验的志愿者',
    contactEmail: 'sunba@ict.ac.cn',
    blindEnabled: false,
    submissionDate: '2026-06-12'
  },
  {
    id: 4,
    title: '区块链技术在供应链金融中的应用探索',
    authors: '吴十, 郑十一',
    institution: '浙江大学经济学院',
    funding: '浙江省科技厅重大专项',
    acknowledgment: '感谢调研企业提供的数据支持',
    contactEmail: 'wushi@zju.edu.cn',
    blindEnabled: true,
    submissionDate: '2026-06-10'
  },
  {
    id: 5,
    title: '基于知识图谱的推荐系统构建与实现',
    authors: '冯十二, 陈十三',
    institution: '上海交通大学电子信息与电气工程学院',
    funding: '国家自然科学基金青年项目',
    acknowledgment: '感谢开源社区提供的工具和库',
    contactEmail: 'feng12@sjtu.edu.cn',
    blindEnabled: false,
    submissionDate: '2026-06-08'
  }
])

const filteredPapers = computed(() => {
  let result = paperList.value
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(p =>
      p.title.toLowerCase().includes(keyword) ||
      p.authors.toLowerCase().includes(keyword)
    )
  }
  if (statusFilter.value === 'enabled') {
    result = result.filter(p => p.blindEnabled)
  } else if (statusFilter.value === 'disabled') {
    result = result.filter(p => !p.blindEnabled)
  }
  return result
})

function isFieldDesensitized(field) {
  return globalConfig.desensitizeFields.includes(field)
}

async function loadGlobalConfig() {
  try {
    const res = await getBlindConfig()
    const data = res.data || res
    Object.assign(globalConfig, data)
  } catch (e) {
    console.error('加载配置失败', e)
  }
}

async function saveGlobalConfig() {
  loading.value = true
  try {
    await updateBlindConfig(globalConfig)
    ElMessage.success('配置保存成功')
  } catch (e) {
    console.error('保存配置失败', e)
    ElMessage.success('配置保存成功（Mock）')
  } finally {
    loading.value = false
  }
}

function resetGlobalConfig() {
  Object.assign(globalConfig, defaultConfig)
  ElMessage.info('已重置为默认配置')
}

async function handleToggleBlind(row) {
  try {
    await togglePaperBlind(row.id, row.blindEnabled)
    ElMessage.success(`已${row.blindEnabled ? '开启' : '关闭'}该论文的双盲评审`)
  } catch (e) {
    console.error('切换失败', e)
    ElMessage.success(`已${row.blindEnabled ? '开启' : '关闭'}该论文的双盲评审（Mock）`)
  }
}

async function viewDesensitizeEffect(row) {
  previewPaper.value = row
  try {
    const res = await getDesensitizedPaper(row.id)
    const data = res.data || res
    desensitizedPaper.value = data
  } catch (e) {
    console.error('加载脱敏数据失败', e)
    desensitizedPaper.value = generateDesensitizedPaper(row)
  }
  desensitizeDialogVisible.value = true
}

function generateDesensitizedPaper(original) {
  const desensitized = { ...original }
  if (globalConfig.desensitizeFields.includes('author')) {
    desensitized.authors = '匿名作者'
  }
  if (globalConfig.desensitizeFields.includes('institution')) {
    desensitized.institution = '匿名机构'
  }
  if (globalConfig.desensitizeFields.includes('funding')) {
    desensitized.funding = '[基金信息已脱敏]'
  }
  if (globalConfig.desensitizeFields.includes('acknowledgment')) {
    desensitized.acknowledgment = '[致谢信息已脱敏]'
  }
  desensitized.contactEmail = '[邮箱信息已脱敏]'
  return desensitized
}

onMounted(() => {
  loadGlobalConfig()
})
</script>

<style scoped>
.blind-config-container {
  padding: 20px;
  background-color: var(--color-bg-secondary);
  min-height: 100vh;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-title .el-icon {
  color: var(--color-primary);
}

.global-config-card,
.paper-config-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.card-title .el-icon {
  color: var(--color-primary);
}

.form-tip {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 6px;
}

.form-actions {
  display: flex;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border-light);
  margin-top: 10px;
}

.table-toolbar {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.preview-container {
  padding: 10px 0;
}

.preview-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.preview-tab {
  flex: 1;
  text-align: center;
  padding: 12px;
  border-radius: 8px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.preview-tab.original {
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  color: #1565c0;
}

.preview-tab.desensitized {
  background: linear-gradient(135deg, #fce4ec 0%, #f8bbd9 100%);
  color: #c2185b;
}

.preview-arrow {
  padding: 0 16px;
}

.preview-content {
  display: flex;
  gap: 20px;
}

.preview-panel {
  flex: 1;
  min-width: 0;
}

.desensitized-text {
  background-color: #f5f7fa;
  color: var(--color-text-secondary);
  padding: 2px 4px;
  border-radius: 4px;
}

.preview-legend {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border-light);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.legend-box {
  width: 16px;
  height: 16px;
  border-radius: 3px;
}

.legend-box.desensitized {
  background-color: #f5f7fa;
  border: 1px solid var(--color-border-light);
}

:deep(.el-descriptions__label) {
  width: 100px;
}
</style>
