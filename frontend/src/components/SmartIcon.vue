<script setup lang="ts">
import { computed } from 'vue'
import { Icon } from '@iconify/vue'
import {
  Activity, Binary, Blocks, BookMarked, Box, BrainCircuit, Braces, Bug,
  Cable, Cloud, Code2, Cpu, Database, FileCode2, FolderCode, GitBranch,
  HardDrive, Layers3, Lightbulb, Network, Package, PanelsTopLeft,
  RadioTower, Server, ShieldCheck, Terminal, Workflow,
} from '@lucide/vue'
import linuxIcon from '@iconify-icons/devicon/linux'
import ubuntuIcon from '@iconify-icons/devicon/ubuntu'
import javaIcon from '@iconify-icons/devicon/java'
import springIcon from '@iconify-icons/devicon/spring'
import dockerIcon from '@iconify-icons/devicon/docker'
import kubernetesIcon from '@iconify-icons/devicon/kubernetes'
import mysqlIcon from '@iconify-icons/devicon/mysql'
import redisIcon from '@iconify-icons/devicon/redis'
import postgresqlIcon from '@iconify-icons/devicon/postgresql'
import mongodbIcon from '@iconify-icons/devicon/mongodb'
import nginxIcon from '@iconify-icons/devicon/nginx'
import vueIcon from '@iconify-icons/devicon/vuejs'
import nodeIcon from '@iconify-icons/devicon/nodejs'
import typescriptIcon from '@iconify-icons/devicon/typescript'
import javascriptIcon from '@iconify-icons/devicon/javascript'
import gitIcon from '@iconify-icons/devicon/git'
import githubIcon from '@iconify-icons/devicon/github'
import goIcon from '@iconify-icons/devicon/go'
import pythonIcon from '@iconify-icons/devicon/python'
import reactIcon from '@iconify-icons/devicon/react'
import rabbitmqIcon from '@iconify-icons/devicon/rabbitmq'
import kafkaIcon from '@iconify-icons/devicon/apachekafka'
import elasticsearchIcon from '@iconify-icons/devicon/elasticsearch'
import mavenIcon from '@iconify-icons/devicon/maven'
import gradleIcon from '@iconify-icons/devicon/gradle'
import tomcatIcon from '@iconify-icons/devicon/tomcat'
import jenkinsIcon from '@iconify-icons/devicon/jenkins'
import hadoopIcon from '@iconify-icons/devicon/hadoop'
import prometheusIcon from '@iconify-icons/devicon/prometheus'
import grafanaIcon from '@iconify-icons/devicon/grafana'
import bashIcon from '@iconify-icons/devicon/bash'

const props = withDefaults(defineProps<{
  text: string
  secondaryText?: string
  size?: number
}>(), {
  secondaryText: '',
  size: 22,
})

// 优先匹配真实技术 Logo；使用静态导入，确保 Android 离线也能显示。
const technologyRules = [
  { pattern: /typescript|type\s*script|\bts\b/i, icon: typescriptIcon },
  { pattern: /javascript|java\s*script|\bjs\b|ecmascript/i, icon: javascriptIcon },
  { pattern: /spring\s*boot|spring\s*cloud|springcloud|spring/i, icon: springIcon },
  { pattern: /kubernetes|\bk8s\b/i, icon: kubernetesIcon },
  { pattern: /postgres(?:ql)?|pgsql/i, icon: postgresqlIcon },
  { pattern: /mysql/i, icon: mysqlIcon },
  { pattern: /redis/i, icon: redisIcon },
  { pattern: /mongodb|mongo\b/i, icon: mongodbIcon },
  { pattern: /rabbit\s*mq/i, icon: rabbitmqIcon },
  { pattern: /kafka/i, icon: kafkaIcon },
  { pattern: /elastic\s*search|elasticsearch/i, icon: elasticsearchIcon },
  { pattern: /node\.?js|nodejs/i, icon: nodeIcon },
  { pattern: /vue(?:\.?js)?|vue3/i, icon: vueIcon },
  { pattern: /react/i, icon: reactIcon },
  { pattern: /ubuntu/i, icon: ubuntuIcon },
  { pattern: /linux|\bgnu\b/i, icon: linuxIcon },
  { pattern: /java(?!script)/i, icon: javaIcon },
  { pattern: /docker/i, icon: dockerIcon },
  { pattern: /nginx/i, icon: nginxIcon },
  { pattern: /python/i, icon: pythonIcon },
  { pattern: /golang|go\s*语言|\bgo\b/i, icon: goIcon },
  { pattern: /github/i, icon: githubIcon },
  { pattern: /git(?!hub|lab)/i, icon: gitIcon },
  { pattern: /jenkins/i, icon: jenkinsIcon },
  { pattern: /maven/i, icon: mavenIcon },
  { pattern: /gradle/i, icon: gradleIcon },
  { pattern: /prometheus/i, icon: prometheusIcon },
  { pattern: /grafana/i, icon: grafanaIcon },
  { pattern: /hadoop/i, icon: hadoopIcon },
  { pattern: /tomcat/i, icon: tomcatIcon },
  { pattern: /bash/i, icon: bashIcon },
]

// 无品牌 Logo 时，根据知识领域选择线条图标。
const semanticRules = [
  { pattern: /dubbo|微服务|rpc|grpc|服务治理|注册中心|nacos|zookeeper/i, icon: Network },
  { pattern: /消息队列|消息中间件|生产者|消费者|\bmq\b/i, icon: RadioTower },
  { pattern: /缓存|数据库|索引|事务|\bsql\b|持久化|存储/i, icon: Database },
  { pattern: /网络|\btcp\b|\budp\b|https?|\bdns\b|socket|负载均衡|网关/i, icon: Cable },
  { pattern: /容器|编排|部署|devops|运维|ci\/cd/i, icon: Blocks },
  { pattern: /安全|认证|鉴权|授权|权限|加密|\bjwt\b|oauth/i, icon: ShieldCheck },
  { pattern: /算法|数据结构|排序|二叉树|图论|哈希/i, icon: Binary },
  { pattern: /进程|线程|内核|并发|死锁|\bcpu\b/i, icon: Cpu },
  { pattern: /测试|单测|集成测试|性能测试|缺陷/i, icon: Bug },
  { pattern: /前端|\bcss\b|\bhtml\b|浏览器|页面|组件/i, icon: PanelsTopLeft },
  { pattern: /接口|\bapi\b|\brest\b|graphql/i, icon: Code2 },
  { pattern: /日志|监控|指标|链路追踪/i, icon: Activity },
  { pattern: /文件|脚本|shell|命令行|终端/i, icon: Terminal },
  { pattern: /设计模式|架构|\bddd\b|分布式/i, icon: Workflow },
]

const fallbackIcons = [
  BookMarked, Box, BrainCircuit, Braces, Cloud, Code2, Database,
  FileCode2, FolderCode, GitBranch, HardDrive, Layers3, Lightbulb,
  Network, Package, Server, Terminal, Workflow,
]
const palettes = [
  { background: '#edf4ff', color: '#3b68cd' },
  { background: '#f0edff', color: '#7755d7' },
  { background: '#e8f7f3', color: '#208c79' },
  { background: '#fff3e7', color: '#ba6b24' },
  { background: '#fcecf3', color: '#b35085' },
  { background: '#e9f5fa', color: '#277d9d' },
]

// 同一文本始终得到同一个哈希，不会因刷新或列表顺序而跳动。
function stableHash(value: string): number {
  let hash = 2166136261
  for (const character of value.normalize('NFKC').toLowerCase()) {
    hash ^= character.codePointAt(0) ?? 0
    hash = Math.imul(hash, 16777619)
  }
  return hash >>> 0
}

function findTechnology(value: string) {
  return technologyRules.find((rule) => rule.pattern.test(value))?.icon ?? null
}

function findSemantic(value: string) {
  return semanticRules.find((rule) => rule.pattern.test(value))?.icon ?? null
}

const hash = computed(() => stableHash(props.text))
const palette = computed(() => palettes[hash.value % palettes.length]!)
const resolved = computed(() => {
  const logo = findTechnology(props.text)
  if (logo) return { logo, symbol: null }
  const semantic = findSemantic(props.text)
  if (semantic) return { logo: null, symbol: semantic }
  const secondaryLogo = findTechnology(props.secondaryText)
  if (secondaryLogo) return { logo: secondaryLogo, symbol: null }
  const secondarySemantic = findSemantic(props.secondaryText)
  return { logo: null, symbol: secondarySemantic ?? fallbackIcons[hash.value % fallbackIcons.length]! }
})
</script>

<template>
  <span class="smart-icon" :style="{ backgroundColor: palette.background, color: palette.color }" aria-hidden="true">
    <Icon v-if="resolved.logo" :icon="resolved.logo" :width="size" :height="size" />
    <component :is="resolved.symbol" v-else :size="size" :stroke-width="1.9" />
  </span>
</template>

<style scoped>
.smart-icon { display: grid; place-items: center; flex-shrink: 0; }
.smart-icon :deep(svg) { display: block; }
</style>
